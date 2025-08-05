// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class SetPrime : ExtractorApi() {
    override val name            = "SetPrime"
    override val mainUrl         = "https://setplay.site"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val partKey  = url.substringAfter("?partKey=").substringAfter("turkce").uppercase()
        @Suppress("NAME_SHADOWING") val url      = url.substringBefore("?partKey=")
        val iSource  = app.post(url.replace("embed?i=", "embed/get?i="), referer=url).text

        val links = Regex("""Links":\["([^"\]]+)""").find(iSource)?.groupValues?.get(1) ?: throw ErrorLoadingException("Links not found")
        if (!links.startsWith("/")) {
            throw ErrorLoadingException("Links not valid")
        }

        val m3uLink = "${mainUrl}${links}"
        Log.d("setf", "m3uLink » $m3uLink")

        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = if (partKey != "") "${this.name} - $partKey" else this.name,
                url     = m3uLink,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to url) // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )
    }
}