// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Animeler : MainAPI() {
    override var mainUrl = "https://animeler.me"
    override var name = "Animeler"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Anime)

    override val mainPage = mainPageOf(
        "${mainUrl}/genre/action/" to "Action",
        "${mainUrl}/genre/adult-cast/" to "Adult Cast",
        "${mainUrl}/genre/adventure/" to "Adventure",
        "${mainUrl}/genre/aksiyon/" to "Aksiyon",
        "${mainUrl}/genre/antropomorfik/" to "Antropomorfik",
        "${mainUrl}/genre/arabalar/" to "Arabalar",
        "${mainUrl}/genre/ask-ucgeni/" to "Aşk Üçgeni",
        "${mainUrl}/genre/military/" to "Askeri",
        "${mainUrl}/genre/avangart/" to "Avangart",
        "${mainUrl}/genre/avant-garde/" to "Avant Garde",
        "${mainUrl}/genre/bilim-kurgu/" to "Bilim Kurgu",
        "${mainUrl}/genre/boys-love/" to "Boys Love",
        "${mainUrl}/genre/buyu/" to "Büyü",
        "${mainUrl}/genre/cute-girls-doing-cute-things/" to "CGDCT",
        "${mainUrl}/genre/childcare/" to "Childcare",
        "${mainUrl}/genre/cocuk-bakimi/" to "Çocuk Bakımı",
        "${mainUrl}/genre/cocuklar/" to "Çocuklar",
        "${mainUrl}/genre/comedy/" to "Comedy",
        "${mainUrl}/genre/comic/" to "Comic",
        "${mainUrl}/genre/cultivation/" to "Cultivation",
        "${mainUrl}/genre/dedektif/" to "Dedektif",
        "${mainUrl}/genre/delinquents/" to "Delinquents",
        "${mainUrl}/genre/demons/" to "Demons",
        "${mainUrl}/genre/dogaustu-gucler/" to "Doğaüstü Güçler",
        "${mainUrl}/genre/dovus-sanatlari/" to "Dövüş Sanatları",
        "${mainUrl}/genre/dram/" to "Dram",
        "${mainUrl}/genre/drama/" to "Drama",
        "${mainUrl}/genre/ecchi/" to "Ecchi",
        "${mainUrl}/genre/fantastik/" to "Fantastik",
        "${mainUrl}/genre/fantasy/" to "Fantasy",
        "${mainUrl}/genre/gag-humor/" to "Gag Humor",
        "${mainUrl}/genre/gerilim/" to "Gerilim",
        "${mainUrl}/genre/girls-love/" to "Girls Love",
        "${mainUrl}/genre/gizem/" to "Gizem",
        "${mainUrl}/genre/gore/" to "Gore",
        "${mainUrl}/genre/gourmet/" to "Gourmet",
        "${mainUrl}/genre/harem/" to "Harem",
        "${mainUrl}/genre/historical/" to "Historical",
        "${mainUrl}/genre/horror/" to "Horror",
        "${mainUrl}/genre/idol/" to "İdol",
        "${mainUrl}/genre/idols-female/" to "Idols (Female)",
        "${mainUrl}/genre/isekai-2/" to "Isekai",
        "${mainUrl}/genre/iyashikei/" to "Iyashikei",
        "${mainUrl}/genre/josei/" to "Josei",
        "${mainUrl}/genre/komedi/" to "Komedi",
        "${mainUrl}/genre/korku/" to "Korku",
        "${mainUrl}/genre/kumar-oyunu/" to "Kumar Oyunu",
        "${mainUrl}/genre/macera/" to "Macera",
        "${mainUrl}/genre/mahou-shoujo/" to "Mahou Shoujo",
        "${mainUrl}/genre/martial-arts/" to "Martial Arts",
        "${mainUrl}/genre/mecha/" to "Mecha",
        "${mainUrl}/genre/medikal/" to "Medikal",
        "${mainUrl}/genre/military-2/" to "Military",
        "${mainUrl}/genre/mitoloji/" to "Mitoloji",
        "${mainUrl}/genre/music/" to "Music",
        "${mainUrl}/genre/muzik/" to "Müzik",
        "${mainUrl}/genre/mystery/" to "Mystery",
        "${mainUrl}/genre/mythology/" to "Mythology",
        "${mainUrl}/genre/okul/" to "Okul",
        "${mainUrl}/genre/op-m-c/" to "OP M.C.",
        "${mainUrl}/genre/oyun/" to "Oyun",
        "${mainUrl}/genre/parodi/" to "Parodi",
        "${mainUrl}/genre/polisiye/" to "Polisiye",
        "${mainUrl}/genre/psikolojik/" to "Psikolojik",
        "${mainUrl}/genre/psychological/" to "Psychological",
        "${mainUrl}/genre/rebirth/" to "Rebirth",
        "${mainUrl}/genre/reenkarnasyon/" to "Reenkarnasyon",
        "${mainUrl}/genre/reincarnation/" to "Reincarnation",
        "${mainUrl}/genre/revenge/" to "Revenge",
        "${mainUrl}/genre/romance/" to "Romance",
        "${mainUrl}/genre/romantic-subtext/" to "Romantic Subtext",
        "${mainUrl}/genre/romantizm/" to "Romantizm",
        "${mainUrl}/genre/sahne-sanatcilari/" to "Sahne Sanatçıları",
        "${mainUrl}/genre/samuray/" to "Samuray",
        "${mainUrl}/genre/school/" to "School",
        "${mainUrl}/genre/sci-fi/" to "Sci-Fi",
        "${mainUrl}/genre/seinen/" to "Seinen",
        "${mainUrl}/genre/seytan/" to "Şeytan",
        "${mainUrl}/genre/shoujo/" to "Shoujo",
        "${mainUrl}/genre/shoujo-ai/" to "Shoujo Ai",
        "${mainUrl}/genre/shounen/" to "Shounen",
        "${mainUrl}/genre/shounen-ai/" to "Shounen Ai",
        "${mainUrl}/genre/slice-of-life/" to "Slice of Life",
        "${mainUrl}/genre/spor/" to "Spor",
        "${mainUrl}/genre/sports/" to "Sports",
        "${mainUrl}/genre/strategy-game/" to "Strategy Game",
        "${mainUrl}/genre/strateji-oyunu/" to "Strateji Oyunu",
        "${mainUrl}/genre/super-gucler/" to "Süper Güçler",
        "${mainUrl}/genre/super-power/" to "Super Power",
        "${mainUrl}/genre/supernatural/" to "Supernatural",
        "${mainUrl}/genre/suspense/" to "Suspense",
        "${mainUrl}/genre/tarihi/" to "Tarihi",
        "${mainUrl}/genre/team-sports/" to "Team Sports",
        "${mainUrl}/genre/time-travel/" to "Time Travel",
        "${mainUrl}/genre/uzay/" to "Uzay",
        "${mainUrl}/genre/vampir/" to "Vampir",
        "${mainUrl}/genre/video-game/" to "Video Game",
        "${mainUrl}/genre/visual-arts/" to "Visual Arts",
        "${mainUrl}/genre/workplace/" to "Workplace",
        "${mainUrl}/genre/yasamdan-kesitler/" to "Yaşamdan Kesitler",
        "${mainUrl}/genre/yemek/" to "Yemek",
        "${mainUrl}/genre/yetiskin-karakterler/" to "Yetişkin Karakterler",
        "${mainUrl}/genre/zaman-yolculugu/" to "Zaman Yolculuğu"
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
        val title = this.selectFirst("span.show")?.text() ?: return null
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

        // Fetch and parse the document
        val document = app.get(data).document
        val linkContainer = document.select("div.player")
        if (linkContainer.isEmpty()) {
            Log.w("Animeler", "No player container found")
            return false
        }

        // Extract the iframe source URL
        val link = linkContainer.select("iframe").attr("src")
        Log.d("Animeler", "link = $link")

     val videoSource = linkContainer.select("video > source").attr("src")
        
        

        try {
            if (videoSource.isNotEmpty() && videoSource.endsWith(".m3u8")) {
         callback(
            ExtractorLink(
               name = "Animeizlesene",
               source = "Animeizlesene",
               url = videoSource,
               referer = mainUrl,
               quality = Qualities.Unknown.value,
               type = ExtractorLink.Type.M3U8
               )
              )
            }
            when {
                link.contains("anizmplayer.com") -> {
                    AincradExtractor().getUrl(link, mainUrl).forEach(callback)
                }

                else -> {
                    loadExtractor(
                        url = link,
                        referer = mainUrl,
                        subtitleCallback = subtitleCallback,
                        callback = callback
                    )
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("Animeler", "Error loading links", e)
            return false
        }
    }
}