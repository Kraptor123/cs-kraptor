package com.keyiflerolsun

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class DiziPalPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DiziPal())
    }
}