// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll

class TrDiziIzle : MainAPI() {
    override var mainUrl = "https://www.trdiziizle.vip/"
    override var name = "TrDiziIzle"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.TvSeries)

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse = coroutineScope {
        if (page != 1) return@coroutineScope newHomePageResponse(emptyList())

        val kategoriler = listOf(
            "" to "Tüm Diziler", "Program" to "Programlar", "aile" to "Aile", "aksiyon" to "Aksiyon", "animasyon" to "Animasyon", "anime" to "Anime",
            "belgesel" to "Belgesel", "bilim" to "Bilim Kurgu", "biyografi" to "Biyografi", "dram" to "Dram",
            "fantastik" to "Fantastik", "genclik" to "Gençlik", "gerilim" to "Gerilim", "gizem" to "Gizem",
            "komedi" to "Komedi", "korku" to "Korku", "macera" to "Macera", "muzikal" to "Müzikal",
            "polisiye" to "Polisiye", "romantik" to "Romantik", "savas" to "Savaş", "spor" to "Spor",
            "suc" to "Suç", "tarih" to "Tarih", "western" to "Western"
        )

        val jobs = kategoriler.map { (turParam, gorunenAd) ->
            async(Dispatchers.IO) {
                val document = app.post(
                    "https://www.trdiziizle.vip/wp-admin/admin-ajax.php",
                    referer = "https://www.trdiziizle.vip/dizi-arsivi-01/",
                    headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
                        "X-Requested-With" to "XMLHttpRequest"
                    ),
                    data = mapOf(
                        "filtrele" to "meta_value_num",
                        "sirala" to "DESC",
                        "yil" to "",
                        "imdb" to "",
                        "kelime" to "",
                        "tur" to turParam,
                        "action" to "customfilter"
                    )
                ).document

                val items = document.select("div.single-item")
                    .mapNotNull { it.toMainPageResult() }

                if (items.isNotEmpty()) {
                    HomePageList(gorunenAd, items)
                } else null
            }
        }

        val sections = jobs.awaitAll().filterNotNull()
        newHomePageResponse(sections)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.cat-title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div#list-series").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.cat-title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.category_image img")?.attr("data-src"))
        val description = document.selectFirst("div#icerikcatright")?.ownText()?.trim()
        val year = document
            .selectFirst("#icerikcat2 span:contains(Yapım Yılı)")
            ?.nextSibling()
            ?.outerHtml()
            ?.trim()
            ?.toIntOrNull()
        val actors = document
            .selectFirst("#icerikcat2 span:contains(Oyuncular)")
            ?.nextSibling()
            ?.outerHtml()
            ?.split(",")
            ?.map { Actor(it.trim()) }
            ?: emptyList()
        val tags = document
            .selectFirst("#icerikcat2 span:contains(Tür)")
            ?.nextSibling()
            ?.outerHtml()
            ?.split(",")
            ?.map { it.trim() }
            ?: emptyList()
        val rating = document
            .selectFirst("span.dt_rating_vgs")
            ?.text()
            ?.trim()
        val trailer = Regex("""embed\/(.*)\?rel""")
            .find(document.html())
            ?.groupValues
            ?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        val pageUrls = mutableListOf(url).apply {
            document
                .select("div.sayfalama a[href]")
                .map { it.attr("href") }
                .forEach { add(it) }
        }
        val episodeList = coroutineScope {
            pageUrls.map { pageUrl ->
                async(Dispatchers.IO) {
                    val doc = app.get(pageUrl).document
                    doc.select("div.container div.bolumust").mapNotNull { el ->
                        val linkTag = el.select("a[href]:not(.wpfp-link)").firstOrNull()
                            ?: return@mapNotNull null
                        val epHref = fixUrlNull(linkTag.attr("href")) ?: return@mapNotNull null

                        val titleText = el
                            .selectFirst("div.baslik")
                            ?.text()
                            ?.trim()
                            ?: return@mapNotNull null

                        val match = Regex(""".*?(\d+)\.Bölüm(?:\s*-\s*(.*))?""")
                            .find(titleText)
                        val epNumber = match?.groups?.get(1)?.value?.toInt()
                        val epTitle = match?.groups?.get(2)?.value?.trim() ?: "Bölüm"

                        newEpisode(epHref) {
                            this.episode = epNumber
                            this.name = epTitle
                        }
                    }
                }
            }.awaitAll().flatten()
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeList) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(rating)
            addActors(actors)
            trailer?.let { addTrailer(it) }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // Check for parts in the main document
        val partLink = document.select("div.diziplus_part a").attr("href")
        val partUrl = if (partLink.isNotEmpty()) fixUrl(partLink) else null

        // Determine which document to use for iframe extraction
        val targetDoc = partUrl?.let { app.get(it).document } ?: document

        val iframeSrc = targetDoc.selectFirst("iframe")?.attr("src")

        val iframe: String = iframeSrc?.let { src ->
            val realUrl = if ("id=" in src) {
                src.substringAfter("id=")
            } else {
                src
            }
            fixUrlNull(realUrl) as String
        } ?: ""

        Log.d("trdizi", "Selected iframe: $iframe")


        val iframeiki = app.get(
            iframe,
            referer = iframe,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.118 Safari/537.36",
                "Sec-Fetch-Dest" to "iframe",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
            )
        ).document

