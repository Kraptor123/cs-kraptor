// ! Bu araç @kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.M3u8Helper2.m3u8Generation
import kotlin.collections.get

open class AlucardExtractor : ExtractorApi() {
    override val name = "Alucard"
    override val mainUrl = "https://alucard.stream"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
        val videoReq = app.get(url, referer = extRef).text
        Log.d("tralucard", "raw playlist:\n$videoReq")

        val separator = "#EXT-X-STREAM-INF"
        videoReq.substringAfter(separator).split(separator).map {
            val quality = it.substringAfter("RESOLUTION=")
                .substringAfter("x")
                .replace(",","")
                .substringBefore("FRAME") + "p"
            val videoUrl = it.substringAfter("\n")
                .substringBefore("\n")

            val headercik = mapOf(
                "Host" to "alucard.stream",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.5",
//                "Accept-Encoding" to "gzip, deflate, br, zstd",
//                "Origin" to url,
//                "Connection" to "keep-alive",
//                "Referer" to url,
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "cross-site",
                "cache-control" to "no-cache",
                "pragma" to "no-cache",
                "priority" to "u=1, i",
                "sec-ch-ua" to """"Google Chrome";v="137", "Chromium";v="137", "Not/A)Brand";v="24"""",
                "sec-ch-ua-mobile" to "?0",
                "sec-ch-ua-platform" to """"Windows"""",
                "sec-fetch-storage-access" to "active"
            )

                val extractorcuk =   newExtractorLink(
                       source = this.name + "Beta",
                       name   =   this.name + "Beta",
                       url   = videoUrl,
                       type  = ExtractorLinkType.M3U8
                    ) {
                        this.headers = headercik
                        this.quality = getQualityFromName(quality)
                    }
            Log.d("tralucard", "extratorcuk $extractorcuk")
                callback.invoke(extractorcuk)
            }
        }
    }