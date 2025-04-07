package com.nikyokki

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class FilmKovasi : MainAPI() {
    override var mainUrl = "https://filmkovasi.pw"
    override var name = "FilmKovası"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/filmizle/aile/" to "Aile",
        "${mainUrl}/filmizle/aksiyon-hd/" to "Aksiyon",
        "${mainUrl}/filmizle/animasyon/" to "Animasyon",
        "${mainUrl}/filmizle/belgesel-hd/" to "Belgesel",
        "${mainUrl}/filmizle/bilim-kurgu/" to "Bilim Kurgu",
        "${mainUrl}/filmizle/dram-hd/" to "Dram",
        "${mainUrl}/filmizle/fantastik-hd/" to "Fantastik",
        "${mainUrl}/filmizle/gerilim/" to "Gerilim",
        "${mainUrl}/filmizle/gizem/" to "Gizem",
        "${mainUrl}/filmizle/komedi-hd/" to "Komedi",
        "${mainUrl}/filmizle/korku/" to "Korku",
        "${mainUrl}/filmizle/macera-hd/" to "Macera",
        "${mainUrl}/filmizle/romantik-hd/" to "Romantik",
        "${mainUrl}/filmizle/savas-hd/" to "Savaş",
        "${mainUrl}/filmizle/suc-hd/" to "Suç",
        "${mainUrl}/filmizle/tarih/" to "Tarih",
        "${mainUrl}/filmizle/vahsi-bati-hd/" to "Vahşi Batı",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}page/$page").document
        val home = document.select("div.movie-box").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.film-ismi a")?.text()?.replace(" izle", "") ?: ""
        val href = fixUrlNull(this.selectFirst("div.film-ismi a")?.attr("href")) ?: ""
        val posterUrl = fixUrlNull(this.selectFirst("div.poster img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.movie-box").mapNotNull { it.toMainPageResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.title a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("div.title a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title =
            document.selectFirst("h1.title-border")?.text()?.replace(" izle", "")?.trim() ?: ""
        val poster = fixUrlNull(document.selectFirst("div.film-afis img")?.attr("src"))
        val description = document.selectFirst("div#film-aciklama")?.text()?.trim()
        var year = document.selectFirst("div.release a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div#listelements a").map { it.text() }
        var rating = document.selectFirst("div.imdb")?.text()?.replace("IMDb Puanı:", "")
            ?.split("/")?.first()?.trim()?.toRatingInt()
        var actors = document.select("div.actor a").map { it.text() }
        val trailer = document.selectFirst("div.film-afis iframe")?.attr("src")
        val listItems = document.select("div.list-item")
        for (item in listItems) {
            if (item.selectFirst("a")?.attr("href")?.contains("/yil/") == true) {
                year = item.selectFirst("a")?.text()?.toIntOrNull()
            }
            if (item.selectFirst("a")?.attr("href")?.contains("/oyuncu/") == true) {
                actors = item.select("a").map { it.text() }
            }
        }
        document.select("div#listelements div").forEach {
            if (it.text().contains("IMDb:")) {
                rating = it.text().trim().split(" ").last().toRatingInt()
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.rating = rating
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a img")?.attr("alt") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("FKV", "data » ${data}")
        val document = app.get(data).document
        val iframe = document.selectFirst("iframe")?.attr("src")
        val fName = document.selectFirst("div.sources span")?.text() ?: this.name
        if (iframe != null) {
            loadLinkExtractor(iframe, fName, subtitleCallback, callback)
        }
        document.select("div.sources a").forEach {
            val name = it.selectFirst("span")?.text() ?: this.name
            val doc = app.get(it.attr("href")).document
            val iffi = doc.selectFirst("iframe")?.attr("src") ?: ""
            Log.d("FKV", iffi)
            loadLinkExtractor(iffi, name, subtitleCallback, callback)
        }
        return true
    }

    private suspend fun loadLinkExtractor(
        iframe: String,
        name: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val ianaLink = iframe.substringBefore("/watch/")
        Log.d("FKV", "ianaLink » $ianaLink")
        val idoc = app.get(iframe, referer = iframe).document
        val script = idoc.select("script").find { it.data().contains("sources:") }?.data() ?: ""
        val vidJson = script.substringAfter("var video = ").substringBefore(";")
        val source = script.substringAfter("sources: [").substringBefore("],")
            .replace("`", "\"").addMarks("file").addMarks("type").addMarks("preload")
        val tracks = script.substringAfter("tracks: [").substringBefore("]")
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val video: FKVSource = objectMapper.readValue(vidJson)
        val file: FileSource = objectMapper.readValue(source)
        val track: Track = objectMapper.readValue(tracks)
        Log.d("FKV", "video » $video")
        Log.d("FKV", "file » $file")
        Log.d("FKV", "track » $track")
        subtitleCallback.invoke(SubtitleFile(lang = "Türkçe Altyazı", url = track.file!!))

        val sonLink = ianaLink + file.file?.replace("\${video.uid}", "${video.uid}")
            ?.replace("\${video.md5}", "${video.md5}")?.replace("\${video.id}", "${video.id}")
            ?.replace("\${video.status}", "${video.status}")
        Log.d("FKV", "sonLink » $sonLink")

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = sonLink,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to iframe) // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )
    }
    }

    private data class FKVSource(
        @JsonProperty("uid") val uid: String? = null,
        @JsonProperty("md5") val md5: String? = null,
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("id") val id: String? = null,
        @JsonProperty("status") val status: String? = null,
    )

    private data class FileSource(
        @JsonProperty("file") val file: String? = null,
        @JsonProperty("type") val type: String? = null,
        @JsonProperty("preload") val preload: String? = null,
    )

    private data class Track(
        @JsonProperty("label") val label: String? = null,
        @JsonProperty("file") val file: String? = null,
        @JsonProperty("kind") val kind: String? = null,
    )

    private fun String.addMarks(str: String): String {
        return this.replace(Regex("\"?$str\"?"), "\"$str\"")
    }