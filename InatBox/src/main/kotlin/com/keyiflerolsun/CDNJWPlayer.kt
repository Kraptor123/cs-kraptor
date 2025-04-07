package com.keyiflerolsun

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.*

class CDNJWPlayer : ExtractorApi() {
    override val name: String = "CDN JWPlayer"
    override val mainUrl: String = "https://cdn.jwplayer.com"
    override val requiresReferer: Boolean = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = url,
                type    = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to "") // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )
    }
}