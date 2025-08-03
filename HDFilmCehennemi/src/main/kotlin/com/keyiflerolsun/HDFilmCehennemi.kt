package com.keyiflerolsun

import android.util.Base64
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.*
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.nio.charset.StandardCharsets

class HDFilmCehennemi : MainAPI() {
    override var mainUrl              = "https://www.hdfilmcehennemi.nl"
    override var name                 = "HDFilmCehennemi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = true
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries)

    // ! CloudFlare bypass
    override var sequentialMainPage = true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay       = 50L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 50L  // ? 0.05 saniye

    // ! CloudFlare v2
    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request  = chain.request()
            val response = chain.proceed(request)
            val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

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
        "${mainUrl}/load/page/1/home/"                        to "Yeni Eklenen Filmler",
        "${mainUrl}/load/page/1/categories/nette-ilk-filmler/"            to "Nette İlk Filmler",
        "${mainUrl}/load/page/1/home-series/"                             to "Yeni Eklenen Diziler",
        "${mainUrl}/load/page/1/categories/tavsiye-filmler-izle2/"        to "Tavsiye Filmler",
        "${mainUrl}/load/page/1/imdb7/"                                   to "IMDB 7+ Filmler",
        "${mainUrl}/load/page/1/mostLiked/"                               to "En Çok Beğenilenler",
        "${mainUrl}/load/page/1/genres/aile-filmleri-izleyin-6/"          to "Aile Filmleri",
        "${mainUrl}/load/page/1/genres/aksiyon-filmleri-izleyin-5/"       to "Aksiyon Filmleri",
        "${mainUrl}/load/page/1/genres/animasyon-filmlerini-izleyin-5/"   to "Animasyon Filmleri",
        "${mainUrl}/load/page/1/genres/belgesel-filmlerini-izle-1/"       to "Belgesel Filmleri",
        "${mainUrl}/load/page/1/genres/bilim-kurgu-filmlerini-izleyin-3/" to "Bilim Kurgu Filmleri",
        "${mainUrl}/load/page/1/genres/komedi-filmlerini-izleyin-1/"      to "Komedi Filmleri",
        "${mainUrl}/load/page/1/genres/korku-filmlerini-izle-4/"          to "Korku Filmleri",
        "${mainUrl}/load/page/1/genres/romantik-filmleri-izle-2/"         to "Romantik Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // URL'deki sayfa numarasını güncelle
        val url = if (page == 1) {
            request.data
                .replace("/load/page/1/genres/","/tur/")
                .replace("/load/page/1/categories/","/category/")
                .replace("/load/page/1/imdb7/","/imdb-7-puan-uzeri-filmler/")
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

        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan      = this.selectFirst("span.imdb")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
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

            val title     = document.selectFirst("h4.title")?.text() ?: return@forEach
            val href      = fixUrlNull(document.selectFirst("a")?.attr("href")) ?: return@forEach
            val posterUrl = fixUrlNull(document.selectFirst("img")?.attr("src")) ?:
            fixUrlNull(document.selectFirst("img")?.attr("data-src"))
            val puan      = document.selectFirst("span.imdb")?.text()?.trim()

            searchResults.add(
                newMovieSearchResponse(title, href, TvType.Movie) {
                    this.posterUrl = posterUrl?.replace("/thumb/", "/list/")
                    this.score     = Score.from10(puan)
                }
            )
        }

        return searchResults
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document



        val title       = document.selectFirst("h1.section-title")?.text()?.substringBefore(" izle") ?: return null
        val poster      = fixUrlNull(document.select("aside.post-info-poster img.lazyload").lastOrNull()?.attr("data-src"))
        val tags        = document.select("div.post-info-genres a").map { it.text() }
        val year        = document.selectFirst("div.post-info-year-country a")?.text()?.trim()?.toIntOrNull()
        val tvType      = if (document.select("div.seasons").isEmpty()) TvType.Movie else TvType.TvSeries
        val description = document.selectFirst("article.post-info-content > p")?.text()?.trim()
        val rating      = document.selectFirst("div.post-info-imdb-rating span")?.text()?.substringBefore("(")?.trim()
        val actors      = document.select("div.post-info-cast a").map {
            Actor(it.selectFirst("strong")!!.text(), it.select("img").attr("data-src"))
        }

        val recommendations = document.select("div.section-slider-container div.slider-slide").mapNotNull {
            val recName      = it.selectFirst("a")?.attr("title") ?: return@mapNotNull null
            val recHref      = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val recPosterUrl = fixUrlNull(it.selectFirst("img")?.attr("data-src")) ?:
            fixUrlNull(it.selectFirst("img")?.attr("src"))
            val puan      = it.selectFirst("span.imdb")?.text()?.trim()

            newTvSeriesSearchResponse(recName, recHref, TvType.TvSeries) {
                this.posterUrl = recPosterUrl
                this.score     = Score.from10(puan)
            }
        }

        return if (tvType == TvType.TvSeries) {
            val trailer  = document.selectFirst("div.post-info-trailer button")?.attr("data-modal")
                ?.substringAfter("trailer/")?.let { "https://www.youtube.com/embed/$it" }
            val episodes = document.select("div.seasons-tab-content a").mapNotNull {
                val epName    = it.selectFirst("h4")?.text()?.trim() ?: return@mapNotNull null
                val epHref    = fixUrlNull(it.attr("href")) ?: return@mapNotNull null
                val epEpisode = Regex("""(\d+)\. ?Bölüm""").find(epName)?.groupValues?.get(1)?.toIntOrNull()
                val epSeason  = Regex("""(\d+)\. ?Sezon""").find(epName)?.groupValues?.get(1)?.toIntOrNull() ?: 1

                newEpisode(epHref) {
                    this.name = epName
                    this.season = epSeason
                    this.episode = epEpisode
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl       = poster
                this.year            = year
                this.plot            = description
                this.tags            = tags
                this.score           = Score.from10(rating)
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            val trailer = document.selectFirst("div.post-info-trailer button")?.attr("data-modal")
                ?.substringAfter("trailer/")?.let { "https://www.youtube.com/embed/$it" }

            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl       = poster
                this.year            = year
                this.plot            = description
                this.tags            = tags
                this.score = Score.from10(rating)
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    private suspend fun invokeLocalSource(
        source: String,
        url: String,
//        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val script    = app.get(url, referer = "${mainUrl}/").document.select("script")
            .find { it.data().contains("sources:") }?.data() ?: return

        Log.d("fix","urlne $url")

        val videoData = getAndUnpack(script).substringAfter("file_link=\"").substringBefore("\";")
//        val subData   = script.substringAfter("tracks: [").substringBefore("]")

//        Log.d("fix","subdata $subData")

        callback.invoke(
            newExtractorLink(
                source  = source,
                name    = source,
                url     = base64Decode(videoData),
                type    = INFER_TYPE
            ) {
                // DSL builder kullanarak referer ve quality ayarı
                this.referer = "${mainUrl}/"
                this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Norton/124.0.0.0")
                this.quality = Qualities.Unknown.value
            }
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name","data = $data")
        val document = app.get(data).document
        val iframealak = fixUrlNull(
            document.selectFirst(".close")?.attr("data-src")
                ?: document.selectFirst(".rapidrame")?.attr("data-src")
        ).toString()
        Log.d("kraptor_$name","iframealak = $iframealak")

        // Process hdfilmcehennemi.mobi subtitles
        if (iframealak.contains("hdfilmcehennemi.mobi")) {
            val iframedoc = app.get(iframealak, referer = mainUrl).document
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
                    val subUrl = track.attr("src").let { src ->
                        if (src.startsWith("http")) src else "$baseUri/$src".replace("//", "/")
                    }
                    subtitleCallback(SubtitleFile(lang, subUrl))
                }
        } else if (iframealak.contains("rplayer")) {
            val iframeDoc = app.get(iframealak, referer = "$data/").document
//            Log.d("kraptor_$name","iframeDoc = $iframeDoc")
            val regex = Regex("\"file\":\"((?:[^\"]|\"\")*)\"", options = setOf(RegexOption.IGNORE_CASE))
            val matches = regex.findAll(iframeDoc.toString())

            for (match in matches) {
                val fileUrlEscaped = match.groupValues[1]
                val fileUrl = fileUrlEscaped.replace("\\/", "/")
                val tamUrl = fixUrlNull(fileUrl).toString()
                val sonUrl = "${tamUrl}/"
                val langCode = when {
                    fileUrl.contains("Turkish", ignoreCase = true) -> "Turkish"
                    fileUrl.contains("English", ignoreCase = true) -> "English"
                    else -> "Unknown"
                }
                subtitleCallback.invoke(SubtitleFile(lang = langCode, url = sonUrl))
            }
        }


        Log.d("kraptor_$name", "iframegeldi mi $iframealak")

        document.select("div.alternative-links").map { element ->
            element to element.attr("data-lang").uppercase()
        }.forEach { (element, langCode) ->
            element.select("button.alternative-link").map { button ->
                button.text().replace("(HDrip Xbet)", "").trim() + " $langCode" to button.attr("data-video")
            }.forEach { (source, videoID) ->
                val apiGet = app.get(
                    "${mainUrl}/video/$videoID/",
                    headers = mapOf(
                        "Content-Type"     to "application/json",
                        "X-Requested-With" to "fetch"
                    ),
                    referer = data
                ).text


                var iframe = Regex("""data-src=\\"([^"]+)""").find(apiGet)?.groupValues?.get(1)!!.replace("\\", "")

                Log.d("kraptor_$name", "iframe mi $iframe")

                val iframeGet = app.get(iframe, referer = "${mainUrl}/").text

                val evalRegex = Regex("""eval\((.*?\\.*?\\.*?\\.*?\{\}\)\))""", RegexOption.DOT_MATCHES_ALL)
                val packedCode = evalRegex.find(iframeGet)?.value
                val unpackedJs = JsUnpacker(packedCode).unpack().toString()
                Log.d("kraptor_$name", "unpackedJs $unpackedJs")
                val dchelloVar = if (unpackedJs.contains("dc_hello")) {
                    "var"
                } else {
                    "yok"
                }
                Log.d("kraptor_$name", "dchelloVar $dchelloVar")
                val dcRegex = if (dchelloVar.contains("var")) {
                    Regex(pattern = "dc_hello\\(\"([^\"]*)\"\\)", options = setOf(RegexOption.IGNORE_CASE))
                } else {
                    Regex("""dc_[a-zA-Z0-9_]+\(\[(.*?)\]\)""", RegexOption.DOT_MATCHES_ALL)
                }
                val match = dcRegex.find(unpackedJs)
//                Log.d("kraptor_$name", "match $match")

                val realUrl = if (dchelloVar.contains("var")) {
                    val parts      = match?.groupValues[1].toString()
                    Log.d("kraptor_$name", "parts $parts")
                    val decodedUrl = dcHello(parts)
                    Log.d("kraptor_$name", "decodedUrl $decodedUrl")
                    decodedUrl
                } else{
                    val parts = match!!.groupValues[1]
                        .split(",")
                        .map { it.trim().removeSurrounding("\"") }
                    Log.d("kraptor_$name", "parts $parts")

                    Log.d("kraptor_$name", "dc parts: $parts")
                    val decodedUrl = dcDecode(parts)
                    Log.d("kraptor_$name", "decoded URL: $decodedUrl")
                    decodedUrl
                }

                Log.d("kraptor_$name", "realUrl $realUrl")



                if (iframe.contains("?rapidrame_id=")) {
                    iframe = "${mainUrl}/playerr/" + iframe.substringAfter("?rapidrame_id=")
                }

                val videoIsim = if (realUrl.contains("rapidrame")) {
                    "Rapidrame"
                } else if (realUrl.contains("cdnimages")) {
                    "Close"
                } else {
                    "HDFilmCehennemi"
                }

                val refererSon = if (realUrl.contains("cdnimages")) {
                    "https://hdfilmcehennemi.mobi/"
                } else {
                    "${mainUrl}/"
                }

                Log.d("kraptor_$name", "$source » $videoID » $iframe")
                callback.invoke(newExtractorLink(
                    source = videoIsim,
                    name = videoIsim,
                    url = realUrl,
                    type = ExtractorLinkType.M3U8,
                    {
                        this.referer = refererSon
                        this.quality = Qualities.Unknown.value
                    }
                ))
            }
        }

        return true
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

fun dcDecode(valueParts: List<String>): String {
    try {
        // 1) Join array elements (matches: let value = value_parts.join(''))
        var result = valueParts.joinToString(separator = "")

        // 2) ROT13 transformation (matches the replace function in JS)
        result = result.map { c ->
            when {
                c in 'a'..'z' -> {
                    val shifted = c.code + 13
                    if (shifted <= 'z'.code) shifted.toChar() else (shifted - 26).toChar()
                }
                c in 'A'..'Z' -> {
                    val shifted = c.code + 13
                    if (shifted <= 'Z'.code) shifted.toChar() else (shifted - 26).toChar()
                }
                else -> c
            }
        }.joinToString("")

        // 3) Reverse the string (matches: split('').reverse().join(''))
        result = result.reversed()

        // 4) Base64 decode (matches: atob(result))
        result = try {
            val decoded = Base64.decode(result, Base64.DEFAULT)
            String(decoded, StandardCharsets.ISO_8859_1)
        } catch (e: Exception) {
            return "" // Handle decode errors
        }

        // 5) Un-mix: Apply character transformation
        val unmix = StringBuilder(result.length)
        for (i in result.indices) {
            val charCode = result[i].code
            val delta = 399756995 % (i + 5)
            // Match JS logic: (charCode - delta + 256) % 256
            val transformedCode = (charCode - delta + 256) % 256
            unmix.append(transformedCode.toChar())
        }

        return unmix.toString()

    } catch (e: Exception) {
        return "" // Handle any other errors
    }
}

fun dcHello(encoded: String): String {
    // İlk Base64 çöz
    val firstDecoded = base64Decode(encoded)
    Log.d("kraptor_hdfilmcehennemi", "firstDecoded $firstDecoded")
    // Ters çevir
    val reversed = firstDecoded.reversed()
    Log.d("kraptor_hdfilmcehennemi", "reversed $reversed")
    // İkinci Base64 çöz
    val secondDecoded = base64Decode(reversed)

    val gercekLink    = secondDecoded.substringAfter("http")
    val sonLink       = "http$gercekLink"
    Log.d("kraptor_hdfilmcehennemi", "sonLink $sonLink")
    return sonLink.trim()

}