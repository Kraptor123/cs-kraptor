// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class CizgiMax : MainAPI() {
    override var mainUrl              = "https://cizgimax.online"
    override var name                 = "CizgiMax"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = true
    override val supportedTypes       = setOf(TvType.Cartoon)

    override val mainPage = mainPageOf(
        "?orderby=date&order=DESC"                                   to "Son Eklenenler",
        "?s_type&tur[0]=aile&orderby=date&order=DESC"                to "Aile",
        "?s_type&tur[0]=aksiyon-macera&orderby=date&order=DESC"      to "Aksyion",
        "?s_type&tur[0]=animasyon&orderby=date&order=DESC"           to "Animasyon",
        "?s_type&tur[0]=bilim-kurgu-fantazi&orderby=date&order=DESC" to "Bilim Kurgu",
        "?s_type&tur[0]=cocuklar&orderby=date&order=DESC"            to "Çocuklar",
        "?s_type&tur[0]=komedi&orderby=date&order=DESC"              to "Komedi",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${mainUrl}/diziler/page/${page}${request.data}").document
        val home     = document.select("ul.filter-results li").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("h2.truncate")?.text()?.trim() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.poster-subject a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.poster-media img")?.attr("data-src"))

        return newTvSeriesSearchResponse(title, href, TvType.Cartoon) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("${mainUrl}/ajaxservice/index.php?qr=${query}").parsedSafe<SearchResult>()?.data?.result ?: return listOf()

        return response.mapNotNull { result ->
            if (result.sName.contains(".Bölüm") || result.sName.contains(".Sezon") || result.sName.contains("-Sezon") || result.sName.contains("-izle")) {
                return@mapNotNull null
            }

            newTvSeriesSearchResponse(
                result.sName,
                fixUrl(result.sLink),
                TvType.Cartoon
            ) {
                this.posterUrl = fixUrlNull(result.sImage)
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title       = document.selectFirst("h1.page-title")?.text() ?: return null
        val poster      = fixUrlNull(document.selectFirst("img.series-profile-thumb")?.attr("src")) ?: return null
        val description = document.selectFirst("p#tv-series-desc")?.text()?.trim()
        val tags        = document.select("div.genre-item a").mapNotNull { it.text().trim() }
        val rating      = document.selectFirst("div.color-imdb")?.text()?.trim()?.toRatingInt()


        val episodes = document.select("div.asisotope div.ajax_post").mapNotNull {
            val epName     = it.selectFirst("span.episode-names")?.text()?.trim() ?: return@mapNotNull null
            val epHref     = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val epEpisode  = Regex("""(\d+)\.Bölüm""").find(epName)?.groupValues?.get(1)?.toIntOrNull()
            val seasonName = it.selectFirst("span.season-name")?.text()?.trim() ?: ""
            val epSeason   = Regex("""(\d+)\.Sezon""").find(seasonName)?.groupValues?.get(1)?.toIntOrNull() ?: 1

            newEpisode(epHref) {
                this.name = epName
                this.season = epSeason
                this.episode = epEpisode
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.Cartoon, episodes) {
            this.posterUrl = poster
            this.plot      = description
            this.tags      = tags
            this.rating    = rating
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("CZGM", "data » $data")
        val document = app.get(data).document

        document.select("ul.linkler li").forEach {
            val iframe = fixUrlNull(it.selectFirst("a")?.attr("data-frame")) ?: return@forEach
            Log.d("CZGM", "iframe » $iframe")

            loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }
}
