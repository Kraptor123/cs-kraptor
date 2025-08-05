// ! Bu araç @kerimmkirac tarafından | @kerimmkirac için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DiziAsya : MainAPI() {
    override var mainUrl = "https://api.diziasya.com"
    override var name = "DiziAsya"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(
        TvType.AsianDrama, TvType.Movie, TvType.Anime
    )

    override val mainPage = mainPageOf(
        "$mainUrl/v2/contents/new-contents?take=18" to "Yeni Eklenenler",
        "$mainUrl/v2/contents/content-by-type?type=SERIES&take=18" to "Diziler",
        "$mainUrl/v2/contents/content-by-type?type=MOVIE&take=18" to "Filmler",
        "$mainUrl/v2/contents/content-by-type?type=ANIME&take=18" to "Anime",
        "$mainUrl/v2/contents/content-by-type?type=SHOW&take=18" to "Programlar"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val json = app.get("${request.data}&page=$page").parsed<Map<String, List<Map<String, Any>>>>()
        val items = json["data"] ?: emptyList()

        val results = items.mapNotNull { item ->
            val title = item["title"] as? String ?: return@mapNotNull null
            val slug = item["slug"] as? String ?: return@mapNotNull null
            val cover = item["cover_image"] as? String
            val optimizedCover = cover?.let { "https://www.diziasya.com/_next/image?url=$it&w=384&q=75" } 
            val type = (item["type"] as? String)?.uppercase()

            val tvType = when (type) {
                "MOVIE" -> TvType.Movie
                "SERIES" -> TvType.AsianDrama
                "ANIME" -> TvType.Anime
                "SHOW" -> TvType.AsianDrama
                else -> return@mapNotNull null
            }

            
            val displayTitle = if (request.name == "Yeni Eklenenler" && tvType != TvType.Movie) {
                
                val chapters = item["chapters"] as? List<Map<String, Any>>
                if (!chapters.isNullOrEmpty()) {
                    val latestChapter = chapters.first()
                    val seasonNumber = (latestChapter["season"] as? Map<String, Any>)?.get("number") as? Number ?: 1
                    val chapterNumber = latestChapter["number"] as? String ?: "1"
                    "$title ${seasonNumber}x${chapterNumber}"
                } else {
                    title
                }
            } else {
                title
            }

            newMovieSearchResponse(displayTitle, "$mainUrl/v2/contents/$slug", tvType) {
                this.posterUrl = optimizedCover
            }
        }

        return newHomePageResponse(request.name, results)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$mainUrl/v2/contents/content-by-type?search=$encodedQuery&page=1&take=100"

        val res = app.get(url).parsed<Map<String, List<Map<String, Any>>>>()
        val results = res["data"] ?: return emptyList()

        return results.mapNotNull { item ->
            val title = item["title"] as? String ?: return@mapNotNull null
            val slug = item["slug"] as? String ?: return@mapNotNull null
            val cover = item["cover_image"] as? String
            val type = (item["type"] as? String)?.uppercase()

            val tvType = when (type) {
                "MOVIE"  -> TvType.Movie
                "SERIES" -> TvType.AsianDrama
                "ANIME"  -> TvType.Anime
                "SHOW"   -> TvType.AsianDrama
                else     -> return@mapNotNull null
            }

            newMovieSearchResponse(title, "$mainUrl/v2/contents/$slug", tvType) {
                this.posterUrl = cover?.let { "https://www.diziasya.com/_next/image?url=$it&w=384&q=75" }
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        Log.d("DiziAsya", "Loading content from: $url")
        
        val res = app.get(url).parsed<Map<String, Any>>()

        val type = res["type"] as? String ?: return null
        val title = res["title"] as? String ?: return null
        val slug = res["slug"] as? String ?: return null
        val plot = res["plot"] as? String
        val year = (res["publish_year"] as? String)?.toIntOrNull()
        val poster = res["cover_image"] as? String
        val rating = (res["rate"] as? String)?.toIntOrNull()
        
        Log.d("DiziAsya", "Content type: $type, Title: $title, Slug: $slug")
        
       
        val publishStatus = (res["publish_status"] as? Number)?.toInt()
        val statusText = when (publishStatus) {
            1 -> "Devam Ediyor"
            2 -> "Tamamlandı"
            else -> null
        }

       
        val tags = (res["tags"] as? List<Map<String, Any>>)?.mapNotNull { tag ->
            tag["name"] as? String
        }?.toMutableList() ?: mutableListOf()
        
       
        statusText?.let { tags.add(it) }

        val tvType = when (type.uppercase()) {
            "MOVIE" -> TvType.Movie
            "SERIES", "SHOW", "ANIME" -> TvType.AsianDrama
            else -> return null
        }

        val posterUrl = poster?.let {
            "https://www.diziasya.com/_next/image?url=$it&w=384&q=75"
        }

       
        val recommendations = (res["similar_contents"] as? List<Map<String, Any>>)?.mapNotNull { item ->
            val recTitle = item["title"] as? String ?: return@mapNotNull null
            val recSlug = item["slug"] as? String ?: return@mapNotNull null
            val recCover = item["cover_image"] as? String
            val recType = (item["type"] as? String)?.uppercase()

            val recTvType = when (recType) {
                "MOVIE" -> TvType.Movie
                "SERIES" -> TvType.AsianDrama
                "ANIME" -> TvType.Anime
                "SHOW" -> TvType.AsianDrama
                else -> return@mapNotNull null
            }

            newMovieSearchResponse(recTitle, "$mainUrl/v2/contents/$recSlug", recTvType) {
                this.posterUrl = recCover?.let { "https://www.diziasya.com/_next/image?url=$it&w=384&q=75" }
            }
        } ?: emptyList()

        
        if (tvType == TvType.Movie) {
            Log.d("DiziAsya", "Movie detected, checking for movie_content")
            
            
            val movieContent = res["movie_content"] as? Map<String, Any>
            if (movieContent != null) {
                Log.d("DiziAsya", "Movie content found, using movie links")
                return newMovieLoadResponse(title, url, tvType, url) {
                    this.posterUrl = posterUrl
                    this.plot = plot
                    this.year = year
                    this.rating = rating
                    this.tags = tags
                    this.recommendations = recommendations
                }
            } else {
                Log.w("DiziAsya", "Movie content not found, trying as series")
               
            }
        }

        
        Log.d("DiziAsya", "Series detected or movie without movie_content, creating episodes")
        
        
        var seasons = res["seasons"] as? List<Map<String, Any>>
        
        
        if (seasons == null) {
            val seriesMeta = res["series_meta"] as? Map<String, Any>
            if (seriesMeta != null) {
                
                seasons = listOf(mapOf(
                    "number" to 1,
                    "chapters" to emptyList<Map<String, Any>>()
                ))
            } else {
                Log.w("DiziAsya", "No seasons or series_meta found")
                return null
            }
        }

        val episodes = seasons.flatMap { season ->
            val seasonNum = (season["number"] as? Number)?.toInt() ?: 1
            val chapters = season["chapters"] as? List<Map<String, Any>> ?: emptyList()

            chapters.mapNotNull { ep ->
                val epNum = (ep["number"] as? String)?.toIntOrNull() ?: return@mapNotNull null
                val epId = ep["id"] as? String ?: return@mapNotNull null
                val epTitle = ep["title"] as? String
                
                
                val publishDateStr = (ep["publish_date"] as? String) ?: (ep["publishment_date"] as? String)

                val publishDateMillis = publishDateStr?.let {
                    try {
                        val dateFormat = if (it.contains("T")) {
                            // ISO format: 2023-12-25T14:30:00
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        } else {
                            // Custom format: 2023-12-25 14:30
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        }

                        // UTC timezone'ı ayarla
                        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

                        // Parse et ve milisaniye cinsinden döndür
                        dateFormat.parse(it)?.time
                    } catch (e: Exception) {
                        null
                    }
                }

               
                val episodeName = if (!epTitle.isNullOrBlank()) {
                    epTitle
                } else {
                    "Bölüm $epNum"
                }

                
                val episodeUrl = "$mainUrl/v2/chapters/show?contentSlug=$slug&chapterNo=$epNum&seasonNo=$seasonNum&contentType=${type.uppercase()}"

                newEpisode(
                    url = episodeUrl,
                    {
                        name = episodeName
                        this.season = seasonNum
                        episode = epNum
                        date = publishDateMillis
                        this.posterUrl = posterUrl
                    }
                )
            }
        }

        Log.d("DiziAsya", "Created ${episodes.size} episodes")
        return newTvSeriesLoadResponse(title, url, TvType.AsianDrama, episodes) {
            this.posterUrl = posterUrl
            this.plot = plot
            this.year = year
            this.rating = rating
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("DiziAsya", " Loading links for: $data")
        
        try {
            val response = app.get(data).parsed<Map<String, Any>>()
            
            
            if (data.contains("/v2/contents/")) {
                Log.d("DiziAsya", " Movie detected, checking for movie_content")
                
                val movieContent = response["movie_content"] as? Map<String, Any>
                
                if (movieContent != null) {
                    Log.d("DiziAsya", " Movie content found, extracting links")
                    
                    val alternativeTitles = movieContent["alternative_titles"] as? List<String> ?: emptyList()
                    val alternativeLinks = movieContent["alternative_links"] as? List<String> ?: emptyList()
                    
                    Log.d("DiziAsya", " Found ${alternativeTitles.size} alternative sources for movie")
                    
                    if (alternativeTitles.isNotEmpty()) {
                        processLinks(alternativeTitles, alternativeLinks, subtitleCallback, callback)
                        return true
                    }
                } else {
                    Log.w("DiziAsya", " Movie content not found, trying as series")
                    
                    val alternativeTitles = response["alternative_titles"] as? List<String> ?: emptyList()
                    val alternativeLinks = response["alternative_links"] as? List<String> ?: emptyList()
                    
                    if (alternativeTitles.isNotEmpty()) {
                        processLinks(alternativeTitles, alternativeLinks, subtitleCallback, callback)
                        return true
                    }
                }
            }
            
            
            else if (data.contains("/v2/chapters/show")) {
                Log.d("DiziAsya", " Episode detected, extracting chapter links")
                
                
                val chapter = response["chapter"] as? Map<String, Any>
                val chapterContent = chapter?.get("chapterContent") as? Map<String, Any>
                
                if (chapterContent == null) {
                    Log.e("DiziAsya", " Chapter content not found in response")
                    Log.d("DiziAsya", "Response keys: ${response.keys}")
                    chapter?.let { Log.d("DiziAsya", "Chapter keys: ${it.keys}") }
                    return false
                }
                
                val alternativeTitles = chapterContent["alternativeTitles"] as? List<String> ?: emptyList()
                val alternativeLinks = chapterContent["alternativeLinks"] as? List<String> ?: emptyList()
                
                Log.d("DiziAsya", " Found ${alternativeTitles.size} alternative sources for episode")
                
                if (alternativeTitles.isNotEmpty()) {
                    processLinks(alternativeTitles, alternativeLinks, subtitleCallback, callback)
                    return true
                } else {
                    Log.w("DiziAsya", " No alternative sources found")
                    return false
                }
            }
            
            return false
        } catch (e: Exception) {
            Log.e("DiziAsya", "Error loading links: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private suspend fun processLinks(
        titles: List<String>,
        links: List<String>,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val linkPairs = titles.zip(links)
        
        linkPairs.forEach { (title, link) ->
            val cleanLink = if (link.startsWith("//")) "https:$link" else link
            Log.d("DiziAsya", "Processing $title: $cleanLink")
            
            when (title.lowercase()) {
                "vidmoly" -> {
                    Log.d("DiziAsya", "Vidmoly iframe çözümleyici çalışıyor...")
                    extractVidmolyDirectly(cleanLink, callback)
                }
                "okru" -> {
                    Log.d("DiziAsya", " OkRu extractor loading...")
                    loadExtractor(cleanLink, "$mainUrl/", subtitleCallback, callback)
                }
                "dracarys" -> {
                    Log.d("DiziAsya", " DzenRu extractor loading...")
                    loadExtractor(cleanLink, "$mainUrl/", subtitleCallback, callback)
                }
                "diziasya", "diziasya2" -> {
                    Log.d("DiziAsya", " DiziAsya custom extractor loading...")
                    extractDiziAsyaLinks(cleanLink, callback)
                }
                "klaus" -> {
                    Log.d("DiziAsya", " Klaus custom extractor loading...")
                    extractKlausLinks(cleanLink, callback)
                }
                "lulu" -> {
                    Log.d("DiziAsya", " LuluVdo custom extractor loading...")
                    extractLuluLinks(cleanLink, callback)
                }
                "ev" -> {
                    Log.d("DiziAsya", " EV custom extractor loading...")
                    extractGenericLinks(cleanLink, "EV", callback)
                }
                "p2p" -> {
                    Log.d("DiziAsya", " P2P custom extractor loading...")
                    extractGenericLinks(cleanLink, "P2P", callback)
                }
                "abstr" -> {
                    Log.d("DiziAsya", " ABStr custom extractor loading...")
                    extractGenericLinks(cleanLink, "ABStr", callback)
                }
                else -> {
                    Log.d("DiziAsya", "⚙️ Generic extractor trying for $title...")
                    try {
                        loadExtractor(cleanLink, "$mainUrl/", subtitleCallback, callback)
                    } catch (e: Exception) {
                        Log.w("DiziAsya", " Could not extract from $title: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun extractDiziAsyaLinks(url: String, callback: (ExtractorLink) -> Unit) {
        try {
            val baseUrl = "https://video.diziasya.com"
            
            val query = url.substringAfter("?", "")
            val params = query.split("&").associate {
                val (key, value) = it.split("=")
                key to value
            }

            val id = params["i"] ?: return
            val s = params["s"] ?: return
            val u = params["u"] ?: return

            val postUrl = "$baseUrl/embed/get?i=$id&s=$s&u=$u"

            val response = app.post(
                postUrl,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Referer" to "$baseUrl/",
                    "Accept" to "*/*"
                )
            ).parsed<Map<String, Any>>()

            if (response["isError"] == true) {
                Log.e("DiziAsya", " API returned error")
                return
            }

            val links = response["Links"] as? List<String> ?: emptyList()

            for (entry in links) {
                val quality = Regex("\\[(\\d+)p]").find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: Qualities.Unknown.value
                val encoded = entry.substringAfter("/redirect?v=", "")
                if (encoded.isBlank()) continue

                val redirectUrl = "$baseUrl/redirect?v=$encoded"

                val realUrl = app.get(
                    redirectUrl,
                    allowRedirects = false,
                    headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                        "Referer" to "$baseUrl/",
                        "Accept" to "*/*"
                    )
                ).headers["location"] ?: continue

                Log.d("DiziAsya", " $quality: $realUrl")

                callback(
                    newExtractorLink(
                        source = "DiziAsya",
                        name = "DiziAsya [$quality]",
                        url = realUrl,
                        type = if (realUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        headers = mapOf("Referer" to baseUrl)
                        this.quality = quality
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("DiziAsya", " extractDiziAsyaLinks error: ${e.message}")
        }
    }

    private suspend fun extractVidmolyDirectly(url: String, callback: (ExtractorLink) -> Unit) {
        try {
            val headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36",
                "Sec-Fetch-Dest" to "iframe",
                "Referer" to "https://vidmoly.to/"
            )

            val iSource = app.get(url, headers = headers, referer = "$mainUrl/").text

            val matches = Regex("""file\s*:\s*"([^"]+\.m3u8[^"]*)"""").findAll(iSource).toList()

            if (matches.isEmpty()) throw ErrorLoadingException("m3u8 link not found in iframe")

            Log.d("DiziAsya", "${matches.size} adet m3u8 bulundu")

            matches.forEachIndexed { index, match ->
                val m3uLink = match.groupValues[1]
                Log.d("DiziAsya", " m3uLink[$index] → $m3uLink")

                callback(
                    newExtractorLink(
                        source = "VidMoly",
                        name = "VidMoly [#${index + 1}]",
                        url = m3uLink,
                        type = ExtractorLinkType.M3U8,
                        
                    ) {
                        this.referer = "https://vidmoly.to/"
                        this.quality = Qualities.Unknown.value
                       
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("DiziAsya", " VidMoly extractor error: ${e.message}")
        }
    }

    private suspend fun extractKlausLinks(url: String, callback: (ExtractorLink) -> Unit) {
        try {
            val baseUrl = "https://video.diziasya.com"
        
            val query = url.substringAfter("?", "")
            val params = query.split("&").associate {
                val (key, value) = it.split("=")
                key to value
            }

            val id = params["i"] ?: return
            val s = params["s"] ?: return
            val u = params["u"] ?: return

            val postUrl = "$baseUrl/embed/get?i=$id&s=$s&u=$u"

            val response = app.post(
                postUrl,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Referer" to "$baseUrl/",
                    "Accept" to "*/*"
                )
            ).parsed<Map<String, Any>>()

            if (response["isError"] == true) {
                Log.e("DiziAsya", " API returned error")
                return
            }

            val links = response["Links"] as? List<String> ?: emptyList()

            for (entry in links) {
                val quality = Regex("\\[(\\d+)p]").find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: Qualities.Unknown.value
                val encoded = entry.substringAfter("/redirect?v=", "")
                if (encoded.isBlank()) continue

                val redirectUrl = "$baseUrl/redirect?v=$encoded"

                val realUrl = app.get(
                    redirectUrl,
                    allowRedirects = false,
                    headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                        "Referer" to "$baseUrl/",
                        "Accept" to "*/*"
                    )
                ).headers["location"] ?: continue

                Log.d("DiziAsya", " $quality: $realUrl")

                callback(
                    newExtractorLink(
                        source = "Klaus",
                        name = "Klaus",
                        url = realUrl,
                        type = if (realUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        headers = mapOf("Referer" to baseUrl)
                        this.quality = quality
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("DiziAsya", " extractKlausLinks error: ${e.message}")
        }
    }

    private suspend fun extractLuluLinks(url: String, callback: (ExtractorLink) -> Unit) {
        try {
            val doc = app.get(url).document
            val videoUrl = doc.selectFirst("video source")?.attr("src")
                ?: doc.selectFirst("iframe")?.attr("src")
            
            videoUrl?.let { link ->
                Log.d("DiziAsya", " LuluVdo link found: $link")
                callback(
                    newExtractorLink(
                        source = "LuluVdo",
                        name = "LuluVdo",
                        url = link,
                        type = if (link.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        headers = mapOf("Referer" to url)
                        quality = Qualities.Unknown.value
                    }
                )
            } ?: Log.w("DiziAsya", " LuluVdo video URL not found")
        } catch (e: Exception) {
            Log.e("DiziAsya", " LuluVdo extractor error: ${e.message}")
        }
    }

    private suspend fun extractGenericLinks(url: String, sourceName: String, callback: (ExtractorLink) -> Unit) {
        try {
            val doc = app.get(url).document
            val videoUrl = doc.selectFirst("video source")?.attr("src")
                ?: doc.selectFirst("iframe")?.attr("src")
            
            videoUrl?.let { link ->
                Log.d("DiziAsya", " $sourceName link found: $link")
                callback(
                    newExtractorLink(
                        source = sourceName,
                        name = sourceName,
                        url = link,
                        type = if (link.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        headers = mapOf("Referer" to url)
                        quality = Qualities.Unknown.value
                    }
                )
            } ?: Log.w("DiziAsya", " $sourceName video URL not found")
        } catch (e: Exception) {
            Log.e("DiziAsya", " $sourceName extractor error: ${e.message}")
        }
    }
}
