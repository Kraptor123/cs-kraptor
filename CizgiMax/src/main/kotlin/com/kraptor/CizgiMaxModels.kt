// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.kraptor

import com.fasterxml.jackson.annotation.JsonProperty


data class SearchResult(
    @JsonProperty("data") val data: SearchData?
)

data class SearchData(
    @JsonProperty("result") val result: List<SearchItem>? = arrayListOf(),
)

data class SearchItem(
    @JsonProperty("s_link")  val sLink: String,
    @JsonProperty("s_name")  val sName: String,
    @JsonProperty("s_image") val sImage: String,
)