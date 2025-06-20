// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.kraptor

import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import kotlin.text.iterator

open class VidMoxy : ExtractorApi() {
    override val name            = "VidMoxy"
    override val mainUrl         = "https://vidmoxy.com"
    override val requiresReferer = true


    fun decodeHexEscapes(input: String): String {
        return input.replace(Regex("""\\x([0-9A-Fa-f]{2})""")) {
            val byte = it.groupValues[1].toInt(16).toByte()
            byte.toChar().toString()
        }
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

        val extractedValue = Regex(""""file": "([^"]*)"""").find(videoReq)
            ?.groupValues?.get(1).toString()

        Log.d("filmizlesene", "extractedValue = $extractedValue")

        val realUrl = decodeHexEscapes(extractedValue)
        Log.d("filmizlesene", "realUrl = $realUrl")



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