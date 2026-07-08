package com.muses.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import java.io.File
import java.security.MessageDigest
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagField
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT

class AudioMetadataReader(private val context: Context) {
    fun readFromFile(file: File, cacheKey: String): JSObject {
        if (!file.exists() || file.length() <= 0L) {
            throw AudioMetadataException("empty_file", "音频缓存为空，无法读取标签。")
        }

        val result = JSObject()
        val diagnostics = mutableSetOf<String>()

        try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag

            putStringMetadata(result, "title", firstTagValue(tag, FieldKey.TITLE, TITLE_FIELD_ALIASES))
            putStringMetadata(result, "artist", firstTagValue(tag, FieldKey.ARTIST, ARTIST_FIELD_ALIASES))
            putStringMetadata(result, "album", firstTagValue(tag, FieldKey.ALBUM, ALBUM_FIELD_ALIASES))

            val duration = audioFile.audioHeader?.trackLength ?: 0
            if (duration > 0) {
                result.put("duration", duration.toDouble())
            }

            tag?.let { activeTag ->
                firstLyricsValue(activeTag)?.let { lyrics ->
                    result.put("lyrics", lyrics)
                    result.put("lyricsSource", "embedded")
                }
                activeTag.firstArtwork?.binaryData?.let { picture ->
                    writeCover(cacheKey, picture)?.let { result.put("coverUri", it) }
                }
            }
        } catch (exception: Exception) {
            diagnostics.add("parse_failed")
        }

        readWithMediaMetadataRetriever(file, cacheKey, result, diagnostics)

        if (!hasAnyTag(result)) {
            diagnostics.add("no_tags")
        }
        if (!result.has("lyrics")) {
            diagnostics.add("no_lyrics")
        }
        putDiagnostics(result, diagnostics)

        return result
    }

    fun readFromContentUri(uri: Uri, cacheKey: String): JSObject {
        val tempFile = File.createTempFile("metadata-", ".audio", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw AudioMetadataException("content_uri_not_found", "音频文件不可访问。")
            return readFromFile(tempFile, cacheKey)
        } finally {
            tempFile.delete()
        }
    }

    private fun readWithMediaMetadataRetriever(
        file: File,
        cacheKey: String,
        result: JSObject,
        diagnostics: MutableSet<String>,
    ) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            putStringMetadataIfMissing(result, "title", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
            putStringMetadataIfMissing(result, "artist", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
            putStringMetadataIfMissing(result, "album", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
            putDurationIfMissing(result, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
            if (!result.has("coverUri")) {
                retriever.embeddedPicture?.let { picture ->
                    writeCover(cacheKey, picture)?.let { result.put("coverUri", it) }
                }
            }
        } catch (exception: Exception) {
            diagnostics.add("fallback_failed")
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun firstTagValue(tag: Tag?, key: FieldKey, aliases: Set<String>): String? {
        if (tag == null) {
            return null
        }

        tag.getFirst(key)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        return firstRawFieldValue(tag, aliases)
    }

    private fun firstLyricsValue(tag: Tag): String? {
        extractEmbeddedLyrics(tag.getFirst(FieldKey.LYRICS))?.let { return it }
        return firstRawFieldValue(tag, LYRICS_FIELD_ALIASES)?.let(::extractEmbeddedLyrics)
    }

    private fun firstRawFieldValue(tag: Tag, aliases: Set<String>): String? {
        val normalizedAliases = aliases.map { normalizeFieldId(it) }.toSet()
        return allFields(tag).firstNotNullOfOrNull { field ->
            val fieldId = normalizeFieldId(field.id)
            if (fieldId !in normalizedAliases) {
                return@firstNotNullOfOrNull null
            }
            readFieldValue(field)?.trim()?.takeIf { it.isNotBlank() }
        }
    }

    private fun allFields(tag: Tag): Sequence<TagField> {
        return sequence {
            val fields = runCatching { tag.fields }.getOrNull() ?: return@sequence
            while (fields.hasNext()) {
                yield(fields.next())
            }
        }
    }

    private fun readFieldValue(field: TagField): String? {
        readUnsyncedLyrics(field)?.let { return it }

        val rawContent = runCatching {
            val method = field.javaClass.methods.firstOrNull { it.name == "getContent" && it.parameterTypes.isEmpty() }
            method?.invoke(field) as? String
        }.getOrNull()

        return rawContent ?: field.toString()
    }

    private fun readUnsyncedLyrics(field: TagField): String? {
        val body = (field as? AbstractID3v2Frame)?.body as? FrameBodyUSLT ?: return null
        return body.lyric?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun extractEmbeddedLyrics(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return trimmed.takeIf { it.contains("[") && it.contains("]") }
    }

    private fun writeCover(cacheKey: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) {
            return null
        }
        val directory = File(context.cacheDir, "covers").apply { mkdirs() }
        val file = File(directory, "${sha256(cacheKey)}.jpg")
        file.writeBytes(bytes)
        return Uri.fromFile(file).toString()
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun putStringMetadata(result: JSObject, key: String, value: String?) {
        if (!value.isNullOrBlank()) {
            result.put(key, value)
        }
    }

    private fun putStringMetadataIfMissing(result: JSObject, key: String, value: String?) {
        if (!result.has(key)) {
            putStringMetadata(result, key, value)
        }
    }

    private fun putDurationIfMissing(result: JSObject, durationMs: String?) {
        if (result.has("duration")) {
            return
        }
        val durationSeconds = durationMs?.toLongOrNull()?.takeIf { it > 0L }?.div(1000.0) ?: return
        result.put("duration", durationSeconds)
    }

    private fun hasAnyTag(result: JSObject): Boolean {
        return result.has("title") || result.has("artist") || result.has("album") || result.has("duration") || result.has("coverUri") || result.has("lyrics")
    }

    private fun putDiagnostics(result: JSObject, diagnostics: Set<String>) {
        if (diagnostics.isEmpty()) {
            return
        }
        val diagnosticObject = JSObject()
        val codes = JSArray()
        diagnostics.forEach { codes.put(it) }
        diagnosticObject.put("codes", codes)
        result.put("metadataDiagnostic", diagnosticObject)
    }

    private fun normalizeFieldId(value: String?): String {
        return value.orEmpty().trim().lowercase().replace("_", "").replace("-", "")
    }

    private companion object {
        val TITLE_FIELD_ALIASES = setOf("TITLE", "TIT2", "©nam", "NAME")
        val ARTIST_FIELD_ALIASES = setOf("ARTIST", "ALBUM_ARTIST", "TPE1", "TPE2", "©ART", "AUTHOR", "PERFORMER")
        val ALBUM_FIELD_ALIASES = setOf("ALBUM", "TALB", "©alb")
        val LYRICS_FIELD_ALIASES = setOf("LYRICS", "UNSYNCEDLYRICS", "UNSYNCED_LYRICS", "UNSYNCED LYRICS", "USLT", "SYLT", "DESCRIPTION", "DESC", "©lyr")
    }
}

class AudioMetadataException(
    val diagnosticCode: String,
    message: String,
) : Exception(message)
