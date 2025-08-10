package com.nikyokki

import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jsoup.nodes.Element
import java.net.URLDecoder

class DiziGom : MainAPI() {
    override var mainUrl = "https://dizigom1.live"
    override var name = "DiziGom"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
    "${mainUrl}/tum-bolumler" to "Yeni Bölümler",
    "" to "Tüm Diziler",
    "" to "Tüm Filmler",
    "" to "Aksiyon",
    "" to "Animasyon",
    "" to "Belgesel",
    "" to "Bilim Kurgu",
    "" to "Dram",
    "" to "Fantastik",
    "" to "Gerilim",
    "" to "Gizem",
    "" to "Komedi",
    "" to "Korku",
    "" to "Macera",
    "" to "Romantik",
    "" to "Savaş",
    "" to "Suç",
    "" to "Tarih"
)

    private var WpOnce: String? = null
    private val initMutex = Mutex()

    private suspend fun initSession() {
        if (WpOnce != null) return
        initMutex.withLock {
            if (WpOnce != null) return@withLock
            Log.d("kraptor_Dizigom", "🔄 Oturum başlatılıyor: WpOnce Aliniyor")
            val resp = app.get(mainUrl, timeout = 120)
            val wpOnceAl = resp.document.selectFirst("script#ajax-users-list-js-extra")?.data().toString()
            val regex = Regex(pattern = "\"admin_ajax_nonce\":\"([^\"]*)\"", options = setOf(RegexOption.IGNORE_CASE))
            val wpOnceMatch = regex.find(wpOnceAl)?.groupValues[1].toString()
            WpOnce = wpOnceMatch
            Log.d("kraptor_Dizigom", "WpOnce: $WpOnce")
        }
    }

override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    initSession()

    val document = when {
        request.name.contains("Yeni Bölümler") -> {
            app.get("${request.data}/page/$page").document
        }
        request.name.contains("Tüm Filmler") -> {
            if (page == 1) {
                app.get("${mainUrl}/tum-yabanci-filmler-hd2/?filtrele=tarih&sirala=DESC&imdb=&yil=&tur=", referer = "${mainUrl}/").document
            } else {
                app.get("${mainUrl}/tum-yabanci-filmler-hd2/page/$page/?filtrele=tarih&sirala=DESC&imdb=&yil=&tur=", referer = "${mainUrl}/").document
            }
        }
        else -> {
            app.post("${mainUrl}/wp-admin/admin-ajax.php", referer = "${mainUrl}/", data = mapOf(
                "action" to "dizigom_search_action",
                "formData" to if (request.name.contains("Tüm")) {
                    "filtrele=tarih&sirala=DESC&yil=&imdb=&kelime=&tur="
                } else {
                    "filtrele=tarih&sirala=DESC&yil=&imdb=&kelime=&tur=${request.name}"
                },
                "filterType" to "series",
                "paged" to "$page",
                "_wpnonce" to WpOnce!!,
            )).document
        }
    }

    val home = when {
        request.name.contains("Yeni Bölümler") -> {
            document.select("div.episode-box").mapNotNull { it.toEpisodeResult() }
        }
        request.name.contains("Tüm Filmler") -> {
            document.select("div.movie-box").mapNotNull { it.toMainPageResult() }
        }
        else -> {
            document.select("div.single-item").mapNotNull { it.toMainPageResult() }
        }
    }

    return newHomePageResponse(request.name, list = home)
}

