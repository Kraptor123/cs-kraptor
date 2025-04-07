package com.keyiflerolsun

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class Watch2MoviesPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Watch2Movies())
        registerExtractorAPI(W2MExtractor("https://hanatyury.online/", context))
        registerExtractorAPI(W2MExtractor("https://pepepeyo.xyz/",     context))
        registerExtractorAPI(W2MExtractor("https://zizicoi.online/",   context))
        registerExtractorAPI(W2MExtractor("https://watch2movies.net/", context))
    }
}