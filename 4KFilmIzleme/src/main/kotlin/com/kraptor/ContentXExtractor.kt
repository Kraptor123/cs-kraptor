// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.kraptor

import android.util.Base64
import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.base64DecodeArray
import com.lagradost.cloudstream3.utils.*
import java.nio.charset.Charset

open class ContentX : ExtractorApi() {
    override val name            = "ContentX"
    override val mainUrl         = "https://contentx.me"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef   = referer ?: ""
        Log.d("kraptor_$name", "url » $url")

        val iSource = app.get(url, referer = extRef).text
        val iExtract = Regex("""window\.openPlayer\('([^']+)'""").find(iSource)!!.groups[1]?.value ?: throw ErrorLoadingException("iExtract is null")

        val subUrls = mutableSetOf<String>()

// DÜZELTİLMİŞ REGEX (Unicode ve özel karakterleri düzgün yakalıyor)
        Regex(""""file":"((?:\\\\\"|[^"])+)","label":"((?:\\\\\"|[^"])+)"""").findAll(iSource).forEach {
            val (subUrlRaw, subLangRaw) = it.destructured

            // URL ve Dil için escape karakterleri temizleme
            val subUrl = subUrlRaw.replace("\\/", "/").replace("\\u0026", "&").replace("\\", "")
            val subLang = subLangRaw
                .replace("\\u0131", "ı")
                .replace("\\u0130", "İ")
                .replace("\\u00fc", "ü")
                .replace("\\u00e7", "ç")
                .replace("\\u011f", "ğ")
                .replace("\\u015f", "ş")

            val keywords = listOf("tur", "tr", "türkçe", "turkce")
            val language = if (subLang.contains("Forced")) {
                "Turkish Forced"
            } else if (keywords.any { subLang.contains(it, ignoreCase = true) }) {
                "Turkish"
            } else {
                subLang
            }

            if (subUrl in subUrls) return@forEach
            subUrls.add(subUrl)

            subtitleCallback.invoke(
                SubtitleFile(
                    lang = language,
                    url = fixUrl(subUrl)
                )
            )
        }

        Log.d("Kekik_$name", "subtitle » $subUrls -- subtitle diger $subtitleCallback")

        val vidSource  = app.get("${mainUrl}/source2.php?v=${iExtract}", referer=extRef).text
        val vidExtract = Regex("""file":"([^"]+)""").find(vidSource)?.groups?.get(1)?.value ?: throw ErrorLoadingException("vidExtract is null")
        val m3uLink    = vidExtract.replace("\\", "")

        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = m3uLink,
                type = ExtractorLinkType.M3U8

            ) {
                headers = mapOf("Referer" to url,
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Norton/124.0.0.0") // "Referer" ayarı burada yapılabilir
                quality = Qualities.Unknown.value
            }
        )

        val iDublaj = Regex(""","([^']+)","Türkçe""").find(iSource)?.groups?.get(1)?.value
        if (iDublaj != null) {
            val dublajSource  = app.get("${mainUrl}/source2.php?v=${iDublaj}", referer=extRef).text
            val dublajExtract = Regex("""file":"([^"]+)""").find(dublajSource)!!.groups[1]?.value ?: throw ErrorLoadingException("dublajExtract is null")
            val dublajLink    = dublajExtract.replace("\\", "")

            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = dublajLink,
                    type = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to url,
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Norton/124.0.0.0") // "Referer" ayarı burada yapılabilir
                    quality = Qualities.Unknown.value
                }
            )
        }
    }
}


// RapidVid EXTRACTOR
open class RapidVid : ExtractorApi() {
    override val name = "RapidVid"
    override val mainUrl = "https://rapidvid.net"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
        val videoReq = app.get(url, referer = extRef).text

//        Log.d("kraptor_${this.name}", "url » $url")

//        Log.d("kraptor_${this.name}", "videoReq » $videoReq")

        // 1) Altyazıları çekelim
        val subUrls = mutableSetOf<String>()
        Regex(""""captions","file":"([^"]*)","label":"([^"]*)"\}""").findAll(videoReq).forEach {
            val (subUrl, subLang) = it.destructured
            val sublangDuz = subLang
                .replace("\\u0131", "ı")
                .replace("\\u0130", "İ")
                .replace("\\u00fc", "ü")
                .replace("\\u00e7", "ç")
            val keywords = listOf("tur", "tr", "türkçe", "turkce")
            val language = if (keywords.any { sublangDuz.contains(it, ignoreCase = true) }) {
                "Turkish"
            } else {
                sublangDuz
            }
            if (subUrls.add(subUrl)) {
                subtitleCallback(
                    SubtitleFile(
                        lang = language,
                        url = fixUrl(subUrl.replace("\\", ""))
                    )
                )
            }
        }

