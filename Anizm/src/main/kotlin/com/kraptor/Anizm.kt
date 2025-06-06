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
        "2"  to "Aksiyon",
        "52" to "Arabalar",
        "7"  to "Askeri",
        "8"  to "Bilim-Kurgu",
        "50" to "Bunama",
        "9"  to "Büyü",
        "49" to "Çocuklar",
        "10" to "Dedektif",
        "54" to "Dementia",
        "11" to "Doğaüstü-Güçler",
        "12" to "Dövüş",
        "30" to "Dövüş Sanatları",
        "4"  to "Dram",
        "6"  to "Ecchi",
        "56" to "Erkeklerin Aşkı",
        "13" to "Fantastik",
        "14" to "Gerilim",
        "15" to "Gizem",
        "16" to "Harem",
        "17" to "Hazine-Avcılığı",
        "55" to "Hentai",
        "18" to "Josei",
        "57" to "Kızların Aşkı",
        "3"  to "Komedi",
        "20" to "Korku",
//        "51" to "Live Action",
        "1"  to "Macera",
        "21" to "Mecha",
        "22" to "Movie",
        "23" to "Müzik",
        "24" to "Ninja",
        "25" to "OAD - ONA - OVA",
        "26" to "Okul",
        "27" to "Oyun",
        "48" to "Parodi",
        "53" to "Polisiye",
//        "28" to "Politik",
        "29" to "Psikolojik",
        "5"  to "Romantizm",
        "47" to "Samuray",
        "46" to "Savaş",
        "31" to "Seinen",
        "45" to "Şeytanlar",
        "32" to "Shoujo",
        "33" to "Shoujo-Ai",
        "34" to "Shounen",
        "35" to "Shounen-Ai",
        "37" to "Spor",
        "38" to "Süper-Güç",
        "39" to "Tarihi",
//        "40" to "Tuhaf",
        "41" to "Uzay",
        "42" to "Vampir",
        "43" to "Yaoi",
        "36" to "Yaşamdan Kesitler",
        "44" to "Yuri",
    )

    // Ana Sayfa Yükleme
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document =
            app.post("${mainUrl}/tavsiyeRobotuResult",
            headers = mapOf(
                "X-CSRF-TOKEN" to "qi5SteotzukwLBcWpAQeka3IaqD3Bj2tpL547aEW",
                "X-Requested-With" to "XMLHttpRequest",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0"),
            data = mapOf(
                "kategoriler%5B%5D" to request.data
            ),
            cookies = mapOf(
                "adcash_shown_count2" to "3",
                "XSRF-TOKEN" to "eyJpdiI6IlNRNkVZNmtoVm9CTkRZT0hBM0NsT0E9PSIsInZhbHVlIjoiUWlPZjFERkpINFZYZnNNMm5sc3hHZG1BZDNxS1U4dGFGMkUxWEdPNkE2NVpaT0ZJZW9JZ1U4Z3h1QzJMWFVqRzB2endzb1R5cmpMUFFNcUNITEtvUjA0Q2Rzbll4K2xkTjBhb3pmOWozeDhETUhxSk9JVkpoNGxqZlY0NGk4ZzAiLCJtYWMiOiI3M2U0MzllNDZiOTJmZTc0OTE2OTk1YzJiODU0NzBiNWJiYWFmOWY4NjdkYTIyNzIzNWZiOTNmY2ExMjUwYWRlIn0",
                "anizm_session" to "eyJpdiI6Im1qc1wvdzViWVp5dVc4M3hqYVo5dU9BPT0iLCJ2YWx1ZSI6InNmVkNmTjFrZnFHZyttUXdTc2NJS2NcL0NCRFE2MlFLUlVvc2F0RmVUU3c1N2poc21QUUxLbmRFNTFLalJVa1AwTmlLSmZTTnhwbXI3STNwZ1wvSnE2d3lCbWFweUFQTkNLYWN4ZGFXRWhGM1dESXUrdUFFK1pjVzlheXJNQnBHVEwiLCJtYWMiOiIzNGRkMmU3NGMxODUxOTU3MDkzNmQ2YzFkZmI0OTI2NDg3YjYzZjdlMGViZjllYjk5ODIwMzM4NjEwOGJkNzI3In0="
                )
        ).document
//        Log.d("Anizm","document = $document")

        val home = document.select("div.aramaSonucItem").mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(request.name, home, hasNext = false)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a.titleLink")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a.imgWrapperLink")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
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

        return newAnimeLoadResponse(title, url, TvType.Anime, true) {
            this.posterUrl = poster
            this.episodes = mutableMapOf(DubStatus.Subbed to episodes)
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
                    loadExtractor(
                        url = url,
                        referer = mainUrl,
                        subtitleCallback = subtitleCallback,
                        callback = callback
                    )
                }
        return true
    }
}