package com.nikyokki

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class DiziMagPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DiziMag())
    }
}