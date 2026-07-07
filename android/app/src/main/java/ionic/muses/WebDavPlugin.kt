package ionic.muses

import android.net.Uri
import android.util.Base64
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient

@CapacitorPlugin(name = "WebDav")
class WebDavPlugin : Plugin() {
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .build()
    }

    private val audioCache by lazy { WebDavAudioCache(context, httpClient) }

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

    @PluginMethod
    fun readMetadata(call: PluginCall) {
        val url = call.getString("url")
        val username = call.getString("username")
        val password = call.getString("password")

        if (url.isNullOrEmpty()) {
            call.reject("缺少 WebDAV 文件地址。", "missingUrl")
            return
        }

        if (username == null || password == null) {
            call.reject("缺少 WebDAV 认证信息。", "missingCredentials")
            return
        }

        bridge.execute {
            try {
                val cacheKey = call.getString("songId") ?: safeCacheKey(url)
                val diagnostics = mutableSetOf<String>()
                val cachedFile = try {
                    audioCache.getOrDownload(url, username, password)
                } catch (exception: Exception) {
                    throw WebDavMetadataException(downloadDiagnosticCode(exception), "WebDAV 音频下载失败，无法读取标签。")
                }

                if (cachedFile.length() <= 0L) {
                    throw WebDavMetadataException("empty_file", "WebDAV 音频缓存为空，无法读取标签。")
                }

                val result = AudioMetadataReader(context).readFromFile(cachedFile, cacheKey)
                readSidecarLyrics(url, username, password)?.let { lyrics ->
                    if (!result.has("lyrics")) {
                        result.put("lyrics", lyrics)
                        result.put("lyricsSource", "sidecar")
                    }
                }
                if (!result.has("lyrics")) {
                    diagnostics.add("no_lyrics")
                }
                result.put("tagsScanned", true)
                appendDiagnostics(result, diagnostics)
                call.resolve(result)
            } catch (exception: WebDavMetadataException) {
                rejectMetadataFailure(call, exception.diagnosticCode, exception.message ?: "WebDAV 标签读取失败。")
            } catch (exception: AudioMetadataException) {
                rejectMetadataFailure(call, exception.diagnosticCode, exception.message ?: "WebDAV 标签读取失败。")
            } catch (exception: Exception) {
                rejectMetadataFailure(call, "metadata_failed", "WebDAV 标签读取失败。")
            }
        }
    }

    private fun rejectMetadataFailure(call: PluginCall, code: String, message: String) {
        val data = JSObject()
        val diagnostic = JSObject()
        val codes = JSArray()
        codes.put(code)
        diagnostic.put("codes", codes)
        data.put("metadataDiagnostic", diagnostic)
        call.reject(message, code, null, data)
    }

    private fun appendDiagnostics(result: JSObject, diagnostics: Set<String>) {
        if (diagnostics.isEmpty()) {
            return
        }
        val existing = result.getJSObject("metadataDiagnostic") ?: JSObject()
        val codes = existing.getJSONArray("codes") ?: JSArray()
        diagnostics.forEach { codes.put(it) }
        existing.put("codes", codes)
        result.put("metadataDiagnostic", existing)
    }

    private fun downloadDiagnosticCode(exception: Exception): String {
        val message = exception.message.orEmpty()
        return when {
            message.contains("webdavCacheEmpty", ignoreCase = true) -> "empty_file"
            message.contains("webdavCacheDownloadFailed", ignoreCase = true) -> "download_failed"
            else -> "download_failed"
        }
    }

    private fun safeCacheKey(url: String): String {
        return runCatching {
            val uri = Uri.parse(url)
            uri.buildUpon().query(null).fragment(null).build().toString()
        }.getOrDefault("webdav-audio")
    }

    private fun readSidecarLyrics(audioUrl: String, username: String, password: String): String? {
        val lyricUrl = buildSidecarLyricsUrl(audioUrl) ?: return null
        val request = Request.Builder()
            .url(lyricUrl)
            .get()
            .header("Authorization", "Basic ${encodeBasicAuth(username, password)}")
            .header("Accept", "text/plain, */*")
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use null
                }
                val bytes = response.body?.bytes() ?: return@use null
                decodeResponseBody(bytes, response.header("Content-Type")).takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun buildSidecarLyricsUrl(audioUrl: String): String? {
        return runCatching {
            val uri = Uri.parse(audioUrl)
            val lastSegment = uri.lastPathSegment ?: return null
            val lyricSegment = lastSegment.substringBeforeLast('.', lastSegment) + ".lrc"
            uri.buildUpon()
                .path(uri.path.orEmpty().substringBeforeLast('/') + "/" + lyricSegment)
                .query(null)
                .fragment(null)
                .build()
                .toString()
        }.getOrNull()
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

private class WebDavMetadataException(
    val diagnosticCode: String,
    message: String,
) : Exception(message)
