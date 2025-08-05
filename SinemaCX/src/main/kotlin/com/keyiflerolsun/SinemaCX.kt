// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class SinemaCX : MainAPI() {
    override var mainUrl = "https://www.sinema.dev"
    override var name = "SinemaCX"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie)

    // ! CloudFlare bypass
    override var sequentialMainPage =
        true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay = 250L // ? 0.25 saniye
    override var sequentialMainPageScrollDelay = 250L // ? 0.25 saniye

    override val mainPage = mainPageOf(
        "${mainUrl}/izle/aile-filmleri/page/" to "Aile Filmleri",
        "${mainUrl}/izle/aksiyon-filmleri/page/" to "Aksiyon Filmleri",
        "${mainUrl}/izle/animasyon-filmleri/page/" to "Animasyon Filmleri",
        "${mainUrl}/izle/belgesel/page/" to "Belgesel Filmleri",
        "${mainUrl}/izle/bilim-kurgu-filmleri/page/" to "Bilim Kurgu Filmler",
        "${mainUrl}/izle/biyografi/page/" to "Biyografi Filmleri",
        "${mainUrl}/izle/fantastik-filmler/page/" to "Fantastik Filmler",
        "${mainUrl}/izle/gizem-filmleri/page/" to "Gizem Filmleri",
        "${mainUrl}/izle/komedi-filmleri/page/" to "Komedi Filmleri",
        "${mainUrl}/izle/korku-filmleri/page/" to "Korku Filmleri",
        "${mainUrl}/izle/macera-filmleri/page/" to "Macera Filmleri",
        "${mainUrl}/izle/romantik-filmler/page/" to "Romantik Filmler",
        "${mainUrl}/izle/erotik-filmler/page/" to "Erotik Film izle",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home = document.select("div.icerik div.frag-k").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.yanac span")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("div.yanac a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a.resim img")?.attr("src")) ?: fixUrlNull(
            this.selectFirst("a.resim img")?.attr("data-src")
        )

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.icerik div.frag-k").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("div.f-bilgi h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("link[rel='image_src']")?.attr("href"))
        val year = document.selectFirst("div.f-bilgi ul.detay a[href*='yapim']")?.text()?.toIntOrNull()
        val description = document.selectFirst("div.f-bilgi div.ackl")?.text()?.trim()
        val tags = document.select("div.f-bilgi div.tur a").map { it.text() }
        val rating = document.selectFirst("b#puandegistir")?.text()?.trim()
        val duration =
            Regex("""Süre: </span>(\d+) Dakika</li>""").find(document.html())?.groupValues?.get(1)?.toIntOrNull()
        val actors = document.select("li.oync li.oyuncu-k").map {
            Actor(it.selectFirst("span.isim")!!.text(), it.selectFirst("img")!!.attr("data-src"))
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.score = Score.from10(rating)
            this.duration = duration
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val iframeUrl = document.selectFirst("iframe")?.attr("data-vsrc") ?: return false
        val hash = iframeUrl.substringAfterLast("/")
        val iframeDocument = app.get(iframeUrl, referer = data).document
        val subtitleSectionRegex = Regex("""playerjsSubtitle\s*=\s*"(.+?)"""")
        val subtitleSectionMatch = subtitleSectionRegex.find(iframeDocument.html())
        if (subtitleSectionMatch != null) {
            val subtitleSection = subtitleSectionMatch.groupValues[1]
            val subtitleRegex = Regex("""\[(.*?)](https?://[^\s",]+)""")
            val subtitleMatches = subtitleRegex.findAll(subtitleSection)

            for (subtitleMatch in subtitleMatches) {
                val subtitleGroups = subtitleMatch.groupValues
                val subtitleLanguage = subtitleGroups[1]
                val subtitleUrl = subtitleGroups[2]

                subtitleCallback.invoke(
                    SubtitleFile(
                        lang = subtitleLanguage,
                        url = fixUrl(subtitleUrl)
                    )
                )
            }
        }

        val apiResponse = app.post(
            "https://player.filmizle.in/player/index.php?data=$hash&do=getVideo",
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Origin" to mainUrl,
                "Referer" to data
            ),
            data = mapOf(
                "hash" to hash,
                "r" to data,
                "d" to "www.sinemax.cc",
            )
        ).parsedSafe<VideoResponse>() ?: return false

        val videoUrl = apiResponse.securedLink ?: apiResponse.videoSource ?: return false

        callback.invoke(
            ExtractorLink(
                source = name,
                name = name,
                url = videoUrl,
                referer = iframeUrl,
                quality = Qualities.Unknown.value,
                type = ExtractorLinkType.M3U8,
                headers = mapOf(
                    "Cookie" to "ck=${apiResponse.ck?.replace("\\x", "")?.hexToString()}",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                )
            )
        )

        return true
    }

    private fun String.hexToString() = chunked(2).joinToString("") { it.toInt(16).toChar().toString() }

    private data class VideoResponse(
        @JsonProperty("securedLink") val securedLink: String?,
        @JsonProperty("videoSource") val videoSource: String?,
        @JsonProperty("ck") val ck: String?
    )
}