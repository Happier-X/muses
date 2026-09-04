package com.muses.player.feature.scrape

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import kotlinx.coroutines.launch

/**
 * 跨页面刮削队列入口：供歌曲页 ⋮ 菜单/多选条入队（不拉起刮削页也能标记）。
 * 独立于 [ScrapeViewModel]——后者持有四态机状态，跨页面复用会引入不必要的状态。
 */
class ScrapeQueueAccessViewModel constructor(
    private val queueStore: ScrapeQueueStore,
) : ViewModel() {

    /** 批量入队（幂等，已在队列中的 songId 自动去重） */
    fun enqueue(songIds: List<String>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch { queueStore.enqueue(songIds) }
    }

    /** 带回执的入队（供 Snackbar 区分“已加入/已在队列”） */
    suspend fun enqueueWithResult(songIds: List<String>): com.muses.player.core.scrape.queue.ScrapeQueueStore.EnqueueResult {
        if (songIds.isEmpty()) return com.muses.player.core.scrape.queue.ScrapeQueueStore.EnqueueResult(added = 0)
        return queueStore.enqueue(songIds)
    }
}
