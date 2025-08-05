// ! Bu araç @kerimmkirac tarafından | @kerimmkirac için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class YeniWatch : MainAPI() {
    override var mainUrl              = "https://yeniwatch.net.tr"
    override var name                 = "YeniWatch"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Anime)

    override val mainPage = mainPageOf(
        "${mainUrl}/episodes/page/1/"          to "Yeni Bölümler",
        "${mainUrl}/anime-arsivi/page/1/"      to "Tüm Animeler",
        "${mainUrl}/anime-arsivi/page/1/?filtrele=imdb&sirala=DESC&yil=&imdb=&kelime=&tur=Aksiyon"       to "Aksiyon Animeleri",
        "${mainUrl}/anime-arsivi/page/1/?filtrele=imdb&sirala=DESC&yil=&imdb=&kelime=&tur=Komedi"     to "Komedi Animeleri",
        "${mainUrl}/anime-arsivi/page/1/?filtrele=imdb&sirala=DESC&yil=&imdb=&kelime=&tur=İsekai"      to "İsekai Animeleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
       
        val pageUrl = request.data.replace("/page/1/", "/page/$page/")
        
        val document = app.get(pageUrl).document
        
        val home = if (request.data.contains("/episodes/")) {
           
            document.select("div.episode-box").mapNotNull { it.toEpisodePageResult() }
        } else {
           
            document.select("div.single-item").mapNotNull { it.toMainPageResult() }
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toEpisodePageResult(): SearchResponse? {
        val aTag = selectFirst("div.poster a") ?: return null
        var href = fixUrlNull(aTag.attr("href")) ?: return null
        val img = aTag.selectFirst("img") ?: return null
        val posterUrl = fixUrlNull(img.attr("data-src")) ?: fixUrlNull(img.attr("src"))
        
        
        val seriesName = selectFirst("div.serie-name a")?.text()?.trim() ?: return null
        val episodeInfo = selectFirst("div.episode-name a")?.text()?.trim() ?: return null
        
       
        val title = "$seriesName - $episodeInfo"

        
        href = convertEpisodeUrlToCategoryUrl(href)

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val aTag = selectFirst("div.cat-img a") ?: return null
        var href = fixUrlNull(aTag.attr("href")) ?: return null
        val img = aTag.selectFirst("img") ?: return null
        val posterUrl = fixUrlNull(img.attr("src"))
        val title = selectFirst("div.categorytitle a")?.text()?.trim() ?: return null

        
        href = convertEpisodeUrlToCategoryUrl(href)

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    private fun convertEpisodeUrlToCategoryUrl(url: String): String {
        
        
        val episodePattern = Regex("""^(https://yeniwatch\.net\.tr/)(.+?)(-\d+-sezon-\d+-bolum)/?$""")
        val match = episodePattern.find(url)
        
        return if (match != null) {
            val baseUrl = match.groupValues[1]
            val animeName = match.groupValues[2]
            "${baseUrl}category/$animeName/"
        } else {
            url 
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.single-item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = selectFirst("div.cat-img a") ?: return null
        var href = fixUrlNull(aTag.attr("href")) ?: return null
        val img = aTag.selectFirst("img") ?: return null
        val posterUrl = fixUrlNull(img.attr("src"))
        val title = selectFirst("div.categorytitle a")?.text()?.trim() ?: return null

        
        href = convertEpisodeUrlToCategoryUrl(href)

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        
        
        val title = document.selectFirst("h1")?.text()?.trim()?.let { 
            
            it.replace(Regex("""\s*-\s*YeniWatch$"""), "")
        } ?: return null
        val poster = fixUrlNull(document.selectFirst("div.category_image img")?.attr("src"))
        val description = document.selectFirst("div.category_desc")?.text()?.trim()
        
        
        val tags = document.select("div.genres a").mapNotNull { 
            it.text()?.trim()?.takeIf { tag -> tag.lowercase() != "yeniwatch" }
        }
        
        
        val trailer = document.selectFirst("iframe.trailer-video")?.attr("src")
        
        
        val seasons = document.select("#myBtnContainer button.btn").mapNotNull { button ->
            button.attr("search-text")?.trim()?.let { seasonText ->
                if (seasonText.isNotBlank()) {
                    Regex("""(\d+)\.\s*Sezon""").find(seasonText)?.groupValues?.get(1)?.toIntOrNull()
                } else null
            }
        }
        
       
        val episodes = document.select("div.bolumust").mapNotNull { element ->
            val episodeLink = element.selectFirst("a")?.attr("href")
            val episodeTitle = element.selectFirst("div.baslik")?.text()?.trim()
            val episodeDate = element.selectFirst("div.tarih")?.text()?.trim()
            val episodeName = element.selectFirst("div.bolum-ismi")?.text()?.trim()?.let {
                
                it.replace(Regex("""^\((.*)\)$"""), "$1")
            }
            
            if (episodeLink != null && episodeTitle != null) {
               
                val seasonEpisode = Regex("""(\d+)\.\s*Sezon\s*(\d+)\.\s*Bölüm""").find(episodeTitle)
                val seasonNum = seasonEpisode?.groupValues?.get(1)?.toIntOrNull()
                val episodeNum = seasonEpisode?.groupValues?.get(2)?.toIntOrNull()
                
                
                if (seasonNum != null && seasonNum > 0) {
                    newEpisode(
                        url = episodeLink,
                        {
                            name = episodeName ?: "Bölüm $episodeNum"
                            season = seasonNum
                            episode = episodeNum
                            posterUrl = poster
                        }
                       
                    )
                } else null
            } else null
        }
        
        
        return newAnimeLoadResponse(title, url, TvType.Anime, true) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
           this.episodes = mutableMapOf(DubStatus.Subbed to episodes)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newAnimeSearchResponse(title, href, TvType.Anime) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("STF", "data » $data")
        val document = app.get(data).document

        val iframeUrl = document.selectFirst("iframe[src]")?.attr("src")
            ?.takeIf { it.contains("cizgipass5.online/embed/") }
            ?.let { fixUrl(it) }

        if (iframeUrl != null) {
            loadExtractor(
                iframeUrl,
                referer = data,
                subtitleCallback = subtitleCallback,
                callback = callback
            )
        } else {
            Log.e("STF", "iframe bulunamadı")
        }

        return true
    }
}
