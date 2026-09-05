package com.muses.player.feature.player.lyric

/**
 * 兼容层：将旧的 AmllLyricLine 映射到新的  LyricLine。
 *
 * 这样 PlayerScreen.kt 和 PlayerViewModel.kt 可以继续使用旧的 AmllLyricLine 类型，
 * 而内部实现使用新的  类型。
 *
 * 09-05-desktop-player-lyrics Y1：AmllLyricLine/AmllWord/toAmllLyricLine(s)/LyricsParser
 * 已上收 :core:common jvmShared（同包名透传，本模块零 import 改动）；
 * 本文件仅保留依赖 accompanist lyrics-core 的 [AmllMapper]（该库不进 :core:common）。
 */

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
