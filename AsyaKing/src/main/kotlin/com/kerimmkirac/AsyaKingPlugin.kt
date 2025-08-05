package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class AsyaKingPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(AsyaKing())
    }
}