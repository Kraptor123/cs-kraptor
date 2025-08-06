// ! Bu araç @kraptor123 tarafından yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.app
import org.json.JSONObject
import org.jsoup.Jsoup

private val anizmHeaders = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    "Accept-Language" to "en-US,en;q=0.9",
    "Connection" to "keep-alive",
    "Referer" to "https://anizm.net/",
    "Sec-Fetch-Dest" to "iframe",
    "Sec-Fetch-Mode" to "navigate",
    "Sec-Fetch-Site" to "same-origin",
    "Host" to "anizm.net"
)

suspend fun getVideoUrls(data: String): Map<String, String> {
    val mainResponse = app.get(data, headers = anizmHeaders).text

    val doc = Jsoup.parse(mainResponse)

    val allPlayerLinks = mutableMapOf<String, String>()
    doc.select("div.fansubSecimKutucugu a[translator]").forEach { elem ->
        Log.d("kraptor_anizmskici", "elem = $elem")
        val translatorLink = elem.attr("translator")
        val translatorName = elem.selectFirst("div.title")?.text().orEmpty()
        var attempts = 0
        val maxAttempts = 3

        while (attempts < maxAttempts) {
            attempts++
            try {
                val jsonResponse = app.get(translatorLink, headers = anizmHeaders).text

                if (!jsonResponse.contains("Attention Required!")) {
                    val jsonData = JSONObject(jsonResponse)
                    val embedDoc = Jsoup.parse(jsonData.getString("data"))

                    embedDoc.select("a.videoPlayerButtons[video]").forEach { element ->
                        val name = element.select("span").text()
                        val videoUrl = element.attr("video").replace("/video/", "//player/")
                        // Aynı isimde birden fazla kaynak olabilir, bu yüzden unique key oluşturuyoruz
                        val uniqueKey = "$name, $translatorName"
                        if (!allPlayerLinks.containsKey(uniqueKey)) {
                            allPlayerLinks[uniqueKey] = videoUrl
                        }
                    }
                    break
                }
            } catch (e: Exception) {
                Log.d("kraptor_anizmskici", "Error in attempt $attempts for $translatorName: ${e.message}")
                // Hata durumunda da devam et, diğer çevirmenları dene
            }
        }
    }

    Log.d("kraptor_anizmskici", "Found ${allPlayerLinks.size} player links: ${allPlayerLinks.keys}")

    val realVideoUrls = mutableMapOf<String, String>()
    allPlayerLinks.forEach { (name, url) ->
        try {
            val redirectResponse = app.get(
                url,
                headers = mapOf("Referer" to "https://anizm.net/"),
                allowRedirects = false
            )

            // Headers tipinde geliyor, get() metodunu kullan
            val realUrl = redirectResponse.headers.get("Location") ?: url

            Log.d("kraptor_anizmskici", "Processing $name: $url -> $realUrl")

            if (realUrl.contains("myvi.ru") || realUrl.contains("supervideo.tv")) {
                Log.d("kraptor_anizmskici", "Skipping $name: contains blocked domain")
                return@forEach
            }

            if (realUrl.contains("player")) {
                val finalResponse = app.get(
                    realUrl,
                    headers = mapOf("Referer" to "https://anizm.net/"),
                    allowRedirects = false
                )

                val finalLocation = finalResponse.headers.get("Location") ?: realUrl

                Log.d("kraptor_anizmskici", "Final redirect for $name: $realUrl -> $finalLocation")
                realVideoUrls[name] = finalLocation
            } else {
                realVideoUrls[name] = realUrl
            }

        } catch (e: Exception) {
            Log.d("kraptor_anizmskici", "Error processing $name: ${e.message}")
            // Hata durumunda bile devam et, diğer kaynakları işlemeye devam et
        }
    }

    Log.d("kraptor_anizmskici", "Final realVideoUrls count: ${realVideoUrls.size}")
    Log.d("kraptor_anizmskici", "realVideoUrls = $realVideoUrls")
    return realVideoUrls
}