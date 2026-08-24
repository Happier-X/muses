package com.muses.player.core.media.metadata

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT
import java.io.File

/** 标签解析结果（M1 字段集；ReplayGain 仅在标签存在且合法时给出） */
data class TrackTags(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationSec: Long = 0L,
    val lyrics: String? = null,
    val replayGainTrackDb: Double? = null,
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
            replayGainTrackDb = parseReplayGainTrackDb(tag),
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

    /**
     * 读取 track 级 ReplayGain（dB）。
     * jaudiotagger 3.0.1 无 REPLAYGAIN FieldKey，走原始字段别名扫描；
     * 解析失败返回 null，调用方不得写入 0 等假增益。
     */
    fun parseReplayGainTrackDb(tag: Tag): Double? {
        firstRawFieldValue(tag, REPLAYGAIN_TRACK_ALIASES)?.let { raw ->
            parseReplayGainDbString(raw)?.let { return it }
        }
        // TXXX 自定义帧：content 形如 "REPLAYGAIN_TRACK_GAIN=-6.54 dB"
        firstRawFieldValue(tag, setOf("TXXX"))?.let { content ->
            val lower = content.lowercase()
            if (lower.contains("replaygain_track_gain") || lower.contains("r128_track_gain")) {
                parseReplayGainDbString(content.substringAfter('=', content))?.let { return it }
            }
        }
        return null
    }

    fun parseReplayGainDbString(raw: String?): Double? {
        val withoutUnit = raw?.trim()
            ?.replace(Regex("\\s*dB\\s*$", RegexOption.IGNORE_CASE), "")
            ?.trim()
            .orEmpty()
        if (withoutUnit.isEmpty()) return null
        val value = withoutUnit.toDoubleOrNull() ?: return null
        return normalizeReplayGainDbValue(value)
    }

    /** 常规 RG 已是 dB；Opus 等 R128 常为 Q7.8 整数（÷256）。无法落入合理区间则丢弃。 */
    fun normalizeReplayGainDbValue(value: Double): Double? {
        if (!value.isFinite()) return null
        if (kotlin.math.abs(value) <= REPLAY_GAIN_DB_ABS_MAX) return value
        val asQ78 = value / 256.0
        if (asQ78.isFinite() && kotlin.math.abs(asQ78) <= REPLAY_GAIN_DB_ABS_MAX) return asQ78
        return null
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
    private val REPLAYGAIN_TRACK_ALIASES = setOf(
        "REPLAYGAIN_TRACK_GAIN",
        "R128_TRACK_GAIN",
    )

    /** 合理 track gain |dB| 上限；超出则尝试 Q7.8 换算 */
    const val REPLAY_GAIN_DB_ABS_MAX = 30.0
}
