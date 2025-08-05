// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Base64
import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class FullHDFilmizlesene : MainAPI() {
    override var mainUrl              = "https://www.fullhdfilmizlesene.so"
    override var name                 = "FullHDFilmizlesene"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/filmizle/aile-filmleri-hdf-izle"       to "Aile Filmleri",
        "${mainUrl}/filmizle/aksiyon-filmleri-hdf-izle"    to "Aksiyon Filmleri",
        "${mainUrl}/filmizle/animasyon-filmleri-fhd-izle"  to "Animasyon Filmleri",
        "${mainUrl}/filmizle/belgesel-filmleri-izle"       to "Belgeseller",
        "${mainUrl}/filmizle/bilim-kurgu-filmleri-izle-2"  to "Bilim Kurgu Filmleri",
        "${mainUrl}/filmizle/bluray-filmler-izle"          to "Blu Ray Filmler",
        "${mainUrl}/filmizle/cizgi-filmler-fhd-izle"       to "Çizgi Filmler",
        "${mainUrl}/filmizle/dram-filmleri-hd-izle"        to "Dram Filmleri",
        "${mainUrl}/filmizle/fantastik-filmler-hd-izle"    to "Fantastik Filmler",
        "${mainUrl}/filmizle/gerilim-filmleri-fhd-izle"    to "Gerilim Filmleri",
        "${mainUrl}/filmizle/gizem-filmleri-hd-izle"       to "Gizem Filmleri",
        "${mainUrl}/filmizle/hint-filmleri-fhd-izle"       to "Hint Filmleri",
        "${mainUrl}/filmizle/komedi-filmleri-fhd-izle"     to "Komedi Filmleri",
        "${mainUrl}/filmizle/korku-filmleri-izle-3"        to "Korku Filmleri",
        "${mainUrl}/filmizle/macera-filmleri-fhd-izle"     to "Macera Filmleri",
        "${mainUrl}/filmizle/muzikal-filmler-izle"         to "Müzikal Filmler",
        "${mainUrl}/filmizle/polisiye-filmleri-izle"       to "Polisiye Filmleri",
        "${mainUrl}/filmizle/psikolojik-filmler-izle"      to "Psikolojik Filmler",
        "${mainUrl}/filmizle/romantik-filmler-fhd-izle"    to "Romantik Filmler",
        "${mainUrl}/filmizle/savas-filmleri-fhd-izle"      to "Savaş Filmleri",
        "${mainUrl}/filmizle/suc-filmleri-izle"            to "Suç Filmleri",
        "${mainUrl}/filmizle/tarih-filmleri-fhd-izle"      to "Tarih Filmleri",
        "${mainUrl}/filmizle/western-filmler-hd-izle-3"    to "Western Filmler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page").document
        val home     = document.select("li.film").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("span.film-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val rating    = this.selectFirst("span.imdb")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(rating)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/${query}").document

        return document.select("li.film").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div[class=izle-titles]")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div img")?.attr("data-src"))
        val year            = document.selectFirst("div.dd a.category")?.text()?.split(" ")?.get(0)?.trim()?.toIntOrNull()
        val description     = document.selectFirst("div.ozet-ic > p")?.text()?.trim()
        val tags            = document.select("a[rel='category tag']").map { it.text() }
        val rating          = document.selectFirst("div.puanx-puan")?.text()?.split(" ")?.last()
        val duration        = document.selectFirst("span.sure")?.text()?.split(" ")?.get(0)?.trim()?.toIntOrNull()
        val trailer         = Regex("""embedUrl": "(.*)"""").find(document.html())?.groupValues?.get(1)
        val actors          = document.select("div.film-info ul li:nth-child(2) a > span").map {
            Actor(it.text())
        }


        val recommendations = document.selectXpath("//div[span[text()='Benzer Filmler']]/following-sibling::section/ul/li").mapNotNull {
            val recName      = it.selectFirst("span.film-title")?.text() ?: return@mapNotNull null
            val recHref      = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val recPosterUrl = fixUrlNull(it.selectFirst("img")?.attr("data-src"))
            newMovieSearchResponse(recName, recHref, TvType.Movie) {
                this.posterUrl = recPosterUrl
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.year            = year
            this.plot            = description
            this.tags            = tags
            this.score = Score.from10(rating)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun atob(s: String): String {
        return String(Base64.decode(s, Base64.DEFAULT))
    }

    private fun rtt(s: String): String {
        fun rot13Char(c: Char): Char {
            return when (c) {
                in 'a'..'z' -> ((c - 'a' + 13) % 26 + 'a'.code).toChar()
                in 'A'..'Z' -> ((c - 'A' + 13) % 26 + 'A'.code).toChar()
                else -> c
            }
        }

        return s.map { rot13Char(it) }.joinToString("")
    }

    private fun getVideoLinks(document: Document): List<Map<String, String>> {
        val scriptElement = document.select("script").firstOrNull { it.data().isNotEmpty() }
        val scriptContent = scriptElement?.data()?.trim() ?: return emptyList()

        val scxData         = Regex("scx = (.*?);").find(scriptContent)?.groupValues?.get(1) ?: return emptyList()
        val scxMap: SCXData = jacksonObjectMapper().readValue(scxData)
        val keys             = listOf("atom", "advid", "advidprox", "proton", "fast", "fastly", "tr", "en")

        val linkList = mutableListOf<Map<String, String>>()

        for (key in keys) {
            val t = when (key) {
                "atom"      -> scxMap.atom?.sx?.t
                "advid"     -> scxMap.advid?.sx?.t
                "advidprox" -> scxMap.advidprox?.sx?.t
                "proton"    -> scxMap.proton?.sx?.t
                "fast"      -> scxMap.fast?.sx?.t
                "fastly"    -> scxMap.fastly?.sx?.t
                "tr"        -> scxMap.tr?.sx?.t
                "en"        -> scxMap.en?.sx?.t
                else        -> null
            }

            when (t) {
                is List<*> -> {
                    val links = t.filterIsInstance<String>().map { link -> atob(rtt(link)) }
                    linkList.add(mapOf(key to links.joinToString(",")))
                }
                is Map<*, *> -> {
                    val links = t.mapValues { (_, value) ->
                        if (value is String) atob(rtt(value)) else ""
                    }
                    val safeLinks = links.mapKeys { (key, _) ->
                        key?.toString() ?: "Unknown"
                    }
                    linkList.add(safeLinks)
                }
            }
        }

        return linkList
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("FHD", "data » $data")
        val document    = app.get(data).document
        val videoLinks = getVideoLinks(document)
        Log.d("FHD", "videoLinks » $videoLinks")
        if (videoLinks.isEmpty()) return false


        for (videoMap in videoLinks) {
            for ((key, value) in videoMap) {
                val videoUrl = fixUrlNull(value) ?: continue
                if (videoUrl.contains("turbo.imgz.me")) {
                    loadExtractor("${key}||${videoUrl}", "${mainUrl}/", subtitleCallback, callback)
                } else {
                    loadExtractor(videoUrl, "${mainUrl}/", subtitleCallback, callback)
                }
            }
        }

        return true
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SCXData(
        @JsonProperty("atom")      val atom: AtomData?      = null,
        @JsonProperty("advid")     val advid: AtomData?     = null,
        @JsonProperty("advidprox") val advidprox: AtomData? = null,
        @JsonProperty("proton")    val proton: AtomData?    = null,
        @JsonProperty("fast")      val fast: AtomData?      = null,
        @JsonProperty("fastly")    val fastly: AtomData?    = null,
        @JsonProperty("tr")        val tr: AtomData?        = null,
        @JsonProperty("en")        val en: AtomData?        = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AtomData(
        @JsonProperty("sx") var sx: SXData
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SXData(
        @JsonProperty("t") var t: Any
    )
}
