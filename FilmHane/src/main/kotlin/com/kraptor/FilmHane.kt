// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import org.jsoup.Jsoup

class FilmHane : MainAPI() {
    override var mainUrl              = "https://filmhane.net"
    override var name                 = "FilmHane"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = true
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/tum-diziler" to "Tüm Diziler",
        "${mainUrl}/filmler"              to  "Tüm Filmler",
        "${mainUrl}/film/tur/aile"        to  "Aile",
        "${mainUrl}/film/tur/aksiyon"     to  "Aksiyon",
        "${mainUrl}/film/tur/aksiyon"     to  "Aksiyon filmleri",
        "${mainUrl}/film/tur/animasyon"   to  "Animasyon",
        "${mainUrl}/film/tur/belgesel"    to  "Belgesel",
        "${mainUrl}/film/tur/bilim-kurgu" to  "Bilim-Kurgu",
        "${mainUrl}/film/tur/dram"        to  "Dram",
        "${mainUrl}/film/tur/fantastik"   to  "Fantastik",
        "${mainUrl}/film/tur/gerilim"     to  "Gerilim",
        "${mainUrl}/film/tur/gizem"       to  "Gizem",
        "${mainUrl}/film/tur/komedi"      to  "Komedi",
        "${mainUrl}/film/tur/korku"       to  "Korku",
        "${mainUrl}/film/tur/macera"      to  "Macera",
        "${mainUrl}/film/tur/muzik"       to  "Müzik",
        "${mainUrl}/film/tur/romantik"    to  "Romantik",
        "${mainUrl}/film/tur/savas"       to  "Savaş",
        "${mainUrl}/film/tur/suc"         to  "Suç",
        "${mainUrl}/film/tur/tarih"       to  "Tarih",
        "${mainUrl}/film/tur/tv-film"     to  "TV film"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = when (request.name) {
            "Tüm Diziler" -> {
                app.get(request.data).document
            }
            "Tüm Filmler" -> {
                if (page == 1) {
                    app.get("${request.data}/sayfa").document
                } else {
                    app.get("${request.data}/sayfa/$page").document
                }
            }
            else -> {
                app.get("${request.data}/$page").document
            }
        }

        val home = buildList {
            addAll(document.select("div.poster-long").mapNotNull { it.toMainPageResult() }) // Filmler
            addAll(document.select("div.poster-md.relative").mapNotNull { it.toMainPageResult() }) // Diziler
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text() ?: this.selectFirst("h3")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.post(
            "$mainUrl/search",
            data = mapOf("query" to query),
            referer = mainUrl,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
                "X-Requested-With" to "XMLHttpRequest",
                "Origin" to mainUrl,
                "Referer" to mainUrl
            )
        )

        val jsonString = document.body.string()
        val themeHtml = JSONObject(jsonString).getString("theme")
        val doc = Jsoup.parse(themeHtml)
        val items = doc.select("div.leftbar-search-result-box-content ul.flex li")

