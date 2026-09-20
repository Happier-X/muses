package com.muses.player.core.search.di

import com.muses.player.core.search.OnlineSearchService
import com.muses.player.core.search.http.SearchHttp
import com.muses.player.core.search.provider.KgSearchProvider
import com.muses.player.core.search.provider.KwSearchProvider
import com.muses.player.core.search.provider.MgSearchProvider
import com.muses.player.core.search.provider.TxSearchProvider
import com.muses.player.core.search.provider.WySearchProvider
import org.koin.dsl.module

/**
 * 在线搜索 Koin 装配：5 个平台 provider + 聚合服务。
 *
 * 说明：各平台接口均为公开的逆向接口（无需登录/签名），失败语义在 provider 内
 * 统一包装为 [com.muses.player.core.search.OnlineSearchException]，
 * 聚合层做「部分失败不阻断」处理（见 OnlineSearchService.searchAll）。
 */
fun searchModule() = module {
    single { SearchHttp() }
    single { KwSearchProvider(get()) }
    single { TxSearchProvider(get()) }
    single { WySearchProvider(get()) }
    single { KgSearchProvider(get()) }
    single { MgSearchProvider(get()) }
    single {
        OnlineSearchService(
            providers = listOf(get<KwSearchProvider>(), get<TxSearchProvider>(), get<WySearchProvider>(), get<KgSearchProvider>(), get<MgSearchProvider>()),
        )
    }
}
