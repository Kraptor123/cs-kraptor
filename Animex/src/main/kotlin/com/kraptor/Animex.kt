// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class Animex : MainAPI() {
    override var mainUrl = "https://animex.tr"
    override var name = "Animex"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Anime)


    override val mainPage = mainPageOf(
        "${mainUrl}/animeler/" to "Animeler",
        "${mainUrl}/film/" to "Filmler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get(request.data).document
        } else {
            app.get("${request.data}/page/$page/").document
        }
        val home = document.select("div.poster.poster-md").mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text() ?: return null
        Log.d("Anx", "title = ${title}")
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.poster-media img")?.attr("data-src"))
        Log.d("Anx", "poster = ${posterUrl}")

        return newAnimeSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.poster.poster-md").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newAnimeSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.page-title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.ui.items img")?.attr("src"))
        val description = document.selectFirst("p#tv-series-desc")?.text()?.trim()
        val text = document.selectFirst(".genre-item")?.text() ?: ""
        val year = Regex("""\b\d{4}\b""").find(text)?.value?.toInt()
        val tags = document.select("div.nano-content a").map { it.text() }
        val rating = document.selectFirst("div.color-imdb")?.text()?.trim()?.toRatingInt()
        val duration =
            document.selectFirst("table.ui > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > div:nth-child(2)")
                ?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val trailer = Regex("""embed/(.*)\?rel""").find(document.html())?.groupValues?.get(1)
            ?.let { "https://www.youtube.com/embed/$it" }
        val episodeListesi = document.select("div.ajax_post a").mapNotNull { bolumElemanlari ->
//            val epTitle = document.selectFirst("span.episode-names")?.text()?.trim() ?: return null
            val epHref = fixUrlNull(document.selectFirst("div.ajax_post a")?.attr("href"))
            newEpisode(epHref) {
                this.name = "bölüm"
            }
        }.let { list ->
            mutableMapOf(DubStatus.Subbed to list)
        }

        return if (url.contains("/film/"))
            newMovieLoadResponse(title, url, type = TvType.AnimeMovie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.rating = rating
                this.duration = duration
                addTrailer(trailer)

            }
        else
            newAnimeLoadResponse(title, url, TvType.Anime) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.rating = rating
                this.duration = duration
                addTrailer(trailer)
                this.episodes = episodeListesi
            }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data, referer = data).document
        val iframeUrl = fixUrlNull(
            document.selectFirst("div.play_top li.belink a")?.attr("data-frame")
        )
        Log.d("Animex", "iframeUrl: $iframeUrl")
        iframeUrl ?: return false

        if (iframeUrl.contains("animtube")) {
            try {
                val iframeDoc = app.get(iframeUrl, referer = mainUrl).document
                val scriptNodes = iframeDoc.select("script").mapNotNull { it.html() }
                val script = scriptNodes.firstOrNull { it.contains("bePlayer") }
                    ?: throw Exception("bePlayer script bulunamadı")

                val regex = Regex("""bePlayer\(\s*['"]([^'"]+)['"]\s*,\s*(['"]\{.*?\}['"])""")
                val match = regex.find(script)
                if (match == null) throw Exception("bePlayer parametreleri bulunamadı")

                val hash = match.groupValues[1]
                val setJsonRaw = match.groupValues[2]
                    .removePrefix("\"")
                    .removePrefix("'")
                    .removeSuffix("\"")
                    .removeSuffix("'")
                val decrypted = decryptSetParams(setJsonRaw, hash)
                val videoLocation = JSONObject(decrypted).getString("video_location")
                callback.invoke(
                    newExtractorLink(
                        url = videoLocation,
                        source = "animtube",
                        name = "Animtube HLS",
                        type = ExtractorLinkType.M3U8
                    ) {
                        quality = Qualities.Unknown.value
                        referer = iframeUrl
                    }
                )
                return true

            } catch (_: Exception) {
                return false
            }
        }

        loadExtractor(iframeUrl, data, subtitleCallback, callback)
        return true
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val out = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            out[i / 2] = ((Character.digit(hex[i], 16) shl 4)
                    + Character.digit(hex[i+1], 16)).toByte()
        }
        return out
    }

    private fun evpBytesToKey(
        password: ByteArray,
        salt: ByteArray,
        keyLen: Int,
        ivLen: Int
    ): Pair<ByteArray, ByteArray> {
        val md5 = MessageDigest.getInstance("MD5")
        var prev = ByteArray(0)
        val result = mutableListOf<Byte>()
        while (result.size < keyLen + ivLen) {
            md5.reset()
            md5.update(prev)
            md5.update(password)
            md5.update(salt)
            val digest = md5.digest()
            result += digest.toTypedArray()
            prev = digest
        }
        val key = result.take(keyLen).toByteArray()
        val iv  = result.drop(keyLen).take(ivLen).toByteArray()
        return Pair(key, iv)
    }

    private fun decryptSetParams(setJson: String, hash: String): String {
        try {
            val obj = JSONObject(setJson)
            val ctB64 = obj.getString("ct")
            val ivHex = obj.getString("iv")
            val sHex  = obj.getString("s")
            val cipherText = Base64.decode(ctB64, Base64.DEFAULT)
            val saltBytes  = hexStringToByteArray(sHex)
            val password   = hash.toByteArray(StandardCharsets.UTF_8)
            val (key, iv) = evpBytesToKey(password, saltBytes, 32, 16)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val plain = cipher.doFinal(cipherText)
            val result = String(plain, StandardCharsets.UTF_8)
            return result
        } catch (e: Exception) {
            throw e
        }
    }
}


