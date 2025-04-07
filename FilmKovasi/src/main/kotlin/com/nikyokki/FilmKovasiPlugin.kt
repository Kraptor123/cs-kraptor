package com.nikyokki

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class FilmKovasiPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(FilmKovasi())
    }
}