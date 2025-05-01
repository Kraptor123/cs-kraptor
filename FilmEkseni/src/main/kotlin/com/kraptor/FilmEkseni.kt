// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class FilmEkseni : MainAPI() {
    override var mainUrl = "https://filmekseni.net"
    override var name = "Film Ekseni"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/tur/aile-filmleri/page"  to "Aile Filmleri",
        "${mainUrl}/tur/aksiyon-filmleri/page"           to "Aksiyon Filmleri",
        "${mainUrl}/tur/animasyon-filmleri/page"         to "Animasyon Filmleri",
        "${mainUrl}/tur/belgesel-filmleri/page"          to "Belgesel Filmleri",
        "${mainUrl}/tur/bilim-kurgu-filmleri/page"       to "Bilim Kurgu Filmleri",
        "${mainUrl}/tur/biyografi-filmleri/page"         to "Biyografi Filmleri",
        "${mainUrl}/tur/dram-filmleri/page"              to "Dram Filmleri",
        "${mainUrl}/tur/fantastik-filmler/page"               to "Fantastik Filmleri",
        "${mainUrl}/tur/gerilim-filmleri/page"           to "Gerilim Filmleri",
        "${mainUrl}/tur/gizem-filmleri/page"             to "Gizem Filmleri",
        "${mainUrl}/tur/komedi-filmleri/page"            to "Komedi Filmleri",
        "${mainUrl}/tur/korku-filmleri/page"             to "Korku Filmleri",
        "${mainUrl}/tur/macera-filmleri/page"            to "Macera Filmleri",
        "${mainUrl}/tur/muzik-filmleri/page"             to "Müzik Filmleri",
        "${mainUrl}/tur/muzikal/page"                         to "Müzikal Filmleri",
        "${mainUrl}/tur/romantik-filmler/page"                to "Romantik Filmleri",
        "${mainUrl}/tur/savas-filmleri/page"             to "Savaş Filmleri",
        "${mainUrl}/tur/spor-filmleri/page"              to "Spor Filmleri",
        "${mainUrl}/tur/suc-filmleri/page"               to "Suç Filmleri",
        "${mainUrl}/tur/tarih-filmleri/page"             to "Tarih Filmleri",
        "${mainUrl}/tur/western-filmler/page"            to "Western Filmleri",
        "${mainUrl}/diziler/page"                        to "Diziler",
        "${mainUrl}/en-cok-izlenenler/page"              to "En Çok İzlenenler",
        "${mainUrl}/kategori/tavsiye-filmler/page"       to "Tavsiye Filmler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page").document
        val home = document.select("div.poster").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<com.lagradost.cloudstream3.SearchResponse> {
        val url = "$mainUrl/search/"

        // Preparing the data for POST request
        val data = mapOf("query" to query)

        try {
            val response = app.post(
                url = url,
                headers = mapOf(
                    "X-Requested-With" to "XMLHttpRequest",
                    "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                    "Referer" to mainUrl,
                ),
                data = data
            )
            val jsonResponse = AppUtils.parseJson<SearchApiResponse>(response.text)
            return jsonResponse.result.mapNotNull { item ->
                val title = item.title ?: return@mapNotNull null
                val slug = item.slug ?: return@mapNotNull null
                val href = "$mainUrl/$slug"
                val posterUrl = item.cover?.let { "$mainUrl/uploads/poster/$it" }

                newMovieSearchResponse(title, href, TvType.Movie) {
                    this.posterUrl = posterUrl
                }
            }
        } catch (e: Exception) {
            Log.e("Eksen", "Search error: ${e.message}", e)
            return emptyList()
        }
    }

    // Data class to parse JSON response with all fields nullable
    data class SearchApiResponse(
        val result: List<SearchResultItem> = emptyList(),
        val query: String? = null
    )

    data class SearchResultItem(
        val postid: String? = null,
        val access: String? = null,
        val poster: String? = null,
        val cover: String? = null,
        val imdb: String? = null,
        val original_title: String? = null,
        val title: String? = null,
        val second_title: String? = null,
        val slug: String? = null,
        val year: String? = null,
        val type: String? = null,
        val akatitle: String? = null,
        val slug_prefix: String? = null,
        val overview: String? = null
    )

//    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val rawTitle = document.selectFirst("div.page-title h1")?.text()?.trim() ?: return null
        val title = rawTitle.substringBeforeLast(" izle", rawTitle).trim()
        val poster = fixUrlNull(document.selectFirst("picture.poster-auto > source:nth-child(2)")?.attr("data-srcset"))
        val description = document.selectFirst("article.text-white")?.text()?.trim()
        val year = document.selectFirst("strong a")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.pb-2")
            .mapNotNull { it.text().removePrefix("Tür: ").takeIf { txt -> txt.contains(",") || txt.isNotBlank() } }
            .flatMap { it.split(",").map { tag -> tag.trim() } }
        val rating = document.selectFirst("div.rate")?.text()?.trim()?.toRatingInt()
        val duration =
            document.selectFirst("d-flex flex-column text-nowrap")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.poster").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("div.card-body.p-0.pt-2 .story-item").map {
            val name = it.selectFirst(".story-item-title")?.text() ?: ""
            val image = it.selectFirst("img")?.attr("data-src") ?: ""
            Actor(name, image)
        }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.rating = rating
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a img")?.attr("alt") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("Eksen", "data » ${data}")
        val document = app.get(data).document

        // Get iframe element
        val iframeElement = document.select("div.card-video iframe").firstOrNull()
        if (iframeElement == null) {
            Log.d("Eksen", "No video iframe found")
            return false
        }

        // Get and validate iframe URL
        var iframeUrl = iframeElement.attr("data-src")
        iframeUrl = when {
            iframeUrl.startsWith("//") -> "https:$iframeUrl"
            iframeUrl.isBlank() -> {
                Log.d("Eksen", "Empty iframe URL")
                return false
            }

            else -> iframeUrl
        }
        Log.d("Eksen", "iframe = $iframeUrl")

        // Extract and validate video ID
        val videoId = iframeUrl.substringAfterLast("/")
        if (videoId.isBlank() || videoId == iframeUrl) {
            Log.d("Eksen", "Invalid video ID")
            return false
        }

        // Validate master playlist URL
        val masterListe = "https://eksenload.site/uploads/encode/$videoId/master.m3u8"
        if (!masterListe.startsWith("https://") || !masterListe.endsWith(".m3u8")) {
            Log.d("Eksen", "Invalid master URL format")
            return false
        }

        // Validate subtitles before sending
        val subtitleGetiren = listOf(
            SubtitleFile("Türkçe", "https://eksenload.site/uploads/encode/$videoId/${videoId}_tur.vtt"),
            SubtitleFile("İngilizce", "https://eksenload.site/uploads/encode/$videoId/${videoId}_en.vtt")
        ).filter {
            // Verify subtitle URLs have valid format
            it.url.startsWith("https://") && it.url.endsWith(".vtt")
        }

        if (subtitleGetiren.isEmpty()) {
            Log.d("Eksen", "No valid subtitles found")
            return false
        }

        subtitleGetiren.forEach { subtitleCallback(it) }

        val link = newExtractorLink(
            source = this.name,
            name = this.name,
            url = masterListe,
            type = ExtractorLinkType.M3U8
        ) {
            headers = mapOf("Referer" to mainUrl)
            quality = Qualities.P1080.value
        }

        if (link.url.isBlank()) {
            Log.d("Eksen", "Empty extractor link")
            return false
        }

        callback(link)
        return true
    }
}