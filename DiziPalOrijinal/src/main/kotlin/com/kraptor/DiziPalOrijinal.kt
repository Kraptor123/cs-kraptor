// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.AesHelper
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class DiziPalOrijinal : MainAPI() {
    override var mainUrl              = "https://dizipal932.com"
    override var name                 = "DiziPalOrijinal"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Movie)
    //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,

    override var sequentialMainPage = true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
     override var sequentialMainPageDelay       = 250L // ? 0.25 saniye
     override var sequentialMainPageScrollDelay = 250L // ? 0.25 saniye

    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request  = chain.request()
            val response = chain.proceed(request)
            val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

            if (doc.html().contains("Just a moment") || doc.html().contains("verifying")) {
                Log.d("kraptor_Dizipal", "!!cloudflare geldi!!")
                return cloudflareKiller.intercept(chain)
            }

            return response
        }
    }


    override val mainPage = mainPageOf(
        ""   to "Yeni Eklenenler",
        ""   to "Yüksek Imdb Puanlı Diziler",
        "1"   to "Exxen Dizileri",
        "6"   to "Disney+ Dizileri",
        "10"  to "Netflix Dizileri",
        "53"  to "Amazon Dizileri",
        "54"  to "Apple TV+ Dizileri",
        "66"  to "Max Dizileri",
        "78"  to "Hulu Dizileri",
        "181" to "TOD Dizileri",
        "242" to "Tabii Dizileri",
        "19"  to "Anime",
    )

    private var sessionCookies: Map<String, String>? = null
    private var cKey: String? = null
    private var cValue: String? = null
    private val initMutex = Mutex()

    private suspend fun initSession() {
        if (sessionCookies != null && cKey != null && cValue != null) return
        initMutex.withLock {
            if (sessionCookies != null && cKey != null && cValue != null) return@withLock

            Log.d("kraptor_Dizipal", "🔄 Oturum başlatılıyor: cookie, cKey ve cValue alınıyor")

            val resp = app.get(mainUrl, interceptor = interceptor, timeout = 120)
            sessionCookies = resp.cookies.mapValues { (_, v) -> URLDecoder.decode(v, "UTF-8") }

            val document = resp.document
            cKey = document.selectFirst("input[name=cKey]")?.`val`()
            cValue = document.selectFirst("input[name=cValue]")?.`val`()

            Log.d("kraptor_Dizipal", "cKey: $cKey, cValue: ${cValue}")
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        initSession()
        val kanallarliste = listOf(
            "Exxen Diziler",
            "Disney+ Dizileri",
            "Netflix Dizileri",
            "Amazon Dizileri",
            "Apple TV+ Dizileri",
            "Max Dizileri",
            "Hulu Dizileri",
            "TOD Dizileri",
            "Tabii Dizileri")

        val response = if (kanallarliste.any { request.name.contains(it) }) {
            app.post(
                "${mainUrl}/bg/getserielistbychannel", data = mapOf(
                    "cKey" to "$cKey", "cValue" to "$cValue",
                    "curPage" to "$page",
                    "channelId" to request.data,
                    "languageId" to "2,3,4"
                )
            )

        } else if (request.name.contains("Yeni Eklenenler")) {
         app.post("${mainUrl}/bg/findseries",data = mapOf(
             "cKey" to "$cKey",
             "cValue" to "$cValue",
             "currentPage" to "$page",
             "categoryIdsComma[]" to request.data,
             "releaseYearStart" to "1923",
             "releaseYearEnd"  to "2025",
             "orderType" to "date_asc")
         )
        }else {
            app.post("${mainUrl}/bg/findseries",data = mapOf(
                "cKey" to "$cKey",
                "cValue" to "$cValue",
                "currentPage" to "$page",
                "categoryIdsComma[]" to request.data,
                "releaseYearStart" to "1923",
                "releaseYearEnd"  to "2025",
                "orderType" to "imdb_desc"
            )
            )
        }

        val bodyText = response.text

        val htmlFragment = if (bodyText.trimStart().startsWith("{")) {
            // JSON içinde data.html var
            JSONObject(bodyText)
                .getJSONObject("data")
                .getString("html")
        } else {
            // Direkt HTML döndü, JSON yok
            bodyText
        }

        val doc = Jsoup.parseBodyFragment(htmlFragment)

        val home = doc.select("div.prm-borderb")
            .mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("img")?.attr("alt") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        initSession()
        val responseBody = app.post("$mainUrl/bg/searchcontent", data = mapOf(
            "cKey"       to cKey!!,
            "cValue"     to cValue!!,
            "searchterm" to query
        )).text

        // 2) JSONObject ile parse
        val json       = JSONObject(responseBody)
        Log.d("kraptor_Dizipal", "json: $json")
        val data       = json.getJSONObject("data")
        val resultList = data.optJSONArray("result") ?: return emptyList()

        // 3) Her bir sonucu map edip SearchResponse’a çeviriyoruz
        return (0 until resultList.length()).mapNotNull { i ->
            val item = resultList.getJSONObject(i)

            // Sadece Series tipinde olanları almak istersen burayı açabilirsin:
            // if (item.optString("used_type") != "Series") return@mapNotNull null

            val title     = item.optString("object_name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Log.d("kraptor_Dizipal", "title: $title")
            val slug      = item.optString("used_slug").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Log.d("kraptor_Dizipal", "slug: $slug")
            val href      = fixUrlNull("$mainUrl/$slug") ?: return@mapNotNull null
            Log.d("kraptor_Dizipal", "href: $href")
            val posterUrl = item.optString("object_poster_url").takeIf { it.isNotBlank() }

            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("img.w-full.h-full.object-cover")?.attr("src"))
        val description     = document.selectFirst("p.text-white.text-base")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("ul.rigth-content > li:nth-child(5) a").map { it.text() }
        val rating          = document.selectFirst("ul.rigth-content > li:nth-child(3) div.value")?.text()?.trim()?.toRatingInt()
        val duration        = document.selectFirst("ul.rigth-content > li:nth-child(8) > div.value")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val bolumler        = document.select("a.text.block").map { bolumler ->
            val bolumHref   = bolumler.attr("href")
            val bolumName   = "Bölüm"
            val bolumEpisode =
                bolumler.selectFirst("div.text-white.text-sm.opacity-80.font-light")?.text()
                    ?.substringBeforeLast(".")
                    ?.substringAfterLast(" ")
                    ?.toIntOrNull()
            val bolumSeason = bolumler.selectFirst("div.text-white.text-sm.opacity-80.font-light")
                ?.text()
                ?.substringBefore(".")?.toIntOrNull()
            newEpisode(bolumHref, {
                this.name = bolumName
                this.season = bolumSeason
                this.episode = bolumEpisode
            })
        }


        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, bolumler) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.rating          = rating
            this.duration        = duration
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document
        val hiddenJson = document.selectFirst("div[data-rm-k]")!!.text()
        val key = "3hPn4uCjTVtfYWcjIcoJQ4cL1WWk1qxXI39egLYOmNv6IblA7eKJz68uU3eLzux1biZLCms0quEjTYniGv5z1JcKbNIsDQFSeIZOBZJz4is6pD7UyWDggWWzTLBQbHcQFpBQdClnuQaMNUHtLHTpzCvZy33p6I7wFBvL4fnXBYH84aUIyWGTRvM2G5cfoNf4705tO2kv"
        val obj = JSONObject(hiddenJson)
        val ciphertext = obj.getString("ciphertext")
        val iv         = obj.getString("iv")
        val salt       = obj.getString("salt")
//        Log.d("kraptor_$name", "ciphertext = $ciphertext iv = $iv")
        try {
            val decryptedContent = decrypt(key, salt, iv, ciphertext)
            val iframe           = fixUrlNull(decryptedContent).toString()
            Log.d("kraptor_$name", "iframe = $iframe")

             loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)


        } catch (e: Exception) {
            Log.e("kraptor_$name", "Decryption failed: ${e.message}")
            return false
        }

        return true
    }
}

fun decrypt(
    passphrase: String,
    saltHex: String,
    ivHex: String,
    ciphertextBase64: String
): String {
    val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val iv = ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
    val spec = PBEKeySpec(passphrase.toCharArray(), salt, 999, 256)
    val tmp = factory.generateSecret(spec)
    val secret = SecretKeySpec(tmp.encoded, "AES")

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secret, IvParameterSpec(iv))

    val decoded = base64DecodeArray(ciphertextBase64)
    val plaintextBytes = cipher.doFinal(decoded)
    return String(plaintextBytes, Charsets.UTF_8)
}