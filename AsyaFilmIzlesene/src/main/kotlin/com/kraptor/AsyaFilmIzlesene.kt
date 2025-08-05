// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class AsyaFilmIzlesene : MainAPI() {
    override var mainUrl              = "https://asyafilmizlesene.org"
    override var name                 = "AsyaFilmIzlesene"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.AsianDrama)
    //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,

    override val mainPage = mainPageOf(
        "${mainUrl}/en-cok-izlenen-filmler/" to "En Çok İzlenenler",
        "${mainUrl}/en-son-eklenen-filmler-v2/"    to "En Son Eklenenler",
        "${mainUrl}/imdb-7-filmler-izle/"          to "Imdb 7+ Üzeri Filmler",
        "${mainUrl}/uzak-dogu-filmleri/"           to "Uzak Doğu Filmleri",
        "${mainUrl}/hint-filmleri-izle/"           to "Hint Filmleri",
        "${mainUrl}/seri-filmler/"                 to "Seri Filmler",
        "${mainUrl}/aile-filmleri-izle/"           to "Aile",
        "${mainUrl}/aksiyon-filmleri-izle/"        to "Aksiyon",
        "${mainUrl}/bilim-kurgu-filmleri-izle/"    to "Bilim-Kurgu",
        "${mainUrl}/biyografi-filmleri-izle/"      to "Biyografi",
        "${mainUrl}/dram-filmleri-izle/"           to "Dram",
        "${mainUrl}/fantastik-filmler-izle/"       to "Fantastik",
        "${mainUrl}/gerilim-filmleri-izle/"        to "Gerilim",
        "${mainUrl}/gizem-filmleri-izle/"          to "Gizem",
        "${mainUrl}/komedi-filmleri-izle/"         to "Komedi",
        "${mainUrl}/korku-filmleri-izle/"          to "Korku",
        "${mainUrl}/macera-filmleri-izle/"         to "Macera",
        "${mainUrl}/polisiye-suc-filmleri-izle/"   to "Polisiye-Suç",
        "${mainUrl}/romantik-filmler-izl/"         to "Romantik",
        "${mainUrl}/savas-filmleri-izle/"          to "SavaŞ",
        "${mainUrl}/spor-filmleri-izle/"           to "Spor",
        "${mainUrl}/tarihi-filmler-izle/"          to "Tarih",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}page/$page/").document
        val home     = document.select("div.grid-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h4")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val kontrolHref   = if (href.contains("youtube")) {
            fixUrlNull(this.selectFirst("div.entry-button a:nth-child(2)")?.attr("href")) ?: return null
        }else {
            href
        }
        val fragman   = if (href.contains("youtube")) {
            href
        }else {
            fixUrlNull(this.selectFirst("a.fancybox\\.iframe")?.attr("href"))
        }
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val hrefFrag  = "$kontrolHref|$fragman"


        return newMovieSearchResponse(title, hrefFrag, TvType.AsianDrama) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.post("${mainUrl}/wp-admin/admin-ajax.php", referer = "${mainUrl}/", data = mapOf(
            "s" to query,
            "id" to "6944",
            "action" to "is_ajax_load_posts",
            "page"   to "1",
            "security" to "3459d9ff86"
        )).document

        return document.select("div.is-search-sections").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.is-title a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val kontrolHref   = if (href.contains("youtube")) {
            fixUrlNull(this.selectFirst("div.entry-button a:nth-child(2)")?.attr("href")) ?: return null
        }else {
            href
        }
        val fragman   = if (href.contains("youtube")) {
            href
        }else {
            fixUrlNull(this.selectFirst("a.fancybox\\.iframe")?.attr("href"))
        }

        val hrefFrag  = "$kontrolHref|$fragman"
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, hrefFrag, TvType.AsianDrama) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val fragman  = url.substringAfter("|")
        val html      = url.substringBefore("|")
        val document = app.get(html).document

        val title           = document.selectFirst("h1, h1.a")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.entry-thumb img")?.attr("src"))
        val description     = document.selectFirst("div.e-content p")?.text()?.substringAfter("konusu:")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("ul.info-list li:nth-child(3) a").map { it.text() }
        val rating          = document.selectFirst("ul.info-list li:nth-child(6) span")?.text()?.trim()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("ul.info-list li:nth-child(1) a").map { Actor(it.text()) }
        val trailer         = fragman
        val filmUrl         = fixUrlNull(document.selectFirst("a.amy-redirect-watch-online")?.attr("href")) ?: return null

        return newMovieLoadResponse(title, filmUrl, TvType.AsianDrama, filmUrl) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(rating)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.AsianDrama) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val iframe   = document.select("span.optinal-link.amy-streaming-link")

        iframe.forEach { iframe ->
            val iframeSrc = fixUrlNull(iframe.attr("data-source")).toString()
            Log.d("kraptor_$name", "iframeSrc = ${iframeSrc}")
            loadExtractor(iframeSrc, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }
}