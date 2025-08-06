// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class AsyaFanatiklerim : MainAPI() {
    override var mainUrl              = "https://asyafanatiklerim.com"
    override var name                 = "AsyaFanatiklerim"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/bolum/"      to   "Son Bölümler",
        "${mainUrl}/film/"      to   "Filmler",
        "${mainUrl}/dizi/"                  to   "Diziler",
        "${mainUrl}/tur/aile/"              to   "Aile",
        "${mainUrl}/tur/aksiyon/"           to   "Aksiyon",
        "${mainUrl}/tur/dram/"              to   "Dram",
        "${mainUrl}/tur/fantastik/"         to   "Fantastik",
        "${mainUrl}/tur/genclik/"           to   "Gençlik",
        "${mainUrl}/tur/gerilim/"           to   "Gerilim",
        "${mainUrl}/tur/gizem/"             to   "Gizem",
        "${mainUrl}/tur/hukuk/"             to   "Hukuk",
        "${mainUrl}/tur/macera/"            to   "Macera",
        "${mainUrl}/tur/komedi/"            to   "Komedi",
        "${mainUrl}/tur/medikal/"           to   "Medikal",
        "${mainUrl}/tur/polisiye/"          to   "Polisiye",
        "${mainUrl}/tur/romantik/"          to   "Romantik",
        "${mainUrl}/tur/tarih-i/"           to   "Tarih",
        "${mainUrl}/tur/anime/"             to   "Animeler",
        "${mainUrl}/tur/cin-dizileri-tr/"   to   "Çin Dizileri",
        "${mainUrl}/tur/tayvan-dizileri/"   to   "Tayvan Dizileri",
        "${mainUrl}/tur/tayland-dizileri/"  to   "Tayland Dizileri",
        "${mainUrl}/tur/japon-dizileri/"    to   "Japon Dizileri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val document = if (page == 1) {
        app.get(request.data).document
    } else {
        app.get("${request.data}page/$page/", allowRedirects = false).document
    }

    val home = if (request.data.contains("/bolum/")) {
        document.select("article.item.se.episodes").mapNotNull { it.toEpisodeItem() }
    } else {
        document.select("div.items article.item").mapNotNull { it.toMainPageResult() }
    }

    return newHomePageResponse(request.name, home)
}

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h3")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }
    private fun Element.toEpisodeItem(): SearchResponse? {
    val episodeName = this.selectFirst("h3")?.text()?.trim() ?: return null
    val showName = this.selectFirst("div.data span")?.text()?.trim() ?: return null
    val title = "$showName - $episodeName"

    val originalHref = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
    
    
    val seriesHref = convertEpisodeToSeriesUrl(originalHref)
    
    val poster = fixUrlNull(this.selectFirst("img")?.attr("src"))

    return newMovieSearchResponse(title, seriesHref, TvType.TvSeries) {
        this.posterUrl = poster
    }
}

private fun convertEpisodeToSeriesUrl(episodeUrl: String): String {
    
    
    return episodeUrl
        .replace("/bolum/", "/dizi/")  
        .replace(Regex("-\\d+-bolum-izle/?$"), "/") 
}

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.result-item article").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.title a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.title a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.poster img")?.attr("src"))
        val description = document.selectFirst("div.wp-content p")?.text()?.trim()
        val year = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.sgeneros a").map { it.text() }
        val rating = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("span.valor a").map { Actor(it.text()) }
        val trailer = Regex("""<iframe[^>]+src="https://www\.youtube\.com/embed/([^"?]+)""")
    .find(document.html())
    ?.groupValues?.get(1)
    ?.let { "https://www.youtube.com/embed/$it" }

        val episodeList = document.select("ul.episodios li").mapNotNull { episodeBlock ->
            val bolumler = episodeBlock.selectFirst("div.imagen")
            val posterler = fixUrlNull(bolumler?.selectFirst("img")?.attr("src")) ?: return null
            val bhref = fixUrlNull(bolumler?.selectFirst("a")?.attr("href"))  ?: return null
            val rawTitle = episodeBlock.selectFirst("div.episodiotitle")?.text() ?: "Bölüm"
            val epTitle  = rawTitle
                .replace(Regex(".Bölüm"), "Bölüm ")
                .replace(Regex("[0-9]+"),"")
                .replace(Regex(".Bölüm\\s.*"), "Bölüm")

            newEpisode(bhref) {
                this.name = epTitle
                this.posterUrl = posterler
            }
        }

        return if (episodeList.isNotEmpty()) {
            Log.d("asya", "bolum oldugu anlasildi")
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeList) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                this.recommendations = recommendations
                addActors(actors)
                
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.TvSeries, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("asya", "data » ${data}")
        val document = app.get(data).document
        val iframeElements = document.select("iframe")
        iframeElements.forEach { element ->
            // src özniteliğini al ve gerekirse düzelt
            val rawSrc = element.attr("src")
            val iframeUrl = fixUrlNull(rawSrc).toString()
            if (!iframeUrl.contains("youtube")) {
                loadExtractor(iframeUrl, data, subtitleCallback, callback)
                return true
            }
        }
        return true
    }
}