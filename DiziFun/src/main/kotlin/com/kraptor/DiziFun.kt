// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.R.string.*
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URLDecoder

class DiziFun : MainAPI() {
    override var mainUrl = "https://dizifun4.com"
    override var name = "DiziFun"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/diziler" to "Diziler",
        "${mainUrl}/filmler" to "Filmler",
        "${mainUrl}/netflix" to "NetFlix Dizileri",
        "${mainUrl}/exxen" to "Exxen Dizileri",
        "${mainUrl}/disney" to "Disney+ Dizileri",
        "${mainUrl}/tabii-dizileri" to "Tabii Dizileri",
        "${mainUrl}/blutv" to "BluTV Dizileri",
        "${mainUrl}/todtv" to "TodTV Dizileri",
        "${mainUrl}/gain" to "Gain Dizileri",
        "${mainUrl}/hulu" to "Hulu Dizileri",
        "${mainUrl}/primevideo" to "PrimeVideo Dizileri",
        "${mainUrl}/hbomax" to "HboMax Dizileri",
        "${mainUrl}/paramount" to "Paramount+ Dizileri",
        "${mainUrl}/unutulmaz" to "Unutulmaz Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?p=${page}").document
        val home = document.select("div.uk-width-1-3").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h5.uk-panel-title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a.uk-position-cover")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.platformmobile img")?.attr("src"))

        // Burada tür kontrolü yapıyoruz
        val type = if (href.contains("/film/")) TvType.Movie else TvType.TvSeries

        return newTvSeriesSearchResponse(title, href, type) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama?query=${query}").document
        return document.select("div.uk-width-1-3").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h5")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a.uk-position-cover")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.uk-overlay img")?.attr("src"))

        // Tür kontrolü eklendi
        val type = if (href.contains("/film/")) TvType.Movie else TvType.TvSeries

        return newTvSeriesSearchResponse(title, href, type) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.text-bold")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("img.responsive-img")?.attr("src"))
        val description = document.selectFirst("p.text-muted")?.text()?.trim()
        val year = document.select("ul.subnav li")
            .firstOrNull { it.text().contains("Dizi Yılı") || it.text().contains("Film Yılı") }
            ?.ownText()
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
        val tags = document.select("div.series-info")
            .map { it.text() }
            .flatMap { text ->
                text.removePrefix("Türü:")
                    .split(",")
                    .map { it.trim() }
            }
        val actors = document.select("div.actor-card").map { card ->
            val name = card.selectFirst("span.actor-name")?.text()?.trim() ?: return@map null
            val image = fixUrlNull(card.selectFirst("img")?.attr("src"))
            val actor = Actor(name, image)
            ActorData(
                actor = actor,
            )
        }.filterNotNull()
        val trailer = Regex("""embed/([^?"]+)""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        val type = if (url.contains("/film/")) TvType.Movie else TvType.TvSeries

        val score = document.selectFirst("div.imdb-score")?.text()

        if (type == TvType.Movie) {
            val movieData = url
            return newMovieLoadResponse(title, url, type, movieData) {
                this.posterUrl = poster
                this.year = year
                this.tags = tags
                this.plot = description
                this.actors = actors
                this.score  = Score.from10(score)
                addTrailer(trailer)
            }
        } else {
            val episodes = document.select("div.season-detail").flatMap { seasonDiv ->
                val seasonId = seasonDiv.attr("id") // örnek: "season-1"
                val season = seasonId.removePrefix("season-").toIntOrNull() ?: 1
                seasonDiv.select("div.bolumtitle a").mapNotNull { aTag ->
                    val rawHref = aTag.attr("href")
                    val href = if (rawHref.startsWith("?")) "$url$rawHref"
                    else aTag.absUrl("href").ifBlank { fixUrl(rawHref) }

                    if (href.isBlank()) return@mapNotNull null
                    val episodeDiv = aTag.selectFirst("div.episode-button") ?: return@mapNotNull null
                    val name = episodeDiv.text().trim()
                    val episodeNumber = name.filter { it.isDigit() }.toIntOrNull() ?: 1
                    newEpisode(href) {
                        this.name = name
                        this.season = season
                        this.episode = episodeNumber
                    }
                }
            }

            return newTvSeriesLoadResponse(title, url, type, episodes) {
                this.posterUrl = poster
                this.year = year
                this.tags = tags
                this.plot = description
                this.actors = actors
                addTrailer(trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val allScripts = document.select("script").joinToString("\n") { it.html() }

        // Tek hex çözme fonksiyonu
        fun hexToString(hex: String): String {
            val result = StringBuilder()
            for (i in 0 until hex.length step 2) {
                val endIndex = minOf(i + 2, hex.length)
                result.append(hex.substring(i, endIndex).toInt(16).toChar())
            }
            return URLDecoder.decode(result.toString(), "UTF-8")
        }

        // Base URL'ler
        val videoBaseUrls = listOf(
            "https://d2.premiumvideo.click",
            "https://gangamstyle19999.davaydanalol.shop",
            "https://cdn99928.dizifunplay.click",
            "https://d1.premiumvideo.click",
            "https://argamdavay.click",
            "https://gangamstyle1.dailymonvideo.biz"
        )
        val subtitleBaseUrls = videoBaseUrls
        val refererUrl = "https://d1.premiumvideo.click/"
        val altReferer = "https://gujan.premiumvideo.click/"
        val movieReferer = "https://playhouse.premiumvideo.click/"

        // Pattern'ler
        val decodeCallPatterns = listOf(
            Regex("""decodeURIComponent\(\s*(hexToStringAlt|hexToString)\(\s*['\"]([0-9A-Fa-f]+)['\"]\s*\)\s*\)"""),
            Regex("""decodeURIComponent\(\s*(hexToStringLondon|hexToStringArmony|hexToStringVietnam)\(\s*['\"]([0-9A-Fa-f]+)['\"]\s*\)\s*\)"""),
            Regex("""(hexToString\w*)\(\s*['\"]([0-9A-Fa-f]+)['\"]\s*\)""")
        )

        val iframeUrlPatterns = listOf(
            Regex("""const\s+(londonIframeUrl|armonyIframeUrl|vietnamIframeUrl)\s*=\s*decodeURIComponent\(\s*\w+\(\s*['\"]([0-9A-Fa-f]+)['\"]\s*\)\s*\);""")
        )

        val m3u8Pattern = Regex("""file\s*:\s*['\"]([^'\"]+\.m3u8)['\"]""")
        val subtitlePattern = Regex("""file\s*:\s*['\"]([^'\"]+\.vtt)['\"]""")
        val htmlSourcePattern = Regex("""<source\s+src=['\"]([^'\"]+\.m3u8)['\"]""")
        val altSubtitlePattern = Regex("""subtitle\s*:\s*['\"]([^'\"]+\.vtt)['\"]""")
        val trackSubtitlePattern = Regex("""<track\s+[^>]*src=['\"]([^'\"]+\.vtt)['\"][^>]*>""")

        // Ortak subtitle işleme fonksiyonu
        fun processSubtitles(content: String) {
            val subtitleUrls = mutableSetOf<Pair<String, String>>()

            val subtitlePatterns = listOf(subtitlePattern, altSubtitlePattern, trackSubtitlePattern)

            subtitlePatterns.forEach { pattern ->
                pattern.findAll(content).forEach { match ->
                    val path = match.groups[1]?.value ?: return@forEach
                    val lang = when {
                        path.contains("eng") -> "English"
                        path.contains("tur") -> "Turkish"
                        else -> "Unknown"
                    }
                    val keywords = listOf("tur", "tr", "türkçe", "turkce")
                    val language = if (keywords.any { path.contains(it, ignoreCase = true) }) {
                        "Turkish"
                    } else {
                        lang
                    }
                    val fullUrl = if (path.startsWith("http")) path else {
                        if (path.startsWith("/")) subtitleBaseUrls.first() + path
                        else subtitleBaseUrls.first() + "/" + path
                    }
                    subtitleUrls.add(Pair(fullUrl, language))
                }
            }

            // Subtitle callback'lerini çağır
            subtitleUrls.forEach { (url, lang) ->
                try {
                    subtitleCallback.invoke(SubtitleFile(lang, url))
                } catch (e: Exception) {
                    Log.e("Dfun", "Subtitle error: ${e.message}")
                }
            }
        }

        // Global olarak işlenmiş URL'leri takip et
        val globalVisitedUrls = mutableSetOf<String>()

        // Ortak video işleme fonksiyonu
        suspend fun processVideoContent(content: String, sourceName: String, referer: String) {
            // M3U8 pattern kontrolü
            m3u8Pattern.find(content)?.groups?.get(1)?.value?.let { path ->
                videoBaseUrls.forEach { base ->
                    val fullUrl = if (path.startsWith("http")) path else "$base$path"

                    if (!globalVisitedUrls.add(fullUrl)) {
                        Log.d("Dfun", "Duplicate URL atlandı: $fullUrl")
                        return@forEach
                    }

                    try {
                        val response = app.get(fullUrl, referer = referer)
                        val body = response.body?.string()

                        if (body == null || "404" in body || "Not Found" in body) {
                            Log.d("Dfun", "Link geçersiz: $fullUrl")
                            return@forEach
                        }

                        callback.invoke(
                            newExtractorLink(
                                source = "DiziFun ($sourceName)",
                                name = name,
                                url = fullUrl
                            ) {
                                headers = mapOf("Referer" to referer)
                                quality = Qualities.Unknown.value
                            }
                        )
                        Log.d("Dfun", "Video link eklendi: $fullUrl")
                    } catch (e: Exception) {
                        Log.e("Dfun", "Video link error: ${e.message}")
                    }
                }
            }

            // HTML source pattern kontrolü
            htmlSourcePattern.find(content)?.groups?.get(1)?.value?.let { path ->
                val fullUrl = if (path.startsWith("http")) path else videoBaseUrls.first() + path

                if (globalVisitedUrls.add(fullUrl)) {
                    callback.invoke(
                        newExtractorLink(
                            source = "DiziFun ($sourceName)",
                            name = name,
                            url = fullUrl
                        ) {
                            headers = mapOf("Referer" to referer)
                            quality = Qualities.Unknown.value
                        }
                    )
                    Log.d("Dfun", "HTML Video link eklendi: $fullUrl")
                }
            }

            // Subtitle'ları işle
            processSubtitles(content)
        }

        // Film iframe URL'lerini işle
        iframeUrlPatterns.forEach { pattern ->
            pattern.findAll(allScripts).forEach { match ->
                val iframeType = match.groupValues[1]
                val hexValue = match.groupValues[2]
                val decodedUrl = hexToString(hexValue)

                val normalizedUrl = when {
                    decodedUrl.startsWith("//") -> "https:$decodedUrl"
                    decodedUrl.startsWith("/") -> videoBaseUrls.first() + decodedUrl
                    else -> decodedUrl
                }

                // Duplicate URL kontrolü
                if (!globalVisitedUrls.add(normalizedUrl)) {
                    Log.d("Dfun", "Duplicate iframe URL atlandı: $normalizedUrl")
                    return@forEach
                }

                Log.d("Dfun", "Film iframe işleniyor: $iframeType → $normalizedUrl")

                try {
                    val response = app.get(normalizedUrl, headers = mapOf("Referer" to movieReferer))
                    if (response.isSuccessful) {
                        processVideoContent(response.text, iframeType, movieReferer)
                    }
                } catch (e: Exception) {
                    Log.e("Dfun", "Film iframe hata: ${e.message}")
                }
            }
        }

        // Decode call pattern'lerini işle
        decodeCallPatterns.forEach { pattern ->
            pattern.findAll(allScripts).forEach { match ->
                val funcName = match.groupValues[1]
                val hexValue = match.groupValues[2]

                if (hexValue.isEmpty()) return@forEach

                val rawDecoded = hexToString(hexValue)
                val normalizedUrl = when {
                    rawDecoded.startsWith("//") -> "https:$rawDecoded"
                    rawDecoded.startsWith("/") -> videoBaseUrls.first() + rawDecoded
                    else -> rawDecoded
                }

                // Duplicate URL kontrolü
                if (!globalVisitedUrls.add(normalizedUrl)) {
                    Log.d("Dfun", "Duplicate decode URL atlandı: $normalizedUrl")
                    return@forEach
                }

                Log.d("Dfun", "$funcName işleniyor = $normalizedUrl")

                val referer = when (funcName) {
                    "hexToStringLondon", "hexToStringArmony", "hexToStringVietnam" -> movieReferer
                    "hexToStringAlt" -> altReferer
                    else -> refererUrl
                }

                try {
                    val response = app.get(normalizedUrl, headers = mapOf("Referer" to referer))
                    if (response.isSuccessful) {
                        processVideoContent(response.text, funcName, referer)
                    }
                } catch (e: Exception) {
                    Log.e("Dfun", "Hata: ${e.message}")
                }
            }
        }

        return true
    }
}