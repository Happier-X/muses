package com.muses.player.core.data.log

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * CrashHandler 持久化-读回单测（Robolectric，使用框架提供的临时 filesDir）。
 * 覆盖：崩溃现场落盘 + 必须委托原 handler / 重启后读回插入缓冲头部且文件删除 /
 * install 不抛异常（幂等）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashHandlerTest {

    private lateinit var context: Context

    /** 记录委托调用的假前置 handler */
    private var delegated: Pair<Thread, Throwable>? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        delegated = null
        // 预置假前置 handler：一是验证委托链，二是避免 previous==null 走 exitProcess 杀掉测试进程
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            delegated = thread to throwable
        }
    }

    @Test
    fun `崩溃时写入 crash-latest 并必须委托原 handler`() {
        val store = RingBufferErrorLogStore()
        store.log(ErrorLogStore.Level.WARN, "WebDavScan", "崩溃前的日志")
        CrashHandler.install(context, store)

        val thread = Thread.currentThread()
        val boom = IllegalStateException("未捕获异常测试")
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(thread, boom)

        // 委托原 handler（保持系统崩溃流程）
        assertEquals(thread, delegated!!.first)
        assertEquals(boom, delegated!!.second)
        // 崩溃文件已落盘
        assertTrue(CrashHandler.crashFile(context).exists())
    }

    @Test
    fun `重启读回 上次崩溃进入新缓冲头部并删除文件`() = runTest {
        // ── 上次会话：埋点 + 安装 + 触发崩溃 ──
        val oldStore = RingBufferErrorLogStore()
        oldStore.log(ErrorLogStore.Level.WARN, "WebDavScan", "上次会话的警告")
        CrashHandler.install(context, oldStore)
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), RuntimeException("上次崩溃"))

        // ── 本次会话：新 store 安装即读回 ──
        val newStore = RingBufferErrorLogStore()
        CrashHandler.install(context, newStore)

        val dump = newStore.dump()!!
        assertTrue(dump.contains("== 上次会话崩溃 =="))
        assertTrue(dump.contains("[Crash] 未捕获异常：上次崩溃"))
        assertTrue(dump.contains("RuntimeException: 上次崩溃"))
        assertTrue(dump.contains("[WebDavScan] 上次会话的警告"))
        // 读回后文件删除，避免重复报告
        assertFalse(CrashHandler.crashFile(context).exists())
    }

    @Test
    fun `无上次崩溃文件时安装不产生崩溃段`() = runTest {
        val store = RingBufferErrorLogStore()
        CrashHandler.install(context, store)
        assertEquals(null, store.dump())
    }

    @Test
    fun `install 幂等不抛异常`() {
        val store = RingBufferErrorLogStore()
        CrashHandler.install(context, store)
        CrashHandler.install(context, store) // 二次安装不得抛出
    }
}