//        Log.d("trdizi", "$iframeiki")


        val videoyakala = Regex(pattern = "sources: \\[[^\\]]*\\],", options = setOf(RegexOption.IGNORE_CASE))

        val videourl: MatchResult? = videoyakala.find(iframeiki.html())

        videourl?.let { match ->
            val matchedContent = match.value
            Log.d("trdizi", "Found match: $matchedContent")

            // Extract file URL directly from the full match
            val cleanedSrc = matchedContent
                .replace("\\/", "/")
                .replace("\\", "")
                .trim()
                .trimEnd(',')
                .trim('"')
            val fileRegex = Regex("file:\"(https?://[^\"]+)\"")
            val fileMatch = fileRegex.find(cleanedSrc)
            val videoUrl = fileMatch?.groupValues?.get(1)

            // Extract label
            val labelRegex = Regex("label:\\s*\"([^\"]+)\"")
            val labelMatch = labelRegex.find(matchedContent)
            val label = labelMatch?.groupValues?.get(1)

            // Extract type
            val typeRegex = Regex("type:\\s*\"([^\"]+)\"")
            val typeMatch = typeRegex.find(matchedContent)
            val type = typeMatch?.groupValues?.get(1)

            Log.d("trdizi", "Extracted video URL: $videoUrl")
            Log.d("trdizi", "Extracted label: $label")
            Log.d("trdizi", "Extracted type: $type")

            // Add the extracted link to callback
            if (videoUrl == null) return true

            return try {
                when {

                    iframe.contains("youtube") -> {
                        YoutubeExtractor()
                            .getUrl(iframe, iframe)
                            ?.forEach(callback)
                    }

                    videoUrl.contains("disk.yandex.ru") -> {
                        val resolvedUrl = app.get(
                            videoUrl,
                            referer = iframe,
                            headers = mapOf(
                                "User-Agent" to USER_AGENT,
                                "Accept" to "*/*"
                            ),
                            allowRedirects = true
                        ).url

                        callback(
                            newExtractorLink(
                                name = "TrDizi",
                                source = "Yandex - TrDizi",
                                url = resolvedUrl,
                                type = INFER_TYPE
                            ) {
                                quality = Qualities.Unknown.value
                                referer = videoUrl
                                headers = mapOf(
                                    "User-Agent" to USER_AGENT,
                                    "Accept" to "*/*",
                                    "Referer" to videoUrl
                                )
                            }
                        )
                    }

                    else -> {
                        val type = if (videoUrl.contains("googlevideo") || videoUrl.contains(".mp4"))
                            ExtractorLinkType.VIDEO
                        else
                            ExtractorLinkType.M3U8

                        callback(
                            newExtractorLink(
                                name = "TrDizi",
                                source = "TrDizi",
                                url = videoUrl,
                                type = INFER_TYPE
                            ) {
                                quality = Qualities.Unknown.value
                                referer = videoUrl
                                headers = mapOf(
                                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                            "Chrome/124.0.6367.118 Safari/537.36",
                                    "Accept" to "*/*"
                                )
                            }
                        )
                    }
                }
                true
            } catch (e: Exception) {
                Log.e("trdizi", "Error in loadExtractor: ${e.message}")
                true
            }
        }
        return true
    }
    }