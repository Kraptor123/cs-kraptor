// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import android.util.Base64
import com.lagradost.api.Log
import java.nio.charset.Charset

open class PlayerFilmIzle : ExtractorApi() {
    override val name = "PlayerFilmIzle"
    override val mainUrl = "https://player.filmizle.in"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = mainUrl
        val videoReq = app.get(url, referer=extRef).text

        val regex = Regex(pattern = """FirePlayer\|([^|]+)\|""", options = setOf(RegexOption.IGNORE_CASE))

        val regexSub = Regex(pattern = "playerjsSubtitle = \"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE))

        val subYakala = regexSub.find(videoReq)?.groupValues?.get(1).toString()

        val subUrl    = subYakala.substringAfter("]")

        val subLang   = subYakala.substringBefore("]").removePrefix("[")

        Log.d("kraptor_$name","suburl = $subUrl ve sublang = $subLang")

        subtitleCallback(
            SubtitleFile(
                url = subUrl,
                lang = subLang
                )
            )

        val data = regex.find(videoReq)?.groupValues?.get(1)

        Log.d("kraptor_$name","data = $data")

        val urlPost = "https://player.filmizle.in/player/index.php?data=$data&do=getVideo"

        val getUrl  = app.post(urlPost, referer = extRef, headers = mapOf("X-Requested-With" to "XMLHttpRequest") , data = mapOf("hash" to "$data", "r" to "")).text.replace("\\","")

        Log.d("kraptor_$name","geturl = $getUrl")

        val urlYakala = Regex(pattern = """"securedLink":"([^"]*)"""", options = setOf(RegexOption.IGNORE_CASE))

        val m3u8 = urlYakala.find(getUrl)?.groupValues?.get(1).toString()

        Log.d("kraptor_$name","m3u8 = $m3u8")


        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = m3u8,
                type = ExtractorLinkType.M3U8
            ) {
                quality = Qualities.Unknown.value
                headers = mapOf("Referer" to extRef)
            }
        )
    }
}