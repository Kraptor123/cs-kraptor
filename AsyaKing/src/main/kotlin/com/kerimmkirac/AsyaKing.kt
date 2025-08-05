// ! Bu araç @kerimmkirac tarafından | @kerimmkirac için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class AsyaKing : MainAPI() {
    override var mainUrl              = "https://www.asyaking.com"
    override var name                 = "AsyaKing"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.AsianDrama)

    override val mainPage = mainPageOf(
        "${mainUrl}/tum-bolumler/"   to "Son Bölümler",
        "${mainUrl}/dizi-arsivi"      to "Diziler"
        
        
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        
        val home = if (request.data.contains("tum-bolumler")) {
           
            document.select("div.episode-box").mapNotNull { it.toLatestEpisodeResult() }
        } else {
            
            document.select("div.single-item").mapNotNull { it.toMainPageResult() }
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.categorytitle > a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("div.cat-img a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = posterUrl
        }
    }

    private suspend fun Element.toLatestEpisodeResult(): SearchResponse? {
        val seriesName = this.selectFirst("div.episode-title div.serie-name a")?.text() ?: return null
        val episodeName = this.selectFirst("div.episode-title div.episode-name a")?.text() ?: ""
        val episodeHref = fixUrlNull(this.selectFirst("div.poster div.img a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.poster div.img a img")?.attr("data-src"))
        
       
        val fullEpisodeTitle = if (episodeName.isNotEmpty()) {
            "$seriesName $episodeName"
        } else {
            seriesName
        }
        
        return try {
           
            val episodeDocument = app.get(episodeHref).document
            val mainSeriesLink = episodeDocument.selectFirst("div#benzerli a")?.attr("href")
            
            if (!mainSeriesLink.isNullOrEmpty()) {
                val mainSeriesHref = fixUrlNull(mainSeriesLink) ?: return null
                
                newMovieSearchResponse(fullEpisodeTitle, mainSeriesHref, TvType.AsianDrama) {
                    this.posterUrl = posterUrl
                }
            } else {
                
                newMovieSearchResponse(fullEpisodeTitle, episodeHref, TvType.AsianDrama) {
                    this.posterUrl = posterUrl
                }
            }
        } catch (e: Exception) {
            
            
            newMovieSearchResponse(fullEpisodeTitle, episodeHref, TvType.AsianDrama) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.single-item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.categorytitle > a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("div.cat-img a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val rawTitle = document.selectFirst("h1")?.text()?.trim() ?: return null
        val title = rawTitle.substringBefore("Türkçe").replace(Regex("""\(\d{4}\)"""), "").trim()

        val poster = fixUrlNull(document.selectFirst("div.category_image img")?.attr("data-src"))
        val description = document.selectFirst("div.category_desc")?.text()?.trim()

        val imdb = document.select("div#icerikcat2 > div:nth-child(3)").text().trim()
        val rating = imdb.toDoubleOrNull()?.times(10)?.toInt()

        val yearText = document.select("div#icerikcat2 > div:nth-child(1)").text()
        val year = Regex("""\d{4}""").find(yearText)?.value?.toIntOrNull()

        val tags = document.select("div#icerikcat2 .genres a").map { it.text() }

        val episodes = document.select("div.bolumust").mapNotNull { ep ->
            val link = fixUrlNull(ep.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val name = ep.selectFirst("div.baslik")?.text()?.trim() ?: return@mapNotNull null
            newEpisode(link, {this.name = name})
        }

        return newTvSeriesLoadResponse(title, url, TvType.AsianDrama, episodes) {
            this.posterUrl = poster
            this.plot = description
            this.score = Score.from10(rating)
            this.year = year
            this.tags = tags
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
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

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        
        
        val document = app.get(data).document
        
        
        val allSources = mutableListOf<String>()
        
        
        allSources.add(data)
        
       
        val sourcesDiv = document.selectFirst("div.sources")
        sourcesDiv?.select("a.post-page-numbers")?.forEach { sourceLink ->
            val sourceUrl = fixUrlNull(sourceLink.attr("href"))
            if (!sourceUrl.isNullOrEmpty()) {
                allSources.add(sourceUrl)
            }
        }
        
       
        for (sourceUrl in allSources) {
            try {
                val sourceDocument = app.get(sourceUrl).document
                var iframe = sourceDocument.selectFirst("iframe.metaframe")?.attr("data-src")
                    ?: sourceDocument.selectFirst("iframe")?.attr("data-src")
                    ?: sourceDocument.selectFirst("iframe")?.attr("src")
                
                if (!iframe.isNullOrEmpty()) {
                    
                    if (iframe.startsWith("//")) {
                        iframe = "https:$iframe"
                    }
                    
                    Log.d("AsyaKing", "Found iframe: $iframe from $sourceUrl")
                    
                    
                    if (iframe.contains("vidmoly.to")) {
                        extractVidmoly(iframe, callback)
                    } else {
                        
                        loadExtractor(iframe, "$mainUrl/", subtitleCallback, callback)
                    }
                }
            } catch (e: Exception) {
               
            }
        }
        
        return true
    }
}