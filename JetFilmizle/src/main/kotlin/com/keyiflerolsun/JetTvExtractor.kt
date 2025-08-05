// ! Bu araç @kraptor tarafından | @kekikanime için yazılmıştır.
package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.*
import org.json.JSONObject

open class JetTv : ExtractorApi() {
    override val name            = "JetTv"
    override val mainUrl         = "https://jetv.xyz"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url).text
        // Varsayılan olarak sayfa içinden file linkine bakıyoruz
        val regex = Regex(pattern = "file: '([^']*)'", options = setOf(RegexOption.IGNORE_CASE))
        val link = regex.find(document)?.groups[1]?.value.orEmpty()
        Log.d("kraptor_${this.name}", "m3u8 » $link")

        val vidId = url.substringAfter("id=")
        val vidIdGetResponse = app.get("https://jetv.xyz/apollo/get_video.php?id=$vidId", referer = url).text
        Log.d("kraptor_$this.name}", "vidIdget » $vidIdGetResponse")

        try {
            val json = JSONObject(vidIdGetResponse)
            val success = json.optBoolean("success", false)
            if (success) {
                val masterUrl = json.optString("masterUrl")
                val referrerUrl = json.optString("referrerUrl")
                // Eğer masterUrl mevcutsa, onu kullanarak bağlantı oluşturuyoruz
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = name,
                        url = masterUrl,
                        type = ExtractorLinkType.M3U8
                    ) {
                        headers = mapOf("Referer" to referrerUrl)
                        quality = getQualityFromName(Qualities.Unknown.value.toString())
                    }
                )
                return
            }
        } catch (e: Exception) {
            Log.e("kraptor_${this.name}", "JSON parse error $e")
        }

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = link,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to "${mainUrl}/")
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )
    }
}