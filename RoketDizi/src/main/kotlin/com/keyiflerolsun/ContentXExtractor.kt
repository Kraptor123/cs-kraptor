// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class ContentX : ExtractorApi() {
    override val name            = "ContentX"
    override val mainUrl         = "https://contentx.me"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = if (url.contains("https://four.pichive.online/iframe.php?")) {
           "https://dizilla.club/"
        }else {
            referer ?: ""
        }
        Log.d("Kekik_${this.name}", "url » $url")

        val iSource = app.get(url, referer = extRef, headers = mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")).text
        Log.d("Kekik_${this.name}", "iSource » $iSource")
        val iExtract = Regex("""window\.openPlayer\('([^']+)'""").find(iSource)!!.groups[1]?.value ?: throw ErrorLoadingException("iExtract is null")

        val subUrls = mutableSetOf<String>()
        Regex(""""file":"((?:\\\\\"|[^"])+)","label":"((?:\\\\\"|[^"])+)"""").findAll(iSource).forEach {
            val (subUrlExt, subLangExt) = it.destructured

            val subUrl = subUrlExt.replace("\\/", "/").replace("\\u0026", "&").replace("\\", "")
            val subLang = subLangExt.replace("\\u0131", "ı").replace("\\u0130", "İ").replace("\\u00fc", "ü").replace("\\u00e7", "ç").replace("\\u011f", "ğ").replace("\\u015f", "ş")

            if (subUrl in subUrls) return@forEach
            subUrls.add(subUrl)

            subtitleCallback.invoke(
                SubtitleFile(
                    lang = subLang,
                    url = fixUrl(subUrl)
                )
            )
        }

        val vidSource = app.get("${mainUrl}/source2.php?v=${iExtract}", referer = extRef).text
        val vidExtract = Regex("""file":"([^"]+)""").find(vidSource)?.groups?.get(1)?.value ?: throw ErrorLoadingException("vidExtract is null")
        val m3uLink = vidExtract.replace("\\", "")

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name   = this.name,
                url    = m3uLink,
                type   = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to url,
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Norton/124.0.0.0")
                quality = Qualities.Unknown.value
            }
        )

        val iDublaj = Regex(""","([^']+)","Türkçe""").find(iSource)?.groups?.get(1)?.value
        if (iDublaj != null) {
            val dublajSource = app.get("${mainUrl}/source2.php?v=${iDublaj}", referer = extRef).text
            val dublajExtract = Regex("""file":"([^"]+)""").find(dublajSource)!!.groups[1]?.value ?: throw ErrorLoadingException("dublajExtract is null")
            val dublajLink = dublajExtract.replace("\\", "")

            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name   = this.name,
                    url    = dublajLink,
                    type   = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to url,
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Norton/124.0.0.0")
                    quality = Qualities.Unknown.value
                }
            )
        }
    }
}