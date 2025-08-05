package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class JetFilmizlePlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(JetFilmizle())
        registerExtractorAPI(PixelDrain())
        registerExtractorAPI(JetTv())
    }
}