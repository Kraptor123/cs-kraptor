// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class UgurFilm : MainAPI() {
    override var mainUrl              = "https://ugurfilm10.com"
    override var name                 = "UgurFilm"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/turkce-altyazili-filmler/page/" to "Türkçe Altyazılı Filmler",
        "${mainUrl}/yerli-filmler/page/"            to "Yerli Filmler",
        "${mainUrl}/en-cok-izlenen-filmler/page/"   to "En Çok İzlenen Filmler",
        "${mainUrl}/category/kisa-film/page/"       to "Kısa Film",
        "${mainUrl}/category/aksiyon/page/"         to "Aksiyon",
        "${mainUrl}/category/bilim-kurgu/page/"     to "Bilim Kurgu",
        "${mainUrl}/category/belgesel/page/"        to "Belgesel",
        "${mainUrl}/category/komedi/page/"          to "Komedi",
        "${mainUrl}/category/kara-film/page/"       to "Kara Film",
        "${mainUrl}/category/erotik/page/"          to "Erotik",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home     = document.select("div.icerik div").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("span:nth-child(1)")?.text()?.trim() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.icerik div").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document
    
    val title = document.selectFirst("div.bilgi h2")?.text()?.trim() ?: return null
    val poster = fixUrlNull(document.selectFirst("div.resim img")?.attr("src"))
    val year = document.selectFirst("a[href*='/yil/']")?.text()?.trim()?.toIntOrNull()
    val description = document.selectFirst("div.slayt-aciklama")?.text()?.trim()
    val tags = document.select("p.tur a[href*='/category/']").map { it.text() }
    val rating = document.selectFirst("span.puan")?.text()?.replace("IMDb : ", "")?.trim()
    val duration = document.selectXpath("//span[contains(text(), 'Süre:')]//following-sibling::b").text()
        .replace("Dakika", "").trim().toIntOrNull()
    val actors = document.select("li.oyuncu-k").map {
        Actor(
            it.selectFirst("span.isim")?.text() ?: it.selectFirst("a")?.attr("title") ?: "",
            fixUrlNull(it.selectFirst("img")?.attr("src"))
        )
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

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("UGF", "data » $data")
        val mainDocument = app.get(data).document

        mainDocument.select("li.parttab a").forEach {
            val subUrl  = fixUrlNull(it.attr("href")) ?: return false
            val document = app.get(subUrl).document

            val iframe   = fixUrlNull(document.selectFirst("div#vast iframe")?.attr("src")) ?: return@forEach
            Log.d("UGF", "iframe » $iframe")

            if (iframe.contains(mainUrl)) {
                val kaynaklar = app.get(iframe, referer=data).document.select("li.c-dropdown__item").associate { kaynak ->
                    kaynak.attr("data-dropdown-value") to kaynak.attr("data-order-value")
                }
                Log.d("UGF", "kaynaklar » $kaynaklar")

                val vidId    = iframe.substringAfter("/play.php?vid=").trim()
                Log.d("UGF", "vidId » $vidId")

                val yuklenenler = mutableListOf<String>()

                for ((kaynak, order) in kaynaklar) {
                    Log.d("UGF", "kaynak » $kaynak | order » $order")

                    val playerApi = app.post(
                        "${mainUrl}/player/ajax_sources.php",
                        data = mapOf(
                            "vid"         to vidId,
                            "alternative" to kaynak,
                            "ord"         to order
                        )
                    ).text
                    val playerData = AppUtils.tryParseJson<AjaxSource>(playerApi) ?: continue
                    Log.d("UGF", "playerData » $playerData")

                    if (playerData.iframe in yuklenenler) continue

                    yuklenenler.add(playerData.iframe)
                    loadExtractor(playerData.iframe, "${mainUrl}/", subtitleCallback, callback)
                }
            } else {
                loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
            }
        }

        return true
    }
}
