

package com.nikyokki

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class FilmIzlesene : MainAPI() {
    override var mainUrl = "https://www.filmizlesene.plus"
    override var name = "Filmİzlesene"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/kategori/turler/aile-filmleri-izle-1" to "Aile",
        "${mainUrl}/kategori/turler/animasyon-izle" to "Animasyon",
        "${mainUrl}/kategori/turler/belgesel-filmleri-izle-1" to "Belgesel",
        "${mainUrl}/kategori/turler/biyografi-filmleri-izle-1" to "Biyografi",
        "${mainUrl}/kategori/turler/dram-izle" to "Dram",
        "${mainUrl}/kategori/turler/fantastik-bilim-kurgu-izle" to "Fantasik-Bilim Kurgu",
        "${mainUrl}/kategori/turler/gizem-izle" to "Gizem",
        "${mainUrl}/kategori/turler/komedi-izle" to "Komedi",
        "${mainUrl}/kategori/turler/korku-gerilim-izle" to "Korku-Gerilim",
        "${mainUrl}/kategori/turler/macera-aksiyon-izle" to "Macera-Aksiyon",
        "${mainUrl}/kategori/turler/polisiye-suc-izle" to "Polisiye-Suç",
        "${mainUrl}/kategori/turler/romantik-duygusal-izle" to "Romantik",
        "${mainUrl}/kategori/turler/savas-filmleri-izle-2" to "Savaş",
        "${mainUrl}/kategori/turler/tarih-filmleri-izle-1" to "Tarih",
        "${mainUrl}/kategori/turler/western-izle" to "Western",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/${page}").document
        val home = document.select("div.movie-box").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse {
        val title = this.selectFirst("div.film-ismi a")?.text() ?: ""
        Log.d("FIS", "Title: $title")
        val href = fixUrlNull(this.selectFirst("div.film-ismi a")?.attr("href")) ?: ""
        Log.d("FIS", "Href: $href")
        val posterUrl = fixUrlNull(this.selectFirst("div.poster img")?.attr("data-src"))
        Log.d("FIS", "Poster: $posterUrl")

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.movie-box").mapNotNull { it.toMainPageResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val orgTitle = document.selectFirst("div.title-border h1")?.text()?.trim() ?: ""
        val altTitle = document.selectFirst("div.bolum-ismi")?.text()?.trim() ?: ""
        val title = if (altTitle.isNotEmpty()) "$orgTitle - $altTitle" else orgTitle
        val poster = fixUrlNull(document.selectFirst("div.film-afis img")?.attr("src"))
        val description = document.selectFirst("div#film-aciklama")?.text()?.trim()
        var year = document.selectFirst("div.release a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div#listelements a").map { it.text() }
        var rating = document.selectFirst("div.imdb")?.text()?.replace("IMDb Puanı:", "")
                ?.split("/")?.first()?.trim()?.toRatingInt()
        var actors = document.select("div.actor a").map { it.text() }
        val trailer = document.selectFirst("div.container iframe")?.attr("src")
        val listItems = document.select("div.list-item")
        for (item in listItems) {
            if (item.selectFirst("a")?.attr("href")?.contains("/yil/") == true) {
                year = item.selectFirst("a")?.text()?.toIntOrNull()
            }
            if (item.selectFirst("a")?.attr("href")?.contains("/oyuncu/") == true) {
                actors = item.select("a").map { it.text() }
            }
        }
        document.select("div#listelements div").forEach {
            if (it.text().contains("IMDb:")) {
                rating = it.text().trim().split(" ").last().toRatingInt()
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.rating = rating
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
        Log.d("FIS", "data » $data")
        val document = app.get(data).document

        document.select("div.sources script").forEach {
            if (it.toString().contains("#source")) {
                val url = it.toString().substringAfter("<iframe src=\"").substringBefore("\"")
                val doci = app.get(
                    url,
                    headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                        "Accept-Language" to " en-US,en;q=0.5",
                    ), referer = "https://www.filmizlesene.pro/"
                )
                var iframe = doci.document.select("iframe").attr("src")
                if (iframe.contains("/vidmo/")) {
                    val doci2 = app.get(
                        iframe,
                        headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
                            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                            "Accept-Language" to " en-US,en;q=0.5",
                        ), referer = "https://www.filmizlesene.pro/"
                    )
                    var iframe2 = doci2.document.select("iframe").attr("src")
                    loadExtractor(iframe2, iframe, subtitleCallback, callback)
                } else if (iframe.contains("/hdplayer/drive/")) {

                } else {
                    loadExtractor(iframe, url, subtitleCallback, callback)
                }
            }
        }

        return true
    }

}