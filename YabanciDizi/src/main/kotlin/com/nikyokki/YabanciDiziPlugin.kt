package com.nikyokki

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class YabanciDiziPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(YabanciDizi())
    }
}