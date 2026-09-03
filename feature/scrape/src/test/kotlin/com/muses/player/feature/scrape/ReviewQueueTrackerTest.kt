package com.muses.player.feature.scrape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * S3 待审队列状态机单测（纯 JVM，无 Android 依赖）：
 * start 置队列返回首项 / advance 剔除已写回并推进 / cancel 清队列不强推 / remove 仅清理。
 */
class ReviewQueueTrackerTest {

    @Test
    fun `start 置队列并返回第一首`() {
        val tracker = ReviewQueueTracker()
        val first = tracker.start(listOf("a", "b", "c"))
        assertEquals("a", first)
        assertEquals(listOf("a", "b", "c"), tracker.queue.value)
    }

    @Test
    fun `start 空队列返回 null`() {
        val tracker = ReviewQueueTracker()
        assertNull(tracker.start(emptyList()))
        assertEquals(emptyList<String>(), tracker.queue.value)
    }

    @Test
    fun `advance 剔除已写回并返回下一首`() {
        val tracker = ReviewQueueTracker()
        tracker.start(listOf("a", "b", "c"))
        assertEquals("b", tracker.advance("a"))
        assertEquals(listOf("b", "c"), tracker.queue.value)
        assertEquals("c", tracker.advance("b"))
        assertEquals(listOf("c"), tracker.queue.value)
    }

    @Test
    fun `advance 到队尾返回 null`() {
        val tracker = ReviewQueueTracker()
        tracker.start(listOf("a"))
        assertNull(tracker.advance("a"))
        assertEquals(emptyList<String>(), tracker.queue.value)
    }

    @Test
    fun `advance 未在队列的 id 不改变队列但返回队首`() {
        val tracker = ReviewQueueTracker()
        tracker.start(listOf("a", "b"))
        // 写回了队列外的歌（单曲模式）：队列不变，返回队首供宿主判断
        assertEquals("a", tracker.advance("x"))
        assertEquals(listOf("a", "b"), tracker.queue.value)
    }

    @Test
    fun `cancel 清队列`() {
        val tracker = ReviewQueueTracker()
        tracker.start(listOf("a", "b"))
        tracker.cancel()
        assertEquals(emptyList<String>(), tracker.queue.value)
    }

    @Test
    fun `remove 仅清理不推进语义`() {
        val tracker = ReviewQueueTracker()
        tracker.start(listOf("a", "b", "c"))
        tracker.remove("b")
        assertEquals(listOf("a", "c"), tracker.queue.value)
    }
}
