// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.utils.requireReferer
import org.jsoup.nodes.Element
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class FilmMakinesi : MainAPI() {
    override var mainUrl = "https://filmmakinesi.de"
    override var name = "FilmMakinesi"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    // ! CloudFlare bypass
    override var sequentialMainPage =
        true // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay = 50L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 50L  // ? 0.05 saniye

    private var appContext: Context? = null

    fun setContext(context: Context) {
        this.appContext = context
    }

    override val mainPage = mainPageOf(
        "${mainUrl}/filmler/" to "Son Filmler",
        "${mainUrl}/kanal/netflix/" to "Netflix",
        "${mainUrl}/kanal/disney/" to "Disney",
        "${mainUrl}/kanal/amazon/" to "Amazon",
        "${mainUrl}/film-izle/olmeden-izlenmesi-gerekenler/" to "Ölmeden İzle",
        "${mainUrl}/film-izle/aksiyon-filmleri-izle/" to "Aksiyon",
        "${mainUrl}/film-izle/bilim-kurgu-filmi-izle/" to "Bilim Kurgu",
        "${mainUrl}/film-izle/macera-filmleri/" to "Macera",
        "${mainUrl}/film-izle/komedi-filmi-izle/" to "Komedi",
        "${mainUrl}/film-izle/romantik-filmler-izle/" to "Romantik",
        "${mainUrl}/film-izle/belgesel/" to "Belgesel",
        "${mainUrl}/film-izle/fantastik-filmler-izle/" to "Fantastik",
        "${mainUrl}/film-izle/polisiye-filmleri-izle/" to "Polisiye Suç",
        "${mainUrl}/film-izle/korku-filmleri-izle-hd/" to "Korku",
        // "${mainUrl}/film-izle/savas/page/"                        to "Tarihi ve Savaş",
        // "${mainUrl}/film-izle/gerilim-filmleri-izle/page/"        to "Gerilim Heyecan",
        // "${mainUrl}/film-izle/gizemli/page/"                      to "Gizem",
        // "${mainUrl}/film-izle/aile-filmleri/page/"                to "Aile",
        // "${mainUrl}/film-izle/animasyon-filmler/page/"            to "Animasyon",
        // "${mainUrl}/film-izle/western/page/"                      to "Western",
        // "${mainUrl}/film-izle/biyografi/page/"                    to "Biyografik",
        // "${mainUrl}/film-izle/dram/page/"                         to "Dram",
        // "${mainUrl}/film-izle/muzik/page/"                        to "Müzik",
        // "${mainUrl}/film-izle/spor/page/"                         to "Spor"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val base = request.data.trimEnd('/')
        val url = if (page == 1) base else "$base/sayfa/$page/"
        val document = app.get(url).document
        val home = document.select("div.item-relative").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("div.item-relative a.item")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.thumbnail-outer img.thumbnail")?.attr("src")) ?: fixUrlNull(
            this.selectFirst("img.thumbnail")?.attr("src")
        )
        val puan = this.selectFirst("div.rating")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score = Score.from10(puan)
        }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val title = this.select("div.title").last()?.text() ?: return null
        val href = fixUrlNull(this.select("div.item-relative a.item").last()?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.thumbnail-outer img.thumbnail")?.attr("src"))
        val puan = this.selectFirst("div.rating")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score = Score.from10(puan)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/?s=${query}").document
        Log.d("kraptor_$name", "arama = $document")
        return document.select("div.item-relative").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.content h1.title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description = document.select("div.info-description p").last()?.text()?.trim()
        val tags = document.select("div.type a").map { it.text() }
        val imdbScore = document.selectFirst("div.info b")?.text()?.trim()
        val year = document.selectFirst("span.date a")?.text()?.trim()?.toIntOrNull()

        val durationText = document.selectFirst("div.time")?.text()?.trim() ?: ""
        val duration = if (durationText.startsWith("Süre:")) {
            // "Süre: 155 Dakika" gibi bir metni işliyoruz
            val durationValue = durationText.removePrefix("Süre:").trim().split(" ")[0]
            durationValue.toIntOrNull() ?: 0
        } else {
            0
        }
        val recommendations = document.select("div.item-relative").mapNotNull { it.toRecommendResult() }
        val actors = document.select("div.content a.cast")  // Tüm a.cast öğelerini al
            .map { Actor(it.text().trim()) }  // Her birini Actor nesnesine dönüştür

        val trailer = document.selectFirst("a.trailer-button")?.attr("data-video_url")

        val bolumler = document.select("div.col-12.col-sm-6.col-md-3").map { bolum ->
            val bolumHref = fixUrlNull(bolum.selectFirst("a")?.attr("href")).toString()
            val bolumName =
                bolum.selectFirst("div.ep-details span")?.text() ?: bolum.selectFirst("div.ep-title")?.text()
            val bolum = bolumHref.substringAfter("bolum-").substringBefore("/").toIntOrNull()
            val sezon = bolumHref.substringAfter("sezon-").substringBefore("/").toIntOrNull()
            newEpisode(bolumHref, {
                this.name = bolumName
                this.episode = bolum
                this.season = sezon
            })

        }

        return if (url.contains("dizi")) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, bolumler) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(imdbScore)
                this.duration = duration
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(imdbScore)
                this.duration = duration
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    private fun cleanupWebView(wv: WebView) {
        try {
            wv.stopLoading()
            wv.setWebChromeClient(null)   // nullable setter kullan
            wv.webViewClient = object : WebViewClient() {}
            wv.removeAllViews()
            wv.clearHistory()
            wv.loadUrl("about:blank")
            wv.destroy()
        } catch (ignored: Throwable) {}
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

        val wv = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    extractWithDelay(view, { result ->
                        onResult(result)
                        // işi bitince temizle (main thread)
                        Handler(Looper.getMainLooper()).post {
                            Log.d("kraptor_filmmak", "webview temizlendi")
                            cleanupWebView(this@apply)
                        }
                    }, 0)
                }
            }
            loadDataWithBaseURL(baseUrl, modifiedHtml, "text/html", "UTF-8", null)
        }

        return@withContext wv
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
                    }, 200)
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
        callback: (ExtractorLink) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        Log.d("kraptor_$name", "data » $data")
        val document = app.get(data).document
