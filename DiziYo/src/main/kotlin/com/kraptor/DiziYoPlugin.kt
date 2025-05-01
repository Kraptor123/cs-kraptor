package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class DiziYoPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DiziYo())
    }
}