package com.nikyokki

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import org.jsoup.nodes.Element

class FullHDFilmIzlede : MainAPI() {
    override var mainUrl              = "https://fullhdfilmizlede.net"
    override var name                 = "FullHDFilmİzlede"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/kategori/aksiyon-filmleri-izle"      to "Aksiyon",
        "${mainUrl}/kategori/belgesel-izle"   to "Belgesel",
        "${mainUrl}/kategori/bilim-kurgu-filmleri-izle" to "Bilim Kurgu",
        "${mainUrl}/kategori/macera-filmleri-izle"  to "Macera",
        "${mainUrl}/kategori/gerilim-filmleri-izle"  to "Gerilim",
        "${mainUrl}/kategori/komedi-filmleri-izle"  to "Komedi",
        "${mainUrl}/kategori/dram-filmleri-izle"  to "Dram",
        "${mainUrl}/kategori/korku-filmleri-izle"  to "Korku",
        "${mainUrl}/kategori/romantik-filmleri-izle"  to "Romantik",
        "${mainUrl}/kategori/suc-filmleri-izle"  to "Suç",
        "${mainUrl}/kategori/tarih-filmleri-izle"  to "Tarih",
        "${mainUrl}/kategori/savas-filmleri-izle"  to "Savaş",
        "${mainUrl}/kategori/fantastik-filmleri-izle"  to "Fantastik",
        "${mainUrl}/kategori/animasyon-filmleri-izle"  to "Animasyon",
        "${mainUrl}/kategori/aile-filmleri-izle"  to "Aile",
        "${mainUrl}/kategori/gizem-filmleri-izle"  to "Gizem",
        "${mainUrl}/kategori/western-filmleri-izle"  to "Western"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        var url = request.data
        if (page != 1) {
            url += "/sayfa=$page\""
        }
        val document = app.get(url).document
        val home     = document.select("li.movie").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.movieName a")?.text() ?: "title"
        val href      = fixUrlNull(this.selectFirst("div.movieName a")?.attr("href")) ?: "href"
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.post("${mainUrl}/ara",
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            data = mapOf("kelime" to query)).document

        return document.select("li.movie").mapNotNull { it.toMainPageResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.title a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.title a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div.movieBar h2")?.text()?.replace(" izle", "")?.trim() ?: "title"
        val poster          = fixUrlNull(document.selectFirst("div.moviePoster img")?.attr("src"))
        val description     = document.selectFirst("div.movieDescription h2")?.text()?.trim()
        val year            = document.selectXpath("//span[text()='Yapım Yılı: ']//following-sibling::span").text().split(" ").first().toIntOrNull()
        val tags            = document.selectXpath("//span[text()='Kategori: ']//following-sibling::span").select("a").map { it.text().replace(" izle", "") }
        val rating          = document.selectFirst("span.imdb")?.text()?.trim()?.toRatingInt()
        val duration        =
            document.selectXpath("//span[text()='Film Süresi: ']//following-sibling::span").text().split(" ").first().trim().toIntOrNull()
                ?.times(60)
        val recommendations = document.select("div.popularMovieContainer li").mapNotNull { it.toRecommendationResult() }
        val actors          = document.selectXpath("//span[text()='Oyuncular: ']//following-sibling::span").text().split(",")
        val trailer         = document.selectFirst("a.js-modal-btn")?.attr("data-video-id")?.let { "https://www.youtube.com/embed/$it" }
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.rating          = rating
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("div.movieName")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.movieImage img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("FHI", "data » ${data}")
        val document = app.get(data).document
        val iframe = document.selectFirst("iframe")?.attr("src") ?: ""
        Log.d("FHI", "iframe » ${iframe}")
        val iDocument = app.get(iframe, referer = "${mainUrl}/").document
        val script = iDocument.select("script").find { it.data().contains("sources") }?.data() ?: ""
        val file = script.substringAfter("file:\"").substringBefore("\",")
        Log.d("FHI", "File: $file")
        val tracks = script.substringAfter("\"tracks\": [").substringBefore("],").replace("},", "}")
        tryParseJson<List<FHISource>>("[${tracks}]")
            ?.filter { it.kind == "captions" }?.map {
                subtitleCallback.invoke(
                    SubtitleFile(
                        it.label.toString(),
                        fixUrl(it.file.toString())
                    )
                )
            }
        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = file,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to "$mainUrl/") // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
            )
        /*M3u8Helper.generateM3u8(
            name,
            file,
            "$mainUrl/"
        ).forEach(callback)*/
        return true
    }

    private data class FHISource(
        @JsonProperty("file") val file: String? = null,
        @JsonProperty("label") val label: String? = null,
        @JsonProperty("kind") val kind: String? = null,
        @JsonProperty("default") val default: Boolean? = null,
    )
}