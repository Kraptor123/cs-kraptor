// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import java.nio.charset.Charset
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.extractors.*
import com.lagradost.cloudstream3.utils.loadExtractor

class AsyaAnimeleri : MainAPI() {
    override var mainUrl = "https://asyaanimeleri.top"
    override var name = "AsyaAnimeleri"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Anime)

    override val mainPage = mainPageOf(
        "${mainUrl}/series/?page=sayfa&status=&type=&order=update" to "Son Eklenenler",
        "${mainUrl}/series/?page=sayfa&status=&type=&order=popular" to "Popüler",
        "${mainUrl}/genres/aksiyon/page/sayfa/" to "Aksiyon",
        "${mainUrl}/genres/askeri/page/sayfa/" to "Askeri",
        "${mainUrl}/genres/bilim-kurgu/page/sayfa/" to "Bilim-kurgu",
        "${mainUrl}/genres/buyu/page/sayfa/" to "Buyu",
        "${mainUrl}/genres/comic/page/sayfa/" to "Comic",
        "${mainUrl}/genres/dedektif/page/sayfa/" to "Dedektif",
        "${mainUrl}/genres/dogaustu/page/sayfa/" to "Dogaustu",
        "${mainUrl}/genres/dovus-sanatlari/page/sayfa/" to "Dovus-Sanatlari",
        "${mainUrl}/genres/dram/page/sayfa/" to "Dram",
        "${mainUrl}/genres/ecchi/page/sayfa/" to "Ecchi",
        "${mainUrl}/genres/fantastik/page/sayfa/" to "Fantastik",
        "${mainUrl}/genres/gerilim/page/sayfa/" to "Gerilim",
        "${mainUrl}/genres/gizem/page/sayfa/" to "Gizem",
        "${mainUrl}/genres/harem/page/sayfa/" to "Harem",
        "${mainUrl}/genres/hayat-dilimi/page/sayfa/" to "Hayat-dilimi",
        "${mainUrl}/genres/isekai/page/sayfa/" to "İsekai",
        "${mainUrl}/genres/josei/page/sayfa/" to "Josei",
        "${mainUrl}/genres/komedi/page/sayfa/" to "Komedi",
        "${mainUrl}/genres/korku/page/sayfa/" to "Korku",
        "${mainUrl}/genres/macera/page/sayfa/" to "Macera",
        "${mainUrl}/genres/mecha/page/sayfa/" to "Mecha",
        "${mainUrl}/genres/mitoloji/page/sayfa/" to "Mitoloji",
        "${mainUrl}/genres/muzik/page/sayfa/" to "Muzik",
        "${mainUrl}/genres/okul/page/sayfa/" to "Okul",
        "${mainUrl}/genres/oyun/page/sayfa/" to "Oyun",
        "${mainUrl}/genres/parodi/page/sayfa/" to "Parodi",
        "${mainUrl}/genres/psikolojik/page/sayfa/" to "Psikolojik",
        "${mainUrl}/genres/reenkarnasyon/page/sayfa/" to "Reenkarnasyon",
        "${mainUrl}/genres/romantik/page/sayfa/" to "Romantik",
        "${mainUrl}/genres/romantizm/page/sayfa/" to "Romantizm",
        "${mainUrl}/genres/samuray/page/sayfa/" to "Samuray",
        "${mainUrl}/genres/seinen/page/sayfa/" to "Seinen",
        "${mainUrl}/genres/seytanlar/page/sayfa/" to "Seytanlar",
        "${mainUrl}/genres/shoujo/page/sayfa/" to "Shoujo",
        "${mainUrl}/genres/shounen/page/sayfa/" to "Shounen",
        "${mainUrl}/genres/spor/page/sayfa/" to "Spor",
        "${mainUrl}/genres/super-guc/page/sayfa/" to "Super-guc",
        "${mainUrl}/genres/tarihi/page/sayfa/" to "Tarihi",
        "${mainUrl}/genres/tarihsel/page/sayfa/" to "Tarihsel",
        "${mainUrl}/genres/ters-harem/page/sayfa/" to "Ters-harem",
        "${mainUrl}/genres/uzay/page/sayfa/" to "Uzay",
        "${mainUrl}/genres/vahset/page/sayfa/" to "Vahset",
        "${mainUrl}/genres/vampir/page/sayfa/" to "Vampir",
        "${mainUrl}/genres/yaris/page/sayfa/" to "Yaris",
        "${mainUrl}/genres/zamanda-yolculuk/page/sayfa/" to "Zamanda yolculuk"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) {
            // İlk sayfa için "page/sayfa" kısmını sil
            request.data.replace("/page/sayfa", "").replace("sayfa", "$page")
        } else {
            // Diğer sayfalar için sayfa numarasını koy
            request.data.replace("sayfa", "$page")
        }

        val document = app.get(url).document
        val home = document.select("article.bs").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.tt.tts h2")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newAnimeSearchResponse(title, href, TvType.Anime) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("https://asyaanimeleri.top/series/list-mode/").document

        val elements = document.select("div.soralist li a")

        return elements.mapNotNull { element ->
            val title = element.text().trim()
            val url = element.absUrl("href").trim()

            if (title.contains(query, ignoreCase = true)) {
                val detailDoc = app.get(url).document
                val posterUrl = detailDoc.selectFirst("div.thumb img")?.absUrl("src") // örnek selector

                newAnimeSearchResponse(
                    name = title,
                    url = url,
                ).apply {
                    this.posterUrl = posterUrl
                }
            } else null
        }
    }

