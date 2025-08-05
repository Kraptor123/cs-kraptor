// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class TauVideo : ExtractorApi() {
    override val name            = "TauVideo"
    override val mainUrl         = "https://tau-video.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef = referer ?: ""
        Log.d("kraptor_$name", "extRef » $extRef")
        Log.d("kraptor_$name", "url » $url")

        val videolink = url.substringBefore("|")
        val cevirmen  = url.substringAfter("|")
        val videoKey  = videolink.substringAfterLast("/")


        val videoUrl = "${mainUrl}/api/video/${videoKey}"
        Log.d("kraptor_ACX", "videoUrl » $videoUrl")
        Log.d("kraptor_ACX", "cevirmen » $cevirmen")
        val api = app.get(videoUrl).parsedSafe<TauVideoUrls>() ?: throw ErrorLoadingException("TauVideo")
        val finalName = listOfNotNull(this.name, cevirmen)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" + ") ?: this.name
        for (video in api.urls) {
            callback.invoke(
                newExtractorLink(
                    source = finalName,
                    name = finalName,
                    url = video.url,
                    type = INFER_TYPE
                ) {
                    headers = mapOf("Referer" to extRef)
                    quality = getQualityFromName(video.label)
                }
            )
        }

        }

    data class RequestData(
        @JsonProperty("url") val url: String,
        @JsonProperty("extra") val extra: String
    )

    data class TauVideoUrls(
        @JsonProperty("urls") val urls: List<TauVideoData>
    )

    data class TauVideoData(
        @JsonProperty("url")   val url: String,
        @JsonProperty("label") val label: String,
    )
}