// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class WebDramaTurkey : MainAPI() {
    override var mainUrl              = "https://webdramaturkey.org"
    override var name                 = "WebDramaTurkey"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.AsianDrama)
    override val mainPage = mainPageOf(
        "${mainUrl}/diziler"             to "Diziler",
        "${mainUrl}/filmler"             to "Filmler",
        "${mainUrl}/animeler"            to "Animeler",
        "${mainUrl}/tur/dram"            to "Dram",
        "${mainUrl}/tur/fantastik"       to "Fantastik",
        "${mainUrl}/tur/gerilim"         to "Gerilim",
        "${mainUrl}/tur/komedi"          to "Komedi",
        "${mainUrl}/tur/korku"           to "Korku",
        "${mainUrl}/tur/lise"            to "Lise",
        "${mainUrl}/tur/ofis-aski"       to "Ofis Aşkı",
        "${mainUrl}/tur/romantik"        to "Romantik",
        "${mainUrl}/tur/universite"      to "Üniversite",
        "${mainUrl}/tur/aile"            to "Aile",
        "${mainUrl}/tur/aksiyon"         to "Aksiyon",
        "${mainUrl}/tur/belgesel"        to "Belgesel",
        "${mainUrl}/tur/bilim-kurgu"     to "Bilim Kurgu",
        "${mainUrl}/tur/bromance"        to "Bromance",
        "${mainUrl}/tur/eglence"         to "Eğlence",
        "${mainUrl}/tur/genclik"         to "Gençlik",
        "${mainUrl}/tur/gezi"            to "Gezi",
        "${mainUrl}/tur/gizem"           to "Gizem",
        "${mainUrl}/tur/gl"              to "GL",
        "${mainUrl}/tur/izdivac"         to "İzdivaç",
        "${mainUrl}/tur/muzik"           to "Müzik",
        "${mainUrl}/tur/reality"         to "Reality",
        "${mainUrl}/tur/tarihi"          to "Tarihi",
        "${mainUrl}/tur/tartisma"        to "Tartışma",
        "${mainUrl}/tur/varyete"         to "Varyete",
        "${mainUrl}/tur/web-drama"       to "Web Drama",
        "${mainUrl}/tur/wuxia"           to "Wuxia",
        "${mainUrl}/tur/xianxia"         to "Xianxia",
        "${mainUrl}/tur/yarisma"         to "Yarışma",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?page=$page").document
        val home     = document.select("div.col").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a.list-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.media.media-cover")?.attr("data-src"))
        val tvType    = if (href.contains("/film/")) {
            TvType.Movie
        } else if (href.contains("/anime/")){
            TvType.Anime
        }
        else {
            TvType.AsianDrama
        }


        return newMovieSearchResponse(title, href, type = tvType) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/${query}").document

        return document.select("div.col").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a.list-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.media.media-cover")?.attr("data-src"))
        val tvType    = if (href.contains("/film/")) {
            TvType.Movie
        } else if (href.contains("/anime/")){
            TvType.Anime
        }
        else {
            TvType.AsianDrama
        }

        return newTvSeriesSearchResponse(title, href, type = tvType) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.media.media-cover")?.attr("data-src"))
        val description     = document.selectFirst("div.text-content")?.text()?.trim()
        val movDescription  = document.selectFirst("div.video-attr:nth-child(4) > div:nth-child(2)")?.text()?.trim()
        val year            = document.selectFirst("div.featured-attr:nth-child(1) > div:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val movYear         = document.selectFirst("div.video-attr:nth-child(3) > div:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.categories a").map { it.text() }
        val movTags         = document.select("div.category a").map { it.text() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }
        val recommendations = document.select("div.col").mapNotNull { it.toRecommendationResult() }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }
        val bolumler        = document.select("div.episodes a").map { bolum ->
            val bHref       = fixUrlNull(bolum.attr("href"))
            val bNum        = bolum.selectFirst("div.episode")?.text()?.substringBefore(".")?.toIntOrNull()
            val bSeason     = bHref?.substringBefore("-sezon")?.substringAfterLast("/")?.toIntOrNull()
            newEpisode(bHref, {
                this.episode = bNum
                this.season  = bSeason
                this.name    = "Bölüm"
            })
        }
        return if (url.contains("/film/")) {
         newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.plot            = movDescription
            this.year            = movYear
            this.tags            = movTags
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    } else if (url.contains("/anime/")) {
            newAnimeLoadResponse(title, url, TvType.Anime, true) {
                this.posterUrl       = poster
                this.plot            = description
                this.year            = year
                this.tags            = tags
                this.recommendations = recommendations
                this.episodes        = mutableMapOf(DubStatus.Subbed to bolumler)
                addTrailer(trailer)
            }
    }else {
            newTvSeriesLoadResponse(title, url, TvType.AsianDrama, bolumler) {
                this.posterUrl       = poster
                this.plot            = description
                this.year            = year
                this.tags            = tags
                this.recommendations = recommendations
                addTrailer(trailer)
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a.list-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.media.media-cover")?.attr("data-src"))
        val tvType    = if (href.contains("/film/")) {
            TvType.Movie
        } else if (href.contains("/anime/")){
            TvType.Anime
        }
        else {
            TvType.AsianDrama
        }

        return newMovieSearchResponse(title, href, type = tvType) { this.posterUrl = posterUrl }
    }


    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
//        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val embedIds = document.select("[data-embed]")
            .mapNotNull { it.attr("data-embed") }
            .distinct()

        for (id in embedIds) {
            val response = app.post(
                url = "$mainUrl/ajax/embed",
                referer = data,
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                ),
                data = mapOf("id" to id)
            ).document
            val iframe = response.selectFirst("iframe")?.attr("src").toString()

            val iframeGet = app.get(iframe, referer = data).document

            val iframeSon = iframeGet.selectFirst("iframe")?.attr("src").toString()
            Log.d("kraptor_$name", "iframeSon = ${iframeSon}")
             loadExtractor(iframeSon, "${mainUrl}/", subtitleCallback, callback)
        }
        return true
    }
}