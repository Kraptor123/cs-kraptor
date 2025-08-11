package com.keyiflerolsun

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.util.Base64
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.WebResourceRequest
import org.json.JSONObject
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.runBlocking

class HDFilmCehennemi : MainAPI() {
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    override var name = "HDFilmCehennemi"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    private var appContext: Context? = null

    // Context'i alabilmek için bir function ekleyelim
    fun setContext(context: Context) {
        this.appContext = context
    }


    // ! CloudFlare bypass
    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 50L
    override var sequentialMainPageScrollDelay = 50L

    // ! CloudFlare v2
    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            val doc = Jsoup.parse(response.peekBody(1024 * 1024).string())

            if (doc.text().contains("Just a moment")) {
                return cloudflareKiller.intercept(chain)
            }

            return response
        }
    }

    // ObjectMapper for JSON parsing
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    // Standard headers for requests
    private val standardHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0",
        "Accept" to "*/*",
        "X-Requested-With" to "fetch"
    )

    // Ana sayfa kategorilerini tanımlıyoruz
    override val mainPage = mainPageOf(
        "${mainUrl}/load/page/1/home/" to "Yeni Eklenen Filmler",
        "${mainUrl}/load/page/1/categories/nette-ilk-filmler/" to "Nette İlk Filmler",
        "${mainUrl}/load/page/1/home-series/" to "Yeni Eklenen Diziler",
        "${mainUrl}/load/page/1/categories/tavsiye-filmler-izle2/" to "Tavsiye Filmler",
        "${mainUrl}/load/page/1/imdb7/" to "IMDB 7+ Filmler",
        "${mainUrl}/load/page/1/mostLiked/" to "En Çok Beğenilenler",
        "${mainUrl}/load/page/1/genres/aile-filmleri-izleyin-6/" to "Aile Filmleri",
        "${mainUrl}/load/page/1/genres/aksiyon-filmleri-izleyin-5/" to "Aksiyon Filmleri",
        "${mainUrl}/load/page/1/genres/animasyon-filmlerini-izleyin-5/" to "Animasyon Filmleri",
        "${mainUrl}/load/page/1/genres/belgesel-filmlerini-izle-1/" to "Belgesel Filmleri",
        "${mainUrl}/load/page/1/genres/bilim-kurgu-filmlerini-izleyin-3/" to "Bilim Kurgu Filmleri",
        "${mainUrl}/load/page/1/genres/komedi-filmlerini-izleyin-1/" to "Komedi Filmleri",
        "${mainUrl}/load/page/1/genres/korku-filmlerini-izle-4/" to "Korku Filmleri",
        "${mainUrl}/load/page/1/genres/romantik-filmleri-izle-2/" to "Romantik Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // URL'deki sayfa numarasını güncelle
        val url = if (page == 1) {
            request.data
                .replace("/load/page/1/genres/", "/tur/")
                .replace("/load/page/1/categories/", "/category/")
                .replace("/load/page/1/imdb7/", "/imdb-7-puan-uzeri-filmler/")
        } else {
            request.data
                .replace("/page/1/", "/page/${page}/")
        }

        // API isteği gönder
        val response = app.get(url, headers = standardHeaders, referer = mainUrl)

        // Yanıt başarılı değilse boş liste döndür
        if (response.text.contains("Sayfa Bulunamadı")) {
            Log.d("HDCH", "Sayfa bulunamadı: $url")
            return newHomePageResponse(request.name, emptyList())
        }

        try {
            // JSON yanıtını parse et
            val hdfc: HDFC = objectMapper.readValue(response.text)
            val document = Jsoup.parse(hdfc.html)

            Log.d("HDCH", "Kategori ${request.name} için ${document.select("a").size} sonuç bulundu")

            // Film/dizi kartlarını SearchResponse listesine dönüştür
            val results = document.select("a").mapNotNull { it.toSearchResult() }

            return newHomePageResponse(request.name, results)
        } catch (e: Exception) {
            Log.e("HDCH", "JSON parse hatası (${request.name}): ${e.message}")
            return newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.attr("title")
            .takeIf { it.isNotEmpty() }
            .takeUnless {
                it?.contains("Seri Filmler", ignoreCase = true) == true
                        || it?.contains("Japonya Filmleri", ignoreCase = true) == true
                        || it?.contains("Kore Filmleri", ignoreCase = true) == true
                        || it?.contains("Hint Filmleri", ignoreCase = true) == true
                        || it?.contains("Türk Filmleri", ignoreCase = true) == true
                        || it?.contains("DC Yapımları", ignoreCase = true) == true
                        || it?.contains("Marvel Yapımları", ignoreCase = true) == true
                        || it?.contains("Amazon Yapımları", ignoreCase = true) == true
                        || it?.contains("1080p Film izle", ignoreCase = true) == true
            } ?: return null

        val href = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan = this.selectFirst("span.imdb")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score = Score.from10(puan)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get(
            "${mainUrl}/search?q=${query}",
            headers = mapOf("X-Requested-With" to "fetch")
        ).parsedSafe<Results>() ?: return emptyList()

        val searchResults = mutableListOf<SearchResponse>()

        response.results.forEach { resultHtml ->
            val document = Jsoup.parse(resultHtml)

            val title = document.selectFirst("h4.title")?.text() ?: return@forEach
            val href = fixUrlNull(document.selectFirst("a")?.attr("href")) ?: return@forEach
            val posterUrl = fixUrlNull(document.selectFirst("img")?.attr("src")) ?: fixUrlNull(
                document.selectFirst("img")?.attr("data-src")
            )
            val puan = document.selectFirst("span.imdb")?.text()?.trim()

            searchResults.add(
                newMovieSearchResponse(title, href, TvType.Movie) {
                    this.posterUrl = posterUrl?.replace("/thumb/", "/list/")
                    this.score = Score.from10(puan)
                }
            )
        }

        return searchResults
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.section-title")?.text()?.substringBefore(" izle") ?: return null
        val poster = fixUrlNull(document.select("aside.post-info-poster img.lazyload").lastOrNull()?.attr("data-src"))
        val tags = document.select("div.post-info-genres a").map { it.text() }
        val year = document.selectFirst("div.post-info-year-country a")?.text()?.trim()?.toIntOrNull()
        val tvType = if (document.select("div.seasons").isEmpty()) TvType.Movie else TvType.TvSeries
        val description = document.selectFirst("article.post-info-content > p")?.text()?.trim()
        val rating = document.selectFirst("div.post-info-imdb-rating span")?.text()?.substringBefore("(")?.trim()
        val actors = document.select("div.post-info-cast a").map {
            Actor(it.selectFirst("strong")!!.text(), it.select("img").attr("data-src"))
        }

        val recommendations = document.select("div.section-slider-container div.slider-slide").mapNotNull {
            val recName = it.selectFirst("a")?.attr("title") ?: return@mapNotNull null
            val recHref = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val recPosterUrl =
                fixUrlNull(it.selectFirst("img")?.attr("data-src")) ?: fixUrlNull(it.selectFirst("img")?.attr("src"))
            val puan = it.selectFirst("span.imdb")?.text()?.trim()

            newTvSeriesSearchResponse(recName, recHref, TvType.TvSeries) {
                this.posterUrl = recPosterUrl
                this.score = Score.from10(puan)
            }
        }

        return if (tvType == TvType.TvSeries) {
            val trailer = document.selectFirst("div.post-info-trailer button")?.attr("data-modal")
                ?.substringAfter("trailer/")?.let { "https://www.youtube.com/embed/$it" }
            val episodes = document.select("div.seasons-tab-content a").mapNotNull {
                val epName = it.selectFirst("h4")?.text()?.trim() ?: return@mapNotNull null
                val epHref = fixUrlNull(it.attr("href")) ?: return@mapNotNull null
                val epEpisode = Regex("""(\d+)\. ?Bölüm""").find(epName)?.groupValues?.get(1)?.toIntOrNull()
                val epSeason = Regex("""(\d+)\. ?Sezon""").find(epName)?.groupValues?.get(1)?.toIntOrNull() ?: 1

                newEpisode(epHref) {
                    this.name = epName
                    this.season = epSeason
                    this.episode = epEpisode
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(rating)
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            val trailer = document.selectFirst("div.post-info-trailer button")?.attr("data-modal")
                ?.substringAfter("trailer/")?.let { "https://www.youtube.com/embed/$it" }

            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(rating)
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    suspend fun createWebViewAndExtract(
        context: Context,
        baseUrl: String,
        html: String,
        onResult: (String?) -> Unit
    ): WebView = withContext(Dispatchers.Main) {

        val modifiedHtml = html.replace(
            Regex("""jwplayer\s*\(\s*["']player["']\s*\)\s*\.setup\s*\(\s*configs\s*\)\s*;"""),
            """
        window.configs = configs;
        console.log('jwplayer configs set:', JSON.stringify(configs));
        jwplayer("player").setup(configs);
        """.trimIndent()
        )

        return@withContext WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d("kraptor_webview_console", "${consoleMessage?.message()}")
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    extractWithDelay(view, onResult, 0)
                }
            }

            loadDataWithBaseURL(baseUrl, modifiedHtml, "text/html", "UTF-8", null)
        }
    }

    private fun extractWithDelay(webView: WebView?, onResult: (String?) -> Unit, attempt: Int) {
        if (webView == null || attempt > 15) {
            Log.d("kraptor_webview", "Timeout reached or WebView is null")
            onResult(null)
            return
        }

        Log.d("kraptor_webview", "Attempt $attempt - Checking for configs...")

        val extractScript = """
        (function() {
            // 1. JWPlayer configs
            if (typeof window.configs !== 'undefined' && window.configs) {
                return JSON.stringify(window.configs);
            }
            
            // 2. VideoJS player
            if (typeof videojs !== 'undefined') {
                try {
                    var player = videojs('videoplayer');
                    if (player && player.currentSources && player.currentSources().length > 0) {
                        var sources = player.currentSources();
                        var textTracks = [];
                        
                        if (player.textTracks && player.textTracks().length > 0) {
                            for (var i = 0; i < player.textTracks().length; i++) {
                                var track = player.textTracks()[i];
                                if (track.kind === 'captions' || track.kind === 'subtitles') {
                                    textTracks.push({
                                        file: track.src,
                                        label: track.label,
                                        language: track.language,
                                        kind: track.kind,
                                        default: track.default || track.mode === 'showing'
                                    });
                                }
                            }
                        }
                        
                        return JSON.stringify({
                            sources: sources,
                            tracks: textTracks,
                            type: 'videojs'
                        });
                    }
                } catch (e) {}
            }
            
            return null;
        })();
    """.trimIndent()

        webView.evaluateJavascript(extractScript) { resultJson ->
            Log.d("kraptor_webview", "=== CONFIG JSON DEBUG ===")
            Log.d("kraptor_webview", "Raw resultJson: '$resultJson'")

            val cleanResult = resultJson?.let { raw ->
                if (raw == "null" || raw == "\"null\"") {
                    null
                } else {
                    raw.removePrefix("\"").removeSuffix("\"")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                }
            }

            Log.d("kraptor_webview", "Cleaned result length: ${cleanResult?.length ?: 0}")

            if (cleanResult.isNullOrEmpty() || cleanResult == "null") {
                if (attempt < 15) {
                    Log.d("kraptor_webview", "Config is null/empty, retrying in 800ms...")
                    Handler(Looper.getMainLooper()).postDelayed({
                        extractWithDelay(webView, onResult, attempt + 1)
                    }, 800)
                } else {
                    Log.d("kraptor_webview", "Max attempts reached, giving up")
                    onResult(null)
                }
            } else {
                Log.d("kraptor_webview", "SUCCESS! Found config")
                onResult(cleanResult)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        Log.d("kraptor_$name", "data = $data")
        val document = app.get(data).document

        document.select("div.alternative-links").map { element ->
            element to element.attr("data-lang").uppercase()
        }.forEach { (element, langCode) ->
            element.select("button.alternative-link").mapIndexed { index, button -> // index eklendi
                val sourceName = button.text().replace("(HDrip Xbet)", "").trim()
                Triple(sourceName, langCode, button.attr("data-video")) // Triple kullanıldı
            }.forEach { (sourceName, langCode, videoID) ->
                try {
                    val apiGet = app.get(
                        "${mainUrl}/video/$videoID/",
                        headers = mapOf(
                            "Content-Type" to "application/json",
                            "X-Requested-With" to "fetch"
                        ),
                        referer = data
                    ).text

                    Log.d("kraptor_$name", "apiGet = $apiGet")

                    val iframeUrl = Regex("""data-src=\\"([^"]+)""")
                        .find(apiGet)
                        ?.groupValues
                        ?.get(1)
                        ?.replace("\\", "")
                        ?: return@forEach

                    Log.d("kraptor_$name", "iframe = $iframeUrl")

                    val videoReferer = if (iframeUrl.contains("mobi")) {
                        "https://hdfilmcehennemi.mobi/"
                    } else {
                        "${mainUrl}/"
                    }

                    val iframeContent = app.get(iframeUrl, referer = videoReferer).textLarge
                    Log.d("kraptor_$name", "iframeContent length = ${iframeContent.length}")

                    if (appContext != null) {
                        Log.d("kraptor_$name", "Using WebView to extract config...")

                        // Config objesini al
                        val configJson = suspendCoroutine<String?> { continuation ->
                            runBlocking {
                                createWebViewAndExtract(appContext!!, iframeUrl, iframeContent) { result ->
                                    continuation.resume(result)
                                }
                            }
                        }

                        Log.d("kraptor_$name", "Config result = $configJson")

                        configJson?.let { configStr ->
                            if (configStr.isNotEmpty() && configStr != "null") {
                                try {
                                    val configObj = JSONObject(configStr)

                                    // --- ÖNCE: altyazıları işle ve callback'i çağır ---
                                    if (configObj.has("tracks")) {
                                        val tracks = configObj.getJSONArray("tracks")
                                        for (i in 0 until tracks.length()) {
                                            val trackObj = tracks.getJSONObject(i)
                                            val kind = trackObj.optString("kind", "")

                                            if (kind == "captions") {
                                                val subFile = trackObj.optString("file", "")
                                                val label = trackObj.optString("label", "")
                                                val language = trackObj.optString("language", "")

                                                if (subFile.isNotEmpty()) {
                                                    val fullSubUrl = if (subFile.startsWith("http")) {
                                                        subFile
                                                    } else {
                                                        val baseUrl = if (iframeUrl.contains("mobi")) {
                                                            "https://hdfilmcehennemi.mobi"
                                                        } else {
                                                            "${mainUrl}"
                                                        }
                                                        "$baseUrl$subFile"
                                                    }

                                                    val cleanLang = when {
                                                        label.contains("İngilizce") || language == "en" -> "English"
                                                        label.contains("Türkçe") || language == "tr" -> "Turkish"
                                                        language == "forced" -> "Forced"
                                                        label.isNotEmpty() -> label.replace("├╝", "ü").replace("─░", "İ")
                                                        language.isNotEmpty() -> language
                                                        else -> "Unknown"
                                                    }

                                                    subtitleCallback(SubtitleFile(
                                                        cleanLang,
                                                        fullSubUrl,
                                                    ))
                                                }
                                            }
                                        }
                                    }
                                    if (configObj.has("sources")) {
                                        val sources = configObj.getJSONArray("sources")
                                        for (i in 0 until sources.length()) {
                                            val sourceObj = sources.getJSONObject(i)
                                            val videoUrl = sourceObj.optString("file", "").ifEmpty {
                                                sourceObj.optString("src", "")
                                            }
                                            val quality = sourceObj.optString("label", "")

                                            if (videoUrl.isNotEmpty() && videoUrl.startsWith("http")) {
                                                // Her kaynak için benzersiz bir ad oluştur
                                                val fullSourceName = if (sources.length() > 1) {
                                                    "$sourceName $langCode - ${if (quality.isNotEmpty() && quality != "0") "Quality $quality" else "Source ${i + 1}"}"
                                                } else {
                                                    "$sourceName $langCode"
                                                }

                                                Log.d(
                                                    "kraptor_$name",
                                                    "Video URL found: $videoUrl (Source: $fullSourceName)"
                                                )
                                                val refererSon = if (videoUrl.contains("cdnimages")) {
                                                    "https://hdfilmcehennemi.mobi/"
                                                } else if (videoUrl.contains("hls13.playmix.uno")) {
                                                    "https://hdfilmcehennemi.mobi/"
                                                } else {
                                                    "${mainUrl}/"
                                                }

                                                callback.invoke(
                                                    newExtractorLink(
                                                        source = fullSourceName,
                                                        name = fullSourceName,
                                                        url = videoUrl,
                                                        type = ExtractorLinkType.M3U8,
                                                        {
                                                            this.referer = refererSon
                                                            this.quality = Qualities.Unknown.value
                                                        }
                                                    ))
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("kraptor_$name", "Error parsing config JSON: $e")
                                    e.printStackTrace()
                                }
                            }
                        }
                    } else {
                        Log.e("kraptor_$name", "appContext is null!")
                    }

                } catch (e: Exception) {
                    Log.e("kraptor_$name", "Error processing link: $e")
                    e.printStackTrace()
                }
            }
        }
        return@withContext true
    }

    data class Results(
        @JsonProperty("results") val results: List<String> = arrayListOf()
    )

    data class HDFC(
        @JsonProperty("html") val html: String,
        @JsonProperty("meta") val meta: Meta
    )

    data class Meta(
        @JsonProperty("title") val title: String,
        @JsonProperty("canonical") val canonical: Boolean,
        @JsonProperty("keywords") val keywords: Boolean
    )
}