package com.muses.player.feature.scrape

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 批量逐首审核的待审队列（S3，纯状态机，无 Android/IO 依赖，可纯 JVM 单测）。
 *
 * 语义（design §3.2）：
 * - 仅在「应用并下一首」路径推进（[advance]）；用户手动返回由宿主调 [cancel]，不强推下一首。
 * - 外部写回（审核页自己的单曲 applyScrapeChanges）由宿主调 [remove] 同步，避免预览残留已处理歌曲。
 */
class ReviewQueueTracker {

    private val _queue = MutableStateFlow<List<String>>(emptyList())

    /** 待审队列（当前剩余） */
    val queue: StateFlow<List<String>> = _queue.asStateFlow()

    /**
     * 开始逐首审核：置待审队列。
     * @return 队列第一首 songId；队列为空返回 null
     */
    fun start(items: List<String>): String? {
        _queue.value = items.toList()
        return items.firstOrNull()
    }

    /**
     * 写回成功后推进：剔除已写回者。
     * @return 队列中下一首 songId；无则 null（宿主停止连续推进）
     */
    fun advance(writtenSongId: String): String? {
        val rest = _queue.value.filter { it != writtenSongId }
        _queue.value = rest
        return rest.firstOrNull()
    }

    /** 用户手动返回（非应用路径）：清队列，不强推下一首 */
    fun cancel() {
        _queue.value = emptyList()
    }

    /** 外部写回同步：剔除指定歌曲（不推进指针语义，仅清理） */
    fun remove(songId: String) {
        _queue.value = _queue.value.filter { it != songId }
    }
}
