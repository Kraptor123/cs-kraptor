package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class KoreanTurkPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(KoreanTurk())
    }
}