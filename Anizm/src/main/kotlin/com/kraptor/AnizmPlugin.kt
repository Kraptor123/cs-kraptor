// ! Bu araç @kraptor123 tarafından yazılmıştır.

package com.kraptor

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class AnizmPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(Anizm())
        registerExtractorAPI(AincradExtractor())
        registerExtractorAPI(GoogleDriveExtractor())
    }
}