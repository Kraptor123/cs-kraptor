// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URLEncoder
import com.lagradost.cloudstream3.DubStatus
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.coroutines.cancellation.CancellationException


class AnimeIzlesene : MainAPI() {
    override var mainUrl = "https://www.animeizlesene.com"
    override var name = "AnimeIzlesene"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override val mainPage = mainPageOf(
        "${mainUrl}/category/aksiyon" to "Aksiyon",
        "${mainUrl}/category/fantastik" to "Fantastik",
        "${mainUrl}/category/isekai" to "Isekai",
        "${mainUrl}/category/komedi" to "Komedi",
        "${mainUrl}/category/korku" to "Korku",
        "${mainUrl}/category/macera" to "Macera",
        "${mainUrl}/category/seinen" to "Seinen",
        "${mainUrl}/category/shounen" to "Shounen",
        "${mainUrl}/category/turkce-dublaj" to "Türkçe Dublaj",
        "${mainUrl}/category/arabalar" to "Arabalar",
        "${mainUrl}/category/askeri" to "Askeri",
        "${mainUrl}/category/bilim-kurgu" to "Bilim Kurgu",
        "${mainUrl}/category/boys-love" to "Boys Love",
        "${mainUrl}/category/buyu" to "Büyü",
        "${mainUrl}/category/cocuk" to "Çocuk",
        "${mainUrl}/category/dedektif" to "Dedektif",
        "${mainUrl}/category/dogaustu-gucler" to "Doğaüstü Güçler",
        "${mainUrl}/category/donghua" to "Donghua",
        "${mainUrl}/category/dovus-sanatlari" to "Dövüş Sanatları",
        "${mainUrl}/category/dram" to "Dram",
        "${mainUrl}/category/ecchi" to "Ecchi",
        "${mainUrl}/category/erotik" to "Erotik",
        "${mainUrl}/category/gerilim" to "Gerilim",
        "${mainUrl}/category/girls-love" to "Girls Love",
        "${mainUrl}/category/gizem" to "Gizem",
        "${mainUrl}/category/gurme" to "Gurme",
        "${mainUrl}/category/harem" to "Harem",
        "${mainUrl}/category/iyashikei" to "Iyashikei",
        "${mainUrl}/category/josei" to "Josei",
        "${mainUrl}/category/kisilik-bolunmesi" to "Kişilik Bölünmesi",
        "${mainUrl}/category/mecha" to "Mecha",
        "${mainUrl}/category/mitoloji" to "Mitoloji",
        "${mainUrl}/category/muzik" to "Müzik",
        "${mainUrl}/category/okul" to "Okul",
        "${mainUrl}/category/ona" to "ONA",
        "${mainUrl}/category/oyun" to "Oyun",
        "${mainUrl}/category/parodi" to "Parodi",
        "${mainUrl}/category/polisiye" to "Polisiye",
        "${mainUrl}/category/psikolojik" to "Psikolojik",
        "${mainUrl}/category/reenkarnasyon" to "Reenkarnasyon",
        "${mainUrl}/category/romantizim" to "Romantizim",
        "${mainUrl}/category/samuray" to "Samuray",
        "${mainUrl}/category/savas-sanatlari" to "Savaş Sanatları",
        "${mainUrl}/category/seytanlar" to "Şeytanlar",
        "${mainUrl}/category/shoujo" to "Shoujo",
        "${mainUrl}/category/shoujo-ai" to "Shoujo Ai",
        "${mainUrl}/category/spor" to "Spor",
        "${mainUrl}/category/strateji-oyunu" to "Strateji Oyunu",
        "${mainUrl}/category/super-gucler" to "Süper Güçler",
        "${mainUrl}/category/tarihi" to "Tarihi",
        "${mainUrl}/category/uzay" to "Uzay",
        "${mainUrl}/category/vampir" to "Vampir",
        "${mainUrl}/category/yasamdan-kesitler" to "Yaşamdan Kesitler",
        "${mainUrl}/category/zaman-yolculugu" to "Zaman Yolculuğu"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?filter=null&page=$page").document
        val home = document.select("div.col").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.col a.list-title")?.text() ?: return null
        if ("TEST" in title) return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl =
            fixUrlNull(this.selectFirst("div.media.media-cover.nightowl-daylight.nightowl-daylight")?.attr("data-src"))

        val type = if (href.contains("/movie/")) TvType.AnimeMovie else TvType.Anime

        return newAnimeSearchResponse(title, href, type) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return try {
            // 1) Query’i encode et
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
//            Log.d("arama", "Encoded Query = $encodedQuery")

            // 2) HTTP GET isteği
            val httpResponse = app.get(
                url     = "$mainUrl/ajax/posts",
                params  = mapOf("q" to encodedQuery),
                referer = mainUrl,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
                    "Host"       to "www.animeizlesene.com"
                )
            )
//            Log.d("arama", "HTTP ${httpResponse}")
            val responseText = httpResponse.text
//            Log.d("arama", "Raw JSON Response: $responseText")

            // 3) JSON parse
            val mapper   = jacksonObjectMapper()
            val rootNode = mapper.readTree(responseText)
            val dataNode = rootNode["data"] ?: return emptyList()

            // 4) JsonNode listesini AnimeSearchResponse’a çevir
            dataNode.mapNotNull { itemNode ->
                val name     = itemNode["name"]?.asText()    ?: return@mapNotNull null
                val imageUrl = itemNode["image"]?.asText().orEmpty()
                val url      = itemNode["url"]?.asText()     ?: return@mapNotNull null
                val typeStr  = itemNode["type"]?.asText().orEmpty()

                val tvType = when {
                    typeStr.contains("Serisi", ignoreCase = true) -> TvType.Anime
                    typeStr.contains("Film",   ignoreCase = true) -> TvType.AnimeMovie
                    else                                          -> TvType.Anime
                }

                newAnimeSearchResponse(
                    name = name,
                    url  = url,
                    type = tvType,
                    fix  = false
                ) {
                    this.posterUrl   = imageUrl
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
//            Log.e("arama", "Search failed", e)
            emptyList()
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> {
        // quickSearch, search()'e direkt yönlendiriyor
        return search(query)
    }

    @RequiresApi(Build.VERSION_CODES.N)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val isMovie = url.contains("/movie/")

        // Different selectors for movies and series
        val title = if (isMovie) {
            document.selectFirst("div.caption h1")?.text()?.trim()
        } else {
            document.selectFirst("div.col-md-9 h1")?.text()?.trim()
        } ?: return null

        val poster = fixUrlNull(
            document.selectFirst("div.media.media-cover.nightowl-daylight.nightowl-daylight")?.attr("data-src")
        )

        val description = if (isMovie) {
            document.selectFirst("div.video-attr:nth-child(6) > div:nth-child(2)")?.text()?.trim()
        } else {
            document.selectFirst("div.text-content")?.text()?.trim()
        }

        val year = if (isMovie) {
            document.selectFirst("div.video-attr:nth-child(5) > div:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        } else {
            document.selectFirst("div.featured-attr:nth-child(3) > div:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        }

        val tags = if (isMovie) {
            document.select("div.category a").map { it.text() }
        } else {
            document.select("div.categories a").map { it.text() }
        }

        val rating = if (isMovie) {
            null // Rating not available for movies as per your description
        } else {
            document.selectFirst("div.featured-attr:nth-child(1) > div:nth-child(2) ")?.text()?.trim()?.toRatingInt()
        }

        val duration = if (isMovie) {
            document.selectFirst("div.video-attr:nth-child(4) > div:nth-child(2)")?.text()?.split(" ")?.first()
                ?.trim()?.toIntOrNull()
        } else {
            document.selectFirst("div.featured-attr:nth-child(4) > div:nth-child(2) ")?.text()?.split(" ")?.first()
                ?.trim()?.toIntOrNull()
        }

        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        // For movies, create a single episode
        if (isMovie) {
            return newMovieLoadResponse(title, url, TvType.AnimeMovie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.duration = duration
                addTrailer(trailer)

                // Recommendations for movies
                this.recommendations = document.select("div.col div.list-movie").mapNotNull {
                    val recTitle = it.selectFirst("a.list-title")?.text() ?: return@mapNotNull null
                    val recHref = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
                    val recPosterUrl = fixUrlNull(it.selectFirst("div.media.media-cover")?.attr("data-src"))
                    val recType = if (recHref.contains("/movie/")) TvType.AnimeMovie else TvType.Anime

                    newAnimeSearchResponse(recTitle, recHref, recType) { this.posterUrl = recPosterUrl }
                }
            }
        }

        // For series, handle episodes
        val episodeMap = mutableMapOf<DubStatus, List<Episode>>()

        document.select("div.episodes.tab-content div.tab-pane").forEach { seasonPane ->
            val seasonNumber = seasonPane.attr("id").substringAfter("season-").toIntOrNull() ?: 1

            val seasonEpisodes = seasonPane.select("a").mapNotNull { episodeElement ->
                val episodeUrl = fixUrlNull(episodeElement.attr("href")) ?: return@mapNotNull null
                val episodeNumberText = episodeElement.selectFirst("div.episode")?.text()?.trim()
                val episodeName = episodeElement.selectFirst("div.name")?.text()?.trim()

                val episodeNumber = episodeNumberText?.substringBefore(".")?.trim()?.toIntOrNull()

                newEpisode(episodeUrl) {
                    this.name = episodeName
                    this.episode = episodeNumber
                    this.season = seasonNumber
                }
            }

            episodeMap.merge(DubStatus.Subbed, seasonEpisodes) { old, new -> old + new }
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.rating = rating
            this.duration = duration
            addTrailer(trailer)
            this.episodes = episodeMap
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("Animei", "data » $data")

        val document = app.get(data).document
        val parildaElements = document.select(".parilda")

        if (parildaElements.isEmpty()) return false

        var foundLinks = false

        parildaElements.forEach { element ->
            val embedId = element.attr("data-embed")
            if (embedId.isNotBlank()) {
                val headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.5",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Origin" to "https://www.animeizlesene.com"
                )

                try {
                    val response = app.post(
                        url = "https://www.animeizlesene.com/ajax/embed",
                        headers = headers,
                        referer = data,
                        data = mapOf("id" to embedId),
                        verify = false
                    ).text

                    val regex = Regex("""src="([^"]+)"""")


                    val videoUrls = regex.findAll(response)
                        .mapNotNull { it.groups[1]?.value?.replace("&amp;", "&") }
                        .toList()

                    videoUrls.find { it.contains("hdvid.tv") }?.let { url ->
                        HdBestVidExtractor().getUrl(url, data).forEach(callback)
                        foundLinks = true
                    }

                    val fixedUrls = videoUrls.map { url ->
                        if (url.startsWith("//")) {
                            "https:$url"
                        } else {
                            url
                        }
                    }

                    if (fixedUrls.isNotEmpty()) {
                        foundLinks = true
                        Log.d("Animei", "Video: $fixedUrls")

                        fixedUrls.forEach { url ->
                            loadExtractor(url, "$mainUrl/", subtitleCallback, callback)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("Animei", "Hata oluştu: ${e.localizedMessage}")
                }
            }
        }

        return foundLinks
    }
}