package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class SetFilmIzlePlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(SetFilmIzle())
        registerExtractorAPI(SetPlay())
        registerExtractorAPI(SetPrime())
        registerExtractorAPI(ExPlay())
    }
}