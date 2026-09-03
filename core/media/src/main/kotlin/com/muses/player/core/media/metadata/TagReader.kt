package com.muses.player.core.media.metadata

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT
import java.io.File

/** 标签解析结果 */
data class TrackTags(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationSec: Long = 0L,
    val lyrics: String? = null,
    /** 内嵌封面原始字节（jpg/png 均可能，由调用方落盘） */
    val coverBytes: ByteArray? = null,
)

/**
 * jaudiotagger 标签读取器（移植自旧工程 AudioMetadataReader，去 Capacitor 化）。
 * 纯 JVM 可测：解析入口接受 [Tag]，文件入口负责 AudioFileIO。
 */
object TagReader {

    fun read(file: File): TrackTags {
        val audioFile = AudioFileIO.read(file)
        val durationSec = audioFile.audioHeader?.trackLength?.toLong() ?: 0L
        return parse(audioFile.tag, fallbackDurationSec = durationSec)
    }

    fun parse(tag: Tag?, fallbackDurationSec: Long = 0L): TrackTags {
        if (tag == null) return TrackTags(durationSec = fallbackDurationSec)
        return TrackTags(
            title = firstTagValue(tag, FieldKey.TITLE, TITLE_ALIASES),
            artist = firstTagValue(tag, FieldKey.ARTIST, ARTIST_ALIASES),
            album = firstTagValue(tag, FieldKey.ALBUM, ALBUM_ALIASES),
            durationSec = fallbackDurationSec,
            lyrics = firstLyricsValue(tag),
            coverBytes = runCatching { tag.firstArtwork?.binaryData }.getOrNull()?.takeIf { it.isNotEmpty() },
        )
    }

    fun firstTagValue(tag: Tag, key: FieldKey, aliases: Set<String>): String? {
        runCatching { tag.getFirst(key) }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        return firstRawFieldValue(tag, aliases)
    }

    private fun firstLyricsValue(tag: Tag): String? {
        extractEmbeddedLyrics(runCatching { tag.getFirst(FieldKey.LYRICS) }.getOrNull())?.let { return it }
        return firstRawFieldValue(tag, LYRICS_ALIASES)?.let(::extractEmbeddedLyrics)
    }

    fun firstRawFieldValue(tag: Tag, aliases: Set<String>): String? {
        val normalizedAliases = aliases.map { normalizeFieldId(it) }.toSet()
        return allFields(tag).firstNotNullOfOrNull { field ->
            val fieldId = normalizeFieldId(field.id)
            if (fieldId !in normalizedAliases) return@firstNotNullOfOrNull null
            readFieldValue(field)?.trim()?.takeIf { it.isNotBlank() }
        }
    }

    private fun allFields(tag: Tag): Sequence<org.jaudiotagger.tag.TagField> = sequence {
        val fields = runCatching { tag.fields }.getOrNull() ?: return@sequence
        while (fields.hasNext()) {
            yield(fields.next())
        }
    }

    private fun readFieldValue(field: org.jaudiotagger.tag.TagField): String? {
        // USLT 帧：getContent 可能带语言/描述前缀，直接读 lyric 正文
        ((field as? AbstractID3v2Frame)?.body as? FrameBodyUSLT)?.let { uslt ->
            uslt.lyric?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        val content = runCatching {
            field.javaClass.methods
                .firstOrNull { it.name == "getContent" && it.parameterTypes.isEmpty() }
                ?.invoke(field) as? String
        }.getOrNull()
        return content ?: field.toString()
    }

    private fun extractEmbeddedLyrics(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return trimmed.takeIf { it.contains("[") && it.contains("]") }
    }

    private fun normalizeFieldId(value: String?): String =
        value.orEmpty().trim().lowercase().replace("_", "").replace("-", "")

    private val TITLE_ALIASES = setOf("TITLE", "TIT2", "©nam", "NAME")
    private val ARTIST_ALIASES = setOf("ARTIST", "ALBUMARTIST", "TPE1", "TPE2", "©ART", "AUTHOR", "PERFORMER")
    private val ALBUM_ALIASES = setOf("ALBUM", "TALB", "©alb")
    private val LYRICS_ALIASES = setOf("LYRICS", "UNSYNCEDLYRICS", "USLT", "SYLT", "DESCRIPTION", "DESC", "©lyr")
}
