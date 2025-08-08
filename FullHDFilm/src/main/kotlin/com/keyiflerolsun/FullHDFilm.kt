// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.nodes.Element

class FullHDFilm : MainAPI() {
    override var mainUrl              = "https://fullhdfilm.cx"
    override var name                 = "FullHDFilm"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "${mainUrl}/yabanci-dizi-izle/page"			    to "Yabancı Dizi",
        "${mainUrl}/yabanci-film-izle/page"			    to "Yabancı Filmler",
        "${mainUrl}/yerli-film-izle/page"				to "Yerli Film",
        "${mainUrl}/netflix-filmleri-izle/page"		    to "Netflix",
        "${mainUrl}/aile-filmleri/page"				    to "Aile",
        "${mainUrl}/aksiyon-filmleri-izle-hd1/page"	    to "Aksiyon",
        "${mainUrl}/animasyon-filmleri-izlesene/page"	to "Animasyon",
        "${mainUrl}/anime-izle/page"					to "Anime",
        "${mainUrl}/belgesel/page"					    to "Belgesel",
        "${mainUrl}/bilim-kurgu-filmleri/page"		    to "Bilim-Kurgu",
        "${mainUrl}/biyografi-filmleri/page"			to "Biyografi",
        "${mainUrl}/dram-filmleri/page"				    to "Dram",
        "${mainUrl}/fantastik-filmler-izle/page"		to "Fantastik",
        "${mainUrl}/gerilim-filmleri-izle-hd/page"		to "Gerilim",
        "${mainUrl}/gizem-filmleri/page"				to "Gizem",
        "${mainUrl}/komedi-filmleri/page"				to "Komedi",
        "${mainUrl}/korku-filmleri-izle/page"			to "Korku",
        "${mainUrl}/macera-filmleri-izle-hd/page"		to "Macera",
        "${mainUrl}/romantik-filmler/page"			    to "Romantik",
        "${mainUrl}/savas-filmleri-izle-hd/page"		to "Savaş",
        "${mainUrl}/suc-filmleri-izle/page"			    to "Suç"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/${page}/").document
        val home     = document.select("div.movie_box").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
        val puan      = this.selectFirst("span.imdb")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/?s=${query}").document

        return document.select("div.movie_box").mapNotNull { it.toMainPageResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url).document

    val title       = document.selectFirst("h1 span")?.text()?.trim() ?: return null
    val poster      = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
    val description = document.selectFirst("div[itemprop='description']")?.text()?.substringAfter("⭐")?.substringAfter("izleyin.")?.substringAfter("konusu:")?.trim()
    val year        = document.selectFirst("span[itemprop='dateCreated'] a")?.text()?.trim()?.toIntOrNull()
    val tags        = document.select("div.detail ul.bottom li:nth-child(5) span a").map { it.text() }
    val rating      = document.selectFirst("ul.right li:nth-child(2) span")?.text()?.trim()
    val duration    = document.selectFirst("span[itemprop='duration']")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
    val actors      = document.select("sc[itemprop='actor'] span").map { Actor(it.text()) }
    val trailer     = fixUrlNull(document.selectFirst("[property='og:video']")?.attr("content"))

    if (url.contains("-dizi") || tags.any { it.lowercase().contains("dizi") }) {
        val episodes = mutableListOf<Episode>()

        val iframeSkici = IframeKodlayici()

        val partNumbers  = document.select("li.psec").map { it.attr("id") }
        val partNames    = document.select("li.psec a").map { it.text().trim() }
        val pdataMatches = Regex("""pdata\['(.*?)'] = '(.*?)';""").findAll(document.html())
        val pdataList    = pdataMatches.map { it.destructured }.toList()

        partNumbers.forEachIndexed { index, partNumber ->
            val partName = partNames.getOrNull(index)
            val pdata    = pdataList.getOrNull(index)

            val value = pdata?.component2()

            if (partName!!.lowercase().contains("fragman") || partNumber.lowercase().contains("fragman")) return@forEachIndexed

            try {
                // VideoData objesi al
                val videoData = iframeSkici.iframeCoz(value!!)
                
                // M3U8 URL'ini kullan (artık çözülmüş iframe URL'i değil)
                val videoUrl = videoData.m3u8Url

                val szNum = partNumber.takeIf { it.contains("sezon") }?.substringBefore("sezon")?.toIntOrNull() ?: 1
                val epNum = partName.substringBefore(".").trim().toIntOrNull() ?: 1

                episodes.add(newEpisode(videoUrl) {
                    this.name = "${szNum}. Sezon ${epNum}. Bölüm"
                    this.season = szNum
                    this.episode = epNum
                })
            } catch (e: Exception) {
                Log.e("FHDF", "Error processing episode $partName: ${e.message}")
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot      = description
            this.year      = year
            this.tags      = tags
            this.score = Score.from10(rating)
            this.duration  = duration
            addActors(actors)
            addTrailer(trailer)
        }
    } else {
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot      = description
            this.year      = year
            this.tags      = tags
            this.score = Score.from10(rating)
            this.duration  = duration
            addActors(actors)
            addTrailer(trailer)
        }
    }
}   

    @OptIn(DelicateCoroutinesApi::class)