        // 2) “av(…)” formundaki gömülü Base64+şifreyi çöz
        var decodedUrl: String? = null
        Regex("""file":\s*av\('([^']*)'\)""").find(videoReq)?.groupValues?.get(1)?.let { encrypted ->
            decodedUrl = decodeAv(encrypted)
        }

        // 3) Eğer “av” yoksa fallback… eval-based JWSetup çözme
        if (decodedUrl == null) {
            val evalMatch = Regex(
                """\s*(eval\(function.*?)(?=\s*function+)""",
                RegexOption.DOT_MATCHES_ALL
            ).find(videoReq)?.groupValues?.get(1)
                ?: throw ErrorLoadingException("File not found")

            // Burada “getAndUnpack” işlevinizi çağırabilirsiniz
            val unpacked = getAndUnpack(getAndUnpack(evalMatch)).replace("\\\\", "\\")
            val hexString = Regex("""file":"([^"]+)","label""")
                .find(unpacked)
                ?.groupValues
                ?.get(1)
                ?: throw ErrorLoadingException("File not found")

            // Hex dizgisini UTF-8’e çevir
            val bytes = hexString.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            decodedUrl = String(bytes, Charsets.UTF_8)
        }

        // 4) Sonuç callback ile dön
        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = decodedUrl,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to extRef)
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )
    }

    /** av(o) → _(o) JavaScript fonksiyonunun Kotlin’e portu */
    private fun decodeAv(input: String): String {
        // 1) Dizgiyi ters çevir ve Base64’ten decode et
        val reversed = input.reversed()
        val firstPass = Base64.decode(reversed, Base64.DEFAULT)

        // 2) Her byte’ı “K9L” anahtarına göre küçült
        val key = "K9L"
        val adjusted = ByteArray(firstPass.size) { i ->
            val sub = (firstPass[i].toInt() and 0xFF) - ((key[i % 3].code % 5) + 1)
            sub.toByte()
        }

        // 3) İkinci Base64 çözümü ve UTF-8 string’e çevir
        val secondPass = Base64.decode(adjusted, Base64.DEFAULT)
        return String(secondPass, Charset.forName("UTF-8"))
    }
}

// Sobreatsesuyp EXTRACTOR

open class Sobreatsesuyp : ExtractorApi() {
    override val name            = "Sobreatsesuyp"
    override val mainUrl         = "https://sobreatsesuyp.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef = url

        Log.d("kraptor_${this.name}", "Sobreat url = $url")

        val videoReq = app.get(url, referer = extRef).text

        val file     = Regex("""file":"([^"]+)""").find(videoReq)?.groupValues?.get(1) ?: throw ErrorLoadingException("File not found")
        val postLink = "${mainUrl}/" + file.replace("\\", "")
        val rawList  = app.post(postLink, referer = extRef).parsedSafe<List<Any>>() ?: throw ErrorLoadingException("Post link not found")

        val postJson: List<SobreatsesuypVideoData> = rawList.drop(1).map { item ->
            val mapItem = item as Map<*, *>
            SobreatsesuypVideoData(
                title = mapItem["title"] as? String,
                file  = mapItem["file"]  as? String
            )
        }
        Log.d("kraptor_${this.name}", "postJson » $postJson")

        for (item in postJson) {
            if (item.file == null || item.title == null) continue

            val videoData = app.post("${mainUrl}/playlist/${item.file.substring(1)}.txt", referer = extRef).text

            callback.invoke(
                newExtractorLink(
                    source  = this.name,
                    name    = "${this.name} - ${item.title}",
                    url     = videoData,
                    type    = INFER_TYPE
                ) {
                    headers = mapOf("Referer" to extRef) // "Referer" ayarı burada yapılabilir
                    quality = getQualityFromName(Qualities.Unknown.value.toString())
                }
            )
        }
    }

    data class SobreatsesuypVideoData(
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("file")  val file: String?  = null
    )
}

// TRsTX EXTRACTOR

open class TRsTX : ExtractorApi() {
    override val name            = "TRsTX"
    override val mainUrl         = "https://trstx.org"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef = referer ?: ""

        val videoReq = app.get(url, referer=extRef).text

        val file     = Regex("""file":"([^"]+)""").find(videoReq)?.groupValues?.get(1) ?: throw ErrorLoadingException("File not found")
        val postLink = "${mainUrl}/" + file.replace("\\", "")
        val rawList  = app.post(postLink, referer=extRef).parsedSafe<List<Any>>() ?: throw ErrorLoadingException("Post link not found")

