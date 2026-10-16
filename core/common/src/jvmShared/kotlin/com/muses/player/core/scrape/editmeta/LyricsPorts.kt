package com.muses.player.core.scrape.editmeta

import com.muses.player.core.lyrics.amll.AmllTtmlDbClient
import com.muses.player.core.model.lyrics.AmllMatchQuery
import com.muses.player.core.model.lyrics.AmllFailReason
import com.muses.player.core.model.lyrics.AmllMatchResult
import com.muses.player.core.model.lyrics.LyricsProvider
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import com.muses.player.core.network.captureProviderFailures
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * editMeta 歌词维度 ↔ 歌词在线搜索的端口适配（任务 08-25-native-lyrics-online L3）。
 *
 * - [AmllLyricsPort]：AMLL TTML 聚合库，始终参与、不随平台过滤（对齐 searchEditCloudMeta.ts）
 * - [ProviderLyricsPort]：平台五源 / LRCLIB，按 id 参与平台过滤
 *
 * 09-05-lyrics-kmp：随 core:lyrics KMP 化上收 :core:common jvmShared（引用 jvmShared
 * AmllTtmlDbClient，不能进 commonMain）；同包名，安卓 ScrapeModule 装配零改动，
 * 桌面 DesktopScrapeGraph 直接 import。
 */

/** AMLL 端口（id = "amll"，与 EditCloudMetaSearch.AMLL_PORT_ID 对齐） */
class AmllLyricsPort(private val client: AmllTtmlDbClient) : LyricsSearchPort {

    override val id: String = "amll"

    override suspend fun searchLyrics(query: EditCloudMetaQuery): LyricsHit? {
        var failure: AmllFailReason? = null
        // 先保留底层 HTTP 失败，避免把限流或鉴权失败折叠成通用网络错误。
        val hit = captureProviderFailures {
            when (val result = client.match(toAmllQuery(query))) {
                is AmllMatchResult.Ok -> LyricsHit(text = result.ttml, format = "ttml")
                is AmllMatchResult.Fail -> { failure = result.reason; null }
            }
        }
        if (hit != null) return hit
        return when (failure) {
            AmllFailReason.NETWORK -> throw IOException("AMLL 来源请求失败")
            AmllFailReason.PARSE -> throw SerializationException("AMLL 歌词响应异常")
            AmllFailReason.ABORTED -> throw EditSearchAbortedException()
            AmllFailReason.NO_MATCH, null -> null
        }
    }

    private fun toAmllQuery(query: EditCloudMetaQuery) = AmllMatchQuery(
        songId = query.songId,
        title = query.title,
        artist = query.artist,
        album = query.album,
        durationSec = query.durationSec,
        bypassNegativeCache = true,
    )
}

/** 平台 / LRCLIB provider 端口 */
class ProviderLyricsPort(private val delegate: LyricsProvider) : LyricsSearchPort {

    override val id: String = delegate.id.wire

    override suspend fun searchLyrics(query: EditCloudMetaQuery): LyricsHit? {
        val hit = delegate.searchLyrics(
            OnlineLyricsQuery(
                songId = query.songId,
                title = query.title,
                artist = query.artist,
                album = query.album,
                durationSec = query.durationSec,
            ),
        ) ?: return null
        return LyricsHit(
            text = hit.text,
            format = hit.format.wire,
            translationText = hit.translationText,
        )
    }
}
