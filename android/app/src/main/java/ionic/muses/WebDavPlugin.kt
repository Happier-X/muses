package ionic.muses

import android.util.Base64
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@CapacitorPlugin(name = "WebDav")
class WebDavPlugin : Plugin() {
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .build()
    }

    @PluginMethod
    fun propfind(call: PluginCall) {
        val url = call.getString("url")
        val username = call.getString("username")
        val password = call.getString("password")

        if (url.isNullOrEmpty()) {
            call.reject("缺少 WebDAV 请求地址。", "missingUrl")
            return
        }

        if (username == null || password == null) {
            call.reject("缺少 WebDAV 认证信息。", "missingCredentials")
            return
        }

        bridge.execute {
            try {
                val requestBody = PROPFIND_BODY.toRequestBody(XML_MEDIA_TYPE)
                val request = Request.Builder()
                    .url(url)
                    .method("PROPFIND", requestBody)
                    .header("Depth", "1")
                    .header("Content-Type", "application/xml; charset=utf-8")
                    .header("Accept", "application/xml, text/xml, */*")
                    .header("Accept-Charset", "utf-8, gbk;q=0.8, gb2312;q=0.8")
                    .header("Authorization", "Basic ${encodeBasicAuth(username, password)}")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val responseBytes = response.body?.bytes() ?: ByteArray(0)
                    val data = decodeResponseBody(responseBytes, response.header("Content-Type"))
                    val result = JSObject()
                    result.put("status", response.code)
                    result.put("data", data)
                    call.resolve(result)
                }
            } catch (exception: Exception) {
                call.reject(exception.message, exception)
            }
        }
    }

    private fun encodeBasicAuth(username: String, password: String): String {
        val rawValue = "$username:$password"
        return Base64.encodeToString(rawValue.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    private fun decodeResponseBody(bytes: ByteArray, contentType: String?): String {
        if (bytes.isEmpty()) {
            return ""
        }

        val headerCharset = parseHeaderCharset(contentType)
        if (headerCharset != null) {
            return String(bytes, headerCharset)
        }

        val utf8Text = String(bytes, StandardCharsets.UTF_8)
        val xmlCharset = parseXmlCharset(utf8Text)
        if (xmlCharset != null && xmlCharset != StandardCharsets.UTF_8) {
            return String(bytes, xmlCharset)
        }

        if (utf8Text.contains(REPLACEMENT_CHARACTER)) {
            return runCatching { String(bytes, Charset.forName("GBK")) }.getOrDefault(utf8Text)
        }

        return utf8Text
    }

    private fun parseHeaderCharset(contentType: String?): Charset? {
        if (contentType.isNullOrBlank()) {
            return null
        }

        return CHARSET_PATTERN.find(contentType)?.groupValues?.getOrNull(1)?.let(::safeCharset)
    }

    private fun parseXmlCharset(xmlText: String): Charset? {
        return XML_ENCODING_PATTERN.find(xmlText)?.groupValues?.getOrNull(1)?.let(::safeCharset)
    }

    private fun safeCharset(name: String): Charset? {
        return runCatching { Charset.forName(name.trim()) }.getOrNull()
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val PROPFIND_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><d:propfind xmlns:d=\"DAV:\"><d:allprop /></d:propfind>"
        const val REPLACEMENT_CHARACTER = '\uFFFD'
        val XML_MEDIA_TYPE = "application/xml; charset=utf-8".toMediaType()
        val CHARSET_PATTERN = Regex("charset=([^;\\s]+)", RegexOption.IGNORE_CASE)
        val XML_ENCODING_PATTERN = Regex("<\\?xml[^>]*encoding=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    }
}
