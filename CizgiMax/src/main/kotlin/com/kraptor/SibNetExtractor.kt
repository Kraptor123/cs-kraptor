// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.kraptor

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class SibNet : ExtractorApi() {
    override val name            = "SibNet"
    override val mainUrl         = "https://video.sibnet.ru"
    override val requiresReferer = true

    data class RequestData(
        @JsonProperty("url") val url: String,
        @JsonProperty("extra") val extra: String
    )

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef  = referer ?: ""
        val mapper = jacksonObjectMapper()

        val isJson = url.trim().startsWith("{") && url.trim().endsWith("}")
        val (realUrl, extraInfo) = if (isJson) {
            val requestData = mapper.readValue<RequestData>(url)
            requestData.url to requestData.extra
        } else {
            url to null
        }

        val iSource = app.get(realUrl, referer = extRef).text
        var m3uLink = Regex("""player.src\(\[\{src: "([^"]+)""").find(iSource)?.groupValues?.get(1)
            ?: throw ErrorLoadingException("m3u link not found")

        m3uLink = "$mainUrl$m3uLink"
        Log.d("Kekik_${this.name}", "m3uLink » $m3uLink")

        val finalName = listOfNotNull(this.name, extraInfo)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" + ") ?: this.name

        callback.invoke(
            newExtractorLink(
                source = finalName,
                name = finalName,
                url = m3uLink,
                type = INFER_TYPE
            ) {
                headers = mapOf("Referer" to realUrl)
                quality = Qualities.Unknown.value
            }
        )
    }
}

