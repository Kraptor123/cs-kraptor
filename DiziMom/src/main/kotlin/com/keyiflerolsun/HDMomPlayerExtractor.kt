// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.helper.AesHelper
import com.lagradost.cloudstream3.utils.*

open class HDMomPlayer : ExtractorApi() {
    override val name            = "HDMomPlayer"
    override val mainUrl         = "https://hdmomplayer.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val m3uLink:String?
        val extRef  = referer ?: ""
        val iSource = app.get(url, referer=extRef).text

        val bePlayer = Regex("""bePlayer\('([^']+)',\s*'(\{[^}]+\})'\);""").find(iSource)?.groupValues
        if (bePlayer != null) {
            val bePlayerPass = bePlayer[1]
            val bePlayerData = bePlayer[2]
            val encrypted    = AesHelper.cryptoAESHandler(bePlayerData, bePlayerPass.toByteArray(), false)?.replace("\\", "") ?: throw ErrorLoadingException("failed to decrypt")
            Log.d("Kekik_${this.name}", "encrypted » $encrypted")

            m3uLink = Regex("""video_location":"([^"]+)""").find(encrypted)?.groupValues?.get(1)
        } else {
            m3uLink = Regex("""file:"([^"]+)""").find(iSource)?.groupValues?.get(1)

            val trackStr = Regex("""tracks:\[([^]]+)""").find(iSource)?.groupValues?.get(1)
            if (trackStr != null) {
                val tracks:List<Track> = jacksonObjectMapper().readValue("[${trackStr}]")

                for (track in tracks) {
                    if (track.file == null || track.label == null) continue
                    if (track.label.contains("Forced")) continue

                    subtitleCallback.invoke(
                        SubtitleFile(
                            lang = track.label,
                            url  = fixUrl(mainUrl + track.file)
                        )
                    )
                }
            }
        }

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = m3uLink ?: throw ErrorLoadingException("m3u link not found"),
                type = ExtractorLinkType.M3U8 // isM3u8 artık bu şekilde belirtiliyor
            ) {
                headers = mapOf("Referer" to url) // Eski "referer" artık headers içinde
                quality = Qualities.Unknown.value // Kalite ayarlandı
            }
        )
    }

    data class Track(
        @JsonProperty("file")     val file: String?,
        @JsonProperty("label")    val label: String?,
        @JsonProperty("kind")     val kind: String?,
        @JsonProperty("language") val language: String?,
        @JsonProperty("default")  val default: String?
    )
}