package com.keyiflerolsun

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities

class Dzen : ExtractorApi(){
    override val name            = "Dzen"
    override val mainUrl         = "https://cdn.dzen.ru/"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val type = if (url.contains(".m3u8")) "m3u8" else if(url.contains(".mpd")) "dash" else null

        val extractorLink = when(type) {
            "m3u8" -> ExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = url,
                referer = "",
                quality = Qualities.Unknown.value,
                type    = ExtractorLinkType.M3U8
            )

            "dash" -> ExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = url,
                referer = "",
                quality = Qualities.Unknown.value,
                type    = ExtractorLinkType.DASH
            )

            else -> null
        }

        if (extractorLink != null) {
            callback.invoke(extractorLink)
        }
    }
}