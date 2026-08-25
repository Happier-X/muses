package com.muses.player.core.scrape.editmeta

import com.muses.player.core.lyrics.LyricsMatcher
import com.muses.player.core.lyrics.amll.AmllTtmlDbClient
import com.muses.player.core.model.lyrics.AmllMatchQuery
import com.muses.player.core.model.lyrics.LyricsProvider
import com.muses.player.core.model.lyrics.OnlineLyricsQuery

/**
 * editMeta 歌词维度 ↔ 歌词在线搜索的端口适配（任务 08-25-native-lyrics-online L3）。
 *
 * - [AmllLyricsPort]：AMLL TTML 聚合库，始终参与、不随平台过滤（对齐 searchEditCloudMeta.ts）
 * - [ProviderLyricsPort]：平台五源 / LRCLIB，按 id 参与平台过滤
 */

/** AMLL 端口（id = "amll"，与 EditCloudMetaSearch.AMLL_PORT_ID 对齐） */
class AmllLyricsPort(private val client: AmllTtmlDbClient) : LyricsSearchPort {

    override val id: String = "amll"

    override suspend fun searchLyrics(query: EditCloudMetaQuery): LyricsHit? {
        return when (val result = client.match(toAmllQuery(query))) {
            is com.muses.player.core.model.lyrics.AmllMatchResult.Ok ->
                LyricsHit(text = result.ttml, format = "ttml")
            else -> null
        }
    }

    private fun toAmllQuery(query: EditCloudMetaQuery) = AmllMatchQuery(
        songId = query.songId,
        title = query.title,
        artist = query.artist,
        album = query.album,
        durationSec = query.durationSec,
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
