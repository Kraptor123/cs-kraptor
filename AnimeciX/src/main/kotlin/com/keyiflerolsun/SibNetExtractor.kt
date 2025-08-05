// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class SibNet : ExtractorApi() {
    override val name = "SibNet"
    override val mainUrl = "https://video.sibnet.ru"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
        if (url.contains("|")) {
            val videolink = url.substringBefore("|")
            val cevirmen = url.substringAfter("|")
            val iSource = app.get(videolink, referer = extRef).text
            var m3uLink = Regex("""player.src\(\[\{src: "([^"]+)""").find(iSource)?.groupValues?.get(1)
                ?: throw ErrorLoadingException("m3u link not found")

            m3uLink = "$mainUrl$m3uLink"
            Log.d("Kekik_${this.name}", "m3uLink » $m3uLink")

            val finalName = listOfNotNull(this.name, cevirmen)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" + ") ?: this.name

            callback.invoke(
                newExtractorLink(
                    source = finalName,
                    name = finalName,
                    url = m3uLink,
                    type = INFER_TYPE
                ) {
                    headers = mapOf("Referer" to videolink)
                    quality = Qualities.Unknown.value
                }
            )
        } else {


            val iSource = app.get(url, referer = extRef).text
            var m3uLink = Regex("""player.src\(\[\{src: "([^"]+)""").find(iSource)?.groupValues?.get(1)
                ?: throw ErrorLoadingException("m3u link not found")

            m3uLink = "$mainUrl$m3uLink"
            Log.d("Kekik_${this.name}", "m3uLink » $m3uLink")


            callback.invoke(
                newExtractorLink(
                    source = "Sibnet",
                    name = "Sibnet",
                    url = m3uLink,
                    type = INFER_TYPE
                ) {
                    headers = mapOf("Referer" to url)
                    quality = Qualities.Unknown.value
                }
            )
        }
    }
}

