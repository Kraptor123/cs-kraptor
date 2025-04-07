package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class Tafdi : MainAPI() {
    override var mainUrl              = "https://tafdi.info"
    override var name                 = "Tafdi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/category/aile-filmleri/"      to "Aile",
        "${mainUrl}/category/aksiyon-filmleri/"   to "Aksiyon",
        "${mainUrl}/category/animasyon-filmleri/" to "Animasyon",
        "${mainUrl}/category/belgeseller/"  to "Belgesel",
        "${mainUrl}/category/bilim-kurgu-filmleri/" to "Bilim Kurgu",
        "${mainUrl}/category/biyografi-filmleri/" to "Biyografi",
        "${mainUrl}/category/blu-ray-filmler/" to "Blu Ray",
        "${mainUrl}/category/cizgi-filmler/" to "Çizgi",
        "${mainUrl}/category/dram-filmleri/" to "Dran",
        "${mainUrl}/category/fantastik-filmler/" to "Fantastik",
        "${mainUrl}/category/gerilim-filmleri/" to "Gerilim",
        "${mainUrl}/category/gizem-filmleri/" to "Gizem",
        "${mainUrl}/category/hint-filmleri/" to "Hint",
        "${mainUrl}/category/komedi-filmleri/" to "Komedi",
        "${mainUrl}/category/korku-filmleri/" to "Korku",
        "${mainUrl}/category/macera-filmleri/" to "Macera",
        "${mainUrl}/category/muzikal-filmler/" to "Müzikal",
        "${mainUrl}/category/polisiye-filmleri/" to "Polisiye",
        "${mainUrl}/category/psikolojik-filmler/" to "Psikolojik",
        "${mainUrl}/category/romantik-filmler/" to "Romantik",
        "${mainUrl}/category/savas-filmleri/" to "Savaş",
        "${mainUrl}/category/suc-filmleri/" to "Suç",
        "${mainUrl}/category/tarih-filmleri/" to "Tarih",
        "${mainUrl}/category/western-filmler/" to "Western",
        "${mainUrl}/category/yerli-filmler/" to "Yerli",

    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/${page}").document
        val home = document.select("div.movie-box").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.film-ismi a")?.text()?.replace(" izle", "") ?: ""
        val href = fixUrlNull(this.selectFirst("div.film-ismi a")?.attr("href")) ?: ""
        val posterUrl = fixUrlNull(this.selectFirst("div.poster img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.movie-box").mapNotNull { it.toMainPageResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.title a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.title a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.title-border")?.text()?.replace(" izle", "")?.trim() ?: ""
        val poster = fixUrlNull(document.selectFirst("div.film-afis img")?.attr("src"))
        val description = document.selectFirst("div#film-aciklama")?.text()?.trim()
        var year = document.selectFirst("div.release a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div#listelements a").map { it.text() }
        var rating = document.selectFirst("div.imdb")?.text()?.replace("IMDb Puanı:", "")
            ?.split("/")?.first()?.trim()?.toRatingInt()
        var actors = document.select("div.actor a").map { it.text() }
        val trailer = document.selectFirst("div.container iframe")?.attr("src")
        val listItems = document.select("div.list-item")
        for (item in listItems) {
            if (item.selectFirst("a")?.attr("href")?.contains("/yil/") == true) {
                year = item.selectFirst("a")?.text()?.toIntOrNull()
            }
            if (item.selectFirst("a")?.attr("href")?.contains("/oyuncu/") == true) {
                actors = item.select("a").map { it.text() }
            }
        }
        document.select("div#listelements div").forEach {
            if (it.text().contains("IMDb:")) {
                rating = it.text().trim().split(" ").last().toRatingInt()
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.rating = rating
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("TFD", "data » ${data}")
        val document = app.get(data).document
        val iframe = document.selectFirst("iframe")?.attr("src") ?: ""
        Log.d("TFD", iframe)

        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}