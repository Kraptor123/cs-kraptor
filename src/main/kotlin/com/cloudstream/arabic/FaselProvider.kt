package com.cloudstream.arabic

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class FaselProvider : MainAPI() {

    override var name = "Fasel"
    override var mainUrl = "https://faselhd.pro"
    override var lang = "ar"

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document

        return document.select(".block").mapNotNull { item ->
            val title = item.selectFirst("h3")?.text() ?: return@mapNotNull null
            val link = item.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = item.selectFirst("img")?.attr("src")

            newMovieSearchResponse(
                title,
                link,
                TvType.Movie
            ) {
                this.posterUrl = poster
            }
        }
    }

    override fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: "Fasel"
        val poster = document.selectFirst("img")?.attr("src")

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            url
        ) {
            this.posterUrl = poster
        }
    }
}
