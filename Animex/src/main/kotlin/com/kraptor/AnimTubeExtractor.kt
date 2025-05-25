// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AnimTubeExtractor : ExtractorApi() {
    override val name = "AnimTube"
    override val mainUrl = "https://animtube.online"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink> {
        val links = mutableListOf<ExtractorLink>()
        try {
            // Load the iframe or player page
            val iframeDoc = app.get(url, referer = referer ?: mainUrl).document
            // Find the relevant script containing bePlayer
            val script = iframeDoc.select("script").mapNotNull { it.html() }
                .firstOrNull { it.contains("bePlayer") }
                ?: throw Exception("bePlayer script not found")

            // Extract parameters via regex
            val regex = Regex("""bePlayer\(\s*['\"]([^'\"]+)['\"]\s*,\s*(['\"]\{.*?\}['\"])""")
            val match = regex.find(script)
                ?: throw Exception("bePlayer parameters not found")

            val hash = match.groupValues[1]
            val setJsonRaw = match.groupValues[2]
                .removeSurrounding("\"", "\"")
                .removeSurrounding("'", "'")

            // Decrypt parameters and parse JSON
            val decrypted = decryptSetParams(setJsonRaw, hash)
            val videoLocation = JSONObject(decrypted).getString("video_location")

            // Build the HLS link
            links.add(
                newExtractorLink(
                    source = name,
                    name = "AnimTube",
                    url = videoLocation,
                    type = ExtractorLinkType.M3U8
                ) {
                    quality = Qualities.Unknown.value
                    headers = mapOf("Referer" to url)
                }
            )
        } catch (_: Exception) {
            // Extraction failed, return empty
        }
        return links
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val out = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            out[i / 2] = ((Character.digit(hex[i], 16) shl 4)
                    + Character.digit(hex[i + 1], 16)).toByte()
        }
        return out
    }

    private fun evpBytesToKey(
        password: ByteArray,
        salt: ByteArray,
        keyLen: Int,
        ivLen: Int
    ): Pair<ByteArray, ByteArray> {
        val md5 = MessageDigest.getInstance("MD5")
        var prev = ByteArray(0)
        val result = mutableListOf<Byte>()
        while (result.size < keyLen + ivLen) {
            md5.reset()
            md5.update(prev)
            md5.update(password)
            md5.update(salt)
            val digest = md5.digest()
            result += digest.toTypedArray()
            prev = digest
        }
        val key = result.take(keyLen).toByteArray()
        val iv = result.drop(keyLen).take(ivLen).toByteArray()
        return Pair(key, iv)
    }

    private fun decryptSetParams(setJson: String, hash: String): String {
        val obj = JSONObject(setJson)
        val ctB64 = obj.getString("ct")
        val ivHex = obj.getString("iv")
        val sHex = obj.getString("s")
        val cipherText = android.util.Base64.decode(ctB64, android.util.Base64.DEFAULT)
        val saltBytes = hexStringToByteArray(sHex)
        val password = hash.toByteArray(StandardCharsets.UTF_8)
        val (key, iv) = evpBytesToKey(password, saltBytes, 32, 16)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val plain = cipher.doFinal(cipherText)
        return String(plain, StandardCharsets.UTF_8)
    }
}
