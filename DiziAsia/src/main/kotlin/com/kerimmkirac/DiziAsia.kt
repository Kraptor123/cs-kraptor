// ! Bu araç @kerimmkirac tarafından | @kerimmkirac için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class DiziAsia : MainAPI() {
    override var mainUrl              = "https://diziasia.com"
    override var name                 = "DiziAsia"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.AsianDrama, TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}" to "Yeni Bölümler",
        "${mainUrl}/diziler" to "Diziler",
        "${mainUrl}/filmler" to "Filmler",
        
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val document = app.get("${request.data}?page=$page").document
    val isMainPage = request.data == mainUrl 

    val mainRow = document.select("div.row.row-cols-xxl-6.row-cols-md-4.row-cols-2").first()
    val home = mainRow?.select("div.col-lg-2")?.mapNotNull { it.toMainPageResult(isMainPage) } ?: emptyList()

    return newHomePageResponse(request.name, home)
}


    private fun Element.toMainPageResult(isMainPage: Boolean = false): SearchResponse? {
        val title = this.selectFirst("h3.title")?.text()?.trim() ?: return null
        val subtitle = this.selectFirst("h4.title_sub")?.text()?.trim() ?: ""
        
       
        val fullTitle = if (isMainPage && subtitle.isNotEmpty()) "$title - $subtitle" else title
        
        val originalHref = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val href = cleanSeriesUrl(originalHref)
        
        val posterUrl = fixUrlNull(
            this.selectFirst("picture source")?.attr("data-srcset")
                ?: this.selectFirst("img")?.attr("src")
        )
        
        return newMovieSearchResponse(fullTitle, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }
    
    private fun cleanSeriesUrl(url: String): String {
        val cleanedUrl = url.replace(Regex("""-\d+-sezon-\d+-bolum/?$"""), "")
        return cleanedUrl
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/${query}").document
        val results = document.select("div.col-lg-2").mapNotNull { it.toSearchResult() }
        return results
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3.title")?.text()?.trim() ?: return null
        
        
        val originalHref = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val href = cleanSeriesUrl(originalHref)
        
        val posterUrl = fixUrlNull(
            this.selectFirst("picture source")?.attr("data-srcset")
                ?: this.selectFirst("img")?.attr("src")
        )
        
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val isSeries = "/dizi/" in url

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val originalTitle = document.selectFirst("h2")?.text()?.trim()
        val poster = fixUrlNull(
            document.selectFirst("div.col-md-auto img")?.attr("data-src")
                ?: document.selectFirst("div.col-md-auto img")?.attr("src")
        )
        val description = document.select("p.fs-sm.text-muted").getOrNull(1)?.text()?.trim()
        val recommendations = document.select("div.col-lg-2").mapNotNull { it.toRecommendationResult() }

        val year = document.select("ul.list-inline li")
            .find { it.text().contains(Regex("\\d{4}")) }
            ?.text()?.toIntOrNull()

        val tags = document.select("div.card-tag p, div.card-tags a")
            .flatMap { it.text().split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return if (isSeries) {
            val episodes = document.select("div.card-episode").mapIndexedNotNull { index, ep ->
                val name = ep.selectFirst("a.episode")?.text()?.trim() ?: "Bölüm ${index + 1}"
                val link = fixUrl(ep.selectFirst("a.episode")?.attr("href") ?: return@mapIndexedNotNull null)
                newEpisode(link, {
                    this.name   = name
                    this.season = 1
                })
            }

            newTvSeriesLoadResponse(title, url, TvType.AsianDrama, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.recommendations = recommendations
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("h3.title")?.text()?.trim() ?: return null
        
        
        val originalHref = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val href = cleanSeriesUrl(originalHref)
        
        val posterUrl = fixUrlNull(
            this.selectFirst("picture source")?.attr("data-srcset")
                ?: this.selectFirst("img")?.attr("src")
        )
        
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        try {
            val document = app.get(data).document
            val sourceButtons = document.select("button.btn-stream")

            if (sourceButtons.isEmpty()) {
                return false
            }

            var successCount = 0

            for (btn in sourceButtons) {
                val id = btn.attr("data-id")?.takeIf { it.isNotBlank() } ?: continue

                try {
                    val response = app.post(
                        url = "https://diziasia.com/ajax/embed",
                        data = mapOf("id" to id),
                        headers = mapOf(
                            "X-Requested-With" to "XMLHttpRequest",
                            "Referer" to data,
                            "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8"
                        )
                    )

                    val responseDoc = response.document
                    val iframe = responseDoc.selectFirst("iframe")?.let {
                        val src = it.attr("src").ifBlank { it.attr("data-src") }
                        src
                    }

                    if (iframe.isNullOrBlank()) continue

                    if (iframe.contains("vidmoly.to")) {
                        val vidmolyResult = extractVidmoly(iframe, callback)
                        if (vidmolyResult) successCount++
                    } else {
                        val extractorResult = loadExtractor(iframe, "https://diziasia.com", subtitleCallback, callback)
                        if (extractorResult) successCount++
                    }

                } catch (e: Exception) {
                    continue
                }
            }

            return successCount > 0

        } catch (e: Exception) {
            return false
        }
    }

    private suspend fun extractVidmoly(url: String, callback: (ExtractorLink) -> Unit): Boolean {
        return try {
            val headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36",
                "Sec-Fetch-Dest" to "iframe",
                "Referer" to "https://vidmoly.to/"
            )
           
            val iSource = app.get(url, headers = headers, referer = "$mainUrl/").text
            val matches = Regex("""file\s*:\s*"([^"]+\.m3u8[^"]*)"""").findAll(iSource).toList()
           
            if (matches.isEmpty()) {
                return false
            }
            
            matches.forEach { match ->
                val m3u8Url = match.groupValues[1]
                
                callback(
                    newExtractorLink(
                        source = "Vidmoly",
                        name = "Vidmoly",
                        url = m3u8Url,
                        type = ExtractorLinkType.M3U8
                    ){
                        this.referer = url
                        this.quality = Qualities.Unknown.value
                    }
                )
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
}