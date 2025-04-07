package com.keyiflerolsun

import android.util.Log
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.nicehttp.NiceResponse
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import kotlinx.coroutines.delay
import okhttp3.FormBody
import kotlin.reflect.KClass

val jsonParser = object : ResponseParser {
    val objectMapper: ObjectMapper = jacksonObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
    ).configure(
        JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true
    )

    override fun <T : Any> parse(text: String, kClass: KClass<T>): T {
        return objectMapper.readValue(text, kClass.java)
    }

    override fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T? {
        return try {
            objectMapper.readValue(text, kClass.java)
        } catch (exception: Exception) {
            null
        }
    }

    override fun writeValueAsString(obj: Any): String {
        return objectMapper.writeValueAsString(obj)
    }
}

val httpClient = Requests(responseParser = jsonParser).apply {
    defaultHeaders = mapOf("User-Agent" to USER_AGENT)
}

inline fun <reified T : Any> parseJson(text: String): T {
    return jsonParser.parse(text, T::class)
}

fun convertRuntimeToMinutes(runtimeText: String): Int {
    var totalMinutes = 0
    val timeParts = runtimeText.split(" ")

    for (timePart in timeParts) {
        when {
            timePart.endsWith("h") -> {
                val hours = timePart.removeSuffix("h").trim().toIntOrNull() ?: 0
                totalMinutes += hours * 60
            }
            timePart.endsWith("m") -> {
                val minutes = timePart.removeSuffix("m").trim().toIntOrNull() ?: 0
                totalMinutes += minutes
            }
        }
    }

    return totalMinutes
}

data class VerifyUrl(
    val url: String
)

suspend fun bypassVerification(mainUrl: String): String {
    val homePageDocument = httpClient.get("${mainUrl}/home").document
    val addHash          = homePageDocument.select("body").attr("data-addhash")

    var verificationUrl  = "https://raw.githubusercontent.com/SaurabhKaperwan/Utils/main/NF.json"
    // https://userverify.netmirror.app/verify?dp1=###&a=y

    verificationUrl      = httpClient.get(verificationUrl).parsed<VerifyUrl>().url.replace("###", addHash)
    val hashDigits       = addHash.filter { it.isDigit() }
    val first16Digits    = hashDigits.take(16)
    Log.d("NFX", "Verification URL: ${verificationUrl}&t=0.${first16Digits}")
    httpClient.get("${verificationUrl}&t=0.${first16Digits}")

    var verifyCheck: String
    var verifyResponse: NiceResponse

    do {
        delay(1000)
        val requestBody = FormBody.Builder().add("verify", addHash).build()
        verifyResponse  = httpClient.post("${mainUrl}/verify2.php", requestBody = requestBody)
        verifyCheck     = verifyResponse.text
        Log.d("NFX", "Verification Check: $verifyCheck")
    } while (!verifyCheck.contains("\"statusup\":\"All Done\""))

    return verifyResponse.cookies["t_hash_t"].orEmpty()
}
