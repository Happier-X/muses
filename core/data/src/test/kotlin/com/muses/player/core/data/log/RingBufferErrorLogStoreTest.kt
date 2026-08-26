package com.muses.player.core.data.log

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 环形缓冲错误日志单测（纯 JVM）：容量淘汰 / dump 格式 / latestSummary /
 * serialize-restore 崩溃读回往返。
 */
class RingBufferErrorLogStoreTest {

    private fun newStore() = RingBufferErrorLogStore()

    @Test
    fun `空缓冲时 dump 为 null 且 latestSummary 为 null`() = runTest {
        val store = newStore()
        assertNull(store.dump())
        assertNull(store.latestSummary.value)
    }

    @Test
    fun `log 后 latestSummary 更新为 时间 加 首行消息`() {
        val store = newStore()
        store.log(ErrorLogStore.Level.WARN, "WebDavScan", "列表请求失败")
        assertTrue(store.latestSummary.value!!.endsWith("列表请求失败"))
        // 摘要含 MM-dd HH:mm 时间前缀（形如 "12-31 23:59 ..."）
        assertTrue(Regex("""\d{2}-\d{2} \d{2}:\d{2}""").containsMatchIn(store.latestSummary.value!!))
    }

    @Test
    fun `dump 包含级别 时间戳 标签 与 异常堆栈续行缩进`() = runTest {
        val store = newStore()
        store.log(ErrorLogStore.Level.ERROR, "Playback", "播放失败", java.io.IOException("网络断开"))

        val text = store.dump()!!
        assertTrue(text.startsWith("--- ERROR "))
        assertTrue(text.contains("[Playback] 播放失败"))
        // throwable 堆栈附在后续行且缩进 4 空格
        assertTrue(text.contains("\n    java.io.IOException: 网络断开"))
        assertFalse(text.contains("上次会话崩溃"))
    }

    @Test
    fun `超过容量上限时最早的日志被丢弃`() = runTest {
        val store = newStore()
        repeat(RingBufferErrorLogStore.CAPACITY + 100) { index ->
            store.log(ErrorLogStore.Level.WARN, "T", "msg-$index")
        }
        val text = store.dump()!!
        val entryCount = text.split("\n--- ").size
        assertEquals(RingBufferErrorLogStore.CAPACITY, entryCount)
        // 最早 100 条被淘汰
        assertFalse(text.contains("msg-0\n"))
        assertTrue(text.contains("msg-${RingBufferErrorLogStore.CAPACITY + 99}"))
    }

    @Test
    fun `serialize 后 restore 到新实例 归入上次会话崩溃段并保留原时间序`() = runTest {
        val source = newStore()
        source.log(ErrorLogStore.Level.WARN, "WebDavScan", "第一条")
        source.log(ErrorLogStore.Level.ERROR, "Playback", "第二条", IllegalStateException("boom"))
        val serialized = source.serialize()

        val target = newStore()
        target.restore(serialized)

        val text = target.dump()!!
        // 上次会话内容归入独立段落
        assertTrue(text.contains("== 上次会话崩溃 =="))
        assertTrue(text.indexOf("== 上次会话崩溃 ==").let { pos -> text.indexOf("第一条", pos) > 0 })
        assertTrue(text.contains("[Playback] 第二条"))
        assertTrue(text.contains("\n    java.lang.IllegalStateException: boom"))
        // 缓冲原本为空：恢复后摘要指向最后一条恢复条目
        assertTrue(target.latestSummary.value!!.endsWith("第二条"))
    }

    @Test
    fun `restore 空内容与非法行静默忽略`() = runTest {
        val store = newStore()
        store.restore("")
        store.restore("not-a-log-line\nL\tbroken")
        assertNull(store.dump())
    }

    @Test
    fun `本会话新日志不落入上次会话崩溃段`() = runTest {
        val source = newStore()
        source.log(ErrorLogStore.Level.ERROR, "Crash", "旧崩溃")
        val target = newStore()
        target.restore(source.serialize())
        target.log(ErrorLogStore.Level.ERROR, "Playback", "本次新错误")

        val text = target.dump()!!
        val crashSection = text.indexOf("== 上次会话崩溃 ==")
        val newEntryPos = text.indexOf("本次新错误")
        assertTrue(crashSection > 0)
        // 新日志在崩溃段之前（正文区），崩溃段只含旧内容
        assertTrue(newEntryPos < crashSection)
        assertFalse(text.substring(crashSection).contains("本次新错误"))
    }
}