        return items.mapNotNull { li ->
            li.selectFirst("div.result-series")?.let { seriesDiv ->
                val title = seriesDiv.selectFirst("span.block")?.text()?.trim() ?: return@mapNotNull null
                val href = seriesDiv.parent()?.attr("href")?.let(::fixUrlNull) ?: return@mapNotNull null
                val posterUrl = seriesDiv.selectFirst("img")?.attr("data-src")?.let(::fixUrlNull)
                return@mapNotNull newTvSeriesSearchResponse(title, fixUrl(href), TvType.TvSeries) {
                    this.posterUrl = posterUrl
                }
            }
            li.selectFirst("div.result-movies")?.let { movieDiv ->
                val title = movieDiv.selectFirst("div.result-movies-text a")?.text()?.trim() ?: return@mapNotNull null
                val href = movieDiv.selectFirst("div.result-movies-text a")?.attr("href")?.let(::fixUrlNull) ?: return@mapNotNull null
                val posterUrl = movieDiv.selectFirst("div.result-movies-image img")?.attr("data-src")?.let(::fixUrlNull)
                return@mapNotNull newMovieSearchResponse(title, fixUrl(href), TvType.Movie) {
                    this.posterUrl = posterUrl
                }
            }
            null
        }
    }



    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.series-profile-image img")?.attr("src"))
        val description     = document.selectFirst("div.series-profile-summary.article p")?.text()?.trim()
        val year            = document.selectFirst("li.sm\\:w-1\\/5:nth-child(5) > p:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.series-profile-type.tv-show-profile-type a").map { it.text() }
        val rating          = document.selectFirst("span.color-imdb")?.text()?.trim()?.toRatingInt()
        val duration        = document.selectFirst("li.sm\\:w-1\\/5:nth-child(2) > p:nth-child(2)")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val actors = document
            .select("li.w-auto.sm\\:w-full.flex-shrink-0")
            .map { element ->
                // Her li içinde img öğesini seçiyoruz
                val posterUrl = element
                    .selectFirst("img")
                    ?.attr("data-src")
                    ?.let { fixUrlNull(it).toString() }
                Actor(element.text(), posterUrl)
            }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }
        val episodeElements = document.select("li.flex.items-center.justify-between a.block.truncate")
        val episodeList = episodeElements.mapNotNull { episodeElement ->
            val epHref = fixUrlNull(episodeElement.attr("href")) ?: return@mapNotNull null
            val titleText = episodeElement.attr("a.block.truncate").trim()
            val match =  Regex("""^(\d+)\. Bölüm\s*-\s*(.*)$""").find(titleText)

            val epNumber = match?.groups?.get(1)?.value?.toInt()
            val epTitle = match?.groups?.get(2)?.value?.trim()

            newEpisode(epHref) {
                this.name = epTitle
                this.episode = epNumber
            }

        }

        return when {
            url.contains("/film/") -> {
                newMovieLoadResponse(title, url, TvType.Movie, url) {
                    this.posterUrl = poster
                    this.plot = description
                    this.year = year
                    this.tags = tags
                    this.rating = rating
                    this.duration = duration
                    addActors(actors)
                    addTrailer(trailer)
                }
            }

            url.contains("/dizi/") -> {
                newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeList) {
                    this.posterUrl = poster
                    this.plot = description
                    this.year = year
                    this.tags = tags
                    this.rating = rating
                    this.duration = duration
                    addActors(actors)
                    addTrailer(trailer)
                }
            }
            else -> null
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("fhane", "data » ${data}")
        val document = app.get(data).document

        val selectLi = document.selectFirst("div.series-watch-player.relative.flex")

        val iframeIlk = fixUrlNull(selectLi?.selectFirst("iframe")?.attr("src")).toString()

//        Log.d("fhane", "iframeilk = ${iframeIlk}")

        val gercekIframe = app.get(iframeIlk).document

        val videoReg = Regex("file:\"[^\"]*\"")
        val qualityRegex = Regex("(1080p|720p|480p|360p)", RegexOption.IGNORE_CASE)

        val qualities = qualityRegex.find(gercekIframe.html())
        val qualitiesval = qualities?.value

        val match = videoReg.find(gercekIframe.html())
        val videoLink = match?.value?.removePrefix("file:\"")?.removeSuffix("\"").toString()

//        Log.d("fhane", "videolinkim = ${videoLink}")

        val subReg = Regex("\"subtitle\":\"[^\"]*\"")
        val subtitleMatch = subReg.find(gercekIframe.html())

        subtitleMatch?.let {
            val rawValue = it.value.removePrefix("\"subtitle\":\"").removeSuffix("\"")
            val fixedValue = fixUrlNull(rawValue)

            if (!fixedValue.isNullOrEmpty()) {
                val subtitleUrls = fixedValue.split(",")

                for (part in subtitleUrls) {
                    var subtitleUrl = part.trim()
                    if (subtitleUrl.contains("[") && subtitleUrl.contains("]")) {
                        subtitleUrl = subtitleUrl.substring(subtitleUrl.indexOf("]") + 1).trim()
                    }

                    // URL'nin içeriğine göre dili belirle (sadece "_eng." veya "_tur." kontrol et)
                    val language = if (subtitleUrl.contains("_eng.", ignoreCase = true)) {
                        "İngilizce"
                    } else if (subtitleUrl.contains("_tur.", ignoreCase = true)) {
                        "Türkçe"
                    } else {
                        "Bilinmiyor"
                    }

                    if (subtitleUrl.isNotEmpty()) {
                        subtitleCallback.invoke(
                            SubtitleFile(
                                lang = language,
                                url = subtitleUrl
                            )
                        )
                    }
                }
            }
        }


        callback.invoke(
            newExtractorLink(
                source = "FilmHane $qualitiesval",
                name = "filmHane $qualitiesval",
                url = videoLink,
                type = ExtractorLinkType.M3U8,
                {
                    referer = iframeIlk
                    quality = when (qualitiesval) {
                        "1080p" -> Qualities.P1080.value
                        "720p" -> Qualities.P720.value
                        "480p" -> Qualities.P480.value
                        "360p" -> Qualities.P360.value
                        else -> Qualities.Unknown.value
                    }
                    headers = mapOf("User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                }
            )
        )
        return true
    }
}