package com.muses.player.core.media.playback

import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 播放失败自动恢复控制器（规格书 = src/features/player/controller.ts 的
 * advanceToNextRecoveryCandidate / PlaybackRecoveryContext，语义逐条对齐）：
 *
 * - 播放失败时沿当前生效顺序向后查找「未尝试过」的下一首候选；
 * - 最多回绕一次（offset ≤ items.length），保证损坏队列不会无限推进；
 * - attempted 集合在用户主动切歌/队列变更时重置。
 */
class PlaybackRecoveryController constructor() {

    /** 安全错误文案；用户主动操作/成功恢复后置 null */
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    fun clearError() {
        _playbackError.value = null
    }

    fun setError(copy: String) {
        _playbackError.value = copy
    }

    /** 本次恢复链已尝试的歌曲（controller.ts attemptedSongIds） */
    private val attemptedSongIds = LinkedHashSet<String>()

    fun markAttempted(songId: String) {
        attemptedSongIds.add(songId)
    }

    fun isAttempted(songId: String): Boolean = songId in attemptedSongIds

    /** 用户主动切歌/换队列：清空恢复链状态 */
    fun reset() {
        attemptedSongIds.clear()
    }

    /**
     * 选择播放失败恢复链的下一首候选（queue.ts advanceToNextRecoveryCandidate 对齐）：
     *
     * - 始终沿 active order 向后查找并最多回绕一次；
     * - 临时忽略单曲循环，且跳过本次恢复链已尝试过的歌曲；
     * - 返回候选的下标；无可播候选返回 null。
     *
     * @param activeOrder 当前生效顺序的 songId 序列（shuffle 启用时为洗牌序）
     * @param errorIndex  播放失败的条目下标
     */
    fun selectNextCandidate(activeOrder: List<String>, errorIndex: Int): Int? {
        if (activeOrder.isEmpty()) return null

        val startIndex = if (errorIndex in activeOrder.indices) errorIndex else -1
        for (offset in 1..activeOrder.size) {
            val candidateIndex = (startIndex + offset).mod(activeOrder.size)
            val candidateId = activeOrder[candidateIndex]
            if (candidateId !in attemptedSongIds) {
                return candidateIndex
            }
        }
        return null
    }

    /** 候选被采纳后登记尝试 */
    fun recordAttempt(candidateSongId: String) {
        attemptedSongIds.add(candidateSongId)
    }
}
