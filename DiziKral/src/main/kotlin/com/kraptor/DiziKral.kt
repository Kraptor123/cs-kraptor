// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.mapNotNull

class DiziKral : MainAPI() {
    override var mainUrl              = "https://dizikral.club"
    override var name                 = "DiziKral"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/diziler/page/sayfa"              to  "Tüm Diziler",
        "${mainUrl}/channel/gain"                                to  "Gain",
        "${mainUrl}/channel/amazon"                              to  "Amazon",
        "${mainUrl}/channel/disney-plus"                         to  "Disney+",
        "${mainUrl}/channel/exxen"                               to  "Exxen",
        "${mainUrl}/channel/blu-tv"                              to  "Blu TV",
        "${mainUrl}/diziler/page/sayfa?tur=aile"                 to  "Aile",
        "${mainUrl}/diziler/page/sayfa?tur=aksiyon-macera"       to  "Aksiyon & Macera",
        "${mainUrl}/diziler/page/sayfa?tur=animasyon"            to  "Animasyon",
        "${mainUrl}/diziler/page/sayfa?tur=belgesel"             to  "Belgesel",
        "${mainUrl}/diziler/page/sayfa?tur=bilim-kurgu-fantazi"  to  "Bilim Kurgu & Fantazi",
        "${mainUrl}/diziler/page/sayfa?tur=biyografi"            to  "Biyografi",
        "${mainUrl}/diziler/page/sayfa?tur=cocuklar"             to  "Çocuklar",
        "${mainUrl}/diziler/page/sayfa?tur=dram"                 to  "Dram",
        "${mainUrl}/diziler/page/sayfa?tur=gerceklik"            to  "Gerçeklik",
        "${mainUrl}/diziler/page/sayfa?tur=gizem"                to  "Gizem",
        "${mainUrl}/diziler/page/sayfa?tur=komedi"               to  "Komedi",
        "${mainUrl}/diziler/page/sayfa?tur=pembe-dizi"           to  "Pembe Dizi",
        "${mainUrl}/diziler/page/sayfa?tur=romantik"             to  "Romantik",
        "${mainUrl}/diziler/page/sayfa?tur=savas-politik"        to  "Savaş & Politik",
        "${mainUrl}/diziler/page/sayfa?tur=suc"                  to  "Suç",
        "${mainUrl}/diziler/page/sayfa?tur=talk"                 to  "Talk",
        "${mainUrl}/diziler/page/sayfa?tur=tarih"                to  "Tarih",
        "${mainUrl}/diziler/page/sayfa?tur=vahsi-bati"           to  "Vahşi Batı"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data.replace("sayfa","$page")}").document
        val home     = document.select("article ul li").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("span.title")?.text()?.trim()?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/diziler/?s=${query}").document

        return document.select("article ul li").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("span.title")?.text()?.trim()?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div.cover h5")?.text()?.trim() ?: return null
        val style           = document.selectFirst("div.cover")?.attr("style")?: return null
        val poster          = style.substringAfter("url(").substringBefore(")").trim()
        val description     = document.selectFirst("div.summary p")?.text()?.trim()
        val year            = document.selectFirst(".popup-summary > ul:nth-child(2) > li:nth-child(6) > div:nth-child(2) > a:nth-child(1)")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.value a").map { it.text() }
        val rating          = document.selectFirst(".popup-summary > ul:nth-child(2) > li:nth-child(1) > div:nth-child(2)")?.text()?.trim()
        val duration        = document.selectFirst(".popup-summary > ul:nth-child(2) > li:nth-child(7) > div:nth-child(2)")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }
        val episode         = document.select("div.episode-item").mapNotNull { bolum ->
            val href        = fixUrlNull(bolum.selectFirst("a")?.attr("href")) ?: return null
            val baslik      = bolum.selectFirst("div.name")?.text()?.trim() ?: return null
            val episodeText = bolum
                .selectFirst("div.episode")
                ?.text()
                ?.trim()
                ?: return null
            val afterSeason = episodeText.substringAfter("Sezon ").substringBefore(".Bölüm")
            val episodeNumber = afterSeason.toIntOrNull() ?: return null
            val seasonNumber = episodeText
                .substringBefore(".Sezon")
                .toIntOrNull()
                ?: return null
            val dateText = bolum
                .selectFirst("div.date")
                ?.text()
                ?.trim()
                ?: return null
            val formatter = SimpleDateFormat("d MMMM yyyy", Locale("tr"))
            val parsedDate: Date = formatter.parse(dateText) ?: return null
            val dateLong: Long = parsedDate.time
            val posterUrl   = fixUrlNull(bolum.selectFirst("img")?.attr("src"))
            newEpisode(href){
                    this.name = baslik
                    this.posterUrl = posterUrl
                    this.episode = episodeNumber
                    this.season = seasonNumber
                    this.date   = dateLong
                }
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episode) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score = Score.from10(rating)
            this.duration        = duration
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("Temel", "data » ${data}")
        val document = app.get(data).document.html()

        val iframeRegex = Regex("""iframe\s+src="([^"]*)"""", RegexOption.IGNORE_CASE)
        val match = iframeRegex.find(document)

// Grup 1’i al
        val iframe: String? = match?.groups?.get(1)?.value

        val url = fixUrlNull(iframe).toString()

        Log.d("dkral","iframe $iframe")


        loadExtractor(url, "https://dizikral.club/", subtitleCallback, callback)

        return true
    }
}