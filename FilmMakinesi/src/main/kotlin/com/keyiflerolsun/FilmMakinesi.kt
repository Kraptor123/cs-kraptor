// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element


class FilmMakinesi : MainAPI() {
    override var mainUrl              = "https://filmmakinesi.de"
    override var name                 = "FilmMakinesi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    // ! CloudFlare bypass
    override var sequentialMainPage            = true // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay       = 50L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 50L  // ? 0.05 saniye

    override val mainPage = mainPageOf(
        "${mainUrl}/page/"                                        to "Son Filmler",
        "${mainUrl}/film-izle/olmeden-izlenmesi-gerekenler/page/" to "Ölmeden İzle",
        "${mainUrl}/film-izle/aksiyon-filmleri-izle/page/"        to "Aksiyon",
        "${mainUrl}/film-izle/bilim-kurgu-filmi-izle/page/"       to "Bilim Kurgu",
        "${mainUrl}/film-izle/macera-filmleri/page/"              to "Macera",
        "${mainUrl}/film-izle/komedi-filmi-izle/page/"            to "Komedi",
        "${mainUrl}/film-izle/romantik-filmler-izle/page/"        to "Romantik",
        "${mainUrl}/film-izle/belgesel/page/"                     to "Belgesel",
        "${mainUrl}/film-izle/fantastik-filmler-izle/page/"       to "Fantastik",
        "${mainUrl}/film-izle/polisiye-filmleri-izle/page/"       to "Polisiye Suç",
        "${mainUrl}/film-izle/korku-filmleri-izle-hd/page/"       to "Korku",
        // "${mainUrl}/film-izle/savas/page/"                        to "Tarihi ve Savaş",
        // "${mainUrl}/film-izle/gerilim-filmleri-izle/page/"        to "Gerilim Heyecan",
        // "${mainUrl}/film-izle/gizemli/page/"                      to "Gizem",
        // "${mainUrl}/film-izle/aile-filmleri/page/"                to "Aile",
        // "${mainUrl}/film-izle/animasyon-filmler/page/"            to "Animasyon",
        // "${mainUrl}/film-izle/western/page/"                      to "Western",
        // "${mainUrl}/film-izle/biyografi/page/"                    to "Biyografik",
        // "${mainUrl}/film-izle/dram/page/"                         to "Dram",
        // "${mainUrl}/film-izle/muzik/page/"                        to "Müzik",
        // "${mainUrl}/film-izle/spor/page/"                         to "Spor"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home     = if (request.data.contains("/film-izle/")) {
            document.select("section#film_posts article").mapNotNull { it.toSearchResult() }
        } else {
            document.select("section#film_posts div.tooltip").mapNotNull { it.toSearchResult() }
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("h6 a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("h6 a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src")) ?: fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val title     = this.select("a").last()?.text() ?: return null
        val href      = fixUrlNull(this.select("a").last()?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}?s=${query}").document

        return document.select("section#film_posts article").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div#film_izle h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description     = document.select("section#film_single article p").last()?.text()?.trim()
        val tags            = document.selectFirst("dt:contains(Tür:) + dd")?.text()?.split(", ")
        val rating          = document.selectFirst("dt:contains(IMDB Puanı:) + dd")?.text()?.trim()?.toRatingInt()
        val year            = document.selectFirst("dt:contains(Yapım Yılı:) + dd")?.text()?.trim()?.toIntOrNull()

        val durationElement = document.select("dt:contains(Film Süresi:) + dd time").attr("datetime")
        // ? ISO 8601 süre formatını ayrıştırma (örneğin "PT129M")
        val duration        = if (durationElement.startsWith("PT") && durationElement.endsWith("M")) {
            durationElement.drop(2).dropLast(1).toIntOrNull() ?: 0
        } else {
            0
        }

        val recommendations = document.select("div.hidden-mobile li").mapNotNull { it.toRecommendResult() }
        val actors          = document.selectFirst("dt:contains(Oyuncular:) + dd")?.text()?.split(", ")?.map {
            Actor(it.trim())
        }

        val trailer         = fixUrlNull(document.selectXpath("//iframe[@title='Fragman']").attr("data-src"))

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.year            = year
            this.plot            = description
            this.tags            = tags
            this.rating          = rating
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }


    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("FLMM", "data » $data")
        val document      = app.get(data).document
        val iframe = document.selectFirst("iframe")?.attr("src") ?: ""
        Log.d("FLMM", iframe)

        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}
