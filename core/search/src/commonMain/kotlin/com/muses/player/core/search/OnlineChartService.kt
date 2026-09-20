package com.muses.player.core.search

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * 榜单聚合服务：多平台榜单列表并行拉取、按需取榜单歌曲。
 *
 * 与 [OnlineSearchService] 同一取舍：逆向接口随时可能单点失效，
 * **单平台失败不阻断其它平台**（逐个携带成功/失败交给 UI 展示）。
 */
class OnlineChartService(
    private val providers: List<OnlineChartProvider>,
    /** 并行拉取的最大并发（默认 3：兼顾速度与风控） */
    private val concurrency: Int = 3,
) {

    /** 平台 key → 展示名（UI 平台筛选/标签用） */
    val platformNames: Map<String, String> = providers.associate { it.platform to it.displayName }

    /** 全部可用平台 key（保持 provider 注册顺序） */
    val platforms: List<String> = providers.map { it.platform }

    /**
     * 多平台榜单列表。
     * @param platforms 目标平台；为空则全部
     */
    suspend fun charts(platforms: List<String> = emptyList()): List<PlatformChartsOutcome> {
        val targets = providers.filter { p -> platforms.isEmpty() || p.platform in platforms }
        if (targets.isEmpty()) return emptyList()

        val semaphore = Semaphore(concurrency.coerceAtLeast(1))
        return coroutineScope {
            targets.map { provider ->
                async {
                    semaphore.withPermit {
                        runCatching { provider.charts() }.fold(
                            onSuccess = {
                                PlatformChartsOutcome.Success(
                                    platform = provider.platform,
                                    displayName = provider.displayName,
                                    charts = it,
                                )
                            },
                            onFailure = { e ->
                                PlatformChartsOutcome.Failure(
                                    platform = provider.platform,
                                    displayName = provider.displayName,
                                    message = e.message ?: "榜单加载失败",
                                    cause = e,
                                )
                            },
                        )
                    }
                }
            }.map { it.await() }
        }
    }

    /** 单平台榜单歌曲；平台不存在时抛 [OnlineChartException] */
    suspend fun chartSongs(
        platform: String,
        chartId: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): OnlineChartPage {
        val provider = providers.firstOrNull { it.platform == platform }
            ?: throw OnlineChartException(platform, "不支持的榜单平台：$platform")
        return provider.chartSongs(chartId, page, pageSize)
    }
}

/** 单平台榜单列表结局 */
sealed interface PlatformChartsOutcome {
    val platform: String

    data class Success(
        override val platform: String,
        val displayName: String,
        val charts: List<OnlineChart>,
    ) : PlatformChartsOutcome

    data class Failure(
        override val platform: String,
        val displayName: String,
        val message: String,
        val cause: Throwable? = null,
    ) : PlatformChartsOutcome
}
