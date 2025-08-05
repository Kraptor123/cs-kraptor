package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class UgurFilmPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(UgurFilm())
        registerExtractorAPI(MailRu())
        registerExtractorAPI(Odnoklassniki())
    }
}