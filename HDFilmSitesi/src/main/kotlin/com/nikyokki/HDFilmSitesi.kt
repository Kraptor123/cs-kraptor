package com.nikyokki

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.Actor
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.toRatingInt
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class HDFilmSitesi : MainAPI() {
    override var mainUrl = "https://hdfilmsitesi.pro"
    override var name = "HDFilmSitesi"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/filmizle/aile-filmleri-izle" to "Aile",
        "${mainUrl}/filmizle/aksiyon-filmleri-izle" to "Aksiyon",
        "${mainUrl}/filmizle/animasyon-filmleri-hd-izle" to "Animasyon",
        "${mainUrl}/filmizle/bilim-kurgu-filmleri-izle" to "Bilim Kurgu",
        "${mainUrl}/filmizle/belgesel-filmleri-izle" to "Belgesel",
        "${mainUrl}/filmizle/dram-filmleri-izle" to "Dram",
        "${mainUrl}/filmizle/fantastik-filmler-izle" to "Fantastik",
        "${mainUrl}/filmizle/gerilim-filmleri-hd-izle" to "Gerilim",
        "${mainUrl}/filmizle/gizem-filmleri-izle" to "Gizem",
        "${mainUrl}/filmizle/komedi-filmleri-hd-izle" to "Komedi",
        "${mainUrl}/filmizle/korku-filmleri-izle" to "Korku",
        "${mainUrl}/filmizle/macera-filmleri-izle" to "Macera",
        "${mainUrl}/filmizle/romantik-filmler-hd-izle" to "Romantik",
        "${mainUrl}/filmizle/savas-filmleri-izle" to "Savaş",
        "${mainUrl}/filmizle/suc-filmleri-izle" to "Suç",
        "${mainUrl}/filmizle/western-filmler-hd-izle-2" to "Western",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/${page}/").document
        val home = document.select("div.movie_box").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("h2")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/${query}").document

        return document.select("div.movie_box").mapNotNull { it.toMainPageResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title =
            document.selectFirst("h1 span")?.text()?.substringBefore(" izle")?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description =
            document.selectFirst("div[itemprop='description']")?.text()?.substringAfter("⭐")
                ?.substringAfter("izleyin.")?.substringAfter("konusu:")?.trim()
        val year = document.selectFirst("span[itemprop='name']")?.text()?.trim()?.toIntOrNull()
        val tags =
            document.select("a[rel='category']").map { it.text().substringBefore(" Filmleri") }
        val rating =
            document.selectFirst("div.puanlar span")?.text()?.trim()?.substringAfter("IMDb")
                .toRatingInt()
        val duration =
            document.selectFirst("span[itemprop='duration']")?.text()?.split(" ")?.first()?.trim()
                ?.toIntOrNull()
        val actors = document.select("a.cast").map { Actor(it.text(), it.attr("href")) }
        val trailer = fixUrlNull(document.selectFirst("[property='og:video']")?.attr("content"))

        if (document.selectFirst("div.part_buton_sec")?.text()?.contains("Sezon") == true) {
            val episodes = mutableListOf<Episode>()

            val iframeSkici = IframeKodlayici()

            val pdataMatches = Regex("""pdata\[\'(.*?)'\] = \'(.*?)\';""").findAll(document.html())
            val pdataList = pdataMatches.map { it.destructured }.toList()

            for (pdata in pdataList) {
                val key = pdata.component1();
                val value = pdata.component2();

                val iframeData = iframeSkici.iframeCoz(value!!)
                val iframeLink = app.get(iframeData, referer = "${mainUrl}/").url.toString()

                //val sz_num = partNumber.takeIf { it.contains("sezon") }?.substringBefore("sezon")?.toIntOrNull() ?: 1
                //val ep_num = partName.substringBefore(".")?.trim()?.toIntOrNull() ?: 1

                val sz_num = key.substringAfter("prt_").substringBefore("sezon").toIntOrNull() ?: 1
                var ep_num = key.substringAfter("sezon").toIntOrNull()
                if (ep_num != null) {
                    ep_num += 1
                } else {
                    ep_num = 1
                }

                episodes.add(
                    Episode(
                        data = iframeLink,
                        name = "${sz_num}. Sezon ${ep_num}. Bölüm",
                        season = sz_num,
                        episode = ep_num
                    )
                )
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.rating = rating
                this.duration = duration
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.rating = rating
                this.duration = duration
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.contains("vidmody")) {
            val aa = app.get(data, referer = "${mainUrl}/").document
            val bb = aa.body().selectFirst("script").toString()
                .substringAfter("var id =").substringBefore(";")
                .replace("'", "").trim()
            val m3uLink = "https://vidmody.com/vs/$bb"
            val m3uicerik = app.get(m3uLink, referer = mainUrl).text
            val audioRegex = Regex(
                "#EXT-X-MEDIA:TYPE=AUDIO,.*NAME=\"(.*?)\".*URI=\"(.*?)\"",
                RegexOption.MULTILINE
            )
            val audioMatches = audioRegex.findAll(m3uicerik)
            audioMatches.forEach { matchResult ->
                val name = matchResult.groupValues[1]
                val uri = matchResult.groupValues[2]
            }

            val audioList = audioMatches.toList()
            // SUBTITLES verilerini al
            val subtitlesRegex = Regex(
                "#EXT-X-MEDIA:TYPE=SUBTITLES,.*NAME=\"(.*?)\".*URI=\"(.*?)\"",
                RegexOption.MULTILINE
            )
            val subtitlesMatches = subtitlesRegex.findAll(m3uicerik)
            subtitlesMatches.forEach { matchResult ->
                val name = matchResult.groupValues[1]
                val uri = matchResult.groupValues[2]
                subtitleCallback.invoke(
                    SubtitleFile(
                        lang = name,
                        url = fixUrl(uri)
                    )
                )
            }

            // STREAM verilerini al
            val streamRegex = Regex(
                "#EXT-X-STREAM-INF:.*RESOLUTION=(\\d+x\\d+).*\\s+(https?://\\S+)",
                RegexOption.MULTILINE
            )
            val streamMatches = streamRegex.findAll(m3uicerik)
            streamMatches.forEachIndexed { index, matchResult ->
                val resolution = matchResult.groupValues[1]
                val uri = matchResult.groupValues[2]
                val uriv2 = matchResult.groupValues[2].replace("a1.gif", "a2.gif")
                var name1 = ""
                var name2 = ""
                if (audioList.isNotEmpty()) {
                    name1 = audioList[0].groupValues.getOrNull(1).toString()
                    name2 = audioList[1].groupValues.getOrNull(1).toString()
                }
                callback.invoke(
                    ExtractorLink(
                        source = this.name,
                        name = "$resolution - $name1",
                        url = uri,
                        referer = uri,
                        quality = getQualityFromName("4k"),
                        isM3u8 = true
                    )
                )
                callback.invoke(
                    ExtractorLink(
                        source = this.name,
                        name = "$resolution - $name2",
                        url = uriv2,
                        referer = uriv2,
                        quality = getQualityFromName("4k"),
                        isM3u8 = true
                    )
                )
            }
        } else if (data.contains("vidlop")) {

            val vidUrl = app.post(
                "https://vidlop.com/player/index.php?data=" + data.split("/")
                    .last() + "&do=getVideo",
                headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
                referer = "${mainUrl}/"
            ).parsedSafe<VidLop>()?.securedLink ?: return false
            callback.invoke(
                ExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = vidUrl,
                    referer = data,
                    quality = Qualities.Unknown.value,
                    isM3u8 = true
                )
            )
            loadExtractor(data, subtitleCallback, callback)
        }
        val document = app.get(data).document
        val iframeSkici = IframeKodlayici()
        val pdataMatches = Regex("""pdata\[\'(.*?)'\] = \'(.*?)\';""").findAll(document.html())
        val pdataList = pdataMatches.map { it.destructured }.toList()

        for (pdata in pdataList) {
            val key = pdata.component1()
            val value = pdata.component2()
            val iframeData = iframeSkici.iframeCoz(value!!)
            val iframeLink = app.get(iframeData, referer = "${mainUrl}/").url.toString()
            if (iframeLink.contains("vidmody")) {
                val aa = app.get(iframeLink, referer = "${mainUrl}/").document
                val bb = aa.body().selectFirst("script").toString()
                    .substringAfter("var id =").substringBefore(";")
                    .replace("'", "").trim()
                val m3uLink = "https://vidmody.com/vs/$bb"
                /*val m3uicerik = app.get(m3uLink, referer = mainUrl).text
                val audioRegex = Regex(
                    "#EXT-X-MEDIA:TYPE=AUDIO,.*NAME=\"(.*?)\".*URI=\"(.*?)\"",
                    RegexOption.MULTILINE
                )
                val audioMatches = audioRegex.findAll(m3uicerik)
                audioMatches.forEach { matchResult ->
                    val name = matchResult.groupValues[1]
                    val uri = matchResult.groupValues[2]
                }
                val audioList = audioMatches.toList()
                // SUBTITLES verilerini al
                val subtitlesRegex = Regex(
                    "#EXT-X-MEDIA:TYPE=SUBTITLES,.*NAME=\"(.*?)\".*URI=\"(.*?)\"",
                    RegexOption.MULTILINE
                )
                val subtitlesMatches = subtitlesRegex.findAll(m3uicerik)
                subtitlesMatches.forEach { matchResult ->
                    val name = matchResult.groupValues[1]
                    val uri = matchResult.groupValues[2]
                    subtitleCallback.invoke(
                        SubtitleFile(
                            lang = name,
                            url = fixUrl(uri)
                        )
                    )
                }

                // STREAM verilerini al
                val streamRegex = Regex(
                    "#EXT-X-STREAM-INF:.*RESOLUTION=(\\d+x\\d+).*\\s+(https?://\\S+)",
                    RegexOption.MULTILINE
                )
                val streamMatches = streamRegex.findAll(m3uicerik)
                streamMatches.forEachIndexed { index, matchResult ->
                    val resolution = matchResult.groupValues[1]
                    val uri = matchResult.groupValues[2]
                    val uriv2 = matchResult.groupValues[2].replace("a1.gif", "a2.gif")
                    var name1 = ""
                    var name2 = ""
                    if (audioList.isNotEmpty()) {
                        name1 = audioList[0].groupValues.getOrNull(1).toString()
                        name2 = audioList[1].groupValues.getOrNull(1).toString()
                    }
                    callback.invoke(
                        ExtractorLink(
                            source = this.name,
                            name = "$resolution - $name1",
                            url = uri,
                            referer = uri,
                            quality = getQualityFromName("4k"),
                            isM3u8 = true
                        )
                    )
                    callback.invoke(
                        ExtractorLink(
                            source = this.name,
                            name = "$resolution - $name2",
                            url = uriv2,
                            referer = uriv2,
                            quality = getQualityFromName("4k"),
                            isM3u8 = true
                        )
                    )

                }*/
                callback.invoke(
                    ExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = m3uLink,
                        referer = "$mainUrl/",
                        quality = getQualityFromName("4k"),
                        isM3u8 = true
                    )
                )
                loadExtractor(iframeLink, "$mainUrl/", subtitleCallback, callback)
            } else if (iframeLink.contains("vidlop")) {
                val vidUrl = app.post(
                    "https://vidlop.com/player/index.php?data=" + data.split("/")
                        .last() + "&do=getVideo",
                    headers = mapOf("X-Requested-With" to "XMLHttpRequest"),
                    referer = "${mainUrl}/"
                ).parsedSafe<VidLop>()?.securedLink ?: return false
                callback.invoke(
                    ExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = vidUrl,
                        referer = data,
                        quality = Qualities.Unknown.value,
                        isM3u8 = true
                    )
                )
                loadExtractor(data, subtitleCallback, callback)
            }
        }
        return true
    }

    data class VidLop(
        @JsonProperty("hls") val hls: Boolean? = null,
        @JsonProperty("securedLink") val securedLink: String? = null
    )
}