override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    Log.d("FHDF", "data » $data")

    
    if (data.contains("hdload.site/uploads/encode/") && data.contains("master.m3u8")) {
        
        val videoId = data.substringAfter("/uploads/encode/").substringBefore("/master.m3u8")
        Log.d("FHDF", "Direct M3U8 - videoId » $videoId")
        
       
        callback(
            newExtractorLink(
                source = "FullHdFilm",
                name = "FullHdFilm",
                url = data,
                type = ExtractorLinkType.M3U8
            ) {
                headers = mapOf("Referer" to "https://hdload.site")
                quality = Qualities.Unknown.value
            }
        )

       
        val altyazilar = IframeKodlayici.altyaziLinkleriOlustur(videoId)
        altyazilar.forEach { (language, subtitleUrl) ->
            val subtitleFile = SubtitleFile(
                lang = language,
                url = subtitleUrl
            )
            
            subtitleCallback.invoke(subtitleFile)
            Log.d("FHDF", "Direct M3U8 subtitle » $language - $subtitleUrl")
        }
        
        return true
    }

    if (!data.contains(mainUrl)) {
        loadExtractor(data, "${mainUrl}/", subtitleCallback, callback)
        return true
    }

    
    val document = app.get(data).document
    val iframeKodlayici = IframeKodlayici()

    val partNumbers = document.select("li.psec").map { it.attr("id") }
    val partNames = document.select("li.psec a").map { it.text().trim() }

    
    val pdataMatches = Regex("""pdata\['(.*?)'] = '(.*?)';""").findAll(document.html())
    val pdataList = pdataMatches.map { it.destructured }.toList()

    Log.d("FHDF", "Found ${partNumbers.size} parts, ${pdataList.size} pdata entries")

    partNumbers.forEachIndexed { index, partNumber ->
        val partName = partNames.getOrNull(index)
        val pdata = pdataList.getOrNull(index)

        if (partName!!.lowercase().contains("fragman") || partNumber.lowercase().contains("fragman")) {
            return@forEachIndexed
        }

        Log.d("FHDF", "partNumber » $partNumber")
        Log.d("FHDF", "partName   » $partName")

        try {
            
            val pdataValue = pdata?.component2()
            
            if (pdataValue != null && pdataValue.isNotEmpty()) {
                Log.d("FHDF", "pdataValue » $pdataValue")
                
                
                val videoData = iframeKodlayici.iframeCoz(pdataValue)
                
                Log.d("FHDF", "videoId » ${videoData.videoId}")
                Log.d("FHDF", "m3u8Url » ${videoData.m3u8Url}")

                
                callback(
                    newExtractorLink(
                        source = "FHDF",
                        name = "FullHdFilm",
                        url = videoData.m3u8Url,
                        type = ExtractorLinkType.M3U8
                    ) {
                        headers = mapOf("Referer" to videoData.referer)
                        quality = Qualities.Unknown.value
                    }
                )

                
                videoData.altyazilar.forEach { (language, subtitleUrl) ->
                    val subtitleFile = SubtitleFile(
                        lang = language,
                        url = subtitleUrl
                    )
                    
                    subtitleCallback.invoke(subtitleFile)
                    Log.d("FHDF", "subtitle » $language - $subtitleUrl")
                }
            } else {
                
                val iframes = document.select("iframe[src*=hdload.site]")
                val iframe = iframes.getOrNull(index)
                
                if (iframe != null) {
                    val iframeSrc = iframe.attr("src")
                    Log.d("FHDF", "fallback iframeSrc » $iframeSrc")
                    
                    val videoId = IframeKodlayici.videoIdCikar(iframeSrc)
                    val m3u8Url = IframeKodlayici.m3u8LinkOlustur(videoId)
                    val altyazilar = IframeKodlayici.altyaziLinkleriOlustur(videoId)
                    
                    Log.d("FHDF", "fallback videoId » $videoId")
                    Log.d("FHDF", "fallback m3u8Url » $m3u8Url")
                    
                    callback(
                        newExtractorLink(
                            source = "FullHdFilm",
                            name = "FullHdFilm ",
                            url = m3u8Url,
                            type = ExtractorLinkType.M3U8
                        ) {
                            headers = mapOf("Referer" to "https://hdload.site")
                            quality = Qualities.Unknown.value
                        }
                    )
                    
                    altyazilar.forEach { (language, subtitleUrl) ->
                        val subtitleFile = SubtitleFile(
                            lang = language,
                            url = subtitleUrl
                        )
                        
                        subtitleCallback.invoke(subtitleFile)
                        Log.d("FHDF", "fallback subtitle » $language - $subtitleUrl")
                    }
                }
            }
        } catch (e: Exception) {
            
        }
    }

    return true
}}