// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class Sinefy : MainAPI() {
    override var mainUrl              = "https://sinefy3.com"
    override var name                 = "Sinefy"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries)
    override var sequentialMainPageScrollDelay = 50L  // ? 0.05 saniye

    override val mainPage = mainPageOf(
        "${mainUrl}/en-yenifilmler"        to  "Yeni Filmler",
        "${mainUrl}/netflix-filmleri-izle"        to  "Netflix Filmleri",
        "${mainUrl}/dizi-izle/netflix"         to   "Netflix Dizileri",
        "${mainUrl}/gozat/filmler/ulke/turkiye" to  "Türk Filmleri",
        "${mainUrl}/gozat/filmler/ulke/kore"   to  "Kore Filmleri",
        "${mainUrl}/gozat/filmler/belgesel"    to  "Belgesel",
        "${mainUrl}/gozat/filmler/aile"        to  "Aile Filmleri",
        "${mainUrl}/gozat/diziler/aile"        to  "Aile Dizileri",
        "${mainUrl}/gozat/filmler/aksiyon"     to  "Aksiyon Filmleri",
        "${mainUrl}/gozat/diziler/aksiyon"     to  "Aksiyon Dizileri",
        "${mainUrl}/gozat/filmler/animasyon"   to  "Animasyon Filmleri",
        "${mainUrl}/gozat/diziler/animasyon"   to  "Animasyon Dizileri",
        "${mainUrl}/gozat/filmler/bilim-kurgu" to  "Bilim Kurgu Filmleri",
        "${mainUrl}/gozat/filmler/dram"        to  "Dram Filmleri",
        "${mainUrl}/gozat/diziler/dram"        to  "Dram Dizileri",
        "${mainUrl}/gozat/filmler/fantastik"   to  "Fantastik Filmleri",
        "${mainUrl}/gozat/diziler/fantastik"   to  "Fantastik Dizileri",
        "${mainUrl}/gozat/filmler/gerilim"     to  "Gerilim Filmleri",
        "${mainUrl}/gozat/diziler/gerilim"     to  "Gerilim Dizileri",
        "${mainUrl}/gozat/filmler/gizem"       to  "Gizem Filmleri",
        "${mainUrl}/gozat/diziler/gizem"       to  "Gizem Dizileri",
        "${mainUrl}/gozat/filmler/komedi"      to  "Komedi Filmleri",
        "${mainUrl}/gozat/diziler/komedi"      to  "Komedi Dizileri",
        "${mainUrl}/gozat/filmler/korku"       to  "Korku Filmleri",
        "${mainUrl}/gozat/diziler/korku"       to  "Korku Dizileri",
        "${mainUrl}/gozat/filmler/macera"      to  "Macera Filmleri",
        "${mainUrl}/gozat/diziler/macera"      to  "Macera Dizileri",
        "${mainUrl}/gozat/filmler/muzik"       to  "Müzik Filmleri",
        "${mainUrl}/gozat/filmler/romantik"    to  "Romantik Filmleri",
        "${mainUrl}/gozat/diziler/romantik"    to  "Romantik Dizileri",
        "${mainUrl}/gozat/filmler/savas"       to  "Savaş Filmleri",
        "${mainUrl}/gozat/diziler/savas"       to  "Savaş Dizileri",
        "${mainUrl}/gozat/filmler/suc"         to  "Suç Filmleri",
        "${mainUrl}/gozat/diziler/suc"         to  "Suç Dizileri",
        "${mainUrl}/gozat/filmler/western"     to  "Western Filmleri",
        "${mainUrl}/gozat/diziler/western"     to  "Western Dizileri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get(request.data).document
        } else if (
            request.data.contains("/en-yenifilmler") ||
            request.data.contains("/netflix") ||
            request.data.contains("dizi-izle/netflix")
        ) {
            app.get("${request.data}/$page").document
            }
        else {
            app.get("${request.data}&page=${page}").document
        }

        val home     = document.select("div.poster-with-subject, div.dark-segment div.poster-md.poster").mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val srcset = this.selectFirst("img")?.attr("data-srcset")
        val posterUrl = srcset
            ?.split(",")
            ?.map { it.trim() }
            ?.firstOrNull { it.endsWith("1x") }
            ?.substringBefore(" ")
        Log.d("sinef", "poster $posterUrl")

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .add("cKey", "ca1d4a53d0f4761a949b85e51e18f096")
            .add("cValue", "MTc0NzI2OTAwMDU3ZTEwYmZjMDViNWFmOWIwZDViODg0MjU4MjA1ZmYxOThmZTYwMDdjMWQzMzliNzY5NzFlZmViMzRhMGVmNjgwODU3MGIyZA==")
            .add("searchTerm", query)
            .build()

        val request = Request.Builder()
            .url("$mainUrl/bg/searchcontent")
            .post(formBody)
            .addHeader("User-Agent", "Mozilla/5.0")
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .build()

        val response = client.newCall(request).execute()
        val raw = response.body.string()
        return extractResults(raw)
    }

    private fun extractResults(raw: String): List<SearchResponse> {
        // 1) Trimle ve '{' ile başlayan kısmı al
        val clean = raw
            .dropWhile { it.code <= 32 }
            .let { bs ->
                bs.indexOfFirst { it == '{' }
                    .takeIf { it >= 0 }
                    ?.let { bs.substring(it) }
                    ?: bs
            }

        // 2) Önce normal JSON parse dene
        val root = try {
            JSONObject(clean)
        } catch (_: Exception) {
            null
        }

        // 3) "result" dizisini elde etme
        val resultArray: JSONArray? = root
            ?.optJSONObject("data")
            ?.optJSONArray("result")
            ?: run {
                // Fallback: root yoksa veya data/result eksikse,
                // temiz metin içinde ilk "result":[ … ] kısmını elle ayıkla:
                parseResultManually(clean)
            }

        if (resultArray == null) {
            Log.e("arama", "Result dizisi bulunamadı")
            return emptyList()
        }

        // 4) Son adım: her öğeyi JSON’dan çek
        val results = mutableListOf<SearchResponse>()
        for (i in 0 until resultArray.length()) {
            val item = resultArray.optJSONObject(i) ?: continue

            val name      = item.optString("object_name", "").takeIf(String::isNotBlank) ?: continue
            val rawSlug   = item.optString("used_slug", "")
            val slug      = rawSlug.replace("\\/", "/")
            val t         = item.optString("used_type", "Movies")
            val type      = when {
                t.equals("Series",      true) ||
                        t.equals("TvSeries",    true) ||
                        t.equals("MovieSeries", true) -> TvType.TvSeries
                else                           -> TvType.Movie
            }
            val year      = item.optInt("object_release_year", 0).takeIf { it > 0 }
            val rawPosterUrl = item.optString("object_poster_url", null)
                // Eğer AMP cache URL geldiyse, kendi sunucuna çevir; değilse orijinali kullan
               val posterUrl = rawPosterUrl.let { url ->
                   if (url.contains("cdn.ampproject.org")) {
                       // Son segmenti alıp orijinal yolunu oluştur
                       url.substringAfterLast('/').takeIf(String::isNotBlank)?.let { filename ->
                           "https://images.macellan.online/images/movie/poster/180/275/80/$filename"
                       }
                   } else {
                       // Genellikle zaten doğru, sadece escaped slash’ları düzelt
                       url.replace("\\/", "/")
                   }
               }

            Log.d("arama", "[$i] $name ⇒ posterUrl=$posterUrl")

            val href = when {
                slug.startsWith("http") -> slug
                slug.startsWith("/")    -> "$mainUrl$slug"
                else                     -> "$mainUrl/$slug"
            }

            results += newMovieSearchResponse(name, href, type) {
                this.year      = year
                this.posterUrl = posterUrl
            }
        }
        Log.d("arama", "Total results: ${results.size}")
        return results
    }

    private fun parseResultManually(s: String): JSONArray? {
        val key = "\"result\""
        val pos = s.indexOf(key).takeIf { it >= 0 } ?: return null
        val start = s.indexOf('[', pos).takeIf { it >= 0 } ?: return null

        var depth = 0
        for (i in start until s.length) {
            when (s[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        val arrText = s.substring(start, i + 1)
                        return try {
                            JSONArray(arrText)
                        }
                        catch (_: Exception) {
                            null
                        }
                    }
                }
            }
        }
        return null
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val srcset          = document.selectFirst("div.ui.items img")?.attr("data-srcset")
        val posterUrl = srcset
            ?.split(",")
            ?.map { it.trim() }
            ?.firstOrNull { it.endsWith("1x") }
            ?.substringBefore(" ")
        val description     = document.selectFirst("p#tv-series-desc")?.text()?.trim()
        val year            = document.selectFirst("table.ui > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(5) > div:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.item.categories a").map { it.text() }
        val rating          = document.selectFirst("span.color-imdb")?.text()?.trim()?.toRatingInt()
        val duration        = document.selectFirst("table.ui > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(6) > div:nth-child(2)")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.srelacionados article").mapNotNull { it.toRecommendationResult() }
        val actorNames      = document.select("div.content h5").eachText().map { Actor(it) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }
        val seasonLinks = document.select("section.episodes-box")
        return if (seasonLinks.isNotEmpty()) {
            val episodes = mutableListOf<Episode>()
            for (seasonLink in seasonLinks) {
                val hrefList = seasonLink.select("div.ui.vertical.fluid.tabular.menu a").eachAttr("href")
                val seasonVarList = hrefList.map { fixUrl(it) + "/bolum-1" }
                Log.d("sinefy","seasonlar $seasonVarList")
                seasonVarList.forEach { seasonUrl ->
                    val seasonDocument = app.get(seasonUrl).document
                    val text = seasonDocument.selectFirst("span.light-title")?.text()?.trim() ?: ""
                    val seasonNumber = Regex("""(\d+)\.\s*Sezon""").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 1

                    seasonDocument.select("div.swiper-slide.ss-episode").forEach { episodeElement ->
                        val episodeNumber = episodeElement.attr("data-episode").toIntOrNull()
                        val epHref = episodeElement.selectFirst("a.episode-link")?.attr("href")
                        val epName = episodeElement.selectFirst("h3")?.text()?.trim()

                        if (epHref != null && epName != null && episodeNumber != null) {
                            episodes.add(
                                newEpisode(fixUrl(epHref)) {
                                    name = epName
                                    season = seasonNumber
                                    episode = episodeNumber // ✅ doğru olan bu
                                }
                            )
                        }
                    }
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = description
                this.year = year
                this.tags = tags
                this.rating = rating
                this.duration = duration
                this.recommendations = recommendations
                addActors(actorNames)
                addTrailer(trailer)
            }
        } else {
            // Sezon linkleri yoksa film olarak işle
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = posterUrl
                this.plot = description
                this.year = year
                this.tags = tags
                this.rating = rating
                this.duration = duration
                this.recommendations = recommendations
                addActors(actorNames)
                addTrailer(trailer)
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("a img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("sinefy", "data » ${data}")
        val document = app.get(data).document

        val iframe   = fixUrlNull(document.selectFirst("iframe")?.attr("src")).toString()


        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}