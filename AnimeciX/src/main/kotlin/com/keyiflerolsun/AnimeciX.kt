// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject
import kotlin.collections.mapOf
import kotlin.*
import kotlin.text.*

class AnimeciX : MainAPI() {
    override var mainUrl = "https://animecix.tv"
    override var name = "AnimeciX"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Anime)

    override var sequentialMainPage =
        true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay = 200L  // ? 0.20 saniye
    override var sequentialMainPageScrollDelay = 200L  // ? 0.20 saniye

    override val mainPage = mainPageOf(
        "${mainUrl}/secure/last-episodes" to "Son Eklenen Bölümler",
        "${mainUrl}/secure/titles?type=series&onlyStreamable=true" to "Animeler",
        "${mainUrl}/secure/titles?type=movie&onlyStreamable=true" to "Anime Filmleri",
        "${mainUrl}/secure/titles?genre=action&onlyStreamable=true" to "Aksiyon",
        "${mainUrl}/secure/titles?keyword=military&onlyStreamable=true" to "Askeri",
        "${mainUrl}/secure/titles?keyword=magic&onlyStreamable=true" to "Büyü",
        "${mainUrl}/secure/titles?genre=drama&onlyStreamable=true" to "Dram",
        "${mainUrl}/secure/titles?keyword=sport&onlyStreamable=true" to "Spor",
        "${mainUrl}/secure/titles?genre=thriller&onlyStreamable=true" to "Gerilim",
        "${mainUrl}/secure/titles?genre=mystery&onlyStreamable=true" to "Gizem",
        "${mainUrl}/secure/titles?genre=comedy&onlyStreamable=true" to "Komedi",
        "${mainUrl}/secure/titles?keyword=school&onlyStreamable=true" to "Okul",
        "${mainUrl}/secure/titles?keyword=isekai&onlyStreamable=true" to "Isekai",
        "${mainUrl}/secure/titles?keyword=shounen&onlyStreamable=true" to "Shounen",
        "${mainUrl}/secure/titles?keyword=shoujo&onlyStreamable=true" to "Shoujo",
        "${mainUrl}/secure/titles?keyword=seinen&onlyStreamable=true" to "Seinen",
        "${mainUrl}/secure/titles?genre=romance&onlyStreamable=true" to "Romance",
        "${mainUrl}/secure/titles?keyword=harem&onlyStreamable=true" to "Harem",
        "${mainUrl}/secure/titles?keyword=ecchi&onlyStreamable=true" to "Ecchi",

        )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        return if (request.data.contains("/last-episodes")) {
            val response = app.get(
                "${mainUrl}/secure/last-episodes?page=$page&perPage=10",
                headers = mapOf(
                    "x-e-h" to "7Y2ozlO+QysR5w9Q6Tupmtvl9jJp7ThFH8SB+Lo7NvZjgjqRSqOgcT2v4ISM9sP10LmnlYI8WQ==.xrlyOBFS5BHjQ2Lk"
                )
            ).parsedSafe<LastEpisodesResponse>()?.data ?: emptyList()

            val home = response.map {
                val formattedTitle = "S${it.seasonNumber}B${it.episodeNumber} - ${it.titleName}"
                newAnimeSearchResponse(
                    formattedTitle,
                    "${mainUrl}/secure/titles/${it.titleId}?titleId=${it.titleId}",
                    TvType.Anime
                ) {
                    this.posterUrl = fixUrlNull(it.titlePoster)
                    this.score     = Score.from10(it.rating)
                    this.episodes  = mutableMapOf(DubStatus.Subbed to it.episodeCount)
                }
            }

            newHomePageResponse(request.name, home)
        } else {
            val response = app.get(
                "${request.data}&page=${page}&perPage=16",
                headers = mapOf(
                    "x-e-h" to "7Y2ozlO+QysR5w9Q6Tupmtvl9jJp7ThFH8SB+Lo7NvZjgjqRSqOgcT2v4ISM9sP10LmnlYI8WQ==.xrlyOBFS5BHjQ2Lk"
                )
            ).parsedSafe<Category>()

            val home = response?.pagination?.data?.map { anime ->
                newAnimeSearchResponse(
                    anime.title,
                    "${mainUrl}/secure/titles/${anime.id}?titleId=${anime.id}",
                    TvType.Anime
                ) {
                    this.posterUrl = fixUrlNull(anime.poster)
                    this.score     = Score.from10(anime.rating)
                    this.episodes  = mutableMapOf(DubStatus.Subbed to anime.episodeCount)
                }
            } ?: listOf()

            newHomePageResponse(request.name, home)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("${mainUrl}/secure/search/${query}?limit=20").parsedSafe<Search>() ?: return listOf()

        return response.results.map { anime ->
            newAnimeSearchResponse(
                anime.title,
                "${mainUrl}/secure/titles/${anime.id}?titleId=${anime.id}",
                TvType.Anime
            ) {
                this.posterUrl = fixUrlNull(anime.poster)
                this.score     = Score.from10(anime.rating)
                this.episodes  = mutableMapOf(DubStatus.Subbed to anime.episodeCount)
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        // Gerekli başlıkları ayarla
        val headers = mapOf(
            "x-e-h" to "7Y2ozlO+QysR5w9Q6Tupmtvl9jJp7ThFH8SB+Lo7NvZjgjqRSqOgcT2v4ISM9sP10LmnlYI8WQ==.xrlyOBFS5BHjQ2Lk"
        )

        // İlk isteği yapıp Title objesini al
        val response = app.get(url, headers = headers)
        val title: Title? = try {
            response.parser?.parseSafe<Title>(response.text, Title::class)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (title == null) return null

        // URL'den titleId parametresini çıkar
        val titleId = url.substringAfter("?titleId=")
        val episodes = mutableListOf<Episode>()

        // Eğer tür anime ise her sezon için ilgili videoları yükle
        if (title.title.titleType == "anime") {
            for (season in title.title.seasons) {
                val relatedUrl = "$mainUrl/secure/related-videos" +
                        "?episode=1&season=${season.number}&videoId=0&titleId=$titleId"
                val relResp = app.get(relatedUrl)
                // JSON'u doğrudan TitleVideos sınıfına çevir
                val titleVideos: TitleVideos? = ObjectMapper()
                    .readValue(relResp.body.string(), TitleVideos::class.java)

                Log.d("kraptor_$name","titleVideos = ${titleVideos}")

                titleVideos?.videos?.forEach { video ->
                    Log.d("kraptor_$name","bolum poster = ${video.poster}")
                    episodes.add(

                        newEpisode(video.url)
                        {
                            this.name = video.name
                            this.season = video.seasonNum
                            this.episode = video.episodeNum
                            this.posterUrl = video.poster
                            this.description = video.description
                        }
                    )
                }
            }

            // Anime için LoadResponse oluştur
            return newAnimeLoadResponse(
                title.title.title,
                "$mainUrl/secure/titles/${title.title.id}?titleId=${title.title.id}",
                TvType.Anime,
                true,

            ) {
                this.posterUrl = fixUrlNull(title.title.poster)
                this.episodes  = mutableMapOf(DubStatus.Subbed to episodes)
                this.year = title.title.year
                this.plot = title.title.description
                this.tags = title.title.tags.map { it.name }
                this.score = Score.from10(title.title.rating.toString())
                this.addActors(
                    title.title.actors.map { Actor(it.name, fixUrlNull(it.poster)) }
                )
                this.addTrailer(title.title.trailer)
            }
        } else {
            // Film için eski kodun mantığını kullan
            if (title.title.videos.isNotEmpty()) {
                return newMovieLoadResponse(
                    title.title.title,
                    "${mainUrl}/secure/titles/${title.title.id}?titleId=${title.title.id}",
                    TvType.AnimeMovie,
                    "${mainUrl}/secure/titles/${title.title.id}?titleId=${title.title.id}"
                ) {
                    this.posterUrl = fixUrlNull(title.title.poster)
                    this.year = title.title.year
                    this.plot = title.title.description
                    this.tags = title.title.tags.map { it.name }
                    this.score = Score.from10(title.title.rating.toString())
                    addActors(title.title.actors.map { Actor(it.name, fixUrlNull(it.poster)) })
                    addTrailer(title.title.trailer)
                }
            }
        }

        return null
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_ACX", "data » $data")

        // Film için eski kodun mantığını kullan (data URL'si anm.cx içeriyorsa)
        val iframeResponse = app.get(data, referer = "$mainUrl/", allowRedirects = true)
        val iframeLink = iframeResponse.url
        Log.d("kraptor_ACX", "Final iframeLink » $iframeLink")

        if (iframeLink.contains("anm.cx")) {
            val rawJson = app
                .get(
                    data, headers = mapOf(
                        "x-e-h" to "7Y2ozlO+QysR5w9Q6Tupmtvl9jJp7ThFH8SB+Lo7NvZjgjqRSqOgcT2v4ISM9sP10LmnlYI8WQ==.xrlyOBFS5BHjQ2Lk"
                    )
                )
                .body
                .string()
            Log.d("kraptor_ACX", "rawJson =  $rawJson")
            val json = JSONObject(rawJson)
            val videosArray = json.getJSONObject("title").getJSONArray("videos")
            for (i in 0 until videosArray.length()) {
                val videoObj = videosArray.getJSONObject(i)
                val url = videoObj.getString("url")
                val videoName = videoObj.getString("extra")

                // Eğer tmdb, anm.cx veya youtube içeriyorsa geç
                if (url.contains("tmdb", ignoreCase = true)
                    || url.contains("anm.cx", ignoreCase = true)
                    || url.contains("youtube", ignoreCase = true)
                ) continue

                Log.d("kraptor_ACX", "Video URL: $url")
                loadExtractor(url = url, "$mainUrl/", subtitleCallback, callback)
            }
        } else {
            // Anime için yeni kodun mantığını kullan
            val episodeVideosUrl = "${mainUrl}/secure/episode-videos"
            val requestUrl = data.replaceBefore("?", episodeVideosUrl)

            val response = app.get(
                url = requestUrl,
                referer = "${mainUrl}/"
            )

            val videoList = response.parsed<List<Map<String, Any>>>()

            val videos = videoList.mapNotNull { videoData ->
                val episodeNum = (videoData["episode_num"] as? Number)?.toInt()
                val seasonNum = (videoData["season_num"] as? Number)?.toInt()
                val videoUrl = videoData["url"] as? String
                val extra = videoData["extra"] as? String
                val poster = videoData["poster"] as? String
                val description = videoData["description"] as? String
                val name        = videoData["name"] as? String

                if (videoUrl != null) {
                    Video(episodeNum, seasonNum, videoUrl, extra, poster = poster , description = description, name = name)
                } else null
            }

            for (video in videos) {
                val requestData = "${video.url}|${video.extra}"
                if (listOf("tau-video", "sibnet").any { video.url.contains(it) }) {
                    Log.d("kraptor_ACX", "liste  =  $requestData")
                    loadExtractor(
                        requestData,
                        "${mainUrl}/",
                        subtitleCallback,
                        callback
                    )
                } else {
                    Log.d("kraptor_ACX", "else url  =  ${video.url}")
                    loadExtractor(video.url, "${mainUrl}/", subtitleCallback, callback)
                }
            }
        }
        return true
    }
}