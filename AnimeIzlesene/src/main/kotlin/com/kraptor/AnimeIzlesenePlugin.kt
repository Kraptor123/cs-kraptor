// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class AnimeIzlesenePlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(AnimeIzlesene())
        registerExtractorAPI(HdBestVidExtractor())
    }
}