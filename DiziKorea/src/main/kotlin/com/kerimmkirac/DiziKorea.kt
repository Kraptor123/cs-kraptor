// ! Bu araç @kerimmkirac tarafından | @kerimmkirac için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup

class DiziKorea : MainAPI() {
    override var mainUrl              = "https://dizikorea.pw"
    override var name                 = "DiziKorea"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = true
    override val supportedTypes       = setOf(TvType.AsianDrama , TvType.Movie)

    override var sequentialMainPage = true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay       = 150L  // ? 0.15 saniye
    override var sequentialMainPageScrollDelay = 150L  // ? 0.15 saniye

    // ! CloudFlare v2
    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request  = chain.request()
            val response = chain.proceed(request)
            val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

            if (doc.html().contains("Just a moment")) {
                return cloudflareKiller.intercept(chain)
            }

            return response
        }
    }

    override val mainPage = mainPageOf(
        "yeni-eklenenler" to "Yeni Eklenenler",
        "${mainUrl}/tum-kore-dizileri/"   to "Kore Dizileri",
        "${mainUrl}/kore-filmleri-izle1/" to "Kore Filmleri",
        "${mainUrl}/tayland-dizileri/"    to "Tayland Dizileri",
        "${mainUrl}/tayland-filmleri/"    to "Tayland Filmleri",
        "${mainUrl}/cin-dizileri/"        to "Çin Dizileri",
        "${mainUrl}/cin-filmleri/"        to "Çin Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val home = if (request.data == "yeni-eklenenler") {
           
            getYeniEklenenler(page)
        } else {
           
            val document = app.get("${request.data}${page}", interceptor = interceptor).document
            Log.d("DZK", "Ana sayfa HTML içeriği:\n${document.outerHtml()}")
            document.select("div.poster-long").mapNotNull { it.toSearchResult() }
        }

        return newHomePageResponse(request.name, home)
    }

    private suspend fun getYeniEklenenler(page: Int): List<SearchResponse> {
        val response = app.post(
            "${mainUrl}/episode/load",
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                "Accept" to "application/json, text/javascript, */*; q=0.01"
            ),
            referer = "${mainUrl}/",
            data = mapOf(
                "page" to page.toString(),
                "tag_id" to "22"
            ),
            interceptor = interceptor
        ).parsedSafe<EpisodeLoadResponse>()

        if (response?.success == true && response.theme.isNotEmpty()) {
            val document = Jsoup.parse(response.theme)
            val results = mutableListOf<SearchResponse>()

            document.select("li").forEach { listItem ->
                val result = listItem.toEpisodeSearchResult()
                result?.let { results.add(it) }
            }

            Log.d("DZK", "Yeni eklenenler sayfa $page: ${results.size} sonuç")
            return results
        }

        return emptyList()
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text()?.trim() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.poster-long-image img.lazy")?.attr("data-src"))
        val rating      = this.selectFirst("span.rating.flex.items-center")?.text()?.trim()

        return newTvSeriesSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(rating)
        }
    }

    private fun Element.toEpisodeSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text()?.trim() ?: return null
        val originalHref = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.lazy")?.attr("data-src"))
        val episodeInfo = this.selectFirst("p.truncate")?.text()?.trim()
        val rating      = this.selectFirst("span.rating.flex.items-center")?.text()?.trim()
        
        
        val diziHref = convertEpisodeUrlToSeriesUrl(originalHref)
        
        
        val fullTitle = if (episodeInfo != null) "$title - $episodeInfo" else title

        return newTvSeriesSearchResponse(fullTitle, diziHref, TvType.AsianDrama) { 
            this.posterUrl = posterUrl
            this.score     = Score.from10(rating)
        }
    }

    private fun convertEpisodeUrlToSeriesUrl(episodeUrl: String): String {
    
    val path = episodeUrl.substringAfter("/dizi/")
    
    return when {
        
        path.contains("/sezon-") && path.contains("/bolum-") -> {
            val seriesName = path.substringBefore("/sezon-")
            "${mainUrl}/dizi/${seriesName}"
        }
        
        path.matches(Regex(".*-\\d+-sezon-\\d+-bolum-\\d+-izle.*")) -> {
            
            val seriesName = path.replaceFirst(Regex("-\\d+-sezon-\\d+-bolum-\\d+-izle.*"), "")
            "${mainUrl}/dizi/${seriesName}"
        }
        
        path.matches(Regex(".*-sezon-\\d+-bolum-\\d+-izle(?:/.*)?$")) -> {
            val seriesName = path.replaceFirst(Regex("-sezon-\\d+-bolum-\\d+-izle(?:/.*)?$"), "")
            "${mainUrl}/dizi/${seriesName}"
        }
       
        path.matches(Regex(".*-[0-9]+-sezon-[0-9]+-bolum-[0-9]+-izle.*")) -> {
            val seriesName = path.replaceFirst(Regex("-[0-9]+-sezon-[0-9]+-bolum-[0-9]+-izle.*"), "")
            "${mainUrl}/dizi/${seriesName}"
        }
        
        path.contains("-sezon-") && path.contains("-bolum-") -> {
            
            val lastSezonIndex = path.lastIndexOf("-sezon-")
            if (lastSezonIndex > 0) {
                
                val beforeSezon = path.substring(0, lastSezonIndex)
                val lastDashIndex = beforeSezon.lastIndexOf("-")
                
                
                if (lastDashIndex > 0) {
                    val possibleNumber = beforeSezon.substring(lastDashIndex + 1)
                    if (possibleNumber.matches(Regex("\\d+"))) {
                        val seriesName = beforeSezon.substring(0, lastDashIndex)
                        "${mainUrl}/dizi/${seriesName}"
                    } else {
                        
                        "${mainUrl}/dizi/${beforeSezon}"
                    }
                } else {
                    "${mainUrl}/dizi/${beforeSezon}"
                }
            } else {
                episodeUrl
            }
        }
        
        else -> episodeUrl
    }
}

    private fun Element.toSeriesSearchResult(): SearchResponse? {
        val title = this.selectFirst("span.block.truncate")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.lazy")?.attr("data-src"))
        val rating      = this.selectFirst("span.rating.flex.items-center")?.text()?.trim()

        return newTvSeriesSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(rating)
        }
    }

    private fun Element.toMovieSearchResult(): SearchResponse? {
        val title = this.selectFirst("span.block a")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.lazy")?.attr("data-src"))
        val rating      = this.selectFirst("span.rating.flex.items-center")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(rating)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.post(
            "${mainUrl}/search",
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
            referer = "${mainUrl}/",
            data    = mapOf("query" to query)
        ).parsedSafe<KoreaSearch>()!!.theme

        val document = Jsoup.parse(response)
        val results  = mutableListOf<SearchResponse>()

        
        document.select("div.leftbar-search-result-box").forEach { resultBox ->
            val sectionTitle = resultBox.selectFirst("span.block")?.text()?.trim()
            
            if (sectionTitle?.contains("Diziler") == true) {
                resultBox.select("ul li").forEach { listItem ->
                    val href = listItem.selectFirst("a")?.attr("href")
                    if (href != null && href.contains("/dizi/")) {
                        val result = listItem.toSeriesSearchResult()
                        result?.let { results.add(it) }
                    }
                }
            } else if (sectionTitle?.contains("Filmler") == true) {
                resultBox.select("ul li").forEach { listItem ->
                    val href = listItem.selectFirst("a")?.attr("href")
                    if (href != null && href.contains("/film/")) {
                        val result = listItem.toMovieSearchResult()
                        result?.let { results.add(it) }
                    }
                }
            }
        }

        return results
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, interceptor = interceptor).document

        val title       = document.selectFirst("h1 a")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content")) ?: return null
        val year        = document.selectFirst("h1 span")?.text()?.substringAfter("(")?.substringBefore(")")?.toIntOrNull()
        val description = document.selectFirst("div.series-profile-summary p")?.text()?.trim()
        val tags        = document.select("div.series-profile-type a").mapNotNull { it.text().trim() }
        val rating      = document.selectFirst("span.color-imdb")?.text()?.trim()
        val duration    = document.selectXpath("//span[text()='Süre']//following-sibling::p").text().trim().split(" ").first().toIntOrNull()
        val trailerId     = document.selectFirst("div.series-profile-trailer")?.attr("data-yt")
        val trailerUrl = trailerId?.takeIf { it.isNotEmpty() }?.let { "https://www.youtube.com/watch?v=$it" }
        val actors      = document.select("div.series-profile-cast li").map {
            Actor(it.selectFirst("h5")!!.text(), it.selectFirst("img")!!.attr("data-src"))
        }

        if (url.contains("/dizi/")) {
            val episodes    = mutableListOf<Episode>()
            document.select("div.series-profile-episode-list").forEach {
                val epSeason = it.parent()!!.id().split("-").last().toIntOrNull()

                it.select("li").forEach ep@ { episodeElement ->
                    val epHref    = fixUrlNull(episodeElement.selectFirst("h6 a")?.attr("href")) ?: return@ep
                    val epEpisode = episodeElement.selectFirst("a.truncate data")?.text()?.trim()?.toIntOrNull()

                    episodes.add(newEpisode(epHref) {
                        this.name = "${epSeason}. Sezon ${epEpisode}. Bölüm"
                        this.season = epSeason
                        this.episode = epEpisode
                    })
                }
            }

            return newTvSeriesLoadResponse(title, url, TvType.AsianDrama, episodes) {
                this.posterUrl = poster
                this.year      = year
                this.plot      = description
                this.tags      = tags
                this.score = Score.from10(rating)
                this.duration  = duration
                addActors(actors)
                addTrailer(trailerUrl)
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.AsianDrama, url) {
                this.posterUrl = poster
                this.year      = year
                this.plot      = description
                this.tags      = tags
                this.score = Score.from10(rating)
                this.duration  = duration
                addActors(actors)
                addTrailer(trailerUrl)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("DZK", "data » $data")
        val document = app.get(data, interceptor = interceptor).document

        document.select("div.video-services button").forEach {
            val rawHhs = it.attr("data-hhs")
            Log.d("DZK", "Found button with data-hhs: $rawHhs")

            val iframe = fixUrlNull(rawHhs) ?: return@forEach
            Log.d("DZK", "iframe » $iframe")

            
            if (iframe.contains("vidmoly.to")) {
                Log.d("DZK", "Vidmoly linki tespit edildi, özel extractor kullanılıyor")
                extractVidmolyDirectly(iframe, callback)
            } else {
                
                loadExtractor(iframe, "$mainUrl/", subtitleCallback, callback)
            }
        }

        return true
    }

    
    private suspend fun extractVidmolyDirectly(url: String, callback: (ExtractorLink) -> Unit) {
        try {
            val headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36",
                "Sec-Fetch-Dest" to "iframe",
                "Referer" to "https://vidmoly.to/"
            )
            
            Log.d("DZK", "Vidmoly URL'si işleniyor: $url")
            val iSource = app.get(url, headers = headers, referer = "$mainUrl/").text
            Log.d("DZK", "Vidmoly iframe içeriği alındı, m3u8 aranıyor...")
            
            val matches = Regex("""file\s*:\s*"([^"]+\.m3u8[^"]*)"""").findAll(iSource).toList()
            
            if (matches.isEmpty()) {
                Log.w("DZK", "Vidmoly'de m3u8 link bulunamadı")
                return
            }
            
            Log.d("DZK", "Vidmoly'de ${matches.size} adet m3u8 bulundu")
            
            matches.forEachIndexed { index, match ->
                val m3uLink = match.groupValues[1]
                Log.d("DZK", "Vidmoly m3uLink[$index] → $m3uLink")

                callback(
                    newExtractorLink(
                        source = "VidMoly",
                        name = "VidMoly",
                        url = m3uLink,
                        
                        
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.referer = "https://vidmoly.to/"
                        this.quality = Qualities.Unknown.value
                        this.headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("DZK", "Vidmoly extractor hatası: ${e.message}")
        }
    }

    
    data class EpisodeLoadResponse(
        val last: Int,
        val success: Boolean,
        val theme: String
    )

    data class KoreaSearch(
        val theme: String
    )
}