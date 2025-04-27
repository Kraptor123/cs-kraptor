// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
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
    override var mainUrl = "https://dizifun2.com"
    override var name = "DiziFun"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override val mainPage = mainPageOf(
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
//        Log.d("Dfun", "home verisi = $home")

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
        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.text-bold")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("img.responsive-img")?.attr("src"))
        val description = document.selectFirst("p.text-muted")?.text()?.trim()
        val year = document.select("ul.subnav li")
            .firstOrNull { it.text().contains("Dizi Yılı") }
            ?.ownText() // sadece '2025' kısmını almak için
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
        val tags = document.select("div.series-info")
            .map { it.text() }
            .flatMap { text ->
                text.removePrefix("Türü:") // baştaki "Türü:" yazısını kaldır
                    .split(",")             // virgüle göre ayır
                    .map { it.trim() }       // her bir parçanın başındaki ve sonundaki boşlukları temizle
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
        Log.d("Dfun", "trailer = $trailer")
        val type = if (url.contains("/film/")) TvType.Movie else TvType.TvSeries
        return if (type == TvType.Movie) {
            newMovieLoadResponse(title, url, type, movies) {
                this.posterUrl = poster
                this.year = year
                this.tags = tags
                this.plot = description
                this.actors = actors
                addTrailer(trailer)
            }
        } else {
            newTvSeriesLoadResponse(title, url, type, episodes) {
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
        // Sayfayı al ve tüm <script> içeriğini birleştir
        val document = app.get(data).document
        val allScripts = document.select("script").joinToString("\n") { it.html() }

        // Hex çözme fonksiyonları
        fun hexToString(hex: String): String {
            val result = StringBuilder()
            for (i in 0 until hex.length step 2) {
                val endIndex = minOf(i + 2, hex.length)
                result.append(hex.substring(i, endIndex).toInt(16).toChar())
            }
            return URLDecoder.decode(result.toString(), "UTF-8")
        }

        fun hexToStringAlt(hex: String): String = hexToString(hex)

        // Decode çağrı kalıbı
        val decodeCallPattern = Regex(
            """decodeURIComponent\(\s*(hexToStringAlt|hexToString)\(\s*['\"]([0-9A-Fa-f]+)['\"]\s*\)\s*\)"""
        )

        // M3U8 ve altyazı pattern'leri (JS içi)
        val m3u8Pattern          = Regex("""file\s*:\s*['\"]([^'\"]+\.m3u8)['\"]""")
        val subtitlePattern      = Regex("""file\s*:\s*['\"]([^'\"]+\.vtt)['\"]""")
        // HTML <video> içi M3U8 kaynak pattern'i
        val htmlSourcePattern    = Regex("""<source\s+src=['\"]([^'\"]+\.m3u8)['\"]""")

        // Base URL'ler ve referer'lar
        val videoBaseUrls    = listOf(
            "https://ganadavay.click",
            "https://funnydavay.click",
            "https://donkeygrorup.click",
            "https://gujan.premiumvideo.click"
        )
        val subtitleBaseUrls = videoBaseUrls
        val refererUrl       = "https://d1.premiumvideo.click/"
        val altReferer       = "https://gujan.premiumvideo.click/"

        // Tüm decode çağrılarını işle
        decodeCallPattern.findAll(allScripts).forEach { match ->
            val funcName   = match.groupValues[1]
            val hexValue   = match.groupValues[2]
            val rawDecoded = if (funcName == "hexToStringAlt") hexToStringAlt(hexValue) else hexToString(hexValue)
            val partialUrl = rawDecoded

            // Normalize URL
            val normalizedUrl = when {
                partialUrl.startsWith("//") -> "https:$partialUrl"
                partialUrl.startsWith("/")  -> videoBaseUrls.first() + partialUrl
                else                           -> partialUrl
            }
            Log.d("Dfun", "$funcName → $normalizedUrl")

            // Alt iframe için özel işlemler (HTML <video> veya JS içi)
            if (funcName == "hexToStringAlt") {
                try {
                    val response = app.get(normalizedUrl, headers = mapOf("Referer" to altReferer))
                    if (!response.isSuccessful) return@forEach
                    val content = response.text

                    // Önce JS içinden m3u8Path ara
                    val jsPath = m3u8Pattern.find(content)?.groups?.get(1)?.value
                    if (jsPath != null) {
                        videoBaseUrls.forEach { base ->
                            val fullUrl = "$base$jsPath"
                            callback.invoke(
                                newExtractorLink(
                                    source = "DiziFun (Alt IFrame)",
                                    name   = name,
                                    url    = fullUrl
                                ) { headers = mapOf("Referer" to altReferer); quality = Qualities.Unknown.value }
                            )
                        }
                    } else {
                        // Eğer JS içi pattern yoksa, HTML <video> tag'i içerisinden al
                        htmlSourcePattern.find(content)?.groups?.get(1)?.value?.let { path ->
                            val finalUrl = if (path.startsWith("http")) path else videoBaseUrls.first() + path
                            callback.invoke(
                                newExtractorLink(
                                    source = "DiziFun (Alt IFrame)",
                                    name   = name,
                                    url    = finalUrl
                                ) { headers = mapOf("Referer" to altReferer); quality = Qualities.Unknown.value }
                            )
                        }
                    }

                    // Altyazılar
                    subtitlePattern.findAll(content)
                        .mapNotNull { it.groups[1]?.value }
                        .forEachIndexed { idx, path ->
                            val baseUrl = subtitleBaseUrls.getOrNull(idx) ?: subtitleBaseUrls.first()
                            subtitleCallback(
                                SubtitleFile(
                                    lang = when {
                                        path.contains("eng") -> "Ingilizce"
                                        path.contains("tur") -> "Turkce"
                                        else                  -> "Unknown"
                                    },
                                    url = "$baseUrl$path"
                                )
                            )
                        }
                } catch (e: Exception) {
                    Log.e("Dfun", "Alt iframe hata: ${e.message}")
                }
            } else {
                // Standart player hexToString çağrısı
                try {
                    val response = app.get(normalizedUrl, headers = mapOf("Referer" to refererUrl))
                    if (!response.isSuccessful) return@forEach
                    val content = response.text

                    // M3U8
                    m3u8Pattern.find(content)?.groups?.get(1)?.value?.let { path ->
                        videoBaseUrls.forEach { base ->
                            val fullUrl = "$base$path"
                            callback.invoke(
                                newExtractorLink(
                                    source = "DiziFun",
                                    name   = name,
                                    url    = fullUrl
                                ) { headers = mapOf("Referer" to refererUrl); quality = Qualities.Unknown.value }
                            )
                        }
                    }
                    // Altyazılar
                    subtitlePattern.findAll(content)
                        .mapNotNull { it.groups[1]?.value }
                        .forEachIndexed { idx, path ->
                            val baseUrl = subtitleBaseUrls.getOrNull(idx) ?: subtitleBaseUrls.first()
                            subtitleCallback(
                                SubtitleFile(
                                    lang = when {
                                        path.contains("eng") -> "Ingilizce"
                                        path.contains("tur") -> "Turkce"
                                        else                  -> "Unknown"
                                    },
                                    url = "$baseUrl$path"
                                )
                            )
                        }
                } catch (e: Exception) {
                    Log.e("Dfun", "Hata: ${e.message}")
                }
            }
        }

        return true
    }
}