// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Base64
import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

private fun getm3uLink(data: String): String {
    val first  = Base64.decode(data,Base64.DEFAULT).reversedArray()
    val second = Base64.decode(first, Base64.DEFAULT)
    val result = second.toString(Charsets.UTF_8).split("|")[1]

    return result
}

open class CloseLoadFm : ExtractorApi() {
    override val name            = "CloseLoadFm"
    override val mainUrl         = "https://closeload.filmmakinesi.de"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef = referer ?: ""
        Log.d("mak", "url » $url")

        val iSource = app.get(url, referer = extRef)

        iSource.document.select("track").forEach {
            val lang = it.attr("label").let {
                when (it) {
                    "Turkish" -> "Turkish"
                    "English" -> "English"
                    "French"  -> "French"
                    else -> it
                }
            }
            subtitleCallback.invoke(
                SubtitleFile(
                    lang = lang,
                    url  = fixUrl(it.attr("src"))
                )
            )
        }

        val obfuscatedScript = iSource.document.select("script[type=text/javascript]")[1].data().trim()
        val rawScript        = getAndUnpack(obfuscatedScript)
        val regex = Regex("var player=this\\}\\);var(.*?);myPlayer\\.src")
        val matchResult = regex.find(rawScript)
        if (matchResult != null) {
            val extractedString = matchResult.groups[1]?.value?.trim()?.substringAfter("=\"")?.substringBefore("\"")
            val m3uLink = Base64.decode(extractedString, Base64.DEFAULT).toString(Charsets.UTF_8)
            Log.d("mak", "m3uLink » $m3uLink")

            callback.invoke(
                newExtractorLink(
                    source  = this.name,
                    name    = this.name,
                    url     = m3uLink,
                    type = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to mainUrl,
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Norton/124.0.0.0"
                        ) // "Referer" ayarı burada yapılabilir
                    quality = getQualityFromName(Qualities.Unknown.value.toString()) // Int değeri String'e dönüştürülüyor
                }
            )

        } else {
            println("No match found")
        }

    }
}
