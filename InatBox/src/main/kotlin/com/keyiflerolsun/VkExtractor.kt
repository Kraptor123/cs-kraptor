package com.keyiflerolsun

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

class Vk : ExtractorApi() {
    override val name            = "Vk"
    override val mainUrl         = "https://vk.com/"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val response = app.get(
            url     = url,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
            referer = this.mainUrl,
        )

        val m3u8Regex     = Regex(""""([^"]*m3u8[^"]*)"""")
        val m3u8SourceUrl = m3u8Regex.find(response.text)?.groupValues?.get(1)?.replace("\\/", "/")

        if (m3u8SourceUrl != null) {
            callback.invoke(
                newExtractorLink(
                    source  = this.name,
                    name    = this.name,
                    url     = m3u8SourceUrl,
                    type    = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to mainUrl) // "Referer" ayarı burada yapılabilir
                    quality = getQualityFromName(Qualities.Unknown.value.toString())
                }
            )
        } else {
            return
        }
    }
}