package com.muses.player.core.search

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * 在线搜索聚合服务：管理多平台 provider，支持单平台与多平台并行搜索。
 *
 * 设计取舍：
 * - 多平台并行受 [concurrency] 限流（各平台接口对突发请求敏感，且并发过高易触发风控）；
 * - 单平台失败**不影响**其它平台（返回 [PlatformSearchOutcome] 逐个携带成功/失败），
 *   因为逆向接口随时可能单点失效，全量失败比部分可用体验差得多。
 */
class OnlineSearchService(
    private val providers: List<OnlineSearchProvider>,
    /** 并行搜索的最大并发（默认 3：兼顾速度与风控） */
    private val concurrency: Int = 3,
) {

    /** 平台 key → 展示名（供 UI 渲染筛选/标签） */
    val platformNames: Map<String, String> =
        providers.associate { it.platform to it.displayName }

    /** 全部可用平台 key（保持 provider 注册顺序） */
    val platforms: List<String> = providers.map { it.platform }

    /** 单平台搜索；平台不存在时抛 [OnlineSearchException] */
    suspend fun search(
        platform: String,
        keyword: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): OnlineSearchPage {
        val provider = providers.firstOrNull { it.platform == platform }
            ?: throw OnlineSearchException(platform, "不支持的平台：$platform")
        return provider.search(keyword, page, pageSize)
    }

    /**
     * 多平台并行搜索。
     *
     * @param platforms 目标平台；为空则搜全部
     * @return 每个平台一个结果（成功或失败），顺序与 [platforms] 一致
     */
    suspend fun searchAll(
        keyword: String,
        platforms: List<String> = emptyList(),
        page: Int = 1,
        pageSize: Int = 30,
    ): List<PlatformSearchOutcome> {
        val targets = providers.filter { p ->
            platforms.isEmpty() || p.platform in platforms
        }
        if (targets.isEmpty()) return emptyList()

        val semaphore = Semaphore(concurrency.coerceAtLeast(1))
        return coroutineScope {
            targets.map { provider ->
                async {
                    semaphore.withPermit {
                        runCatching { provider.search(keyword, page, pageSize) }
                            .fold(
                                onSuccess = { PlatformSearchOutcome.Success(it) },
                                onFailure = { e ->
                                    PlatformSearchOutcome.Failure(
                                        platform = provider.platform,
                                        displayName = provider.displayName,
                                        message = e.message ?: "搜索失败",
                                        cause = e,
                                    )
                                },
                            )
                    }
                }
            }.map { it.await() }
        }
    }
}

/** 单平台搜索结局 */
sealed interface PlatformSearchOutcome {
    /** 平台 key */
    val platform: String

    data class Success(val page: OnlineSearchPage) : PlatformSearchOutcome {
        override val platform: String get() = page.platform
    }

    data class Failure(
        override val platform: String,
        val displayName: String,
        val message: String,
        val cause: Throwable? = null,
    ) : PlatformSearchOutcome
}