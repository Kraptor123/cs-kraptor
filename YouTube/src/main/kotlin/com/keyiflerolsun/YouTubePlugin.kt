package com.keyiflerolsun

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class YouTubePlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(YouTube())
    }
}