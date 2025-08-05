// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.extractors.SibNet
import com.lagradost.cloudstream3.extractors.YourUpload

class Animex : MainAPI() {
    override var mainUrl = "https://animex.tr"
    override var name = "Animex"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Anime)


    override val mainPage = mainPageOf(
        "" to "Yeni Bölümler",
        "${mainUrl}/animeler/" to "Animeler",
        "${mainUrl}/film/" to "Filmler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1 && !request.name.contains("Yeni Bölümler", ignoreCase = true)) {
            app.get(request.data).document
        } else if (request.name.contains("Yeni Bölümler")) {
            app.post("$mainUrl/wp-admin/admin-ajax.php", data = mapOf(
                "type" to "last-tv",
                "pageNumber" to "$page",
                "ppp"   to "20",
                "action" to "daha_fazla_yukle"
                ), headers = mapOf("X-Requested-With" to "XMLHttpRequest")).document
        }
        else {
            app.get("${request.data}/page/$page/").document
        }
        Log.d("kraptor_","document = $document")
        val home = buildList {
            addAll(document.select("div.poster.poster-md").mapNotNull { it.toMainPageResult() })
            if (request.name.contains("Yeni Bölümler", ignoreCase = true)) {
                addAll(document.select("li.segment-poster-sm").mapNotNull { it.toMainPageResult() })
            }
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val baseTitle = this.selectFirst("h2")?.text() ?: return null
        val episodeInfo = this.selectFirst("li.segment-poster-sm span.episode-no")?.text()

        val title = if (!episodeInfo.isNullOrBlank()) {
            "$baseTitle - $episodeInfo"
        } else {
            baseTitle
        }
        val rawHref = this.selectFirst("a")?.attr("href") ?: return null

        val href = if (!rawHref.contains("/animeler/") && !rawHref.contains("/film/")) {
            rawHref
                .replace(Regex("-\\d+.*"), "") // -sayı- ve sonrasını temizle
                .replace("https://animex.tr/", "https://animex.tr/animeler/") + "-izle/"
        } else {
            rawHref
        }
        Log.d("Anx", "title = ${title}")
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newAnimeSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.poster.poster-md").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newAnimeSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.page-title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.ui.items img")?.attr("src"))
        val description = document.selectFirst("p#tv-series-desc")?.text()?.trim()
        val text = document.selectFirst(".genre-item")?.text() ?: ""
        val year = Regex("""\b\d{4}\b""").find(text)?.value?.toInt()
        val tags = document.select("div.nano-content a").map { it.text() }
        val rating = document.selectFirst("div.color-imdb")?.text()?.trim()
        val duration =
            document.selectFirst("table.ui > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > div:nth-child(2)")
                ?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val trailer = document.selectFirst("a.prettyPhoto")?.attr("href")
    ?.takeIf { it.contains("youtube.com/watch") }
    ?.replace("watch?v=", "embed/")

        val episodeListesi = document.select("div.ajax_post a").mapNotNull { bolumElemanlari ->
//            val epTitle = document.selectFirst("span.episode-names")?.text()?.trim() ?: return null
            val epHref = fixUrlNull(document.selectFirst("div.ajax_post a")?.attr("href"))
            newEpisode(epHref) {
                this.name = "bölüm"
            }
        }.let { list ->
            mutableMapOf(DubStatus.Subbed to list)
        }

        return if (url.contains("/film/"))
            newMovieLoadResponse(title, url, type = TvType.AnimeMovie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                addTrailer(trailer)

            }
        else
            newAnimeLoadResponse(title, url, TvType.Anime) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                addTrailer(trailer)
                this.episodes = episodeListesi
            }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data, referer = data).document

        // Tüm alternatif linkleri al
        val linkElements = document.select("div.play_top li.belink a")
        if (linkElements.isEmpty()) return false

        for (el in linkElements) {
            val iframeUrl = fixUrlNull(el.attr("data-frame")).toString()
            loadExtractor(iframeUrl, subtitleCallback, callback)
        }
        return true
    }
}