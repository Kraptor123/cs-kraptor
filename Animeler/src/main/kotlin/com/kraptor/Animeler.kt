// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
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
        "${mainUrl}/genre/action/"           to "Action",
        "${mainUrl}/genre/adult-cast/"                   to "Adult Cast",
        "${mainUrl}/genre/adventure/"                    to "Adventure",
        "${mainUrl}/genre/aksiyon/"                      to "Aksiyon",
        "${mainUrl}/genre/antropomorfik/"                to "Antropomorfik",
        "${mainUrl}/genre/arabalar/"                     to "Arabalar",
        "${mainUrl}/genre/ask-ucgeni/"                   to "Aşk Üçgeni",
        "${mainUrl}/genre/military/"                     to "Askeri",
        "${mainUrl}/genre/avangart/"                     to "Avangart",
        "${mainUrl}/genre/avant-garde/"                  to "Avant Garde",
        "${mainUrl}/genre/bilim-kurgu/"                  to "Bilim Kurgu",
        "${mainUrl}/genre/boys-love/"                    to "Boys Love",
        "${mainUrl}/genre/buyu/"                         to "Büyü",
        "${mainUrl}/genre/cute-girls-doing-cute-things/" to "CGDCT",
        "${mainUrl}/genre/childcare/"                    to "Childcare",
        "${mainUrl}/genre/cocuk-bakimi/"                 to "Çocuk Bakımı",
        "${mainUrl}/genre/cocuklar/"                     to "Çocuklar",
        "${mainUrl}/genre/comedy/"                       to "Comedy",
        "${mainUrl}/genre/comic/"                        to "Comic",
        "${mainUrl}/genre/cultivation/"                  to "Cultivation",
        "${mainUrl}/genre/dedektif/"                     to "Dedektif",
        "${mainUrl}/genre/delinquents/"                  to "Delinquents",
        "${mainUrl}/genre/demons/"                       to "Demons",
        "${mainUrl}/genre/dogaustu-gucler/"              to "Doğaüstü Güçler",
        "${mainUrl}/genre/dovus-sanatlari/"              to "Dövüş Sanatları",
        "${mainUrl}/genre/dram/"                         to "Dram",
        "${mainUrl}/genre/drama/"                        to "Drama",
        "${mainUrl}/genre/ecchi/"                        to "Ecchi",
        "${mainUrl}/genre/fantastik/"                    to "Fantastik",
        "${mainUrl}/genre/fantasy/"                      to "Fantasy",
        "${mainUrl}/genre/gag-humor/"                    to "Gag Humor",
        "${mainUrl}/genre/gerilim/"                      to "Gerilim",
        "${mainUrl}/genre/girls-love/"                   to "Girls Love",
        "${mainUrl}/genre/gizem/"                        to "Gizem",
        "${mainUrl}/genre/gore/"                         to "Gore",
        "${mainUrl}/genre/gourmet/"                      to "Gourmet",
        "${mainUrl}/genre/harem/"                        to "Harem",
        "${mainUrl}/genre/historical/"                   to "Historical",
        "${mainUrl}/genre/horror/"                       to "Horror",
        "${mainUrl}/genre/idol/"                         to "İdol",
        "${mainUrl}/genre/idols-female/"                 to "Idols (Female)",
        "${mainUrl}/genre/isekai-2/"                     to "Isekai",
        "${mainUrl}/genre/iyashikei/"                    to "Iyashikei",
        "${mainUrl}/genre/josei/"                        to "Josei",
        "${mainUrl}/genre/komedi/"                       to "Komedi",
        "${mainUrl}/genre/korku/"                        to "Korku",
        "${mainUrl}/genre/kumar-oyunu/"                  to "Kumar Oyunu",
        "${mainUrl}/genre/macera/"                       to "Macera",
        "${mainUrl}/genre/mahou-shoujo/"                 to "Mahou Shoujo",
        "${mainUrl}/genre/martial-arts/"                 to "Martial Arts",
        "${mainUrl}/genre/mecha/"                        to "Mecha",
        "${mainUrl}/genre/medikal/"                      to "Medikal",
        "${mainUrl}/genre/military-2/"                   to "Military",
        "${mainUrl}/genre/mitoloji/"                     to "Mitoloji",
        "${mainUrl}/genre/music/"                        to "Music",
        "${mainUrl}/genre/muzik/"                        to "Müzik",
        "${mainUrl}/genre/mystery/"                      to "Mystery",
        "${mainUrl}/genre/mythology/"                    to "Mythology",
        "${mainUrl}/genre/okul/"                         to "Okul",
        "${mainUrl}/genre/op-m-c/"                       to "OP M.C.",
        "${mainUrl}/genre/oyun/"                         to "Oyun",
        "${mainUrl}/genre/parodi/"                       to "Parodi",
        "${mainUrl}/genre/polisiye/"                     to "Polisiye",
        "${mainUrl}/genre/psikolojik/"                   to "Psikolojik",
        "${mainUrl}/genre/psychological/"                to "Psychological",
        "${mainUrl}/genre/rebirth/"                      to "Rebirth",
        "${mainUrl}/genre/reenkarnasyon/"                to "Reenkarnasyon",
        "${mainUrl}/genre/reincarnation/"                to "Reincarnation",
        "${mainUrl}/genre/revenge/"                      to "Revenge",
        "${mainUrl}/genre/romance/"                      to "Romance",
        "${mainUrl}/genre/romantic-subtext/"             to "Romantic Subtext",
        "${mainUrl}/genre/romantizm/"                    to "Romantizm",
        "${mainUrl}/genre/sahne-sanatcilari/"            to "Sahne Sanatçıları",
        "${mainUrl}/genre/samuray/"                      to "Samuray",
        "${mainUrl}/genre/school/"                       to "School",
        "${mainUrl}/genre/sci-fi/"                       to "Sci-Fi",
        "${mainUrl}/genre/seinen/"                       to "Seinen",
        "${mainUrl}/genre/seytan/"                       to "Şeytan",
        "${mainUrl}/genre/shoujo/"                       to "Shoujo",
        "${mainUrl}/genre/shoujo-ai/"                    to "Shoujo Ai",
        "${mainUrl}/genre/shounen/"                      to "Shounen",
        "${mainUrl}/genre/shounen-ai/"                   to "Shounen Ai",
        "${mainUrl}/genre/slice-of-life/"                to "Slice of Life",
        "${mainUrl}/genre/spor/"                         to "Spor",
        "${mainUrl}/genre/sports/"                       to "Sports",
        "${mainUrl}/genre/strategy-game/"                to "Strategy Game",
        "${mainUrl}/genre/strateji-oyunu/"               to "Strateji Oyunu",
        "${mainUrl}/genre/super-gucler/"                 to "Süper Güçler",
        "${mainUrl}/genre/super-power/"                  to "Super Power",
        "${mainUrl}/genre/supernatural/"                 to "Supernatural",
        "${mainUrl}/genre/suspense/"                     to "Suspense",
        "${mainUrl}/genre/tarihi/"                       to "Tarihi",
        "${mainUrl}/genre/team-sports/"                  to "Team Sports",
        "${mainUrl}/genre/time-travel/"                  to "Time Travel",
        "${mainUrl}/genre/uzay/"                         to "Uzay",
        "${mainUrl}/genre/vampir/"                       to "Vampir",
        "${mainUrl}/genre/video-game/"                   to "Video Game",
        "${mainUrl}/genre/visual-arts/"                  to "Visual Arts",
        "${mainUrl}/genre/workplace/"                    to "Workplace",
        "${mainUrl}/genre/yasamdan-kesitler/"            to "Yaşamdan Kesitler",
        "${mainUrl}/genre/yemek/"                        to "Yemek",
        "${mainUrl}/genre/yetiskin-karakterler/"         to "Yetişkin Karakterler",
        "${mainUrl}/genre/zaman-yolculugu/"              to "Zaman Yolculuğu"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            app.get(request.data).document
        } else {
            app.get("${request.data}/page/$page/").document
        }
        val home =
            url.select("div.w-full.bg-gradient-to-t.from-primary.to-transparent.rounded.overflow-hidden.shadow.shadow-primary")
                .mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2 a")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

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
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst(".xl\\:w-full h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.anime-image img")?.attr("src"))
        val description = document.selectFirst("div.line-clamp-3")?.text()?.trim()
        val tags = document.select("span.leading-6 a").map { it.text() }
        val elements = document.select("li.list-none.mbe-1")
        val rating = Regex("MAL:\\s*(\\d+(?:\\.\\d+)?)").find(elements.text())?.groups?.get(1)?.value?.toRatingInt()
        val duration = Regex("Süre:\\s*(\\d+)").find(elements.text())?.groups?.get(1)?.value?.toInt()
        val year = Regex("(\\d{4})").find(elements.text())?.groups?.get(1)?.value?.toInt()
        val recommendations =
            document.select("div.w-full.bg-gradient-to-t.from-primary.to-transparent.rounded.overflow-hidden.shadow.shadow-primary")
                .mapNotNull { it.toRecommendationResult() }
        val trailer = Regex("""embed/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        val episodeElements = document.select("div.swiper-slide a")
        val isMovie = episodeElements.any { it.attr("href").contains("-movie", ignoreCase = true) }

        val episodeList = episodeElements.mapNotNull { episodeElement ->
            val epHref = fixUrlNull(episodeElement.attr("href")) ?: return@mapNotNull null
            val titleText = episodeElement.attr("title").trim()
            val match = Regex("^(\\d+)\\. Bölüm\\s*-\\s*(.*)$").find(titleText)

            val epNumber = match?.groups?.get(1)?.value?.toInt()
            val epTitle = match?.groups?.get(2)?.value?.trim()

            newEpisode(epHref) {
                this.name = epTitle
                this.episode = epNumber
            }
        }.let { list ->
            mutableMapOf(DubStatus.Subbed to list)
        }

        Log.d("Animeler", "filmmi = $isMovie")

        return if (isMovie) {
            newMovieLoadResponse(title, url, TvType.AnimeMovie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.rating = rating
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
                this.rating = rating
                this.duration = duration
                this.recommendations = recommendations
                this.episodes = episodeList
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
        val linkContainer = document.select("div.player")
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
                                    Log.e("Animeler", "Error loading video for key $key", e)
                                }
                            }
                        }.toList().awaitAll()
                    }

                    return true
                }

                iframeUrl.contains("anizmplayer.com") -> {
                    AincradExtractor().getUrl(iframeUrl, mainUrl).forEach(callback)
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
            Log.e("Animeler", "Error loading links", e)
            return false
        }
    }
}