//        Log.d("kraptor_$name", "document = $document")
        val iframe = document.selectFirst("div.after-player iframe")?.attr("data-src") ?: ""
        Log.d("kraptor_$name", "iframe = $iframe")
        val iframeGet = app.get(iframe, referer = "${mainUrl}/")
        val iframeText = iframeGet.text
        if (appContext != null && iframe.contains("filmmakinesi")) {
            Log.d("kraptor_$name", "Using WebView to extract config...")

            val configJson = suspendCoroutine<String?> { continuation ->
                runBlocking {
                    createWebViewAndExtract(appContext!!, iframe, iframeText) { result ->
                        continuation.resume(result)
                    }
                }
            }

            configJson?.let { configStr ->
                val configObj = JSONObject(configStr)

                if (configObj.has("tracks")) {
                    val tracks = configObj.getJSONArray("tracks")
                    for (i in 0 until tracks.length()) {
                        val trackObj = tracks.getJSONObject(i)
                        val kind = trackObj.optString("kind", "")

                        if (kind == "captions") {
                            val subFile = trackObj.optString("file", "")
                            val label = trackObj.optString("label", "")
                            val language = trackObj.optString("language", "")

                            val base = iframe.split("/").take(3).joinToString("/")

                            if (subFile.isNotEmpty()) {
                                val fullSubUrl = if (subFile.startsWith("http")) {
                                    subFile
                                } else {
                                    "$base$subFile"
                                }

                                val cleanLang = when {
                                    label.contains("İngilizce") || language == "en" -> "English"
                                    label.contains("Türkçe") || language == "tr" -> "Turkish"
                                    language == "forced" -> "Forced"
                                    language.isNotEmpty() -> language
                                    else -> "Unknown"
                                }
                                val subHeaders = if (iframe.contains("close")) {
                                    mapOf(
                                        "Referer" to "${base}/",
                                        "Accept" to "*/*")
                                    } else {
                                    mapOf(
                                        "Referer" to "${base}/",
                                        "Accept" to "*/*")
                                    }

                                Log.d("kraptor_$name", "altyazi = $fullSubUrl")

                                 subtitleCallback.invoke(newSubtitleFile(
                                    cleanLang,
                                    fullSubUrl,
                                     {
                                         headers = subHeaders
                                     }
                                )
                                )
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
                        if (videoUrl.isNotEmpty() && videoUrl.startsWith("http")) {

                            val refererSon = if (videoUrl.contains("cdnimages")) {
                                "https://closeload.filmmakinesi.de/"
                            }else if (videoUrl.contains("rapidrame")) {
                                "https://rapid.filmmakinesi.de/"
                            }else if (videoUrl.contains("playmix")) {
                                "https://closeload.filmmakinesi.de/"
                            } else {
                                "${mainUrl}/"
                            }

                            val sourceName = if (videoUrl.contains("cdnimages")){
                                "Close"
                            } else if (videoUrl.contains("rapidrame")) {
                                "Rapidrame"
                            } else{
                                "FilmMakinesi"
                            }

                            callback.invoke(
                                newExtractorLink(
                                    source = "FilmMakinesi $sourceName",
                                    name = "FilmMakinesi $sourceName",
                                    url = videoUrl,
                                    type = ExtractorLinkType.M3U8,
                                    {
                                        this.referer = refererSon
                                        this.quality = Qualities.Unknown.value
                                    }
                                ))
                        }
                    }
                }else {
                    loadExtractor(iframe, subtitleCallback, callback)
                }
            }
        }
        return@withContext true
    }
}