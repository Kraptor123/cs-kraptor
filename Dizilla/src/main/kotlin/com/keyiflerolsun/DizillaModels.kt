// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.fasterxml.jackson.annotation.JsonProperty

data class SearchResult(
    @JsonProperty("response") val response: String?
)

data class SearchData(
    @JsonProperty("state")   val state: Boolean?           = null,
    @JsonProperty("result")  val result: List<SearchItem>? = arrayListOf(),
    @JsonProperty("message") val message: String?          = null,
    @JsonProperty("html")    val html: String?             = null
)

data class SearchItem(
    @JsonProperty("used_slug")         val slug: String?   = null,
    @JsonProperty("object_name")       val title: String?  = null,
    @JsonProperty("object_poster_url") val poster: String? = null,
    @JsonProperty("imdb_point")        val puan:   String? =null,
)