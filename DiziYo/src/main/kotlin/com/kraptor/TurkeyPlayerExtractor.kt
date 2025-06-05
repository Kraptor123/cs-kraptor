// ! Bu araç @kraptor tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class TurkeyPlayer : ExtractorApi() {
    override val name = "TurkeyPlayer"
    override val mainUrl = "https://watch.turkeyplayer.com"
    override val requiresReferer = true


    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
        val videoReq = app.get(url, referer = extRef).text
        val m3uMatch = Regex("""\"file\":\"([^\"]+)\"""").find(videoReq)
        val rawM3u = m3uMatch?.groupValues?.get(1)?.replace("\\", "")
        val fixM3u = rawM3u?.replace("thumbnails.vtt", "master.txt")

        fixM3u?.contains("master.txt")?.let {
            if (!it) {
                val lang = when {
                    fixM3u.contains("tur", ignoreCase = true) -> "Türkçe"
                    fixM3u.contains("en", ignoreCase = true) -> "İngilizce"
                    else -> "Bilinmeyen"
                }
                subtitleCallback.invoke(SubtitleFile(lang, fixM3u.toString()))
            }

            Log.d("filmizlesene", "normalized m3u » $fixM3u")


            val dil = Regex("""title\":\"([^\"]*)\"""").find(videoReq)
                ?.groupValues?.get(1)
                ?: throw ErrorLoadingException("Dil bulunamadı")
            val lang = when {
                dil.contains("SUB", ignoreCase = true) -> "Altyazılı"
                dil.contains("DUB", ignoreCase = true) -> "Dublaj"
                else -> ""
            }
            callback.invoke(
                newExtractorLink(
                    source = "TurkeyPlayerxBet $lang",
                    name = "TurkeyPlayerxBet $lang",
                    url = fixM3u,
                    type = ExtractorLinkType.M3U8,
                    {
                        this.quality = Qualities.Unknown.value
                        this.referer = extRef
                        this.headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0"
                        )
                    }
                )
            )
        }
    }
}