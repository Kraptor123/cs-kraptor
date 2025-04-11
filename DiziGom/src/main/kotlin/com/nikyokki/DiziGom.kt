package com.nikyokki

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class DiziGom : MainAPI() {
    override var mainUrl = "https://dizigom1.co"
    override var name = "DiziGom"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/tur/aile/" to "Aile",
        "${mainUrl}/tur/aksiyon/" to "Aksiyon",
        "${mainUrl}/tur/animasyon/" to "Animasyon",
        "${mainUrl}/tur/belgesel/" to "Belgesel",
        "${mainUrl}/tur/bilim-kurgu/" to "Bilim Kurgu",
        "${mainUrl}/tur/dram/" to "Dram",
        "${mainUrl}/tur/fantastik/" to "Fantastik",
        "${mainUrl}/tur/gerilim/" to "Gerilim",
        "${mainUrl}/tur/komedi/" to "Komedi",
        "${mainUrl}/tur/korku/" to "Korku",
        "${mainUrl}/tur/macera/" to "Macera",
        "${mainUrl}/tur/polisiye/" to "Polisiye",
        "${mainUrl}/tur/romantik/" to "Romantik",
        "${mainUrl}/tur/savas/" to "Savaş",
        "${mainUrl}/tur/suc/" to "Suç",
        "${mainUrl}/tur/tarih/" to "Tarih",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val search = "/wp-admin/admin-ajax.php"
        val document = app.get("${request.data}/#p=$page").document
        val tax = document.selectFirst("form.dizigom_advenced_search input")?.attr("name")
        val value = document.selectFirst("form.dizigom_advenced_search input")?.attr("value")
        if (page > 1) {
            val pagedoc = app.post(
                mainUrl + search, cookies = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Referer" to request.data
                ),
                data = mapOf(
                    "action" to "dizigom_search_action",
                    "formData" to "$tax=$value",
                    "paged" to "$page",
                    "_wpnonce" to "18a90a7287"
                )
            ).document
            val home = pagedoc.select("div.episode-box").mapNotNull { it.toMainPageResult() }
            return newHomePageResponse(request.name, home)
        } else {
            val home = document.select("div.episode-box").mapNotNull { it.toMainPageResult() }
            return newHomePageResponse(request.name, home)
        }



    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.serie-name a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.single-item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.categorytitle a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("div.categorytitle a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("div.serieTitle h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(
            document.selectFirst("div.seriePoster")?.attr("style")
                ?.substringAfter("background-image:url(")?.substringBefore(")")
        )
        Log.d("DZG", "Poster: $poster")
        val description = document.selectFirst("div.serieDescription p")?.text()?.trim()
        val year = document.selectFirst("div.airDateYear a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.genreList a").map { it.text() }
        val rating = document.selectFirst("div.score")?.text()?.trim()?.toRatingInt()
        val duration = document.select("div.serieMetaInformation").select("div.totalSession")
            .last()?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val actors = document.select("div.owl-stage a")
            .map { Actor(it.text(), it.selectFirst("img")?.attr("href")) }
        //val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        val episodeses = mutableListOf<Episode>()

        document.select("div.bolumust").forEach {
            val epHref = it.selectFirst("a")?.attr("href") ?: ""
            val epName = it.selectFirst("div.bolum-ismi")?.text()
            val epSeason =
                it.selectFirst("div.baslik")?.text()?.split(" ")?.first()?.replace(".", "")
                    ?.toIntOrNull()
            val epEp = it.selectFirst("div.baslik")?.text()?.split(" ")?.get(2)?.replace(".", "")
                ?.toIntOrNull()
            episodeses.add(
                newEpisode(
                    data = epHref,
                    {
                        this.name = epName
                        this.season = epSeason
                        this.episode = epEp
                    }
                )
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeses) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.duration = duration
            this.rating = rating
            addActors(actors)
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
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Log.d("DZG", "data » ${data}")
        val document = app.get(data, referer = "$mainUrl/").document
        Log.d("Docum", document.toString())
        val embed = document.selectFirst("div#content")?.selectFirst("script")?.data()
        val contentJson: Gof = objectMapper.readValue(embed!!)
        Log.d("DZG", "iframe » ${contentJson.contentUrl}")
        val iframeDocument = app.get(
            contentJson.contentUrl.replace("https://", "https://play."),
            referer = "$mainUrl/"
        ).document
        val script =
            iframeDocument.select("script").find { it.data().contains("eval(function(p,a,c,k,e") }
                ?.data()
                ?: ""
        val unpack = JsUnpacker(script).unpack()
        val sourceJ = unpack?.substringAfter("sources:[")?.substringBefore("]")?.replace("\\/", "/")
        Log.d("DZG", "sourceJ » ${sourceJ}")

        val source: Go = objectMapper.readValue(sourceJ!!)
        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = source.file,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to "$mainUrl/")
                quality = getQualityFromName(source.label)
            }
        )

        return true
    }

    data class Go(
        @JsonProperty("file") val file: String,
        @JsonProperty("label") val label: String,
        @JsonProperty("type") val type: String
    )

    data class Gof(
        @JsonProperty("@context") val context: String,
        @JsonProperty("@type") val type: String,
        @JsonProperty("position") val position: String,
        @JsonProperty("name") val name: String,
        @JsonProperty("description") val description: String,
        @JsonProperty("thumbnailUrl") val thumbnailUrl: String,
        @JsonProperty("uploadDate") val uploadDate: String,
        @JsonProperty("duration") val duration: String,
        @JsonProperty("contentUrl") val contentUrl: String,
        @JsonProperty("timeRequired") val timeRequired: String,
        @JsonProperty("embedUrl") val embedUrl: String,
        @JsonProperty("interactionCount") val interactionCount: String,
    )
}