        val postJson: List<TrstxVideoData> = rawList.drop(1).map { item ->
            val mapItem = item as Map<*, *>
            TrstxVideoData(
                title = mapItem["title"] as? String,
                file  = mapItem["file"]  as? String
            )
        }
        Log.d("Kekik_${this.name}", "postJson » $postJson")

        val vidLinks = mutableSetOf<String>()
        val vidMap   = mutableListOf<Map<String, String>>()
        for (item in postJson) {
            if (item.file == null || item.title == null) continue

            val fileUrl   = "${mainUrl}/playlist/" + item.file.substring(1) + ".txt"
            val videoData = app.post(fileUrl, referer=extRef).text

            if (videoData in vidLinks) { continue }
            vidLinks.add(videoData)

            vidMap.add(mapOf(
                "title"     to item.title,
                "videoData" to videoData
            ))
        }


        for (mapEntry in vidMap) {
            Log.d("Kekik_${this.name}", "mapEntry » $mapEntry")
            val title    = mapEntry["title"] ?: continue
            val m3uLink = mapEntry["videoData"] ?: continue

            callback.invoke(
                newExtractorLink(
                    source  = this.name,
                    name    = "${this.name} - $title",
                    url     = m3uLink,
                    type    = INFER_TYPE
                ) {
                    headers = mapOf("Referer" to extRef) // "Referer" ayarı burada yapılabilir
                    quality = getQualityFromName(Qualities.Unknown.value.toString())
                }
            )
        }
    }

    data class TrstxVideoData(
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("file")  val file: String?  = null
    )
}


// TurboImgz EXTRACTOR

open class TurboImgz : ExtractorApi() {
    override val name            = "TurboImgz"
    override val mainUrl         = "https://turbo.imgz.me"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef   = referer ?: ""
        val videoReq = app.get(url.substringAfter("||"), referer=extRef).text

        val videoLink = Regex("""file: "(.*)",""").find(videoReq)?.groupValues?.get(1) ?: throw ErrorLoadingException("File not found")
        Log.d("Kekik_${this.name}", "videoLink » $videoLink")

        callback.invoke(
            newExtractorLink(
                source  = "${this.name} - " + url.substringBefore("||").uppercase(),
                name    = "${this.name} - " + url.substringBefore("||").uppercase(),
                url     = videoLink,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to extRef) // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )
    }
}

// TurkeyPlayer EXTRACTOR

open class TurkeyPlayer : ExtractorApi() {
    override val name = "TurkeyPlayer"
    override val mainUrl = "https://watch.turkeyplayer.com"
    override val requiresReferer = true


    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
        val videoReq = app.get(url, referer = extRef).text
        val m3uMatch = Regex("""\"file\":\"([^\"]+)\"""").find(videoReq)
        val rawM3u = m3uMatch?.groupValues?.get(1)?.replace("\\", "")
        val fixM3u = rawM3u?.replace("thumbnails.vtt", "master.txt")

        fixM3u?.contains("master.txt")?.let {
            if (!it) {
                val lang = when {
                    fixM3u.contains("tur", ignoreCase = true) -> "Turkish"
                    fixM3u.contains("tr", ignoreCase = true) -> "Turkish"
                    fixM3u.contains("Türkçe", ignoreCase = true) -> "Turkish"
                    fixM3u.contains("en", ignoreCase = true) -> "English"
                    else -> "Bilinmeyen"
                }
                subtitleCallback.invoke(SubtitleFile(lang, fixM3u.toString()))
            }

            Log.d("kraptor_unutulmaz", "normalized m3u » $fixM3u")


            val dil = Regex("""title\":\"([^\"]*)\"""").find(videoReq)
                ?.groupValues?.get(1)
                ?: throw ErrorLoadingException("Dil bulunamadı")
            val lang = when {
                dil.contains("SUB", ignoreCase = true) -> "Altyazılı"
                dil.contains("DUB", ignoreCase = true) -> "Dublaj"
                else -> ""
            }
            callback.invoke(
                newExtractorLink(
                    source = "TurkeyPlayerxBet $lang",
                    name = "TurkeyPlayerxBet $lang",
                    url = fixM3u,
                    type = ExtractorLinkType.M3U8,
                    {
                        this.quality = Qualities.Unknown.value
                        this.referer = extRef
                        this.headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0"
                        )
                    }
                )
            )
        }
    }
}

