package com.muses.player.core.ai.di

import com.muses.player.core.ai.AiChatClient
import com.muses.player.core.ai.AiRecommendService
import com.muses.player.core.ai.AiSuggestionMatcher
import com.muses.player.core.ai.LibraryProfileBuilder
import org.koin.dsl.module

/**
 * AI 推荐 Koin 装配（双端共用一份，同 :core:search 的 jvmShared 形态）。
 *
 * 注意：**这里不持有 API Key**。Key 存于加密凭据库（CredentialsRepository），
 * 由 UI/ViewModel 在发起推荐时取出、组装成 [com.muses.player.core.ai.AiRecommendConfig] 传入，
 * 避免明文 Key 常驻内存单例或被日志带出。
 */
fun aiModule() = module {
    single { AiChatClient() }
    // SongDao 在 :core:data 的 databaseModule 里是 factory：这里取一次由 builder 长持
    single { LibraryProfileBuilder(get()) }
    // 匹配器复用搜索聚合服务（含并发限流）
    single { AiSuggestionMatcher(get()) }
    single { AiRecommendService(get(), get()) }
}
