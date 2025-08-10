// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLDecoder

class DiziWatch : MainAPI() {
    override var mainUrl              = "https://diziwatch.tv"
    override var name                 = "DiziWatch"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.TvSeries, TvType.Anime)
    override var sequentialMainPage            = true
    override var sequentialMainPageDelay       = 50L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 50L  // ? 0.05 saniye
    //Anime, AnimeAnime, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,

    override val mainPage = mainPageOf(
        "15"  to "Aile",
        "9"   to "Aksiyon",
        "17"  to "Animasyon",
        "5"   to "Bilim Kurgu",
        "2"   to "Dram",
        "12"  to "Fantastik",
        "3"   to "Gizem",
        "4"   to "Komedi",
        "8"   to "Korku",
        "24"  to "Macera",
        "14"  to "Müzik",
        "7"   to "Romantik",
        "23"  to "Spor",
        "1"   to "Suç",
    )


    private var sessionCookies: Map<String, String>? = null

    private var cKey: String? = null

    private var cValue: String? = null
    private val initMutex = Mutex()

    private suspend fun initSession() {
        if (sessionCookies != null && cKey != null && cValue != null) return
        initMutex.withLock {
            if (sessionCookies != null && cKey != null && cValue != null) return@withLock
            Log.d("kraptor_${this.name}", "🔄 Oturum başlatılıyor: cookie ve CSRF alınıyor")
            val resp = app.get("${mainUrl}/anime-arsivi", timeout = 120)
            sessionCookies = resp.cookies.mapValues { (_, v) -> URLDecoder.decode(v, "UTF-8") }
            cKey         = resp.document.selectFirst("form.bg-\\[rgba\\(255\\,255\\,255\\,\\.15\\)\\] > input:nth-child(1)")?.attr("value")
            cValue           = resp.document.selectFirst("form.bg-\\[rgba\\(255\\,255\\,255\\,\\.15\\)\\] > input:nth-child(2)")?.attr("value")
            Log.d("kraptor_${this.name}", "✅ Cookie sayısı: ${sessionCookies?.size} ckey=  $cKey cvalue = $cValue")
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        initSession()
        val document = app.get("${mainUrl}/anime-arsivi?category=${request.data}&minImdb=&name=&release_year=&sort=date_desc&page=$page", headers = mapOf(
            "Cookies" to "$sessionCookies",
            "Referer" to "${mainUrl}/"
        )).document
//        Log.d("kraptor_$name","document = $document")
        val home     = document.select("div.content-inner a").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val score     = this.selectFirst("span.hidden")?.text()?.substringAfterLast(" ")

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(score)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.post("${mainUrl}/bg/searchcontent", cookies = sessionCookies!!, data = mapOf(
            "cKey" to "$cKey",
            "cValue" to "$cValue",
            "searchterm" to query
        ), headers = mapOf("X-Requested-With" to "XMLHttpRequest", "Accept" to "application/json, text/javascript, */*; q=0.01")).text

        val mapper = jacksonObjectMapper().registerKotlinModule()

        val response = mapper.readValue(document, ApiResponse::class.java)
        val items = response.data?.result

//        Log.d("kraptor_$name","response = $response")

        return items!!.mapNotNull { icerik ->
            icerik.toSearchResult() }
    }

    private fun Icerikler.toSearchResult(): SearchResponse? {
        val title     = objectName?.replace("\\","").toString()
        val href      = fixUrlNull(usedSlug?.replace("\\","")).toString()
        val posterUrl = objectPosterUrl?.replace("images-macellan-online.cdn.ampproject.org/i/s/", "")
            ?.replace("file.dizilla.club", "file.macellan.online")
            ?.replace("images.dizilla.club", "images.macellan.online")
            ?.replace("images.dizimia4.com", "images.macellan.online")
            ?.replace("file.dizimia4.com", "file.macellan.online")
            ?.replace("/f/f/", "/630/910/")
            ?.replace(Regex("(file\\.)[\\w\\.]+\\/?"), "$1macellan.online/")
            ?.replace(Regex("(images\\.)[\\w\\.]+\\/?"), "$1macellan.online/")
        val score     = objectRelatedImdbPoint

        val type      = if (href.contains("dizi/")){
            TvType.TvSeries
        } else {
            TvType.Anime
        }

        return newAnimeSearchResponse(title, href, type) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf("Referer" to "${mainUrl}/")
            this.score     = Score.from10(score)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h2")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("img.rounded-md")?.attr("src"))
        val description     = document.selectFirst("div.text-sm.text-white p")?.text()?.trim()
        val year            = document.selectFirst("div.text-sm:nth-child(2) span.text-white:nth-child(3)")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.text-sm:nth-child(3) span.text-white:nth-child(3)")
            .flatMap{ it.text().split(",") } // her elemana split uygula
            .map { it.trim() }
        val rating     = document.selectFirst("span.hidden")?.text()?.substringAfterLast(" ")
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        val bolumler        = document.select("ul a").map { bolum ->
            val bHref       = bolum.attr("href").toString()
            val bName       = bolum.selectFirst("span.hidden.sm\\:block")?.text()
            val bSezon      = bolum.selectFirst("span.text-sm")?.text()?.substringBefore(".")?.toIntOrNull()
            val bBolum      = bolum.selectFirst("span.text-sm")?.text()?.substringAfterLast(".")?.substringBeforeLast(" ")?.toIntOrNull()
            newEpisode(bHref, {
                this.name = bName
                this.season = bSezon
                this.episode = bBolum
            })
        }

        val type      = if (url.contains("dizi/")){
            TvType.TvSeries
        } else {
            TvType.Anime
        }

        return newAnimeLoadResponse(title, url, type, true) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(rating)
            this.duration        = duration
            this.recommendations = recommendations
            this.episodes        = mutableMapOf(DubStatus.Subbed to bolumler)
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newAnimeSearchResponse(title, href, TvType.Anime) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val iframe   = fixUrlNull(document.selectFirst("iframe")?.attr("src")).toString()

         loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiResponse(
    @JsonProperty("data")
    val data: DataWrapper? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataWrapper(
    val state: Boolean? = null,
    val result: List<Icerikler>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Icerikler(
    @JsonProperty("object_id")
    val objectId: Int? = null,
    @JsonProperty("object_related_imdb_id")
    val objectRelatedImdbId: String? = null,
    @JsonProperty("object_related_imdb_point")
    val objectRelatedImdbPoint: Double? = null,
    @JsonProperty("used_min_trust_score")
    val usedMinTrustScore: Int? = null,
    @JsonProperty("used_slug")
    val usedSlug: String? = null,
    @JsonProperty("used_type")
    val usedType: String? = null,
    @JsonProperty("object_name")
    val objectName: String? = null,
    @JsonProperty("object_alternative_name")
    val objectAlternativeName: String? = null,
    @JsonProperty("object_name_alternative")
    val objectNameAlternative: String? = null,
    @JsonProperty("object_release_year")
    val objectReleaseYear: Int? = null,
    @JsonProperty("object_release_date")
    val objectReleaseDate: String? = null,
    @JsonProperty("object_detail_name")
    val objectDetailName: String? = null,
    @JsonProperty("object_language")
    val objectLanguage: String? = null,
    @JsonProperty("object_categories")
    val objectCategories: String? = null,
    @JsonProperty("object_full_text_source")
    val objectFullTextSource: String? = null,
    @JsonProperty("object_content_count")
    val objectContentCount: Int? = null,
    @JsonProperty("object_poster_url")
    val objectPosterUrl: String? = null,
    @JsonProperty("object_back_url")
    val objectBackUrl: String? = null,
    @JsonProperty("object_face_url")
    val objectFaceUrl: String? = null,
    @JsonProperty("object_logo_url")
    val objectLogoUrl: String? = null,
    @JsonProperty("object_square_url")
    val objectSquareUrl: String? = null
)