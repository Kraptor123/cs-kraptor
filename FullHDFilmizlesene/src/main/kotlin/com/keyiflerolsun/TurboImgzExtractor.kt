// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class TurboImgz : ExtractorApi() {
    override val name            = "TurboImgz"
    override val mainUrl         = "https://turbo.imgz.me"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef   = referer ?: ""
        val videoReq = app.get(url.substringAfter("||"), referer=extRef).text

        val videoLink = Regex("""file: "(.*)",""").find(videoReq)?.groupValues?.get(1) ?: throw ErrorLoadingException("File not found")
        Log.d("Kekik_${this.name}", "videoLink » $videoLink")

        callback.invoke(
            newExtractorLink(
                source  = "${this.name} - " + url.substringBefore("||").uppercase(),
                name    = "${this.name} - " + url.substringBefore("||").uppercase(),
                url     = videoLink,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to extRef) // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
            )
    }
}