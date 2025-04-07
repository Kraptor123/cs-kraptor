package com.nikyokki

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class HDFilmSitesiPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(HDFilmSitesi())
    }
}