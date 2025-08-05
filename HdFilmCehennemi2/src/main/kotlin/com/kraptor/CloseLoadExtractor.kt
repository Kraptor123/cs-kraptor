// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.kraptor

import android.util.Base64
import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import okio.ByteString.Companion.decodeBase64

private fun getm3uLink(data: String): String {
    val first  = Base64.decode(data,Base64.DEFAULT).reversedArray()
    val second = Base64.decode(first, Base64.DEFAULT)
    val result = second.toString(Charsets.UTF_8).split("|")[1]

    return result
}

open class CloseLoad : ExtractorApi() {
    override val name            = "CloseLoad"
    override val mainUrl         = "https://closeload.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef = referer ?: ""
        Log.d("dkral_closeload", "url » $url")


        val iSource = app.get(url, referer = extRef)
        Log.d("dkral_closeload", "kaynak = $iSource")

        val regex = Regex("""\|Player\|\s*(.*?)\s*\|""", RegexOption.IGNORE_CASE)
        val allMatches = regex.findAll(iSource.text).toList()

        val rawUrl = allMatches.getOrNull(1)?.groupValues?.get(1)

        val sonUrl = rawUrl?.decodeBase64()?.utf8()
        val link = sonUrl.toString()

        Log.d("dkral_closeload", "rawurl » $rawUrl")
        Log.d("dkral_closeload", "urlbak » $sonUrl")


        iSource.document
            .select("track[kind=captions]")   // sadece altyazı içeren <track> etiketlerini al
            .forEach { element ->
                val src       = element.attr("src")
                val srclang   = element.attr("srclang")
                val labelAttr = element.attr("label")
                val lang = when (srclang) {
                    "tr" -> "Turkish"
                    "en" -> "English"
                    "fr" -> "French"
                    else -> labelAttr.ifBlank { srclang }
                }
                val isDefault = element.hasAttr("default")

                subtitleCallback.invoke(
                    SubtitleFile(
                        lang       = lang,
                        url        = fixUrl(src),
                    )
                )
            }

          callback.invoke(
           newExtractorLink(
               source = this.name,
               name = this.name,
               url = link,
               type = ExtractorLinkType.M3U8,
               {
                   this.referer = "${mainUrl}/"
                   this.quality = Qualities.Unknown.value
               }
           ))
        }
    }