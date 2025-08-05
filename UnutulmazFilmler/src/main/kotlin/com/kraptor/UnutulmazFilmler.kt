// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLDecoder

class UnutulmazFilmler : MainAPI() {
    override var mainUrl              = "https://unutulmazfilmler4.com"
    override var name                 = "UnutulmazFilmler"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries)

    override var sequentialMainPage            = true
    override var sequentialMainPageDelay       = 250L // ? 0.25 saniye
    override var sequentialMainPageScrollDelay = 250L // ? 0.25 saniye
    //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,

    override val mainPage = mainPageOf(
        ""    to    "Son Eklenen Filmler",
        ""    to    "Son Eklenen Diziler",
        "49"  to    "Aile",
        "59"  to    "Aksiyon",
        "44"  to    "Animasyon",
//        "47"  to    "Belgesel",
        "66"  to    "Bilim Kurgu",
        "74"  to    "Biyografi",
        "48"  to    "Dram",
        "61"  to    "Fantastik",
//        "84"  to    "Film-Noir",
        "68"  to    "Gerilim",
        "51"  to    "Gizem",
//        "83"  to    "Kısa",
        "45"  to    "Komedi",
        "63"  to    "Korku",
        "60"  to    "Macera",
//        "64"  to    "Müzik",
//        "70"  to    "Reality-TV",
        "65"  to    "Romantik",
        "69"  to    "Savaş",
//        "73"  to    "Spor",
        "46"  to    "Suç",
//        "62"  to    "Tarih",
//        "102" to    "TV film",
//        "78"  to    "Western",
        "15"   to   "Aile Dizi",
        "9"    to   "Aksiyon Dizi",
        "17"   to   "Animasyon Dizi",
//        "13"   to   "Belgesel Dizi",
        "5"    to   "Bilim Kurgu Dizi",
//        "6"    to   "Biyografi Dizi",
//        "2"    to   "Dram Dizi",
//        "16"   to   "Drama Dizi",
//        "12"   to   "Fantastik Dizi",
//        "28"   to   "Game-Show Dizi",
//        "43"   to   "Gerçeklik Dizi",
        "18"   to   "Gerilim Dizi",
        "3"    to   "Gizem Dizi",
        "4"    to   "Komedi Dizi",
        "8"    to   "Korku Dizi",
//        "24"   to   "Macera Dizi",
//        "20"   to   "Müzikal Dizi",
//        "7"    to   "Romantik Dizi",
//        "26"   to   "Savaş Dizi",
//        "23"   to   "Spor Dizi",
        "1"    to   "Suç Dizi",
//        "10"   to   "Tarih Dizi",
        )

    private var sessionCookies: Map<String, String>? = null
    private var cKeyToken: String? = null
    private var cValueToken: String? = null
    private val initMutex = Mutex()

    private suspend fun initSession() {
        if (sessionCookies != null && cKeyToken != null && cValueToken != null) return
        initMutex.withLock {
            if (sessionCookies != null && cKeyToken != null && cValueToken != null) return@withLock
            Log.d("kraptor_$name", "🔄 Oturum başlatılıyor: cookie ve cKey/cValue alınıyor")
            val resp = app.get(mainUrl, interceptor = interceptor, timeout = 120)
            sessionCookies = resp.cookies
                .mapValues { (_, v) -> URLDecoder.decode(v, "UTF-8") }

            // cKey ve cValue token’larını form içinden çekiyoruz
            cKeyToken = resp.document
                .selectFirst("input[name=cKey]")   // <input name="cKey" ...>
                ?.attr("value")
                ?.let { URLDecoder.decode(it, "UTF-8") }

            cValueToken = resp.document
                .selectFirst("input[name=cValue]") // <input name="cValue" ...>
                ?.attr("value")
                ?.let { URLDecoder.decode(it, "UTF-8") }

            Log.d("kraptor_$name", "✅ Cookie sayısı: ${sessionCookies?.size}, cKey: $cKeyToken, cValue: $cValueToken")
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        initSession()
        val document = if (request.name.contains("Dizi")){
            app.post(
                "$mainUrl/bg/findseries", cookies = sessionCookies!!, data = mapOf(
                    "cKey" to cKeyToken!!,
                    "cValue" to cValueToken!!,
                    "currentPage" to "$page",
                    "currentPageCount" to "30",
                    "imdbPointMin" to "0",
                    "imdbPointMax" to "10",
                    "releaseYearStart" to "1900",
                    "releaseYearEnd" to "2500",
                    "countryIdsComma" to "",
                    "yerliCountry" to "9",
                    "categoryIdsComma" to "${request.data}",
                    "orderType" to "date_desc"
                ), interceptor = interceptor
            ).text
        }else {
            app.post(
                "$mainUrl/bg/findmovies", cookies = sessionCookies!!, data = mapOf(
                    "cKey" to cKeyToken!!,
                    "cValue" to cValueToken!!,
                    "currentPage" to "$page",
                    "currentPageCount" to "30",
                    "imdbPointMin" to "0",
                    "imdbPointMax" to "10",
                    "releaseYearStart" to "1900",
                    "releaseYearEnd" to "2500",
                    "countryIdsComma" to "",
                    "yerliCountry" to "9",
                    "categoryIdsComma" to "${request.data}",
                    "orderType" to "date_desc"
                ), interceptor = interceptor
            ).text
        }

        val mapper = jacksonObjectMapper()
        val rootNode = mapper.readTree(document)

        val htmlString = rootNode
            .path("data")
            .path("html")
            .asText()

//        Log.d("kraptor_$name","htmlString = $htmlString")

        val doc: Document = Jsoup.parse(htmlString)


//        Log.d("kraptor_$name","doc = $doc")
        val home     = doc.select("a.ambilight").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text()?.replace("izle","") ?: return null
        val href = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan      = this.selectFirst("span.backdrop-blur:nth-of-type(2)")?.text()?.trim()

        return if (href.contains("/dizi/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
                this.score     = Score.from10(puan)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
                this.score     = Score.from10(puan)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.post("$mainUrl/bg/searchcontentfulltext", data = mapOf(
            "cKey" to cKeyToken!!,
            "cValue" to cValueToken!!,
            "searchterm" to query
            ), cookies = sessionCookies!!).text

        val mapper = jacksonObjectMapper()
        val rootNode = mapper.readTree(document)

        val htmlString = rootNode
            .path("data")
            .path("html")
            .asText()

//        Log.d("kraptor_$name","htmlString = $htmlString")

        val doc: Document = Jsoup.parse(htmlString)

        Log.d("kraptor_$name","doc = $doc")

        return doc.select("div.flex.flex-col.gap-2 > a").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.text-white.text-sm")
            ?.text()
            ?.replace("izle","")
            ?.trim()
            ?: return null

        val href = fixUrlNull(this.attr("href")) ?: return null

        val posterUrl = fixUrlNull(this.selectFirst("img[data-src]")?.attr("data-src")
        )
        val puan      = this.selectFirst("span.backdrop-blur:nth-of-type(2)")?.text()?.trim()

        return if (href.contains("/dizi/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
                this.score     = Score.from10(puan)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
                this.score     = Score.from10(puan)
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim()?.replace("izle","") ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.sm\\:col-span-1 img")?.attr("src"))
        val filmPoster      = fixUrlNull(document.selectFirst("div.col-span-2 img.rounded-lg")?.attr("src"))
        val description     = document.selectFirst("div.lhome.overflow-hidden p")?.text()?.trim()
        val filmDescription = document.selectFirst("div.lhome.overflow-hidden")?.text()?.trim()
        val year            = document.selectFirst("span.font-normal:nth-child(4) > a:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("span.flex a").map { it.text() }
        val rating          = document.selectFirst("div.border-\\[\\#eab308\\]")?.text()?.trim()
        val duration        = document.selectFirst("span.font-normal:nth-child(8)")?.text()?.substringBefore(" ")?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("a.border-\\[\\#222\\]").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("div.flex-wrap a").map { aktor ->
            val aktorIsim   = aktor.attr("title")
            val aktorPoster = aktor.selectFirst("img")?.attr("data-src")
            Actor(aktorIsim,aktorPoster)
        }
val trailer = Regex("""youtube\.com/watch\?v=([\w\-]+)""")
    .find(document.html())
    ?.groupValues?.get(1)
    ?.let { "https://www.youtube.com/embed/$it" }
        val bolumListesi    = document.select("div.sznstabs a").map { bolumler ->
            val bolumHref   = bolumler.attr("href")
            val bolumName   = bolumler.selectFirst("span.block.text-sm")?.text()
            val bolumSezon  = bolumHref.substringBefore(".").toIntOrNull()
            val bolumSayi   = bolumHref.substringAfter("Sezon ").substringBefore(".").toIntOrNull()
            newEpisode(bolumHref,{
                this.name   = bolumName
                this.season = bolumSezon
                this.episode = bolumSayi
            })
        }
        return if (url.contains("/dizi/")) {
         newTvSeriesLoadResponse(title, url, TvType.TvSeries, bolumListesi) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score = Score.from10(rating)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }else {
        newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl       = filmPoster
                this.plot            = filmDescription
                this.year            = year
                this.tags            = tags
                this.score = Score.from10(rating)
                this.duration        = duration
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        }}

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title")?.replace("izle","") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan      = this.selectFirst("span.backdrop-blur:nth-of-type(2)")?.text()?.trim()

        return if (href.contains("/dizi/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
                this.score     = Score.from10(puan)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
                this.score     = Score.from10(puan)
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val iframe   = fixUrlNull(document.selectFirst("iframe")?.attr("src")).toString()

        Log.d("kraptor_$name", "iframe = ${iframe}")

         loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}





private val cloudflareKiller by lazy { CloudflareKiller() }
private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request  = chain.request()
        val response = chain.proceed(request)
        val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

        if (doc.html().contains("Just a moment") || doc.html().contains("verifying")) {
            Log.d("kraptor_Unutulmaz", "!!cloudflare geldi!!")
            return cloudflareKiller.intercept(chain)
        }

        return response
    }
}