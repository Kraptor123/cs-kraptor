// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class AsyaMinik : MainAPI() {
    override var mainUrl              = "https://asyaminik.com"
    override var name                 = "AsyaMinik"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.AsianDrama)
    //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,

    override val mainPage = mainPageOf(
        "${mainUrl}/category/devam-eden-diziler/"          to   "Devam Eden Diziler",
//        "${mainUrl}/category/cok-yakinda/"                 to   "Çok Yakında",
        "${mainUrl}/category/cin-diziler/"                 to   "Çin Diziler",
        "${mainUrl}/category/cin-mini-diziler/"            to   "Çin Mini Diziler",
        "${mainUrl}/category/kore-mini-diziler/"           to   "Kore & Mini Diziler",
        "${mainUrl}/category/tayvan-dizileri/"             to   "Tayvan Dizileri",
        "${mainUrl}/category/tayland-dizileri/"            to   "Tayland Dizileri",
//        "${mainUrl}/category/bl-tayland/"                  to   "BL Tayland",
        "${mainUrl}/category/endonezya/"                   to   "Endonezya Dizi & Film",
        "${mainUrl}/category/malezya-dizileri/"            to   "Malezya Dizileri",
        "${mainUrl}/category/filipinler/"                  to   "Filipin Dizileri",
        "${mainUrl}/category/japon-dizileri/"              to   "Japon Dizileri",
        "${mainUrl}/category/anime-dizi-film/"             to   "Anime Dizi & Film",
        "${mainUrl}/category/kore-cin-tayland-yabanci/"    to   "Film Keyfi",
        "${mainUrl}/category/final-yapan-diziler/"         to   "Final Yapan Diziler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get(request.data).document
        }else{
            app.get("${request.data}page/$page/").document
        }
        val home = document.select("div.post-container").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.title a")?.text()?.substringBefore(" izle") ?: return null
        if (title.contains("Bölüm")) return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.AsianDrama) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.post-container").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.title a")?.text()?.substringBefore(" izle") ?: return null
        if (title.contains("Bölüm")) return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.AsianDrama) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("#singleBlock img")?.attr("data-src"))
        val descriptionAlt1 = document.selectFirst("div.reading#singleContent > p:nth-child(6)")?.text()
        val descriptionAlt2 = document.selectFirst("div.reading#singleContent > p:nth-child(7)")?.text()?.trim()
        val descriptionAlt3 = document.selectFirst("div.reading#singleContent > p:nth-child(9)")?.text()
        val descriptionAlt4 = document.selectFirst("div.reading#singleContent > p:nth-child(14)")?.text()?.trim()
        Log.d("kraptor_$name", "descriptionAlt3 = $descriptionAlt3")
        val descriptionF = document.selectFirst("div.reading#singleContent > p:nth-child(8)")?.text()?.trim()
        val description = when {
            !descriptionF.isNullOrBlank() -> descriptionF
            !descriptionAlt4.isNullOrBlank() -> descriptionAlt4
            !descriptionAlt3.isNullOrBlank() -> descriptionAlt3
            !descriptionAlt2.isNullOrBlank() -> descriptionAlt2
            !descriptionAlt1.isNullOrBlank() -> descriptionAlt1
            else -> null
        }
        val year =
            document.selectFirst("div.reading#singleContent#singleContent > ul:nth-child(4) > li:nth-child(9)")?.text()
                ?.substringBefore(" -")
                ?.substringAfterLast(" ")
                ?.trim()?.toIntOrNull()
        val tags = document.select("div.reading#singleContent#singleContent > ul:nth-child(4) > li:nth-child(3)").text()
            .replace("Tür:", "")
            .split(",")
            .map { it.trim() }
        val rating = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val duration = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("div.reading#singleContent#singleContent > ul:nth-child(4) > li:nth-child(7)")
            .text()
            .split(",")
            .map { Actor(it.trim()) }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }
//         div.reading h5 a  #singleContent > p a
        val iframe = document.selectFirst("p iframe")
        val dizimiFilmmi = if (iframe == null) {
            TvType.AsianDrama
        } else {
            TvType.Movie
        }
        val episodelar =
            document.select("div.reading#singleContent > p a, div.reading h5 a, div.reading h6 a").map { bolumler ->
                val bolumHref = bolumler.attr("href")
                val bolumFinal = bolumHref.substringBefore("-final").substringAfterLast("-").toIntOrNull()
                val bolumSayi = bolumHref.substringBefore("-bolum").substringAfterLast("-").toIntOrNull()
                val bolumler = if (bolumHref.contains("-final")) {
                    bolumFinal
                } else {
                    bolumSayi
                }
                val bolumSeason = if (bolumHref.contains("-sezon")) {
                    bolumHref.substringBefore("-sezon").substringAfterLast("-").toIntOrNull()
                } else {
                    1
                }
                newEpisode(bolumHref, {
                    this.name = "Bölüm"
                    this.episode = bolumler
                    this.season = bolumSeason
                })
            }


        return if (dizimiFilmmi == TvType.Movie) {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            newTvSeriesLoadResponse(title, url, TvType.AsianDrama, episodelar) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
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

        document.select("p iframe").forEach { iframeEl ->
            val rawSrc = iframeEl.attr("src")
            val fixUrl = fixUrlNull(rawSrc) ?: return@forEach
            loadExtractor(fixUrl, "$mainUrl/", subtitleCallback, callback)
        }

        return true
    }
}