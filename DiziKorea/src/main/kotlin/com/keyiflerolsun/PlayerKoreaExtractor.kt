// ! Bu araç @kraptor tarafından | @kekikanime için yazılmıştır.
package com.keyiflerolsun

import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.fixUrl
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.nio.charset.Charset

open class PlayerKorea : ExtractorApi() {
    override val name = "PlayerKorea"
    override val mainUrl = "https://playerkorea10.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: "https://dizikorea.pw/"
        val videoAnahtar = url.substringAfter("video/")
        val getVideo = "https://playerkorea10.xyz/player/index.php?data=$videoAnahtar&do=getVideo"
        val videoLinkAl = app.post(getVideo, data = mapOf("hash" to videoAnahtar, "r" to "") , headers = mapOf(
            "Accept" to "*/*",
//            "Accept-Encoding" to "gzip, deflate, br, zstd",
            "Accept-Language" to "en-US,en;q=0.5",
            "Cache-Control" to "no-cache",
            "Connection" to "keep-alive",
            "Content-Length" to "40",
            "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
            "DNT" to "1",
            "Host" to "playerkorea10.xyz",
            "Origin" to "https://playerkorea10.xyz",
            "Pragma" to "no-cache",
            "Priority" to "u=4",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "no-cors",
            "Sec-Fetch-Site" to "same-origin",
            "Sec-GPC" to "1",
            "TE" to "trailers",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
            "X-Requested-With" to "XMLHttpRequest"
        )).text


        Log.d("kraptor_${this.name}", "getVideo = $getVideo videoLinkAl » $videoLinkAl")

        val regex = Regex(pattern = "\"videoSource\":\"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE))

        val videoM3u8 = regex.find(videoLinkAl)?.groupValues[1]?.replace("\\","").toString()

        Log.d("kraptor_${this.name}", "videoM3u8 = $videoM3u8")

            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = videoM3u8,
                    type = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to extRef)
                    quality = getQualityFromName(Qualities.Unknown.value.toString())
                }
            )
        }
    }
