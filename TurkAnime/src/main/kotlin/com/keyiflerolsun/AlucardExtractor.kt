// ! Bu araç @kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.actions.temp.VlcPackage
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
        Log.d("tralucard", "URL URL  $url")

        val separator = "#EXT-X-STREAM-INF"
        videoReq.substringAfter(separator).split(separator).map {
            val quality = it.substringAfter("RESOLUTION=")
                .substringAfter("x")
                .replace(",","")
                .substringBefore("FRAME") + "p"
            val videoUrl = it.substringAfter("\n")
                .substringBefore("\n")

                val extractorcuk =   newExtractorLink(
                       source = this.name + " Beta",
                       name   =   this.name + " Beta",
                       url   = videoUrl,
                       type  = ExtractorLinkType.M3U8
                    ) {
                        this.quality = getQualityFromName(quality)
                    }

            Log.d("tralucard", "extratorcuk $extractorcuk")
                callback.invoke(extractorcuk)
            }
        }
    }
