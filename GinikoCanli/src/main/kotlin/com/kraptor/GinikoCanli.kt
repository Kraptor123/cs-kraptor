// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import okhttp3.ResponseBody

class GinikoCanli : MainAPI() {
    override var mainUrl = "https://www.giniko.com"
    override var name = "VPN/GinikoCanli"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Live)

    override val mainPage = mainPageOf(
        "${mainUrl}/watch-all-channels.php" to "Tüm Kanallar",
        "${mainUrl}/country.php?id=2" to "Türk Kanalları",
        "${mainUrl}/country.php?id=217" to "United Kingdom",
        "${mainUrl}/country.php?id=218" to "United States",
        "${mainUrl}/country.php?id=236" to "USA",
        "${mainUrl}/country.php?id=11" to "Afghanistan",
        "${mainUrl}/country.php?id=13" to "Algeria",
        "${mainUrl}/country.php?id=20" to "Armenia",
        "${mainUrl}/country.php?id=23" to "Austria",
        "${mainUrl}/country.php?id=24" to "Azerbaijan",
        "${mainUrl}/country.php?id=29" to "Belarus",
        "${mainUrl}/country.php?id=30" to "Belgium",
        "${mainUrl}/country.php?id=38" to "Brazil",
        "${mainUrl}/country.php?id=46" to "Canada",
        "${mainUrl}/country.php?id=52" to "China",
        "${mainUrl}/country.php?id=64" to "Congo",
        "${mainUrl}/country.php?id=63" to "Czech Republic",
        "${mainUrl}/country.php?id=66" to "Djibouti",
        "${mainUrl}/country.php?id=70" to "Egypt",
        "${mainUrl}/country.php?id=80" to "France",
        "${mainUrl}/country.php?id=1" to "Georgia",
        "${mainUrl}/country.php?id=84" to "Germany",
        "${mainUrl}/country.php?id=85" to "Ghana",
        "${mainUrl}/country.php?id=100" to "India",
        "${mainUrl}/country.php?id=102" to "Iran",
        "${mainUrl}/country.php?id=103" to "Iraq",
        "${mainUrl}/country.php?id=106" to "Italy",
        "${mainUrl}/country.php?id=58" to "Ivory Coast",
        "${mainUrl}/country.php?id=109" to "Jordan",
        "${mainUrl}/country.php?id=235" to "Korea",
        "${mainUrl}/country.php?id=113" to "Kuwait",
        "${mainUrl}/country.php?id=120" to "Libya",
        "${mainUrl}/country.php?id=130" to "Mali",
        "${mainUrl}/country.php?id=146" to "Mexico",
        "${mainUrl}/country.php?id=9" to "Morocco",
        "${mainUrl}/country.php?id=135" to "Netherlands",
        "${mainUrl}/country.php?id=162" to "Nigeria",
        "${mainUrl}/country.php?id=157" to "Oman",
        "${mainUrl}/country.php?id=181" to "Portugal",
        "${mainUrl}/country.php?id=179" to "Qatar",
        "${mainUrl}/country.php?id=178" to "Romania",
        "${mainUrl}/country.php?id=177" to "Russian Federation",
        "${mainUrl}/country.php?id=176" to "Rwanda",
        "${mainUrl}/country.php?id=167" to "Saudi Arabia",
        "${mainUrl}/country.php?id=166" to "Senegal",
        "${mainUrl}/country.php?id=186" to "Singapore",
        "${mainUrl}/country.php?id=190" to "Somalia",
        "${mainUrl}/country.php?id=7" to "Southern Sudan",
        "${mainUrl}/country.php?id=194" to "Spain",
        "${mainUrl}/country.php?id=5" to "Sudan",
        "${mainUrl}/country.php?id=216" to "Ukraine",
        "${mainUrl}/country.php?id=230" to "United Arab Emirates",
        "${mainUrl}/country.php?id=225" to "Vietnam"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get(request.data).document
        } else {
            app.get("${request.data}?=&list=$page&asc=1").document
        }
        val home = document.select("div.portfolio-item").mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val anchor = this.selectFirst("span.title a") ?: return null
        val malako = this.selectFirst("div.portfolio-item").toString()
        Log.d("giniko","hanimis $malako")
        val regex = Regex(pattern = "gplusbg3", options = setOf(RegexOption.IGNORE_CASE))
        var title = anchor.text().trim()
        val href = fixUrlNull(anchor.attr("href")) ?: return null
        var posterUrl = fixUrlNull(this.selectFirst("img[src*='']")?.attr("src"))

        if (regex.containsMatchIn(malako)) {
            return null
        }

        return newLiveSearchResponse(title, href, TvType.Live) { this.posterUrl = posterUrl }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.desc strong")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.desc img")?.attr("src"))
        val description = document.selectFirst(".content p")?.text()?.trim()
        val tags =
            document.select(".content > table:nth-child(3) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > h1:nth-child(1) > a:nth-child(2) > u:nth-child(1)")
                .map { it.text() }

        return newLiveStreamLoadResponse(title, url, url,) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("giniko", "data = ${data}")
        val document = app.get(data).document
        val regex = Regex("source:\\s*\"([^\"]*)\"").find(document.html())
        val icerik = regex?.groupValues?.get(1).toString()
        Log.d("giniko", "icerik = ${icerik}")
        val icerikIcerigi = fixUrlNull(icerik).toString()

        Log.d("giniko", "icerikicerigi = ${icerikIcerigi}")

        callback.invoke(
            newExtractorLink(
            "Giniko",
            "Giniko",
            icerikIcerigi,
            ExtractorLinkType.M3U8,
            {
                this.referer = "${mainUrl}/"
                this.headers = mapOf( "Host" to "tgn.bozztv.com",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:138.0) Gecko/20100101 Firefox/138.0",
                    "Connection" to "keep-alive",
                    "Referer" to "https://www.giniko.com/",
                )
            }
        ))
        return true
    }
}
