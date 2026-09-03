package com.muses.player.feature.player.lyric

import com.muses.player.core.lyrics.model.LyricLine
import com.muses.player.core.lyrics.model.LyricSyllable
import com.muses.player.core.lyrics.model.LyricsDocument

/**
 * 兼容层：将旧的 AmllLyricLine 映射到新的  LyricLine。
 *
 * 这样 PlayerScreen.kt 和 PlayerViewModel.kt 可以继续使用旧的 AmllLyricLine 类型，
 * 而内部实现使用新的  类型。
 */

/**
 * 旧的 AmllLyricLine 数据类（兼容层）
 */
data class AmllLyricLine(
    val words: List<AmllWord>,
    val startTime: Int,
    val endTime: Int,
    val translatedLyric: String,
    val romanLyric: String,
    val isBG: Boolean,
    val isDuet: Boolean,
)

/**
 * 旧的 AmllWord 数据类（兼容层）
 */
data class AmllWord(
    val startTime: Int,
    val endTime: Int,
    val word: String,
)

/**
 * 将  LyricLine 转换为旧的 AmllLyricLine
 */
fun LyricLine.toAmllLyricLine(): AmllLyricLine {
    return AmllLyricLine(
        words = syllables.map { syllable ->
            AmllWord(
                startTime = syllable.startTimeMs.toInt(),
                endTime = syllable.endTimeMs.toInt(),
                word = syllable.text,
            )
        }.ifEmpty {
            // 如果没有音节，创建一个覆盖整行的单词
            listOf(
                AmllWord(
                    startTime = timeMs.toInt(),
                    endTime = (timeMs + (durationMs ?: 3000L)).toInt(),
                    word = text,
                )
            )
        },
        startTime = timeMs.toInt(),
        endTime = (timeMs + (durationMs ?: 3000L)).toInt(),
        translatedLyric = translation.orEmpty(),
        romanLyric = romanization.orEmpty(),
        isBG = accompaniment.isNotEmpty(),
        isDuet = false,
    )
}

/**
 * 将  LyricsDocument 转换为旧的 AmllLyricLine 列表
 */
fun LyricsDocument.toAmllLyricLines(): List<AmllLyricLine> {
    return lines.map { it.toAmllLyricLine() }
}

/**
 * 旧的 LyricsParser 兼容层
 */
object LyricsParser {
    /**
     * 解析歌词文本，返回旧的 AmllLyricLine 列表
     */
    fun parse(raw: String?): List<AmllLyricLine>? {
        return parseDocument(raw)?.toAmllLyricLines()
    }

    /**
     * 解析歌词文本，返回  LyricsDocument
     */
    fun parseDocument(raw: String?): com.muses.player.core.lyrics.model.LyricsDocument? {
        if (raw.isNullOrBlank()) return null
        return try {
            // 尝试 TTML 解析
            val ttmlDocument = com.muses.player.core.lyrics.parser.TtmlLyricsParser.parse(raw)
            if (ttmlDocument.lines.isNotEmpty()) return ttmlDocument

            // 尝试 YRC 解析（网易逐字；必须在 KRC 之前——两者行头同形，KRC 会误食 YRC）
            val yrcLines = com.muses.player.core.lyrics.model.NeteaseLyricParser.parseYrc(raw)
            if (yrcLines.isNotEmpty()) {
                return com.muses.player.core.lyrics.model.LyricsDocument(
                    lines = yrcLines,
                    quality = com.muses.player.core.lyrics.model.LyricQuality.WordSynchronized,
                )
            }

            // 尝试 KRC 解析
            val krcDocument = com.muses.player.core.lyrics.parser.KugouKrcLyricsParser.parse(raw)
            if (krcDocument.lines.isNotEmpty()) return krcDocument

            // 尝试 QRC 解析（QQ音乐，可能是解密后的文本）
            try {
                val qrcDocument = com.muses.player.core.lyrics.parser.QQMusicQrcLyricsParser.parse(raw)
                if (qrcDocument.lines.isNotEmpty()) return qrcDocument
            } catch (_: Exception) { /* ignore */ }

            // 尝试 LRC 解析
            val lrcDocument = com.muses.player.core.lyrics.parser.LrcLyricsParser.parse(raw)
            if (lrcDocument.lines.isNotEmpty()) return lrcDocument

            null
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * 旧的 AmllMapper 兼容层
 */
object AmllMapper {
    /**
     * 将旧的 SyncedLyrics 转换为 AmllLyricLine 列表
     */
    fun toAmllLines(synced: com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics): List<AmllLyricLine> {
        return synced.lines.map { line ->
            when (line) {
                is com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine -> {
                    AmllLyricLine(
                        words = line.syllables.map { syllable ->
                            AmllWord(
                                startTime = syllable.start,
                                endTime = syllable.end,
                                word = syllable.content,
                            )
                        },
                        startTime = line.start,
                        endTime = line.end,
                        translatedLyric = line.translation.orEmpty(),
                        romanLyric = line.phonetic.orEmpty(),
                        isBG = line is com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine.AccompanimentKaraokeLine,
                        isDuet = false,
                    )
                }
                else -> {
                    val syncedLine = line as? com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
                    val content = syncedLine?.content ?: ""
                    AmllLyricLine(
                        words = listOf(
                            AmllWord(
                                startTime = line.start,
                                endTime = line.end,
                                word = content,
                            )
                        ),
                        startTime = line.start,
                        endTime = line.end,
                        translatedLyric = syncedLine?.translation.orEmpty(),
                        romanLyric = "",
                        isBG = false,
                        isDuet = false,
                    )
                }
            }
        }
    }
}
