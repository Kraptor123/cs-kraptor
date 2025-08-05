// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.util.*
import kotlinx.coroutines.*

class BelgeselX : MainAPI() {
    override var mainUrl = "https://belgeselx.com"
    override var name = "BelgeselX"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Documentary)

    override val mainPage = mainPageOf(
        "${mainUrl}/konu/turk-tarihi-belgeselleri&page=" to "Türk Tarihi",
        "${mainUrl}/konu/tarih-belgeselleri&page=" to "Tarih",
        "${mainUrl}/konu/seyehat-belgeselleri&page=" to "Seyahat",
        "${mainUrl}/konu/seri-belgeseller&page=" to "Seri",
        "${mainUrl}/konu/savas-belgeselleri&page=" to "Savaş",
        "${mainUrl}/konu/sanat-belgeselleri&page=" to "Sanat",
        "${mainUrl}/konu/psikoloji-belgeselleri&page=" to "Psikoloji",
        "${mainUrl}/konu/polisiye-belgeselleri&page=" to "Polisiye",
        "${mainUrl}/konu/otomobil-belgeselleri&page=" to "Otomobil",
        "${mainUrl}/konu/nazi-belgeselleri&page=" to "Nazi",
        "${mainUrl}/konu/muhendislik-belgeselleri&page=" to "Mühendislik",
        "${mainUrl}/konu/kultur-din-belgeselleri&page=" to "Kültür Din",
        "${mainUrl}/konu/kozmik-belgeseller&page=" to "Kozmik",
        "${mainUrl}/konu/hayvan-belgeselleri&page=" to "Hayvan",
        "${mainUrl}/konu/eski-tarih-belgeselleri&page=" to "Eski Tarih",
        "${mainUrl}/konu/egitim-belgeselleri&page=" to "Eğitim",
        "${mainUrl}/konu/dunya-belgeselleri&page=" to "Dünya",
        "${mainUrl}/konu/doga-belgeselleri&page=" to "Doğa",
        "${mainUrl}/konu/bilim-belgeselleri&page=" to "Bilim"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home = document.select("div.gen-movie-contain").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun String.toTitleCase(): String {
        val locale = Locale("tr", "TR")
        return this.split(" ").joinToString(" ") { word ->
            word.lowercase(locale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3 a")?.text()?.trim()?.toTitleCase() ?: return null
        val href = fixUrlNull(this.selectFirst("h3 a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src")?.trim())

        return newTvSeriesSearchResponse(title, href, TvType.Documentary) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val cx = "016376594590146270301:iwmy65ijgrm" // ! Might change in the future

        val tokenResponse = app.get("https://cse.google.com/cse.js?cx=${cx}")
        val cseLibVersion = Regex("""cselibVersion": "(.*)"""").find(tokenResponse.text)?.groupValues?.get(1)
        val cseToken = Regex("""cse_token": "(.*)"""").find(tokenResponse.text)?.groupValues?.get(1)

        val response =
            app.get("https://cse.google.com/cse/element/v1?rsz=filtered_cse&num=100&hl=tr&source=gcsc&cselibv=${cseLibVersion}&cx=${cx}&q=${query}&safe=off&cse_tok=${cseToken}&oq=${query}&callback=google.search.cse.api9969&rurl=https%3A%2F%2Fbelgeselx.com%2F")
        Log.d("BLX", "Search result: ${response.text}")

        val titles = Regex(""""titleNoFormatting": "(.*)"""").findAll(response.text).map { it.groupValues[1] }.toList()
        val urls = Regex(""""url": "(.*)"""").findAll(response.text).map { it.groupValues[1] }.toList()
        val posterUrls = Regex(""""ogImage": "(.*)"""").findAll(response.text)
            .map { it.groupValues[1].trim() }  // Trim ekleniyor
            .toList()

        val searchResponses = mutableListOf<TvSeriesSearchResponse>()

        for (i in titles.indices) {
            val title = titles[i].split("İzle")[0].trim().toTitleCase()
            val url = urls.getOrNull(i) ?: continue
            val posterUrl = posterUrls.getOrNull(i) ?: continue

            if (!url.contains("belgeseldizi")) continue
            searchResponses.add(newTvSeriesSearchResponse(title, url, TvType.Documentary) {
                this.posterUrl = posterUrl
            })
        }

        return searchResponses
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h2.gen-title")?.text()?.trim()?.toTitleCase() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.gen-tv-show-top img")?.attr("src")?.trim()) ?: return null
        val description = document.selectFirst("div.gen-single-tv-show-info p")?.text()?.trim()
        val tags = document.select("div.gen-socail-share a[href*='belgeselkanali']")
            .map { it.attr("href").split("/").last().replace("-", " ").toTitleCase() }

        val episodes = document.select("div.gen-movie-contain").mapNotNull {
            val epName = it.selectFirst("div.gen-movie-info h3 a")?.text()?.trim() ?: return@mapNotNull null
            val epHref = fixUrlNull(it.selectFirst("div.gen-movie-info h3 a")?.attr("href")) ?: return@mapNotNull null
            val epId   = it.selectFirst("h3 a")?.attr("id").toString()
            Log.d("kraptor_$name","epId = $epId")
            val epEpisode = it.selectFirst("div.gen-single-meta-holder li:nth-child(1)")?.text()?.substringAfterLast(" ")
                ?.trim()?.toIntOrNull()
            val epSeason = it.selectFirst("div.gen-single-meta-holder li:nth-child(1)")?.text()?.substringBefore(", ")
                ?.substringAfter(" ")
                ?.trim()?.toIntOrNull()


            newEpisode("$epHref|$epId") {
                this.name = epName
                this.season = epSeason
                this.episode = epEpisode
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.Documentary, episodes) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val gercekUrl = data.substringBefore("|")
        val vidId = data.substringAfter("|")
        val number = vidId
        Log.d("kraptor_$name","number = $number")

        val videoUrls = listOf(
            "https://belgeselx.com/video/data/new1.php?id=$number",
            "https://belgeselx.com/video/data/new2.php?id=$number",
            "https://belgeselx.com/video/data/new3.php?id=$number",
            "https://belgeselx.com/video/data/new4.php?id=$number",
            "https://belgeselx.com/video/data/new5.php?id=$number"
        )

        val headers = mapOf(
            "Host" to "belgeselx.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
            "Connection" to "keep-alive",
            "Referer" to data
        )

        // Compile regex once
        val linkRegex = Regex("""file\s*:\s*"([^"]+)"""")
        val labelRegex = Regex("""label\s*:\s*"([^"]+)"""")

        val qualityMap = mapOf(
            "FULL" to Qualities.P1080.value,
            "720p" to Qualities.P720.value,
            "480p" to Qualities.P480.value,
            "360p" to Qualities.P360.value
        )

        coroutineScope {
         videoUrls.map { url ->
                launch(Dispatchers.IO) {
                    try {
                        val html = withTimeout(3_000) {
                            app.get(url, headers).document.html()
                        }
                        val links = linkRegex.findAll(html).map { it.groupValues[1] }.toList()
                        val labels = labelRegex.findAll(html).map { it.groupValues[1] }.toList()


                        links.forEachIndexed { i, linkUrl ->
                            val labelText = labels.getOrNull(i) ?: ""
                            val kaynakIsim = if (linkUrl.contains("googleusercontent")){
                                "Google"
                            } else {
                                "BelgeselX"
                            }
                            Log.d("kraptor_$name","kaynakIsim = $kaynakIsim $linkUrl")

                            val qualityInt = qualityMap[labelText] ?: Qualities.Unknown.value

                          callback.invoke(newExtractorLink(
                                source = kaynakIsim,
                                name = kaynakIsim,
                                url = linkUrl,
                                type = INFER_TYPE,
                                {
                                this.quality = qualityInt
                            }
                          ))
                        }
                    } catch (_: Exception) { /* skip */
                    }
                 }
                }
            }
        return true
    }
}