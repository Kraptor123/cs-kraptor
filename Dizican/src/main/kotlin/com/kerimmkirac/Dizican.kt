// ! Bu araç @kerimmkirac tarafından | @kerimmkirac için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Dizican : MainAPI() {
    override var mainUrl              = "https://dizican.tv"
    override var name                 = "Dizican"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
   override val supportedTypes = setOf(TvType.AsianDrama, TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}"      to "Yeni Bölümler",
        "${mainUrl}/dizi-arsivi"      to "Tüm Diziler",
        "${mainUrl}/film-arsivi" to "Tüm Filmler",
        "${mainUrl}/dizi-kategori/guney-kore-dizileri-izle"   to "Kore Dizileri",
        
        "${mainUrl}/dizi-kategori/cin-dizileri-izle"  to "Çin Dizileri",
        "${mainUrl}/dizi-kategori/tayland-dizileri-izle"  to "Tayland Dizileri",
        "${mainUrl}/dizi-kategori/japon-dizileri-izle"  to "Japon Dizisi",
    )

   override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val document = app.get("${request.data}/page/$page").document
    
    val home = if (request.name == "Yeni Bölümler") {
        
        document.select("div.ep-box").mapNotNull { it.toEpisodeMainPageResult() }
    } else {
        
        document.select("div.movie-box").mapNotNull { it.toMainPageResult() }
    }

    return newHomePageResponse(request.name, home)
}

private fun Element.toEpisodeMainPageResult(): SearchResponse? {
    val episodeLink = this.selectFirst("a")?.attr("href") ?: return null
    
    
    val seriesUrl = convertEpisodeUrlToSeriesUrl(episodeLink)
    
    val seriesTitle = this.selectFirst("span.serietitle")?.text() ?: return null
    val episodeInfo = this.selectFirst("span.episodetitle")?.text() ?: ""
    
    val title = "$seriesTitle - $episodeInfo"
    val posterUrl = fixUrlNull(this.selectFirst("div.img img")?.attr("data-src"))
    
    return newMovieSearchResponse(title, seriesUrl, TvType.AsianDrama) { 
        this.posterUrl = posterUrl
    }
}

private fun convertEpisodeUrlToSeriesUrl(episodeUrl: String): String {
    // https://dizican.tv/bolum/justifiable-defense-7-bolum/ -> https://dizican.tv/dizi/justifiable-defense/
    // https://dizican.tv/bolum/flourished-peony-2-sezon-14-bolum/ -> https://dizican.tv/dizi/flourished-peony/
    
    val regex = """/bolum/(.+?)-(?:\d+-sezon-)?(?:\d+)-bolum/?""".toRegex()
    val match = regex.find(episodeUrl)
    
    return if (match != null) {
        val seriesSlug = match.groupValues[1]
        "$mainUrl/dizi/$seriesSlug/"
    } else {
        episodeUrl 
    }
}

