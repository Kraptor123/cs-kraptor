package com.kraptor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.*

open class MediaFireExtractor : ExtractorApi() {
    override val name = "MediaFire"
    override val mainUrl = "https://www.mediafire.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val videoReq = app.get(url).document
        val videolink = videoReq.selectFirst("a.input.popsok")?.attr("data-scrambled-url").toString()
        val video     = base64Decode(videolink)
        Log.d("kraptor_$name","video = $video")
        callback.invoke(
            newExtractorLink(
            "MediaFire",
            name = "MediaFire",
            url = video,
            type = INFER_TYPE,
            {

            }

        ))
    }
}