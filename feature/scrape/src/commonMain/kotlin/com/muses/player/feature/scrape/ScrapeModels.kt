package com.muses.player.feature.scrape

import com.muses.player.core.model.scrape.WritebackResult

/** 页面四态（对照 ScrapePage.vue pageState: queue/matching/preview/result） */
sealed interface ScrapePageState {
    /** 待刮削队列 */
    data object Queue : ScrapePageState

    /** 匹配中：currentItem 为正在匹配的歌名 */
    data class Matching(val current: Int, val total: Int, val currentItem: String) : ScrapePageState

    /** 候选预览确认（checkedIds 默认空 = 全不选，写回安全红线） */
    data class Preview(
        val items: List<PreviewCandidate>,
        /**
         * 未命中分组（S2）：双链均为 NO_MATCH 的 songId（非限流）。
         * 与 [_throttledIds]（NETWORK/限流）分开，预览页分组列出、可单独重试或去审核改词重搜。
         */
        val noMatchIds: List<String> = emptyList(),
    ) : ScrapePageState

    /** 写回中：点“写回选中”后、文件/DB 落盘期间的过渡态，避免无反馈 */
    data class Writing(val count: Int) : ScrapePageState

    /** 写回结果 + 可撤销 journalId */
    data class Result(val results: List<WritebackResult>, val journalId: String) : ScrapePageState
}

/** 预览行：歌曲 + 匹配到的变更 + 封面候选 + 勾选态（09-03 可编辑：保留原值供对比，edit* 为用户覆写副本） */
data class PreviewCandidate(
    val songId: String,
    val songTitle: String,
    val currentTitle: String = songTitle,
    val currentArtist: String?,
    val currentAlbum: String? = null,
    val currentLyrics: String? = null,
    val matchedTitle: String?,
    val matchedArtist: String?,
    val matchedAlbum: String?,
    val matchedLyrics: String? = null,
    /** 匹配置信度展示（HIGH/MEDIUM/LOW），null = 文本链未命中 */
    val confidence: String?,
    val coverUrl: String?,
    val checked: Boolean = false,
    val checkedFields: Set<String> = emptySet(),
    val editTitle: String? = null,
    val editArtist: String? = null,
    val editAlbum: String? = null,
    val editLyrics: String? = null,
) {
    fun resolvedTitle(): String? = editTitle ?: matchedTitle
    fun resolvedArtist(): String? = editArtist ?: matchedArtist
    fun resolvedAlbum(): String? = editAlbum ?: matchedAlbum
    fun resolvedLyrics(): String? = editLyrics ?: matchedLyrics
    fun hasLyricsChange(): Boolean = !resolvedLyrics().isNullOrBlank()
}
