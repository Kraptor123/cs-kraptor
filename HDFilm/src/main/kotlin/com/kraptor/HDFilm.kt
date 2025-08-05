// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Base64
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class HDFilm : MainAPI() {
    override var mainUrl = "https://hdfilm.cx"
    override var name = "HDFilm"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/filmizle/aile-filmleri-izle" to "Aile Filmleri",
        "${mainUrl}/filmizle/aksiyon-filmleri-izle" to "Aksiyon Filmleri",
        "${mainUrl}/filmizle/animasyon-filmleri-izle" to "Animasyon Filmleri",
        "${mainUrl}/filmizle/belgesel-filmleri-izle" to "Belgeseller",
        "${mainUrl}/filmizle/bilim-kurgu-filmleri-izle" to "Bilim Kurgu Filmleri",
        "${mainUrl}/filmizle/bluray-filmler-izle" to "Blu Ray Filmler",
        "${mainUrl}/filmizle/cizgi-filmler-izle" to "Çizgi Filmler",
        "${mainUrl}/filmizle/dram-filmleri-izle" to "Dram Filmleri",
        "${mainUrl}/filmizle/fantastik-filmler-izle" to "Fantastik Filmler",
        "${mainUrl}/filmizle/gerilim-filmleri-hd-izle" to "Gerilim Filmleri",
        "${mainUrl}/filmizle/gizem-filmleri-izle" to "Gizem Filmleri",
        "${mainUrl}/filmizle/hint-filmleri-izle" to "Hint Filmleri",
        "${mainUrl}/filmizle/komedi-filmleri-hd-izle" to "Komedi Filmleri",
        "${mainUrl}/filmizle/korku-filmleri-izle" to "Korku Filmleri",
        "${mainUrl}/filmizle/macera-filmleri-izle" to "Macera Filmleri",
        "${mainUrl}/filmizle/muzikal-filmler-izle" to "Müzikal Filmler",
        "${mainUrl}/filmizle/polisiye-filmleri-izle" to "Polisiye Filmleri",
        "${mainUrl}/filmizle/psikolojik-filmler-izle" to "Psikolojik Filmler",
        "${mainUrl}/filmizle/romantik-filmler-hd-izle" to "Romantik Filmler",
        "${mainUrl}/filmizle/savas-filmleri-izle" to "Savaş Filmleri",
        "${mainUrl}/filmizle/suc-filmleri-izle" to "Suç Filmleri",
        "${mainUrl}/filmizle/tarih-filmleri-izle" to "Tarih Filmleri",
        "${mainUrl}/filmizle/western-filmler-hd-izle-2" to "Western Filmler",
        "${mainUrl}/filmizle/yerli-film-izle" to "Yerli Filmler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page").document
        val home = document.select("li.film").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("span.kt")?.text()
            ?: this.selectFirst("span.film-title")?.text()
            ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/${query}").document

        return document.select("li.film").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("span.kt")?.text()
            ?: this.selectFirst("span.film-title")?.text()
            ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    private fun String.rot13(): String = buildString {
        for (c in this@rot13) {
            when (c) {
                in 'A'..'Z' -> append(((c - 'A' + 13) % 26 + 'A'.code).toChar())
                in 'a'..'z' -> append(((c - 'a' + 13) % 26 + 'a'.code).toChar())
                else -> append(c)
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.izle-content img")?.attr("data-src"))
        val description = document.selectFirst("div.ozet-ic")?.text()?.trim()
        val year = document.selectFirst("div.dd a.category")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.film-info > ul:nth-child(1) > li:nth-child(4) a").map { it.text() }
        val rating = document.selectFirst("div.imdb-ic > span")?.text()?.trim()
        val actors = document.select("div.film-info > ul:nth-child(1) > li:nth-child(2) > div:nth-child(2) a")
            .map { Actor(it.text()) }
        val trailer = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(rating)
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("HDFilm", "data » $data")
        val document = app.get(data).text

        // Aranacak tipler: Tek Part (video), TR Dublaj, TR Altyazı
        val pattern = Regex(
            """"(Tek Part|a|(?:TR )?Dublaj|(?:TR )?Altyazı(?: HD)?)":"([^"]*)"""",
            RegexOption.IGNORE_CASE
        )

        pattern.findAll(document).forEach { match ->
            val type = match.groupValues[1]    // "Tek Part" veya "TR Dublaj" vs.
            val encoded = match.groupValues[2] // Base64+rot13 karışık
            // önce rot13, sonra base64 decode
            val decodedRot = encoded.rot13()
            val decodedBytes = Base64.decode(decodedRot, Base64.DEFAULT)
            val decoded = decodedBytes.toString(Charsets.UTF_8)
            val decodedB64 = decoded
                .replace("text=", "")
                .replace("[", "")
                .replace("]", "")
                .trimEnd()

            Log.d("HDFilm", "$type = $decodedB64")

            loadExtractor(decodedB64, "$mainUrl/", subtitleCallback, callback)
            return true
        }
        return true
    }
}