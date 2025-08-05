// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Base64
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class FilmIzle : MainAPI() {
    override var mainUrl = "https://filmizle.so"
    override var name = "FilmIzle"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Yeni Filmler",
        "${mainUrl}/en-cok-izlenen-filmler-izle/" to "En Çok İzlenenler",
        "${mainUrl}/tur/aile/" to "Aile",
        "${mainUrl}/tur/aksiyon/" to "Aksiyon",
        "${mainUrl}/tur/animasyon/" to "Animasyon",
        "${mainUrl}/tur/belgeseller/" to "Belgesel",
        "${mainUrl}/tur/bilim-kurgu/" to "Bilim",
        "${mainUrl}/tur/biyografi/" to "Biyografi",
        "${mainUrl}/tur/dram/" to "Dram",
        "${mainUrl}/tur/fantastik/" to "Fantastik",
        "${mainUrl}/tur/gerilim/" to "Gerilim",
        "${mainUrl}/tur/gizem/" to "Gizem",
        "${mainUrl}/tur/komedi/" to "Komedi",
        "${mainUrl}/tur/korku/" to "Korku",
        "${mainUrl}/tur/macera/" to "Macera",
        "${mainUrl}/tur/muzik/" to "Müzik",
        "${mainUrl}/tur/muzikal/" to "Müzikal",
        "${mainUrl}/tur/romantik/" to "Romantik",
        "${mainUrl}/tur/savas-film/" to "Savaş",
        "${mainUrl}/tur/spor/" to "Spor",
        "${mainUrl}/tur/suc/" to "Suç",
        "${mainUrl}/tur/tarih-film/" to "Tarih",
        "${mainUrl}/tur/western-kovboy/" to "Western"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get(request.data).document
        } else {
            app.get("${request.data}page/$page/").document
        }
        val home = document.select("div.movie_box").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("span.title h2")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan      = this.selectFirst("span.box.imdb")?.text()?.trim()
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/${query}").document

        return document.select("div.movie_box").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("span.title h2")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan      = this.selectFirst("span.box.imdb")?.text()?.trim()
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    // Geliştirilmiş Base64 işleme fonksiyonları
    private fun decodeFilmIzleBase64(input: String): String? {
        val magicString = "BSZtFmcmlGP".reversed() // "PGliFlcmtZSB" olacak
        val prefixCheck = "PGltZyB3aWR0aD0iMTAwJSIgaGVpZ2"

        return try {
            val fullString = if (!input.startsWith(prefixCheck)) {
                magicString + input
            } else {
                input
            }

            String(
                Base64.decode(
                    fullString.replace("\n", ""),
                    Base64.NO_WRAP or Base64.URL_SAFE
                ),
                Charsets.UTF_8
            )
        } catch (e: Exception) {
            try {
                String(
                    Base64.decode(
                        input.padEnd((input.length + 3) and -4, '='),
                        Base64.DEFAULT
                    ),
                    Charsets.UTF_8
                )
            } catch (e: Exception) {
                null
            }
        }
    }


    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title =
            document.selectFirst("ul.bottom > li:nth-child(1) > span:nth-child(2) > strong")?.text()?.trim().toString()
        val poster = fixUrlNull(document.selectFirst("div.image img")?.attr("data-src"))
        val description = document.selectFirst(".desc p")?.text()?.trim()
        val year = document.selectFirst("ul.top > li:nth-child(4) > span:nth-child(2) > a")?.text()?.toIntOrNull()
        val tags = document.select("ul.bottom > li:nth-child(3) > span").map { it.text() }
        val rating = document.selectFirst(".imdb > span:nth-child(2)")?.text()
        val duration = document.selectFirst("ul.top > li:nth-child(3) > span:nth-child(2)")?.text()?.split(" ")?.first()
            ?.toIntOrNull()
        val actors = document.select("li.actors span").map { Actor(it.text()) }
        val isSeries = document.selectFirst("li.psec")?.text()?.contains("Bölüm", ignoreCase = true) == true
        val text = app.get(url).text
        val regex = Regex("""pdata\['(prt_.*?)'\]\s*=\s*'(.*?)';""")
        val matches = regex.findAll(text)
        val iframeUrls = mutableListOf<String>()
        var trailerUrl: String? = null
        matches.forEach { match ->
            val partName = match.groupValues[1]
            val raw = match.groupValues[2]

            when {
                partName == "prt_fragman0" -> {
                    val decodedHtml = decodeFilmIzleBase64(raw)
                    decodedHtml?.let { html ->
                        Regex("""youtube\.com/embed/([^"\?]+)""").find(html)?.let {
                            trailerUrl = "https://youtu.be/${it.groupValues[1]}"
                            Log.d("flmcx", "Trailer URL found: $trailerUrl")
                        }
                    }
                }
                partName.startsWith("prt_") -> {
                    val decodedHtml = decodeFilmIzleBase64(raw)
                    decodedHtml?.let { html ->
                        Regex("""src=["']([^"']+)""").find(html)?.let {
                            iframeUrls.add(it.groupValues[1])
                            Log.d("flmcx", "Content URL added: ${it.groupValues[1]}")
                        }
                    }
                }
            }
        }
        return if (isSeries) {
            val episodes = iframeUrls.mapIndexed { index, url ->
                newEpisode(url) {
                    name = "Bölüm ${index + 1}"
                    season = 1
                    episode = index + 1
                }
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                posterUrl = poster
                this.year = year
                plot = description
                this.score = Score.from10(rating)
                this.tags = tags
                this.duration = duration
                addActors(actors)
                trailerUrl?.let { addTrailer(it) }
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, iframeUrls.firstOrNull() ?: url) {
                posterUrl = poster
                plot = description
                this.year = year
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                addActors(actors)
                trailerUrl?.let { addTrailer(it) }
            }
        }
    }
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("flmcx", "loadLinks data: $data")

        val document = app.get(data).document
        val text = app.get(data).text

        if (data.contains("player.vidmody.com")) {
            try {
                loadExtractor(data, subtitleCallback, callback)
                return true
            } catch (e: Exception) {
            }
        }
        if (data.contains("streamplayer")) {
            try {
                val client = OkHttpClient.Builder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build()

                val req = Request.Builder()
                    .url(data)
                    .header("Referer", "https://filmizle.so")  // gerekiyorsa ekleyin
                    .build()

                val resp = client.newCall(req).execute()

                val embedUrl = resp.header("Location").toString()

                Log.d("flmcx", "embed $embedUrl")

                val vidmolyurl = app.get(embedUrl, referer = "https://filmizle.so", headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
                    "Sec-Fetch-Dest" to "iframe",
                    "Sec-Fetch-Mode" to "navigate",
                    "Sec-Fetch-Site" to "cross-site",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                )).document

//                Log.d("flmcx", "vidmoly $vidmolyurl")

                val html = vidmolyurl.outerHtml()

                val setupRegex = Regex("""player\.setup\(\s*\{([\s\S]*?)\}\s*\)""")
                val setupMatch = setupRegex.find(html)
                    ?: throw IllegalStateException("player.setup bloğu bulunamadı")
                val setupBlock = setupMatch.groupValues[1]

                val sourcesRegex = Regex("""sources\s*:\s*(\[[\s\S]*?\])""")
                val labelRegex   = Regex("""label\s*:\s*"([^"]+)"""")
                val sourcesJson  = sourcesRegex.find(setupBlock)
                    ?.groupValues?.get(1) ?: throw IllegalStateException("sources dizisi bulunamadı")
                val labelString  = labelRegex.find(setupBlock)
                    ?.groupValues?.get(1) ?: throw IllegalStateException("label değeri bulunamadı")
                val labelValue: Int? = Regex("""(\d+)""")
                    .find(labelString)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()


                val sourcesArr = JSONArray(sourcesJson)
                val fileUrl    = sourcesArr.getJSONObject(0).getString("file")

                Log.d("flmcx", "fileurl $fileUrl")

                callback.invoke(newExtractorLink(
                    source = data,
                    name   = "VidMoly",
                    url    = fileUrl,
                    type   = ExtractorLinkType.M3U8
                ) {
                    quality = labelValue ?: Qualities.Unknown.value
                    headers = mapOf(
                        "Referer"    to embedUrl,
                        "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/112.0.0.0 Safari/537.36"
                    )
                })

                return true
            } catch (e: Exception) {
            }
        }

        val regex = Regex("""pdata\['(prt_.*?)'\]\s*=\s*'(.*?)';""")
        val matches = regex.findAll(text)

        var foundLinks = false

        for (match in matches) {
            val partName = match.groupValues[1]
            val rawData = match.groupValues[2]

            Log.d("flmcx", "Processing part: $partName")

            val decodedHtml = decodeFilmIzleBase64(rawData)
            if (decodedHtml != null) {
                val srcMatch = Regex("""src=["']([^"']+)""").find(decodedHtml)
                val iframeUrl = srcMatch?.groupValues?.getOrNull(1) ?: data

                if (iframeUrl.isNotEmpty()) {
                    Log.d("flmcx", "Found iframe URL: $iframeUrl")
                    try {
                        loadExtractor(
                            iframeUrl,
                            data,
                            subtitleCallback,
                            callback
                        )
                        foundLinks = true
                        continue
                    } catch (e: Exception) {
                        Log.w("flmcx", "Generic extractor failed: ${e.message}")
                    }
                }
            }
        }

        // Eğer hala link bulunamadıysa alternatif yöntem
        if (!foundLinks) {
            Log.d("flmcx", "Trying alternative decoding method")

            document.select("script:containsData(pdata)").forEach { script ->
                val scriptData = script.data()
                val altMatch = Regex("""atob\(['"](.*?)['"]\)""").find(scriptData)
                val rawBase64 = altMatch?.groupValues?.getOrNull(1)

                val decoded = rawBase64?.let { decodeFilmIzleBase64(it) }
                if (decoded != null) {
                    val srcMatch = Regex("""src=["']([^"']+)""").find(decoded)
                    val iframeUrl = srcMatch?.groupValues?.getOrNull(1)

                    if (!iframeUrl.isNullOrEmpty()) {
                        try {
                            loadExtractor(
                                iframeUrl,
                                data,
                                subtitleCallback,
                                callback
                            )
                            foundLinks = true
                        } catch (e: Exception) {
                            Log.w("flmcx", "Alternative extractor failed: ${e.message}")
                        }
                    }
                }
            }
        }

        return foundLinks
    }
}