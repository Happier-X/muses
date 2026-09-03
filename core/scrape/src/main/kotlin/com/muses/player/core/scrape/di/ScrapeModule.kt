package com.muses.player.core.scrape.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.lyrics.provider.PlatformLyricsProvider
import com.muses.player.core.scrape.cover.CoverMatcher
import com.muses.player.core.scrape.editmeta.AmllLyricsPort
import com.muses.player.core.scrape.editmeta.EditCloudMetaSearch
import com.muses.player.core.scrape.editmeta.ProviderLyricsPort
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.webdav.WebDavRateLimiter
import com.muses.player.core.scrape.queue.ScrapeHistoryStore
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.scrape.text.NegativeCache
import com.muses.player.core.scrape.text.TextMetaMatcher
import com.muses.player.core.scrape.text.provider.KgProvider
import com.muses.player.core.scrape.text.provider.KwProvider
import com.muses.player.core.scrape.text.provider.MgProvider
import com.muses.player.core.scrape.text.provider.TxProvider
import com.muses.player.core.scrape.text.provider.WyProvider
import com.muses.player.core.scrape.writeback.AudioTagFileWriter
import com.muses.player.core.scrape.writeback.CoverBytesFetcher
import com.muses.player.core.scrape.writeback.HttpCoverBytesFetcher
import com.muses.player.core.scrape.writeback.RollbackJournalStore
import com.muses.player.core.scrape.writeback.WebDavAudioTagFileWriter
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import com.muses.player.core.webdav.WebDavClient
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 刮削引擎装配（任务收尾批次）。
 *
 * 仅装配数据层单例，不接线任何 UI（UI 接线归后续任务）。
 * 已知缺口：M1 原生 Source 模型无 username 字段，WebDAV 写回认证用户名暂以空串传入，
 * 接线 UI 前需先补 username 持久化（见 WebDavAudioTagFileWriter 注释）。
 */
@Module
@InstallIn(SingletonComponent::class)
internal object ScrapeModule {

    // ── 网络与匹配链 ──────────────────────────────────────

    /** 共享限流器：复用 core:webdav 的 WebDavRateLimiter 单例（08-27-webdav-playback-429），刮削与播放共享 4 rps */
    @Provides
    @Singleton
    fun provideScrapeHttp(rateLimiter: WebDavRateLimiter): ScrapeHttp = ScrapeHttp(rateLimiter = rateLimiter)

    /** 默认文本五源链 kw→tx→wy→kg→mg（metadata/match.ts defaultProviders） */
    @Provides
    @Singleton
    fun provideTextMetaMatcher(http: ScrapeHttp): TextMetaMatcher = TextMetaMatcher(
        providers = listOf(
            KwProvider(http),
            TxProvider(http),
            WyProvider(http),
            KgProvider(http),
            MgProvider(http),
        ),
        negativeCache = NegativeCache(),
    )

    /** 默认封面六源链 iTunes→kw→tx→wy→kg→mg（cover/match.ts） */
    @Provides
    @Singleton
    fun provideCoverMatcher(http: ScrapeHttp): CoverMatcher = CoverMatcher.withDefaultProviders(http)

    // ── editMeta 歌词维度端口（L3 接线）───────────────────

    @Provides
    @Singleton
    fun provideEditCloudMetaSearch(
        http: ScrapeHttp,
        lyricsHttp: com.muses.player.core.lyrics.http.LyricsHttp,
        amllClient: com.muses.player.core.lyrics.amll.AmllTtmlDbClient,
        lrclibProvider: com.muses.player.core.lyrics.lrclib.LrclibProvider,
    ): EditCloudMetaSearch {
        // 文本五源链（metadata/match.ts defaultProviders）
        val textProviders = listOf(
            com.muses.player.core.scrape.text.provider.KwProvider(http),
            com.muses.player.core.scrape.text.provider.TxProvider(http),
            com.muses.player.core.scrape.text.provider.WyProvider(http),
            com.muses.player.core.scrape.text.provider.KgProvider(http),
            com.muses.player.core.scrape.text.provider.MgProvider(http),
        )
        // 封面六源链（cover/match.ts defaultProviders）
        val coverProviders = CoverMatcher.defaultProviders(http)
        // 歌词端口：AMLL 始终参与 + 平台五源 + LRCLIB（match.ts 组合顺序）
        val lyricsPorts = buildList {
            add(AmllLyricsPort(amllClient))
            PlatformLyricsProvider.defaultChain(lyricsHttp).forEach { p ->
                add(ProviderLyricsPort(p))
            }
            add(ProviderLyricsPort(lrclibProvider))
        }
        return EditCloudMetaSearch(
            textProviders = textProviders,
            coverProviders = coverProviders,
            lyricsPorts = lyricsPorts,
        )
    }

    // ── 存储 ──────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideRollbackJournalStore(dataStore: DataStore<Preferences>): RollbackJournalStore =
        RollbackJournalStore(dataStore)

    @Provides
    @Singleton
    fun provideScrapeQueueStore(
        dataStore: DataStore<Preferences>,
        songRepository: SongRepository,
    ): ScrapeQueueStore = ScrapeQueueStore(
        dataStore,
        existingSongIds = { songRepository.observeSongs().first().map { it.id }.toSet() },
    )

    @Provides
    @Singleton
    fun provideScrapeHistoryStore(dataStore: DataStore<Preferences>): ScrapeHistoryStore =
        ScrapeHistoryStore(dataStore)

    // ── 写回编排 ──────────────────────────────────────────

    @Provides
    @Singleton
    fun provideCoverBytesFetcher(http: ScrapeHttp): CoverBytesFetcher = HttpCoverBytesFetcher(http)

    @Provides
    @Singleton
    fun provideAudioTagFileWriter(
        @ApplicationContext context: Context,
        sourceRepository: com.muses.player.core.data.repository.SourceRepository,
        credentialsRepository: CredentialsRepository,
        webDavClient: Lazy<WebDavClient>,
    ): AudioTagFileWriter = WebDavAudioTagFileWriter(
        sourceRepository = sourceRepository,
        credentialsRepository = credentialsRepository,
        webDavClientFactory = { webDavClient.get() },
        tempDir = File(context.cacheDir, "scrape-writeback").apply { mkdirs() },
    )

    @Provides
    @Singleton
    fun provideWritebackOrchestrator(
        songRepository: SongRepository,
        journalStore: RollbackJournalStore,
        fileWriter: AudioTagFileWriter,
        coverBytesFetcher: CoverBytesFetcher,
        historyStore: Lazy<ScrapeHistoryStore>,
        audioTagReader: Lazy<com.muses.player.core.data.tag.AudioTagReader>,
    ): WritebackOrchestrator = WritebackOrchestrator(
        songRepository = songRepository,
        journalStore = journalStore,
        fileWriter = fileWriter,
        coverBytesFetcher = coverBytesFetcher,
        historySink = { entries -> historyStore.get().append(entries) },
        nowMs = { System.currentTimeMillis() },
        audioTagCacheInvalidator = { path -> audioTagReader.get().invalidate(path) },
    )
}
