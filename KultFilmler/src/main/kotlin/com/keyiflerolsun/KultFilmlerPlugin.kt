package com.keyiflerolsun

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class KultFilmlerPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(KultFilmler())
        registerExtractorAPI(YildizKisaFilm())
    }
}