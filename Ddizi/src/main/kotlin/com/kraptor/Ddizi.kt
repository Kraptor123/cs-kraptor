// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLDecoder

class Ddizi : MainAPI() {
    override var mainUrl = "https://www.ddizi.im"
    override var name = "Ddizi"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.TvSeries)
    //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,

    private var sessionCookies: Map<String, String>? = null
    private val initMutex = Mutex()

    private suspend fun initSession() {
        if (sessionCookies != null) return
        initMutex.withLock {
            if (sessionCookies != null) return@withLock
            Log.d("Anizm", "🔄 Oturum başlatılıyor: cookie ve CSRF alınıyor")
            val resp = app.get(mainUrl, timeout = 120)
            sessionCookies = resp.cookies.mapValues { (_, v) -> URLDecoder.decode(v, "UTF-8") }
        }
    }

    override val mainPage = mainPageOf(
        "${mainUrl}/l.php" to "Son Yüklenen Bölümler",
        "${mainUrl}/eski.diziler/" to "Eski Diziler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (request.data.contains("l.php")) {
            app.get("${request.data}?sayfa=$page").document
        } else {
            app.get("${request.data}$page").document
        }
        val home = document.select("div.col-lg-3").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.img-back")?.attr("data-src"))
//        val score     = this.selectFirst("rating")?.attr("data-src")

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
//            this.score     = Score.from10(score)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        initSession()
        val document = app.post(
            "$mainUrl/arama/",
            data = mapOf("arama" to query),
            cookies = sessionCookies!!
        ).document

        return document.select("div.dizi-boxpost-cat, div.dizi-boxpost").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.img-back-cat")?.attr("data-src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val isEpisodePage = url.endsWith(".htm")

        // 2) Eğer bölüm sayfasıysa, bir üst (ana) sayfayı al; yoksa olduğu gibi devam et
        val document = if (isEpisodePage) {
            val bolumDoc = app.get(url).document
            val anaUrl = bolumDoc
                .selectFirst("ul.breadcrumbX li:nth-child(2) a")
                ?.attr("href")
                .orEmpty()
            app.get(anaUrl).document
        } else {
            app.get(url).document
        }

        // 3) Temel bilgi toplama
        val title = document.selectFirst("ul.breadcrumbX li:nth-child(2)")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.dizi-boxpost-cat img.lazyload")?.attr("data-src"))
        val description = document.selectFirst("div.dizi-boxpost-cat p")?.text()?.trim()
        val year = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()

        // 4) Pagination link’lerini çek, boşsa kendimizi koy
        val rawPages = document
            .select("ul.pagination-sm a")
            .map { it.attr("href") }
            .filter { it.isNotBlank() }
            .distinct()

        val pageUrls: List<String> = rawPages.ifEmpty {
            listOf(url)
        }

        // 5) Her bir sayfayı deneyip bölümleri topla
        val bolumler: List<Episode> = pageUrls.flatMap { pageUrl ->
            runCatching {
                app.get(pageUrl).document
            }.getOrNull()?.select("div.dizi-boxpost-cat")?.map { bolum ->
                val href = bolum.selectFirst("a")?.attr("href").orEmpty()
                val sayi = bolum.selectFirst("a")
                    ?.text()
                    ?.substringBeforeLast(".")
                    ?.substringAfterLast(" ")
                    ?.toIntOrNull()

                val sezon: Int? = Regex("""(\d+)(?:\.\s*)?\s*sezon""", RegexOption.IGNORE_CASE)
                    .find(bolum.text())
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
                val poster = bolum.selectFirst("img")?.attr("data-src").orEmpty()
                newEpisode(href) {
                    this.episode = sayi
                    this.posterUrl = poster
                    this.season = sezon
                }
            } ?: emptyList()
        }


        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, bolumler) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
        }
    }

    override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    Log.d("Ddizi", "data = $data")
    val document = app.get(data).document
    val iframeSrc = fixUrlNull(document.selectFirst("iframe")?.attr("src")).toString()
    Log.d("Ddizi", "iframeSrc = $iframeSrc")

    val iframeGet = if (iframeSrc.contains("/player/oynat/")) {
        app.get(iframeSrc, referer = "${mainUrl}/").text
    } else {
        ""
    }
    Log.d("Ddizi", "iframeGet = $iframeGet")

    
    if (iframeSrc.contains("youtube.com")) {
        Log.d("Ddizi", "YouTube URL detected, fetching iframe content")
        
        
        val iframeContent = app.get(iframeSrc, referer = "${mainUrl}/").text
        Log.d("Ddizi", "iframeContent = $iframeContent")
        
        
        val iframeDocument = app.get(iframeSrc, referer = "${mainUrl}/").document
        val youtubeUrl = iframeDocument.selectFirst("a.div")?.attr("href")
        
        Log.d("Ddizi", "YouTube URL extracted = $youtubeUrl")
        
        if (youtubeUrl?.isNotEmpty() == true) {
            
            loadExtractor(youtubeUrl, "$mainUrl/", subtitleCallback, callback)
            return true
        }
    }

    
    val regex = Regex(
        pattern = """sources:\s*\[\s*\{.*?file:\s*["'](.*?)["'].*?\}\s*,?\s*]""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

   
    val matchResult = regex.find(iframeGet)
    
    val extractedUrl = matchResult?.groupValues?.get(1)?.replace("\\","").toString()

    Log.d("Ddizi", "extractedUrl = $extractedUrl")

    if (extractedUrl.contains("m3u8")) {
        callback.invoke(
            newExtractorLink(
                source = "Ddizi",
                name = "Ddizi",
                url = extractedUrl,
                type = ExtractorLinkType.M3U8
            )
        )
    } else if (extractedUrl.contains("video/mp4")){
        callback.invoke(
            newExtractorLink(
                source = "Google",
                name = "Google",
                url = extractedUrl,
                type = ExtractorLinkType.VIDEO
            )
        )
    }
    else if (extractedUrl.contains(".mp4"))
    {
        callback.invoke(
            newExtractorLink(
                source = "Ddizi",
                name = "Ddizi",
                url = extractedUrl,
                type = ExtractorLinkType.VIDEO
            )
        )
    }
    else {
       
        loadExtractor(iframeSrc, "$mainUrl/", subtitleCallback, callback)
    }

    return true
}}