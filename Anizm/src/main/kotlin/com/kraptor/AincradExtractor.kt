// ! Bu araç @kraptor123 tarafından yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URI

open class AincradExtractor : ExtractorApi() {
    override val name        = "Aincrad"
    override val mainUrl     = "https://anizmplayer.com"
    override val requiresReferer = true


    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink> {
        val fansub = if (url.contains("|")) {
            url.substringAfter("|").replace("(Reklamsız)", "").replace(",","").trim()
        } else {
            name
        }

        val url  = if (url.contains("|")) {
            url.substringBefore("|")
        }else {
            url
        }
        Log.d("kraptor_aincrad", "gelenUrl = $url")
        Log.d("kraptor_aincrad", "fansub = $fansub")


        val hash = URI(url).path.substringAfterLast("/")
        val postUrl = "$mainUrl/player/index.php?data=$hash&do=getVideo"

//        Log.d("kraptor_aincrad", "postUrl = $postUrl")
        
        val response = app.post(
            postUrl,
            data = mapOf(
                "hash" to hash,
                "r" to "https://anizm.net/"
            ),
            headers = mapOf(
                "Origin" to mainUrl,
                "X-Requested-With" to "XMLHttpRequest"
            )
        ).parsedSafe<AincradResponse>()
//        Log.d("kraptor_aincrad", "response = $response")

        return response?.securedLink?.let { hlsUrl ->
            listOf(
                newExtractorLink(
                    source = fansub,
                    name = fansub,
                    url = hlsUrl,
                    type = INFER_TYPE
                ) {
                    headers = mapOf("Referer" to "$mainUrl/") // "Referer" ayarı burada yapılabilir
                    quality = Qualities.P1080.value
                }
                )
        } ?: emptyList()  // Return an empty list if response?.securedLink is null
    }

    private data class AincradResponse(
        @JsonProperty("securedLink") val securedLink: String?
    )
}
