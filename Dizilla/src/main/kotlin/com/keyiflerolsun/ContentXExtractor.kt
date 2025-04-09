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

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef   = referer ?: ""
        Log.d("Dizilla", "url » $url")

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

            if (subUrl in subUrls) return@forEach
            subUrls.add(subUrl)

            subtitleCallback.invoke(
                SubtitleFile(
                    lang = subLang,
                    url = fixUrl(subUrl)
                )
            )
        }

        Log.d("Dizilla", "subtitle » $subUrls -- subtitle diger $subtitleCallback")

        val vidSource  = app.get("${mainUrl}/source2.php?v=${iExtract}", referer=extRef).text
        val vidExtract = Regex("""file":"([^"]+)""").find(vidSource)!!.groups[1]?.value ?: throw ErrorLoadingException("vidExtract is null")
        val m3uLink    = vidExtract.replace("\\", "")

        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = m3uLink,
                type = ExtractorLinkType.M3U8

            ) {
                headers = mapOf("Referer" to url) // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString()) // Int değeri String'e dönüştürülüyor
            }
        )

        val iDublaj = Regex(""","([^']+)","Türkçe""").find(iSource)!!.groups[1]?.value
        if (iDublaj != null) {
            val dublajSource  = app.get("${mainUrl}/source2.php?v=${iDublaj}", referer=extRef).text
            val dublajExtract = Regex("""file":"([^"]+)""").find(dublajSource)!!.groups[1]?.value ?: throw ErrorLoadingException("dublajExtract is null")
            val dublajLink    = dublajExtract.replace("\\", "")

            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = m3uLink,
                    type = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to url)
                    quality = Qualities.Unknown.value
                }
            )
        }
    }
}