// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities

open class PixelDrain : ExtractorApi() {
    override val name            = "PixelDrain"
    override val mainUrl         = "https://pixeldrain.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val pixelId      = Regex("""([^/]+)(?=\?download)""").find(url)?.groupValues?.get(1)
        val downloadLink = "${mainUrl}/api/file/${pixelId}?download"
        Log.d("Kekik_${this.name}", "downloadLink » $downloadLink")

        callback.invoke(
            ExtractorLink(
                source  = "pixeldrain - $pixelId",
                name    = "pixeldrain - $pixelId",
                url     = downloadLink,
                referer = "${mainUrl}/u/${pixelId}?download",
                quality = Qualities.Unknown.value,
                type    = INFER_TYPE
            )
        )
    }
}