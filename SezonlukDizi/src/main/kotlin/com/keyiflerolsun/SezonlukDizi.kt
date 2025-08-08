
package com.keyiflerolsun

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class SezonlukDizi : MainAPI() {
    override var mainUrl              = "https://sezonlukdizi6.com"
    override var name                 = "SezonlukDizi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
    "${mainUrl}/ajax/dataDefaultSonCikan.asp?d=-1&k=0&s=" to "Yeni Bölümler",
    "${mainUrl}/diziler.asp?siralama_tipi=id&s="          to "Yeni Diziler",
    "${mainUrl}/diziler.asp?siralama_tipi=id&kat=3&s="    to "Asya Dizileri",
    "${mainUrl}/diziler.asp?siralama_tipi=id&kat=1&s="    to "Yabancı Diziler",
    
    "${mainUrl}/diziler.asp?siralama_tipi=id&kat=4&s="    to "Animasyonlar",
    "${mainUrl}/diziler.asp?siralama_tipi=id&kat=5&s="    to "Animeler",
    "${mainUrl}/diziler.asp?siralama_tipi=id&kat=6&s="    to "Belgeseller",
)

override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val document = if (request.name == "Yeni Bölümler") {
       
        app.post(
            url = "${request.data}${page}",
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Accept" to "*/*",
                "Referer" to mainUrl
            )
        ).document
    } else {
        
        app.get("${request.data}${page}").document
    }
    
    val home = if (request.name == "Yeni Bölümler") {
        
        document.select("div.column div.ui.card").mapNotNull { it.toNewEpisodeSearchResult() }
    } else {
        
        document.select("div.afis a").mapNotNull { it.toSearchResult() }
    }

    return newHomePageResponse(request.name, home)
}

private fun Element.toSearchResult(): SearchResponse? {
    val title     = this.selectFirst("div.description")?.text()?.trim() ?: return null
    val href      = fixUrlNull(this.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
    val rating = this.selectFirst("span.imdbp")?.ownText()?.trim()
    Log.d("SZD", "rating » $rating")


    return newTvSeriesSearchResponse(title, href, TvType.TvSeries) 
    { 
        
     this.posterUrl = posterUrl
     this.score = rating?.replace(",", ".")?.toDoubleOrNull()?.let { 
        Score.from10(it) 
    }
}
}

private fun Element.toNewEpisodeSearchResult(): SearchResponse? {
    val link = this.selectFirst("a") ?: return null
    
    val titleElement = link.selectFirst("div.box-title span.title")?.text()?.trim() ?: return null
    val episodeElement = link.selectFirst("div.box-title span.seep")?.text()?.trim() ?: return null
    
    val title = "$titleElement $episodeElement"
    val originalHref = link.attr("href")
    
    
    val seriesName = originalHref.split("/").getOrNull(1) ?: return null
    val href = fixUrlNull("/diziler/$seriesName.html") ?: return null
    
    val posterUrl = fixUrlNull(link.selectFirst("img")?.attr("src"))
    val rating = this.selectFirst("span.imdbp")?.ownText()?.trim()
    

    return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
}

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/diziler.asp?adi=${query}").document

        return document.select("div.afis a").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title       = document.selectFirst("div.header")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(document.selectFirst("div.image img")?.attr("data-src")) ?: return null
        val year        = document.selectFirst("div.extra span")?.text()?.trim()?.split("-")?.first()?.toIntOrNull()
        val description = document.selectFirst("span#tartismayorum-konu")?.text()?.trim()
        val tags        = document.select("div.labels a[href*='tur']").mapNotNull { it.text().trim() }
        val rating      = document.selectFirst("div.detail")?.text()?.trim()
        val duration    = document.selectXpath("//span[contains(text(), 'Dk.')]").text().trim().substringBefore(" Dk.").toIntOrNull()

        val endpoint    = url.split("/").last()

        val actorsReq  = app.get("${mainUrl}/oyuncular/${endpoint}").document
        val actors     = actorsReq.select("div.doubling div.ui").map {
            Actor(
                it.selectFirst("div.header")!!.text().trim(),
                fixUrlNull(it.selectFirst("img")?.attr("src"))
            )
        }


        val episodesReq = app.get("${mainUrl}/bolumler/${endpoint}").document
        val episodes    = mutableListOf<Episode>()
        for (sezon in episodesReq.select("table.unstackable")) {
            for (bolum in sezon.select("tbody tr")) {
                val epName    = bolum.selectFirst("td:nth-of-type(4) a")?.text()?.trim() ?: continue
                val epHref    = fixUrlNull(bolum.selectFirst("td:nth-of-type(4) a")?.attr("href")) ?: continue
                val epEpisode = bolum.selectFirst("td:nth-of-type(3)")?.text()?.substringBefore(".Bölüm")?.trim()?.toIntOrNull()
                val epSeason  = bolum.selectFirst("td:nth-of-type(2)")?.text()?.substringBefore(".Sezon")?.trim()?.toIntOrNull()

                episodes.add(newEpisode(epHref) {
                    this.name    = epName
                    this.season  = epSeason
                    this.episode = epEpisode
                })
            }
        }


        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.year      = year
            this.plot      = description
            this.tags      = tags
            this.score = rating?.replace(",", ".")?.toDoubleOrNull()?.let { 
                Score.from10(it) 
            }
            this.duration  = duration
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("SZD", "data » $data")
        val document = app.get(data).document
        val aspData = getAspData()
        val bid      = document.selectFirst("div#dilsec")?.attr("data-id") ?: return false
        Log.d("SZD", "bid » $bid")

        val altyaziResponse = app.post(
            "${mainUrl}/ajax/dataAlternatif${aspData.alternatif}.asp",
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
            data    = mapOf(
                "bid" to bid,
                "dil" to "1"
            )
        ).parsedSafe<Kaynak>()
        altyaziResponse?.takeIf { it.status == "success" }?.data?.forEach { veri ->
            Log.d("SZD", "dil»1 | veri.baslik » ${veri.baslik}")

            val veriResponse = app.post(
                "${mainUrl}/ajax/dataEmbed${aspData.embed}.asp",
                headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
                data    = mapOf("id" to "${veri.id}")
            ).document

            val iframe = when {
    veri.baslik.contains("Dzen", ignoreCase = true) -> {
        val jsScript = veriResponse.selectFirst("script")?.data() ?: return@forEach
        val vid = Regex("""var\s+vid\s*=\s*['"](.+?)['"]""").find(jsScript)?.groupValues?.get(1) ?: return@forEach
        "https://dzen.ru/embed/$vid"
    }

    veri.baslik.contains("Pixel", ignoreCase = true) -> {
        val pixelIframe = fixUrlNull(veriResponse.selectFirst("iframe")?.attr("src")) ?: return@forEach
        val pixelPage = app.get(pixelIframe).document
        val pixelScript = pixelPage.select("script").mapNotNull { it.data() }.joinToString("\n")

        val hexEncoded = Regex("""file\s*:\s*["']((?:\\x[0-9a-fA-F]{2})+)["']""").find(pixelScript)?.groupValues?.get(1) ?: return@forEach

        val decodedUrl = hexEncoded
            .replace("""\\x""".toRegex(), "")
            .chunked(2)
            .joinToString("") { it.toInt(16).toChar().toString() }

        decodedUrl
    }

    else -> {
        fixUrlNull(veriResponse.selectFirst("iframe")?.attr("src")) ?: return@forEach
    }
}

            Log.d("SZD", "dil»1 | iframe » $iframe")

            loadExtractor(iframe, "${mainUrl}/", subtitleCallback) { link ->
                callback.invoke(
                    ExtractorLink(
                        source        = "AltYazı - ${veri.baslik}",
                        name          = "AltYazı - ${veri.baslik}",
                        url           = link.url,
                        referer       = link.referer,
                        quality       = link.quality,
                        headers       = link.headers,
                        extractorData = link.extractorData,
                        type          = link.type
                    )
                )
            }
        }

        val dublajResponse = app.post(
            "${mainUrl}/ajax/dataAlternatif${aspData.alternatif}.asp",
            headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
            data    = mapOf(
                "bid" to bid,
                "dil" to "0"
            )
        ).parsedSafe<Kaynak>()
        dublajResponse?.takeIf { it.status == "success" }?.data?.forEach { veri ->
            Log.d("SZD", "dil»0 | veri.baslik » ${veri.baslik}")

            val veriResponse = app.post(
                "${mainUrl}/ajax/dataEmbed${aspData.embed}.asp",
                headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
                data    = mapOf("id" to "${veri.id}")
            ).document

            val iframe = when {
    veri.baslik.contains("Dzen", ignoreCase = true) -> {
        val jsScript = veriResponse.selectFirst("script")?.data() ?: return@forEach
        val vid = Regex("""var\s+vid\s*=\s*['"](.+?)['"]""").find(jsScript)?.groupValues?.get(1) ?: return@forEach
        "https://dzen.ru/embed/$vid"
    }

    veri.baslik.contains("Pixel", ignoreCase = true) -> {
        val pixelIframe = fixUrlNull(veriResponse.selectFirst("iframe")?.attr("src")) ?: return@forEach
        val pixelPage = app.get(pixelIframe).document
        val pixelScript = pixelPage.select("script").mapNotNull { it.data() }.joinToString("\n")

        val hexEncoded = Regex("""file\s*:\s*["']((?:\\x[0-9a-fA-F]{2})+)["']""").find(pixelScript)?.groupValues?.get(1) ?: return@forEach

        val decodedUrl = hexEncoded
            .replace("""\\x""".toRegex(), "")
            .chunked(2)
            .joinToString("") { it.toInt(16).toChar().toString() }

        decodedUrl
    }

    else -> {
        fixUrlNull(veriResponse.selectFirst("iframe")?.attr("src")) ?: return@forEach
    }
}
            Log.d("SZD", "dil»0 | iframe » $iframe")

            loadExtractor(iframe, "${mainUrl}/", subtitleCallback) { link ->
                callback.invoke(
                    ExtractorLink(
                        source        = "Dublaj - ${veri.baslik}",
                        name          = "Dublaj - ${veri.baslik}",
                        url           = link.url,
                        referer       = link.referer,
                        quality       = link.quality,
                        headers       = link.headers,
                        extractorData = link.extractorData,
                        type          = link.type
                    )
                )
            }
        }

        return true
    }

    //Helper function for getting the number (probably some kind of version?) after the dataAlternatif and dataEmbed
    private suspend fun getAspData() : AspData{
        val websiteCustomJavascript = app.get("${this.mainUrl}/js/site.min.js")
        val dataAlternatifAsp = Regex("""dataAlternatif(.*?).asp""").find(websiteCustomJavascript.text)?.groupValues?.get(1)
            .toString()
        val dataEmbedAsp = Regex("""dataEmbed(.*?).asp""").find(websiteCustomJavascript.text)?.groupValues?.get(1)
            .toString()
        return AspData(dataAlternatifAsp,dataEmbedAsp)
    }
}
