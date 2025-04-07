package com.keyiflerolsun

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class DiziKoreaPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DiziKorea())
        registerExtractorAPI(VideoSeyred())
    }
}