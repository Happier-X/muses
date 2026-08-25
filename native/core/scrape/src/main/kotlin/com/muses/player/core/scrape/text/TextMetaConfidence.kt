package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.MatchConfidence
import com.muses.player.core.model.scrape.OnlineTextQuery

// 规格书 = src/features/metadata/util.ts 的 classifyTextMetaConfidence（child4 R4-2）

/**
 * 判定文本命中置信度（child4 R4-2）：
 * - title exact（normalize 相等）且（artist 命中 或 无查询歌手信息）→ high
 * - title contains 且 artist 命中 → high
 * - 其余（contains 无 artist 信息、artist 不命中）→ low
 *
 * 文本命中不含 duration 字段，时长约束在服务层使用 track 信息时再校验（本函数不强制）。
 */
fun classifyTextMetaConfidence(
    hitTitle: String?,
    hitArtist: String?,
    queryTitle: String?,
    queryArtist: String?,
): MatchConfidence {
    val qTitle = normalizeText(queryTitle)
    val hTitle = normalizeText(hitTitle)
    if (qTitle.isEmpty() || hTitle.isEmpty()) {
        return MatchConfidence.LOW
    }
    val titleExact = qTitle == hTitle
    val titleContains = !titleExact && (qTitle.contains(hTitle) || hTitle.contains(qTitle))
    if (!titleExact && !titleContains) {
        return MatchConfidence.LOW
    }
    val qArtist = normalizeText(queryArtist)
    val hArtist = normalizeText(hitArtist)
    val queryHasArtist = qArtist.isNotEmpty()
    val artistHit =
        qArtist.isNotEmpty() && hArtist.isNotEmpty() &&
            (qArtist == hArtist || qArtist.contains(hArtist) || hArtist.contains(qArtist))
    if (titleExact) {
        return if (queryHasArtist && !artistHit) MatchConfidence.LOW else MatchConfidence.HIGH
    }
    // contains：artist 命中才 high
    return if (artistHit) MatchConfidence.HIGH else MatchConfidence.LOW
}

/** 便捷重载：直接传 OnlineTextQuery（对应 Web 调用处传整个 query 的结构兼容行为） */
fun classifyTextMetaConfidence(
    hit: com.muses.player.core.model.scrape.TextMetaHit,
    query: OnlineTextQuery,
): MatchConfidence = classifyTextMetaConfidence(
    hitTitle = hit.title,
    hitArtist = hit.artist,
    queryTitle = query.title,
    queryArtist = query.artist,
)
