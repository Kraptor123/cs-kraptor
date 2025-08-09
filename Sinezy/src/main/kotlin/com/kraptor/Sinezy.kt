// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Sinezy : MainAPI() {
    override var mainUrl              = "https://sinezy.org"
    override var name                 = "Sinezy"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/"                    to  "Yeni Eklenenler",
        "${mainUrl}/izle/aksiyon-filmleri/"         to  "Aksiyon",
        "${mainUrl}/izle/animasyon-filmleri/"       to  "Animasyon",
        "${mainUrl}/izle/belgesel-izle/"            to  "Belgesel",
        "${mainUrl}/izle/bilim-kurgu-filmleri/"     to  "Bilim Kurgu",
        "${mainUrl}/izle/biyografi-filmleri/"       to  "Biyografi",
        "${mainUrl}/izle/dram-filmleri/"            to  "Dram",
        "${mainUrl}/izle/fantastik-filmler/"        to  "Fantastik",
//        "${mainUrl}/izle/gelecek-filmler/"          to  "Yakında",
        "${mainUrl}/izle/gerilim-filmleri/"         to  "Gerilim",
        "${mainUrl}/izle/gizem-filmleri/"           to  "Gizem",
        "${mainUrl}/izle/komedi-filmleri/"          to  "Komedi",
        "${mainUrl}/izle/korku-filmleri/"           to  "Korku",
        "${mainUrl}/izle/macera-filmleri/"          to  "Macera",
//        "${mainUrl}/izle/muzikal-izle/"             to  "Müzika",
        "${mainUrl}/izle/romantik-film/"            to  "Romantik",
        "${mainUrl}/izle/savas-filmleri/"           to  "Savaş",
//        "${mainUrl}/izle/spor-filmleri/"            to  "Spor",
        "${mainUrl}/izle/suc-filmleri/"             to  "Suç",
//        "${mainUrl}/izle/tarih-filmleri/"           to  "Tarih",
 //       "${mainUrl}/izle/turkce-altyazili-promo/"   to  "Altyazılı Pr",
 //       "${mainUrl}/izle/yabanci-dizi/"             to  "Yabancı Dizi",
        "${mainUrl}/izle/en-iyi-filmler/"           to  "En İyi Filmler",
        "${mainUrl}/izle/en-yeni-filmler/"          to  "Yeni Filmler",
        "${mainUrl}/izle/yerli-filmler/"            to  "Yerli Filmler",
 //       "${mainUrl}/izle/erotik-film-izle/"         to  "Erotik",
 //       "${mainUrl}/izle/yetiskin-film/"            to  "Yetişkin +18",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}page/$page/").document
        val home     = document.select("div.container div.content div.movie_box.move_k").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan      = this.selectFirst("span.coz")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/?s=${query}").document

        return document.select("div.movie_box.move_k").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan      = this.selectFirst("span.coz")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div.detail")?.attr("title") ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.move_k img")?.attr("data-src"))
        val description     = document.selectFirst("div.desc.yeniscroll p")?.text()?.trim()
        val year            = document.selectFirst("div.move_k span.year span")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.detail span a").map { it.text() }
        val rating          = document.selectFirst("span.info span.imdb")?.text()?.trim()
        val duration        = document.selectFirst("div.detail > span:nth-child(1) > span:nth-child(2) > p:nth-child(1)")
            ?.text()
            ?.replace(" Dakika","")
            ?.trim()?.toIntOrNull()
        val actors = document.select("span.oyn p")
            .flatMap { it.text().split(",") }
            .map { Actor(it.trim()) }
val trailer = document.selectFirst("meta[property=og:video]")?.attr("content")

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
        Log.d("kraptor_${this.name}", "data = ${data}")
        val document = app.get(data).text

        val regex = Regex(pattern = """ilkpartkod = '([^']*)';""", options = setOf(RegexOption.IGNORE_CASE))

        val findreg = regex.find(document)?.groupValues?.get(1).toString()

        val reqCoz  = base64Decode(findreg)

        val iframe  = reqCoz.substringAfter("src=").substringBefore(" ").replace("\"","")

        Log.d("kraptor_${this.name}", "iframe = ${iframe}")

         loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}
