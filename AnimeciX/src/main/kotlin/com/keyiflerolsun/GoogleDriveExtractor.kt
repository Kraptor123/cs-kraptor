// ! Bu araç @kraptor123 tarafından yazılmıştır.
package com.keyiflerolsun

import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class GoogleDriveExtractor : ExtractorApi() {
    override val name = "GdrivePlayer"
    override val mainUrl = "https://drive.google.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("kraptor_${this.name}", "isleme girdi")
        val extRef = referer ?: ""
        Log.d("kraptor_GoogleDrive", "url = $url")
        val urlId = url.substringAfter("/d/").substringBefore("/")
        val document = app.post(
            "https://gdplayer.vip/api/video", data = mapOf(
                "file_id" to urlId,
                "subtitle" to ""
            )
        ).text
//        Log.d("kraptor_GoogleDrive","document = $document")
        val mapper = jacksonObjectMapper()
        val response: ApiResponse = mapper.readValue(document)
        val embedUrl = response.data.embedUrl
        Log.d("kraptor_GoogleDrive", "embed_url = $embedUrl")

        val documentNew = app.get(embedUrl).document

//        Log.d("kraptor_GoogleDrive", "documentNew = $documentNew")
        val bodyEl = documentNew.selectFirst("body[ng-init]")

        val ngInit = bodyEl?.attr("ng-init").toString()
        Log.d("kraptor_GoogleDrive", "ng-init = $ngInit")

        val regex = """init\('([^']+)',\s*'([^']+)',\s*'([^']+)',\s*'([^']*)'\)""".toRegex()
        val match = regex.find(ngInit) ?: throw IllegalStateException("init formatı beklenenden farklı")
        val (_, playUrl, keyHex, _) = match.destructured

        val videoApiUrl = "$playUrl/?video_id=$keyHex&action=get_video"

        val jsonText = app.get(videoApiUrl, referer = "https://gdplayer.vip/").text

        // 2) Jackson ile parse et
        val mapperEx = jacksonObjectMapper()
        val root = mapperEx.readTree(jsonText)
        val qualitiesNode = root["qualities"]

        // 3) qualitiesNode içindeki her elemana bak
        qualitiesNode.forEach { qNode ->
            val q = qNode["quality"].asInt()
            val qualities = "${q}p"

            // 4) Yeni URL’i yarat
            val urlWithQuality = "$playUrl/?video_id=$keyHex&quality=$q&action=p"
            Log.d("kraptor_GoogleDrive", "urlWithQuality = $urlWithQuality")

            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name   = this.name,
                    url    = urlWithQuality,
                    type   = INFER_TYPE
                ) {
                    headers = mapOf("Referer" to "https://gdplayer.vip/") // "Referer" ayarı burada yapılabilir
                    quality = getQualityFromName(qualities)
                }
            )
        }
    }
}

data class VideoData(
    val slug: String,
    @JsonProperty("embed_url") val embedUrl: String,
    val allowed_domains: List<String>?,
    val subtitle: String?,
    val ad_url: String?
)

data class ApiResponse(
    val data: VideoData,
    val status: String,
    val message: String
)