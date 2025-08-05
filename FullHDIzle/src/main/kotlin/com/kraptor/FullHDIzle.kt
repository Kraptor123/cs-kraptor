// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.AppContextUtils.html

class FullHDIzle : MainAPI() {
    override var mainUrl              = "https://fullhdizle.one"
    override var name                 = "FullHDIzle"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/"                         to "Yeni Filmler",
        "${mainUrl}/kategori/aile-filmi-izle/"            to "Aile",
        "${mainUrl}/kategori/aksiyon-filmi-izle-hd/"      to "Aksiyon",
        "${mainUrl}/kategori/animasyon-filmi-izle/"       to "Animasyon",
        "${mainUrl}/kategori/belgesel-izle/"              to "Belgesel",
        "${mainUrl}/kategori/bilim-kurgu/"                to "Bilim Kurgu",
        "${mainUrl}/kategori/biyografi-filmleri/"         to "Biyografi",
        "${mainUrl}/kategori/dram-filmi-izle/"            to "Dram",
        "${mainUrl}/kategori/fantastik/"                  to "Fantastik",
        "${mainUrl}/kategori/gelecek-filmler/"            to "Gelecek",
        "${mainUrl}/kategori/genel/"                      to "Genel",
        "${mainUrl}/kategori/gerilim-filmleri-izle/"      to "Gerilim",
        "${mainUrl}/kategori/gizem-filmleri/"             to "Gizem",
        "${mainUrl}/kategori/hint-filmleri/"              to "Hint",
        "${mainUrl}/kategori/komedi-filmleri-izle/"       to "Komedi",
        "${mainUrl}/kategori/korku-filmi-izle/"           to "Korku",
        "${mainUrl}/kategori/macera-filmleri/"            to "Macera",
        "${mainUrl}/kategori/muzikal-filmler/"            to "Müzikal",
        "${mainUrl}/kategori/netflix-izle/"               to "Netflix",
        "${mainUrl}/kategori/romantik-filmler/"           to "Romantik",
        "${mainUrl}/kategori/savas-filmleri-izle/"        to "Savaş",
        "${mainUrl}/kategori/spor-filmleri/"              to "Spor",
        "${mainUrl}/kategori/suc-filmleri-izle/"          to "Suç",
        "${mainUrl}/kategori/tarih-film-izle-full/"       to "Tarih",
        "${mainUrl}/kategori/en-iyi-filmler-izle/"        to "Tavsiye",
        "${mainUrl}/kategori/turk-filmleri/"              to "Türk",
        "${mainUrl}/kategori/western-filmler/"            to "Western",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}page/$page/").document
        val home     = document.select("div.movie_box").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title").toString()
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan      = this.selectFirst("span.imdb")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/?s=${query}").document

        return document.select("div.movie_box").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title").toString()
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan      = this.selectFirst("span.imdb")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h2.orj_title")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.image img")?.attr("data-src"))
        val description     = document.selectFirst("div.desc")?.text()?.substringAfter("bizimle yaşayın.")?.trim()
        val year            = document.selectFirst("div.detail > div:nth-child(3) > ol:nth-child(2) > li:nth-child(4) > span:nth-child(2) > a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.detail > div:nth-child(3) > ul:nth-child(1) > li:nth-child(3) a").map { it.text() }
        val rating          = document.selectFirst("a.imdb")?.text()?.trim()
        val duration        = document.selectFirst("div.detail > div:nth-child(3) > ol:nth-child(2) > li:nth-child(3) > span:nth-child(2)")
            ?.text()
            ?.replace(" Dk.","")
            ?.split(" ")?.first()?.trim()?.toIntOrNull()
        val actors          = document.select("sc span").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score = Score.from10(rating)
            this.duration        = duration
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
//        Log.d("kraptor_$name", "data » ${data}")
        val document = app.get(data).text

        val regex = Regex(pattern = "ilkpartkod = '([^']*)';", options = setOf(RegexOption.IGNORE_CASE))

        val iframeb64 = regex.find(document)?.groupValues[1].toString()

        val iframe = base64Decode(iframeb64).substringAfter("src=\"").substringBefore("\"")

        val iframelink = fixUrlNull(iframe).toString()

        Log.d("kraptor_$name", "iframe » ${iframelink}")


         loadExtractor(iframelink, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}