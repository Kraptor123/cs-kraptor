// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
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
//        Log.d("kraptor_${this.name}", "url » $url")

        val videoSite = app.get(url, referer = extRef).text

        val subRegex = Regex(pattern = "playerjsSubtitle = \"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE))

        val subMatch = subRegex.find(videoSite)?.groupValues[1].toString()

        val keywords = listOf("tur", "tr", "türkçe", "turkce")
        val language = if (keywords.any { subMatch.substringBefore("]").substringAfter("[").contains(it, ignoreCase = true) }) {
            "Turkish"
        } else {
            subMatch.substringBefore("]").substringAfter("[")
        }

        subtitleCallback.invoke(
            SubtitleFile(
                lang = language,
                url = fixUrl(subMatch.substringAfter("]"))
            )
        )

        val vidId   = if (url.contains("video/")) {
            url.substringAfter("video/")
        } else {
            url.substringAfter("?data=")
        }
        val postUrl = "${mainUrl}/player/index.php?data=${vidId}&do=getVideo"

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

//        Log.d("kraptor_${this.name}", "response » $response")

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