private fun Element.toMainPageResult(): SearchResponse? {
    val title = this.selectFirst("div.categorytitle, div.film-ismi")?.text() ?: return null
    val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
    val posterUrl = fixUrlNull(
        this.selectFirst("div.img img")?.attr("data-src") 
         ?: this.selectFirst("div.cat-img img")?.attr("src")
        ?: this.selectFirst("img")?.attr("src")
    )
    val diziYili = this.selectFirst("span.dizimeta")?.text()?.substringAfter("Yapım Yılı : ")?.trim()?.toIntOrNull()
    val rating = this.selectFirst("div.imdbp")?.text()?.substringAfter("IMDb: ")?.substringBefore(")")?.trim()
        ?: this.selectFirst("div.film-ust")?.text()?.trim()
              
    val fimDizi = if (href.contains("-film-") || href.contains("film")) {
        TvType.Movie
    } else {
        TvType.TvSeries
    }

    return newTvSeriesSearchResponse(title, href, fimDizi) {
        this.posterUrl = posterUrl
        this.score = rating?.let { Score.from10(it) }
        this.year = diziYili
    }
}

private fun Element.toEpisodeResult(): SearchResponse? {
    
    val seriesTitle = this.selectFirst("div.serie-name a")?.text() ?: return null
    
   
    val episodeInfo = this.selectFirst("div.episode-name a")?.text() ?: return null
    
    
    val fullTitle = this.selectFirst("div.poster img")?.attr("alt") ?: "$seriesTitle $episodeInfo"
    
    val originalHref = fixUrlNull(this.selectFirst("div.poster a")?.attr("href")) ?: return null
    
    
    val href = convertEpisodeUrlToSeriesUrl(originalHref)
    val posterUrl = fixUrlNull(
        this.selectFirst("div.poster img")?.attr("data-src") 
        ?: this.selectFirst("div.poster img")?.attr("src")
    )
    
    
    val episodeDate = this.selectFirst("div.episode-date")?.text()?.trim()

    return newTvSeriesSearchResponse(fullTitle, href, TvType.TvSeries) {
        this.posterUrl = posterUrl
        
       
    }
}
private fun convertEpisodeUrlToSeriesUrl(episodeUrl: String): String {
    
    
    
    val urlParts = episodeUrl.split("/")
    val episodePart = urlParts.find { it.contains("-sezon-") && it.contains("-bolum-") } ?: return episodeUrl
    
   
    val seriesName = episodePart
        .substringBefore("-sezon-")
        .split("-")
        .filter { !it.matches(Regex("\\d+")) }
        .joinToString("-")
        .trim('-') 
    
    // Yeni URL'yi oluştur
    val baseUrl = episodeUrl.substringBefore("/", "") + "//" + episodeUrl.substringAfter("//").substringBefore("/")
    return "$baseUrl/dizi-izle/$seriesName/"
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

        val title = document.selectFirst("div.serieTitle h1, h1.title-border")?.text()?.trim() ?: return null
        val poster = fixUrlNull(
            document.selectFirst("div.seriePoster")?.attr("style")
                ?.substringAfter("background-image:url(")?.substringBefore(")")
                ?: document.selectFirst("meta[property=og:image]")?.attr("content")
        )
        Log.d("DZG", "Poster: $poster")
        val description =
            document.selectFirst("div.serieDescription p, div#filmbilgileri > div:nth-child(2)")?.text()?.trim()
        val year =
            document.selectFirst("div.airDateYear a, div.movieKunye div.item:nth-child(1) div.value")?.text()?.trim()
                ?.toIntOrNull()
        val trailer = document.selectFirst("meta[property=twitter:player]")?.attr("content")
            ?.takeIf { it.contains("youtube.com/watch") }
            ?.replace("watch?v=", "embed/")

        val tags = document.select("div.genreList a, div#listelements div.elements").map { it.text() }
        val rating = document.selectFirst("div.score")?.text()?.trim()
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
                        this.posterUrl = poster
                    }
                )
            )
        }

        return if (url.contains("-film-")) {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.duration = duration
                addTrailer(trailer)
                this.score = Score.from10(rating)
                addActors(actors)
            }
        }else {
                newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeses) {
                    this.posterUrl = poster
                    this.year = year
                    this.plot = description
                    this.tags = tags
                    this.duration = duration
                    addTrailer(trailer)
                    this.score = Score.from10(rating)
                    addActors(actors)
                }
            }
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