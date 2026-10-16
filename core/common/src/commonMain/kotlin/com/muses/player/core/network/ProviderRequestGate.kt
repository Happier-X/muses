package com.muses.player.core.network

import com.muses.player.core.data.store.platformNowMs
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 文本、歌词和封面共用的来源队列；许可持续到响应内容读取完成。 */
class ProviderRequestGate(
    private val intervalMs: Long = 250L,
    private val nowMs: () -> Long = { platformNowMs() },
) {
    private val lock = Any()
    private val sources = mutableMapOf<String, Permit>()

    class Permit internal constructor(private val intervalMs: Long, private val nowMs: () -> Long) {
        internal val mutex = Mutex()
        private var lastStart: Long? = null

        /** 每次真实请求（包括重试）均经过此处；取消等待不会预占未来许可。 */
        suspend fun beforeRequest() {
            val wait = lastStart?.let { (intervalMs - (nowMs() - it)).coerceIn(0L, intervalMs) } ?: 0L
            if (wait > 0L) delay(wait)
            lastStart = nowMs()
        }
    }

    suspend fun <T> withSource(url: String, block: suspend Permit.() -> T): T {
        val key = providerRequestKey(url)
        val permit = synchronized(lock) { sources.getOrPut(key) { Permit(if (key == "musicbrainz") maxOf(1000L, intervalMs) else intervalMs, nowMs) } }
        return observeProviderRequest(url) { permit.mutex.withLock { permit.block() } }
    }

    companion object {
        /** 两套 HTTP 工具默认共用，确保跨维度与跨实例的同源请求不会叠加。 */
        val Shared = ProviderRequestGate()
    }
}

internal fun providerRequestKey(url: String): String {
    val host = Url(url).host.lowercase()
    fun belongs(domain: String) = host == domain || host.endsWith(".$domain")
    return when {
        belongs("gdstudio.xyz") -> "gdstudio"
        belongs("163.com") || belongs("126.net") -> "wy"
        belongs("qq.com") || belongs("gtimg.cn") -> "tx"
        belongs("kuwo.cn") -> "kw"
        belongs("kugou.com") || belongs("kugou.net") -> "kg"
        belongs("migu.cn") -> "mg"
        belongs("apple.com") || belongs("mzstatic.com") -> "itunes"
        belongs("lrclib.net") -> "lrclib"
        belongs("musicbrainz.org") -> "musicbrainz"
        else -> host
    }
}
