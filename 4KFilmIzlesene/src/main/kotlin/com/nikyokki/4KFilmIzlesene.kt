

package com.nikyokki

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class `4KFilmIzlesene` : MainAPI() {
    override var mainUrl = "https://www.4kfilmizlesene.nl"
    override var name = "4KFilmİzlesene"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/"                   to "Yeni Eklenenler",
        "${mainUrl}/tur/aile-filmleri/"             to "Aile",
        "${mainUrl}/tur/aksiyon-filmleri/"          to "Aksiyon",
        "${mainUrl}/tur/animasyon-filmleri/"        to "Animasyon",
        "${mainUrl}/tur/belgesel/"                  to "Belgesel",
        "${mainUrl}/tur/bilim-kurgu-filmleri/"      to "Bilim Kurgu",
        "${mainUrl}/tur/biyografi/"                 to "Biyografi",
        "${mainUrl}/tur/dram/"                      to "Dram",
        "${mainUrl}/tur/editor/"                    to "Editör",
        "${mainUrl}/tur/fantastik-filmler/"         to "Fantastik",
        "${mainUrl}/tur/genclik/"                   to "Gençlik",
        "${mainUrl}/tur/genel/"                     to "Genel",
        "${mainUrl}/tur/gerilim/"                   to "Gerilim",
        "${mainUrl}/tur/gizem-filmleri/"            to "Gizem",
        "${mainUrl}/tur/komedi-filmleri/"           to "Komedi",
        "${mainUrl}/tur/korku/"                     to "Korku",
        "${mainUrl}/tur/macera-filmleri/"           to "Macera",
        "${mainUrl}/tur/muzik/"                     to "Müzik",
        "${mainUrl}/tur/muzikal/"                   to "Müzikal",
        "${mainUrl}/tur/polisiye-filmler/"          to "Polisiye",
        "${mainUrl}/tur/romantik/"                  to "Romantik",
        "${mainUrl}/tur/savas-filmleri/"            to "Savaş",
        "${mainUrl}/tur/spor/"                      to "Spor",
        "${mainUrl}/tur/suc/"                       to "Suç",
        "${mainUrl}/tur/tarih/"                     to "Tarih",
        "${mainUrl}/tur/western-kovboy-filmleri/"   to "Western & Kovboy",
        "${mainUrl}/tur/yerli-film-izle/"           to "Yerli"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}page/${page}").document
        val home = document.select("div.film-box").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.name")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val puan      = this.selectFirst("span.align-right")?.text()?.trim()
//        Log.d("kraptor_$name","puan = $puan")
        if (this.selectFirst("div.img img")?.attr("data-lazy-src") == "" ||
            this.selectFirst("div.img img")?.attr("data-lazy-src") == null) {
            val posterUrl = fixUrlNull(this.selectFirst("div.img img")?.attr("src"))
            return newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
                this.score     = Score.from10(puan)
            }
        } else {
            val posterUrl = fixUrlNull(this.selectFirst("div.img img")?.attr("data-lazy-src"))
            return newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
                this.score     = Score.from10(puan)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.film-box").mapNotNull { it.toMainPageResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val orgTitle = document.selectFirst("div.film h1")?.text()?.trim() ?: return null
        val altTitle = document.selectFirst("div.original-name span")?.text()?.trim() ?: ""
        val title = if (altTitle.isNotEmpty()) "${orgTitle} - ${altTitle}" else orgTitle
        val poster = fixUrlNull(document.selectFirst("div.img img")?.attr("data-lazy-src"))
        val description = document.selectFirst("div.description")?.text()?.trim()
        val year =
            document.selectFirst("span[itemprop='dateCreated']")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.category a[href*='-filmleri/']").map { it.text() }
        val rating =
            document.selectFirst("div.imdb-count")?.text()?.split(" ")?.first()?.trim()
        val actors = document.select("div.actors").map { it.text() }
        val trailer = document.selectFirst("div.container iframe")?.attr("src")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(rating)
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
        Log.d("4KI", "data » ${data}")
        val document = app.get(data).document
        var iframe   = fixUrlNull(document.selectFirst("div.video-content iframe")?.attr("src")) ?: return false
        Log.d("4KI", "iframe » ${iframe}")

        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
        return true
    }
}
