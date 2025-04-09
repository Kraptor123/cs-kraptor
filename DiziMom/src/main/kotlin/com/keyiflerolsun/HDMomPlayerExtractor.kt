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

            // 1. replace("\\", "") KALDIRILDI!
            val encrypted = AesHelper.cryptoAESHandler(bePlayerData, bePlayerPass.toByteArray(), false)
                ?: throw ErrorLoadingException("failed to decrypt")

            // 2. Jackson'ın Unicode'u otomatik çözmesine izin ver
            val json = jacksonObjectMapper().readTree(encrypted)
            m3uLink = json["video_location"].asText()

            // 3. Altyazı etiketlerini manuel olarak düzenle (Opsiyonel)
            val subtitles = json["strSubtitles"]
            if (subtitles != null && subtitles.isArray) {
                for (sub in subtitles) {
                    val label = sub["label"]?.asText() ?: continue // Jackson otomatik çözecek: "Tu00fcrkçe" → "Türkçe"
                    val file = sub["file"]?.asText() ?: continue
                    val lang = sub["language"]?.asText()?.lowercase() ?: continue

                    val langCode = lang.split("_").first()
                    if (langCode !in listOf("tr", "tur", "en") || label.contains("Forced", true)) continue

                    // 4. Son kullanıcıya gösterilecek dil etiketini özelleştir
                    val displayLabel = when (langCode) {
                        "tr", "tur" -> "Türkçe Altyazı"
                        "en" -> "İngilizce Altyazı"
                        else -> label // Bu satıra asla gelmez (filtre yukarıda)
                    }

                    subtitleCallback.invoke(
                        SubtitleFile(
                            lang = displayLabel, // Özelleştirilmiş etiket
                            url = fixUrl(mainUrl + file)
                        )
                    )
                }
            }
        } else {

            m3uLink = Regex("""file:"([^"]+)""").find(iSource)?.groupValues?.get(1)

            val trackStr = Regex("""tracks:\[([^]]+)""").find(iSource)?.groupValues?.get(1)
            Log.d("Dizimom", "trackstr » $trackStr")
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
        Log.d("Dizimom", "subtitlecall » $subtitleCallback")

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