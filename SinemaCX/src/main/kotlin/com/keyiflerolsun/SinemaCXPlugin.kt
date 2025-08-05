package com.keyiflerolsun

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class SinemaCXPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(SinemaCX())
    }
}