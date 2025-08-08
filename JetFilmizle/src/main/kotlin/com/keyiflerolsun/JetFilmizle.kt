// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.*

import org.jsoup.nodes.Element

class JetFilmizle : MainAPI() {
    override var mainUrl = "https://jetfilmizle.so"
    override var name = "JetFilmizle"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
            "${mainUrl}"                                         to "Son Filmler",
    "${mainUrl}/imdb-puanina-gore"                                         to "Imdb Puanına Göre",
    "${mainUrl}/netflix"                                 to "Netflix",
    "${mainUrl}/editorun-secimi"                         to "Editörün Seçimi",
    "${mainUrl}/turk-film-full-hd-izle"                  to "Türk Filmleri",
    "${mainUrl}/cizgi-filmler-full-izle"                 to "Çizgi Filmler",
    "${mainUrl}/kategoriler/yesilcam-filmleri-full-izle" to "Yeşilçam Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get(
                request.data,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                    "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Connection" to "keep-alive",
                    "Upgrade-Insecure-Requests" to "1",
                    "Sec-Fetch-Dest" to "document",
                    "Sec-Fetch-Mode" to "navigate",
                    "Sec-Fetch-Site" to "same-origin",
                    "Sec-Fetch-User" to "?1"
                )
            ).document
        } else {
            app.get(
                "${request.data}/page/$page",
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                    "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Connection" to "keep-alive",
                    "Upgrade-Insecure-Requests" to "1",
                    "Sec-Fetch-Dest" to "document",
                    "Sec-Fetch-Mode" to "navigate",
                    "Sec-Fetch-Site" to "same-origin",
                    "Sec-Fetch-User" to "?1"
                )
            ).document
        }
//        Log.d("kraptor_$name","document = ${request.data}")
        val home = document.select("div.col-md-24 article").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title").toString()

        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val puan = this.selectFirst("span.puan_1")?.text()?.trim()
        var posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
