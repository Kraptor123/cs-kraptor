// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class HdFilmCehennemi2 : MainAPI() {
    override var mainUrl              = "https://hdfilmcehennemi2.rip"
    override var name                 = "HdFilmCehennemi2"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}"                         to        "Yeni Eklenenler",
        "${mainUrl}/film-arsivi/page/sayfa/?sort=imdb"   to        "Yüksek Puanlılar",
        "${mainUrl}/film-arsivi/page/sayfa/?sort=views"  to        "En Çok İzlenenler",
        "${mainUrl}/film-arsivi/page/sayfa/?lang=tr"     to        "Türkçe Dublaj",
        "${mainUrl}/filmler/aile-filmleri/"              to        "Aile",
        "${mainUrl}/filmler/aksiyon-filmleri/"           to        "Aksiyon",
        "${mainUrl}/filmler/animasyon-filmler/"          to        "Animasyon",
        "${mainUrl}/filmler/belgesel-filmler/"           to        "Belgesel",
        "${mainUrl}/filmler/bilim-kurgu-filmleri/"       to        "Bilim Kurgu",
        "${mainUrl}/filmler/biyografi-filmleri/"         to        "Biyografi",
        "${mainUrl}/filmler/dram-filmleri/"              to        "Dram",
        "${mainUrl}/filmler/fantastik-filmler/"          to        "Fantastik",
        "${mainUrl}/filmler/gerilim-filmleri/"           to        "Gerilim",
        "${mainUrl}/filmler/gizem-filmleri/"             to        "Gizem",
        "${mainUrl}/filmler/komedi-filmleri/"            to        "Komedi",
        "${mainUrl}/filmler/korku-filmleri/"             to        "Korku",
        "${mainUrl}/filmler/macera-filmleri/"            to        "Macera",
        "${mainUrl}/filmler/muzikal-filmler/"            to        "Müzikal",
        "${mainUrl}/filmler/romantik-filmler/"           to        "Romantik",
        "${mainUrl}/filmler/savas-filmleri/"             to        "Savaş",
        "${mainUrl}/filmler/spor-filmleri/"              to        "Spor",
        "${mainUrl}/filmler/suc-filmleri/"               to        "Suç",
        "${mainUrl}/filmler/tarih-filmleri/"             to        "Tarih"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {

        val document = if (request.data.contains("page")){
            app.get(request.data.replace("sayfa", "$page")).document
        }
        else {
            app.get("${request.data}/page/$page").document
        }

        val home     = document.select("div.movie-preview-content").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("span.movie-title")?.text()
            ?.replace("izle","")
            ?.replace(Regex("\\([0-9]+\\).*"), "")
            ?.trim()
            ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.movie-preview-content").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("img")?.attr("alt")
            ?.replace("izle","")
            ?.trim()
            ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.title h1")?.text()?.trim()
            ?.replace("izle","")
            ?.replace(Regex("\\([0-9]+\\).*"), "")
            ?.trim()
            ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.poster img")?.attr("src"))
        val description     = document.selectFirst("div.excerpt")?.text()?.trim()
        val year            = document.selectFirst("div.info-right:nth-child(3) > div:nth-child(1) > div:nth-child(2) > a:nth-child(1)")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.categories a").map { it.text() }
        val rating          = document.selectFirst("span.imdb-rating")?.text()?.replace("IMDB Puanı","")?.trim()
        val recommendations = document.select("ul.flexcroll li").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("div.actor")
            .flatMap { element ->
                element.text()
                    .replace("Oyuncular:", "")
                    .split(",")
                    .map { name -> Actor(name) }
            }
        val pageLinks = document.select("a.post-page-numbers")
        val fragmanElement = pageLinks.firstOrNull { link ->
            link.selectFirst("div.part-name")?.text()?.contains("fragman", ignoreCase = true) == true
        }
        val trailerHref = fragmanElement?.attr("href") ?: ""

        val trailerGet  = app.get(trailerHref).document

        val trailer   = fixUrlNull(trailerGet.select("iframe").attr("src")).toString()
        Log.d("kraptor_$name","trailerHref = $trailerHref")
        Log.d("kraptor_$name","trailer = $trailer")


        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score = Score.from10(rating)
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("span.movie-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document

        val iframe   = fixUrlNull(document.select("iframe").attr("src")).toString()
        Log.d("cehennem", "iframe = $iframe")
        loadExtractor(iframe, referer = iframe, subtitleCallback, callback)

        return true
        }
    }
