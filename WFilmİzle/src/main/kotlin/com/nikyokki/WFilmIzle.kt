

package com.nikyokki

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class WFilmIzle : MainAPI() {
    override var mainUrl = "https://www.wfilmizle.club/"
    override var name = "WFilmİzle"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/filmizle/aile-filmleri-izle-hd/"             to "Aile",
        "${mainUrl}/filmizle/aksiyon-filmleri-izle-hd/"          to "Aksiyon",
        "${mainUrl}/filmizle/animasyon-filmleri-izle/"           to "Animasyon",
        "${mainUrl}/filmizle/belgesel-filmleri-izle/"            to "Belgesel",
        "${mainUrl}/filmizle/bilim-kurgu-filmleri-izle/"         to "Bilim Kurgu",
        "${mainUrl}/filmizle/draam-filmleri-izle/"                to "Dram",
        "${mainUrl}/filmizle/fantaastik-filmler-izle/"            to "Fantastik",
        "${mainUrl}/filmizle/gerilimm-filmleri-izle/"            to "Gerilim",
        "${mainUrl}/filmizle/gizem-filmleri-izle/"               to "Gizem",
        "${mainUrl}/filmizle/komedi-filmleri-izle-hd/"           to "Komedi",
        "${mainUrl}/filmizle/korkuu-filmleri-izle/"              to "Korku",
        "${mainUrl}/filmizle/macera-filmleri-izle-hd/"           to "Macera",
        "${mainUrl}/filmizle/polisiye-filmleri-izle-hd/"         to "Polisiye",
        "${mainUrl}/filmizle/romantik-filmler-izle/"             to "Romantik",
        "${mainUrl}/filmizle/savas-filmmleri-izle/"              to "Savaş",
        "${mainUrl}/filmizle/sporr-filmleri-izle/"               to "Spor",
        "${mainUrl}/filmizle/succ-filmleri-izle/"                to "Suç",
        "${mainUrl}/filmizle/tarih-filmleri-izle-hd/"            to "Tarih",
        "${mainUrl}/filmizle/vahsi-bati-filmleri-izle/"          to "Vahşi Batı",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/${page}").document
        val home = document.select("div.movie-preview-content").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse {
        val title     = this.selectFirst("span.movie-title")?.text()?.replace(" izle","")?.trim() ?: ""
        Log.d("WFI", "Title: $title")
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: ""
        Log.d("WFI", "Href: $href")
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        Log.d("WFI", "Poster: $posterUrl")

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.movie-preview-content").mapNotNull { it.toMainPageResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse {
        Log.d("WFI", "LoadURL: $url")
        val document = app.get(url).document

        val orgTitle = document.selectFirst("div.title h1")?.text()?.replace(" izle","")?.trim() ?: ""
        Log.d("WFI", "OrgTitle: $orgTitle")
        val altTitle = document.selectFirst("div.diger_adi h2")?.text()?.trim() ?: ""
        Log.d("WFI", "altTitle: $altTitle")
        val title = if (altTitle.isNotEmpty()) "$orgTitle - $altTitle" else orgTitle
        Log.d("WFI", "title: $title")
        val poster = fixUrlNull(document.selectFirst("div.poster img")?.attr("src"))
        Log.d("WFI", "poster: $poster")
        val description = document.selectFirst("div.excerpt")?.text()?.trim()
        Log.d("WFI", "description: $description")
        val year =
            document.selectFirst("div.release a")?.text()?.trim()?.toIntOrNull()
        Log.d("WFI", "year: $year")
        val tags = document.select("div.categories a").map { it.text() }
        Log.d("WFI", "tags: $tags")
        val rating = document.select("div.imdb").last()?.text()?.replace("IMDb Puanı:","")?.split("/")
            ?.first()?.trim()?.toRatingInt()
        Log.d("WFI", "rating: " + document.selectFirst("div.imdb").toString())
        val actors = document.select("div.actor a").map { it.text() }
        Log.d("WFI", "actors: $actors")
        val trailer = document.selectFirst("div.container iframe")?.attr("src")
        Log.d("WFI", "trailer: $trailer")
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.rating = rating
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("WFI", "data » $data")
        val cookie = (System.currentTimeMillis() - 50000)  / 1000
        println("Timestamp (milisaniye): $cookie")
        Log.d("WFI", cookie.toString())
        val document = app.get(data, headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"),
            cookies = mapOf(
                "session_starttime" to cookie.toString()
            )).document
        val iframe   = fixUrlNull(document.select("div#vast iframe").last()?.attr("src") ?: "")
        Log.d("WFI", "iframe » $iframe")
        var hash = ""
        if (iframe?.contains("/player/") == true) {
             hash = iframe.substringAfter("data=") ?: ""
        } else {
             hash = iframe?.split("/")?.last() ?: ""
        }
        Log.d("WFI", "hash » $hash")
        if (iframe?.contains("hdplayersystem") == true) {
            val json = app.post("https://hdplayersystem.live/player/index.php?data=$hash&do=getVideo", headers =
            mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox",
                "Accept" to "application/json, text/javascript, */*; q=0.01", "X-Requested-With" to "XMLHttpRequest",
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8"),
                data = mapOf(
                    "hash" to hash.toString(),
                    "r" to "$mainUrl/"
                ),
            )
            Log.d("WFI", "JSONTEXT: ${json.document.body().text()}")
            val jsonData = ObjectMapper().readValue(json.document.body().text(), IframeResponse::class.java)
            // 'ck' alanını decode edelim
            val decodedCK = decodeUnicodeEscapeSequences(jsonData.ck)
            val updatedVideoData = jsonData.copy(ck = decodedCK)
            val master = updatedVideoData.videoSource ?: ""
            Log.d("WFI", "Master: $master")
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = master,
                    type = ExtractorLinkType.M3U8

                ) {
                    referer = mapOf("Referer" to mainUrl).toString() // "Referer" ayarı burada yapılabilir
                    quality = getQualityFromName(Qualities.Unknown.value.toString())
                    headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox",
                        "Accept" to "*/*", "X-Requested-With" to "XMLHttpRequest",
                        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8"
                    )
                }
            )
        }
        //loadExtractor(iframe.toS, "${mainUrl}/", subtitleCallback, callback)
        return true
    }
    private fun decodeUnicodeEscapeSequences(input: String): String {
        val pattern = Regex("\\\\x([0-9a-fA-F]{2})")
        return pattern.replace(input) { matchResult ->
            val hex = matchResult.groupValues[1]
            val charCode = hex.toInt(16)
            charCode.toChar().toString()
        }
    }
}
