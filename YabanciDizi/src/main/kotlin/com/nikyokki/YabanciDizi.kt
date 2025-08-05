package com.nikyokki

import CryptoJS
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.*
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class YabanciDizi : MainAPI() {
    override var mainUrl = "https://yabancidizi.tv"
    override var name = "YabanciDizi"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    // ! CloudFlare bypass
    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)

            // Cloudflare'ın tüm varyantlarını yakala
            if (response.peekBody(1024).string().contains("Cloudflare")) {
                return cloudflareKiller.intercept(chain).also {
                    Log.d("YBD", "Cloudflare bypass yapıldı!")
                }
            }
            return response
        }
    }
    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 250L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 250L  // ? 0.05 saniye

    override val mainPage = mainPageOf(
        "${mainUrl}/dizi/tur/aile-izle" to "Aile",
        "${mainUrl}/dizi/tur/aksiyon-izle-1" to "Aksiyon",
        "${mainUrl}/dizi/tur/bilim-kurgu-izle-1" to "Bilim Kurgu",
        "${mainUrl}/dizi/tur/belgesel" to "Belgesel",
        "${mainUrl}/dizi/tur/dram-izle" to "Dram",
        "${mainUrl}/dizi/tur/fantastik-izle" to "Fantastik",
        "${mainUrl}/dizi/tur/gerilim-izle" to "Gerilim",
        "${mainUrl}/dizi/tur/gizem-izle" to "Gizem",
        "${mainUrl}/dizi/tur/komedi-izle" to "Komedi",
        "${mainUrl}/dizi/tur/korku-izle" to "Korku",
        "${mainUrl}/dizi/tur/macera-izle" to "Macera",
        "${mainUrl}/dizi/tur/romantik-izle-1" to "Romantik",
        "${mainUrl}/dizi/tur/suc" to "Suç",
        "${mainUrl}/dizi/tur/kore-dizileri" to "Kore Dizileri",
        "${mainUrl}/dizi/tur/stand-up" to "Stand Up",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/${page}", interceptor = interceptor).document
        val home = document.select("div.mofy-movbox").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.mofy-movbox-text a")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.post(
            "${mainUrl}/search?qr=$query",
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
            referer = "${mainUrl}/",
        )

        val parsedSafe = response.parsedSafe<JsonResponse>()
        val results = mutableListOf<SearchResponse>()

        if (parsedSafe?.success == 1) {
            parsedSafe.data.result.forEach {
                println("    s_type: ${it.s_type}")
                println("    s_link: ${it.s_link}")
                println("    s_name: ${it.s_name}")
                println("    s_image: ${it.s_image}")
                println("    s_year: ${it.s_year}")
                val title = it.s_name
                val posterUrl = fixUrlNull("$mainUrl/uploads/series/${it.s_image}") ?: ""
                if (it.s_type == "0") {
                    val href = fixUrlNull("$mainUrl/dizi/${it.s_link}") ?: ""
                    results.add(newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                        this.posterUrl = posterUrl
                    })
                } else if (it.s_type == "1") {
                    val href = fixUrlNull("$mainUrl/film/${it.s_link}") ?: ""
                    results.add(newMovieSearchResponse(title, href, TvType.Movie) {
                        this.posterUrl = posterUrl
                    })
                }
            }
        }

        return results
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse {
        val headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        )
        val document = app.get(url, referer = mainUrl, headers = headers, interceptor = interceptor).document

        val title = document.selectFirst("h1.page-title")?.text()?.trim() ?: "Title"
        val poster = fixUrlNull(document.selectFirst("div#series-profile-wrapper img")?.attr("src"))
            ?: ""
        val year =
            document.selectFirst("h1 span")?.text()?.substringAfter("(")?.substringBefore(")")
                ?.toIntOrNull()
        val description = document.selectFirst("div.series-summary-wrapper p")?.text()?.trim()
        val tags = mutableListOf<String>()
        document.select("div.ui.list a").forEach {
            if (!it.attr("href").contains("/oyuncu/")) {
                tags.add(it.text().trim())
            }
        }
        val rating = document.selectFirst("div.color-imdb")?.text()?.trim()
        val duration =
            document.selectXpath("//div[text()='Süre']//following-sibling::div").text().trim()
                .split(" ").first().toIntOrNull()
        val trailer = document.selectFirst("div.media-trailer")?.attr("data-yt")
        val actors = document.selectFirst("div.global-box")?.select("div.item")?.map {
            Actor(it.selectFirst("h5")!!.text(), fixUrlNull(it.selectFirst("img")!!.attr("src")))
        }
        if (url.contains("/dizi/")) {
            val episodes = mutableListOf<Episode>()
            document.select("div.tabular-content").forEach {
                val epSeason = it.parent()?.attr("data-season")?.toIntOrNull()
                var epEpisode = 0
                it.select("div.item").forEach ep@{ episodeElement ->
                    val epHref =
                        fixUrlNull(episodeElement.selectFirst("h6 a")?.attr("href")) ?: return@ep
                    epEpisode++
                    episodes.add(
                        newEpisode(
                            data = epHref,
                            ({
                                name = "${epSeason}. Sezon ${epEpisode}. Bölüm"
                                season = epSeason
                                episode = epEpisode
                            }
                        )
                        )
                    )
                }
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                addActors(actors)
                addTrailer("https://www.youtube.com/embed/${trailer}")
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(rating)
                this.duration = duration
                addActors(actors)
                addTrailer("https://www.youtube.com/embed/${trailer}")
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("div.alternatives-for-this div").forEach {
            val name = it.text()
            val dataLink = it.attr("data-link")
            if (name.contains("Mac")) {
                val mac = app.get(
                    "https://yabancidizi.tv/api/drive/" +
                            dataLink.replace("/", "_").replace("+", "-"),headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                        "Referer" to "$mainUrl/",
                        "Cookie" to "cf_clearance=SzcXE7kPi3ZEiEe7g5CIdusr1zh78qvvolE.gd7zXoE-1744138962-1.2.1.1-p2EURbN9Azg427t4V9cZVAqkCNIqn10vJgLdl3uLpmNulOlCxpneDAMr1esuz6n5kUDF3l9kDkoxxMqYpAGxBX5xx4LHJefJIKYbpRTTQrLa3EQm.esZbXpVYHM.XmTtyE5B9CSK_AIOJ0tvp1.4P9BvqHOGehu5eA3dEfQa1kIojFR1Kz3CsefgXVjCP5bPPBtRZGxWIAnbALrvvgWdIfHFte9NWarOXtZclr6sDlpqpA_empvpr6T2915Es9uyN.fbF0lxoC8v1WmRLlhtw3qln3y8uJR65aTRzLJCVciVWoJCaUV.fN_gqpP3Af1azDvqEPpa6dRiZ77Kc7oc0waEMv..5gz2BbI8rPhAIcg; ci_session=upqat5dadbda3algortaksra1f3bcalr; level=1; _ga_53GGW5VVJQ=GS1.1.1744106126.1.1.1744142896.0.0.0; _ga=GA1.1.1124796233.1744106126; _gid=GA1.2.2011384401.1744106126; udys=1744138960932; _gat_gtag_UA_274501025_1=1" // yukarıdaki gibi
                    ),
                    interceptor = interceptor
                ).document
                val subFrame = mac.selectFirst("iframe")?.attr("src") ?: return false
//                Log.d("kraptor_$name","mac = $mac")
                val iDoc = app.get(
                    subFrame, referer = "${mainUrl}/",
                    headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0")
                ).text
//                Log.d("kraptor_$name","iDoc = $iDoc")
                val cryptData =
                    Regex("""CryptoJS\.AES\.decrypt\("(.*)","""").find(iDoc)?.groupValues?.get(1)
                        ?: ""
//                Log.d("kraptor_$name","cryptData = $cryptData")
                val cryptPass =
                    Regex("""","(.*)"\);""").find(iDoc)?.groupValues?.get(1) ?: ""
//                Log.d("kraptor_$name","cryptPass = $cryptPass")
                val decryptedData = CryptoJS.decrypt(cryptPass, cryptData)
//                Log.d("kraptor_$name","decryptedData = $decryptedData")
                val decryptedDoc = Jsoup.parse(decryptedData)
                val source = decryptedDoc.selectFirst("source")?.attr("src").toString()
                Log.d("kraptor_$name","source = $source")
                val vidUrl =
                    Regex("""file: '(.*)',""").find(decryptedDoc.html())?.groupValues?.get(1)
                        ?: ""
                Log.d("YBD", "Extractor Link Olusturuluyor -> name: $name, url: $vidUrl")
                val aa = app.get(
                    source,
                    referer = "$mainUrl/",
                    headers =
                        mapOf(
                            "Accept" to "*/*",
                            "Accept-Language" to "en-US,en;q=0.9",
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                            "Host" to "dbx.molystream.org",
                            "Origin" to mainUrl,
                            "Referer" to "$mainUrl/",
                            "Connection" to "keep-alive",
                            "Cookie" to "ud=1; Path=/",
                            "Sec-Fetch-Dest" to "empty",
                            "Sec-Fetch-Mode" to "cors",
                            "Sec-Fetch-Site" to "cross-site",
                            "TE" to "trailers",
                            ), interceptor = interceptor).document.body().text()
                val urlList = extractStreamInfoWithRegex(aa)
                Log.d("kraptor_$name","urlList = $urlList")
                for (sonUrl in urlList) {
                    Log.d("kraptor_$name", "sonUrl: ${sonUrl.link} -- ${sonUrl.resolution}")
                    callback.invoke(
                        newExtractorLink(
                            source = "$name -- ${sonUrl.resolution}",
                            name = "$name -- ${sonUrl.resolution}",
                            url = sonUrl.link,
                            type = ExtractorLinkType.M3U8
                        ) {
                            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
                                "Referer" to "https://ydx.molystream.org/") // "Referer" ayarı burada yapılabilir
                            quality = getQualityFromName(sonUrl.resolution)
                        }
                    )
                    return true
                }
            } else if (name.contains("VidMoly")) {
                val mac = app.post(
                    "https://yabancidizi.tv/api/moly/" +
                            dataLink.replace("/", "_").replace("+", "-"), referer = "$mainUrl/"
                            , interceptor = interceptor).document
                val subFrame = mac.selectFirst("iframe")?.attr("src") ?: return false
                loadExtractor(subFrame, "${mainUrl}/", subtitleCallback, callback)
            } else if (name.contains("Okru")) {
                val mac = app.post(
                    "https://yabancidizi.tv/api/ruplay/" +
                            dataLink.replace("/", "_").replace("+", "-"), referer = "$mainUrl/"
                            , interceptor = interceptor).document
                val subFrame = mac.selectFirst("iframe")?.attr("src") ?: return false
                loadExtractor(subFrame, "${mainUrl}/", subtitleCallback, callback)
            }
        }
        return true
    }

    private fun extractStreamInfoWithRegex(m3uString: String): List<StreamInfo> {
        val regex =
            """#EXT-X-STREAM-INF:.*?RESOLUTION=([^\s,]+).*?(https?://\S+)(?:\s|$)""".toRegex()
        val streamInfoList = regex.findAll(m3uString)
            .map { matchResult ->
                val resolution = matchResult.groupValues[1]
                val link = matchResult.groupValues[2]
                StreamInfo(resolution, link)
            }
            .toList()
        return streamInfoList
    }
}

data class StreamInfo(val resolution: String, val link: String)