// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.kraptor

import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

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
        Log.d("cehennem", "url » $url")

        val iSource = app.get(url, referer = extRef)

        iSource.document
            .select("track[kind=captions]")   // sadece altyazı içeren <track> etiketlerini al
            .forEach { element ->
                val src       = element.attr("src")
                val srclang   = element.attr("srclang")
                val labelAttr = element.attr("label")
                val lang = when (srclang) {
                    "tr" -> "Türkçe"
                    "en" -> "İngilizce"
                    "fr" -> "Fransızca"
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

        val isimalak = iSource.document.selectFirst("video")?.attr("poster")?.replace("https://closeload.com/img/","")
            ?.replace("jpg","mp4")

        val urlOlustur = "https://balancehls8.playmix.uno/hls/$isimalak/master.txt"

        Log.d("cehennem", "isimalak  = $isimalak")
        Log.d("cehennem", "urlolustur  = $urlOlustur")


          callback.invoke(
           newExtractorLink(
               source = this.name,
               name = this.name,
               url = urlOlustur,
               type = ExtractorLinkType.M3U8,
               {
                   this.referer = "${mainUrl}/"
                   this.quality = Qualities.Unknown.value
               }
           ))
        }
    }