package com.kraptor

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class CanliTelevizyonPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CanliTelevizyon())
    }
}