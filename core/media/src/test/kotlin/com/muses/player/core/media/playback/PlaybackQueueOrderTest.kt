@file:Suppress("UnsafeOptInUsageError")

package com.muses.player.core.media.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQueueOrderTest {
    @Test
    fun `开启随机时队列遵循播放器洗牌顺序且每项仅出现一次`() {
        val timeline = QueueTimeline(listOf(2, 0, 3, 1))
        assertEquals(listOf(2, 0, 3, 1), timeline.playbackQueueIndices(true))
        assertEquals(listOf("丙", "甲", "丁", "乙"),
            timeline.playbackQueueIndices(true).map { listOf("甲", "乙", "丙", "丁")[it] })
    }

    @Test
    fun `关闭随机后恢复原始顺序`() {
        val timeline = QueueTimeline(listOf(2, 0, 3, 1))
        assertEquals(listOf(0, 1, 2, 3), timeline.playbackQueueIndices(false))
    }

    @Test
    fun `展示索引映射到洗牌前的媒体索引`() {
        val indices = QueueTimeline(listOf(2, 0, 3, 1)).playbackQueueIndices(true)
        assertEquals(3, indices[2])
        assertEquals(0, indices.indexOf(2))
    }

    @Test
    fun `空队列和单曲队列不循环遍历`() {
        assertEquals(emptyList<Int>(), QueueTimeline(emptyList()).playbackQueueIndices(true))
        assertEquals(listOf(0), QueueTimeline(listOf(0)).playbackQueueIndices(true))
    }

    private class QueueTimeline(private val shuffled: List<Int>) : Timeline() {
        override fun getWindowCount() = shuffled.size
        override fun getPeriodCount() = shuffled.size
        override fun getFirstWindowIndex(shuffleModeEnabled: Boolean): Int =
            if (shuffled.isEmpty()) C.INDEX_UNSET else if (shuffleModeEnabled) shuffled.first() else 0

        override fun getNextWindowIndex(windowIndex: Int, repeatMode: Int, shuffleModeEnabled: Boolean): Int {
            assertEquals(Player.REPEAT_MODE_OFF, repeatMode)
            val order = if (shuffleModeEnabled) shuffled else shuffled.indices.toList()
            return order.getOrNull(order.indexOf(windowIndex) + 1) ?: C.INDEX_UNSET
        }

        override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long): Window =
            error("此测试不读取窗口内容")
        override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period =
            error("此测试不读取分段内容")
        override fun getIndexOfPeriod(uid: Any): Int = C.INDEX_UNSET
        override fun getUidOfPeriod(periodIndex: Int): Any = periodIndex
    }
}
