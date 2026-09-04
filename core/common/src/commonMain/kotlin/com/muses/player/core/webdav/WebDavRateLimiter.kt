package com.muses.player.core.webdav

import com.muses.player.core.data.store.platformNowMs
import kotlinx.coroutines.delay

/**
 * WebDAV/刮削共享限流器（漏桶/令牌桶简化版，P2c 搬入 commonMain；语义冻结）。
 *
 * - 默认 4 rps（约 250ms 间隔），跨 WebDAV 播放/刮削链路单例共享，防叠加 burst 触发 Cloudflare 429；
 * - [acquire] 在请求入口调用，使用 [delay] 非阻塞协程；
 * - 线程安全：`synchronized(lock)` 保证并发下串行更新 [nextAvailableMs]，兼顾挂起/阻塞双路径共享同一桶；
 * - 测试可构造自定义间隔或使用 [Unlimited] 跳过限流。
 *
 * 前身为 core:scrape 的 ScrapeRateLimiter，08-27-webdav-playback-429 提升至 core:webdav 供双链路共享。
 *
 * P2c 变更点：默认时钟改 `platformNowMs`（commonMain 无 System.currentTimeMillis）；
 * Android 留守的阻塞路径（OkHttp 同步 Interceptor）改由调用方经 `reserveBlockingDelayMs` 自行 sleep，
 * 本类只保留挂起语义（commonMain 无 Thread.sleep）。
 */
class WebDavRateLimiter(
    /** 许可间隔毫秒，默认 250ms 即 4 rps。 */
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
    private val nowMs: () -> Long = { platformNowMs() },
) {

    private val lock = Any()
    private var nextAvailableMs: Long = 0L

    /**
     * 获取一次许可。
     *
     * 若距离上次许可不足 [intervalMs]，则挂起等待。
     * 首个许可立即放行（[nextAvailableMs] 初始 0）。
     */
    suspend fun acquire() {
        if (intervalMs <= 0L) return
        val delayMs = synchronized(lock) {
            val now = nowMs()
            if (now >= nextAvailableMs) {
                nextAvailableMs = now + intervalMs
                0L
            } else {
                val wait = nextAvailableMs - now
                nextAvailableMs += intervalMs
                wait
            }
        }
        if (delayMs > 0L) {
            delay(delayMs)
        }
    }

    /**
     * 计算本次阻塞等待毫秒并预占下一许可（供 Android 侧 OkHttp 同步 Interceptor 经 Thread.sleep 消费）。
     * 与 [acquire] 共享同一 [nextAvailableMs] 状态，互斥保证串行。
     */
    fun reserveBlockingDelayMs(): Long {
        if (intervalMs <= 0L) return 0L
        return synchronized(lock) {
            val now = nowMs()
            if (now >= nextAvailableMs) {
                nextAvailableMs = now + intervalMs
                0L
            } else {
                val nowWait = nextAvailableMs - now
                nextAvailableMs += intervalMs
                nowWait
            }
        }
    }

    companion object {
        /** 默认 4 rps 对应间隔。 */
        const val DEFAULT_INTERVAL_MS: Long = 250L

        /** 默认 4 rps 实例（与 Module 单例等效）。 */
        fun default(): WebDavRateLimiter = WebDavRateLimiter(DEFAULT_INTERVAL_MS)

        /** 测试用：不限流实例。 */
        val Unlimited: WebDavRateLimiter = WebDavRateLimiter(intervalMs = 0L)
    }
}
