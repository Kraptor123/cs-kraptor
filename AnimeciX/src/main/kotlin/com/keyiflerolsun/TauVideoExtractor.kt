// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
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

        try {
            val mapper = jacksonObjectMapper()
            val isJson = url.trim().startsWith("{") && url.trim().endsWith("}")
            val videoKey: String
            val extraInfo: String?
            if (isJson) {
                val requestData = mapper.readValue<RequestData>(url)
                videoKey = requestData.url.split("/").last()
                extraInfo = requestData.extra
            } else {
                videoKey = url.split("/").last()
                extraInfo = null
            }

            val videoUrl = "${mainUrl}/api/video/${videoKey}"
            Log.d("ACX", "videoUrl » $videoUrl")
            Log.d("ACX", "extraInfo » $extraInfo")

            val api = app.get(videoUrl).parsedSafe<TauVideoUrls>() ?: throw ErrorLoadingException("TauVideo")

            val finalName = listOfNotNull(this.name, extraInfo)
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

        } catch (e: Exception) {
            Log.e("ACX", "TauVideo parsing error: ${e.message}")
            throw ErrorLoadingException("TauVideo parsing failed")
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