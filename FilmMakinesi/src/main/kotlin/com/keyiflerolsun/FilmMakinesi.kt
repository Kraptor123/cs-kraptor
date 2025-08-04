// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Base64
import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Element
import java.nio.charset.StandardCharsets


class FilmMakinesi : MainAPI() {
    override var mainUrl              = "https://filmmakinesi.de"
    override var name                 = "FilmMakinesi"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries)

    // ! CloudFlare bypass
    override var sequentialMainPage            = true // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay       = 50L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 50L  // ? 0.05 saniye

    override val mainPage = mainPageOf(
        "${mainUrl}/filmler/"                    to "Son Filmler",
        "${mainUrl}/kanal/netflix/"                          to "Netflix",
        "${mainUrl}/kanal/disney/"                           to "Disney",
        "${mainUrl}/kanal/amazon/"                           to "Amazon",
        "${mainUrl}/film-izle/olmeden-izlenmesi-gerekenler/" to "Ölmeden İzle",
        "${mainUrl}/film-izle/aksiyon-filmleri-izle/"        to "Aksiyon",
        "${mainUrl}/film-izle/bilim-kurgu-filmi-izle/"       to "Bilim Kurgu",
        "${mainUrl}/film-izle/macera-filmleri/"              to "Macera",
        "${mainUrl}/film-izle/komedi-filmi-izle/"            to "Komedi",
        "${mainUrl}/film-izle/romantik-filmler-izle/"        to "Romantik",
        "${mainUrl}/film-izle/belgesel/"                     to "Belgesel",
        "${mainUrl}/film-izle/fantastik-filmler-izle/"       to "Fantastik",
        "${mainUrl}/film-izle/polisiye-filmleri-izle/"       to "Polisiye Suç",
        "${mainUrl}/film-izle/korku-filmleri-izle-hd/"       to "Korku",
        // "${mainUrl}/film-izle/savas/page/"                        to "Tarihi ve Savaş",
        // "${mainUrl}/film-izle/gerilim-filmleri-izle/page/"        to "Gerilim Heyecan",
        // "${mainUrl}/film-izle/gizemli/page/"                      to "Gizem",
        // "${mainUrl}/film-izle/aile-filmleri/page/"                to "Aile",
        // "${mainUrl}/film-izle/animasyon-filmler/page/"            to "Animasyon",
        // "${mainUrl}/film-izle/western/page/"                      to "Western",
        // "${mainUrl}/film-izle/biyografi/page/"                    to "Biyografik",
        // "${mainUrl}/film-izle/dram/page/"                         to "Dram",
        // "${mainUrl}/film-izle/muzik/page/"                        to "Müzik",
        // "${mainUrl}/film-izle/spor/page/"                         to "Spor"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val base = request.data.trimEnd('/')
        val url = if (page == 1) base else "$base/sayfa/$page/"
        val document = app.get(url).document
        val home = document.select("div.item-relative").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.item-relative a.item")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.thumbnail-outer img.thumbnail")?.attr("src")) ?: fixUrlNull(this.selectFirst("img.thumbnail")?.attr("src"))
        val puan      = this.selectFirst("div.rating")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
        }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val title     = this.select("div.title").last()?.text() ?: return null
        val href      = fixUrlNull(this.select("div.item-relative a.item").last()?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.thumbnail-outer img.thumbnail")?.attr("src"))
        val puan      = this.selectFirst("div.rating")?.text()?.trim()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.score     = Score.from10(puan)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/?s=${query}").document
        Log.d("kraptor_$name", "arama = $document")
        return document.select("div.item-relative").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title           = document.selectFirst("div.content h1.title")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description     = document.select("div.info-description p").last()?.text()?.trim()
        val tags            = document.select("div.type a").map { it.text() }
        val imdbScore          = document.selectFirst("div.info b")?.text()?.trim()
        val year            = document.selectFirst("span.date a")?.text()?.trim()?.toIntOrNull()

        val durationText = document.selectFirst("div.time")?.text()?.trim() ?: ""
        val duration = if (durationText.startsWith("Süre:")) {
            // "Süre: 155 Dakika" gibi bir metni işliyoruz
            val durationValue = durationText.removePrefix("Süre:").trim().split(" ")[0]
            durationValue.toIntOrNull() ?: 0
        } else {
            0
        }
        val recommendations = document.select("div.item-relative").mapNotNull { it.toRecommendResult() }
        val actors = document.select("div.content a.cast")  // Tüm a.cast öğelerini al
            .map { Actor(it.text().trim()) }  // Her birini Actor nesnesine dönüştür

        val trailer = document.selectFirst("a.trailer-button")?.attr("data-video_url")

        val bolumler = document.select("div.col-12.col-sm-6.col-md-3").map { bolum ->
            val bolumHref = fixUrlNull(bolum.selectFirst("a")?.attr("href")).toString()
            val bolumName = bolum.selectFirst("div.ep-details span")?.text() ?: bolum.selectFirst("div.ep-title")?.text()
            val bolum     = bolumHref.substringAfter("bolum-").substringBefore("/").toIntOrNull()
            val sezon     = bolumHref.substringAfter("sezon-").substringBefore("/").toIntOrNull()
            newEpisode(bolumHref,{
                this.name = bolumName
                this.episode = bolum
                this.season  = sezon
            })

        }

        return if (url.contains("dizi")){
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, bolumler) {
                this.posterUrl       = poster
                this.year            = year
                this.plot            = description
                this.tags            = tags
                this.score = Score.from10(imdbScore)
                this.duration        = duration
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        }else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl       = poster
                this.year            = year
                this.plot            = description
                this.tags            = tags
                this.score = Score.from10(imdbScore)
                this.duration        = duration
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        }
        }



    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data » $data")
        val document      = app.get(data).document
