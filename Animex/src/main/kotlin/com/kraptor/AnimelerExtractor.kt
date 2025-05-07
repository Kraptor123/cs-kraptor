// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document

class AnimelerExtractor : ExtractorApi() {
    override val name = "Animeler"
    override val mainUrl = "https://animeler.tr"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink> {
        val links = mutableListOf<ExtractorLink>()
        val document: Document = app.get(url).document

        // Playerjs içeren <script> bloğunu bul
        val scriptContent = document.select("script")
            .mapNotNull { it.data() }
            .firstOrNull { it.contains("new Playerjs") }

        scriptContent?.let { content ->
            // file:"[1080p]../video_proxy.php?...,[720p]..." kısmını ayıkla
            val fileString = Regex("""file\s*:\s*"([^"]+)"""")
                .find(content)
                ?.groupValues
                ?.get(1)
                ?: return links

            // Her "[1080p]../video_proxy.php..." parçasını yakala
            Regex("""\[(\d+p)]\s*(\.\./[^\],]+)""").findAll(fileString).forEach { match ->
                val qualityLabel = match.groupValues[1]          // "1080p"
                val relativePath = match.groupValues[2]          // "../video_proxy.php?id=...&quality=1080"
                val fullUrl = mainUrl + relativePath.removePrefix("..")
                val qualityInt = qualityLabel.removeSuffix("p").toIntOrNull() ?: 0
                val isM3u8 = fullUrl.endsWith(".m3u8")

                links += newExtractorLink(
                    source = name,
                    name = "Animeler $qualityLabel",
                    url = fullUrl,
                    type = null
                ) {
                    this.referer = referer ?: ""
                    this.quality = qualityInt
                    type = ExtractorLinkType.VIDEO
                }
            }
        }

        return links
    }
}
