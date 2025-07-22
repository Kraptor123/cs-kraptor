// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
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
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0")
            ).document
        } else {
            app.get(
                "${request.data}/page/$page",
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0")
            ).document
        }
//        Log.d("kraptor_$name","document = ${request.data}")
        val home = document.select("div.col-md-24 article").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        var title = this.selectFirst("a")?.attr("title").toString()

        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        var posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
//        Log.d("kraptor_$name","title = $title")
        if (posterUrl == null) {
            posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        }

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.post(
            "${mainUrl}/filmara.php",
            referer = "${mainUrl}/",
            data = mapOf("s" to query),
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0")
        ).document

        return document.select("article.movie.jet").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title =
            document.selectFirst("section.movie-exp div.movie-exp-title")?.text()?.substringBefore(" izle")?.trim()
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
            document.selectFirst("section.movie-exp div.imdb_puan span")?.text()?.split(" ")?.last()?.toRatingInt()
        val actors = document.select("section.movie-exp div.oyuncu").map {
            Actor(it.selectFirst("div.name")!!.text(), fixUrlNull(it.selectFirst("img")!!.attr("data-src")))
        }

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
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("JTF", "data » $data")
        val document = app.get(data).document

        // 1) Ana iframe'leri topla ve hemen işle
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
                // Eğer iframe doğrudan oynatıcı sayfasıysa, buradan çıkar:
                loadExtractor(url = iframeUrl, subtitleCallback, callback)
            }
        }

        // 2) Sayfa numaralarından link ve isimleri topla (fragmanları atla)
        val pageLinks = mutableListOf<Pair<String, String>>() // Pair<url, isim>
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

        // 3) Sadece sayfa linklerini işle, içlerindeki iframe'leri bul
        pageLinks.forEach { (url, isim) ->
            try {
                Log.d("JTF", "[$isim] işleniyor: $url")
                val pageDoc = app.get(url).document
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
                        loadExtractor(url = iframeUrl, subtitleCallback, callback)
                    } else {
                        Log.w("JTF", "  ⚠ iframe src bulunamadı: ${frame.outerHtml()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("JTF", "Failed to fetch or parse $url", e)
            }
        }

        return true
    }
}