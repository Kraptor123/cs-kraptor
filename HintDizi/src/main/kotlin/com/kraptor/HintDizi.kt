// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class HintDizi : MainAPI() {
    override var mainUrl              = "https://hintdizi.com"
    override var name                 = "HintDizi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.AsianDrama)
    //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,

    override val mainPage = mainPageOf(
        ""  to "Tüm Diziler",
        ""  to "Aksiyon",
        ""  to "Belgesel",
        ""  to "Dram",
        ""  to "Fantastik",
        ""  to "Gerilim",
        ""  to "Gizem",
        ""  to "Komedi",
        ""  to "Korku",
        ""  to "Romantik",
        ""  to "Suç",
        ""  to "Tarih",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (request.name.contains("Tüm Diziler")) {
            if (page == 1) {
                app.get("${mainUrl}/dizi-arsivi/").document
            } else {
                app.get("${mainUrl}/dizi-arsivi/page/$page/").document
            }
        } else if (page == 1) {
            app.get("${mainUrl}/dizi-arsivi/?filtrele=imdb&sirala=DESC&yil=&imdb=&kelime=&tur=${request.name}").document
        } else {
            app.get("${mainUrl}/dizi-arsivi/page/$page/?filtrele=imdb&sirala=DESC&yil&imdb&kelime&tur=${request.name}").document
        }
        val home     = document.select("div.single-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.categorytitle a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val posterUrl = if (poster!!.contains(".avif")) {
            "https://res.cloudinary.com/di0j4jsa8/image/fetch/f_auto/$poster"
        } else {
            poster
        }

        val score     = this.selectFirst("div.imdbp")?.text()?.substringAfterLast(" ")?.substringBefore(")")

        return newTvSeriesSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(score)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.single-item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.categorytitle a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val poster = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val posterUrl = if (poster!!.contains(".avif")) {
            "https://res.cloudinary.com/di0j4jsa8/image/fetch/f_auto/$poster"
        } else {
            poster
        }

        val score     = this.selectFirst("div.imdbp")?.text()?.substringAfterLast(" ")?.substringBefore(")")

        return newTvSeriesSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(score)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.substringBefore(" Türkçe")?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.category_image img")?.attr("src"))
        val description     = document.selectFirst("div.category_desc")?.text()?.trim()
        val year            = document.selectFirst("#icerikcat2 > div:nth-child(1)")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.genres a").map { it.text() }
        val rating          = document.selectFirst("#icerikcat2 > div:nth-child(3)")?.text()?.trim()
        val actors          = document.select("#icerikcat2 > div:nth-child(2)").text().split(",").map { Actor(it) }
        val bolumler        = document.select("div.bolumust").map { bolum ->
            val bolumHref   = fixUrlNull(bolum.selectFirst("a")?.attr("href")).toString()
            val bolumName   = bolum.selectFirst("div.baslik")?.text()
            val sezon       = bolumName?.substringBefore(".")?.toIntOrNull()
            val bolum       = bolumName?.substringBeforeLast(".")?.substringAfterLast(" ")?.toIntOrNull()
            newEpisode(
                url = bolumHref,
                {
                    this.name    = bolumName
                    this.season  = sezon
                    this.episode = bolum
                }
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.AsianDrama, bolumler) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(rating)
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val iframe   = document.select("div.video-container iframe")

        iframe.forEach { iframe ->
            val video = fixUrlNull(iframe.attr("src")).toString()
            Log.d("kraptor_$name", "video = ${video}")

            loadExtractor(video, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }
}