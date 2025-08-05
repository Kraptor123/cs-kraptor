package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class TurkAnimePlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(TurkAnime())
        registerExtractorAPI(AlucardExtractor())
    }
}