// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class Watch2Movies : MainAPI() {
    override var mainUrl              = "https://watch2movies.net"
    override var name                 = "Watch2Movies"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/genre/action?page="           to "Action",
        "${mainUrl}/genre/action-adventure?page=" to "Action & Adventure",
        "${mainUrl}/genre/adventure?page="        to "Adventure",
        "${mainUrl}/genre/animation?page="        to "Animation",
        "${mainUrl}/genre/biography?page="        to "Biography",
        "${mainUrl}/genre/comedy?page="           to "Comedy",
        "${mainUrl}/genre/crime?page="            to "Crime",
        "${mainUrl}/genre/documentary?page="      to "Documentary",
        "${mainUrl}/genre/drama?page="            to "Drama",
        "${mainUrl}/genre/family?page="           to "Family",
        "${mainUrl}/genre/fantasy?page="          to "Fantasy",
        "${mainUrl}/genre/history?page="          to "History",
        "${mainUrl}/genre/horror?page="           to "Horror",
        "${mainUrl}/genre/kids?page="             to "Kids",
        "${mainUrl}/genre/music?page="            to "Music",
        "${mainUrl}/genre/mystery?page="          to "Mystery",
        "${mainUrl}/genre/news?page="             to "News",
        "${mainUrl}/genre/reality?page="          to "Reality",
        "${mainUrl}/genre/romance?page="          to "Romance",
        "${mainUrl}/genre/sci-fi-fantasy?page="   to "Sci-Fi & Fantasy",
        "${mainUrl}/genre/science-fiction?page="  to "Science Fiction",
        "${mainUrl}/genre/soap?page="             to "Soap",
        "${mainUrl}/genre/talk?page="             to "Talk",
        "${mainUrl}/genre/thriller?page="         to "Thriller",
        "${mainUrl}/genre/tv-movie?page="         to "TV Movie",
        "${mainUrl}/genre/war?page="              to "War",
        "${mainUrl}/genre/war-politics?page="     to "War & Politics",
        "${mainUrl}/genre/western?page="          to "Western",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home     = document.select("div.flw-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h2 a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/search/${query}").document

        return document.select("div.flw-item").mapNotNull { it.toMainPageResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div.dp-i-content h2 a")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.select("div.row-line")
            .firstOrNull { it.text().contains("Released:") }
            ?.text()
            ?.substringAfter("Released:")
            ?.trim()
            ?.split("-")?.firstOrNull()
            ?.toIntOrNull()

        val tags            = document.select("div.row-line a[href*='/genre/']").map { it.text() }
        val rating          = document.selectFirst("button.btn-imdb")?.text()?.trim()?.split(" ")?.last()?.toRatingInt()
        val duration = document.select("div.row-line")
            .firstOrNull { it.text().contains("Duration:") }
            ?.text()
            ?.substringAfter("Duration:")
            ?.replace("min", "")
            ?.trim()
            ?.toIntOrNull()
        val recommendations = document.select("div.flw-item").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("div.row-line a[href*='/cast/']").map { Actor(it.text()) }
        val trailer         = document.selectFirst("iframe#iframe-trailer")?.attr("data-src")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.rating          = rating
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("h3 a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("W2M", "data » $data")
        val epId     = data.split("-").last()
        val document = app.get("${mainUrl}/ajax/episode/list/${epId}", referer=data).document

        document.select("li.nav-item a").forEach {
            val dataId     = it.attr("data-linkid")
            Log.d("W2M", "dataId » $dataId")
            loadExtractor("${data}.${dataId}", "${mainUrl}/", subtitleCallback, callback)

            // val dataSource = app.get("${mainUrl}/ajax/episode/sources/${dataId}", referer=data).parsedSafe<Sources>()
            // Log.d("W2M", "iframe » ${dataSource!!.link}")

            // // ?   master  adb logcat -v tag | logcat-colorize | grep "W2M" 
            // // ! D   W2M      data   » https://watch2movies.net/movie/watch-ferry-2-full-118828
            // // * D   W2M      iframe » https://hanatyury.online/v2/embed-4/NDCSpcJUwFUT?z=
            // // * D   W2M      iframe » https://pepepeyo.xyz/v2/embed-4/MfDjL0xjrfrX?z=
            // // ! D   W2M      data   » https://watch2movies.net/movie/watch-adam-full-1542
            // // * D   W2M      iframe » https://hanatyury.online/v2/embed-4/0DMRS34RzDzF?z=
            // // * D   W2M      iframe » https://hanatyury.online/v2/embed-4/j3MXnGNwTkdx?z=
            // // * D   W2M      iframe » https://upstream.to/embed-tigfkb1a9wol.html
            // // * D   W2M      iframe » https://mixdrop.co/e/kn98qnk6h3k9wv

            // // TODO: Extractors not coded yet » UpCloudExtractor.kt
            // loadExtractor(dataSource!!.link, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }
}