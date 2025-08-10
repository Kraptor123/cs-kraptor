
package com.keyiflerolsun

import android.util.Base64
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.StringUtils.decodeUri
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class DiziBox : MainAPI() {
    override var mainUrl              = "https://www.dizibox.so"
    override var name                 = "DiziBox"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.TvSeries)

    // ! CloudFlare bypass
    override var sequentialMainPageDelay       = 50L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 50L  // ? 0.05 saniye

    // ! CloudFlare v2
    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request  = chain.request()
            val response = chain.proceed(request)
            val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

            if (doc.text().contains("Güvenlik taramasından geçiriliyorsunuz. Lütfen bekleyiniz..")) {
                return cloudflareKiller.intercept(chain)
            }

            return response
        }
    }

    override val mainPage = mainPageOf(
        "${mainUrl}/tum-bolumler/page/SAYFA/?tip=populer"   to "Popüler Dizilerden Son Bölümler",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?ulke[]=turkiye&yil=&imdb"   to "Yerli",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=aile&yil&imdb"       to "Aile",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=aksiyon&yil&imdb"    to "Aksiyon",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=animasyon&yil&imdb"  to "Animasyon",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=belgesel&yil&imdb"   to "Belgesel",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=bilimkurgu&yil&imdb" to "Bilimkurgu",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=biyografi&yil&imdb"  to "Biyografi",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=dram&yil&imdb"       to "Dram",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=drama&yil&imdb"      to "Drama",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=fantastik&yil&imdb"  to "Fantastik",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=gerilim&yil&imdb"    to "Gerilim",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=gizem&yil&imdb"      to "Gizem",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=komedi&yil&imdb"     to "Komedi",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=korku&yil&imdb"      to "Korku",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=macera&yil&imdb"     to "Macera",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=muzik&yil&imdb"      to "Müzik",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=muzikal&yil&imdb"    to "Müzikal",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=reality-tv&yil&imdb" to "Reality TV",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=romantik&yil&imdb"   to "Romantik",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=savas&yil&imdb"      to "Savaş",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=spor&yil&imdb"       to "Spor",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=suc&yil&imdb"        to "Suç",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=tarih&yil&imdb"      to "Tarih",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=western&yil&imdb"    to "Western",
        "${mainUrl}/dizi-arsivi/page/SAYFA/?tur[0]=yarisma&yil&imdb"    to "Yarışma"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url      = request.data.replace("SAYFA", "$page")
        val document = app.get(
            url,
            cookies     = mapOf(
                "LockUser"      to "true",
                "isTrustedUser" to "true",
                "dbxu"          to "1744054959089"
            ),
            interceptor = interceptor
        ).document
        val home = document.select("article.detailed-article, article.article-episode-card a.figure-link")
            .mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h3 a")?.text() ?: this.selectFirst("img")?.attr("alt") ?: return null
        val posterUrl    = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val href      = fixUrlNull(this.selectFirst("h3 a")?.attr("href")) ?: (this.selectFirst("a.figure-link")?.attr("href")
            ?.replace(Regex(pattern = """-[0-9]+-.*""", options = setOf(RegexOption.IGNORE_CASE)),"/")
            ?.replace("$mainUrl/","$mainUrl/diziler/") ?: return null)


        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
    val url = "$mainUrl/wp-admin/admin-ajax.php?s=${query}&action=dwls_search"
    
    val headers = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Accept" to "application/json, text/javascript, */*; q=0.01",
        "Referer" to "$mainUrl/beta/?s=$query"
    )
    
    val cookies = mapOf(
        "LockUser" to "true",
        "isTrustedUser" to "true",
        "dbxu" to "1744054959089"
    )
    
    val response = app.get(url, headers = headers, cookies = cookies)
    val jsonResponse = response.parsed<SearchApiResponse>()
    
    return jsonResponse.results.mapNotNull { it.toSearchResult() }
}

data class SearchApiResponse(
    val searchTerms: String,
    val results: List<SearchResult>
)

data class SearchResult(
    val ID: Int,
    val post_title: String,
    val post_excerpt: String,
    val permalink: String,
    val attachment_thumbnail: String?
)

private fun SearchResult.toSearchResult(): SearchResponse? {
    val title = this.post_title.ifEmpty { return null }
    val href = fixUrlNull(this.permalink) ?: return null
    
    val rawPoster = this.attachment_thumbnail
    val posterUrl = fixUrlNull(rawPoster?.replace("50x50", "200x290"))

    return newMovieSearchResponse(title, href, TvType.AsianDrama) { 
        this.posterUrl = posterUrl 
    }
}

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(
            url,
            cookies     = mapOf(
                "LockUser"      to "true",
                "isTrustedUser" to "true",
                "dbxu"          to "1744054959089"
            ),
            interceptor = interceptor
        ).document

        val title       = document.selectFirst("div.tv-overview h1 a")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(document.selectFirst("div.tv-overview figure img")?.attr("src"))
        val description = document.selectFirst("div.tv-story p")?.text()?.trim()
        val year        = document.selectFirst("a[href*='/yil/']")?.text()?.trim()?.toIntOrNull()
        val tags        = document.select("a[href*='/tur/']").map { it.text() }
        val rating      = document.selectFirst("span.label-imdb b")?.text()?.trim()
        val actors      = document.select("a[href*='/oyuncu/']").map { Actor(it.text()) }
        val trailer     = document.selectFirst("div.tv-overview iframe")?.attr("src")

        val episodeList = mutableListOf<Episode>()
        document.select("div#seasons-list a").forEach {
            val epUrl = fixUrlNull(it.attr("href")) ?: return@forEach
            val epDoc = app.get(
                epUrl,
                cookies     = mapOf(
                    "LockUser"      to "true",
                    "isTrustedUser" to "true",
                    "dbxu"          to "1744054959089"
                ),
                interceptor = interceptor
            ).document

            epDoc.select("article.grid-box").forEach ep@ { epElem ->
                val epTitle   = epElem.selectFirst("div.post-title a")?.text()?.trim() ?: return@ep
                val epHref    = fixUrlNull(epElem.selectFirst("div.post-title a")?.attr("href")) ?: return@ep
                val epSeason  = Regex("""(\d+)\. ?Sezon""").find(epTitle)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val epEpisode = Regex("""(\d+)\. ?Bölüm""").find(epTitle)?.groupValues?.get(1)?.toIntOrNull()

                episodeList.add(newEpisode(epHref) {
                    this.name = epTitle
                    this.season = epSeason
                    this.episode = epEpisode
                    this.posterUrl = poster
                })
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeList) {
            this.posterUrl = poster
            this.plot      = description
            this.year      = year
            this.tags      = tags
            this.score = Score.from10(rating)
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private suspend fun iframeDecode(data:String, iframe:String, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        @Suppress("NAME_SHADOWING") var iframe = iframe

        if (iframe.contains("/player/king/king.php")) {
   
    val subDoc = app.get(
        iframe.replace("king.php?v=", "king.php?wmode=opaque&v="),
        referer = data,
        cookies = mapOf(
            "LockUser" to "true",
            "isTrustedUser" to "true",
            "dbxu" to "1744054959089"
        ),
        interceptor = interceptor
    ).document

    
    val embedUrl = subDoc.selectFirst("div#Player iframe")?.attr("src") ?: return false

    
    val sheilaUrl = embedUrl.replace("/embed/", "/embed/sheila/")

   
    val m3uContent = app.get(
        sheilaUrl,
        referer = embedUrl 
    ).text

    
    val m3u8Url = m3uContent.lineSequence()
        .firstOrNull { it.startsWith("http") } ?: return false

   
    callback.invoke(
        newExtractorLink(
            source = this.name,
            name = this.name,
            url = m3u8Url,
            type = ExtractorLinkType.M3U8
        ) {
            headers = mapOf("Referer" to embedUrl)
            quality = Qualities.P1080.value
        }
    )
}
else if (iframe.contains("/player/moly/moly.php")) {
            iframe = iframe.replace("moly.php?h=", "moly.php?wmode=opaque&h=")
            var subDoc = app.get(
                iframe,
                referer     = data,
                cookies     = mapOf(
                    "LockUser"      to "true",
                    "isTrustedUser" to "true",
                    "dbxu"          to "1744054959089"
                ),
                interceptor = interceptor
            ).document

            val atobData = Regex("""unescape\("(.*)"\)""").find(subDoc.html())?.groupValues?.get(1)
            if (atobData != null) {
                val decodedAtob = atobData.decodeUri()
                val strAtob     = String(Base64.decode(decodedAtob, Base64.DEFAULT), Charsets.UTF_8)
                subDoc          = Jsoup.parse(strAtob)
            }

            val subFrame = subDoc.selectFirst("div#Player iframe")?.attr("src") ?: return false

            loadExtractor(subFrame, "${mainUrl}/", subtitleCallback, callback)

        } else if (iframe.contains("/player/haydi.php")) {
            iframe = iframe.replace("haydi.php?v=", "haydi.php?wmode=opaque&v=")
            var subDoc = app.get(
                iframe,
                referer     = data,
                cookies     = mapOf(
                    "LockUser"      to "true",
                    "isTrustedUser" to "true",
                    "dbxu"          to "1744054959089"
                ),
                interceptor = interceptor
            ).document

            val atobData = Regex("""unescape\("(.*)"\)""").find(subDoc.html())?.groupValues?.get(1)
            if (atobData != null) {
                val decodedAtob = atobData.decodeUri()
                val strAtob     = String(Base64.decode(decodedAtob, Base64.DEFAULT), Charsets.UTF_8)
                subDoc          = Jsoup.parse(strAtob)
            }

            val subFrame = subDoc.selectFirst("div#Player iframe")?.attr("src") ?: return false

            loadExtractor(subFrame, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("DZBX", "data » $data")
        val document = app.get(
            data,
            cookies     = mapOf(
                "LockUser"      to "true",
                "isTrustedUser" to "true",
                "dbxu"          to "1744054959089"
            ),
            interceptor = interceptor
        ).document
        var iframe = document.selectFirst("div#video-area iframe")?.attr("src")?: return false
        Log.d("DZBX", "iframe » $iframe")

        iframeDecode(data, iframe, subtitleCallback, callback)

        document.select("div.video-toolbar option[value]").forEach {
            val altLink = it.attr("value")
            val subDoc  = app.get(
                altLink,
                cookies     = mapOf(
                    "LockUser"      to "true",
                    "isTrustedUser" to "true",
                    "dbxu"          to "1744054959089"
                ),
                interceptor = interceptor
            ).document
            iframe = subDoc.selectFirst("div#video-area iframe")?.attr("src")?: return false
            Log.d("DZBX", "iframe » $iframe")

            iframeDecode(data, iframe, subtitleCallback, callback)
        }

        return true
    }
}