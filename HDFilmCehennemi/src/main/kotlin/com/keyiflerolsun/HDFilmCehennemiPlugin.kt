package com.keyiflerolsun

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class HDFilmCehennemiPlugin: Plugin() {
    override fun load(context: Context) {
        val hdFilmCehennemi = HDFilmCehennemi()
        hdFilmCehennemi.setContext(context)  // Context'i buradan set et
        registerMainAPI(hdFilmCehennemi)
    }
}