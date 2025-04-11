package com.kerimmkirac

import Watch32Provider
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class Watch32Plugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Watch32Provider())
    }
}