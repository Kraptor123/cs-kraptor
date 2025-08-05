// ! Bu araç @kerimmkirac tarafından | @kerimmkirac için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class SeiCode : MainAPI() {
    override var mainUrl              = "https://seicode.net"
    override var name                 = "SeiCode"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Anime , TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/shows"      to "Tüm Animeler",
        "${mainUrl}/movies"   to "Tüm Filmler",
        
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val document = app.get("${request.data}?filter=null&page=$page").document
    val home = document.select("div.col").mapNotNull { it.toMainPageResult() }
    return newHomePageResponse(request.name, home)
}


    private fun Element.toMainPageResult(): SearchResponse? {
    val href = selectFirst("a.list-media")?.attr("href")?.let { fixUrl(it) } ?: return null
    val title = selectFirst("a.list-title")?.text()?.trim() ?: return null
    val posterStyle = selectFirst("div.media-cover")?.attr("style") ?: ""
    val posterUrl = fixUrlNull(this.selectFirst("div.media.media-cover")?.attr("data-src"))
    val type = if( href.contains("/show/")) TvType.Anime else TvType.Movie  

    return newMovieSearchResponse(title, href, type) {
        this.posterUrl = posterUrl
    }
}


    override suspend fun search(query: String): List<SearchResponse> {
    val document = app.get("${mainUrl}/search/${query}").document
    
    
    return document.select("div.col").mapNotNull { col ->
        
        if (col.selectFirst("div.list-actor") != null) {
            return@mapNotNull null
        }
        
       
        col.selectFirst("div.list-movie")?.let { listMovie ->
            listMovie.toSearchResult()
        }
    }
}

private fun Element.toSearchResult(): SearchResponse? {
    val href = selectFirst("a.list-media")?.attr("href")?.let { fixUrl(it) } ?: return null
    val title = selectFirst("a.list-title")?.text()?.trim() ?: return null
    
    
    val posterUrl = selectFirst("div.media-cover")?.attr("style")?.let { style ->
       
        val regex = """background-image:\s*url\(['"]?(.*?)['"]?\)""".toRegex()
        regex.find(style)?.groupValues?.get(1)?.let { fixUrlNull(it) }
    }
    
    val type = if (href.contains("/show/")) TvType.Anime else TvType.Movie
    
    return newMovieSearchResponse(title, href, type) {
        this.posterUrl = posterUrl
    }
}

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document
    
    val title = document.selectFirst("h1")?.text()?.trim() ?: return null
    val poster = fixUrlNull(document.selectFirst("div.media.media-cover")?.attr("data-src"))
    val description = document.selectFirst("div.text-content")?.text()?.trim()
    val movDescription = document.selectFirst("div.video-attr:nth-child(4) > div:nth-child(2)")?.text()?.trim()
    val year = document.selectFirst("div.featured-attr:nth-child(3) > div:nth-child(2)")?.text()?.trim()?.toIntOrNull()
    
    val tags = document.select("div.categories a").map { it.text() }
    val movTags = document.select("div.category a").map { it.text() }
    val trailer = document.select("ul.nav li.nav-item a")
    .mapNotNull { it.attr("aria-controls") }
    .firstOrNull { it.startsWith("trailer-https://www.youtube.com/embed/") }
    ?.substringAfter("trailer-")

    
    
    val bolumler = mutableListOf<Episode>()
    
    
    val seasonTabs = document.select("ul.nav li.nav-item").filter { tab ->
        val tabText = tab.selectFirst("a")?.text()?.trim()
        tabText?.startsWith("Sezon") == true 
    }
    
    seasonTabs.forEach { seasonTab ->
        val seasonLink = seasonTab.selectFirst("a")?.attr("href")
        val seasonText = seasonTab.selectFirst("a")?.text()?.trim()
        
        
        val seasonNumber = seasonText?.substringAfter("Sezon ")?.trim()?.toIntOrNull()
        
        if (seasonLink != null && seasonNumber != null) {
            val seasonId = seasonLink.substringAfter("#") 
            
            
            val seasonDiv = document.selectFirst("div#$seasonId")
            seasonDiv?.select("a")?.forEach { episodeLink ->
                val bHref = fixUrlNull(episodeLink.attr("href"))
                val episodeDiv = episodeLink.selectFirst("div.episode")
                val nameDiv = episodeLink.selectFirst("div.name")
                
                
                val bNum = episodeDiv?.text()?.substringAfter("Bölüm ")?.trim()?.toIntOrNull()
                
                
                val episodeName = nameDiv?.text()?.trim()?.takeIf { it.isNotBlank() } 
                    ?: "Bölüm $bNum"
                
                if (bHref != null && bNum != null) {
                    bolumler.add(
                        newEpisode(bHref) {
                            this.episode = bNum
                            this.season = seasonNumber
                            this.name = episodeName
                        }
                    )
                }
            }
        }
    }
    
    return if (url.contains("/movie/")) {
        newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = movDescription
            this.year = year
            this.tags = movTags
        }
    } else {
        newTvSeriesLoadResponse(title, url, TvType.Anime, bolumler) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            addTrailer(trailer)
        }
    }
}

private fun Element.toRecommendationResult(): SearchResponse? {
    val title = this.selectFirst("a img")?.attr("alt") ?: return null
    val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))
    
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
    Log.d("STF", "data » $data")
    val document = app.get(data).document

   
    val videoSources = document.select("button.btn-service.dropdown-source")
    Log.d("STF", "${videoSources.size} tane video kaynağı")

    videoSources.forEach { source ->
        val embedId = source.attr("data-embed")
        val sourceName = source.selectFirst("span.name")?.text()?.trim() ?: "Unknown"
        Log.d("STF", "Processing source: $sourceName with embed ID: $embedId")

        if (embedId.isNotEmpty()) {
            try {
                val embedResponse = app.post(
                    "$mainUrl/ajax/embed",
                    data = mapOf(
                        "id" to embedId,
                        "self" to embedId
                    ),
                    headers = mapOf(
                        "X-Requested-With" to "XMLHttpRequest",
                        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                        "Referer" to data
                    )
                )

                Log.d("STF", "Embed response status: ${embedResponse.code}")
                val responseText = embedResponse.text
                Log.d("STF", "Embed response text: $responseText")

                val iframeUrl = responseText.substringAfter("src=\"").substringBefore("\"")
                Log.d("STF", "Iframe URL: $iframeUrl")

                
                loadExtractor(iframeUrl, "$mainUrl/", subtitleCallback, callback)
                
            } catch (e: Exception) {
                Log.e("STF", "error loading $embedId: ${e.message}")
            }
        }
    }

    return true
}
}