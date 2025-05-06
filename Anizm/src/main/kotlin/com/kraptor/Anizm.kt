// ! Bu araç @kraptor123 tarafından yazılmıştır.
package com.kraptor

import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Element
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException

class Anizm : MainAPI() {
    override var mainUrl = "https://anizm.net"
    override var name = "Anizm"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Anime)
//    override var sequentialMainPage = true
//    override var sequentialMainPageDelay = 50L
//    override var sequentialMainPageScrollDelay = 50L

    // Cloudflare Bypass


    // JSON Data Class
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AnimeSearchResult(
        @JsonProperty("info_title") val infotitle: String,
        @JsonProperty("info_slug") val infoslug: String,
        @JsonProperty("info_poster") val infoposter: String?,
        @JsonProperty("info_year") val infoyear: String?
    )

    // Ana Sayfa
    override val mainPage = mainPageOf(
        "${mainUrl}/harf?harf=a" to "a",
        "${mainUrl}/harf?harf=b" to "b",
        "${mainUrl}/harf?harf=c" to "c",
        "${mainUrl}/harf?harf=d" to "d",
        "${mainUrl}/harf?harf=e" to "e",
        "${mainUrl}/harf?harf=f" to "f",
        "${mainUrl}/harf?harf=g" to "g",
        "${mainUrl}/harf?harf=h" to "h",
        "${mainUrl}/harf?harf=i" to "i",
        "${mainUrl}/harf?harf=j" to "j",
        "${mainUrl}/harf?harf=k" to "k",
        "${mainUrl}/harf?harf=l" to "l",
        "${mainUrl}/harf?harf=m" to "m",
        "${mainUrl}/harf?harf=n" to "n",
        "${mainUrl}/harf?harf=o" to "o",
        "${mainUrl}/harf?harf=p" to "p",
        "${mainUrl}/harf?harf=q" to "q",
        "${mainUrl}/harf?harf=r" to "r",
        "${mainUrl}/harf?harf=s" to "s",
        "${mainUrl}/harf?harf=t" to "t",
        "${mainUrl}/harf?harf=u" to "u",
        "${mainUrl}/harf?harf=v" to "v",
        "${mainUrl}/harf?harf=w" to "w",
        "${mainUrl}/harf?harf=x" to "x",
        "${mainUrl}/harf?harf=y" to "y",
        "${mainUrl}/harf?harf=z" to "z"
    )

    // Ana Sayfa Yükleme
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val refH = mapOf(
            "Referer" to mainUrl,
            "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15 (Applebot/0.1; +http://www.apple.com/go/applebot)",
            "Sec-Fetch-Site" to "same-origin",
            "Host" to "anizm.net"
            )
        val document = if (page == 1) {
            app.get(request.data, headers = refH).document
        } else {
            app.get("${request.data}&sayfa=$page", headers = refH).document
        }
        val home = document.select("a.pfull").mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.anizm_textUpper.anizm_textBold.truncateText")?.text() ?: return null
        val href = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    // Arama Fonksiyonu (Düzeltilmiş)
    override suspend fun search(query: String): List<SearchResponse> {
        return try {
            // 1. CSRF Token Al
            val csrfToken = app.get(mainUrl)
                .document
                .selectFirst("meta[name='csrf-token']")
                ?.attr("content")
                ?: throw Exception("CSRF Token alınamadı")

            // 2. Sorguyu Encode Et
            val encodedQuery = withContext(Dispatchers.IO) {
                URLEncoder.encode(query, "UTF-8")
            }

            // 3. API İsteği
            val response = app.get(
                "$mainUrl/getAnimeListForSearch",
                headers = mapOf(
                    "Referer" to mainUrl,
                    "X-Requested-With" to "XMLHttpRequest",
                    "X-CSRF-TOKEN" to csrfToken,
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                ),
                params = mapOf("q" to encodedQuery),
                timeout = 5 // Timeout'u 60 saniye yap
            )

            val responseBody = response.body.string()
            val results: List<AnimeSearchResult>? = try {
                parseJson(responseBody)
            } catch (e: Exception) {
                Log.e("ANZM", "JSON Parse Hatası: ${e.message}")
                null
            }

            // 5. Sonuçları işle ve detay sayfasından posterleri çek
            val searchResponses = mutableListOf<SearchResponse>()
            results?.filter {
                it.infotitle.contains(query, ignoreCase = true)
            }?.forEach { item ->
                val detailUrl = "$mainUrl/${item.infoslug}"
                // Detay sayfasından posteri çekmek için ek fonksiyon kullanılıyor
                val poster = getPoster(detailUrl)
                val searchResponse = newAnimeSearchResponse(
                    item.infotitle,
                    detailUrl,
                    TvType.Anime
                ) {
                    posterUrl = poster
                }
                searchResponses.add(searchResponse)
            }
            searchResponses
        } catch (e: CancellationException) {
            // İşlem iptal edildiyse, iptali propagate et
            throw e
        } catch (e: Exception) {
            Log.e("ANZM", "Arama Hatası: ${e.javaClass.simpleName} - ${e.message}")
            emptyList()
        }
    }

    // Detay sayfasından poster URL'si çekmek için yardımcı fonksiyon
    private suspend fun getPoster(url: String): String? {
        return try {
            val doc = app.get(url).document
            fixUrlNull(
                doc.selectFirst("img.anizm_shadow.anizm_round.infoPosterImgItem")?.attr("src")?.let { src ->
                    if (src.startsWith("http")) src else "$mainUrl/$src"
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ANZM", "Poster alınamadı: ${e.message}")
            null
        }
    }

    // Detay Sayfası (Düzeltilmiş)
    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("a.anizm_colorDefault")?.text()?.trim() ?: return null
        val poster = fixUrlNull(
            document.selectFirst("img.anizm_shadow.anizm_round.infoPosterImgItem")?.attr("src")?.let {
                if (it.startsWith("http")) it else "$mainUrl/$it"
            }
        )
        val description = document.selectFirst("div.infoDesc")?.text()?.trim()
        val year = document.selectFirst("div.infoSta.mt-2 li")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("span.ui.label").map { it.text() }
        val rating = document.selectFirst("g.circle-chart__info")?.text()?.trim()?.toRatingInt()
        val trailer = fixUrlNull(document.selectFirst("iframe.yt-hd-thumbnail")?.attr("src"))

        val episodes = document.select("div.four.wide.computer.tablet.five.mobile.column.bolumKutucugu a")
            .mapNotNull { episodeBlock ->
                val epHref = fixUrlNull(episodeBlock.attr("href")) ?: return@mapNotNull null
                val epTitle = episodeBlock.selectFirst("div.episodeBlock")?.ownText()?.trim() ?: "Bölüm"
                newEpisode(epHref) {
                    this.name = epTitle
                }
            }

        return newTvSeriesLoadResponse(title, url, TvType.Anime, episodes) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.rating = rating
            addTrailer(trailer)
        }
    }

    // Video Linkleri
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val videoLinks = getVideoUrls(data)
        videoLinks.forEach { (name, url) ->
            when {
                url.contains("anizmplayer.com") -> {
                    AincradExtractor().getUrl(url, mainUrl).forEach(callback)
                    return true
                }
                else -> {
                    loadExtractor(
                        url = url,
                        referer = mainUrl,
                        subtitleCallback = subtitleCallback,
                        callback = callback
                    )
                }
            }
        }
        return true
    }
}