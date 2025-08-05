// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlin.sequences.forEach


class VidMody : ExtractorApi() {
    override val name = "VidMody"
    override val mainUrl = "https://player.vidmody.com"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink> {
        val links = mutableListOf<ExtractorLink>()
        try {
            val iframeDoc = app.get(url).text
            val regex = Regex(pattern = "var id = '(tt\\d+)'", options = setOf(RegexOption.IGNORE_CASE))
            val matches = regex.findAll(iframeDoc)
            matches.forEach { match ->
                val id = match.groupValues[1]
                val videoLink = "https://vidmody.com/vs/$id"

                Log.d("flmcx", "vidid: $videoLink")

                links.add(
                    newExtractorLink(
                        source = name,
                        name = "VidMody",
                        url = videoLink,
                        type = ExtractorLinkType.M3U8
                    ) {
                        quality = Qualities.Unknown.value
                        headers = mapOf(
                            "Referer" to url,
                            "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36"
                        )
                    }
                )
            }
            return links
        } catch (e: Exception) {
            Log.e("flmcx", "VidMody hata: ${e.message}")
            return links
        }
    }
}