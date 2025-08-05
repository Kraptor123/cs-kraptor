// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.annotation.JsonProperty

open class Odnoklassniki : ExtractorApi() {
    override val name            = "Odnoklassniki"
    override val mainUrl         = "https://odnoklassniki.ru"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        Log.d("Kekik_${this.name}", "url » $url")

        val userAgent = mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")

        val videoReq  = app.get(url, headers=userAgent).text.replace("\\&quot;", "\"").replace("\\\\", "\\")
            .replace(Regex("\\\\u([0-9A-Fa-f]{4})")) { matchResult ->
                Integer.parseInt(matchResult.groupValues[1], 16).toChar().toString()
            }
        val videosStr = Regex(""""videos":(\[[^]]*])""").find(videoReq)?.groupValues?.get(1) ?: throw ErrorLoadingException("Video not found")
        val videos     = AppUtils.tryParseJson<List<OkRuVideo>>(videosStr) ?: throw ErrorLoadingException("Video not found")

        for (video in videos) {
            Log.d("Kekik_${this.name}", "video » $video")

            val videoUrl = if (video.url.startsWith("//")) "https:${video.url}" else video.url

            val quality   = video.name.uppercase()
                .replace("MOBILE", "144p")
                .replace("LOWEST", "240p")
                .replace("LOW",    "360p")
                .replace("SD",     "480p")
                .replace("HD",     "720p")
                .replace("FULL",   "1080p")
                .replace("QUAD",   "1440p")
                .replace("ULTRA",  "4k")

            callback.invoke(
                newExtractorLink(
                    source  = this.name,
                    name    = this.name,
                    url     = videoUrl,
                    type    = INFER_TYPE
                ) {
                    headers = mapOf("Referer" to "userAgent")
                    this.quality = getQualityFromName(quality)
                }
            )
        }
    }

    data class OkRuVideo(
        @JsonProperty("name") val name: String,
        @JsonProperty("url")  val url: String,
    )
}