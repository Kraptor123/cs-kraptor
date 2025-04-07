
package com.nikyokki

import Video
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class HDFilmIzle : MainAPI() {
    override var mainUrl              = "https://www.hdfilmizle.to"
    override var name                 = "HDFilmİzle"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/tur/aile/"          to "Aile Filmleri",
        "${mainUrl}/tur/aksiyon/"          to "Aksiyon Filmleri",
        "${mainUrl}/tur/animasyon/"          to "Animasyon Filmleri",
        "${mainUrl}/tur/belgesel/"          to "Belgesel Filmleri",
        "${mainUrl}/tur/bilim-kurgu/"          to "Bilim Kurgu Filmleri",
        "${mainUrl}/tur/dram/"          to "Dram Filmleri",
        "${mainUrl}/tur/fantastik/"          to "Fantastik Filmleri",
        "${mainUrl}/tur/gerilim/"          to "Gerilim Filmleri",
        "${mainUrl}/tur/gizem/"          to "Gizem Filmleri",
        "${mainUrl}/tur/komedi/"          to "Komedi Filmleri",
        "${mainUrl}/tur/korku/"          to "Korku Filmleri",
        "${mainUrl}/tur/macera/"          to "Macera Filmleri",
        "${mainUrl}/tur/romantik/"          to "Romantik Filmler",
        "${mainUrl}/tur/savas/"          to "Savaş Filmleri",
        "${mainUrl}/tur/suc/"          to "Suç Filmleri",
        "${mainUrl}/tur/tarih/"          to "Tarih Filmleri",
        "${mainUrl}/tur/vahsi-bati/"          to "Vahşi Batı Filmleri",
        "${mainUrl}/tur/yerli-film-izle/"          to "Yerli Filmler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document

        val home: List<SearchResponse>?

        home = document.select("div#moviesListResult a.poster").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title     = this.selectFirst("h2.title")?.text() ?: ""
        val href      = fixUrlNull(this.attr("href")) ?: ""
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val response      = app.post(
            "https://www.hdfilmizle.to/search/",
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
            referer = "https://www.hdfilmizle.to",
            data    = mapOf("query" to query)
        ).document
        val searchResults = mutableListOf<SearchResponse>()

        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        try {
            val videos: List<Video> = objectMapper.readValue(response.body().text())
            videos.forEach { video ->
                val title     = video.name
                val href      = fixUrlNull(video.slug) ?: return@forEach
                val posterUrl = fixUrlNull(video.thumbUrl) ?: fixUrlNull(video.thumbWebp)

                searchResults.add(
                    newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
                )
            }
        } catch (e: Exception) {
            println("Error parsing JSON: ${e.message}")
        }

        return searchResults
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val orgTitle       = document.selectFirst("div.page-title h1")?.text() ?: ""
        val altTitle       = document.selectFirst("div.page-title")?.selectFirst("small.text-muted.alt-name")?.text() ?: ""
        val title =
            if (altTitle.isNotEmpty() && orgTitle != altTitle) "$orgTitle - $altTitle" else orgTitle
        val poster      = fixUrlNull(document.selectFirst("picture.poster-auto img")?.attr("data-src"))
        val tags        = document.select("div.pb-2.genres a").map { it.text() }
        val year        = document.selectFirst("div.page-title")?.selectFirst("small.text-muted")?.text()
            ?.replace("(","")?.replace(")","")?.toIntOrNull()
        val description = document.selectFirst("article.text-white > p")?.text()?.trim()
        val rating      = document.selectFirst("div.rate.mb-2 span")?.text()?.toRatingInt()
        val actors      = document.select("div.stories-wrapper a").map {
            Actor(it.selectFirst("div.story-item-title")!!.text(), fixUrlNull(it.select("img").attr("data-src")))
        }

        val recommendations = document.select("div#swiper-wrapper-benzer").mapNotNull {
                val recName      = it.selectFirst("a")?.attr("title") ?: return@mapNotNull null
                val recHref      = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
                val recPosterUrl = fixUrlNull(it.selectFirst("img")?.attr("data-src")) ?: fixUrlNull(it.selectFirst("img")?.attr("src"))

                newMovieSearchResponse(recName, recHref, TvType.Movie) {
                    this.posterUrl = recPosterUrl
                }
            }
        val trailer = document.selectFirst("div.nav-link")?.attr("data-trailer")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.year            = year
            this.plot            = description
            this.tags            = tags
            this.rating          = rating
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }


    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit ): Boolean {
        Log.d("HDF", "data » ${data}")
        val document = app.get(data).document

        val iframe = document.selectFirst("iframe")?.attr("data-src") ?: ""
        Log.d("HDF", "iframe » ${iframe}")
        loadExtractor(iframe, mainUrl, subtitleCallback,callback)

        return true
    }

    private data class SubSource(
        @JsonProperty("file")  val file: String?  = null,
        @JsonProperty("label") val label: String? = null,
        @JsonProperty("kind")  val kind: String?  = null
    )

    data class Results(
        @JsonProperty("results") val results: List<String> = arrayListOf()
    )
}