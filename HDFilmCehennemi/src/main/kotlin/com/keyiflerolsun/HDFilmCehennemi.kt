// ! https://github.com/hexated/cloudstream-extensions-hexated/blob/master/Hdfilmcehennemi/src/main/kotlin/com/hexated/Hdfilmcehennemi.kt

package com.keyiflerolsun

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class HDFilmCehennemi : MainAPI() {
    override var mainUrl              = "https://www.hdfilmcehennemi.nl"
    override var name                 = "HDFilmCehennemi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = true
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Kategorilerin isimleri ve bağlantı yollarını içeren liste
        val kategoriler = listOf(
            Pair("Yeni Eklenen Diziler", "yabancidiziizle-2"),
            Pair("Tavsiye Filmler", "category/tavsiye-filmler-izle2"),
            Pair("IMDB 7+ Filmler", "imdb-7-puan-uzeri-filmler"),
            Pair("En Çok Yorumlananlar", "en-cok-yorumlananlar-1"),
            Pair("En Çok Beğenilenler", "en-cok-begenilen-filmleri-izle"),
            Pair("Aile Filmleri", "tur/aile-filmleri-izleyin-6"),
            Pair("Aksiyon Filmleri", "tur/aksiyon-filmleri-izleyin-3"),
            Pair("Animasyon Filmleri", "tur/animasyon-filmlerini-izleyin-4"),
            Pair("Belgesel Filmleri", "tur/belgesel-filmlerini-izle-1"),
            Pair("Bilim Kurgu Filmleri", "tur/bilim-kurgu-filmlerini-izleyin-2"),
            Pair("Komedi Filmleri", "tur/komedi-filmlerini-izleyin-1"),
            Pair("Korku Filmleri", "tur/korku-filmlerini-izle-2"),
            Pair("Romantik Filmleri", "tur/romantik-filmleri-izle-1")
        )

        // Tüm kategorilerden elde edilecek sonuçları tutacak liste
        val allHomeResults = mutableListOf<SearchResponse>()

        // Ana sayfa HTML verisini genel bir istekle çekiyoruz (opsiyonel: farklı bir URL'den çekmek istiyorsanız burayı ayarlayabilirsiniz)
        val mainDocument = app.get(request.data).document

        // Her kategori için işlem yapıyoruz
        kategoriler.forEach { kategori ->
            val (kategoriAdi, kategoriPath) = kategori

            // İlgili kategori adının mainDocument içinde bulunup bulunmadığını kontrol ediyoruz
            if (mainDocument.text().contains(kategoriAdi)) {
                // Eğer kategori adı mevcutsa, JSON verisini çekmek için özel referer ile yeni istek yapıyoruz
                val refererHeaders = mapOf(
                    "Host" to "www.hdfilmcehennemi.nl",
                    "Referer" to "$mainUrl/load/page/$page/$kategoriPath/",
                    "X-Requested-With" to "fetch",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0"
                )

                // JSON verisi içeren yanıtı alıyoruz
                val jsonResponse = app.get(request.data, headers = refererHeaders).text
                Log.d("HDCH", "JSON yanıtı ($kategoriAdi): $jsonResponse")

                // JSON yanıtını işleyerek SearchResponse listesine çeviren fonksiyonu çağırıyoruz
                val jsonResults: List<SearchResponse> = parseJsonToSearchResults(jsonResponse)
                allHomeResults.addAll(jsonResults)
            } else {
                // Eğer kategori adı mainDocument'de yoksa, HTML'den normal olarak veri çekiyoruz
                val htmlResults = mainDocument.select("div.section-content a.poster")
                    .mapNotNull { it.toSearchResult() }
                allHomeResults.addAll(htmlResults)
            }
        }

        // Toplanan tüm sonuçlarla yeni HomePageResponse oluşturuyoruz
        return newHomePageResponse(request.name, allHomeResults)
    }

    /**
     * JSON yanıtını işleyip SearchResponse listesine çeviren örnek fonksiyon.
     * Buradaki dönüşüm mantığını JSON yapısına göre kendiniz uyarlamanız gerekmektedir.
     */
    private fun parseJsonToSearchResults(json: String): List<SearchResponse> {
        // Örnek dönüşüm kodu:
        val results = mutableListOf<SearchResponse>()
        try {
            // JSON kütüphanesi kullanarak parse etmeniz gerekebilir (örneğin: kotlinx.serialization, Gson vb.)
            // Bu örnekte parse edilen nesneleri SearchResponse'a dönüştürdüğünüz varsayılmaktadır.
            // Örnek:
            // val jsonObject = JSONObject(json)
            // val itemsArray = jsonObject.getJSONArray("items")
            // for (i in 0 until itemsArray.length()) {
            //     val item = itemsArray.getJSONObject(i)
            //     results.add(item.toSearchResponse())
            // }
        } catch (e: Exception) {
            Log.e("HDCH", "JSON parse hatası: ${e.message}")
        }
        return results
    }


    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("strong.poster-title")?.text() ?: return null
        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val response      = app.get(
            "${mainUrl}/search?q=${query}",
            headers = mapOf("X-Requested-With" to "fetch")
        ).parsedSafe<Results>() ?: return emptyList()
        val searchResults = mutableListOf<SearchResponse>()

        response.results.forEach { resultHtml ->
            val document = Jsoup.parse(resultHtml)

            val title     = document.selectFirst("h4.title")?.text() ?: return@forEach
            val href      = fixUrlNull(document.selectFirst("a")?.attr("href")) ?: return@forEach
            val posterUrl = fixUrlNull(document.selectFirst("img")?.attr("src")) ?: fixUrlNull(document.selectFirst("img")?.attr("data-src"))

            searchResults.add(
                newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl?.replace("/thumb/", "/list/") }
            )
        }

        return searchResults
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title       = document.selectFirst("h1.section-title")?.text()?.substringBefore(" izle") ?: return null
        val poster      = fixUrlNull(document.select("aside.post-info-poster img.lazyload").lastOrNull()?.attr("data-src"))
        val tags        = document.select("div.post-info-genres a").map { it.text() }
        val year        = document.selectFirst("div.post-info-year-country a")?.text()?.trim()?.toIntOrNull()
        val tvType      = if (document.select("div.seasons").isEmpty()) TvType.Movie else TvType.TvSeries
        val description = document.selectFirst("article.post-info-content > p")?.text()?.trim()
        val rating      = document.selectFirst("div.post-info-imdb-rating span")?.text()?.substringBefore("(")?.trim()?.toRatingInt()
        val actors      = document.select("div.post-info-cast a").map {
            Actor(it.selectFirst("strong")!!.text(), it.select("img").attr("data-src"))
        }

        val recommendations = document.select("div.section-slider-container div.slider-slide").mapNotNull {
            val recName      = it.selectFirst("a")?.attr("title") ?: return@mapNotNull null
            val recHref      = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val recPosterUrl = fixUrlNull(it.selectFirst("img")?.attr("data-src")) ?: fixUrlNull(it.selectFirst("img")?.attr("src"))

            newTvSeriesSearchResponse(recName, recHref, TvType.TvSeries) {
                this.posterUrl = recPosterUrl
            }
        }

        return if (tvType == TvType.TvSeries) {
            val trailer  = document.selectFirst("div.post-info-trailer button")?.attr("data-modal")?.substringAfter("trailer/")?.let { "https://www.youtube.com/embed/$it" }
            val episodes = document.select("div.seasons-tab-content a").mapNotNull {
                val epName    = it.selectFirst("h4")?.text()?.trim() ?: return@mapNotNull null
                val epHref    = fixUrlNull(it.attr("href")) ?: return@mapNotNull null
                val epEpisode = Regex("""(\d+)\. ?Bölüm""").find(epName)?.groupValues?.get(1)?.toIntOrNull()
                val epSeason  = Regex("""(\d+)\. ?Sezon""").find(epName)?.groupValues?.get(1)?.toIntOrNull() ?: 1

                newEpisode(epHref) {
                    this.name = epName
                    this.season = epSeason
                    this.episode = epEpisode
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl       = poster
                this.year            = year
                this.plot            = description
                this.tags            = tags
                this.rating          = rating
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            val trailer = document.selectFirst("div.post-info-trailer button")?.attr("data-modal")?.substringAfter("trailer/")?.let { "https://www.youtube.com/embed/$it" }

            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl       = poster
                this.year            = year
                this.plot            = description
                this.tags            = tags
                this.rating          = rating
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    private suspend fun invokeLocalSource(source: String, url: String, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit ) {
        val script    = app.get(url, referer = "${mainUrl}/").document.select("script").find { it.data().contains("sources:") }?.data() ?: return
        val videoData = getAndUnpack(script).substringAfter("file_link=\"").substringBefore("\";")
        val subData   = script.substringAfter("tracks: [").substringBefore("]")

        callback.invoke(
            newExtractorLink(
                source  = source,
                name    = source,
                url     = base64Decode(videoData),
                type    = INFER_TYPE
            ) {
                headers = mapOf("Referer" to "${mainUrl}/") // "Referer" ayarı burada yapılabilir
                quality = getQualityFromName(Qualities.Unknown.value.toString())
            }
        )

        AppUtils.tryParseJson<List<SubSource>>("[${subData}]")?.filter { it.kind == "captions" }?.map {
            subtitleCallback.invoke(
                SubtitleFile(it.label.toString(), fixUrl(it.file.toString()))
            )
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit ): Boolean {
        Log.d("HDCH", "data » $data")
        val document = app.get(data).document

        document.select("div.alternative-links").map { element ->
            element to element.attr("data-lang").uppercase()
        }.forEach { (element, langCode) ->
            element.select("button.alternative-link").map { button ->
                button.text().replace("(HDrip Xbet)", "").trim() + " $langCode" to button.attr("data-video")
            }.forEach { (source, videoID) ->
                val apiGet = app.get(
                    "${mainUrl}/video/$videoID/",
                    headers = mapOf(
                        "Content-Type"     to "application/json",
                        "X-Requested-With" to "fetch"
                    ),
                    referer = data
                ).text

                var iframe = Regex("""data-src=\\"([^"]+)""").find(apiGet)?.groupValues?.get(1)!!.replace("\\", "")
                if (iframe.contains("?rapidrame_id=")) {
                    iframe = "${mainUrl}/playerr/" + iframe.substringAfter("?rapidrame_id=")
                }

                Log.d("HDCH", "$source » $videoID » $iframe")
                invokeLocalSource(source, iframe, subtitleCallback, callback)
            }
        }

        return true
    }

    private data class SubSource(
        @JsonProperty("file")  val file: String?  = null,
        @JsonProperty("label") val label: String? = null,
        @JsonProperty("kind")  val kind: String?  = null
    )

    data class Results(
        @JsonProperty("results") val results: List<String> = arrayListOf()
    )
}