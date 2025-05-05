package com.kraptor

import android.util.Log
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

class HdBestVidExtractor : ExtractorApi() {
    override val name = "HdBestVd"
    override val mainUrl = "https://hdvid.tv"
    override val requiresReferer = true

    // Basit bir URL doğrulama helper fonksiyonu
    private fun isValidVideoUrl(url: String): Boolean {
        // Resim dosyalarını hariç tut
        val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".webp")
        if (imageExtensions.any { url.endsWith(it, ignoreCase = true) }) {
            return false
        }

        // Video URL'si olması muhtemel desenleri kontrol et
        return url.contains("/v.mp4") ||
                url.contains(".m3u8") ||
                (url.contains("/v/") && !url.contains("/i/"))
    }

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink> {
        val sources = mutableListOf<ExtractorLink>()

        try {
            // Sayfayı getir
            val response = app.get(
                url = url,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                    "Referer" to "https://hdbestvd.online/"
                )
            ).text

            Log.d("HdBestVd", "Got response length: ${response.length}")

            // Video URL'lerini almak için öncelikli yöntemler
            val videoUrlPatterns = listOf(
                // v.mp4 dosyaları için regex (loglardan görülen en başarılı pattern)
                Regex("""(https://s1\.hdvid\.tv/[a-zA-Z0-9]+/v\.mp4)"""),

                // Diğer muhtemel video kalıpları
                Regex("""(https://s1\.hdvid\.tv/[^"'\s]+/v[^"'\s]*\.mp4)"""),
                Regex("""(https://s1\.hdvid\.tv/[^"'\s]+\.(?:mp4|m3u8))"""),

                // JWPlayer video kalıbı
                Regex("""file["'\s]*:["'\s]*["']([^"']+\.(?:mp4|m3u8))["']""")
            )

            // Tüm regex kalıplarını dene
            for (pattern in videoUrlPatterns) {
                val matches = pattern.findAll(response)

                for (match in matches) {
                    val videoUrl = match.groupValues[1].replace("&amp;", "&")

                    if (isValidVideoUrl(videoUrl)) {
                        Log.d("HdBestVd", "Found specific video URL pattern: $videoUrl")

                        sources.add(
                            newExtractorLink(
                                source = name,
                                name = "$name - Direct",
                                url = videoUrl
                            ) {
                                this.referer = url
                                this.headers = mapOf(
                                    "Origin" to mainUrl
                                )
                                this.quality = Qualities.P1080.value
                                this.type = if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                            }
                        )

                        // İlk geçerli video kaynağını bulduktan sonra döngüden çık
                        return sources
                    }
                }
            }

            // Eğer hala kaynak bulunamadıysa, video tag'i içinde ara
            val videoTagRegex = Regex("""<video[^>]*src="([^"]+)"""")
            val videoTagMatch = videoTagRegex.find(response)

            if (videoTagMatch != null) {
                val rawVideoUrl = videoTagMatch.groupValues[1]
                val fullVideoUrl = when {
                    rawVideoUrl.startsWith("http") -> rawVideoUrl
                    rawVideoUrl.startsWith("//") -> "https:$rawVideoUrl"
                    else -> "$mainUrl/${rawVideoUrl.trimStart('/')}".replace("//", "/")
                }

                if (isValidVideoUrl(fullVideoUrl)) {
                    sources.add(
                        newExtractorLink(
                            source = name,
                            name = "$name - Video",
                            url = fullVideoUrl
                        ) {
                            this.referer = url
                            this.headers = mapOf("Origin" to mainUrl)
                            this.quality = Qualities.P1080.value
                            this.type = if (fullVideoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                        }
                    )
                    return sources
                }
            }

        } catch (e: Exception) {
            Log.e("HdBestVd", "Error extracting video: ${e.message}", e)
        }

        return sources
    }
}