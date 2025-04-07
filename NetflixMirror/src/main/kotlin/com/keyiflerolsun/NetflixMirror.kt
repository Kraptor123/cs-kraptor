// ! https://github.com/SaurabhKaperwan/CSX/blob/master/NetflixMirrorProvider/src/main/kotlin/com/horis/cloudstreamplugins/NetflixMirrorProvider.kt

package com.keyiflerolsun

import android.util.Log
import com.keyiflerolsun.entities.EpisodesData
import com.keyiflerolsun.entities.PlayList
import com.keyiflerolsun.entities.PostData
import com.keyiflerolsun.entities.SearchData
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.httpsify
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.nodes.Element

class NetflixMirror : MainAPI() {
    override var mainUrl              = "https://iosmirror.cc"
    override var name                 = "NetflixMirror"
    override val hasMainPage          = true
    override var lang                 = "hi"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries)

    private var cookieValue = ""
    private val headers      = mapOf("X-Requested-With" to "XMLHttpRequest")

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        cookieValue = cookieValue.ifEmpty { bypassVerification(mainUrl) }
        val cookies  = mapOf(
            "t_hash_t" to cookieValue,
            "ott"      to "nf",
            "hd"       to "on"
        )

        val allItems = listOf("movies", "series").flatMap { endpoint ->
            app.get(
                "${mainUrl}/${endpoint}",
                cookies = cookies
            ).document.select(".tray-container, #top10").map {
                it.toHomePageList()
            }
        }

        return newHomePageResponse(allItems, false)
    }

    private fun Element.toHomePageList(): HomePageList {
        val name  = select("h2, span").text()
        if (name.contains("Tamil") || name.contains("Hindi") || name.contains("Indian")) {
             return HomePageList("", emptyList())
        }

        val items = select("article, .top10-post").mapNotNull {
            it.toSearchResult()
        }

        return HomePageList(name, items)
    }

    private fun Element.toSearchResult(): SearchResponse {
        val id        = selectFirst("a")?.attr("data-post") ?: attr("data-post")
        val posterUrl = fixUrlNull(selectFirst(".card-img-container img, .top10-img img")?.attr("data-src"))

        return newMovieSearchResponse("", Id(id).toJson()) {
            this.posterUrl = posterUrl
            posterHeaders  = mapOf("Referer" to "${mainUrl}/")
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        cookieValue = cookieValue.ifEmpty { bypassVerification(mainUrl) }
        val cookies  = mapOf(
            "t_hash_t" to cookieValue,
            "hd"       to "on"
        )

        val url  = "${mainUrl}/search.php?s=${query}&t=${APIHolder.unixTime}"
        val data = app.get(url, referer = "${mainUrl}/", cookies = cookies).parsed<SearchData>()

        return data.searchResult.map {
            newMovieSearchResponse(it.t, Id(it.id).toJson()) {
                posterUrl     = "https://img.nfmirrorcdn.top/poster/v/${it.id}.jpg"
                posterHeaders = mapOf("Referer" to "${mainUrl}/")
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse {
        cookieValue = cookieValue.ifEmpty { bypassVerification(mainUrl) }
        val id       = parseJson<Id>(url).id
        val cookies  = mapOf(
            "t_hash_t" to cookieValue,
            "hd"       to "on"
        )

        val data = app.get(
            "${mainUrl}/post.php?id=${id}&t=${APIHolder.unixTime}",
            headers,
            referer = "${mainUrl}/",
            cookies = cookies
        ).parsed<PostData>()

        val episodes = arrayListOf<Episode>()

        val title    = data.title
        val castList = data.cast?.split(",")?.map { it.trim() } ?: emptyList()
        val cast     = castList.map {ActorData(Actor(it))}
        val genre    = listOf(data.ua.toString()) + (data.genre?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList())
        val rating   = data.match?.replace("IMDb ", "")?.toRatingInt()
        val runTime  = convertRuntimeToMinutes(data.runtime.toString())

        if (data.episodes.first() == null) {
            episodes.add(newEpisode(LoadData(title, id)) {name = data.title})
        } else {
            data.episodes.filterNotNull().mapTo(episodes) {
                newEpisode(LoadData(title, it.id)) {
                    this.name      = it.t
                    this.episode   = it.ep.replace("E", "").toIntOrNull()
                    this.season    = it.s.replace("S", "").toIntOrNull()
                    this.posterUrl = "https://img.nfmirrorcdn.top/epimg/150/${it.id}.jpg"
                    // this.runTime   = it.time.replace("m", "").toIntOrNull()
                }
            }

            if (data.nextPageShow == 1) {
                episodes.addAll(getEpisodes(title, url, data.nextPageSeason!!, 2))
            }

            data.season?.dropLast(1)?.amap {
                episodes.addAll(getEpisodes(title, url, it.id, 1))
            }
        }

        val type = if (data.episodes.first() == null) TvType.Movie else TvType.TvSeries

        return newTvSeriesLoadResponse(title, url, type, episodes) {
            posterUrl            = "https://img.nfmirrorcdn.top/poster/v/${id}.jpg"
            backgroundPosterUrl  = "https://img.nfmirrorcdn.top/poster/h/${id}.jpg"
            posterHeaders        = mapOf("Referer" to "${mainUrl}/")
            plot                 = data.desc
            year                 = data.year.toIntOrNull()
            tags                 = genre
            actors               = cast
            this.rating          = rating
            this.duration        = runTime
            this.recommendations = data.suggest?.map {
                newMovieSearchResponse("", Id(it.id).toJson()) {
                    posterUrl     = "https://img.nfmirrorcdn.top/poster/v/${it.id}.jpg"
                    posterHeaders = mapOf("Referer" to "${mainUrl}/")
                }
            }
        }
    }

    private suspend fun getEpisodes(title: String, eid: String, sid: String, page: Int): List<Episode> {
        val episodes = arrayListOf<Episode>()
        val cookies  = mapOf(
            "t_hash_t" to cookieValue,
            "hd"       to "on"
        )
        var pg = page

        while (true) {
            val data = app.get(
                "${mainUrl}/episodes.php?s=${sid}&series=${eid}&t=${APIHolder.unixTime}&page=${pg}",
                headers,
                referer = "${mainUrl}/",
                cookies = cookies
            ).parsed<EpisodesData>()
            data.episodes?.mapTo(episodes) {
                newEpisode(LoadData(title, it.id)) {
                    name           = it.t
                    episode        = it.ep.replace("E", "").toIntOrNull()
                    season         = it.s.replace("S", "").toIntOrNull()
                    this.posterUrl = "https://img.nfmirrorcdn.top/epimg/150/${it.id}.jpg"
                    // this.runTime   = it.time.replace("m", "").toIntOrNull()
                }
            }
            if (data.nextPageShow == 0) break

            pg++
        }

        return episodes
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("NTFX", "data » $data")
        val (title, id) = parseJson<LoadData>(data)
        val cookies     = mapOf(
            "t_hash_t" to cookieValue,
            "hd"       to "on"
        )

        val playlist = app.get(
            "${mainUrl}/playlist.php?id=${id}&t=${title}&tm=${APIHolder.unixTime}",
            headers,
            referer = "${mainUrl}/",
            cookies = cookies
        ).parsed<PlayList>()

        playlist.forEach { item ->
            item.sources.forEach {
                callback.invoke(
                    ExtractorLink(
                        name,
                        it.label,
                        fixUrl(it.file),
                        "${mainUrl}/",
                        getQualityFromName(it.file.substringAfter("q=", "")),
                        true
                    )
                )
            }

            item.tracks?.filter { it.kind == "captions" }?.map { track ->
                subtitleCallback.invoke(
                    SubtitleFile(
                        track.label.toString(),
                        httpsify(track.file.toString())
                    )
                )
            }
        }

        return true
    }

    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request()
                if (request.url.toString().contains(".m3u8")) {
                    val newRequest = request.newBuilder().header("Cookie", "hd=on").build()

                    return chain.proceed(newRequest)
                }

                return chain.proceed(request)
            }
        }
    }

    data class Id(val id: String)

    data class LoadData(val title: String, val id: String)
}