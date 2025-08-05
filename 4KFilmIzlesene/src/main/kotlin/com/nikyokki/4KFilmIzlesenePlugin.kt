package com.nikyokki

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class `4KFilmIzlesenePlugin`: BasePlugin() {
    override fun load() {
        registerMainAPI(`4KFilmIzlesene`())
    }
}