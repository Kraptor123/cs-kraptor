// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class RareFilmm : MainAPI() {
    override var mainUrl              = "https://rarefilmm.com"
    override var name                 = "RareFilmm"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/page/"                       to "LATESTS",
        "${mainUrl}/category/action/page/"       to "ACTION",
        "${mainUrl}/category/adventure/page/"    to "ADVENTURE",
        "${mainUrl}/category/animation/page/"    to "ANIMATION",
        "${mainUrl}/category/camp/page/"         to "CAMP",
        "${mainUrl}/category/comedy/page/"       to "COMEDY",
        "${mainUrl}/category/documentary/page/"  to "DOCUMENTARY",
        "${mainUrl}/category/drama/page/"        to "DRAMA",
        "${mainUrl}/category/erotic/page/"       to "EROTIC",
        "${mainUrl}/category/family/page/"       to "FAMILY",
        "${mainUrl}/category/fantasy/page/"      to "FANTASY",
        "${mainUrl}/category/mystery/page/"      to "MYSTERY",
        "${mainUrl}/category/theatre/page/"      to "THEATRE",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home     = document.select("div.post").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("h2 a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("h2 a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.featured-image")?.attr("style")?.substringAfter("url(")?.substringBefore(")"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}?s=${query}").document

        return document.select("div.post").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title       = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(document.selectFirst("div.featured-image")?.attr("style")?.substringAfter("url(")?.substringBefore(")"))
        val description = document.selectFirst("div.entry-content p")?.text()?.trim()
        val tags        = document.select("div.entry-tags a").map { it.text() }
        val rating      = document.selectFirst("span.js-rmp-avg-rating")?.text()?.trim()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot      = description
            this.tags      = tags
            this.score = Score.from10(rating)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("RRF", "data » $data")
        val document = app.get(data).document
        val iframe   = fixUrlNull(document.selectFirst("article iframe")?.attr("src")) ?: return false
        Log.d("RRF", "iframe » $iframe")

        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}