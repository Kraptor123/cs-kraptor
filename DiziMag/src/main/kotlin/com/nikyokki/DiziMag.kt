package com.nikyokki

import CryptoJS
import com.fasterxml.jackson.databind.ObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.regex.Pattern

class DiziMag : MainAPI() {
    override var mainUrl = "https://dizimag.net"
    override var name = "DiziMag"
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
        "${mainUrl}/dizi/tur/aile" to "Aile",
        "${mainUrl}/dizi/tur/aksiyon-macera" to "Aksiyon-Macera",
        "${mainUrl}/dizi/tur/animasyon" to "Animasyon",
        "${mainUrl}/dizi/tur/belgesel" to "Belgesel",
        "${mainUrl}/dizi/tur/bilim-kurgu-fantazi" to "Bilim Kurgu",
        "${mainUrl}/dizi/tur/dram" to "Dram",
        "${mainUrl}/dizi/tur/gizem" to "Gizem",
        "${mainUrl}/dizi/tur/komedi" to "Komedi",
        "${mainUrl}/dizi/tur/savas-politik" to "Savaş Politik",
        "${mainUrl}/dizi/tur/suc" to "Suç",

        "${mainUrl}/film/tur/aile" to "Aile Film",
        "${mainUrl}/film/tur/animasyon" to "Animasyon Film",
        "${mainUrl}/film/tur/bilim-kurgu" to "Bilim-Kurgu Film",
        "${mainUrl}/film/tur/dram" to "Dram Film",
        "${mainUrl}/film/tur/fantastik" to "Fantastik Film",
        "${mainUrl}/film/tur/gerilim" to "Gerilim Film",
        "${mainUrl}/film/tur/gizem" to "Gizem Film",
        "${mainUrl}/film/tur/komedi" to "Komedi Film",
        "${mainUrl}/film/tur/korku" to "Korku Film",
        "${mainUrl}/film/tur/macera" to "Macera Film",
        "${mainUrl}/film/tur/romantik" to "Romantik Film",
        "${mainUrl}/film/tur/savas" to "Savaş Film",
        "${mainUrl}/film/tur/suc" to "Suç Film",
        "${mainUrl}/film/tur/tarih" to "Tarih Film",
        "${mainUrl}/film/tur/vahsi-bati" to "Vahşi Batı Film",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val mainReq = app.get("${request.data}/${page}")

