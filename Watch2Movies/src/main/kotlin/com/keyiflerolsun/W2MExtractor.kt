// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

open class W2MExtractor(override val mainUrl: String, private val context: Context) : ExtractorApi() {
    override val name            = "W2MExtractor"
    override val requiresReferer = true
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        withContext(Dispatchers.Main) {
            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled                  = true
                    domStorageEnabled                  = true
                    javaScriptCanOpenWindowsAutomatically = true
                    loadWithOverviewMode               = true
                    useWideViewPort                    = true
                    allowFileAccess                    = true
                    builtInZoomControls                = true
                    displayZoomControls                = false
                    allowContentAccess                 = true
                    mixedContentMode                   = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString                    = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36"
                }

                evaluateJavascript(
                    """
Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });
Object.defineProperty(navigator, 'language', { get: () => 'en-US' });
window.chrome = { runtime: {} };
""".trimIndent()
                ) {}

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        @Suppress("NAME_SHADOWING") val url = request?.url.toString()
                        val headers = request?.requestHeaders

                        Thread {
                            fetchAndCheckResponse(url, headers) { sourceUrl, headers ->
                                callback.invoke(
                                    ExtractorLink(
                                        source  = this@W2MExtractor.name,
                                        name    = this@W2MExtractor.name,
                                        url     = sourceUrl,
                                        referer = headers["Referer"] ?: headers["referer"] ?: mainUrl,
                                        quality = Qualities.Unknown.value,
                                        type    = ExtractorLinkType.M3U8,
                                        headers = headers
                                    )
                                )
                            }
                        }.start()

                        return super.shouldInterceptRequest(view, request)
                    }
                }

                loadUrl(url)
            }
        }

        delay(10_000)
    }

    private fun fetchAndCheckResponse(url: String, headers: Map<String, String>?, onResponseCaptured: (url: String, headers: Map<String, String>) -> Unit) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            headers?.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            connection.connect()
            val response = BufferedReader(InputStreamReader(connection.inputStream))
                .lineSequence()
                .joinToString("\n")

            if (response.startsWith("#EXTM3U")) {
                Log.d("W2M", response)
                onResponseCaptured(connection.url.toString(), headers ?: mapOf())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}