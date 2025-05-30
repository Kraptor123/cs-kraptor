// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.mapOf
import kotlin.sequences.forEach

class AnimeciX : MainAPI() {
    override var mainUrl = "https://anm.cx"
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
            }
        } ?: listOf<SearchResponse>()

        return newHomePageResponse(request.name, home)
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
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val response = app.get(
            url,
            headers = mapOf(
                "x-e-h" to "7Y2ozlO+QysR5w9Q6Tupmtvl9jJp7ThFH8SB+Lo7NvZjgjqRSqOgcT2v4ISM9sP10LmnlYI8WQ==.xrlyOBFS5BHjQ2Lk"
            )
        ).parsedSafe<Title>() ?: return null
        val episodes = mutableListOf<Episode>()
        val titleId = url.substringAfter("?titleId=")

        if (response.title.titleType == "anime") {
            for (sezon in response.title.seasons) {
                val sezonJson = app.get(
                    "$mainUrl/secure/related-videos?episode=1&season=${sezon.number}&videoId=0&titleId=$titleId",
                    headers = mapOf(
                        "x-e-h" to "7Y2ozlO+QysR5w9Q6Tupmtvl9jJp7ThFH8SB+Lo7NvZjgjqRSqOgcT2v4ISM9sP10LmnlYI8WQ==.xrlyOBFS5BHjQ2Lk"
                    )
                ).body.string()

                // Gelen JSON: { "videos": [ {...}, {...}, ... ] }
                val rootObj    = JSONObject(sezonJson)
                val videosArray = rootObj.getJSONArray("videos")

                for (i in 0 until videosArray.length()) {
                    val videoObj   = videosArray.getJSONObject(i)
                    val url        = videoObj.getString("url")
                    val seasonNum  = videoObj.getInt("season_num")
                    val episodeNum = videoObj.getInt("episode_num")
                    val fansub     = videoObj.optString("extra")  // String olabildiği için optString

                    if (listOf("tmdb", "anm.cx", "youtube").any { url.contains(it, ignoreCase = true) })
                        continue

                    episodes.add(newEpisode(url) {
                        this.name = buildString {append("${seasonNum}. Sezon ${episodeNum}. Bölüm")
                        }
                        this.season  = seasonNum
                        this.episode = episodeNum
                    })
                }
            }
        }else {
            if (response.title.videos.isNotEmpty()) {
                return newMovieLoadResponse(
                    response.title.title,
                    "${mainUrl}/secure/titles/${response.title.id}?titleId=${response.title.id}",
                    TvType.AnimeMovie,
                    "${mainUrl}/secure/titles/${response.title.id}?titleId=${response.title.id}"
                ) {
                    this.posterUrl = fixUrlNull(response.title.poster)
                    this.year = response.title.year
                    this.plot = response.title.description
                    this.tags = response.title.tags.map { it.name }
                    this.rating = response.title.rating.toRatingInt()
                    addActors(response.title.actors.map { Actor(it.name, fixUrlNull(it.poster)) })
                    addTrailer(response.title.trailer)
                }
            }
        }

        return newAnimeLoadResponse(
            response.title.title,
            "${mainUrl}/secure/titles/${response.title.id}?titleId=${response.title.id}",
            TvType.Anime,
            true
        ) {
            this.posterUrl = fixUrlNull(response.title.poster)
            this.year = response.title.year
            this.plot = response.title.description
            this.episodes = mutableMapOf(DubStatus.Subbed to episodes)
            this.tags = response.title.tags.map { it.name }
            this.rating = response.title.rating.toRatingInt()
            addActors(response.title.actors.map { Actor(it.name, fixUrlNull(it.poster)) })
            addTrailer(response.title.trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("ACX", "data » $data")

        val iframeResponse = app.get(data, referer = "$mainUrl/", allowRedirects = true)
        val iframeLink = iframeResponse.url
        Log.d("ACX", "Final iframeLink » $iframeLink")

        if (iframeLink.contains("anm.cx")) {
            val rawJson = app
                .get(
                    data, headers = mapOf(
                        "x-e-h" to "7Y2ozlO+QysR5w9Q6Tupmtvl9jJp7ThFH8SB+Lo7NvZjgjqRSqOgcT2v4ISM9sP10LmnlYI8WQ==.xrlyOBFS5BHjQ2Lk"
                    )
                )
                .body
                .string()
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

                Log.d("ACX", "Video URL: $url")
                loadExtractor(url = url, "$mainUrl/", subtitleCallback, callback)
            }
        }
        else {
            loadExtractor(iframeLink, "$mainUrl/", subtitleCallback, callback)
        }
        return true
    }
}