private fun Element.toMainPageResult(): SearchResponse? {
   
    val title = this.selectFirst("div.name a")?.text() ?: return null
    
    
    val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
    
    
    val posterUrl = fixUrlNull(this.selectFirst("div.img img")?.attr("data-src"))
    
   
    
    
    return newMovieSearchResponse(title, href, TvType.AsianDrama) { 
        this.posterUrl = posterUrl
       
        
    }
}

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.movie-box").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
    val title = this.selectFirst("div.name a")?.text() ?: return null
    val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("div.img img")?.attr("src"))

     return newMovieSearchResponse(title, href, TvType.AsianDrama, initializer = { this.posterUrl = posterUrl })
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document
    
    
    val isDizi = url.contains("/dizi/")
    
    if (isDizi) {
        
        val title = document.selectFirst("h1.film")?.text() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.poster img")?.attr("data-src"))
        val description = document.selectFirst("div.description")?.text()
        val year = document.selectFirst("li.release span a")?.text()?.toIntOrNull()
        val tags = document.select("div.category a").map { it.text() }
        val status = if (document.selectFirst("span.final") != null) {
            ShowStatus.Completed
        } else {
            ShowStatus.Ongoing
        }
        val episodes = mutableListOf<Episode>()
        document.select("div.s-wrap").forEach { seasonDiv ->
            val seasonId = seasonDiv.attr("id")
            val seasonNumber = seasonId.replace("s-", "").toIntOrNull() ?: 1
            seasonDiv.select("div.ep-box").forEach { episodeDiv ->
                val episodeUrl = episodeDiv.selectFirst("a")?.attr("href")
                val episodeTitle = episodeDiv.selectFirst("div.name a")?.attr("title")
                val episodePoster = fixUrlNull(episodeDiv.selectFirst("div.img img")?.attr("data-src"))
                val episodeDate = episodeDiv.selectFirst("div.date span")?.text()
                val episodeNumber = episodeTitle?.let { title ->
                    val regex = """(\d+)\.\s*Bölüm""".toRegex()
                    regex.find(title)?.groupValues?.get(1)?.toIntOrNull()
                } ?: 1
                
                if (episodeUrl != null && episodeTitle != null) {
                    episodes.add(
                        newEpisode(
                            url = episodeUrl,
                            {
                                name = episodeTitle
                                season = seasonNumber
                                episode = episodeNumber
                                posterUrl = episodePoster
                                this.description = episodeDate
                            }
                        )
                    )
                }
            }
        }
        
        return newTvSeriesLoadResponse(
            title,
            url,
            TvType.AsianDrama,
            episodes
        ) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.showStatus = status
        }
        
    } else {
        
        val title = document.selectFirst("div.film h1")?.text() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.poster img")?.attr("data-src"))
        val description = document.selectFirst("div.description")?.text()
        
        
        val year = document.selectFirst("li.release span a")?.text()?.toIntOrNull()
        
        
        val tags = document.select("div.category a").map { it.text() }
        
        
        val rating = document.selectFirst("div.imdb-count")?.text()?.split(" ")?.get(0)
        
        
        val director = document.selectFirst("div.director a")?.text()
        
        
        val actors = document.select("div.actors a").map { it.text() }
        
        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            url 
        ) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.score = Score.from10(rating)
            
            
            
            director?.let { 
                this.recommendations = listOf() 
            }
            
            if (actors.isNotEmpty()) {
                addActors(actors)
            }
        }
    }
}

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
    Log.d("DiziCan", "data » ${data}")
    val document = app.get(data).document

   
    val videoContainers = document.select("div.video-content iframe, iframe[src*='ok.ru'], iframe[src*='videoembed']")
    
    videoContainers.forEach { iframe ->
        val iframeUrl = iframe.attr("src")
        
        when {
            
            iframeUrl.contains("ok.ru") -> {
                val fullUrl = if (iframeUrl.startsWith("//")) {
                    "https:$iframeUrl"
                } else if (iframeUrl.startsWith("/")) {
                    "https://ok.ru$iframeUrl"
                } else {
                    iframeUrl
                }
                
                Log.d("DiziCan", "OK.ru iframe found: $fullUrl")
                loadExtractor(fullUrl, "$mainUrl/", subtitleCallback, callback)
            }
            
            
            iframeUrl.contains("vk.com") -> {
                val fullUrl = if (iframeUrl.startsWith("//")) {
                    "https:$iframeUrl"
                } else {
                    iframeUrl
                }
                loadExtractor(fullUrl, "$mainUrl/", subtitleCallback, callback)
            }
            
            
            iframeUrl.contains("dailymotion") -> {
                val fullUrl = if (iframeUrl.startsWith("//")) {
                    "https:$iframeUrl"
                } else {
                    iframeUrl
                }
                loadExtractor(fullUrl, "$mainUrl/", subtitleCallback, callback)
            }

            iframeUrl.contains("vidmoly") -> {
                val fullUrl = if (iframeUrl.startsWith("//")) {
                    "https:$iframeUrl"
                } else {
                    iframeUrl
                }
                loadExtractor(fullUrl, "$mainUrl/", subtitleCallback, callback)
            }
            
            
            iframeUrl.contains("youtube.com") || iframeUrl.contains("youtu.be") -> {
                val fullUrl = if (iframeUrl.startsWith("//")) {
                    "https:$iframeUrl"
                } else {
                    iframeUrl
                }
                loadExtractor(fullUrl, "$mainUrl/", subtitleCallback, callback)
            }
        }
    }
    
    
    val scriptTags = document.select("script")
    scriptTags.forEach { script ->
        val scriptContent = script.html()
        
        
        val okRegex = """https?://ok\.ru/videoembed/\d+""".toRegex()
        okRegex.findAll(scriptContent).forEach { match ->
            Log.d("DiziCan", "OK.ru link found in script: ${match.value}")
            loadExtractor(match.value, "$mainUrl/", subtitleCallback, callback)
        }
        
       
        val vkRegex = """https?://vk\.com/video_ext\.php\?[^"']+""".toRegex()
        vkRegex.findAll(scriptContent).forEach { match ->
            Log.d("DiziCan", "VK link found in script: ${match.value}")
            loadExtractor(match.value, "$mainUrl/", subtitleCallback, callback)
        }
    }

    return true
}}