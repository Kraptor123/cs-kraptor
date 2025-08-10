// ! Bu araç @kraptor123 tarafından yazılmıştır.
package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.R.string.season
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Document
import java.net.URLEncoder
import com.lagradost.cloudstream3.extractors.*
import com.lagradost.cloudstream3.newEpisode

class CizgiveDizi : MainAPI() {
    override var mainUrl = "https://cizgivedizi.com"
    override var name = "CizgiveDizi"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Cartoon)

    // Yönetilebilir filtreler
    private val excludedTags = listOf("lgbt")

    // Kategori etiket kodları ve sıralaması
    private val categoryOrder = listOf(
        "çd", "diz", "ani", "yans", "pro", "bel", "kom", "mac", "çi", "yi",
        "sih", "yem", "sav", "ftb", "pemd", "müz", "giz", "kork", "eği", "dra", "gh",
        "tıp", "yar", "aks", "bilkur", "fant", "spor",
    )

    // Etiket kodu -> açıklama
    private val tagLabels by lazy { runBlocking { loadTagLabels() } }

    // İçerik kodu -> etiket kodları
    private val contentTags by lazy {
        runBlocking {
            val diziTags = loadContentTagMappings("dizi")
            val filmTags = loadContentTagMappings("film")
            diziTags + filmTags
        }
    }

    override val mainPage = mainPageOf(
        "$mainUrl/dizi" to "Diziler",
        *categoryOrder.map { code ->
            "$mainUrl/etiket/$code" to tagLabels[code].orEmpty()
        }.toTypedArray()
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (page > 1) return newHomePageResponse(listOf())
        Log.d("CizgiVeDizi", "getMainPage çağrıldı: ${request.name}")

        // Etiket bazlı listeleme: hem dizi hem film
        val tagCode = tagLabels.entries.firstOrNull { it.value == request.name }?.key
        if (tagCode != null) {
            val results = mutableListOf<SearchResponse>()

            // Diziler
            runCatching {
                val (diziKodList, diziIsimMap) = loadIsimData("dizi")
                val diziPosterMap = loadPosterData("dizi")
                diziKodList.filter { (code, _) -> contentTags[code]?.contains(tagCode) == true }
                    .forEach { (code, path) ->
                        if (contentTags[code]?.any { it in excludedTags } == true) return@forEach
                        val title = diziIsimMap[code] ?: return@forEach
                        val url = "$mainUrl/dizi/$code/$path"
                        val rawPoster = diziPosterMap[code]
                        val poster = rawPoster?.let { fixImageFormat(it) }
                        results += newTvSeriesSearchResponse(title, url, TvType.Cartoon) { this.posterUrl = poster }
                    }
            }.onFailure { Log.e("CizgiVeDizi", "Dizi yükleme hatası $it") }

            // Filmler
            runCatching {
                val (filmKodList, filmIsimMap) = loadIsimData("film")
                val filmPosterMap = loadPosterData("film")
                filmKodList.filter { (code, _) -> contentTags[code]?.contains(tagCode) == true }
                    .forEach { (code, path) ->
                        if (contentTags[code]?.any { it in excludedTags } == true) return@forEach
                        val rawTitle = filmIsimMap[code] ?: return@forEach
                        val title = "$rawTitle (film)"
                        val url = "$mainUrl/film/$code/$path"
                        val rawPoster = filmPosterMap[code]
                        val poster = rawPoster?.let { fixImageFormat(it) }
                        results += newMovieSearchResponse(title, url, TvType.Movie) { this.posterUrl = poster }
                    }
            }.onFailure { Log.e("CizgiVeDizi", "Film yükleme hatası $it") }

            // Karışık listeleme için karıştır
            results.shuffle()

            return newHomePageResponse(request.name, results)
        }

        // Ana sayfa: sadece Diziler ana girdisi
        val results = runCatching {
            val (kodList, isimMap) = loadIsimData("dizi")
            val posterMap = loadPosterData("dizi")
            kodList.mapNotNull { (code, path) ->
                if (contentTags[code]?.any { it in excludedTags } == true) return@mapNotNull null
                val title = isimMap[code] ?: return@mapNotNull null
                val url = "$mainUrl/dizi/$code/$path"
                val rawPoster = posterMap[code]
                val poster = rawPoster?.let { fixImageFormat(it) }
                newTvSeriesSearchResponse(title, url, TvType.Cartoon) { this.posterUrl = poster }
            }
        }.getOrElse {
            Log.e("CizgiVeDizi", "Ana sayfa yükleme hatası $it")
            emptyList()
        }

        return newHomePageResponse("Diziler", results)
    }

    private suspend fun loadTagLabels(): Map<String, String> {
        val text = app.get("$mainUrl/etiket.txt").text
        return text.lineSequence().map { it.trim().removePrefix("|") }
            .mapNotNull {
                val parts = it.split('=', limit = 2)
                parts.getOrNull(1)?.let { code -> parts[0].lowercase() to code.trim() }
            }.toMap()
    }

    private suspend fun loadContentTagMappings(basePath: String): Map<String, List<String>> {
        val text = app.get("$mainUrl/$basePath/etiket.txt").text
        return text.lineSequence().map { it.trim().removePrefix("|") }
            .mapNotNull {
                val parts = it.split('=', limit = 2)
                parts.getOrNull(1)?.let {
                    parts[0].lowercase() to it.split(';').map { tag -> tag.trim().lowercase() }
                }
            }.toMap()
    }

    private suspend fun loadIsimData(basePath: String): Pair<List<Pair<String, String>>, Map<String, String>> {
        val resp = app.get("$mainUrl/$basePath/isim.txt")
        val list = mutableListOf<Pair<String, String>>()
        val map = mutableMapOf<String, String>()
        resp.text.lineSequence().forEach { line ->
            line.trim().removePrefix("|").split('=', limit = 2).takeIf { it.size == 2 }?.let {
                val code = it[0].trim().lowercase()
                val title = it[1].trim()
                list += code to title.replace(" ", "_")
                map[code] = title
            }
        }
        return list to map
    }

    private suspend fun loadPosterData(basePath: String): Map<String, String> {
        val resp = app.get("$mainUrl/$basePath/poster.txt")
        return resp.text.lineSequence().map { it.trim().removePrefix("|") }
            .mapNotNull {
                val parts = it.split('=', limit = 2)
                parts.getOrNull(1)?.let { raw ->
                    val url = when {
                        raw.startsWith("http") -> raw
                        raw.startsWith("/") -> "$mainUrl$raw"
                        else -> "$mainUrl/$raw"
                    }
                    parts[0].lowercase() to url
                }
            }.toMap()
    }

    private fun fixImageFormat(url: String): String {
        if (url.isEmpty()) return ""
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        return try {
            "https://res.cloudinary.com/di0j4jsa8/image/fetch/f_auto/$encodedUrl"
        } catch (e: Exception) {
            url
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val normalizedQuery = normalizeString(query.lowercase().trim())
        val results = mutableListOf<SearchResponse>()
        for (basePath in listOf("dizi", "film")) {
            val (kodList, isimMap) = loadIsimData(basePath)
            val posterMap = loadPosterData(basePath)
            kodList.forEach { (code, _) ->
                val titlePlain = isimMap[code] ?: return@forEach
                if (!normalizeString(titlePlain.lowercase()).contains(normalizedQuery)) return@forEach
                if (contentTags[code]?.any { it in excludedTags } == true) return@forEach
                val title = if (basePath == "film") "$titlePlain (film)" else titlePlain
                val formattedRaw = titlePlain.replace(" ", "_")
                val url = "$mainUrl/$basePath/$code/$formattedRaw"
                val rawPoster = posterMap[code]
                val poster = rawPoster?.let { fixImageFormat(it) }
                results += newTvSeriesSearchResponse(
                    title,
                    url,
                    if (basePath == "film") TvType.Movie else TvType.Cartoon
                ) {
                    this.posterUrl = poster
                }
            }
        }
        return results
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    return try {
        val doc = app.get(url).document
        val isMovie = url.contains("/film/")
        if (!isMovie) loadSeries(doc, url) else loadMovie(doc, url)
    } catch (e: Exception) {
        null
    }
}

private suspend fun loadSeries(doc: Document, url: String) = runCatching {
    val titleElement = doc.selectFirst("div.infoLine h4")
        ?: return@runCatching null
    val title = titleElement.text()
    
    val posterElement = doc.selectFirst("picture img")
        ?: return@runCatching null
    val rawPoster = fixUrlNull(posterElement.attr("src"))
        ?: return@runCatching null
    val poster = fixImageFormat(rawPoster)
    
    val plot = doc.selectFirst("div.col-12 p")?.text()?.trim() ?: ""
    
    val tags = doc.select(".hero > div:nth-child(2) > div:nth-child(3) > p:nth-child(1)")
        .flatMap { it.text().split(",") }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    
    val episodes = doc.select("div.container a.bolum").mapNotNull { el ->
        try {
            val cardTitleElement = el.selectFirst(".card-title")
                ?: return@mapNotNull null
            val rawName = cardTitleElement.text().trim()
            val epName = rawName.substringAfter(")").trim()
            val href = fixUrlNull(el.attr("href")) ?: return@mapNotNull null
            
            val regexMatch = Regex("^(\\d+)").find(rawName)
                ?: return@mapNotNull null
            val num = regexMatch.groupValues[1].toInt()
            
            val seasonN = el.attr("data-sezon").toIntOrNull() ?: 1
            
            newEpisode(href) {
                this.name = epName
                this.episode = num
                this.season = seasonN
            }
        } catch (e: Exception) {
            null
        }
    }
    
    newTvSeriesLoadResponse(title, url, TvType.Cartoon, episodes) {
        this.posterUrl = poster
        this.plot = plot
        this.tags = tags
    }
}.getOrNull()

private suspend fun loadMovie(doc: Document, url: String) = runCatching {
    val titleElement = doc.selectFirst("h1.fw-light")
        ?: return@runCatching null
    val rawTitle = titleElement.text()
    val title = "$rawTitle (film)"
    
    val posterElement = doc.selectFirst("picture img")
        ?: return@runCatching null
    val rawPoster = fixUrlNull(posterElement.attr("src"))
        ?: return@runCatching null
    val poster = fixImageFormat(rawPoster)
    
    val plot = doc.selectFirst(".lead")?.text()?.trim() ?: ""
    
    val tags = doc.select(".hero > div:nth-child(2) > div:nth-child(3) > p:nth-child(1)")
        .flatMap { it.text().split(",") }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    
    newMovieLoadResponse(title, url, TvType.Movie, url) {
        this.posterUrl = poster
        this.plot = plot
        this.tags = tags
    }
}.getOrNull()

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        // örnek: iframe veya video linki var mı?
        val playPage = doc.selectFirst("a[href*='/play']")?.attr("href")
            ?: doc.selectFirst("iframe")?.attr("src")
            ?: return false

        val playUrl = fixUrlNull(playPage) ?: return false

        return loadExtractor(playUrl, subtitleCallback, callback)
    }

    private fun fixUrlNull(url: String?) =
        url?.let { if (it.startsWith("http")) it else "$mainUrl${if (it.startsWith("/")) it else "/$it"}" }

    private fun normalizeString(input: String) = input
        .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u')
        .replace('ş', 's').replace('ö', 'o').replace('ç', 'c')
        .replace('-', ' ').replace('_', ' ').replace('.', ' ')
}