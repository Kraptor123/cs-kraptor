// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Base64
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import okio.ByteString.Companion.decodeBase64

class `4KFilmIzleme` : MainAPI() {
    override var mainUrl = "https://www.4kfilmizleme.net"
    override var name = "4KFilmIzleme"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}" to "Yeni Eklenenler",
        "${mainUrl}/turkce-dublaj-izle/" to "Türkçe Dublaj",
        "${mainUrl}/altyazili-film-izle/" to "Altyazılı Film",
        "${mainUrl}/aile-filmleri-izle/" to "Aile",
        "${mainUrl}/aksiyon-filmleri-izle/" to "Aksiyon",
        "${mainUrl}/animasyon-filmleri-izle/" to "Animasyon",
        "${mainUrl}/belgesel/" to "Belgesel",
        "${mainUrl}/bilim-kurgu-filmleri/" to "Bilim Kurgu",
        "${mainUrl}/dram-filmleri/" to "Dram",
        "${mainUrl}/fantastik-filmler-izle/" to "Fantastik",
        "${mainUrl}/gerilim-filmleri-izle/" to "Gerilim",
        "${mainUrl}/gizem/" to "Gizem",
        "${mainUrl}/komedi-filmleri-izle/" to "Komedi",
        "${mainUrl}/korku-filmleri-izle/" to "Korku",
        "${mainUrl}/macera-filmleri-izle/" to "Macera",
        "${mainUrl}/marvel-filmleri-izle/" to "Marvel",
        "${mainUrl}/muzik/" to "Müzik",
        "${mainUrl}/romantik-film-izle/" to "Romantik",
        "${mainUrl}/savas/" to "Savaş",
        "${mainUrl}/suc/" to "Suç",
        "${mainUrl}/tarih/" to "Tarih",
        "${mainUrl}/tv-film/" to "TV film",
        "${mainUrl}/vahsi-bati/" to "Vahşi Batı",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page/").document
        val home = document.select("div.post-film-poster").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.post-movie-title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.post-film-detail").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.post-orj-title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1.movie-name-new")?.text()?.trim()
            ?.replace(Regex("(?i)(izle|film).*$"), "")
            ?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.movie-content-in img")?.attr("src"))
        val description =
            document.selectFirst(".new-detail-info-description > article:nth-child(1) > p:nth-child(2) ")?.text()
                ?.trim()
        val year =
            document.selectFirst("div.single-list > ul:nth-child(1) > li:nth-child(1) > div:nth-child(1) > div:nth-child(2)")
                ?.text()?.trim()?.toIntOrNull()
        val tags = document.select("p a").map { it.text() }
        val rating =
            document.selectFirst(".single-list > ul:nth-child(1) > li:nth-child(2) > div:nth-child(1) > div:nth-child(2)")
                ?.text()?.trim()
        val duration =
            document.selectFirst(".single-list > ul:nth-child(1) > li:nth-child(3) > div:nth-child(1) > div:nth-child(2)")
                ?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val actors = document.select("div.new-detail-list > ul:nth-child(1) > li:nth-child(4) > p:nth-child(1)")
            .map { Actor(it.text().replace("Oyuncular:", "")) }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(rating)
            this.duration = duration
            addActors(actors)
            addTrailer(trailer)
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name", "data » $data")
        for (page in 1..2) {
            val document = app.get("$data/$page").document
            val iframesec = document.select("iframe").attr("src")
            if (!iframesec.contains("youtube")) {
            val iframe = fixUrlNull(iframesec).toString()
            Log.d("kraptor_$name", "iframe » $iframe")
                loadExtractor(iframe, referer = "${mainUrl}/", subtitleCallback = subtitleCallback, callback = callback)
            }
            else return false
            }
            return true
        }
    }