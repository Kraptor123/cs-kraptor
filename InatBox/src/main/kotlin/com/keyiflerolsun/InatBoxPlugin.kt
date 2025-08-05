package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class InatBoxPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(InatBox())
        registerExtractorAPI(DiskYandexComTr())
        registerExtractorAPI(Vk())
        registerExtractorAPI(Dzen())
        registerExtractorAPI(CDNJWPlayer())
    }
}