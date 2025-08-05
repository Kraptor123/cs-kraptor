// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class MixTiger : ExtractorApi() {
    override val name            = "MixTiger"
    override val mainUrl         = "https://www.mixtiger.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val m3uLink:String?
        val extRef  = referer ?: ""
        val postUrl = "${url}?do=getVideo"
        Log.d("Kekik_${this.name}", "postUrl » $postUrl")

        val response = app.post(
            postUrl,
            data = mapOf(
                "hash" to url.substringAfter("video/"),
                "r"    to extRef,
                "s"    to ""
            ),
            referer = extRef,
            headers = mapOf(
                "Content-Type"     to "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With" to "XMLHttpRequest"
            )
        )

        val videoResponse = response.parsedSafe<FirePlayer>() ?: throw ErrorLoadingException("peace response is null")
        Log.d("Kekik_${this.name}", "videoResponse » $videoResponse")

        if (videoResponse.videoSrc != null) {
            m3uLink = videoResponse.videoSrc
            Log.d("Kekik_${this.name}", "m3uLink » $m3uLink")

            loadExtractor(m3uLink, extRef, subtitleCallback, callback)
        } else {
            val videoSources  = videoResponse.videoSources
            m3uLink = if (videoSources.isNotEmpty()) {
                videoSources.lastOrNull()?.file
            } else {
                null
            }

            Log.d("Kekik_${this.name}", "m3uLink » $m3uLink")

            callback.invoke(
                newExtractorLink(
                    source  = this.name,
                    name    = this.name,
                    url     = m3uLink ?: throw ErrorLoadingException("m3u link not found"),
                    type    = INFER_TYPE
                ) {
                    headers = mapOf("Referer" to extRef) // "Referer" ayarı burada yapılabilir
                    quality = getQualityFromName(Qualities.Unknown.value.toString())
                }
            )
        }
    }

    data class FirePlayer(
        @JsonProperty("videoSrc")     val videoSrc: String?               = null,
        @JsonProperty("videoSources") val videoSources: List<VideoSource> = emptyList(),
    )

    data class VideoSource(
        @JsonProperty("file")  val file: String,
        @JsonProperty("label") val label: String,
        @JsonProperty("type")  val type: String
    )
}