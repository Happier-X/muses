package com.muses.player.core.lyrics.di

import com.muses.player.core.lyrics.LyricsMatcher
import com.muses.player.core.lyrics.amll.AmllIndexRepository
import com.muses.player.core.lyrics.amll.AmllTtmlDbClient
import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.lyrics.provider.PlatformLyricsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 歌词在线搜索装配（任务 08-25-native-lyrics-online L3）。
 * 仅装配数据层单例；UI/播放链路接线归后续任务。
 */
@Module
@InstallIn(SingletonComponent::class)
internal object LyricsModule {

    @Provides
    @Singleton
    fun provideLyricsHttp(): LyricsHttp = LyricsHttp()

    @Provides
    @Singleton
    fun provideAmllIndexRepository(http: LyricsHttp): AmllIndexRepository =
        AmllIndexRepository(loadFromNetwork = { http.getText(AMLL_INDEX_URL, timeoutSec = AMLL_INDEX_TIMEOUT_SEC) })

    @Provides
    @Singleton
    fun provideAmllTtmlDbClient(
        http: LyricsHttp,
        indexRepository: AmllIndexRepository,
    ): AmllTtmlDbClient = AmllTtmlDbClient(http, indexRepository)

    /** 默认回退链：平台五源 → LRCLIB（match.ts defaultFallbackProviders） */
    @Provides
    @Singleton
    fun provideLyricsMatcher(
        amllClient: AmllTtmlDbClient,
        http: LyricsHttp,
        lrclibProvider: com.muses.player.core.lyrics.lrclib.LrclibProvider,
    ): LyricsMatcher {
        val fallbacks = buildList {
            addAll(PlatformLyricsProvider.defaultChain(http))
            add(lrclibProvider)
        }
        return LyricsMatcher(amllClient, fallbacks)
    }

    /** M3：全局绑定供 ScrapeModule.provideEditCloudMetaSearch 消费（此前仅手动构造无绑定） */
    @Provides
    @Singleton
    fun provideLrclibProvider(http: LyricsHttp): com.muses.player.core.lyrics.lrclib.LrclibProvider =
        com.muses.player.core.lyrics.lrclib.LrclibProvider(http)

    private const val AMLL_INDEX_URL = com.muses.player.core.lyrics.amll.AMLL_INDEX_URL
    private const val AMLL_INDEX_TIMEOUT_SEC = com.muses.player.core.lyrics.amll.AMLL_INDEX_TIMEOUT_SEC
}
