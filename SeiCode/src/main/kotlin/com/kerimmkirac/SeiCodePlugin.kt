package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class SeiCodePlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(SeiCode())
        registerExtractorAPI(GoogleDriveExtractor())
    }
}