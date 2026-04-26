package com.example.muses.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.muses.data.model.AudioTrack
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object MetadataExtractor {

    private const val TAG = "MetadataExtractor"
    private const val ALBUM_ART_DIR = "album_art"

    data class Metadata(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val durationMs: Long? = null,
        val albumArtUri: Uri? = null
    )

    fun extractFromUri(context: Context, uri: Uri): Metadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            readMetadata(retriever, context, uri.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract metadata from $uri", e)
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun extractFromUrl(context: Context, url: String, authHeader: String? = null): Metadata? {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        val retriever = MediaMetadataRetriever()
        return try {
            val requestBuilder = Request.Builder().url(url)
            if (authHeader != null) {
                requestBuilder.header("Authorization", authHeader)
            }
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }
            val body = response.body ?: return null
            val tempFile = File.createTempFile("meta_", ".tmp", context.cacheDir)
            try {
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buf = ByteArray(8192)
                        var totalRead = 0L
                        val maxBytes = 512 * 1024L
                        while (totalRead < maxBytes) {
                            val read = input.read(buf)
                            if (read == -1) break
                            output.write(buf, 0, read)
                            totalRead += read
                        }
                    }
                }
                response.close()
                extractFromUri(context, Uri.fromFile(tempFile))
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract metadata from URL $url", e)
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun extractFromUrlWithTempFile(
        context: Context,
        url: String,
        authHeader: String? = null
    ): Metadata? = extractFromUrl(context, url, authHeader)

    private fun readMetadata(retriever: MediaMetadataRetriever, context: Context, key: String): Metadata {
        val albumArtUri = saveAlbumArt(context, retriever, key)
        return Metadata(
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull(),
            albumArtUri = albumArtUri
        )
    }

    private fun saveAlbumArt(context: Context, retriever: MediaMetadataRetriever, key: String): Uri? {
        val embeddedPicture = try {
            retriever.embeddedPicture
        } catch (_: Exception) {
            null
        } ?: return null

        // Validate it's a decodable image
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        val artDir = File(context.filesDir, ALBUM_ART_DIR)
        if (!artDir.exists()) artDir.mkdirs()

        val hash = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray())
            .take(8)
            .joinToString("") { "%02x".format(it) }
        val artFile = File(artDir, "$hash.jpg")

        try {
            FileOutputStream(artFile).use { it.write(embeddedPicture) }
            return Uri.fromFile(artFile)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save album art for $key", e)
            return null
        }
    }

    fun applyMetadata(track: AudioTrack, metadata: Metadata): AudioTrack {
        return track.copy(
            title = metadata.title?.takeIf { it.isNotBlank() } ?: track.title,
            artist = metadata.artist?.takeIf { it.isNotBlank() } ?: track.artist,
            album = metadata.album?.takeIf { it.isNotBlank() } ?: track.album,
            durationMs = metadata.durationMs ?: track.durationMs,
            albumArtUri = metadata.albumArtUri ?: track.albumArtUri
        )
    }
}