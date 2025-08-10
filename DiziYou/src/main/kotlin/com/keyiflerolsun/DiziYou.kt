// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Element

class DiziYou : MainAPI() {
    override var mainUrl              = "https://www.diziyou13.com"
    override var name                 = "DiziYou"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        ""  to "Yeni Eklenenler",
        "${mainUrl}/"   to "Ana Sayfa",
//        "" to "Aile",
//        "" to "Aksiyon",
//        "" to "Animasyon",
//        "" to "Belgesel",
//        "" to "Bilim Kurgu",
//        "" to "Dram",
//        "" to "Fantazi",
//        "" to "Gerilim",
//        "" to "Gizem",
//        "" to "Komedi",
//        "" to "Korku",
//        "" to "Macera",
//        "" to "Savaş",
//        "" to "Suç",
//        "" to "Vahşi Batı"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (request.name.contains("Ana Sayfa")) {
            app.get(
                request.data,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
                    "Referer" to "$mainUrl/",
                    "Cookie" to "wordpress_test_cookie=WP+Cookie+check; wordpress_logged_in_32080760cc27b19056828b6dab487783=karaOsman%7C1755826436%7CkTRdcilQ3fHoAaskjoLhyNFfv2PGDakAyZeh2wdpFsL%7C8417df2c2c603c445ffa06dd66f1fa3a8c7c8b875658b65342eeab756500a48d"
                )
            ).document
        } else {
            app.get(
                "${mainUrl}/dizi-arsivi/page/$page/?filtrele=imdb&sirala=DESC&yil&imdb&kelime&tur=${request.name}&sirala=DESC&yil&imdb&kelime&tur=${request.name}",
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
                    "Referer" to "$mainUrl/",
                    "Cookie" to "wordpress_test_cookie=WP+Cookie+check; wordpress_logged_in_32080760cc27b19056828b6dab487783=karaOsman%7C1755826436%7CkTRdcilQ3fHoAaskjoLhyNFfv2PGDakAyZeh2wdpFsL%7C8417df2c2c603c445ffa06dd66f1fa3a8c7c8b875658b65342eeab756500a48d"
                )
            ).document
        }

        val home     = document.select("div.single-item, div#list-series-main").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div#categorytitle a, div.cat-title-main")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div#categorytitle a, a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val score     = this.selectFirst("div.cat-imdb-main")?.text()

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(score)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.incontent div#list-series").mapNotNull { it.toMainPageResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.category_image img")?.attr("src"))
        val description     = document.selectFirst("div.diziyou_desc")?.ownText()?.trim()
        val year            = document.selectFirst("span.dizimeta:contains(Yapım Yılı)")?.nextSibling()?.toString()?.trim()?.toIntOrNull()
        val tags            = document.select("div.genres a").map { it.text() }
        val rating          = document.selectFirst("span.dizimeta:contains(IMDB)")?.nextSibling()?.toString()?.trim()
        val actors          = document.selectFirst("span.dizimeta:contains(Oyuncular)")?.nextSibling()?.toString()?.trim()?.split(", ")?.map { Actor(it) }
        val trailer         = document.selectFirst("iframe.trailer-video")?.attr("src")

        val episodes = document.select("div.bolumust").mapNotNull {
            val epName    = it.selectFirst("div.baslik")?.ownText()?.trim() ?: return@mapNotNull null
            val epHref    = it.closest("a")?.attr("href")?.let { href -> fixUrlNull(href) } ?: return@mapNotNull null
            val epEpisode = Regex("""(\d+)\. Bölüm""").find(epName)?.groupValues?.get(1)?.toIntOrNull()
            val epSeason  = Regex("""(\d+)\. Sezon""").find(epName)?.groupValues?.get(1)?.toIntOrNull() ?: 1

            newEpisode(epHref) {
                this.name = it.selectFirst("div.bolumismi")?.text()?.trim()?.replace(Regex("""[()]"""), "")?.trim() ?: epName
                this.season = epSeason
                this.episode = epEpisode
                this.posterUrl = poster
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot      = description
            this.year      = year
            this.tags      = tags
            this.score = Score.from10(rating)
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("DZY", "data » $data")
        val document = app.get(data).document

        val itemId     = document.selectFirst("iframe#diziyouPlayer")?.attr("src")?.split("/")?.lastOrNull()?.substringBefore(".html") ?: return false
        Log.d("DZY", "itemId » $itemId")

        val subTitles  = mutableListOf<DiziyouSubtitle>()
        val streamUrls = mutableListOf<DiziyouStream>()
        val storage    = mainUrl.replace("www", "storage")

        document.select("span.diziyouOption").forEach {
            val optId   = it.attr("id")

            if (optId == "turkceAltyazili") {
                subTitles.add(DiziyouSubtitle("Turkish", "${storage}/subtitles/${itemId}/tr.vtt"))
                streamUrls.add(DiziyouStream("Orjinal Dil", "${storage}/episodes/${itemId}/play.m3u8"))
            }

            if (optId == "ingilizceAltyazili") {
                subTitles.add(DiziyouSubtitle("English", "${storage}/subtitles/${itemId}/en.vtt"))
                streamUrls.add(DiziyouStream("Orjinal Dil", "${storage}/episodes/${itemId}/play.m3u8"))
            }

            if (optId == "turkceDublaj") {
                streamUrls.add(DiziyouStream("Türkçe Dublaj", "${storage}/episodes/${itemId}_tr/play.m3u8"))
            }
        }

        for (sub in subTitles) {
            subtitleCallback.invoke(
                SubtitleFile(
                    lang = sub.name,
                    url  = fixUrl(sub.url)
                )
            )
        }

        for (stream in streamUrls) {
            callback.invoke(
                newExtractorLink(
                    source  = this.name,
                    name    = this.name,
                    url     = stream.url,
                    type = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to "${mainUrl}/") // Referer burada ayarlandı
                    quality = Qualities.Unknown.value // Kalite ayarlandı
                }
            )
        }

        return true
    }

    data class DiziyouSubtitle(val name: String, val url: String)
    data class DiziyouStream(val name: String, val url: String)
}