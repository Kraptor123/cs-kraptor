// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.StringUtils.decodeUri

open class Drive : ExtractorApi() {
    override var name            = "Drive"
    override var mainUrl         = "https://drive.google.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef  = referer ?: ""

        val docId        = Regex("""file/d/(.*)/preview""").find(url)?.groupValues?.get(1) ?: throw IllegalArgumentException("docId not found")
        val getVideoLink = "https://drive.google.com/get_video_info?docid=${docId}&drive_originator_app=303"
        val iSource      = app.get(getVideoLink, referer=extRef).text

        val bakalim = Regex("""&fmt_stream_map=(.*)&url_encoded_fmt_stream_map""").find(iSource)?.groupValues?.get(1) ?: throw IllegalArgumentException("fmt_stream_map not found")
        val decoded = bakalim.decodeUri()
        val m3uLink = decoded.split("|").last()
        Log.d("Kekik_${this.name}", "m3uLink » $m3uLink")

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = m3uLink,
                type = INFER_TYPE
            ) {
                headers = mapOf("Referer" to url)
                quality = Qualities.Unknown.value
            }
        )
    }
}