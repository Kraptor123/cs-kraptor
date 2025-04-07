// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*

class SineWix : MainAPI() {
    override var mainUrl              = "https://ythls.kekikakademi.org"
    override var name                 = "SineWix"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)

    override val mainPage = mainPageOf(
        "${mainUrl}/sinewix/movies"        to "Filmler",
        "${mainUrl}/sinewix/series"        to "Diziler",
        "${mainUrl}/sinewix/animes"        to "Animeler",
        "${mainUrl}/sinewix/movies/10751"  to "Aile",
        "${mainUrl}/sinewix/movies/28"     to "Aksiyon",
        "${mainUrl}/sinewix/movies/16"     to "Animasyon",
        "${mainUrl}/sinewix/movies/99"     to "Belgesel",
        "${mainUrl}/sinewix/movies/10765"  to "Bilim Kurgu & Fantazi",
        "${mainUrl}/sinewix/movies/878"    to "Bilim-Kurgu",
        "${mainUrl}/sinewix/movies/18"     to "Dram",
        "${mainUrl}/sinewix/movies/14"     to "Fantastik",
        "${mainUrl}/sinewix/movies/53"     to "Gerilim",
        "${mainUrl}/sinewix/movies/9648"   to "Gizem",
        "${mainUrl}/sinewix/movies/35"     to "Komedi",
        "${mainUrl}/sinewix/movies/27"     to "Korku",
        "${mainUrl}/sinewix/movies/12"     to "Macera",
        "${mainUrl}/sinewix/movies/10402"  to "Müzik",
        "${mainUrl}/sinewix/movies/10749"  to "Romantik",
        "${mainUrl}/sinewix/movies/10752"  to "Savaş",
        "${mainUrl}/sinewix/movies/80"     to "Suç",
        "${mainUrl}/sinewix/movies/10770"  to "TV film",
        "${mainUrl}/sinewix/movies/36"     to "Tarih",
    )

    private val twitter = "https://twitter.com/"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url  = "${request.data}/${page}"
        val home = when {
            request.data.contains("/movies") -> {
                app.get(url).parsedSafe<GenresMovie>()?.data?.map { item ->
                    newMovieSearchResponse(item.title, "?type=${item.type}&id=${item.id}", TvType.Movie) { this.posterUrl = item.posterPath }
                } ?: listOf()
            }
            request.data.contains("/series") -> {
                app.get(url).parsedSafe<GenresSerie>()?.data?.map { item ->
                    newTvSeriesSearchResponse(item.name, "?type=${item.type}&id=${item.id}", TvType.TvSeries) { this.posterUrl = item.posterPath }
                } ?: listOf()
            }
            request.data.contains("/animes") -> {
                app.get(url).parsedSafe<GenresSerie>()?.data?.map { item ->
                    newAnimeSearchResponse(item.name, "?type=${item.type}&id=${item.id}", TvType.Anime) { this.posterUrl = item.posterPath }
                } ?: listOf()
            }
            else -> listOf()
        }

        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val request = app.get("${mainUrl}/sinewix/search/${query}")
        val reqData = request.parsedSafe<Search>()?.search

        return reqData?.mapNotNull { item ->
            when (item.type) {
                "movie" -> newMovieSearchResponse(item.name,    "?type=${item.type}&id=${item.id}", TvType.Movie)    { this.posterUrl = item.posterPath }
                "serie" -> newTvSeriesSearchResponse(item.name, "?type=${item.type}&id=${item.id}", TvType.TvSeries) { this.posterUrl = item.posterPath }
                "anime" -> newAnimeSearchResponse(item.name,    "?type=${item.type}&id=${item.id}", TvType.Anime)    { this.posterUrl = item.posterPath }
                else -> null
            }
        } ?: mutableListOf()
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val itemType = url.substringAfter("?type=").substringBefore("&id=")
        val itemId   = url.substringAfter("&id=")

        if (itemType == "movie") {
            val request = app.get("${mainUrl}/sinewix/movie/${itemId}")
            val media   = request.parsedSafe<MovieDetail>() ?: return null

            val orgTitle        = media.title
            val altTitle        = media.originalName ?: ""
            val title           = if (altTitle.isNotEmpty() && orgTitle != altTitle) "$orgTitle - $altTitle" else orgTitle

            val poster          = fixUrlNull(media.posterPath)
            val description     = media.overview
            val year            = media.releaseDate.split("-").first().toIntOrNull()
            val tags            = media.genres?.map { it.name }
            val rating          = "${media.voteAverage}".toRatingInt()
            val recommendations = media.relateds?.map { newMovieSearchResponse(it.title, "?type=${it.type}&id=${it.id}", TvType.Movie) { this.posterUrl = it.posterPath } }
            val actors          = media.casterslist?.map { Actor(it.name, it.profilePath) }

            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl       = poster
                this.plot            = description
                this.year            = year
                this.tags            = tags
                this.rating          = rating
                this.recommendations = recommendations
                addActors(actors)
            }
        } else {
            val request = app.get("${mainUrl}/sinewix/${itemType}/${itemId}")
            val media   = request.parsedSafe<SerieDetail>() ?: return null

            val orgTitle        = media.name
            val altTitle        = media.originalName ?: ""
            val title           = if (altTitle.isNotEmpty() && orgTitle != altTitle) "$orgTitle - $altTitle" else orgTitle

            val poster          = fixUrlNull(media.posterPath)
            val description     = media.overview
            val year            = media.firstAirDate.split("-").first().toIntOrNull()
            val tags            = media.genres?.map { it.name }
            val rating          = "${media.voteAverage}".toRatingInt()
            val recommendations = media.relateds?.map { newMovieSearchResponse(it.name, "?type=${it.type}&id=${it.id}", TvType.Movie) { this.posterUrl = it.posterPath } }
            val actors          = media.casterslist?.map { Actor(it.name, it.profilePath) }

            val episodeList     = mutableListOf<Episode>()

            media.seasons.forEach { season ->
                season.episodes.forEach { episode ->
                    episodeList.add(newEpisode(url + "&source=" + episode.videos.firstOrNull()?.link) {
                        this.name = episode.name
                        this.season = season.seasonNumber
                        this.episode = episode.episodeNumber
                        this.description = episode.overview
                        this.posterUrl = episode.stillPath
                    })
                }
            }

            return newTvSeriesLoadResponse(title, url, if (itemType == "serie") TvType.TvSeries else TvType.Anime, episodeList) {
                this.posterUrl       = poster
                this.plot            = description
                this.year            = year
                this.tags            = tags
                this.rating          = rating
                this.recommendations = recommendations
                addActors(actors)
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        if (!data.contains("&source=")) {
            val itemId  = data.substringAfter("&id=").substringBefore("&source=")
            val request = app.get("${mainUrl}/sinewix/movie/${itemId}")
            val media   = request.parsedSafe<MovieDetail>() ?: return false

            media.videos.forEach { video ->
                Log.d("SNWX", "video » $video")

                if (video.link.contains("mediafire.com")) {
                    loadExtractor(video.link, twitter, subtitleCallback, callback)
                } else {
                    callback.invoke(
                        newExtractorLink(
                            source  = this.name,
                            name    = this.name,
                            url     = video.link,
                            type    = INFER_TYPE
                        ) {
                            headers = mapOf("Referer" to twitter) // "Referer" ayarı burada yapılabilir
                            quality = getQualityFromName(Qualities.Unknown.value.toString())
                        }
                    )
                }

            }
        } else {

            if (data.substringAfter("&source=").contains("mediafire.com")) {
                loadExtractor(data.substringAfter("&source="), twitter, subtitleCallback, callback)
            } else {
                callback.invoke(
                    newExtractorLink(
                        source  = this.name,
                        name    = this.name,
                        url     = data.substringAfter("&source="),
                        type    = INFER_TYPE
                    ) {
                        headers = mapOf("Referer" to twitter) // "Referer" ayarı burada yapılabilir
                        quality = getQualityFromName(Qualities.Unknown.value.toString())
                    }
                )
            }

        }

        return true
    }
}