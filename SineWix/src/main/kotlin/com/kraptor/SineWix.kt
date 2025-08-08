// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import kotlin.collections.map
import kotlin.collections.mapNotNull

class SineWix : MainAPI() {
    override var mainUrl              = "https://ydfvfdizipanel.ru"
    override var name                 = "SineWix"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime, TvType.AsianDrama, TvType.Cartoon)
    //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,

    override val mainPage = mainPageOf(
        "${mainUrl}/public/api/media/seriesEpisodesAll/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"  to "Yeni Bölümler",
        "${mainUrl}/public/api/genres/latestmovies/all/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"              to "Son Filmler",
        "${mainUrl}/public/api/genres/latestseries/all/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"              to "Son Diziler",
        "${mainUrl}/public/api/genres/latestanimes/all/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"              to "Son Animeler",
        "${mainUrl}/public/api/genres/mediaLibrary/show/80/serie/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"    to "Suç Dizileri",
        "${mainUrl}/public/api/genres/mediaLibrary/show/80/movie/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"    to "Suç Filmleri",
        "${mainUrl}/public/api/genres/mediaLibrary/show/9648/serie/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"  to "Gizem Dizileri",
        "${mainUrl}/public/api/genres/mediaLibrary/show/9648/movie/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"  to "Gizem Filmleri",
        "${mainUrl}/public/api/genres/mediaLibrary/show/10769/serie/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA" to "Kore Diziler",
        "${mainUrl}/public/api/genres/mediaLibrary/show/16/movie/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"    to "Animasyonlar"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val responseString = app.get("${request.data}?page=$page", headers = sineHeaders).text

    return if (request.name == "Yeni Bölümler") {
        val mapper = jacksonObjectMapper().registerKotlinModule()
        val response: YeniBolumResponse = mapper.readValue(responseString)
        val items = response.data?.mapNotNull { it.toYeniBolumResult() } ?: emptyList()
        newHomePageResponse(request.name, items)
    } else {
        val mapper = jacksonObjectMapper().registerKotlinModule()
        val response: ResponseHash = mapper.readValue(responseString)
        val items = response.data?.mapNotNull { it.toMainPageResult() } ?: emptyList()
        newHomePageResponse(request.name, items)
    }
}

    fun YeniBolum.toYeniBolumResult(): SearchResponse? {
    val title = showName ?: return null
    val season = seasonNumber ?: return null
    val episode = episodeNumber ?: return null

    val displayTitle = "$title ${season}x${episode.toString().padStart(2, '0')}"
    val href = "$mainUrl/public/api/series/show/$id/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"

    return newTvSeriesSearchResponse(displayTitle, href, TvType.TvSeries) {
        this.posterUrl = posterPath
        this.score = Score.from10(voteAverage)
    }
}


    private fun Icerikler.toMainPageResult(): SearchResponse? {
        val title     = title ?: originalName ?: name ?: return null
        val type = type.toString()
        val tvTypes = if (type.contains("movie")) {
            TvType.Movie
        } else if (type.contains("serie")) {
            TvType.TvSeries
        } else if (type.contains("anime")) {
            TvType.Anime
        } else {
            TvType.Movie
        }
        val genre     = if (type.contains("movie")){
            "media"
        } else if (type.contains("serie")) {
            "series"
        } else {
            "animes"
        }

        val filmDizi = if (type.contains("movie")) {
            "detail"
        } else{
            "show"
        }

        val href      = "${mainUrl}/public/api/$genre/$filmDizi/$id/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"
        val posterUrl = posterPath
        val score     = voteAverage

        return when (tvTypes) {
            TvType.Movie -> newMovieSearchResponse(title, href, tvTypes) {
                this.posterUrl = posterUrl
                this.score = Score.from10(score)
            }
            TvType.TvSeries -> {
                newTvSeriesSearchResponse(title, href, tvTypes) {
                    this.posterUrl = posterUrl
                    this.score = Score.from10(score)
                }
            }
            else -> {
                newAnimeSearchResponse(title, href, tvTypes) {
                    this.posterUrl = posterUrl
                    this.score = Score.from10(score)
                }
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/public/api/search/${query}/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA", headers = sineHeaders).toString().toJson()

        val mapper = jacksonObjectMapper().registerKotlinModule()
        val response: ResponseHash = mapper.readValue<ResponseHash>(document)

        return response.searchResponse!!.mapNotNull { icerik ->
            icerik.toSearchResult() }
    }

    private fun Icerikler.toSearchResult(): SearchResponse? {
        val title     = title ?: originalName ?: name ?: return null
        val tvTypes = if (type!!.contains("movie")) {
            TvType.Movie
        } else if (type.contains("serie")) {
            TvType.TvSeries
        } else if (type.contains("anime")) {
            TvType.Anime
        } else {
            TvType.Movie
        }
        val genre  = if (type.contains("movie")){
            "media"
        } else if (type.contains("serie")) {
            "series"
        } else {
            "animes"
        }

        val filmDizi = if (type.contains("movie")) {
            "detail"
        } else{
            "show"
        }

        val href = "${mainUrl}/public/api/$genre/$filmDizi/$id/9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"
        val posterUrl = posterPath
        val score = voteAverage


        return when (tvTypes) {
            TvType.Movie -> newMovieSearchResponse(title, href, tvTypes) {
                this.posterUrl = posterUrl
                this.score = Score.from10(score)
            }
            TvType.TvSeries -> {
                newTvSeriesSearchResponse(title, href, tvTypes) {
                    this.posterUrl = posterUrl
                    this.score = Score.from10(score)
                }
            }
            else -> {
                newAnimeSearchResponse(title, href, tvTypes) {
                    this.posterUrl = posterUrl
                    this.score = Score.from10(score)
                }
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        Log.d("kraptor_$name","url = $url")
        val document = app.get(url, headers = sineHeaders).text
        val mapper = jacksonObjectMapper().registerKotlinModule()
        val response: Icerikler = mapper.readValue<Icerikler>(document)

        val tvTypes = if (url.contains("movie")) {
            TvType.Movie
        } else if (url.contains("serie")) {
            TvType.TvSeries
        } else if (url.contains("anime")) {
            TvType.Anime
        } else {
            TvType.Movie
        }

        val title = response.name ?: response.title ?: return null
        val poster = response.backdropPathTv ?: response.posterPath ?: response.backdropPath
        val description = response.overview
        val year = response.firstAirDate?.substringBefore("-")?.toIntOrNull()
        val tags = response.genreslist
        val rating = response.voteAverage
        val actors = response.casterslist!!.map { oyuncu ->
            Actor(
                name = oyuncu.name ?: oyuncu.originalName.orEmpty(),
                image = oyuncu.oyuncuPoster.orEmpty()
            )
        }
        val trailer = response.previewPath?.let { "https://www.youtube.com/embed/$it" }

        Log.d("kraptor_$name", "trailer = $trailer")

        val bolumler = response.seasons?.flatMap { season ->
            season.episodes?.mapNotNull { episode ->
                val videoLink = episode.videos?.firstOrNull()?.link
                if (videoLink != null) {
                newEpisode(
                    url = videoLink,
                 {
                    this.season = season.seasonNumber
                    this.episode = episode.episodeNumber
                    this.name = episode.name ?: "Bölüm ${episode.episodeNumber}"
                    this.posterUrl = episode.stillPathTv ?: episode.stillPath
                    this.description = episode.overview
                })
                } else null
            } ?: emptyList()
        } ?: emptyList()

        val firstVideoLink: String = response.videos
            ?.firstOrNull()
            ?.link
            .toString()


        return when (tvTypes) {
            TvType.Movie -> {
                newMovieLoadResponse(title, firstVideoLink, tvTypes, firstVideoLink) {
                    this.posterUrl = poster
                    this.plot = description
                    this.year = year
                    this.tags = tags
                    this.score = rating?.let { Score.from10(it) }
                    addActors(actors)
                    addTrailer(trailer)
                }
            }

            TvType.TvSeries -> {
                newTvSeriesLoadResponse(title, url, TvType.TvSeries, bolumler) {
                    this.posterUrl = poster
                    this.plot = description
                    this.year = year
                    this.tags = tags
                    this.score = rating?.let { Score.from10(it) }
                    addActors(actors)
                    addTrailer(trailer)
                }
            }

            else -> {
                newAnimeLoadResponse(title, url, TvType.Anime, true) {
                    this.posterUrl = poster
                    this.plot = description
                    this.year = year
                    this.tags = tags
                    this.episodes = mutableMapOf(DubStatus.Subbed to bolumler)
                    this.score = rating?.let { Score.from10(it) }
                    addActors(actors)
                    addTrailer(trailer)
                }
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        if (data.contains("mediafire")){
            loadExtractor(data, "${mainUrl}/", subtitleCallback, callback)
        } else {
            callback.invoke(
                newExtractorLink(
                source = "SineWix",
                name = "SineWix",
                url = data,
                type = ExtractorLinkType.VIDEO,
                {
                }
            ))
        }
        return true
    }
}

private val sineHeaders = mapOf(
    "hash256"     to "711bff4afeb47f07ab08a0b07e85d3835e739295e8a6361db77eebd93d96306b",
    "signature"   to "3082058830820370a00302010202145bbfbba9791db758ad12295636e094ab4b07dc24300d06092a864886f70d01010b05003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f69643020170d3231313231353232303433335a180f32303531313231353232303433335a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820222300d06092a864886f70d01010105000382020f003082020a0282020100a5106a24bb3f9c0aaf3a2b228f794b5eaf1757ba758b19736a39d1bdc73fc983a7237b8d5ca5156cfa999c1dab3418bbc2be0920e0ee001c8aa4812d1dae75d080f09e91e0abda83ff9a76e8384a4429f4849248069a59505b12ac2c14ba2e4d1a13afcdaf54e508697ff928a9f738e6f4a6fc27409c55329eb149b5ff89c5a2d7c06bf9e62086f955cad17d7be2623ee9d5ec56068eadc23cb0965a13ff97d49fe10ef41afc6eeca36b4ace9582097faff89f590bc831cdb3a69eec5d15b67c3f2cad49e37ed053733e3d2d400c47755b932bdbe15d749fd6ad1dce30ba5e66094dfb6ee6f64cafb807e11b19a990c5d078c6d6701cda0bdeb21e99404ff166074f4c89b04c418f4e7940db5c78647c475bcfb85d4c4e836ee7d7c1d53e9e736b5d96d4b4d8b98209064b729ac6a682d55a6a930e518d849898bb28329ca0aaa133b5e5270a9d5940cac6af4802a57fd971efda91abb602882dd6aa6ce2b236b57b52ee2481498f0cacbcc2c36c238bc84becad7eaaf1125b9a1ca9ded6c79f3f283a52050377809b2a9995d66e1636b0ed426fdd8685c47cb18e82077f4aefcc07887e1dc58b4d64be1632f0e7b4625da6f40c65a8512a6454a4b96963e7f876136e6c0069a519a79ad632078ed965aa12482458060c030ed50db706d854f88cb004630b49285d8af8b471ff8f6070687826412287b50049bcb7d1b6b62ef90203010001a310300e300c0603551d13040530030101ff300d06092a864886f70d01010b0500038202010051c0b7bd793181dc29ca777d3773f928a366c8469ecf2fa3cfb076e8831970d19bb2b96e44e8ccc647cf0696bb824ac61c23d958525d283cab26037b04d58aa79bf92192db843adf5c26a980f081d2f0e14f759fc5ff4c5bb3dce0860299bfe7b349a8155a2efaf731ba25ce796a80c1442c7bf80f8c1a7912ff0b6f6592264315337251a846460194fa594f81f38f9e5233a63201e931ad9cab5bf119f24025613f307194eaa6eb39a83f3c05a49ba34455b1aff7c6839bbb657d9392ffdf397432af6e56ba9534a8b07d7060fe09691c6cf07cb5324f67b3cc0871a8c621d81fe71d71085c55206a4f57e25f774fd4b979b299e8bb076b50fca42fa57da2d519fd35a4a7c0137babaed4345f8031b63b6a71f5e8268f709d658ccd7c2a58849379d25bfa598c3f4a2c3d9b7d89285fefeb7f0ec65137d38b08ce432a15688b624a179e6a4a505ebc3bcdfbc4d4330508ee2d8d0f016924dcec21a6838ef7d834c6f43bde4a5201ed0b3bb4e9bd377b470e36bcf5bc3d56169dbd8e39567aa7dce4d1a8a8a54a5e1aa6fb1a8aab0062669a966f96e15ccce6fe12ea5e6a8b8c8823bdc94988ca39759fd1cc8fd8ae5c3d74db50b174cf7d77655016c075c91d439ed01cc0a9f695c99fad3b5495fb6cb1e01a5fa020cc6022a85c07ec55f9eba89719f86e49d34ab5bd208c5f70cced2b7b7963c014f8404432979b506de29e",
    "User-Agent"  to "EasyPlex (Android 14; SM-A546B; Samsung Galaxy A54 5G; tr)",
    "Accept"      to "application/json"
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseHash(
    @JsonProperty("current_page")
    val currentPage: Int? = null,
    @JsonProperty("data")
    val data: List<Icerikler>? = null,
    @JsonProperty("search")
    val searchResponse: List<Icerikler>? = null,
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class YeniBolum(
    @JsonProperty("id") val id: Int?,
    @JsonProperty("name") val showName: String?,
    @JsonProperty("episode_name") val episodeName: String?,
    @JsonProperty("season_number") val seasonNumber: Int?,
    @JsonProperty("episode_number") val episodeNumber: Int?,
    @JsonProperty("poster_path") val posterPath: String?,
    @JsonProperty("vote_average") val voteAverage: Double?,
    @JsonProperty("type") val type: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class YeniBolumResponse(
    @JsonProperty("current_page") val currentPage: Int?,
    @JsonProperty("data") val data: List<YeniBolum>?
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class Icerikler(
    val id: Int? = null,
    val name: String? = null,
    @JsonProperty("original_name")
    val originalName: String? = null,
    val title: String? = null,
    @JsonProperty("poster_path")
    val posterPath: String? = null,
    @JsonProperty("backdrop_path")
    val backdropPath: String? = null,
    @JsonProperty("backdrop_path_tv")
    val backdropPathTv: String? = null,
    @JsonProperty("vote_average")
    val voteAverage: Double? = null,
    @JsonProperty("is_anime")
    val isAnime: Int? = null,
    val subtitle: String? = null,
    val genreslist: List<String>? = null,
    val genres: List<Genre>? = null,
    val type: String? = null,
    val overview: String? = null,
    @JsonProperty("release_date")
    val releaseDate: String? = null,
    @JsonProperty("first_air_date")
    val firstAirDate: String? = null,
    @JsonProperty("preview_path")
    val previewPath: String? = null,
    @JsonProperty("casterslist")
    val casterslist: List<Cast>? = null,
    val seasons: List<Season>? = null,
    val videos: List<Video>? = null
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Genre(
    val id: Int? = null,
    @JsonProperty("anime_id")
    val animeId: Int? = null,
    @JsonProperty("genre_id")
    val genreId: Int? = null,
    val name: String? = null
)

data class Cast(
    val id: Int? = null,
    val name: String? = null,
    @JsonProperty("original_name")
    val originalName: String? = null,
    val character: String? = null,
    @JsonProperty("profile_path")
    val oyuncuPoster: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Season(
    val id: Int? = null,
    @JsonProperty("tmdb_id")
    val tmdbId: Int? = null,
    @JsonProperty("serie_id")
    val serieId: Int? = null,
    @JsonProperty("season_number")
    val seasonNumber: Int? = null,
    val name: String? = null,
    val overview: String? = null,
    @JsonProperty("poster_path")
    val posterPath: String? = null,
    @JsonProperty("air_date")
    val airDate: String? = null,
    @JsonProperty("created_at")
    val createdAt: String? = null,
    @JsonProperty("updated_at")
    val updatedAt: String? = null,
    val episodes: List<Episode>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Episode(
    val id: Int? = null,
    @JsonProperty("tmdb_id")
    val tmdbId: Int? = null,
    @JsonProperty("season_id")
    val seasonId: Int? = null,
    @JsonProperty("episode_number")
    val episodeNumber: Int? = null,
    val name: String? = null,
    val overview: String? = null,
    @JsonProperty("still_path")
    val stillPath: String? = null,
    @JsonProperty("still_path_tv")
    val stillPathTv: String? = null,
    @JsonProperty("vote_average")
    val voteAverage: Double? = null,
    @JsonProperty("vote_count")
    val voteCount: Int? = null,
    val views: Int? = null,
    @JsonProperty("air_date")
    val airDate: String? = null,
    @JsonProperty("skiprecap_start_in")
    val skiprecapStartIn: Int? = null,
    val hasrecap: Int? = null,
    @JsonProperty("enable_stream")
    val enableStream: Int? = null,
    @JsonProperty("enable_media_download")
    val enableMediaDownload: Int? = null,
    @JsonProperty("enable_ads_unlock")
    val enableAdsUnlock: Int? = null,
    @JsonProperty("created_at")
    val createdAt: String? = null,
    @JsonProperty("updated_at")
    val updatedAt: String? = null,
    val videos: List<Video>? = null,
    val substitles: List<Subtitle>? = null,
    val downloads: List<Download>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Video(
    val id: Int? = null,
    @JsonProperty("episode_id")
    val episodeId: Int? = null,
    val server: String? = null,
    val header: String? = null,
    val useragent: String? = null,
    val link: String? = null,
    val lang: String? = null,
    @JsonProperty("video_name")
    val videoName: String? = null,
    val embed: Int? = null,
    val youtubelink: Int? = null,
    val hls: Int? = null,
    @JsonProperty("supported_hosts")
    val supportedHosts: Int? = null,
    val drm: Int? = null,
    val drmuuid: String? = null,
    val drmlicenceuri: String? = null,
    val status: Int? = null,
    @JsonProperty("created_at")
    val createdAt: String? = null,
    @JsonProperty("updated_at")
    val updatedAt: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Subtitle(
    val id: Int? = null,
    @JsonProperty("episode_id")
    val episodeId: Int? = null,
    val lang: String? = null,
    val file: String? = null,
    val label: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Download(
    val id: Int? = null,
    @JsonProperty("episode_id")
    val episodeId: Int? = null,
    val quality: String? = null,
    val link: String? = null,
    val size: String? = null
)