package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink


class VidMolyTo : VidMolyExtractor() {
    override var name    = "VidMoly"
    override var mainUrl = "https://vidmoly.to"
}

open class VidMolyExtractor : ExtractorApi() {
    override val name            = "VidMoly"
    override val mainUrl         = "https://vidmoly.net"
    override val requiresReferer = true
    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {

        val url = if (url.contains("https://vidmoly.to")){
            url.replace("vidmoly.to","vidmoly.net")
        }else{
            url
        }

        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36",
            "Sec-Fetch-Dest" to "iframe",
            "Referer" to "${mainUrl}/"
        )
        Log.d("kraptor_$name", "Vidmoly URL'si işleniyor: $url")
        val iSource = app.get(url, headers = headers, referer = "$mainUrl/").text
        Log.d("kraptor_$name", "Vidmoly iframe içeriği alındı, m3u8 aranıyor...")
        val matches = Regex("""file\s*:\s*"([^"]+\.m3u8[^"]*)"""").findAll(iSource).toList()
        if (matches.isEmpty()) {
            Log.w("kraptor_$name", "Vidmoly'de m3u8 link bulunamadı")
            return
        }
        Log.d("kraptor_$name", "Vidmoly'de ${matches.size} adet m3u8 bulundu")
        matches.forEachIndexed { index, match ->
            val m3uLink = match.groupValues[1]
            Log.d("kraptor_$name", "Vidmoly m3uLink[$index] → $m3uLink")
            callback(
                newExtractorLink(
                    source = "VidMoly",
                    name = "VidMoly",
                    url = m3uLink,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "https://vidmoly.to/"
                    this.quality = Qualities.Unknown.value
                    this.headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                    )
                }
            )
        }
    }
}