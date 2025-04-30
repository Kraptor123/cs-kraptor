// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.keyiflerolsun

import android.os.Build
import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Document
import java.net.URLEncoder

class CizgiveDizi : MainAPI() {
    override var mainUrl = "https://cizgivedizi.com"
    override var name = "CizgiveDizi"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Cartoon)

    // Kategori etiket kodları ve sıralaması
    private val categoryOrder = listOf(
        "çd","diz","ani","yans","pro","bel","kom","mac","çi","yi",
        "sih","yem","sav","ftb","pemd","müz","giz","kork","eği","dra","gh",
        "tıp","yar","aks","spor","polis","doğa","suç","füt"
    )

    // Kategori etiket kodu -> açıklama
    private val tagLabels by lazy { runBlocking { loadTagLabels() } }
    // İçerik kodu -> etiket kodları
    private val contentTags by lazy { runBlocking { loadContentTagMappings() } }

    // Ana sayfa: kategori listesi
    override val mainPage = mainPageOf(*categoryOrder.map { code ->
        "$mainUrl/etiket/$code" to tagLabels[code].orEmpty()
    }.toTypedArray())

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Sayfa kontrolü - Sadece ilk sayfayı gösterelim
        if (page > 1) {
            return HomePageResponse(listOf())
        }

        // Eğer kategori isteği ise
        val tagCode = tagLabels.entries.firstOrNull { it.value == request.name }?.key
        if (tagCode != null) {
            val (kodList, isimMap) = loadIsimData("dizi")
            val posterMap = loadPosterData("dizi")
            // Filtreleme: bu tagCode içeren içerikler
            val filtered = kodList.filter { (code, _) ->
                contentTags[code]?.contains(tagCode) == true
            }
            return newHomePageResponse(request.name,
                filtered.mapNotNull { (code, path) ->
                    val title = isimMap[code] ?: return@mapNotNull null
                    // LGBT içerik kontrolü - eğer içerik LGBT etiketine sahipse atla
                    if (contentTags[code]?.contains("lgbt") == true) {
                        return@mapNotNull null
                    }
                    val url = "$mainUrl/dizi/$code/$path"
                    val rawPoster = posterMap[code]
                    val poster = rawPoster?.let { fixImageFormat(it) }
                    newTvSeriesSearchResponse(title, url, TvType.Cartoon) {
                        this.posterUrl = poster
                    }
                }
            )
        }
        // Varsayılan: Diziler ve Filmler ana sayfası
        val basePath = if (request.name == "Filmler") "film" else "dizi"
        val (kodList, isimMap) = loadIsimData(basePath)
        val posterMap = loadPosterData(basePath)
        return newHomePageResponse(request.name,
            kodList.mapNotNull { (code, path) ->
                val title = isimMap[code] ?: return@mapNotNull null
                // LGBT içerik kontrolü - eğer içerik LGBT etiketine sahipse atla
                if (contentTags[code]?.contains("lgbt") == true) {
                    return@mapNotNull null
                }
                val url = "$mainUrl/$basePath/$code/$path"
                val rawPoster = posterMap[code]
                val poster = rawPoster?.let { fixImageFormat(it) }
                newTvSeriesSearchResponse(title, url, TvType.Cartoon) {
                    this.posterUrl = poster
                }
            }
        )
    }

    private suspend fun loadTagLabels(): Map<String,String> {
        val text = app.get("$mainUrl/etiket.txt").text
        return text.lineSequence()
            .map { it.trim().removePrefix("|") }
            .mapNotNull {
                val (code, label) = it.split('=', limit=2).takeIf { it.size==2 } ?: return@mapNotNull null
                code.lowercase() to label.trim()
            }.toMap()
    }

    private suspend fun loadContentTagMappings(): Map<String,List<String>> {
        val text = app.get("$mainUrl/dizi/etiket.txt").text
        return text.lineSequence()
            .map { it.trim().removePrefix("|") }
            .mapNotNull {
                val (contentCode, tags) = it.split('=', limit=2).takeIf { it.size==2 } ?: return@mapNotNull null
                contentCode.lowercase() to tags.split(';').map { it.trim().lowercase() }
            }.toMap()
    }

    // Var olan fonksiyonlar
    private suspend fun loadIsimData(basePath: String): Pair<List<Pair<String,String>>, Map<String,String>> {
        val resp = app.get("$mainUrl/$basePath/isim.txt")
        val list = mutableListOf<Pair<String,String>>()
        val map = mutableMapOf<String,String>()
        resp.text.split("\n").forEach { line ->
            line.trim().removePrefix("|").split('=',limit=2).takeIf{it.size==2}?.let{
                val code=it[0].trim().lowercase(); val title=it[1].trim();
                list.add(code to title.replace(" ","_")); map[code]=title
            }
        }
        return list to map
    }

    private suspend fun loadPosterData(basePath: String): Map<String,String> {
        val resp = app.get("$mainUrl/$basePath/poster.txt")
        return resp.text.lineSequence()
            .map { it.trim().removePrefix("|") }
            .mapNotNull {
                val (code,urlRaw)=it.split('=',limit=2).takeIf{it.size==2}?:return@mapNotNull null
                code.lowercase() to when {
                    urlRaw.startsWith("http") -> urlRaw
                    urlRaw.startsWith("/") -> "$mainUrl$urlRaw"
                    else -> "$mainUrl/$urlRaw"
                }
            }.toMap()
    }

    private fun fixImageFormat(url: String): String {
        if (url.isEmpty()) return ""
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        return "https://res.cloudinary.com/djjnbig4t/image/fetch/f_auto/$encodedUrl"
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val normalizedQuery = normalizeString(query.lowercase().trim())
        val results = mutableListOf<SearchResponse>()

        // Her iki içerik türü için arama yapalım
        for (basePath in listOf("dizi", "film")) {
            val (kodList, isimMap) = loadIsimData(basePath)
            val posterMap = loadPosterData(basePath)

            // isimMap içinde arama yap
            isimMap.forEach { (code, title) ->
                val normalizedTitle = normalizeString(title.lowercase())
                if (normalizedTitle.contains(normalizedQuery)) {
                    // LGBT içerik kontrolü - eğer içerik LGBT etiketine sahipse atla
                    if (contentTags[code]?.contains("lgbt") == true) {
                        return@forEach
                    }

                    val formattedTitle = title.replace(" ", "_")
                    val url = "$mainUrl/${basePath}/$code/$formattedTitle"

                    val rawPoster = posterMap[code]
                    val poster = rawPoster?.let { fixImageFormat(it) }

                    results.add(
                        newTvSeriesSearchResponse(title, url, TvType.Movie) {
                            this.posterUrl = poster
                        }
                    )
                }
            }
        }

        return results
    }

    /**
     * Türkçe karakterleri ve özel karakterleri normalize eden yardımcı fonksiyon
     */
    private fun normalizeString(input: String): String {
        return input
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')
            .replace('é', 'e')
            .replace('è', 'e')
            .replace('ê', 'e')
            .replace('â', 'a')
            .replace('î', 'i')
            .replace('û', 'u')
            .replace('ô', 'o')
            .replace('-', ' ')
            .replace('_', ' ')
            .replace('.', ' ')
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.title a")?.text() ?: return null
        val href      = fixUrl(this.selectFirst("div.title a")?.attr("href")) ?: return null
        val rawPoster = this.selectFirst("backgroundImageEffect")?.attr("src")
        val poster = rawPoster?.let { fixImageFormat(it) }
        return newTvSeriesSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        // Determine content type
        val tvType = when {
            url.contains("/dizi/", ignoreCase = true) -> TvType.TvSeries
            url.contains("/film/", ignoreCase = true) -> TvType.Movie
            else -> null
        }

        if (tvType == null) {
            return null
        }

        // Process based on content type
        if (tvType == TvType.TvSeries) {
            // TV Series specific selectors
            val title = document.selectFirst("div.infoLine h4")?.text() ?: return null
            val rawPoster = fixUrlNull(document.selectFirst("picture img")?.attr("src")) ?: return null
            val poster = fixImageFormat(rawPoster)
            val description = document.selectFirst("div.col-12 p")?.text()?.trim()

            val tagsRaw = document.select(".hero > div:nth-child(2) > div:nth-child(3) > p:nth-child(1)")
                .mapNotNull { it.text()?.trim() }

            val tags = tagsRaw.flatMap { it.split(",") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val episodes = document.select(".bolum").mapNotNull {
                val rawEpName = it.selectFirst(".card-title")?.text()?.trim() ?: return@mapNotNull null
                val epName = rawEpName.substringAfter(")").trim()
                val eprawHref = fixUrlNull(it.selectFirst("a.bolum")?.attr("href")) ?: return@mapNotNull null
                val epHref = eprawHref
                val epEpisode = Regex("""^(\d+)\)""").find(rawEpName)?.groupValues?.get(1)?.toIntOrNull()
                newEpisode(epHref) {
                    this.name = epName
                    this.episode = epEpisode
                }
            }

            return newTvSeriesLoadResponse(title, url, TvType.Cartoon, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.tags = tags
            }
        } else {
            // Movie specific processing
            val movieTitle = document.selectFirst("h1.fw-light")?.text() ?: return null
            val rawPoster = fixUrlNull(document.selectFirst("picture img")?.attr("src")) ?: return null
            val poster = fixImageFormat(rawPoster)
            val movieDescription = document.selectFirst(".lead")?.text()?.trim()

            val tagsRaw = document.select(".hero > div:nth-child(2) > div:nth-child(3) > p:nth-child(1)")
                .mapNotNull { it.text()?.trim() }

            val tags = tagsRaw.flatMap { it.split(",") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            return newMovieLoadResponse(movieTitle, url, TvType.Cartoon, url) {
                this.posterUrl = poster
                this.plot = movieDescription
                this.tags = tags
            }
        }
    }

    private fun fixUrl(url: String?): String? {
        if (url == null || url.isEmpty()) return null

        try {
            val fixedUrl = if (url.startsWith("http")) url else "$mainUrl${if (url.startsWith("/")) url else "/$url"}"
            return fixedUrl
        } catch (e: Exception) {
            Log.e("Cfdz", "URL düzeltme hatası: $url", e)
            return null
        }
    }

    private fun fixUrlNull(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        return try {
            if (url.startsWith("http")) url else "$mainUrl${if (url.startsWith("/")) url else "/$url"}"
        } catch (e: Exception) {
            Log.e("Cfdz", "URL düzeltme hatası: $url", e)
            null
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("Cfdz", "data =  $data")
        val document = app.get(data).document
        val videoLinki = document.selectFirst("iframe")?.attr("src")
        Log.d("Cfdz", "videoLinki » $videoLinki")
        val iframe = "$videoLinki"

        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}