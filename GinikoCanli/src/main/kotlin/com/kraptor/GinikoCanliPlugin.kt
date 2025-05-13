// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class GinikoCanliPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(GinikoCanli())
    }
}