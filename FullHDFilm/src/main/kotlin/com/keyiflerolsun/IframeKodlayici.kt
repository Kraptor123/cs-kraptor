// ! Bu araç @kerimmkirac tarafından yazılmıştır.

package com.keyiflerolsun

import android.util.Base64

class IframeKodlayici {
    companion object {
        fun tersCevir(metin: String): String {
            return metin.reversed()
        }

        fun base64Coz(encodedString: String): String {
            val decodedBytes = Base64.decode(encodedString, Base64.DEFAULT)
            return String(decodedBytes, Charsets.UTF_8)
        }

        fun iframeParse(htmlIcerik: String): String {
            val iframePattern = """<iframe[^>]+src=["']([^"']+)["'][^>]*>""".toRegex()
            val match = iframePattern.find(htmlIcerik)
            return match?.groupValues?.get(1) ?: throw IllegalArgumentException("Iframe src bulunamadı")
        }

        fun videoIdCikar(iframeSrc: String): String {
            // //hdload.site/hdplayer/z4ezvl24iodjgcq3uebc -> z4ezvl24iodjgcq3uebc
            return iframeSrc.substringAfterLast("/")
        }

        fun m3u8LinkOlustur(videoId: String): String {
            return "https://hdload.site/uploads/encode/$videoId/master.m3u8"
        }

        fun altyaziLinkleriOlustur(videoId: String): List<Pair<String, String>> {
            return listOf(
                "English" to "https://hdload.site/uploads/encode/$videoId/${videoId}_eng.vtt",
                "Turkish" to "https://hdload.site/uploads/encode/$videoId/${videoId}_tur.vtt",
                "Turkish Forced" to "https://hdload.site/uploads/encode/$videoId/${videoId}_tur_forced.vtt"
            )
        }
    }

    fun iframeCoz(veri: String): VideoData {
        var tempVeri = veri
        if (!tempVeri.startsWith("PGltZyB3aWR0aD0iMTAwJSIgaGVpZ2")) {
            tempVeri = tersCevir("BSZtFmcmlGP") + tempVeri
        }
        
        val iframe = base64Coz(tempVeri)
        val iframeSrc = iframeParse(iframe)
        val videoId = videoIdCikar(iframeSrc)
        val m3u8Url = m3u8LinkOlustur(videoId)
        val altyazilar = altyaziLinkleriOlustur(videoId)
        
        return VideoData(
            videoId = videoId,
            m3u8Url = m3u8Url,
            referer = "https://hdload.site",
            altyazilar = altyazilar
        )
    }
}

data class VideoData(
    val videoId: String,
    val m3u8Url: String,
    val referer: String,
    val altyazilar: List<Pair<String, String>>
)