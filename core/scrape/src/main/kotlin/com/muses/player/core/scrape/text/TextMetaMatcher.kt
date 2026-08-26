package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.OnlineTextMatchFailReason
import com.muses.player.core.model.scrape.OnlineTextMatchResult
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.provider.KgProvider
import com.muses.player.core.scrape.text.provider.KwProvider
import com.muses.player.core.scrape.text.provider.MgProvider
import com.muses.player.core.scrape.text.provider.TxProvider
import com.muses.player.core.scrape.text.provider.WyProvider
import kotlinx.coroutines.CancellationException

/**
 * 五源文本元数据匹配编排（规格书 = src/features/metadata/match.ts matchOnlineTextMeta）：
 * 1. title 空白 → no-match；
 * 2. needsOnlineTextMeta=false → not-needed 早退；
 * 3. 负缓存命中（queryKey 一致且未过期）→ no-match 短路；
 * 4. 按链序逐源搜索，命中且 hitFillsMissing 即返回（confidence 由 classifyTextMetaConfidence 判定）；
 * 5. 全链无命中：写负缓存，返回 network（链中出现过异常）或 no-match。
 */
class TextMetaMatcher(
    private val providers: List<TextMetaProvider>,
    internal val negativeCache: NegativeCache = NegativeCache(),
) {

    suspend fun match(query: OnlineTextQuery): OnlineTextMatchResult {
        val title = query.title.trim()
        if (title.isEmpty()) {
            return OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NO_MATCH)
        }

        if (!needsOnlineTextMeta(query.toNeedQuery())) {
            return OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NOT_NEEDED)
        }

        val queryKey = NegativeCache.buildQueryKey(query)
        val cached = negativeCache.get(query.songId)
        if (cached != null && cached.queryKey == queryKey && cached.expiresAt > System.currentTimeMillis()) {
            return OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NO_MATCH)
        }

        var sawNetworkError = false

        for (provider in providers) {
            try {
                val hit = provider.search(query)
                val need = query.toNeedQuery()
                if (hit != null && hitFillsMissing(hit, need)) {
                    val finalHit = hit.copy(source = provider.id)
                    return OnlineTextMatchResult.Ok(
                        hit = finalHit,
                        confidence = classifyTextMetaConfidence(finalHit, query),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Web catch 无类型：任何 provider 异常均记为网络错误并继续下一源
                sawNetworkError = true
            }
        }

        negativeCache.put(
            query.songId,
            NegativeCache.NegativeEntry(
                queryKey = queryKey,
                expiresAt = System.currentTimeMillis() + NEGATIVE_CACHE_TTL_MS,
            ),
        )

        return OnlineTextMatchResult.Fail(
            if (sawNetworkError) OnlineTextMatchFailReason.NETWORK else OnlineTextMatchFailReason.NO_MATCH,
        )
    }

    companion object {
        /** match.ts NEGATIVE_CACHE_TTL_MS */
        const val NEGATIVE_CACHE_TTL_MS: Long = 45 * 60 * 1000L

        /**
         * 默认链：kw → tx → wy → kg → mg（对齐 any-listen 国内段，match.ts defaultProviders）
         */
        fun defaultProviders(http: ScrapeHttp): List<TextMetaProvider> = listOf(
            KwProvider(http),
            TxProvider(http),
            WyProvider(http),
            KgProvider(http),
            MgProvider(http),
        )

        /** 默认装配：默认链 + 默认负缓存 */
        fun withDefaultProviders(http: ScrapeHttp = ScrapeHttp()): TextMetaMatcher =
            TextMetaMatcher(defaultProviders(http))
    }
}
