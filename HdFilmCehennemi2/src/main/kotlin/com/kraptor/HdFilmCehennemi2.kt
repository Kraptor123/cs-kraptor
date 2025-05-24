// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class HdFilmCehennemi2 : MainAPI() {
    override var mainUrl              = "https://hdfilmcehennemi2.to"
    override var name                 = "HdFilmCehennemi2"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Yeni Eklenenler",
//        "${mainUrl}/tur/turkce-altyazili"      to  "Türkçe Altyazılı",
//        "${mainUrl}/tur/turkce-dublaj"         to  "Türkçe Dublaj",
//        "${mainUrl}/tur/aile-filmleri"         to  "Aile Filmleri",
//        "${mainUrl}/tur/aksiyon-filmleri"      to  "Aksiyon Filmleri",
//        "${mainUrl}/tur/animasyon-filmleri"    to  "Animasyon Filmleri",
//        "${mainUrl}/tur/belgesel"              to  "Belgesel",
//        "${mainUrl}/tur/bilim-kurgu-filmleri"  to  "Bilim Kurgu Filmleri",
//        "${mainUrl}/tur/biyografi-filmleri"    to  "Biyografi Filmleri",
//        "${mainUrl}/tur/dram-filmleri"         to  "Dram Filmleri",
//        "${mainUrl}/tur/fantastik-filmleri"    to  "Fantastik Filmleri",
//        "${mainUrl}/tur/genel"                 to  "Genel",
//        "${mainUrl}/tur/gerilim-filmleri"      to  "Gerilim Filmleri",
//        "${mainUrl}/tur/gizem-filmleri"        to  "Gizem Filmleri",
//        "${mainUrl}/tur/komedi-filmleri"       to  "Komedi Filmleri",
//        "${mainUrl}/tur/korku-filmleri"        to  "Korku Filmleri",
//        "${mainUrl}/tur/macera-filmleri"       to  "Macera Filmleri",
//        "${mainUrl}/tur/muzik-filmleri"        to  "Müzik Filmleri",
//        "${mainUrl}/tur/muzikal-fimleri"       to  "Müzikal Fimleri",
//        "${mainUrl}/tur/romantik-filmleri"     to  "Romantik Filmleri",
//        "${mainUrl}/tur/savas-filmleri"        to  "Savaş Filmleri",
//        "${mainUrl}/tur/soygun"                to  "Soygun",
//        "${mainUrl}/tur/spor-filmleri"         to  "Spor Filmleri",
//        "${mainUrl}/tur/suc-filmleri"          to  "Suç Filmleri",
//        "${mainUrl}/tur/tarih-filmleri"        to  "Tarih Filmleri",
//        "${mainUrl}/tur/western-filmleri"      to  "Western Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home     = document.select("div.moviefilm").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.movief")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.moviefilm").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.movief")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.filmcontent h1")?.text()?.trim()
            ?.replace("izle","")
            ?.replace(Regex("\\([0-9]+\\).*"), "")
            ?.trim()
            ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.filmaltiimg img")?.attr("src"))
        val rawDescription = document.selectFirst("div.konuozet")?.text()?.trim()
        val description =
            rawDescription?.split("izle,")?.lastOrNull()?.trim()?.replace("hdfilmcehennemi iyi seyirler diler...","")?.replace("hdfilmcehennemi2.net keyifli seyirler diler.","")
        val year            = document.selectFirst("div.filmaltiaciklama > p:nth-child(6)")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.filmaltiaciklama > p:nth-child(7)").map { it.text() }
        val rating          = document.selectFirst("p.block-item a")?.text()?.trim()?.toRatingInt()
        val recommendations = document.select("div.moviefilm").mapNotNull { it.toRecommendationResult() }
        val actors = document.select("div.filmaltiaciklama > p:nth-child(10)")
            .flatMap { element ->
                element.text()
                    .replace("Oyuncular:", "")
                    .split(",")
                    .map { name -> Actor(name) }
            }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }


        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.rating          = rating
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("div.movief")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("hdch2", "data » ${data}")
        val url = listOf(data,
            "${data}/2",
            "${data}/3",
            "${data}/4",
            "${data}/4",

        )

        url.forEach { url->
            val document = app.get(url).document
            val iframe   = fixUrlNull(document.select("div.filmicerik.udvb-container iframe").attr("src")).toString()
                .replace("vidmoly.top","vidmoly.to")
            Log.d("hdch2", "iframe linki » $iframe")
            var headers  = mapOf(
                "User-Agent"     to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36",
                "Sec-Fetch-Dest" to "iframe",
            )
            val iSource = app.get(iframe, headers=headers, referer="https://vidmoly.top/").text
//            Log.d("hdch2", "isource » $iSource")
            val m3uLink = Regex("""file:"([^"]+)""").find(iSource)?.groupValues?.get(1) ?: throw ErrorLoadingException("m3u link not found")

            val regex = Regex("""file:\s*"([^"]+)"""")
            val rawUrls = regex.findAll(iSource)
                .map { it.groupValues[1] }
                .filterNot { it.endsWith(".jpg", ignoreCase = true) }
                .toList()

            if (rawUrls.isEmpty()) {
                throw ErrorLoadingException("sub")
            }

            rawUrls.forEach { url ->
                val fixedUrl = fixUrlNull(url).toString()
                val lang = when {
                    fixedUrl.contains("English", ignoreCase = true) -> "İngilizce"
                    fixedUrl.contains("Turkish", ignoreCase = true) -> "Türkçe"
                    else -> ""
                }

                Log.d("hdch2", "altyazi » $fixedUrl")
                subtitleCallback.invoke(SubtitleFile(lang, fixedUrl))
            }

            Log.d("hdch2", "iframe m3u8» $m3uLink")
                callback.invoke(
                    newExtractorLink(
                        source  = "VidMoly",
                        name    = "VidMoly",
                        url     = m3uLink,
                        type    = INFER_TYPE
                    ) {
                        this.headers = mapOf("Referer" to "https://vidmoly.to/") // "Referer" ayarı burada yapılabilir
                        quality = getQualityFromName(Qualities.Unknown.value.toString())
                    }
                )
            }
        return true
        }
    }
