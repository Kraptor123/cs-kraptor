// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class YildizKisaFilm : ExtractorApi() {
    override val name            = "YildizKisaFilm"
    override val mainUrl         = "https://yildizkisafilm.org"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef  = referer ?: ""
        val vidId   = if (url.contains("video/")) {
            url.substringAfter("video/")
        } else {
            url.substringAfter("?data=")
        }
        val postUrl = "${mainUrl}/player/index.php?data=${vidId}&do=getVideo"
        Log.d("Kekik_${this.name}", "postUrl » $postUrl")

        val response = app.post(
            postUrl,
            data = mapOf(
                "hash" to vidId,
                "r"    to extRef
            ),
            referer = extRef,
            headers = mapOf(
                "Content-Type"     to "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With" to "XMLHttpRequest"
            )
        )

        val videoResponse = response.parsedSafe<SystemResponse>() ?: throw ErrorLoadingException("failed to parse response")
        val m3uLink       = videoResponse.securedLink

        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = m3uLink,
                type    = INFER_TYPE
            ) {
                headers = mapOf("Referer" to extRef) // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )
    }

    data class SystemResponse(
        @JsonProperty("hls")         val hls: String,
        @JsonProperty("videoImage")  val videoImage: String? = null,
        @JsonProperty("videoSource") val videoSource: String,
        @JsonProperty("securedLink") val securedLink: String
    )
}