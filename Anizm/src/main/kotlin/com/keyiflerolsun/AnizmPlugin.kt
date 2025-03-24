// ! Bu araç @kraptor123 tarafından yazılmıştır.

package com.keyiflerolsun

import android.content.Context
import com.keyiflerolsun.extractors.AincradExtractor
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AnizmPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Anizm())
    }
}