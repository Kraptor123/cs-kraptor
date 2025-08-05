package com.kraptor

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class CizgiMaxPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(CizgiMax())
        registerExtractorAPI(SibNet())
        registerExtractorAPI(CizgiDuo())
        registerExtractorAPI(CizgiPass())
        registerExtractorAPI(GoogleDriveExtractor())
    }
}