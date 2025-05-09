// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import android.R.id.input
import android.util.Log
import com.lagradost.cloudstream3.app
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


class DonilasPlayExtractor : ExtractorApi() {
    override val name = "DonilasPlay"
    override val mainUrl = "https://donilasplay.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink> {
        val links = mutableListOf<ExtractorLink>()
        try {
            // Load the iframe or player page
            val iframeDoc = app.get(url).document




            // Find the relevant script containing bePlayer
            val script = iframeDoc.select("script").mapNotNull { it.html() }
                .firstOrNull { it.contains("bePlayer") }
                ?: throw Exception("bePlayer script not found")
            Log.d("dzmo", "input: $script")

            // Extract parameters via regex
            val regex = Regex(pattern = """bePlayer\('([^']*)',\s*'([^']*)'\)""", options = setOf(RegexOption.IGNORE_CASE))

            val match = regex.find(script)
                ?: throw Exception("bePlayer parameters not found")



            val hash = match.groupValues[1]
            val setJsonRaw = match.groupValues[2]
                .removeSurrounding("\"", "\"")
                .removeSurrounding("'", "'")

            Log.d("dzmo", "hash: $hash")
            Log.d("dzmo", "hash: $setJsonRaw")

            // Decrypt parameters and parse JSON
            val decrypted = decryptSetParams(setJsonRaw, hash)
            Log.d("dzmo", "decrypted: $decrypted")
            val videoLocation = JSONObject(decrypted).getString("video_location")
            Log.d("dzmo", "videolocation: $videoLocation")

            // Build the HLS link
            links.add(
                newExtractorLink(
                    source = name,
                    name = "Donilas HLS",
                    url = videoLocation,
                    type = ExtractorLinkType.M3U8
                ) {
                    quality = Qualities.Unknown.value
                    headers = mapOf("Referer" to url,
                        "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36"
                        )
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
