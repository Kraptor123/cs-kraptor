// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class SuperFilmGeldi : MainAPI() {
    override var mainUrl              = "https://www.superfilmgeldi4.art"
    override var name                 = "SuperFilmGeldi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/page/"                                        to "Son Eklenenler",
        "${mainUrl}/hdizle/category/2023-filmleri/"      to "2023 Filmleri",
        "${mainUrl}/hdizle/category/2022-filmleri/"      to "2022 Filmleri",
        "${mainUrl}/hdizle/category/2021-filmleri/"      to "2021 Filmleri",
        "${mainUrl}/hdizle/category/2020-filmleri/"      to "2020 Filmleri",
        "${mainUrl}/hdizle/category/aksiyon/"            to "Aksiyon",
        "${mainUrl}/hdizle/category/animasyon/"          to "Animasyon",
        "${mainUrl}/hdizle/category/belgesel/"           to "Belgesel",
        "${mainUrl}/hdizle/category/biyografi/"          to "Biyografi",
        "${mainUrl}/hdizle/category/bilim-kurgu/"        to "Bilim Kurgu",
        "${mainUrl}/hdizle/category/fantastik/"          to "Fantastik",
        "${mainUrl}/hdizle/category/dram/"               to "Dram",
        "${mainUrl}/hdizle/category/gerilim/"            to "Gerilim",
        "${mainUrl}/hdizle/category/gizem/"              to "Gizem",
        "${mainUrl}/hdizle/category/komedi-filmleri/"    to "Komedi Filmleri",
        "${mainUrl}/hdizle/category/karete-filmleri/"    to "Karete Filmleri",
        "${mainUrl}/hdizle/category/korku/"              to "Korku",
        "${mainUrl}/hdizle/category/muzik/"              to "Müzik",
        "${mainUrl}/hdizle/category/macera/"             to "Macera",
        "${mainUrl}/hdizle/category/romantik/"           to "Romantik",
        "${mainUrl}/hdizle/category/spor/"               to "Spor",
        "${mainUrl}/hdizle/category/savas/"              to "Savaş",
        "${mainUrl}/hdizle/category/suc/"                to "Suç",
        "${mainUrl}/hdizle/category/western/"            to "Western",
        "${mainUrl}/hdizle/category/2019-filmleri/"      to "2019 Filmleri",
        )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home     = document.select("div.movie-preview-content").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun removeUnnecessarySuffixes(title: String): String {
        val unnecessarySuffixes = listOf(
            " izle", 
            " full film", 
            " filmini full",
            " full türkçe",
            " alt yazılı", 
            " altyazılı", 
            " tr dublaj",
            " hd türkçe",
            " türkçe dublaj",
            " yeşilçam ",
            " erotik fil",
            " türkçe",
            " yerli",
        )

        var cleanedTitle = title.trim()

        for (suffix in unnecessarySuffixes) {
            val regex = Regex("${Regex.escape(suffix)}.*$", RegexOption.IGNORE_CASE)
            cleanedTitle = cleanedTitle.replace(regex, "").trim()
        }

        return cleanedTitle
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("span.movie-title a")?.text()?.substringBefore(" izle") ?: return null
        val hrefraw   = fixUrlNull(this.selectFirst("span.movie-title a")?.attr("href")) ?: return null
        val href      = if (hrefraw.contains("erotik")) {
            return null
            }else {
            hrefraw
        }
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(removeUnnecessarySuffixes(title), href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}?s=${query}").document

        return document.select("div.movie-preview-content").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div.title h1")?.text()?.trim()?.substringBefore(" izle") ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.poster img")?.attr("src"))
        val year            = document.selectFirst("div.release a")?.text()?.toIntOrNull()
        val description     = document.selectFirst("div.excerpt p")?.text()?.trim()
        val tags            = document.select("div.categories a").map { it.text() }
        val rating          = document.selectFirst("span.imdb-rating")?.text()?.trim()?.split(" ")?.first()
        val recommendations = document.select("div.film-content div.existing_item").mapNotNull { it.toSearchResult() }
        val actors          = document.select("div.actor a").map {
            Actor(it.text())
        }

        return newMovieLoadResponse(removeUnnecessarySuffixes(title), url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.year            = year
            this.plot            = description
            this.tags            = tags
            this.score = Score.from10(rating)
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("SFG", "data » $data")
        val document = app.get(data).document
        val iframe   = fixUrlNull(document.selectFirst("div#vast iframe")?.attr("src")) ?: return false
        Log.d("SFG", "iframe » $iframe")

        if (iframe.contains("mix") and iframe.contains("index.php?data=")) {
            val iSource  = app.get(iframe, referer="${mainUrl}/").text
            val mixPoint = Regex("""videoUrl":"(.*)","videoServer""").find(iSource)?.groupValues?.get(1)?.replace("\\", "") ?: return false

            var endPoint = "?s=0&d="

            if (iframe.contains("mixlion")) {
                endPoint = "?s=3&d="
            } else if (iframe.contains("mixeagle")) {
                endPoint = "?s=1&d="
            }

            val m3uLink = iframe.substringBefore("/player") + mixPoint + endPoint
            Log.d("SFG", "m3uLink » $m3uLink")

            callback.invoke(
                newExtractorLink(
                    source  = this.name,
                    name    = this.name,
                    url     = m3uLink,
                    type = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to iframe) // "Referer" ayarı burada yapılabilir
                    quality = getQualityFromName(Qualities.Unknown.value.toString())
                }
            )
        } else {
            loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }
}
