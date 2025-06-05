// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.kraptor

import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import android.util.Base64
import java.nio.charset.Charset

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

        // 1) Altyazıları çekelim
        val subUrls = mutableSetOf<String>()
        Regex("""captions","file":"([^"]+)","label":"([^"]+)"""").findAll(videoReq).forEach {
            val (subUrl, subLang) = it.destructured
            if (subUrls.add(subUrl)) {
                subtitleCallback(
                    SubtitleFile(
                        lang = subLang
                            .replace("\\u0131", "ı")
                            .replace("\\u0130", "İ")
                            .replace("\\u00fc", "ü")
                            .replace("\\u00e7", "ç"),
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
        callback(
            newExtractorLink(
                source = name,
                name = name,
                url = decodedUrl ?: throw ErrorLoadingException("Decoded URL yok"),
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