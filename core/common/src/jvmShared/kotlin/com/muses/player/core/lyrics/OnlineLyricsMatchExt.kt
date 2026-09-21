package com.muses.player.core.lyrics

import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.lyrics.parser.LrcLyricsParser
import com.muses.player.core.model.lyrics.OnlineLyricsMatchResult
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import com.muses.player.feature.player.lyric.LyricsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 「Muses 在线歌词匹配 → [LyricsDocument]」的共用落地。
 *
 * 播放页（`PlayerViewModel`）与迷你条歌词模式（`MainViewModel`）共用同一份口径，
 * 避免两处各写一遍查询构造与格式分流。
 *
 * ## 何时用
 * 在线音源曲目**先问洛雪脚本**（`lyric` 动作），取不到时回退到这里。
 * 兜底必要性：多数洛雪自定义源脚本只声明 `musicUrl`（歌词/封面在洛雪桌面端由主程序
 * 自己的接口取，不归脚本），所以「只问脚本」会让绝大多数在线曲目没有歌词。
 *
 * ## 格式分流
 * - 无 timed 译文：走通用解析链（TTML / YRC / QRC / LRC），AMLL 命中即 TTML；
 * - 有 timed 译文（平台源的 tlyric）：走 LRC 合并链（与刮削写回同口径）。
 */
suspend fun LyricsMatcher.matchDocument(
    songId: String,
    title: String,
    artist: String? = null,
    album: String? = null,
    durationMs: Long = 0L,
    durationSec: Long = 0L,
): LyricsDocument? {
    val result = match(
        OnlineLyricsQuery(
            songId = songId,
            title = title,
            artist = artist,
            album = album,
            durationSec = durationSec.takeIf { it > 0 }?.toDouble()
                ?: durationMs.takeIf { it > 0 }?.let { it / 1000.0 },
        ),
    )
    if (result !is OnlineLyricsMatchResult.Ok) return null
    return withContext(Dispatchers.Default) {
        val translation = result.translationText
        if (translation.isNullOrBlank()) {
            LyricsParser.parseDocument(result.text)
        } else {
            LrcLyricsParser.parse(result.text, translation = translation)
        }
    }
}
