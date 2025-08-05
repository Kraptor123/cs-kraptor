package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class DiziYouPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(DiziYou())
    }
}