//    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("div.infox h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.thumb img")?.attr("src"))
        val description = document.selectFirst("div.entry-content b")?.text()?.trim()
        val year = document.selectFirst("span.split:nth-child(3)")?.text()?.trim()?.toIntOrNull()
        val tags = document.select(".spe > span:nth-child(7)").map { it.text() }
        val rating = document.selectFirst("div.rating")?.text()?.trim()
        val trailer = document.selectFirst("a.trailerbutton")?.attr("href")
    ?.takeIf { it.contains("youtube.com/watch") }
    ?.replace("watch?v=", "embed/")

        val duration =
            document.selectFirst(".spe > span:nth-child(4)")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("article.bs").mapNotNull { it.toRecommendationResult() }
        val episodeList = document.select("div.eplister ul li").mapNotNull { episodeElement ->
            val episodeLink = episodeElement.selectFirst("a") ?: return@mapNotNull null
            val epHref = fixUrlNull(episodeLink.attr("href")) ?: return@mapNotNull null

            val epNumber = episodeElement.selectFirst("div.epl-num")?.text()?.trim()
            val rawEpTitle = episodeElement.selectFirst("div.epl-title")?.text()?.trim() ?: ""

            // Tüm gereksiz kısımları temizleme
            val cleanedTitle = rawEpTitle
                .replaceFirst(Regex("(?i)${Regex.escape(title)}[\\s.:-]*"), "") // Anime adını sil
                .replace(Regex("""(?i)\s*-\s*izle"""), "") // Tüm "izle" varyasyonlarını kaldır
                .replace(Regex("""^\d+[\s.:-]*"""), "") // Baştaki sayıları ve işaretleri sil (Örn: "1.")
                .replace(Regex("""[.:-]\s*$"""), "") // Sondaki nokta/tireleri temizle
                .trim()
                .replace(Regex("""\s+"""), " ") // Çoklu boşlukları tek boşluk yap

            // Numarayı epNumber'dan alıp temiz başlık oluşturma
            val finalEpTitle = when {
                cleanedTitle.isNotEmpty() -> cleanedTitle
                else -> "Bölüm ${epNumber?.replace(Regex("[^0-9.]"), "") ?: "?"}"
            }.replace(Regex("""\s{2,}"""), " ") // Çift boşlukları temizle

            newEpisode(epHref) {
                this.name = finalEpTitle
                this.episode = epNumber
                    ?.replace(Regex("[^0-9.]"), "")
                    ?.split(".")?.firstOrNull()
                    ?.toIntOrNull()
            }
        }.let { list ->
            mutableMapOf(DubStatus.Subbed to list)
        }

        Log.d("aanime", "episodetitle = ${document.selectFirst("div.epl-title")}")


        return newAnimeLoadResponse(title, url, TvType.Anime, true) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(rating)
            this.duration = duration
            addTrailer(trailer)
            this.recommendations = recommendations
            this.episodes = episodeList
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title = this.selectFirst("div.tt.tts")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a.tip")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newAnimeSearchResponse(title, href, TvType.Anime) { this.posterUrl = posterUrl }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // Player seçeneklerini al
        val options = document.select("select.mirror option:not(:first-child)")

        options.forEach { option ->
            try {
                val encodedHtml = option.attr("value")
                if (encodedHtml.isBlank()) return@forEach

                // Base64 decode
                val decodedHtml = Base64.decode(encodedHtml).toString(Charset.defaultCharset())

                // Iframe src çek
                val iframeSrc = Regex("""src=["'](.*?)["']""").find(decodedHtml)?.groupValues?.get(1)
                    ?: return@forEach

                // URL'yi temizle
                val cleanUrl = iframeSrc
                    .replace(Regex("""^//"""), "https://")
                    .replace(Regex("""\\/"""), "/")
                    .let { fixUrl(it) }

                Log.d("aanime","cleanurl $cleanUrl")

                loadExtractor(
                    url = cleanUrl,
                    referer = "$mainUrl/",
                    subtitleCallback = subtitleCallback,
                    callback = callback
                )

            } catch (e: Exception) {
            }
        }
        return true
    }
}