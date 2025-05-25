// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.kraptor

import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class VidMoxy : ExtractorApi() {
    override val name            = "VidMoxy"
    override val mainUrl         = "https://vidmoxy.com"
    override val requiresReferer = true


    fun String.rot13(): String = buildString {
        for (c in this@rot13) {
            when (c) {
                in 'A'..'Z' -> append(((c - 'A' + 13) % 26 + 'A'.code).toChar())
                in 'a'..'z' -> append(((c - 'a' + 13) % 26 + 'a'.code).toChar())
                else         -> append(c)
            }
        }
    }

    fun decodeHlsLink(encoded: String): String {
        // 2) Base64 → UTF-8
        val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
        val decoded     = decodedBytes.toString(Charsets.UTF_8)
        Log.d("filmizlesene", "afterBase64 = $decoded")

        // 3) String’i ters çevir
        val reversed    = decoded.reversed()
        Log.d("filmizlesene", "afterReverse = $reversed")

        // 4) Rot13 uygula
        val url         = reversed.rot13()
        Log.d("filmizlesene", "finalUrl = $url")

        return url
    }

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef   = referer ?: ""
        val videoReq = app.get(url, referer=extRef).text

        val subUrls = mutableSetOf<String>()
        Regex("""captions","file":"([^"]+)","label":"([^"]+)"""").findAll(videoReq).forEach {
            val (subUrl, subLang) = it.destructured

            if (subUrl in subUrls) { return@forEach }
            subUrls.add(subUrl)

            subtitleCallback.invoke(
                SubtitleFile(
                    lang = subLang.replace("\\u0131", "ı").replace("\\u0130", "İ").replace("\\u00fc", "ü").replace("\\u00e7", "ç"),
                    url  = fixUrl(subUrl.replace("\\", ""))
                )
            )
        }

        val extractedValue = Regex("""file: EE\.dd\("([^\"]*)"\)""").find(videoReq)
            ?.groupValues?.get(1)
        val realUrl = decodeHlsLink(extractedValue.toString())

        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = realUrl,
                type = ExtractorLinkType.M3U8
                 ) {
                     headers = mapOf("Referer" to extRef) // "Referer" ayarı burada yapılabilir
                     quality = getQualityFromName(Qualities.Unknown.value.toString())
                 }
            )
    }
}