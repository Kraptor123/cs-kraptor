

package com.keyiflerolsun

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.collections.mapNotNull

class RoketDizi : MainAPI() {
    override var mainUrl = "https://www.roketdizi.live"
    override var name = "RoketDizi"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    // ! CloudFlare bypass
    override var sequentialMainPage =
        true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay = 250L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 250L  // ? 0.05 saniye

    override val mainPage = mainPageOf(
        "${mainUrl}/dizi/tur/aksiyon" to "Aksiyon",
        "${mainUrl}/dizi/tur/bilim-kurgu" to "Bilim Kurgu",
        "${mainUrl}/dizi/tur/gerilim" to "Gerilim",
        "${mainUrl}/dizi/tur/fantastik" to "Fantastik",
        "${mainUrl}/dizi/tur/komedi" to "Komedi",
        "${mainUrl}/dizi/tur/korku" to "Korku",
        "${mainUrl}/dizi/tur/macera" to "Macera",
        "${mainUrl}/dizi/tur/suc" to "Suç",
//        "${mainUrl}/film-kategori/animasyon" to "Aksiyon Film"
    )

    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request  = chain.request()
            val response = chain.proceed(request)
            val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

            if (doc.html().contains("Just a moment")) {
                return cloudflareKiller.intercept(chain)
            }

            return response
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val mainReq = app.get("${request.data}?&page=${page}")

        val document = mainReq.document
        val home = document.select("div.w-full.p-4 span.bg-\\[\\#232323\\]").mapNotNull { it.diziler() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.diziler(): SearchResponse? {
        val title = this.selectFirst("span.font-normal.line-clamp-1")?.text() ?: return null
//        Log.d("RKD","title = $title")
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
//        Log.d("RKD","href = $href")
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
//        Log.d("RKD","posterUrl = $posterUrl")

        return if (href.contains("/dizi/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    private fun toSearchResponse(ad: String, link: String, posterLink: String): SearchResponse? {
        if (link.contains("dizi")) {
            return newTvSeriesSearchResponse(
                ad ?: return null,
                link,
                TvType.TvSeries,
            ) {
                this.posterUrl = posterLink
            }
        } else {
            return newMovieSearchResponse(
                ad ?: return null,
                link,
                TvType.Movie,
            ) {
                this.posterUrl = posterLink
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val mainReq = app.get(mainUrl)
        val mainPage = mainReq.document
        val cKey = mainPage.selectFirst("input[name='cKey']")?.attr("value") ?: return emptyList()
        val cValue =
            mainPage.selectFirst("input[name='cValue']")?.attr("value") ?: return emptyList()
        val cookie = mainReq.cookies["PHPSESSID"].toString()
        println("Ckey: $cKey ---- Cvalue: $cValue ---- cookie: $cookie")

        val veriler = mutableListOf<SearchResponse>()

        val searchReq = app.post(
            "${mainUrl}/api/bg/searchContent?searchterm=$query",
            data = mapOf(
                "cKey" to cKey,
                "cValue" to cValue,
            ),
            headers = mapOf(
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "X-Requested-With" to "XMLHttpRequest",
                "Cookie" to "PHPSESSID=$cookie"
            ),
            referer = "${mainUrl}/",
            cookies = mapOf(
                "CNT" to "vakTR",
                "PHPSESSID" to cookie
            )
        ).parsedSafe<SearchResult>()
        println("SearchReq: $searchReq")

        if (searchReq?.data?.state != true) {
            throw ErrorLoadingException("Invalid Json response")
        }

        val searchDoc = searchReq.data.html?.trim()

        searchDoc?.trim()?.split("</a>")?.forEach { item ->

            val bb = item.substringAfter("<a href=\"").substringBefore("\"")
            val diziUrl = bb.trim()
            val cc = item.substringAfter("data-srcset=\"").substringBefore(" 1x")
            val posterLink = cc.trim()
            val dd = item.substringAfter("<span class=\"text-white\">").substringBefore("</span>")
            val ad = dd.trim()
            toSearchResponse(ad, diziUrl, posterLink)?.let { veriler.add(it) }


        }
        return veriler
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val mainReq = app.get(url)
        val document = mainReq.document
        val title = document.selectFirst("h1.text-white")?.text() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.w-full.page-top img")?.attr("src"))
        val year =
            document.select("div.w-fit.min-w-fit")[1].selectFirst("span.text-sm.opacity-60")?.text()
                ?.split(" ")?.last()?.toIntOrNull()
        val description = document.selectFirst("div.mt-2.text-sm")?.text()?.trim()
        val tags =
            document.selectFirst("h3.text-white.opacity-60.text-sm.sm\\:text-md")?.text()?.split(",")?.map { it }
        val rating =
            document.selectFirst("div.flex.items-center")?.selectFirst("span.text-white.text-sm")
                ?.text()?.trim()
        val actors = document.select("div.global-box h5").map {
            Actor(it.text())
        }
        val regex = Regex("""url":"([^"]*)""", RegexOption.IGNORE_CASE)
        val urls = regex.findAll(document.html())
            .map { it.groupValues[1] }
            .toList()

        val isSeries = urls.any { it.contains("bolum-", ignoreCase = true) }

        return if (isSeries) {
            // Her URL için episode objesi yarat
            val episodes = urls
                .filter { it.contains("bolum") }
                .map { epUrl ->
                    val epNames = epUrl.substringAfterLast("/").replace("-c26", "")
                    val epName = if (epNames.contains("bolum")) {
                        "Bölüm"
                    } else {
                        epNames
                    }
                    val seasonNumber = epUrl
                        .substringAfter("/sezon-")
                        .substringBefore("/")
                    newEpisode(epUrl) {
                        name = epName
                        this.season = seasonNumber.toInt()
                    }
                }
            // Dizi yanıtı
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(rating)
                addActors(actors)
            }
        } else {
            // Film yanıtı
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
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
        val document = app.get(data).document
        val jsonElement = document.getElementById("__NEXT_DATA__")?.data()
        val secureData = JSONObject(jsonElement.toString())
            .getJSONObject("props")
            .getJSONObject("pageProps")
            .getString("secureData")
        val sifreCoz = base64Decode(secureData)

        val seenUrls = mutableSetOf<String>()

        val regex = Regex("iframe src=\\\\\"([^\"]*)\"", RegexOption.IGNORE_CASE)

        val matches = regex.findAll(sifreCoz)

        for (match in matches) {
            val rawUrl = match.groupValues[1].substringBeforeLast("\\")
            val iframe = fixUrlNull(rawUrl).toString()

            if (seenUrls.add(iframe)) {
                loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
            } else {
                null
            }
        }
        return true
    }
}