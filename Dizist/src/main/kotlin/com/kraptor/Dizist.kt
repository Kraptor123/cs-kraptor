// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.network.DdosGuardKiller
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup

class Dizist : MainAPI() {
    override var mainUrl = "https://dizist.club"
    override var name = "Dizist"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.TvSeries)
    //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,

    override var sequentialMainPage =
        true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay = 250L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 250L  // ? 0.05 saniye

    private var ddosGuardKiller = DdosGuardKiller(true)

    override val mainPage = mainPageOf(
        "${mainUrl}/yabanci-diziler" to "Yabancı Diziler",
        "${mainUrl}/animeler" to "Animeler",
        "${mainUrl}/asyadizileri" to "Asya Dizileri",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page", interceptor = ddosGuardKiller).document
        val home = document.select("div.poster-long.w-full").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-srcset")?.substringBefore(" "))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

//    override suspend fun search(query: String): List<SearchResponse> {
//        // 1. Ana sayfayı çek, cKey/cValue al
//        val initialDoc = app.get("$mainUrl/").document
//        val cKey = initialDoc.selectFirst("input[name=cKey]")?.`val`()
//            ?: throw IllegalStateException("cKey bulunamadı")
//        Log.d("kraptor_$name", "cKey = ${cKey}")
//        val cValue = initialDoc.selectFirst("input[name=cValue]")?.`val`()
//            ?: throw IllegalStateException("cValue bulunamadı")
//        Log.d("kraptor_$name", "cValue = ${cValue}")
//
//        val headers = mapOf(
//            "Host" to "dizist.club",
//            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:139.0) Gecko/20100101 Firefox/139.0",
//            "Accept" to "application/json, text/javascript, */*; q=0.01",
//            "Accept-Language" to "en-US,en;q=0.5",
//            "Referer" to "https://dizist.club",
//            "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
//            "Connection" to "keep-alive",
//        )
//
//        val cookieler = app.get("$mainUrl/").cookies
//
//        val cookies = cookieler + mapOf(
//            "newCkkKeyAw" to "0",
//            "_ga" to "GA1.1.1427395850.1750624028",
//            "_ga_Y0NFQBFM7S" to "GS2.1.s1750629980\$o3\$g1\$t1750629981\$j59\$l0\$h0"
//        )
//
//        Log.d("kraptor_$name", "cookies = ${cookies}")
//
//        val apiResponse = app.post(
//            "$mainUrl/bg/searchcontent", cookies = cookies, headers = headers, data = mapOf(
//                "cKey" to cKey,
//                "cValue" to cValue,
//                "searchTerm" to query
//            )
//        )
//
//        Log.d("kraptor_$name", "apiResponse = ${apiResponse}")
//
//        // 3. JSON'u parse et
//        val json = JSONObject(apiResponse.body.string())
//        if (!json.optBoolean("state", false)) return emptyList()
//        val html = json.optString("html", "")
//
//        // 4. HTML'i JSoup ile parse edip <li> öğelerini seç
//        val doc = Jsoup.parse(html)
//        return doc.select("ul.flex.flex-wrap li").mapNotNull { li ->
//            li.toSearchResult()
//        }
//    }
//
//    // <li> için dönüşümü buraya taşıyoruz
//    private fun Element.toSearchResult(): SearchResponse? {
//        val a = this.selectFirst("a") ?: return null
//        val title = a.selectFirst("span.truncate")?.text()?.trim() ?: return null
//        Log.d("kraptor_$name", "title = ${title}")
//        val href = fixUrlNull(a.attr("href")) ?: return null
//        Log.d("kraptor_$name", "href = ${href}")
//        // data-srcset içindeki ilk URL'i al
//        val poster = a.selectFirst("img")?.attr("data-srcset")
//            ?.substringBefore(" 1x")
//            ?.trim()
//            ?.let { fixUrlNull(it) }
//        Log.d("kraptor_$name", "poster = ${poster}")
//        return newTvSeriesSearchResponse(title, href, TvType.Movie) {
//            this.posterUrl = poster
//        }
//    }
//
//    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val urlget = app.get(url, interceptor = ddosGuardKiller)
        val document = urlget.document
        val text = urlget.text

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("a.block img")?.attr("data-srcset")?.substringBefore(" 1x"))
        val description = document.selectFirst("div.series-profile-summary > p:nth-child(3)")?.text()?.trim()
        val year = document.selectFirst("li.sm\\:w-1\\/5:nth-child(5) > p:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("span.block a").map { it.text() }
        val rating = document.selectFirst("strong.color-imdb")?.text()?.trim()?.toRatingInt()
        val recommendations = document.select("div.poster-long.w-full").mapNotNull { it.toRecommendationResult() }
        val duration = document.selectFirst("li.sm\\:w-1\\/5:nth-child(2) > p:nth-child(2)")?.text()?.replace(" dk", "")
            ?.split(" ")?.first()?.trim()?.toIntOrNull()
        val actors = document.select("li.w-auto.md\\:w-full.flex-shrink-0").map { aktor ->
            val aktorIsim = aktor.selectFirst("p.truncate")?.text()?.trim() ?: return null
            val aktorResim = fixUrlNull(aktor.selectFirst("img")?.attr("data-srcset"))
            Actor(name = aktorIsim, aktorResim)
        }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }
        val regex = Regex(
            pattern = ",\"url\":\"([^\"]*)\",\"dateModified\":\"[^\"]*\"",
            options = setOf(RegexOption.IGNORE_CASE)
        )
        val bolumListesi: List<Episode> = regex.findAll(text)
            .map { match ->
                val raw = match.groupValues[1].replace("\\", "")

                // Eğer '-bolum' yoksa, sonuna '-1-bolum' ekle
                val fullHref = if (!raw.contains("-bolum")) {
                    // Fazla slash olmasın diye trim edebiliriz
                    raw.replace("/sezon/", "/izle/").trimEnd('/') + "-1-bolum"
                } else {
                    raw
                }

                // URL’i düzelt
                val href = fixUrlNull(fullHref)
                val bolumSayisi = href
                    ?.substringBefore("-bolum")
                    ?.substringAfterLast("-")
                    ?.toIntOrNull()
                val sezonSayisi = href
                    ?.substringBefore("-sezon")
                    ?.substringAfterLast("-")
                    ?.toIntOrNull()
                newEpisode(href) {
                    episode = bolumSayisi
                    name = "Bölüm"
//                    season  = sezonSayisi
                }
            }
            .toList()

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, bolumListesi) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.rating = rating
            this.duration = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-srcset")?.substringBefore(" "))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val kaynakLinkleri = document
            .select("div.series-watch-alternatives.series-watch-alternatives-active.mb-5 li a.focus\\:outline-none")

        Log.d("kraptor_$name", "Bulunan kaynak sayısı: ${kaynakLinkleri.size}")

        // 2. Her bir linki işlemek için forEach kullan
        kaynakLinkleri.forEach { linkElem ->
            val href = linkElem.attr("href")
            Log.d("kraptor_$name", "kaynak = $href")

            // 3. Hangi document'i kullanacağına karar ver
            val iframeSrc = if (href.contains("player=0")) {
                // Orijinal document içinden iframe al
                fixUrlNull(document.selectFirst("iframe")!!.attr("src")).toString()
            } else {
                // Linke gidip yeni document al, oradan iframe al
                val yeniDoc = app.get(href).document
                fixUrlNull(yeniDoc.selectFirst("iframe")!!.attr("src")).toString()
            }

            Log.d("kraptor_$name", "iframe = $iframeSrc")

            // 4. Extractor'u çağır
            loadExtractor(iframeSrc, "$mainUrl/", subtitleCallback, callback)
        }

        return true
    }
}