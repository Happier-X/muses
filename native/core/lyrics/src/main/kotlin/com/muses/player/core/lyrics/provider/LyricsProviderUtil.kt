package com.muses.player.core.lyrics.provider

import com.muses.player.core.model.lyrics.OnlineLyricsQuery

/**
 * provider 搜索辅助（规格书 = src/features/lyrics/providers/util.ts，逐函数翻译）。
 */

/** 关键词：title/artist/album 过滤空后空格连接（util.ts buildKeyword） */
fun buildKeyword(query: OnlineLyricsQuery): String =
    listOfNotNull(
        query.title.takeIf { it.isNotBlank() },
        query.artist?.takeIf { it.isNotBlank() },
        query.album?.takeIf { it.isNotBlank() },
    ).joinToString(" ").trim()

/** 候选条目公共字段（scoreSearchHit 入参形态） */
interface ScoreableHit {
    val title: String?
    val artist: String?
    val album: String?
}

private fun scoreSearchHit(hit: ScoreableHit, query: OnlineLyricsQuery): Int {
    val t = hit.title?.trim()
    val a = hit.artist?.trim()
    val al = hit.album?.trim()
    val qTitle = query.title.trim()
    val qArtist = query.artist?.trim().orEmpty()
    val qAlbum = query.album?.trim().orEmpty()

    var score = 0
    if (t != null && qTitle.isNotEmpty() && (t == qTitle || t.contains(qTitle) || qTitle.contains(t))) score += 10
    if (a != null && qArtist.isNotEmpty() && (a == qArtist || a.contains(qArtist) || qArtist.contains(a))) score += 6
    if (al != null && qAlbum.isNotEmpty() && (al == qAlbum || al.contains(qAlbum) || qAlbum.contains(al))) score += 3
    return score
}

/** 打分取最优候选（util.ts pickBest；stable 排序对齐 JS sort 语义） */
fun <T : ScoreableHit> pickBest(items: List<T>, query: OnlineLyricsQuery): T? {
    if (items.isEmpty()) return null
    return items.sortedByDescending { scoreSearchHit(it, query) }.first()
}

/** lrclist → 标准 LRC（util.ts linesToLrc） */
fun linesToLrc(lines: List<LrcLine>): String {
    val out = mutableListOf<String>()
    for (line in lines) {
        val text = line.text?.trim()
        if (text.isNullOrEmpty()) continue
        val rawTime = line.time ?: continue
        val seconds = when (rawTime) {
            is Double -> rawTime
            is Long -> rawTime.toDouble()
            else -> rawTime.toString().toDoubleOrNull() ?: continue
        }
        if (!seconds.isFinite()) continue
        val totalMs = (seconds * 1000).let { if (it < 0) 0.0 else it }.toInt()
        val m = totalMs / 60000
        val s = (totalMs % 60000) / 1000
        val ms = totalMs % 1000
        out.add("[%02d:%02d.%03d]%s".format(m, s, ms, text))
    }
    return out.joinToString("\n")
}

data class LrcLine(val time: Any?, val text: String?)
