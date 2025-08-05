package com.keyiflerolsun

import android.content.Context
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class WebteIzlePlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(WebteIzle())
    }
}