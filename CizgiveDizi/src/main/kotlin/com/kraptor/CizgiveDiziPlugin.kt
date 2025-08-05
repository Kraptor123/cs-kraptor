package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class CizgiveDiziPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(CizgiveDizi())
        registerExtractorAPI(GoogleDriveExtractor())
        registerExtractorAPI(SibNet())
        registerExtractorAPI(CizgiDuo())
        registerExtractorAPI(CizgiPass())
    }
}