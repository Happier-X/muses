package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit

// 规格书 = src/features/metadata/types.ts 的 TextMetaProvider

/** 单源文本元数据 provider：返回可补缺的命中，无结果返回 null；网络/解析错误抛出由 matcher 归类 */
interface TextMetaProvider {
    val id: OnlineTextSource

    suspend fun search(query: OnlineTextQuery): TextMetaHit?
}
