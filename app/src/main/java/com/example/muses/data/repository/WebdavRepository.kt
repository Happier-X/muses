package com.example.muses.data.repository

import android.net.Uri
import android.util.Log
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.model.TrackSource
import com.example.muses.data.model.WebdavConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.StringReader
import java.util.concurrent.TimeUnit

/**
 * Browses WebDAV servers via PROPFIND using OkHttp + XmlPullParser.
 * Lightweight MVP implementation — no third-party WebDAV library needed.
 */
class WebdavRepository {

    companion object {
        private const val TAG = "WebdavRepo"
        private val PROPFIND_XML = """<?xml version="1.0" encoding="utf-8"?>
            |<D:propfind xmlns:D="DAV:">
            |  <D:prop>
            |    <D:displayname/>
            |    <D:getcontenttype/>
            |    <D:getcontentlength/>
            |    <D:resourcetype/>
            |  </D:prop>
            |</D:propfind>""".trimMargin()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Lists files and directories at the given WebDAV path.
     * Returns success with empty list if the directory is empty.
     */
    fun listDirectory(
        config: WebdavConfig,
        path: String = ""
    ): Result<List<WebdavItem>> {
        return try {
            val url = buildUrl(config.baseUrl, path)
            Log.i(TAG, "PROPFIND $url")

            val requestBody = PROPFIND_XML.toRequestBody("application/xml".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", requestBody)
                .header("Depth", "1")
                .header("Authorization", Credentials.basic(config.username, config.password))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = "HTTP ${response.code}: ${response.message}"
                    Log.w(TAG, msg)
                    return Result.failure(IOException(msg))
                }

                val body = response.body?.string() ?: ""
                if (body.isBlank()) {
                    Log.w(TAG, "Empty PROPFIND response")
                    return Result.success(emptyList())
                }

                val items = parsePropfindResponse(body, config.baseUrl)
                    .filter { it.href != path && it.href != "$path/" }
                    .sortedBy { it.displayName.lowercase() }

                Log.i(TAG, "Found ${items.size} items at $path")
                Result.success(items)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error for $path", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error for $path", e)
            Result.failure(e)
        }
    }

