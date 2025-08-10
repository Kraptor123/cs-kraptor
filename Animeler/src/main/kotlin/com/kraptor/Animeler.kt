// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder

class Animeler : MainAPI() {
    override var mainUrl = "https://animeler.me"
    override var name = "Animeler"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Anime)


    override val mainPage = mainPageOf(
        "${mainUrl}/genre/action/"           to "Aksiyon",
        "${mainUrl}/genre/adult-cast/"                   to "Yetişkin Aktör",
        "${mainUrl}/genre/adventure/"                    to "Macera",
        "${mainUrl}/genre/military/"                     to "Askeri",
        "${mainUrl}/genre/comedy/"                       to "Komedi",
        "${mainUrl}/genre/cultivation/"                  to "Yetişim",
        "${mainUrl}/genre/demons/"                       to "İblisler",
        "${mainUrl}/genre/dogaustu-gucler/"                             to "Doğaüstü",
        "${mainUrl}/genre/dovus-sanatlari/"                           to "Dövüş Sanatları",
        "${mainUrl}/genre/dram/"                         to "Dram",
        "${mainUrl}/genre/drama/"                        to "Drama",
        "${mainUrl}/genre/ecchi/"                        to "Ecchi",
        "${mainUrl}/genre/fantastik/"                    to "Fantastik",
        "${mainUrl}/genre/fantasy/"                      to "Fantasy",
        "${mainUrl}/genre/gore/"                         to "Vahşet",
        "${mainUrl}/genre/gourmet/"                      to "Gurme",
        "${mainUrl}/genre/harem/"                        to "Harem",
        "${mainUrl}/genre/historical/"                   to "Tarihi",
        "${mainUrl}/genre/horror/"                       to "Korku",
        "${mainUrl}/genre/josei/"                        to "Josei", 
        "${mainUrl}/genre/martial-arts/"                 to "Dovüş Sanatları",
        "${mainUrl}/genre/mecha/"                        to "Meka",
        "${mainUrl}/genre/music/"                        to "Müzik",
        "${mainUrl}/genre/mystery/"                      to "Gizem",
        "${mainUrl}/genre/mythology/"                    to "Mitoloji",
        "${mainUrl}/genre/psychological/"                to "Psikolojik",
        "${mainUrl}/genre/reincarnation/"                to "Reenkarnasyon",
        "${mainUrl}/genre/romance/"                      to "Romantik",
        "${mainUrl}/genre/romantizm/"                    to "Romantizm",
        "${mainUrl}/genre/school/"                       to "Okul",
        "${mainUrl}/genre/sci-fi/"                       to "Bilim Kurgu",
        "${mainUrl}/genre/seinen/"                       to "Seinen",
        "${mainUrl}/genre/shoujo/"                       to "Shoujo",
        "${mainUrl}/genre/shounen/"                      to "Shounen",
        "${mainUrl}/genre/slice-of-life/"                to "Yaşamdan kesitler",
        "${mainUrl}/genre/sports/"                       to "Sporlar",
        "${mainUrl}/genre/super-power/"                  to "Super Güç",
        "${mainUrl}/genre/supernatural/"                 to "Supernatural",
        "${mainUrl}/genre/tarihi/"                       to "Tarihi",
        "${mainUrl}/genre/team-sports/"                  to "Takım Sporları",
        "${mainUrl}/genre/video-game/"                   to "Video Oyunu",
       
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            app.get(request.data).document
        }else {
            app.get("${request.data}/page/$page/").document
        }
        val home =
            url.select("div.w-full.bg-gradient-to-t.from-primary.to-transparent.rounded.overflow-hidden.shadow.shadow-primary")
                .mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2 a span.show")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/detayli-arama/?s_keyword=${query}").document

        return document.select("div.w-full.bg-gradient-to-t.from-primary.to-transparent.rounded.overflow-hidden.shadow.shadow-primary")
            .mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3 a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a.absolute")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst(".xl\\:w-full h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.anime-image img")?.attr("data-src"))
        val description = document.selectFirst("div.line-clamp-3")?.text()?.trim()
        val tags = document.select("span.leading-6 a").map { it.text() }
        val elements = document.select("li.list-none.mbe-1")
        val rating = Regex("MAL:\\s*(\\d+(?:\\.\\d+)?)").find(elements.text())?.groups?.get(1)?.value
        val duration = Regex("Süre:\\s*(\\d+)").find(elements.text())?.groups?.get(1)?.value?.toInt()
        val year = Regex("(\\d{4})").find(elements.text())?.groups?.get(1)?.value?.toInt()
        val recommendations =
            document.select("div.w-full.bg-gradient-to-t.from-primary.to-transparent.rounded.overflow-hidden.shadow.shadow-primary")
                .mapNotNull { it.toRecommendationResult() }
        val trailer = Regex("""openVideoEmbed\('([^']+youtube\.com/embed/[^']+)""")
        .find(document.html())
        ?.groupValues?.get(1)
        val episodeElements = document.select("div.swiper-slide a.w-full")
        val isMovie = episodeElements.any { it.attr("href").contains("-movie", ignoreCase = true) }

        val seasonLinks = document
            .select("section.mbe-5 div.relative div.swiper-slide a")
            .map { it.attr("href") }
            .distinct()
            .filter { it.contains("-sezon", ignoreCase = true) }

        val firstSeasonEpisodes: List<Episode> = episodeElements.mapNotNull { episodeElement ->
            val epHref    = fixUrlNull(episodeElement.attr("href")) ?: return@mapNotNull null
            val epNumber  = episodeElement.selectFirst("span.absolute")
                ?.text()?.substringAfterLast(" ")
                ?.toIntOrNull()
            newEpisode(epHref) {
                name    = " Bölüm"
                episode = epNumber
                season  = 1
            }
        }

        val otherSeasons: List<List<Episode>> = seasonLinks.map { sezonUrl ->
            val doc = app.get(sezonUrl).document
            doc.select("div.swiper-slide a.w-full").mapNotNull { episodeElement ->
                val epHref   = fixUrlNull(episodeElement.attr("href")) ?: return@mapNotNull null
                val epSeason = epHref.substringBeforeLast("-sezon")
                    .substringAfterLast("-")
                    .toIntOrNull()
                val epNumber = episodeElement.selectFirst("span.absolute")
                    ?.text()?.substringAfterLast(" ")
                    ?.toIntOrNull()
                newEpisode(epHref) {
                    name    = " Bölüm"
                    episode = epNumber
                    season  = epSeason
                }
            }
        }

        val allEpisodes: List<Episode> = firstSeasonEpisodes + otherSeasons.flatten()

        val episodeMap: MutableMap<DubStatus, List<Episode>> =
            mutableMapOf(DubStatus.Subbed to allEpisodes)

        Log.d("Animeler", "filmmi = $isMovie")


        return if (isMovie) {
            newAnimeLoadResponse(title, url, TvType.AnimeMovie, true) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                this.recommendations = recommendations
                addTrailer(trailer)
            }
        } else {
            newAnimeLoadResponse(title, url, TvType.Anime) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                this.recommendations = recommendations
                this.episodes = episodeMap
                addTrailer(trailer)
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("div.bg-gradient-to-t h3 span")?.attr("data-en-title") ?: return null
        Log.d("Animeler", "title = $title")
        val href = fixUrlNull(this.selectFirst("div.bg-gradient-to-t h3")?.attr("href")) ?: return null
        Log.d("Animeler", "href = $href")
        val posterUrl = fixUrlNull(this.selectFirst("img.absolute.inset-0")?.attr("src"))
        Log.d("Animeler", "posterurl = $posterUrl")

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("Animeler", "data = $data")

        val document = app.get(data).document
        val linkContainer = document.select("div.episode-player-box")
        if (linkContainer.isEmpty()) {
            Log.w("Animeler", "No player container found")
            return false
        }

        val videoSource = linkContainer.select("video > source").attr("src")
        val iframeUrl = linkContainer.select("iframe").attr("src")
        Log.d("Animeler", "iframeUrl = $iframeUrl")

        try {
            if (videoSource.endsWith(".m3u8")) {
                callback(
                    newExtractorLink(
                        name = "Animeizlesene",
                        source = "Animeizlesene",
                        url = videoSource,
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.quality = Qualities.Unknown.value
                        referer = mainUrl
                    }
                )
                return true
            }

            when {
                iframeUrl.contains("play.animeler.pw/fireplayer/video") -> {
                    val absoluteIframe = URI(mainUrl).resolve(iframeUrl).toString()
                    val uri = URI(absoluteIframe)
                    val postUrl = "${uri.scheme}://${uri.host}${uri.path}"
                    val hashVid = iframeUrl.substringAfter("video/")
                    val encodedReferer = URLEncoder.encode(mainUrl, "UTF-8")

                    val headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
                        "Accept" to "*/*",
                        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                        "X-Requested-With" to "XMLHttpRequest",
                        "Origin" to "${uri.scheme}://${uri.host}",
                        "Connection" to "keep-alive",
                        "Sec-Fetch-Dest" to "empty",
                        "Sec-Fetch-Mode" to "cors",
                        "Sec-Fetch-Site" to "same-origin",
                        "TE" to "trailers"
                    )

                    val formData = mapOf(
                        "hash" to hashVid,
                        "r" to encodedReferer
                    )

                    val initResp = app.post(
                        url = "$postUrl?do=getVideo",
                        headers = headers,
                        referer = mainUrl,
                        data = formData
                    )

                    if (!initResp.isSuccessful) {
                        Log.e("Animeler", "Initial POST failed: ${initResp.code}")
                        return false
                    }

                    val responseJson = JSONObject(initResp.body!!.string())
                    val sourceList = responseJson.getJSONObject("sourceList")

                    Log.d("Animeler", "Found ${sourceList.length()} sources")

                    coroutineScope {
                        sourceList.keys().asSequence().map { key ->
                            async {
                                try {
                                    val sourceData = formData.toMutableMap()
                                    sourceData["s"] = key

                                    val resp2 = app.post(
                                        url = "$postUrl?do=getVideo",
                                        headers = headers,
                                        referer = mainUrl,
                                        data = sourceData
                                    )

                                    if (resp2.isSuccessful) {
                                        val jsonResponse = resp2.body!!.string()
                                        Log.d("Animeler", "Source $key response: $jsonResponse")

                                        val videoObject = JSONObject(jsonResponse)
                                        val videoSrc = videoObject.optString("videoSrc", "")

                                        if (videoSrc.isNotEmpty()) {
                                            Log.d("Animeler", "Found video source for $key: $videoSrc")

                                            loadExtractor(
                                                url = videoSrc,
                                                referer = mainUrl,
                                                subtitleCallback = subtitleCallback,
                                                callback = callback
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("Animeler", "Error loading video for key $key$e")
                                }
                            }
                        }.toList().awaitAll()
                    }

                    return true
                }

                else -> {
                    loadExtractor(
                        url = iframeUrl,
                        referer = mainUrl,
                        subtitleCallback = subtitleCallback,
                        callback = callback
                    )
                    return true
                }
            }

        } catch (e: Exception) {
            Log.e("Animeler", "Error loading links $e")
            return false
        }
    }
}