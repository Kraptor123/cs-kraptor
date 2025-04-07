package com.keyiflerolsun

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.*

class Dzen : ExtractorApi(){
    override val name            = "Dzen"
    override val mainUrl         = "https://cdn.dzen.ru/"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val type = if (url.contains(".m3u8")) "m3u8" else if(url.contains(".mpd")) "dash" else null

        val extractorLink = when(type) {
            "m3u8" -> newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = url,
                type    = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to "") // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }

            "dash" -> newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = url,
                type    = ExtractorLinkType.DASH
            ) {
                headers = mapOf("Referer" to "") // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }

            else -> null
        }

        if (extractorLink != null) {
            callback.invoke(extractorLink)
        }
    }
}