// VidMoxy EXTRACTOR


open class VidMoxy : ExtractorApi() {
    override val name            = "VidMoxy"
    override val mainUrl         = "https://vidmoxy.com"
    override val requiresReferer = true


    fun decodeHexEscapes(input: String): String {
        return input.replace(Regex("""\\x([0-9A-Fa-f]{2})""")) {
            val byte = it.groupValues[1].toInt(16).toByte()
            byte.toInt().toChar().toString()
        }
    }

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        Log.d("kraptor_unutulmaz", "url = $url")
        val extRef   = referer ?: ""
        val videoReq = app.get(url, referer=extRef).text
//        Log.d("kraptor_unutulmaz", "videoReq = $videoReq")
        val regex = Regex("""file\s*:\s*EE\.dd\("([^"]+)"""")
        val match = regex.find(videoReq)
        val encoded =  match?.groupValues?.get(1).toString()
        Log.d("kraptor_unutulmaz", "encoded = $encoded")
        val decoded = decodeEE(encoded)
        Log.d("kraptor_unutulmaz", "decoded = $decoded")
        val altyRegex = Regex(pattern = """"file": "([^"]*)"""", options = setOf(RegexOption.IGNORE_CASE))
        altyRegex.findAll(videoReq).map { match ->
            val url = fixUrl(match.groupValues[1])
            Log.d("kraptor_unutulmaz", "url = $url")
            val subLang = url
                .substringAfterLast("/")
                .substringBefore("_")
            Log.d("kraptor_unutulmaz", "subLang = $subLang")
            val keywords = listOf("tur", "tr", "türkçe", "turkce")
            val language = if (keywords.any { subLang.contains(it, ignoreCase = true) }) {
                "Turkish"
            } else {
                subLang
            }
            subtitleCallback.invoke(SubtitleFile(
                lang = language,
                url  = url
            ))
        }.toList()


        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = decoded,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Origin" to mainUrl) // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )
    }
}

fun decodeEE(encoded: String): String {
    // 1. URL-dostu işaretleri geri çevir
    var s = encoded.replace('-', '+')
        .replace('_', '/')
    // 2. Base64 uzunluğu mod4 değilse '=' ekle
    while (s.length % 4 != 0) {
        s += "="
    }
    // 3. Base64 decode
    val decodedBytes = base64DecodeArray(s)
    val a = String(decodedBytes, Charsets.UTF_8)

    // 4. ROT13 dönüşümü
    val rot13 = buildString {
        for (c in a) {
            when (c) {
                in 'A'..'Z' -> append(((c - 'A' + 13) % 26 + 'A'.code).toChar())
                in 'a'..'z' -> append(((c - 'a' + 13) % 26 + 'a'.code).toChar())
                else        -> append(c)
            }
        }
    }

    // 5. Tersine çevir ve döndür
    return rot13.reversed()
}

class VidMolyTo : VidMolyExtractor() {
    override var name    = "VidMoly"
    override var mainUrl = "https://vidmoly.to"
}

open class VidMolyExtractor : ExtractorApi() {
    override val name            = "VidMoly"
    override val mainUrl         = "https://vidmoly.net"
    override val requiresReferer = true
    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {

        val url = if (url.contains("https://vidmoly.to")){
            url.replace("vidmoly.to","vidmoly.net")
        }else{
            url
        }

        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36",
            "Sec-Fetch-Dest" to "iframe",
            "Referer" to "${mainUrl}/"
        )
        Log.d("kraptor_$name", "Vidmoly URL'si işleniyor: $url")
        val iSource = app.get(url, headers = headers, referer = "$mainUrl/").text
        Log.d("kraptor_$name", "Vidmoly iframe içeriği alındı, m3u8 aranıyor...")
        val matches = Regex("""file\s*:\s*"([^"]+\.m3u8[^"]*)"""").findAll(iSource).toList()
        if (matches.isEmpty()) {
            Log.w("kraptor_$name", "Vidmoly'de m3u8 link bulunamadı")
            return
        }
        Log.d("kraptor_$name", "Vidmoly'de ${matches.size} adet m3u8 bulundu")
        matches.forEachIndexed { index, match ->
            val m3uLink = match.groupValues[1]
            Log.d("kraptor_$name", "Vidmoly m3uLink[$index] → $m3uLink")
            callback(
                newExtractorLink(
                    source = "VidMoly",
                    name = "VidMoly",
                    url = m3uLink,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "https://vidmoly.to/"
                    this.quality = Qualities.Unknown.value
                    this.headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                    )
                }
            )
        }
    }
}
