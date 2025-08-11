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

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun createWebViewAndExtract(
        context: Context,
        baseUrl: String,
        html: String,
        onResult: (String?) -> Unit
    ): WebView = withContext(Dispatchers.Main) {

        // HTML'i daha kapsamlı şekilde modifiye et
        val modifiedHtml = html
            // JWPlayer setup'ı yakala
            .replace(
                Regex("""jwplayer\s*\(\s*["']player["']\s*\)\s*\.setup\s*\(\s*configs\s*\)\s*;"""),
                """
            window.configs = configs;
            window.extractedUrl = null;
            if (configs && configs.sources && configs.sources[0] && configs.sources[0].file) {
                window.extractedUrl = configs.sources[0].file;
                console.log('Video URL extracted:', window.extractedUrl);
            }
            jwplayer("player").setup(configs);
            """.trimIndent()
            )
            // VideoJS setup'ı yakala (.mp4 ve .txt dosyaları için)
            .replace(
                Regex("""videojs\s*\(\s*["'][^"']*["']\s*,\s*\{[^}]*sources[^}]*\}"""),
                """
            const originalSetup = $0;
            try {
                const configMatch = originalSetup.match(/sources[^}]*\}/);
                if (configMatch) {
                    const sourcesStr = configMatch[0];
                    const srcMatch = sourcesStr.match(/src['":\s]*['"]([^'"]+)['"]/);
                    if (srcMatch && srcMatch[1]) {
                        window.extractedUrl = srcMatch[1];
                        console.log('VideoJS URL extracted:', window.extractedUrl);
                    }
                }
            } catch (e) {
                console.error('Error parsing VideoJS config:', e);
            }
            originalSetup
            """.trimIndent()
            )
            // Head'e ek script ekle
            .replace(
                "</head>",
                """
            <script>
            window.videoUrlExtracted = false;
            window.realUrls = []; // Gerçek URL'leri sakla
            
            // XMLHttpRequest override - M3U8 URL'lerini yakala
            const origXHR = window.XMLHttpRequest;
            window.XMLHttpRequest = function() {
                const xhr = new origXHR();
                const origOpen = xhr.open;
                xhr.open = function(method, url) {
                    if (url && (url.includes('.m3u8') || url.includes('/hls/'))) {
                        console.log('XHR M3U8 found:', url);
                        window.realUrls.push(url);
                        if (!window.extractedUrl || window.extractedUrl.startsWith('blob:')) {
                            window.extractedUrl = url;
                        }
                    }
                    return origOpen.apply(this, arguments);
                };
                return xhr;
            };
            
            // Fetch override - M3U8 URL'lerini yakala  
            const origFetch = window.fetch;
            window.fetch = function(url) {
                if (typeof url === 'string' && (url.includes('.m3u8') || url.includes('/hls/'))) {
                    console.log('Fetch M3U8 found:', url);
                    window.realUrls.push(url);
                    if (!window.extractedUrl || window.extractedUrl.startsWith('blob:')) {
                        window.extractedUrl = url;
                    }
                }
                return origFetch.apply(this, arguments);
            };
            
            window.extractVideo = function() {
                try {
                    // Önce gerçek M3U8 URL'leri kontrol et
                    if (window.realUrls && window.realUrls.length > 0) {
                        const lastReal = window.realUrls[window.realUrls.length - 1];
                        if (lastReal && !lastReal.startsWith('blob:')) {
                            console.log('Using real M3U8 URL:', lastReal);
                            return lastReal;
                        }
                    }
                    
                    // JWPlayer configs kontrol et - blob değilse
                    if (window.configs && window.configs.sources && window.configs.sources[0]) {
                        const url = window.configs.sources[0].file;
                        if (url && !url.startsWith('blob:')) {
                            window.extractedUrl = url;
                            window.videoUrlExtracted = true;
                            console.log('Extracted URL (JWPlayer):', url);
                            return url;
                        }
                    }
                    
                    // VideoJS players kontrol
                    if (window.videojs) {
                        const players = videojs.getPlayers();
                        for (let playerId in players) {
                            const player = players[playerId];
                            if (player && player.currentSource && player.currentSource()) {
                                const src = player.currentSource().src;
                                if (src && src.includes('http') && !src.startsWith('blob:')) {
                                    window.extractedUrl = src;
                                    window.videoUrlExtracted = true;
                                    console.log('Extracted URL (VideoJS):', src);
                                    return src;
                                }
                            }
                        }
                    }
                    
                    // Video elementleri kontrol et - blob değilse
                    const videos = document.querySelectorAll('video');
                    for (let video of videos) {
                        if (video.src && video.src.includes('http') && !video.src.startsWith('blob:')) {
                            window.extractedUrl = video.src;
                            window.videoUrlExtracted = true;
                            console.log('Extracted URL (Video Element):', video.src);
                            return video.src;
                        }
                    }
                    
                    // JWPlayer instance'dan almaya çalış - blob değilse
                    if (window.jwplayer && window.jwplayer('player')) {
                        const player = window.jwplayer('player');
                        const playlist = player.getPlaylist();
                        if (playlist && playlist[0] && playlist[0].sources && playlist[0].sources[0]) {
                            const url = playlist[0].sources[0].file;
                            if (url && !url.startsWith('blob:')) {
                                window.extractedUrl = url;
                                window.videoUrlExtracted = true;
                                return url;
                            }
                        }
                    }
                } catch (e) {
                    console.error('Error extracting video URL:', e);
                }
                return null;
            };
            
            // VideoJS event listener ekle
            document.addEventListener('DOMContentLoaded', function() {
                setTimeout(function() {
                    window.extractVideo();
                }, 500);
            });
            
            // Video elementlerine event listener ekle - blob değilse
            document.addEventListener('loadstart', function(e) {
                if (e.target.tagName === 'VIDEO' && e.target.src && !e.target.src.startsWith('blob:')) {
                    window.extractedUrl = e.target.src;
                    console.log('Video loadstart event:', e.target.src);
                }
            }, true);
            </script>
            </head>
            """
            )

        Log.d("kraptor_webview", "Modified HTML prepared")

        return@withContext WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = false

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d("kraptor_webview_console", "${consoleMessage?.message()}")
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    Log.d("kraptor_webview", "Page loaded, starting extraction...")

                    // İlk deneme - hemen kontrol et
                    extractVideoUrl(view, onResult, 0)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d("kraptor_webview", "Page started loading")
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    // URL redirect'lerini engelleyelim
                    return false
                }
            }

            loadDataWithBaseURL(baseUrl, modifiedHtml, "text/html", "UTF-8", null)
        }
    }

    private fun extractVideoUrl(webView: WebView?, onResult: (String?) -> Unit, attempt: Int) {
        if (webView == null || attempt > 8) { // 2 saniye timeout (8 * 250ms) - daha hızlı
            Log.d("kraptor_webview", "Timeout or WebView null, returning null")
            onResult(null)
            return
        }

        webView.evaluateJavascript(
            """
        (function() {
            try {
                // Önce gerçek M3U8 URL'leri kontrol et
                if (window.realUrls && window.realUrls.length > 0) {
                    const lastReal = window.realUrls[window.realUrls.length - 1];
                    if (lastReal && !lastReal.startsWith('blob:')) {
                        return JSON.stringify({success: true, url: lastReal, type: 'real_m3u8'});
                    }
                }
                
                // İlk önce window.extractedUrl kontrol et - blob değilse
                if (window.extractedUrl && !window.extractedUrl.startsWith('blob:')) {
                    return JSON.stringify({success: true, url: window.extractedUrl});
                }
                
                // Manuel extraction dene
                const extractedUrl = window.extractVideo ? window.extractVideo() : null;
                if (extractedUrl && !extractedUrl.startsWith('blob:')) {
                    return JSON.stringify({success: true, url: extractedUrl});
                }
                
                // configs objesini kontrol et (JWPlayer) - blob değilse
                if (window.configs && window.configs.sources && window.configs.sources[0]) {
                    const url = window.configs.sources[0].file;
                    if (url && !url.startsWith('blob:')) {
                        window.extractedUrl = url;
                        return JSON.stringify({success: true, url: url, type: 'jwplayer'});
                    }
                }
                
                // s_5dV6vyCGxgu değişkenini kontrol et (obfuscated koddan) - blob değilse
                if (window.s_5dV6vyCGxgu && !window.s_5dV6vyCGxgu.startsWith('blob:')) {
                    return JSON.stringify({success: true, url: window.s_5dV6vyCGxgu, type: 'obfuscated'});
                }
                
                // VideoJS kontrol et (hdfilmcehennemi.mobi için) - blob değilse
                if (window.videojs) {
                    const players = videojs.getPlayers();
                    for (let playerId in players) {
                        const player = players[playerId];
                        if (player && player.currentSource && player.currentSource()) {
                            const src = player.currentSource().src;
                            if (src && src.includes('http') && !src.startsWith('blob:')) {
                                return JSON.stringify({success: true, url: src, type: 'videojs'});
                            }
                        }
                    }
                }
                
                // Video element'lerini kontrol et - blob değilse
                const videos = document.querySelectorAll('video');
                for (let video of videos) {
                    if (video.src && video.src.includes('http') && !video.src.startsWith('blob:')) {
                        return JSON.stringify({success: true, url: video.src, type: 'video_element'});
                    }
                    if (video.currentSrc && video.currentSrc.includes('http') && !video.currentSrc.startsWith('blob:')) {
                        return JSON.stringify({success: true, url: video.currentSrc, type: 'video_currentSrc'});
                    }
                }
                
                // Source element'lerini kontrol et - blob değilse
                const sources = document.querySelectorAll('source');
                for (let source of sources) {
                    if (source.src && source.src.includes('http') && !source.src.startsWith('blob:')) {
                        return JSON.stringify({success: true, url: source.src, type: 'source_element'});
                    }
                }
                
                // JWPlayer'dan almaya çalış - blob değilse
                if (window.jwplayer && window.jwplayer('player')) {
                    const player = window.jwplayer('player');
                    if (player.getPlaylist) {
                        const playlist = player.getPlaylist();
                        if (playlist && playlist[0] && playlist[0].sources && playlist[0].sources[0]) {
                            const url = playlist[0].sources[0].file;
                            if (url && !url.startsWith('blob:')) {
                                return JSON.stringify({success: true, url: url, type: 'jwplayer_playlist'});
                            }
                        }
                    }
                }
                
                // Global değişkenleri tara - blob değilse
                for (let prop in window) {
                    try {
                        if (typeof window[prop] === 'string' && 
                            !window[prop].startsWith('blob:') &&
                            (window[prop].includes('.m3u8') || 
                            window[prop].includes('.mp4') || 
                            window[prop].includes('/hls/'))) {
                            return JSON.stringify({success: true, url: window[prop], type: 'global_var'});
                        }
                    } catch (e) {
                        // ignore
                    }
                }
                
                return JSON.stringify({success: false, message: 'No URL found', attempt: $attempt});
            } catch (e) {
                return JSON.stringify({success: false, error: e.toString(), attempt: $attempt});
            }
        })();
        """.trimIndent()
        ) { resultJson ->
            try {
                Log.d("kraptor_webview", "JavaScript result (attempt $attempt): $resultJson")

                if (resultJson == "null" || resultJson.isNullOrEmpty()) {
                    // Tekrar dene - daha hızlı interval
                    Handler(Looper.getMainLooper()).postDelayed({
                        extractVideoUrl(webView, onResult, attempt + 1)
                    }, 250)
                    return@evaluateJavascript
                }

                // JSON string'i parse et (çift quote'ları temizle)
                val cleanJson = resultJson.removePrefix("\"").removeSuffix("\"")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")

                val jsonObject = JSONObject(cleanJson)

                if (jsonObject.getBoolean("success")) {
                    val videoUrl = jsonObject.getString("url")
                    val extractionType = jsonObject.optString("type", "unknown")

                    // Blob URL ise reddet ve tekrar dene
                    if (videoUrl.startsWith("blob:")) {
                        Log.d("kraptor_webview", "Rejecting blob URL, retrying...")
                        Handler(Looper.getMainLooper()).postDelayed({
                            extractVideoUrl(webView, onResult, attempt + 1)
                        }, 250)
                        return@evaluateJavascript
                    }

                    Log.d("kraptor_webview", "Video URL extracted successfully ($extractionType): $videoUrl")
                    onResult(videoUrl)
                } else {
                    Log.d("kraptor_webview", "Extraction failed: ${jsonObject.optString("message", "Unknown error")}")
                    // Tekrar dene - daha hızlı interval
                    Handler(Looper.getMainLooper()).postDelayed({
                        extractVideoUrl(webView, onResult, attempt + 1)
                    }, 250)
                }
            } catch (e: Exception) {
                Log.e("kraptor_webview", "Error parsing result: $e")
                Log.d("kraptor_webview", "Raw result: $resultJson")

                // Eğer direkt string geliyorsa (quotes olmadan) ve blob değilse
                if (resultJson.contains("http") && !resultJson.contains("success") && !resultJson.contains("blob:")) {
                    onResult(resultJson.replace("\"", ""))
                } else {
                    // Tekrar dene - daha hızlı interval
                    Handler(Looper.getMainLooper()).postDelayed({
                        extractVideoUrl(webView, onResult, attempt + 1)
                    }, 250)
                }
            }
        }
    }

    // loadLinks fonksiyonunu da güncelleyelim
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
            element.select("button.alternative-link").map { button ->
                button.text().replace("(HDrip Xbet)", "").trim() + " $langCode" to button.attr("data-video")
            }.forEach { (source, videoID) ->
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

                    if (iframeUrl.contains("hdfilmcehennemi.mobi")) {
                        val iframedoc = app.get(iframeUrl, referer = mainUrl).document
                        val baseUri = iframedoc.location().substringBefore("/", "https://www.hdfilmcehennemi.mobi")

                        iframedoc.select("track[kind=captions]")
                            .forEach { track ->
                                val lang = track.attr("srclang").let {
                                    when (it) {
                                        "tr" -> "Turkish"
                                        "en" -> "English"
                                        "Türkçe" -> "Turkish"
                                        "İngilizce" -> "English"
                                        else -> it
                                    }
                                }
                                Log.d("kraptor_$name","altyazi track = $track")
                                val subUrl = track.attr("src").let { src ->
                                    if (src.startsWith("http")) src else "$baseUri/$src".replace("//", "/")
                                }
                                Log.d("kraptor_$name","altyazi url = $subUrl")
                                subtitleCallback(newSubtitleFile(lang, subUrl, {
                                    this.headers = mapOf("Referer" to iframeUrl)
                                }))
                            }
                    } else if (iframeUrl.contains("rplayer")) {
                        val iframeDoc = app.get(iframeUrl, referer = "$data/").document
                        Log.d("kraptor_$name","iframeDoc = $iframeDoc")
                        val regex = Regex("\"file\":\"((?:[^\"]|\"\")*)\"", options = setOf(RegexOption.IGNORE_CASE))
                        val matches = regex.findAll(iframeDoc.toString())

                        for (match in matches) {
                            val fileUrlEscaped = match.groupValues[1]
                            Log.d("kraptor_$name","altyazi fileUrlEscaped = $fileUrlEscaped")
                            val fileUrl = fileUrlEscaped.replace("\\/", "/")
                            val tamUrl = fixUrlNull(fileUrl).toString()
                            val sonUrl = "${tamUrl}/"
                            Log.d("kraptor_$name","altyazi sonurl = $sonUrl")
                            val langCode = sonUrl.substringAfterLast("_").substringBefore(".")
                            Log.d("kraptor_$name","altyazi langCode = $langCode")
                            subtitleCallback.invoke(newSubtitleFile(lang = langCode, url = tamUrl, {
                                headers = mapOf(
                                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
                                )
                            }))
                        }
                    }

                   val Videoreferer = if (iframeUrl.contains("id=")) {
                        "https://hdfilmcehennemi.mobi/"
                    } else {
                        "${mainUrl}/"
                    }

                    val iframeContent = app.get(iframeUrl, referer = Videoreferer).textLarge
                    Log.d("kraptor_$name", "iframeContent length = ${iframeContent.length}")

                    if (appContext != null) {
                        Log.d("kraptor_$name", "Using WebView to extract video URL")

                        // Suspending function olarak bekle
                        val extractedUrl = suspendCoroutine<String?> { continuation ->
                            runBlocking {
                                createWebViewAndExtract(appContext!!, iframeUrl, iframeContent) { result ->
                                    continuation.resume(result)
                                }
                            }
                        }

                        Log.d("kraptor_$name", "Final result = $extractedUrl")

                        extractedUrl?.let { videoUrl ->
                            if (videoUrl.isNotEmpty() && videoUrl != "null") {
                                callback.invoke(
                                    newExtractorLink(
                                        this@HDFilmCehennemi.name,
                                        source,
                                        videoUrl,
                                        type = ExtractorLinkType.M3U8,
                                        {
                                            referer = Videoreferer
                                            quality = Qualities.Unknown.value
                                        }
                                    )
                                )
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