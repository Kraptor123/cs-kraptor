// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class DiziFon : MainAPI() {
    override var mainUrl = "https://www.dizifon.com"
    override var name = "DiziFon"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/diziler/anime/" to "Anime",
        "${mainUrl}/diziler/cin-dizileri/" to "Çin Dizileri",
        "${mainUrl}/diziler/hint-dizileri/" to "Hint Dizileri",
        "${mainUrl}/diziler/japon-dizileri/" to "Japon Dizileri",
        "${mainUrl}/diziler/kore-dizileri/" to "Kore Dizileri",
        "${mainUrl}/diziler/tayland-dizileri/" to "Tayland Dizileri",
        "${mainUrl}/diziler/aksiyon/" to "Aksiyon",
        "${mainUrl}/diziler/arkadaslik/" to "Arkadaşlık",
        "${mainUrl}/diziler/dogaustu/" to "Doğaüstü",
        "${mainUrl}/diziler/dram/" to "Dram",
        "${mainUrl}/diziler/fantastik/" to "Fantastik",
        "${mainUrl}/diziler/genclik/" to "Gençlik",
        "${mainUrl}/diziler/gerilim/" to "Gerilim",
        "${mainUrl}/diziler/gizem/" to "Gizem",
        "${mainUrl}/diziler/hukuk/" to "Hukuk",
        "${mainUrl}/diziler/is/" to "İş",
        "${mainUrl}/diziler/komedi/" to "Komedi",
        "${mainUrl}/diziler/korku/" to "Korku",
        "${mainUrl}/diziler/medikal/" to "Medikal",
        "${mainUrl}/diziler/muzik/" to "Müzik",
        "${mainUrl}/diziler/okul/" to "Okul",
        "${mainUrl}/diziler/romantik/" to "Romantik",
        "${mainUrl}/diziler/spor/" to "Spor",
        "${mainUrl}/diziler/suc/" to "Suç",
        "${mainUrl}/diziler/tarihi/" to "Tarihi",
        "${mainUrl}/diziler/yasam/" to "Yaşam"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page/").document
        val home = document.select("div.frag-k").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a.baslik span")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newTvSeriesSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.frag-k").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a.baslik span")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("div.ssag h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.afis img")?.attr("data-src"))
        val description = document.selectFirst("div.aciklama")?.text()?.trim()
        val tags = document.select("ul.detay li a").map { it.text() }
        val rating = document.selectFirst("i.fa.fa-imdb")?.text()?.trim()?.toRatingInt()
        val recommendations = document.select("div.sag-vliste li").mapNotNull { it.toRecommendationResult() }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }
        val year =
            document.selectFirst("ul.detay > li:nth-child(4)")?.text()?.substringAfterLast(" ")?.trim()?.toIntOrNull()
        val duration = document.selectFirst("ul.detay > li:nth-child(5)")?.text()
            ?.replace("Bölüm Süresi:", "")
            ?.replace("Dakika", "")
            ?.replace(" ", "")
            ?.split(" ")
            ?.first()
            ?.trim()
            ?.toIntOrNull()
        val bolumler = document.select("li.szn").mapNotNull { bolum ->
            val posterBol = bolum.selectFirst("a img")?.attr("data-src")
            val hrefBol = fixUrlNull(bolum.selectFirst("div.resim a")?.attr("href")) ?: return null
            val episodeName = bolum.selectFirst("div.baslik")?.text()
            val episodeNum = bolum.selectFirst("div.num")?.text()?.toInt()
            newEpisode(url = hrefBol, {
                this.name = episodeName
                this.posterUrl = posterBol
                this.episode = episodeNum
            })
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, bolumler) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.rating = rating
            this.recommendations = recommendations
            this.year = year
            this.duration = duration
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newTvSeriesSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name", "data = $data")
        val document1 = app.get(data).document

        // İlk iframe’i buradan alıyoruz
        val iframe1 = fixUrlNull(document1.select("iframe").attr("vdo-src"))
        Log.d("kraptor_$name", "iframe1 = $iframe1")

        // İkinci part linki boş değilse ikinci dokümanı al, yoksa boş string
        val ikinciPart = document1
            .selectFirst("li.parttab:nth-child(2) > a:nth-child(1)")
            ?.attr("href")
            .orEmpty()
        Log.d("kraptor_$name", "ikinciPart = $ikinciPart")

        // İkinci iframe’i ancak ikinciPart varsa al
        val iframe2 = if (ikinciPart.isNotEmpty()) {
            val document2 = app.get(ikinciPart).document
            val src = fixUrlNull(document2.select("iframe").attr("vdo-src"))
            Log.d("kraptor_$name", "iframe2 = $src")
            src
        } else {
            Log.d("kraptor_$name", "ikinciPart boş, iframe2 atlanıyor")
            null
        }

        // Null olmayan tüm iframe URL’lerini sırayla loadExtractor’a yolla
        listOfNotNull(iframe1, iframe2).forEach { url ->
            Log.d("kraptor_$name", "loading iframe: $url")
            loadExtractor(url, mainUrl, subtitleCallback, callback)
        }

        return true
    }
}