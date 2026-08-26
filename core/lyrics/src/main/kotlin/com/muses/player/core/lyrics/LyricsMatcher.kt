package com.muses.player.core.lyrics

import com.muses.player.core.model.lyrics.LyricsProvider
import com.muses.player.core.model.lyrics.OnlineLyricsFailReason
import com.muses.player.core.model.lyrics.OnlineLyricsMatchResult
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import com.muses.player.core.lyrics.amll.AmllTtmlDbClient

/**
 * 在线歌词编排（规格书 = src/features/lyrics/match.ts matchOnlineLyrics）：
 * amll TTML 优先 → 可插拔 fallback（平台五源 → LRCLIB）串行，任一命中即停。
 * 不写库；由调用方写入播放状态。
 */
class LyricsMatcher(
    private val amllClient: AmllTtmlDbClient,
    private val fallbackProviders: List<LyricsProvider>,
) {

    /**
     * amll 优先；miss/失败后串行 fallback；任一命中即停。
     * 结局区分：sawNetwork → network；`sawParse && fallbackProviders.isEmpty()` → parse；否则 no-match。
     */
    suspend fun match(query: OnlineLyricsQuery): OnlineLyricsMatchResult {
        val title = query.title.trim()
        val songId = query.songId.trim()
        if (title.isEmpty() || songId.isEmpty()) {
            return OnlineLyricsMatchResult.Fail(OnlineLyricsFailReason.NO_MATCH)
        }

        val amll = amllClient.match(
            com.muses.player.core.model.lyrics.AmllMatchQuery(
                songId = songId,
                title = title,
                artist = query.artist,
                album = query.album,
                durationSec = query.durationSec,
            ),
        )

        if (amll is com.muses.player.core.model.lyrics.AmllMatchResult.Ok) {
            return OnlineLyricsMatchResult.Ok(
                text = amll.ttml,
                format = com.muses.player.core.model.lyrics.OnlineLyricsFormat.TTML,
                source = com.muses.player.core.model.lyrics.OnlineLyricsSource.AMLL,
                confidence = amll.confidence,
            )
        }
        if (amll !is com.muses.player.core.model.lyrics.AmllMatchResult.Fail) {
            // 理论不可达：密封接口只有 Ok/Fail
            return OnlineLyricsMatchResult.Fail(OnlineLyricsFailReason.NO_MATCH)
        }

        var sawNetwork = amll.reason == com.muses.player.core.model.lyrics.AmllFailReason.NETWORK
        val sawParse = amll.reason == com.muses.player.core.model.lyrics.AmllFailReason.PARSE

        for (provider in fallbackProviders) {
            try {
                val hit = provider.searchLyrics(query)
                val text = hit?.text?.trim()
                if (!text.isNullOrEmpty()) {
                    val translationText = hit.translationText?.trim()
                    return OnlineLyricsMatchResult.Ok(
                        text = text,
                        format = hit.format,
                        source = provider.id,
                        translationText = translationText?.takeIf { it.isNotEmpty() },
                    )
                }
            } catch (_: Exception) {
                sawNetwork = true
            }
        }

        if (sawNetwork) {
            return OnlineLyricsMatchResult.Fail(OnlineLyricsFailReason.NETWORK)
        }
        if (sawParse && fallbackProviders.isEmpty()) {
            return OnlineLyricsMatchResult.Fail(OnlineLyricsFailReason.PARSE)
        }
        return OnlineLyricsMatchResult.Fail(OnlineLyricsFailReason.NO_MATCH)
    }
}
