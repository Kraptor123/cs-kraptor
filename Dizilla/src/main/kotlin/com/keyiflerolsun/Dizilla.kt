// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Base64
import android.util.Log
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.*
import kotlin.coroutines.cancellation.CancellationException


class Dizilla : MainAPI() {
    override var mainUrl = "https://dizilla.club"
    override var name = "Dizilla"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.TvSeries)


    override val mainPage = mainPageOf(
        //"${mainUrl}/tum-bolumler" to "Altyazılı Bölümler",
        "${mainUrl}/arsiv" to "Yeni Eklenen Diziler",
        "${mainUrl}/dizi-turu/aile" to "Aile",
        "${mainUrl}/dizi-turu/aksiyon" to "Aksiyon",
        "${mainUrl}/dizi-turu/bilim-kurgu" to "Bilim Kurgu",
        "${mainUrl}/dizi-turu/dram" to "Dram",
        "${mainUrl}/dizi-turu/fantastik" to "Fantastik",
        "${mainUrl}/dizi-turu/gerilim" to "Gerilim",
        "${mainUrl}/dizi-turu/komedi" to "Komedi",
        "${mainUrl}/dizi-turu/korku" to "Korku",
        "${mainUrl}/dizi-turu/macera" to "Macera",
        "${mainUrl}/dizi-turu/romantik" to "Romantik",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        var document = app.get(request.data).document
        val home = if (request.data.contains("dizi-turu")) {
            document.select("span.watchlistitem-").mapNotNull { it.diziler() }
        } else if (request.data.contains("/arsiv")) {
            val yil = Calendar.getInstance().get(Calendar.YEAR)
            val sayfa = "?page=sayi&tab=1&sort=date_desc&filterType=2&imdbMin=5&imdbMax=10&yearMin=1900&yearMax=$yil"
            val replace = sayfa.replace("sayi", page.toString())
            document = app.get("${request.data}${replace}").document
            document.select("a.w-full").mapNotNull { it.yeniEklenenler() }
        } else {
            document.select("div.col-span-3 a").mapNotNull { it.sonBolumler() }
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.diziler(): SearchResponse {
        val title = this.selectFirst("span.font-normal")?.text() ?: "return null"
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: "return null"
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    private fun Element.yeniEklenenler(): SearchResponse {
        val title = this.selectFirst("h2")?.text() ?: "return null"
        val href = fixUrlNull(this.attr("href")) ?: "return null"
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }
    private suspend fun Element.sonBolumler(): SearchResponse {
        val name = this.selectFirst("h2")?.text() ?: ""
        val epName = this.selectFirst("div.opacity-80")!!.text().replace(". Sezon ", "x")
            .replace(". Bölüm", "")

        val title = "$name - $epName"

        val epDoc = fixUrlNull(this.attr("href"))?.let { app.get(it).document }

        val href = fixUrlNull(epDoc?.selectFirst("div.poster a")?.attr("href")) ?: "return null"

        val posterUrl = fixUrlNull(epDoc?.selectFirst("div.poster img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    private fun SearchItem.toSearchResponse(): SearchResponse? {
        return newTvSeriesSearchResponse(
            title ?: return null,
            "${mainUrl}/${slug}",
            TvType.TvSeries,
        ) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return try {
            val response = app.post(
                "${mainUrl}/api/bg/searchcontent?searchterm=$query",
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0",
                    "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0",
                    "Accept" to "application/json, text/plain, */*",
                    "Accept-Language" to "en-US,en;q=0.5",
                    "X-Requested-With" to "XMLHttpRequest",
                    "Sec-Fetch-Site" to "same-origin",
                    "Sec-Fetch-Mode" to "cors",
                    "Sec-Fetch-Dest" to "empty",
                    "Referer" to "${mainUrl}/"
                ),
                referer = "${mainUrl}/"
            )
            val responseBody = response.body.string()
            val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val searchResult: SearchResult = objectMapper.readValue(responseBody)
            val decodedSearch = base64Decode(searchResult.response.toString())
            val contentJson: SearchData = objectMapper.readValue(decodedSearch)
            if (contentJson.state != true) {
                throw ErrorLoadingException("Invalid Json response")
            }
            val results = mutableListOf<SearchResponse>()
            contentJson.result?.forEach {
                val name = it.title.toString()
                val link = fixUrl(it.slug.toString())
                val posterLink = it.poster.toString()
                results.add(newTvSeriesSearchResponse(name, link, TvType.TvSeries) {
                    this.posterUrl = posterLink
                })
            }
            results
        } catch (e: Exception) {
            Log.e("Dizilla", "Search error: ${e.message}")
            emptyList()
        }
    }

    private fun toSearchResponse(ad: String, link: String, posterLink: String): SearchResponse {
        return newTvSeriesSearchResponse(
            ad,
            link,
            TvType.TvSeries,
        ) {
            this.posterUrl = posterLink
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        try {
            val response = app.get(url)
            val bodyString = response.body.string()
            val document = org.jsoup.Jsoup.parse(bodyString)

            val titleElement = document.selectFirst("div.poster.poster h2")
            if (titleElement == null) {
                return null
            }
            val title = titleElement.ownText()

            val posterElement = document.selectFirst("div.w-full.page-top.relative img")
            val poster = fixUrlNull(posterElement?.attr("src"))
            val yearElements = document.select("div.w-fit.min-w-fit")
            var year: Int? = null
            if (yearElements.size > 1) {
                val yearText = yearElements.getOrNull(1)
                    ?.selectFirst("span.text-sm.opacity-60")
                    ?.ownText()
                year = yearText
                    ?.split(" ")
                    ?.lastOrNull()
                    ?.toIntOrNull()
            } else {
            }

            val description = document.selectFirst("div.mt-2.text-sm")?.ownText()?.trim()

            val tagsText = document.selectFirst("div.poster.poster h3")?.ownText()
            val tags = tagsText?.split(",")?.map { it.trim() }

            val ratingText = document.selectFirst("div.flex.items-center")
                ?.selectFirst("span.text-white.text-sm")
                ?.ownText()
                ?.trim()
            val rating = ratingText.toRatingInt()

            val actorsElements = document.select("div.global-box h5")
            val actors = actorsElements.map { Actor(it.ownText()) }

            val episodeses = mutableListOf<Episode>()
            val seasonElements = document.select("div.flex.items-center.flex-wrap.gap-2.mb-4 a")
            for (sezon in seasonElements) {
                val sezonHref = fixUrl(sezon.attr("href"))
                val sezonResponse = app.get(sezonHref)
                val sezonBody = sezonResponse.body.string()
                val sezonDoc = org.jsoup.Jsoup.parse(sezonBody)
                val split = sezonHref.split("-")
                val season = split.getOrNull(split.size - 2)?.toIntOrNull()
                val episodesContainer = sezonDoc.select("div.episodes")
                for (bolum in episodesContainer.select("div.cursor-pointer")) {
                    val linkElements = bolum.select("a")
                    if (linkElements.isEmpty()) {
                        continue
                    }
                    val epName = linkElements.last()?.ownText() ?: continue
                    val epHref = fixUrlNull(linkElements.last()?.attr("href")) ?: continue
                    val epEpisode = bolum.selectFirst("a")?.ownText()?.trim()?.toIntOrNull()
                    val newEpisode = newEpisode(epHref) {
                        this.name = epName
                        this.season = season
                        this.episode = epEpisode
                    }
                    episodeses.add(newEpisode)
                }
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeses) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                addActors(actors)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        try {
            val response = app.get(data)
            val bodyString = response.body.string()
            val document = Jsoup.parse(bodyString)

            val scriptElement = document.selectFirst("script#__NEXT_DATA__")
            if (scriptElement == null) {
                Log.e("Dizilla", "__NEXT_DATA__ script bulunamadı")
                return false
            }
            val script = scriptElement.data()

            val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val rootNode = objectMapper.readTree(script)
            val secureDataNode = rootNode.get("props")?.get("pageProps")?.get("secureData")
            if (secureDataNode == null) {
                return false
            }

            val secureDataString = secureDataNode.toString().replace("\"", "")
            val decodedData = try {
                Base64.decode(secureDataString, Base64.DEFAULT).toString(Charsets.UTF_8)
            } catch (e: Exception) {
                return false
            }

            val decodedJson = objectMapper.readTree(decodedData)
            val sourceNode = decodedJson.get("RelatedResults")?.get("getEpisodeSources")?.get("result")?.get(0)?.get("source_content")
            if (sourceNode == null) {
                Log.e("Dizilla", "source_content bulunamadı")
                return false
            }
            val source = sourceNode.toString().replace("\"", "").replace("\\", "")
            val iframe = fixUrlNull(Jsoup.parse(source).select("iframe").attr("src"))
            if (iframe == null) {
                Log.e("Dizilla", "Iframe URL bulunamadı")
                return false
            }

            loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
            return true

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        }
    }
}