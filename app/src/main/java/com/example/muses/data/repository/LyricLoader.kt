package com.example.muses.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.muses.ui.util.LrcParser
import com.example.muses.ui.util.LyricLine
import com.example.muses.ui.util.ParsedLyrics
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Loads lyrics from external LRC files (local or WebDAV).
 */
object LyricLoader {
    private const val TAG = "LyricLoader"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Load lyrics from a local LRC file.
     * @param lrcUri URI to the .lrc file
     */
    fun loadFromUri(context: Context, lrcUri: Uri): ParsedLyrics? {
        return try {
            context.contentResolver.openInputStream(lrcUri)?.use { input ->
                LrcParser.parse(input)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load lyrics from $lrcUri", e)
            null
        }
    }

    /**
     * Load lyrics from a WebDAV URL.
     * @param url WebDAV URL to the .lrc file
     * @param authHeader Optional Basic auth header
     */
    fun loadFromWebdav(url: String, authHeader: String? = null): ParsedLyrics? {
        return try {
            val requestBuilder = Request.Builder().url(url)
            if (authHeader != null) {
                requestBuilder.header("Authorization", authHeader)
            }
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "WebDAV request failed: ${response.code} for $url")
                response.close()
                return null
            }
            response.body?.byteStream()?.use { input ->
                LrcParser.parse(input)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load lyrics from WebDAV $url", e)
            null
        }
    }

    /**
     * Try to find and load LRC file for a given audio file.
     * Priority: external LRC file > embedded ID3 lyrics
     * @param audioUri The audio file URI
     * @param isWebdav Whether the file is from WebDAV
     * @param authHeader Optional Basic auth header for WebDAV
     * @return Parsed lyrics if found, null otherwise
     */
    fun loadForAudio(
        context: Context,
        audioUri: Uri,
        isWebdav: Boolean = false,
        authHeader: String? = null
    ): ParsedLyrics? {
        Log.d(TAG, "loadForAudio: uri=$audioUri, isWebdav=$isWebdav")

        // Try external LRC file first
        val externalLyrics = if (isWebdav) {
            val url = audioUri.toString()
            val lrcUrl = url.replace(Regex("\\.[^.]+$"), ".lrc")
            Log.d(TAG, "Trying WebDAV LRC: $lrcUrl")
            loadFromWebdav(lrcUrl, authHeader)
        } else {
            val lrcUri = findLocalLrcUri(context, audioUri)
            Log.d(TAG, "Local LRC URI: $lrcUri")
            lrcUri?.let { loadFromUri(context, it) }
        }

        if (externalLyrics != null && externalLyrics.lines.isNotEmpty()) {
            Log.d(TAG, "Found external LRC file with ${externalLyrics.lines.size} lines")
            return externalLyrics
        }

        // Try embedded ID3 lyrics
        Log.d(TAG, "No external LRC file with content, trying ID3 embedded lyrics")
        return loadEmbeddedLyrics(context, audioUri, isWebdav, authHeader)
    }

    /**
     * Load embedded lyrics from ID3 tags.
     */
    private fun loadEmbeddedLyrics(
        context: Context,
        audioUri: Uri,
        isWebdav: Boolean,
        authHeader: String?
    ): ParsedLyrics? {
        return try {
            if (isWebdav) {
                // Download and extract ID3 lyrics
                val url = audioUri.toString()
                Log.d(TAG, "Loading embedded lyrics from WebDAV: $url")
                val requestBuilder = Request.Builder().url(url)
                if (authHeader != null) {
                    requestBuilder.header("Authorization", authHeader)
                }
                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "WebDAV request failed: ${response.code}")
                    response.close()
                    return null
                }
                response.body?.byteStream()?.use { input ->
                    val lyricsText = Id3LyricsExtractor.extractFromStream(input)
                    Log.d(TAG, "ID3 extraction result: ${lyricsText?.take(100)}...")
                    lyricsText?.let { parseLyricsText(it) }
                }
            } else {
                Log.d(TAG, "Loading embedded lyrics from local: $audioUri")
                val lyricsText = Id3LyricsExtractor.extractFromUri(context, audioUri)
                Log.d(TAG, "ID3 extraction result: ${lyricsText?.take(100)}...")
                lyricsText?.let { parseLyricsText(it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load embedded lyrics from $audioUri", e)
            null
        }
    }

    /**
     * Parse lyrics text, handling both LRC format and plain text.
     */
    private fun parseLyricsText(text: String): ParsedLyrics? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // Check if it's LRC format (contains timestamps)
        val hasTimestamps = trimmed.contains(Regex("\\[\\d{1,2}:\\d{2}"))
        return if (hasTimestamps) {
            LrcParser.parse(trimmed)
        } else {
            // Plain text lyrics - create a single line at time 0
            ParsedLyrics(
                lines = trimmed.lines()
                    .filter { it.isNotBlank() }
                    .mapIndexed { index, line ->
                        LyricLine(timeMs = index * 5000L, text = line.trim())  // 5s per line as fallback
                    }
            )
        }
    }

    /**
     * Find the corresponding .lrc file for a local audio file.
     */
    private fun findLocalLrcUri(context: Context, audioUri: Uri): Uri? {
        // Try to get file path from content URI
        val path = audioUri.path ?: return null
        val lrcPath = path.replace(Regex("\\.[^.]+$"), ".lrc")
        val lrcFile = File(lrcPath)

        return if (lrcFile.exists()) {
            Uri.fromFile(lrcFile)
        } else {
            // Try content resolver query for .lrc
            try {
                val parent = File(lrcPath).parentFile ?: return null
                val fileName = File(lrcPath).name
                // Look for file in same directory via content resolver
                val children = context.contentResolver.query(
                    Uri.fromFile(parent),
                    null,
                    null,
                    null,
                    null
                )
                children?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex("_display_name")
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIndex)
                        if (name.equals(fileName, ignoreCase = true)) {
                            // Found matching .lrc file
                            return Uri.withAppendedPath(Uri.fromFile(parent), name)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query for .lrc file", e)
            }
            null
        }
    }
}
