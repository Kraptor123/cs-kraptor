package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class SuperFilmGeldiPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(SuperFilmGeldi())
        registerExtractorAPI(MixPlayHD())
        registerExtractorAPI(MixTiger())
    }
}