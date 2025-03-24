// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.fasterxml.jackson.annotation.JsonProperty

class SinemaCX : MainAPI() {
    override var mainUrl              = "https://www.sinemax.cc"
    override var name                 = "SinemaCX"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    // ! CloudFlare bypass
    override var sequentialMainPage = true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay       = 250L // ? 0.25 saniye
    override var sequentialMainPageScrollDelay = 250L // ? 0.25 saniye

    override val mainPage = mainPageOf(
        "${mainUrl}/izle/aile-filmleri/page/"			 to		"Aile Filmleri",
        "${mainUrl}/izle/aksiyon-filmleri/page/"		 to		"Aksiyon Filmleri",
        "${mainUrl}/izle/animasyon-filmleri/page/"		 to		"Animasyon Filmleri",
        "${mainUrl}/izle/belgesel/page/"				 to		"Belgesel Filmleri",
        "${mainUrl}/izle/bilim-kurgu-filmleri/page/"	 to		"Bilim Kurgu Filmler",
        "${mainUrl}/izle/biyografi/page/"				 to		"Biyografi Filmleri",
        "${mainUrl}/izle/fantastik-filmler/page/"		 to		"Fantastik Filmler",
        "${mainUrl}/izle/gizem-filmleri/page/"			 to		"Gizem Filmleri",
        "${mainUrl}/izle/komedi-filmleri/page/"			 to		"Komedi Filmleri",
        "${mainUrl}/izle/korku-filmleri/page/"			 to		"Korku Filmleri",
        "${mainUrl}/izle/macera-filmleri/page/"			 to		"Macera Filmleri",
        "${mainUrl}/izle/romantik-filmler/page/"		 to		"Romantik Filmler",
        "${mainUrl}/izle/erotik-filmler/page/"			 to		"Erotik Film izle",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home     = document.select("div.icerik div.frag-k").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.yanac span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.yanac a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a.resim img")?.attr("src")) ?: fixUrlNull(this.selectFirst("a.resim img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.icerik div.frag-k").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title       = document.selectFirst("div.f-bilgi h1")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(document.selectFirst("link[rel='image_src']")?.attr("href"))
        val year        = document.selectFirst("div.f-bilgi ul.detay a[href*='yapim']")?.text()?.toIntOrNull()
        val description = document.selectFirst("div.f-bilgi div.ackl")?.text()?.trim()
        val tags        = document.select("div.f-bilgi div.tur a").map { it.text() }
        val rating      = document.selectFirst("b#puandegistir")?.text()?.trim()?.toRatingInt()
        val duration    = Regex("""Süre: </span>(\d+) Dakika</li>""").find(document.html())?.groupValues?.get(1)?.toIntOrNull()
        val actors      = document.select("li.oync li.oyuncu-k").map {
            Actor(it.selectFirst("span.isim")!!.text(), it.selectFirst("img")!!.attr("data-src"))
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.year      = year
            this.plot      = description
            this.tags      = tags
            this.rating    = rating
            this.duration  = duration
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("SCX", "data » $data")
        val document = app.get(data).document
        val iframe   = fixUrlNull(document.selectFirst("iframe")?.attr("data-vsrc"))?.substringBefore("?img=") ?: return false
        Log.d("SCX", "iframe » $iframe")

        val iframeSource         = app.get(iframe, referer="${mainUrl}/").text
        val subtitleSectionRegex = Regex("""playerjsSubtitle\s*=\s*"(.+?)"""")
        val subtitleSectionMatch = subtitleSectionRegex.find(iframeSource)
        if (subtitleSectionMatch != null) {
            val subtitleSection = subtitleSectionMatch.groupValues[1]
            val subtitleRegex   = Regex("""\[(.*?)](https?://[^\s",]+)""")
            val subtitleMatches = subtitleRegex.findAll(subtitleSection)

            for (subtitleMatch in subtitleMatches) {
                val subtitleGroups   = subtitleMatch.groupValues
                val subtitleLanguage = subtitleGroups[1]
                val subtitleUrl      = subtitleGroups[2]

                subtitleCallback.invoke(
                    SubtitleFile(
                        lang = subtitleLanguage,
                        url  = fixUrl(subtitleUrl)
                    )
                )

            }
        }

        if (iframe.contains("panel.sinema.cx")){
            val vidUrl = app.post(
                "https://panel.sinema.cx/player/index.php?data=" + iframe.split("/").last() + "&do=getVideo",
                headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
                referer = "${mainUrl}/"
            ).parsedSafe<Panel>()?.securedLink ?: return false

            callback.invoke(
                ExtractorLink(
                    source  = this.name,
                    name    = this.name,
                    url     = vidUrl,
                    referer = iframe,
                    quality = Qualities.Unknown.value,
                    isM3u8  = true
                )
            )
        } else {
            loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }

    data class Panel(
        @JsonProperty("hls")         val hls: Boolean?        = null,
        @JsonProperty("securedLink") val securedLink: String? = null
    )
}