//        Log.d("kraptor_$name","title = $title")
        if (posterUrl == null) {
            posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        }

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.post(
            "${mainUrl}/filmara.php",
            referer = "${mainUrl}/",
            data = mapOf("s" to query),
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                "Connection" to "keep-alive",
                "Upgrade-Insecure-Requests" to "1",
                "Sec-Fetch-Dest" to "document",
                "Sec-Fetch-Mode" to "navigate",
                "Sec-Fetch-Site" to "same-origin",
                "Sec-Fetch-User" to "?1"
            )
        ).document

        return document.select("article.movie.jet").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")).document
        val title = document.selectFirst("section.movie-exp div.movie-exp-title")?.text()?.substringBefore(" izle")?.trim()
                ?: return null
        val poster = fixUrlNull(document.selectFirst("section.movie-exp img")?.attr("data-src"))
            ?: fixUrlNull(document.selectFirst("section.movie-exp img")?.attr("src"))
        val yearDiv =
            document.selectXpath("//div[@class='yap' and contains(strong, 'Vizyon') or contains(strong, 'Yapım')]")
                .text().trim()
        val year = Regex("""(\d{4})""").find(yearDiv)?.groupValues?.get(1)?.toIntOrNull()
        val description = document.selectFirst("section.movie-exp p.aciklama")?.text()?.trim()
        val tags = document.select("section.movie-exp div.catss a").map { it.text() }
        val rating =
            document.selectFirst("section.movie-exp div.imdb_puan span")?.text()?.split(" ")?.last()
        val actors = document.select("section.movie-exp div.oyuncu").map {
            Actor(it.selectFirst("div.name")!!.text(), fixUrlNull(it.selectFirst("img")!!.attr("data-src")))
        }

        val pageLinks = document.select("a.post-page-numbers")
        val fragmanElement = pageLinks.firstOrNull { link ->
            link.selectFirst("span")?.text()?.contains("fragman", ignoreCase = true) == true
        }
        val trailerHref = fragmanElement?.attr("href") ?: ""
        val trailerGet  = app.get(trailerHref,     headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "same-origin",
            "Sec-Fetch-User" to "?1"
        )
        ).document
        val trailerAl   = trailerGet.select("iframe")
        val trailer = (trailerAl.attr("src")
            .takeIf { it.isNotBlank() }
            ?: trailerAl.attr("data-src")
                .takeIf { it.isNotBlank() }
            ?: trailerAl.attr("data-lazy-src")
                .takeIf { it.isNotBlank() })
            ?: trailerAl.attr("data-litespeed-src")
                .takeIf { it.isNotBlank() }
            ?: ""

        Log.d("kraptor_$name","trailer = $trailer")


        val recommendations = document.select("div#benzers article").mapNotNull {
            var recName =
                it.selectFirst("h2 a")?.text() ?: it.selectFirst("h3 a")?.text() ?: it.selectFirst("h4 a")?.text()
                ?: it.selectFirst("h5 a")?.text() ?: it.selectFirst("h6 a")?.text() ?: return@mapNotNull null
            recName = recName.substringBefore(" izle")

            val recHref = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val recPosterUrl = fixUrlNull(it.selectFirst("img")?.attr("data-src"))

            newMovieSearchResponse(recName, recHref, TvType.Movie) {
                this.posterUrl = recPosterUrl
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
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

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("JTF", "data » $data")
        val document = app.get(data, headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "same-origin",
            "Sec-Fetch-User" to "?1"
        )).document

        
        document.select("iframe").forEach { frame ->
            val src = frame.attr("src")
                .takeIf { it.isNotBlank() && it != "about:blank" }
                ?: frame.attr("data-src")
                    .takeIf { it.isNotBlank() }
                ?: frame.attr("data-lazy-src")
                    .takeIf { it.isNotBlank() }
                ?: frame.attr("data-litespeed-src")
                    .takeIf { it.isNotBlank() }
            if (src != null) {
                val iframeUrl = fixUrlNull(src).toString()
                Log.d("JTF", "ANA iframe » $iframeUrl")
                
               
                if (iframeUrl.contains("jfvid.com/play/")) {
                    val streamUrl = iframeUrl.replace("/play/", "/stream/")
                    Log.d("JTF", "JFVid stream » $streamUrl")
                    callback.invoke(
                        newExtractorLink(
                            name = "JFVid",
                            source = "JFVid", 
                            url = streamUrl,
                            
                            type = ExtractorLinkType.M3U8
                        ){
                            this.referer = iframeUrl
                        }
                    )
                } else {
                    loadExtractor(url = iframeUrl, subtitleCallback, callback)
                }
            }
        }

        
        val pageLinks = mutableListOf<Pair<String, String>>()
        document.select("a.post-page-numbers").forEach { aTag ->
            val isim = aTag.selectFirst("span")?.text() ?: ""
            if (isim != "Fragman") {
                aTag.attr("href")
                    .takeIf { it.isNotBlank() }
                    ?.let { href ->
                        pageLinks += href to isim
                        Log.d("JTF", "KAYNAK link » $href (isim: $isim)")
                    }
            }
        }

        
        pageLinks.forEach { (url, isim) ->
            try {
                Log.d("JTF", "[$isim] işleniyor: $url")
                val pageDoc = app.get(url, headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                    "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Connection" to "keep-alive",
                    "Upgrade-Insecure-Requests" to "1",
                    "Sec-Fetch-Dest" to "document",
                    "Sec-Fetch-Mode" to "navigate",
                    "Sec-Fetch-Site" to "same-origin",
                    "Sec-Fetch-User" to "?1"
                )).document
                pageDoc.select("div#movie iframe").forEach { frame ->
                    val src = frame.attr("src")
                        .takeIf { it.isNotBlank() && it != "about:blank" }
                        ?: frame.attr("data-src")
                            .takeIf { it.isNotBlank() }
                        ?: frame.attr("data-lazy-src")
                            .takeIf { it.isNotBlank() }
                        ?: frame.attr("data-litespeed-src")
                            .takeIf { it.isNotBlank() }
                    if (src != null) {
                        val iframeUrl = fixUrlNull(src).toString()
                        Log.d("JTF", "iframe » $iframeUrl")
                        
                        // JFVid URL'ini kontrol et
                        if (iframeUrl.contains("jfvid.com/play/")) {
                            val streamUrl = iframeUrl.replace("/play/", "/stream/")
                            Log.d("JTF", "JFVid stream » $streamUrl")
                            callback.invoke(
                                newExtractorLink(
                                    name = "JFVid ($isim)",
                                    source = "JFVid",
                                    url = streamUrl,
                                    
                                    type = ExtractorLinkType.M3U8
                                ){
                                    this.referer = iframeUrl

                                }
                            )
                        } else {
                            loadExtractor(url = iframeUrl, subtitleCallback, callback)
                        }
                    } else {
                        Log.w("JTF", "  ⚠ iframe src bulunamadı: ${frame.outerHtml()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("JTF", "Failed to fetch or parse $url  $e")
            }
        }

        return true
    }}