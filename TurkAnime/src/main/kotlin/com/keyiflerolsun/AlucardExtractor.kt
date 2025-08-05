// ! Bu araç @kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
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

            val bakalimbak = app.get(url).text

//            Log.d("tralucard", "bakalimn $bakalimbak")


            val headercik = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
                "Origin" to "https://www.turkanime.co",
//                "Cookie" to "cf_clearance=; _ga=; _ga_X5VBMNE3D1=; _ga_2SSTLBYBZR=; PHPSESSID=;",
                "Csrf-Token" to "EqdGHqwZJvydjfbmuYsZeGvBxDxnQXeARRqUNbhRYnPEWqdDnYFEKVBaUPCAGTZA",
                "Content-Type" to "text/html",
                "Cookie" to "cf_clearance=ZP_ffFXOJ_D2FsD2G0iKAlFN1H7Kdo3qUEP7FuIMOqM-1748437466-1.2.1.1-It5_Hk511r0G05BTOdULBto9pFwuizcUVMxZcZkZG6cwYy8wCzkJBeS1B9Aa0vXvFuxLSWKTZzShhzyhesWsqnQ5EmKgXCdYFMwKWLgQEhL6CBjGcsNE9H6OJ20rwQhIanK8H74AWfS92o8Phnv6y0UnpHlJfp2ajfcLQQc7lxEMUb3LuE_aS_81k48.lQQQq5n3HzZ9VR9pJQGcDjCjUte2LcO4S8UaIxO2dfWiTpByPOoccy14eHZj8QiezD5Vol9nBjBJAXn.b.KvHige.ld4_u8_zs_iQqnrxE2ckUF0Id8dvMCanfbm3dk7rZ8CI.N8o2VUYHl_N8LUYSZ9aZ03jZXSoLvKIdgdGxbuS.s",
                "accept" to "*/*",
                "Connection" to "keep-alive",
                "sec-fetch-mode" to "cors",
                "sec-fetch-dest" to "empty",
                "Sec-Fetch-Site" to "cross-site",
                "DNT" to "1"
            )



                val extractorcuk =   newExtractorLink(
                       source = this.name + " Beta",
                       name   =   this.name + " Beta",
                       url   = url,
                       type  = ExtractorLinkType.M3U8
                    ) {
                        this.quality = getQualityFromName(quality)
                    this.headers = headercik
                    this.referer = "https://www.turkanime.co"
                    }

//            Log.d("tralucard", "extratorcuk $extractorcuk")
                callback.invoke(extractorcuk)
            }
        }
    }