//        Log.d("kraptor_$name", "document = $document")
        val iframe = document.selectFirst("div.after-player iframe")?.attr("data-src") ?: ""
        Log.d("kraptor_$name", "iframe = $iframe")
        val iframeGet = app.get(iframe, referer = "${mainUrl}/").document
        val scripts = iframeGet.getElementsByTag("script")
        val scriptAl  = scripts.filter { it.html().contains("eval(") }
        val rawscript = scriptAl[0].html()
        Log.d("kraptor_$name", "scriptAl = $scriptAl")
        val scriptUnpack = getAndUnpack(rawscript)
        Log.d("kraptor_$name", "scriptUnpack = $scriptUnpack")
        val dchelloVar = if (scriptUnpack.contains("dc_hello")) {
            "var"
        } else {
            "yok"
        }
        val dcRegex = if (dchelloVar.contains("var")) {
            Regex(pattern = "dc_hello\\(\"([^\"]*)\"\\)", options = setOf(RegexOption.IGNORE_CASE))
        } else {
            Regex("""dc_[a-zA-Z0-9_]+\(\[(.*?)\]\)""", RegexOption.DOT_MATCHES_ALL)
        }
        val match = dcRegex.find(scriptUnpack)
//        Log.d("kraptor_$name", "match $match")

        val realUrl = if (dchelloVar.contains("var")) {
            val parts      = match?.groupValues[1].toString()
            Log.d("kraptor_$name", "parts $parts")
            val decodedUrl = dcHello(parts)
            Log.d("kraptor_$name", "decodedUrl $decodedUrl")
            decodedUrl
        } else{
            val parts = match!!.groupValues[1]
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
            Log.d("kraptor_$name", "dc parts: $parts")
            val decodedUrl = dcDecode(parts)
            Log.d("kraptor_$name", "decoded URL: $decodedUrl")
            decodedUrl
        }
        val refererSon = if (realUrl.contains("cdnimages")) {
            "https://hdfilmcehennemi.mobi/"
        } else {
            "${mainUrl}/"
        }

        callback.invoke(newExtractorLink(
            source = this.name,
            name = this.name,
            url = realUrl,
            type = ExtractorLinkType.M3U8,
            {
                this.referer = refererSon
                quality = Qualities.Unknown.value
            }
        ))

        return true
    }
}

fun dcDecode(valueParts: List<String>): String {
    try {
        // 1) Join array elements (matches: let value = value_parts.join(''))
        var result = valueParts.joinToString(separator = "")

        // 2) ROT13 transformation (matches the replace function in JS)
        result = result.map { c ->
            when {
                c in 'a'..'z' -> {
                    val shifted = c.code + 13
                    if (shifted <= 'z'.code) shifted.toChar() else (shifted - 26).toChar()
                }
                c in 'A'..'Z' -> {
                    val shifted = c.code + 13
                    if (shifted <= 'Z'.code) shifted.toChar() else (shifted - 26).toChar()
                }
                else -> c
            }
        }.joinToString("")

        // 3) Base64 decode (matches: atob(result))
        val decodedBytes = try {
            Base64.decode(result, Base64.DEFAULT)
        } catch (e: Exception) {
            return "" // Handle decode errors
        }

        // 4) Reverse the bytes (matches: split('').reverse().join(''))
        val reversedBytes = decodedBytes.reversedArray()

        // 5) Un-mix: Apply character transformation on bytes
        val unmixedBytes = ByteArray(reversedBytes.size)
        for (i in reversedBytes.indices) {
            val byteValue = reversedBytes[i].toInt() and 0xFF // Convert to unsigned byte
            val delta = 399756995 % (i + 5)
            // Match JS logic: (charCode - delta + 256) % 256
            val transformedByte = (byteValue - delta + 256) % 256
            unmixedBytes[i] = transformedByte.toByte()
        }

        // Convert final bytes to string using Latin-1 encoding (matches JavaScript behavior)
        return String(unmixedBytes, StandardCharsets.ISO_8859_1)

    } catch (e: Exception) {
        return "" // Handle any other errors
    }
}

fun dcHello(encoded: String): String {
    // İlk Base64 çöz
    val firstDecoded = base64Decode(encoded)
    Log.d("kraptor_hdfilmcehennemi", "firstDecoded $firstDecoded")
    // Ters çevir
    val reversed = firstDecoded.reversed()
    Log.d("kraptor_hdfilmcehennemi", "reversed $reversed")
    // İkinci Base64 çöz
    val secondDecoded = base64Decode(reversed)

    val gercekLink    = secondDecoded.substringAfter("http")
    val sonLink       = "http$gercekLink"
    Log.d("kraptor_hdfilmcehennemi", "sonLink $sonLink")
    return sonLink.trim()

}