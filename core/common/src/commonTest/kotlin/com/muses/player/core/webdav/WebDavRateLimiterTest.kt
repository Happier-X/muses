package com.muses.player.core.webdav

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 限流器节流单测（任务 08-27-scrape-throttle-429）。
 *
 * 原为 core:scrape 的 ScrapeRateLimiterTest（经兼容别名测共享实现）；
 * W2 上收时别名删除，本测试归位到被测类所在包，语义不变。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebDavRateLimiterTest {

    @Test
    fun `默认4rps约250ms间隔_连续acquire产生延迟`() = runTest {
        val limiter = WebDavRateLimiter(intervalMs = 100L, nowMs = { currentTime })
        limiter.acquire() // 首次立即：虚拟时间仍为 0
        assertEquals(0L, currentTime)

        limiter.acquire() // 二次应延迟约 100ms（虚拟时间推进）
        assertEquals(100L, currentTime)
        limiter.acquire() // 三次再 100ms
        assertEquals(200L, currentTime)
    }

    @Test
    fun `Unlimited不限流_连续acquire无延迟`() = runTest {
        val limiter = WebDavRateLimiter(intervalMs = 0L, nowMs = { currentTime })
        repeat(10) { limiter.acquire() }
        assertEquals(0L, currentTime)
        // 同时验证预置 Unlimited 单例（真实时钟，间隔 0）也不延迟
        repeat(10) { WebDavRateLimiter.Unlimited.acquire() }
        assertEquals(0L, currentTime)
    }

    @Test
    fun `自定义间隔_限流生效`() = runTest {
        val limiter = WebDavRateLimiter(intervalMs = 250L, nowMs = { currentTime })
        limiter.acquire()
        assertEquals(0L, currentTime)
        limiter.acquire()
        assertEquals(250L, currentTime)
        limiter.acquire()
        assertEquals(500L, currentTime)
    }
}
