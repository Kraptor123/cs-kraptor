package com.keyiflerolsun

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class SetFilmIzlePlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(SetFilmIzle())
        registerExtractorAPI(SetPlay())
        registerExtractorAPI(SetPrime())
        registerExtractorAPI(ExPlay())
    }
}