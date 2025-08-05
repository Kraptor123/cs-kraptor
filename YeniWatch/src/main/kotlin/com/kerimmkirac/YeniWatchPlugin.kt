package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class YeniWatchPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(YeniWatch())
        registerExtractorAPI(CizgiPass())
    }
}