        //val document = mainReq.document.body()
        val document = Jsoup.parse(mainReq.body.string())
        val home = document.select("div.poster-long").mapNotNull { it.diziler() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.diziler(): SearchResponse? {
        val title =
            this.selectFirst("div.poster-long-subject h2")?.text() ?: return null
        val href =
            fixUrlNull(this.selectFirst("div.poster-long-subject a")?.attr("href"))
                ?: return null
        val posterUrl =
            fixUrlNull(this.selectFirst("div.poster-long-image img")?.attr("data-src"))

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

    private fun Element.toPostSearchResult(): SearchResponse? {
        val title = this.selectFirst("span")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        if (href.contains("/dizi/")) {
            return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            return newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchReq = app.post(
            "${mainUrl}/search",
            data = mapOf(
                "query" to query
            ),
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "X-Requested-With" to "XMLHttpRequest",
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                "Accept-Language" to "en-US,en;q=0.5"
            ),
            referer = "${mainUrl}/"
        ).parsedSafe<SearchResult>()

        if (searchReq?.success != true) {
            throw ErrorLoadingException("Invalid Json response")
        }

        val searchDoc = searchReq.theme

        val document = Jsoup.parse(searchDoc.toString())
        val results = mutableListOf<SearchResponse>()

        document.select("ul li").forEach { listItem ->
            val href = listItem.selectFirst("a")?.attr("href")
            if (href != null && (href.contains("/dizi/") || href.contains("/film/"))) {
                val result = listItem.toPostSearchResult()
                result?.let { results.add(it) }
            }
        }
        return results
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val mainReq = app.get(url, referer = mainUrl)
        val document = mainReq.document
        val title = document.selectFirst("div.page-title h1")?.selectFirst("a")?.text() ?: return null
        val orgtitle = document.selectFirst("div.page-title p")?.text() ?: ""
        var tit = "$title - $orgtitle"
        val poster =
            fixUrlNull(document.selectFirst("div.series-profile-image img")?.attr("src"))
        val year =
            document.selectFirst("h1 span")?.text()?.substringAfter("(")?.substringBefore(")")
                ?.toIntOrNull()
        val rating = document.selectFirst("span.color-imdb")?.text()?.trim()?.toRatingInt()
        val duration =
            document.selectXpath("//span[text()='Süre']//following-sibling::p").text().trim()
                .split(" ").first().toIntOrNull()
        val description = document.selectFirst("div.series-profile-summary p")?.text()?.trim()
        val tags = document.selectFirst("div.series-profile-type")?.select("a")
            ?.mapNotNull { it.text().trim() }
        val trailer = document.selectFirst("div.series-profile-trailer")?.attr("data-yt")
        val actors = mutableListOf<Actor>()
        document.select("div.series-profile-cast li").forEach {
            val img = fixUrlNull(it.selectFirst("img")?.attr("data-src"))
            val name = it.selectFirst("h5.truncate")?.text()?.trim() ?: return null
            actors.add(Actor(name, img))
        }
        if (url.contains("/dizi/")) {
            val episodeses = mutableListOf<Episode>()
            var szn = 1
            for (sezon in document.select("div.series-profile-episode-list")) {
                var blm = 1
                for (bolum in sezon.select("li")) {
                    val epName = bolum.selectFirst("h6.truncate a")?.text() ?: continue
                    val epHref = fixUrlNull(bolum.select("h6.truncate a").attr("href")) ?: continue
                    val epEpisode = blm++
                    val epSeason = szn
                    episodeses.add(
                        Episode(
                            data = epHref,
                            name = epName,
                            season = epSeason,
                            episode = epEpisode
                        )
                    )
                }
                szn++
            }

            return newTvSeriesLoadResponse(tit, url, TvType.TvSeries, episodeses) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                addActors(actors)
                addTrailer("https://www.youtube.com/embed/${trailer}")
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                this.duration = duration
                addActors(actors)
                addTrailer("https://www.youtube.com/embed/${trailer}")
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Referer" to "$mainUrl/"
        )

        android.util.Log.d("dzmg", "loadLinks: Starting with data URL - $data")

        val aa = app.get(mainUrl)
        val ciSession = aa.cookies["ci_session"].toString()
        android.util.Log.d("dzmg", "ci_session cookie obtained: ${ciSession.take(5)}...") // Kısaltılmış log

        val document = app.get(
            data, headers = headers, cookies = mapOf(
                "ci_session" to ciSession
            )
        ).document

        val iframe = fixUrlNull(document.selectFirst("div#tv-spoox2 iframe")?.attr("src")) ?: run {
            android.util.Log.e("dzmg", "iframe src not found in document")
            return false
        }
        android.util.Log.d("dzmg", "iframe URL found: $iframe")

        val docum = app.get(iframe, headers = headers, referer = "$mainUrl/").document
        android.util.Log.d("dzmg", "iframe content fetched, scanning scripts...")

        docum.select("script").forEach { sc ->
            if (sc.toString().contains("bePlayer")) {
                android.util.Log.d("dzmg", "bePlayer script found")
                val pattern = Pattern.compile("bePlayer\\('(.*?)', '(.*?)'\\)")
                val matcher = pattern.matcher(sc.toString().trimIndent())
                if (matcher.find()) {
                    android.util.Log.d("dzmg", "bePlayer pattern matched successfully")
                    val key = matcher.group(1)
                    val jsonCipher = matcher.group(2)
                    android.util.Log.d("dzmg", "decryption key: ${key?.take(3)}..., cipher: ${jsonCipher?.take(10)}...")

                    try {
                        val cipherData = ObjectMapper().readValue(
                            jsonCipher?.replace("\\/", "/"),
                            Cipher::class.java
                        )
                        android.util.Log.d("dzmg", "cipher data parsed - iv: ${cipherData.iv.take(5)}..., s: ${cipherData.s}")

                        val decrypt = key?.let { CryptoJS.decrypt(it, cipherData.ct, cipherData.iv, cipherData.s) }
                        android.util.Log.d("dzmg", "decryption result: ${decrypt?.take(50)}...")

                        val jsonData = ObjectMapper().readValue(decrypt, JsonData::class.java)
                        android.util.Log.d("dzmg", "JSON data parsed with ${jsonData.strSubtitles?.size} subtitles")

                        jsonData.strSubtitles?.let { subtitles -> // Null kontrolü ekledik
                            for (sub in subtitles) {
                                android.util.Log.d("dzmg", "adding subtitle: ${sub.label} (${sub.file})")
                                subtitleCallback.invoke(
                                    SubtitleFile(
                                        lang = sub.label.toString(),
                                        url = "https://epikplayer.xyz${sub.file}"
                                    )
                                )
                            }
                        }


                        android.util.Log.d("dzmg", "fetching m3u8 content from ${jsonData.videoLocation}")
                        val m3u8Content = app.get(
                            jsonData.videoLocation,
                            referer = iframe,
                            headers = mapOf("Accept" to "*/*", "Referer" to iframe)
                        ).document.body()

                        val regex = Regex("#EXT-X-STREAM-INF:.*? (https?://\\S+)")
                        val matchResult = regex.find(m3u8Content.text())
                        val m3uUrl = matchResult?.groupValues?.get(1) ?: ""
                        android.util.Log.d("dzmg", "m3u8 URL extracted: ${m3uUrl.take(50)}...")
                        val myHeaders = mapOf("Accept" to "*/*", "Referer" to iframe)

                        if (m3uUrl.isNotEmpty()) {
                            android.util.Log.d("dzmg", "invoking callback with m3u8 URL")
                            callback.invoke(
                                newExtractorLink(
                                    source = this.name,
                                    name = this.name,
                                    url = jsonData.videoLocation,
                                    type = ExtractorLinkType.M3U8 // isM3u8 = true yerine ExtractorLinkType belirtiliyor
                                ) {
                                    this.headers = myHeaders
                                    quality = Qualities.Unknown.value
                                }
                            )
                        } else {
                            android.util.Log.w("dzmg", "m3u8 URL extraction failed")
                        }

                        android.util.Log.d("dzmg", "invoking callback with videoLocation")
                        callback.invoke(
                            newExtractorLink(
                                source = this.name,
                                name = this.name,
                                url = jsonData.videoLocation,
                                type = ExtractorLinkType.M3U8 // isM3u8 = true yerine ExtractorLinkType belirtiliyor
                            ) {
                                this.headers = myHeaders
                                quality = Qualities.Unknown.value
                            }
                        )

                    } catch (e: Exception) {
                        android.util.Log.e("dzmg", "decryption/parsing error: ${e.stackTraceToString()}")
                    }
                } else {
                    android.util.Log.w("dzmg", "bePlayer pattern match failed")
                }
            }
        }

        android.util.Log.d("dzmg", "fallback to loadExtractor")
        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}