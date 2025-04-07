// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class SetPlay : ExtractorApi() {
    override val name            = "SetPlay"
    override val mainUrl         = "https://setplay.cfd"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef   = referer ?: ""
        val iSource  = app.get(url, referer=extRef).text

        val videoUrl    = Regex("""videoUrl":"([^",]+)""").find(iSource)?.groupValues?.get(1) ?: throw ErrorLoadingException("videoUrl not found")
        val videoServer = Regex("""videoServer":"([^",]+)""").find(iSource)?.groupValues?.get(1) ?: throw ErrorLoadingException("videoServer not found")
        val title       = Regex("""title":"([^",]+)""").find(iSource)?.groupValues?.get(1)?.split(".")?.last() ?: "Unknown"
        val m3uLink     = "${mainUrl}${videoUrl.replace("\\", "")}?s=${videoServer}"
        Log.d("set", "m3uLink » $m3uLink")

        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = "${this.name} - $title",
                url     = m3uLink,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to url) // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )
    }
}