    suspend fun listAudioFiles(
        config: WebdavConfig,
        path: String,
        recursive: Boolean
    ): Result<List<WebdavItem>> = withContext(Dispatchers.IO) {
        try {
            val audioFiles = mutableListOf<WebdavItem>()
            collectAudioFiles(config, path, recursive, audioFiles)
            Result.success(audioFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list audio files in $path", e)
            Result.failure(e)
        }
    }

    private fun collectAudioFiles(
        config: WebdavConfig,
        path: String,
        recursive: Boolean,
        result: MutableList<WebdavItem>
    ) {
        val dirResult = listDirectory(config, path)
        dirResult.fold(
            onSuccess = { items ->
                Log.d(TAG, "collectAudioFiles: $path has ${items.size} items")
                for (item in items) {
                    if (item.isAudioFile) {
                        Log.d(TAG, "  audio: ${item.displayName} (ct=${item.contentType})")
                        result.add(item)
                    } else if (recursive && item.isCollection) {
                        Log.d(TAG, "  subdir: ${item.displayName}")
                        collectAudioFiles(config, item.href, true, result)
                    }
                }
            },
            onFailure = { e ->
                Log.w(TAG, "Skipping directory $path: ${e.message}")
            }
        )
    }

    /**
     * Converts a [WebdavItem] to an [AudioTrack] for playback.
     */
    fun toAudioTrack(item: WebdavItem, config: WebdavConfig): AudioTrack {
        val fullUrl = if (item.href.startsWith("http")) {
            item.href
        } else {
            val base = config.baseUrl.trimEnd('/')
            val path = item.href.trimStart('/')
            "$base/$path"
        }

        val fileName = item.displayName.substringBeforeLast('.')
        return AudioTrack(
            id = "webdav:${item.href}",
            uri = Uri.parse(fullUrl),
            title = fileName,
            artist = "",
            album = "",
            durationMs = 0L,
            sizeBytes = item.contentLength,
            source = TrackSource.WEBDAV
        )
    }

    /** Returns the parent directory path of the given path. */
    fun parentPath(path: String): String {
        val trimmed = path.trimEnd('/')
        val slashIndex = trimmed.lastIndexOf('/')
        return if (slashIndex > 0) trimmed.substring(0, slashIndex) else ""
    }

    private fun buildUrl(baseUrl: String, path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = path.trimStart('/')
        return base + cleanPath
    }

    private fun localName(name: String): String {
        val i = name.indexOf(':')
        return if (i >= 0) name.substring(i + 1) else name
    }

    // --- XML Parsing ---

    /**
     * Parses a WebDAV PROPFIND multistatus XML response.
     * Uses non-namespace-aware XmlPullParser for simplicity.
     * Handles both prefixed (D:href) and unprefixed (href) tag names.
     * Tracks current element via START_TAG/END_TAG since parser.name
     * is unreliable during TEXT events.
     */
    private fun parsePropfindResponse(xml: String, baseUrl: String): List<WebdavItem> {
        val items = mutableListOf<WebdavItem>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var currentHref: String? = null
            var currentDisplayName: String? = null
            var currentContentType: String? = null
            var currentContentLength = 0L
            var currentIsCollection = false
            var inProp = false
            var currentElement: String? = null
            var textBuffer = StringBuilder()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentElement = localName(parser.name)
                        textBuffer = StringBuilder()
                        when (currentElement) {
                            "prop" -> inProp = true
                            "collection" -> {
                                if (inProp) currentIsCollection = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        textBuffer.append(parser.text ?: "")
                    }
                    XmlPullParser.END_TAG -> {
                        val element = localName(parser.name)
                        val text = textBuffer.toString().trim()
                        when (element) {
                            "href" -> {
                                if (text.isNotEmpty()) {
                                    currentHref = decodeHref(text, baseUrl)
                                }
                            }
                            "displayname" -> {
                                if (inProp) {
                                    currentDisplayName = text.ifBlank { null }
                                }
                            }
                            "getcontenttype" -> {
                                if (inProp) {
                                    currentContentType = text.ifBlank { null }
                                }
                            }
                            "getcontentlength" -> {
                                if (inProp) {
                                    currentContentLength = text.toLongOrNull() ?: 0L
                                }
                            }
                            "prop" -> inProp = false
                            "response" -> {
                                val href = currentHref
                                if (href != null) {
                                    val displayName = currentDisplayName
                                        ?: extractFileName(href)
                                    items.add(
                                        WebdavItem(
                                            href = href,
                                            displayName = displayName,
                                            contentType = currentContentType,
                                            contentLength = currentContentLength,
                                            isCollection = currentIsCollection
                                        )
                                    )
                                }
                                currentHref = null
                                currentDisplayName = null
                                currentContentType = null
                                currentContentLength = 0L
                                currentIsCollection = false
                            }
                        }
                        currentElement = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "XML parse error", e)
        } catch (e: IOException) {
            Log.e(TAG, "IO error during parse", e)
        }

        return items
    }

    // --- XML Parsing ---

    /**
     * Decodes URL-encoded href values returned by WebDAV servers.
     */
    private fun decodeHref(href: String, baseUrl: String): String {
        return try {
            // Remove base URL prefix if present
            val path = if (href.startsWith("http")) {
                // Try to make it relative to base
                if (href.startsWith(baseUrl)) {
                    href.removePrefix(baseUrl)
                } else {
                    // Extract path portion
                    val slashAfterHost = href.indexOf('/', 8) // skip https://
                    if (slashAfterHost > 0) href.substring(slashAfterHost)
                    else "/"
                }
            } else {
                href
            }
            // Decode percent-encoded characters
            java.net.URLDecoder.decode(path, "UTF-8")
        } catch (e: Exception) {
            href
        }
    }

    private fun extractFileName(href: String): String {
        val trimmed = href.trimEnd('/')
        val slashIndex = trimmed.lastIndexOf('/')
        return if (slashIndex >= 0 && slashIndex < trimmed.length - 1) {
            trimmed.substring(slashIndex + 1)
        } else {
            trimmed
        }
    }
}

/**
 * A file or directory discovered via WebDAV PROPFIND.
 */
@androidx.compose.runtime.Immutable
data class WebdavItem(
    val href: String,
    val displayName: String,
    val contentType: String?,
    val contentLength: Long,
    val isCollection: Boolean
) {
    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "wav", "ogg", "opus", "aac", "m4a", "wma",
        "alac", "ape", "aiff", "m3u", "m3u8"
    )

    val isAudioFile: Boolean
        get() {
            if (isCollection) return false
            if (contentType != null && contentType.startsWith("audio/")) return true
            val ext = displayName.substringAfterLast('.').lowercase()
            if (contentType == "application/octet-stream" || contentType == null) {
                return ext in AUDIO_EXTENSIONS
            }
            return false
        }
}
