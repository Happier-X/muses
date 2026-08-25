package com.muses.player.core.scrape.cover.provider

import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.text.normalizeText

/**
 * 封面 provider 共用辅助：
 * - 关键词拼装与打分 10/6/3 部分在 Web 各 provider（tx/kg/mg/wy/itunes）中逐文件重复，
 *   逻辑完全一致，此处收敛为一个实现；各 provider 的 +1 条件各自保留。
 */

/** 各 provider 的关键词拼装：[title, artist, album].filter(trim).join(' ').trim() */
internal fun buildCoverKeyword(query: OnlineCoverQuery): String =
    listOfNotNull(
        query.title?.takeIf { it.isNotBlank() },
        query.artist?.takeIf { it.isNotBlank() },
        query.album?.takeIf { it.isNotBlank() },
    ).joinToString(" ").trim()

/** OnlineCoverQuery → normalizeText 打分入参（避免各 provider 重复 normalize 六个字段） */
internal class CoverScoreInput(query: OnlineCoverQuery) {
    val qTitle: String = normalizeText(query.title)
    val qArtist: String = normalizeText(query.artist)
    val qAlbum: String = normalizeText(query.album)
}

/**
 * 相关性打分共有部分：title=10 / artist=6 / album=3。
 * 比较规则 = normalize 后相等或互相包含（normalize.ts + 各 provider scoreItem）。
 */
internal fun scoreRelated(
    title: String,
    artist: String,
    album: String,
    qTitle: String,
    qArtist: String,
    qAlbum: String,
): Int {
    var score = 0
    fun related(a: String, b: String): Boolean = a == b || a.contains(b) || b.contains(a)
    if (title.isNotEmpty() && qTitle.isNotEmpty() && related(title, qTitle)) score += 10
    if (artist.isNotEmpty() && qArtist.isNotEmpty() && related(artist, qArtist)) score += 6
    if (album.isNotEmpty() && qAlbum.isNotEmpty() && related(album, qAlbum)) score += 3
    return score
}
