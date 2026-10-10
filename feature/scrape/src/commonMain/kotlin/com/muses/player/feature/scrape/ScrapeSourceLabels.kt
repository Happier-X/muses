package com.muses.player.feature.scrape

/**
 * 刮削候选来源展示名：wire 值与搜索页/歌词提供方保持一致。
 *
 * 歌词维度含聚合库 [amll]（AMLL TTML DB）与 lrclib；文本/封面为平台五源与 iTunes。
 * 未知值原样展示，避免新来源接入后被吞掉。
 */
internal fun scrapeSourceLabel(source: String?): String = when (source?.trim()?.lowercase()) {
    null, "" -> "未知来源"
    "amll" -> "AMLL DB"
    "lrclib" -> "LRCLIB"
    "wy" -> "网易云"
    "tx", "qrc" -> "QQ 音乐"
    "kg" -> "酷狗"
    "kw" -> "酷我"
    "mg" -> "咪咕"
    "itunes" -> "iTunes"
    else -> source
}
