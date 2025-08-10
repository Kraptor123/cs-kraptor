// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.network.DdosGuardKiller
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder

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
    private var cookieler: Map<String, String>? = null
    private var cKey: String? = null
    private var cValue: String? = null
    private val initMutex = Mutex()
    private suspend fun initSession() {
        if (cookieler != null  && cKey != null && cValue != null) return
        initMutex.withLock {
            if (!cookieler.isNullOrEmpty() && cKey != null && cValue != null) return@withLock

//            Log.d("kraptor_Dizist", "🔄 Oturum başlatılıyor: cookie, cKey ve cValue alınıyor")

            val resp = app.get("${mainUrl}/",  timeout = 120)

            cookieler      = resp.cookies

            if (cookieler.isNullOrEmpty()) {
                throw IllegalStateException("Çerezler boş olamaz")
            }

            val document = resp.document
            cKey = document.selectFirst("input[name=cKey]")?.`val`()
            cValue = document.selectFirst("input[name=cValue]")?.`val`()

            Log.d("kraptor_Dizist", "cKey: $cKey, cValue: ${cValue}")
        }
    }

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Yeni Eklenen Bölümler",
        "${mainUrl}/yabanci-diziler" to "Yabancı Diziler",
        "${mainUrl}/animeler" to "Animeler",
        "${mainUrl}/asyadizileri" to "Asya Dizileri",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        initSession()
        val cookies: Map<String, String> = cookieler!!
        val document = if (request.name.contains("Yeni Eklenen Bölümler")) {
            app.get("${request.data}", cookies = cookies, interceptor = ddosGuardKiller).document
        }else{
            app.get("${request.data}/page/$page", cookies = cookies, interceptor = ddosGuardKiller).document
        }
//        Log.d("kraptor_Dizist", "neden bos ${document.select("div.poster-xs")}")
        val home = if (request.name.contains("Yeni Eklenen Bölümler")) {
            document.select("div.poster-xs")
                .mapNotNull { it.toMainPageResult() }
        } else {
            document.select("div.poster-long.w-full")
                .mapNotNull { it.toMainPageResult() }
        }

        val hasNext = !request.name.contains("Yeni Eklenen Bölümler")

        return newHomePageResponse(request.name, home, hasNext = hasNext)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")?.replace("/izle/", "/dizi/")?.replace(Regex("-[0-9]+.*$"), "")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-srcset")?.substringBefore(" "))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // 1. Ana sayfayı çek, cKey/cValue al
        initSession()
        val cookies: Map<String, String> = cookieler!!
        val apiResponse = app.post(
            "$mainUrl/bg/searchcontent", cookies = cookies , data = mapOf(
                "cKey"      to (cKey      ?: ""),
                "cValue"    to (cValue    ?: ""),
                "searchTerm" to query
            )
        )
        val dataObj = JSONObject(apiResponse.text).getJSONObject("data")
        val html     = dataObj.getString("html")
        val doc = Jsoup.parseBodyFragment(html)
        return doc.select("ul.flex.flex-wrap li").mapNotNull { li ->
            li.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val a = this.selectFirst("a") ?: return null
        val title = a.selectFirst("span.truncate")?.text()?.trim() ?: return null
//        Log.d("kraptor_$name", "title = ${title}")
        val href = fixUrlNull(a.attr("href")) ?: return null
//        Log.d("kraptor_$name", "href = ${href}")
        val poster = a.selectFirst("img")?.attr("data-srcset")
            ?.substringBefore(" 1x")
            ?.trim()
            ?.let { fixUrlNull(it) }
//        Log.d("kraptor_$name", "poster = ${poster}")
        return newTvSeriesSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        initSession()
        val cookies: Map<String, String> = cookieler!!
        val urlget = app.get(url, cookies = cookies, interceptor = ddosGuardKiller)
        val document = urlget.document
        val text = urlget.text

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("a.block img")?.attr("data-srcset")?.substringBefore(" 1x"))
val description = document.selectFirst("div.series-profile-summary > p:nth-child(3)")?.text()?.trim()
    ?: document.selectFirst("div.series-profile-summary > p:nth-child(2)")?.text()?.trim()
        val year = document.selectFirst("li.sm\\:w-1\\/5:nth-child(5) > p:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("span.block a").map { it.text() }
        val rating = document.selectFirst("strong.color-imdb")?.text()?.trim()
        val recommendations = document.select("div.poster-long.w-full").mapNotNull { it.toRecommendationResult() }
        val duration = document.selectFirst("li.sm\\:w-1\\/5:nth-child(2) > p:nth-child(2)")?.text()?.replace(" dk", "")
            ?.split(" ")?.first()?.trim()?.toIntOrNull()
        val actors = document.select("li.w-auto.md\\:w-full.flex-shrink-0").map { aktor ->
            val aktorIsim = aktor.selectFirst("p.truncate")?.text()?.trim() ?: return null
            val aktorResim = fixUrlNull(aktor.selectFirst("img")?.attr("data-srcset"))?.substringBefore(" ")
            Actor(name = aktorIsim, fixUrlNull(aktorResim))
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
                    ?.replace("-","")
                    ?.toIntOrNull()
                newEpisode(href) {
                    episode = bolumSayisi
                    name = "Bölüm"
                    season  = sezonSayisi
                    posterUrl = poster
                }
            }
            .toList()

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, bolumListesi) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(rating)
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

        return newTvSeriesSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        initSession()
        val cookies: Map<String, String> = cookieler!!
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data, cookies = cookies).document

        val kaynakLinkleri = document
            .select("div.series-watch-alternatives.series-watch-alternatives-active.mb-5 li a.focus\\:outline-none")

//        Log.d("kraptor_$name", "Bulunan kaynak sayısı: ${kaynakLinkleri.size}")

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