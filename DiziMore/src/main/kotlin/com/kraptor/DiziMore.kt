// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class DiziMore : MainAPI() {
    override var mainUrl = "https://dizimore.net"
    override var name = "DiziMore"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/diziler?tarih=2025" to "Yeni Diziler",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=aile" to "Aile",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=animasyon" to "Animasyon",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=belgesel" to "Belgesel",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=bilim-kurgu-fantazi" to "Bilim Kurgu Fantazi",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=cocuklar" to "Cocuklar",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=dram" to "Dram",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=gerceklik" to "Gerceklik",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=gizem" to "Gizem",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=komedi" to "Komedi",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=pembe-dizi" to "Pembe Dizi",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=romantik" to "Romantik",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=savas-politik" to "Savas Politik",
        "${mainUrl}/diziler?s_type=&tur%5B%5D=talk" to "Talk"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // request.data şu anda örneğin:
        // "https://dizimore.net/diziler?s_type=&tur%5B%5D=aksiyon-macera"
        val parts = request.data.split("?", limit = 2)
        val baseUrl = parts[0].trimEnd('/')            // https://dizimore.net/diziler
        val query = parts.getOrNull(1)?.let { "?$it" } ?: ""

        // sayfa 1 ise orijinal, değilse /page/2 gibi ekle
        val pagedUrl = if (page <= 1) {
            "$baseUrl$query"
        } else {
            "$baseUrl/page/$page$query"
        }

        val document = app.get(pagedUrl).document
        val home = document.select("div.poster.poster-md")

            .mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }


    override suspend fun search(query: String): List<SearchResponse> {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "$mainUrl/ajaxservice/index.php?qr=$encoded"

        val response = app.get(url)
        val jsonText = response.text
//        Log.d("dzmo", "RAW JSON response: $jsonText")

        // Use manual JSON parsing which is more reliable
        try {
            val jsonObj = org.json.JSONObject(jsonText)
            val success = jsonObj.optString("success", "")

            if (success == "1") {
                val data = jsonObj.optJSONObject("data")
                if (data != null) {
                    val resultArray = data.optJSONArray("result")
                    if (resultArray != null && resultArray.length() > 0) {
                        val searchResults = ArrayList<SearchResponse>()

                        for (i in 0 until resultArray.length()) {
                            val itemObj = resultArray.getJSONObject(i)
                            val sType = itemObj.optString("s_type", "")

                            // Only process series (type "0")
                            if (sType == "0") {
                                val name = itemObj.optString("s_name", "")
                                val link = itemObj.optString("s_link", "")
                                val image = itemObj.optString("s_image", "")

                                try {
                                    val response = newTvSeriesSearchResponse(name, link, TvType.TvSeries)
                                    response.posterUrl = image
                                    searchResults.add(response)
                                } catch (_: Exception) {
//                                    Log.e("dzmo", "Error creating response for $name", e)
                                }
                            }
                        }

                        return searchResults
                    }
                }
            }
        } catch (_: Exception) {
//            Log.e("dzmo", "JSON parsing failed: ${e.message}", e)
        }

        return emptyList()
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val rawTitle = document.selectFirst("h1.page-title")?.text()?.trim() ?: return null
        val title = rawTitle.replace(Regex("""\s*\(\d{4}\)$"""), "").trim()
        val poster = fixUrlNull(document.selectFirst("div.ui.items.ui img")?.attr("data-src"))
        val description = document.selectFirst("div.series-summary-wrapper p")?.text()?.trim()
        val yearText = document.selectFirst("div.genre-item:nth-child(2)")?.text()?.trim()
        val year = Regex("""\d{4}""").find(yearText ?: "")?.value?.toIntOrNull()
        val tags = document.select("div.genre-item:nth-child(1) > a").map { it.text() }
        val rating = document.selectFirst("div.color-imdb")?.text()?.trim()
        val duration =
            document.selectFirst("table.ui > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > div:nth-child(2)")
                ?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
//        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("div.content h5").map { Actor(it.text()) }
        val trailer = document.selectFirst("a.prettyPhoto")?.attr("href")
    ?.takeIf { it.contains("youtube.com/watch") }
    ?.replace("watch?v=", "embed/")

        val episodes = document.select("div.ajax_post").mapNotNull { bolumElemanlari ->
            val epNumber = bolumElemanlari.attr("data-epnumber").toIntOrNull()
            val epHref = fixUrlNull(bolumElemanlari.selectFirst("a")?.attr("href")) ?: return null
            val seasonName = bolumElemanlari.attr("season-name").toIntOrNull()
            newEpisode(epHref) {
                this.name = "bölüm"
                this.episode = epNumber
                this.season = seasonName
            }
        }



        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(rating)
            this.duration = duration
            this.episodes = episodes
//            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("dzmo", "data » ${data}")
        val document = app.get(data).document

        val iframelink = fixUrlNull(document.selectFirst("li.belink a")?.attr("data-frame")).toString()


        loadExtractor(iframelink, data, subtitleCallback, callback)
        return true
    }
}