package com.muses.player.core.scrape.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.tag.AudioTagReader
import com.muses.player.core.lyrics.amll.AmllTtmlDbClient
import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.lyrics.lrclib.LrclibProvider
import com.muses.player.core.lyrics.provider.PlatformLyricsProvider
import com.muses.player.core.scrape.cover.CoverMatcher
import com.muses.player.core.scrape.editmeta.AmllLyricsPort
import com.muses.player.core.scrape.editmeta.EditCloudMetaSearch
import com.muses.player.core.scrape.editmeta.ProviderLyricsPort
import com.muses.player.core.scrape.http.ScrapeHttp
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
import com.muses.player.core.webdav.WebDavRateLimiter
import java.io.File
import kotlinx.coroutines.flow.first
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 刮削引擎装配（P2a Hilt→Koin：原 `@Module @InstallIn(SingletonComponent)`）。
 * 仅装配数据层单例，不接线任何 UI（UI 接线归后续任务）。
 * 原 `dagger.Lazy` 延迟点改为 `single {}` 内 eager 局部量 + 运行时 lambda（无循环依赖，可直接求值）。
 *
 * 已知缺口：M1 原生 Source 模型无 username 字段，WebDAV 写回认证用户名暂以空串传入，
 * 接线 UI 前需先补 username 持久化（见 WebDavAudioTagFileWriter 注释）。
 */
val scrapeModule = module {

    // ── 网络与匹配链 ──────────────────────────────────────

    /** 共享限流器：复用 core:webdav 的 WebDavRateLimiter 单例（08-27-webdav-playback-429），刮削与播放共享 4 rps */
    single { ScrapeHttp(rateLimiter = get<WebDavRateLimiter>()) }

    /** 默认文本五源链 kw→tx→wy→kg→mg（metadata/match.ts defaultProviders） */
    single {
        val http: ScrapeHttp = get()
        TextMetaMatcher(
            providers = listOf(
                KwProvider(http),
                TxProvider(http),
                WyProvider(http),
                KgProvider(http),
                MgProvider(http),
            ),
            negativeCache = NegativeCache(),
        )
    }

    /** 默认封面六源链 iTunes→kw→tx→wy→kg→mg（cover/match.ts） */
    single {
        val http: ScrapeHttp = get()
        CoverMatcher.withDefaultProviders(http)
    }

    // ── editMeta 歌词维度端口（L3 接线）───────────────────

    single {
        val http: ScrapeHttp = get()
        val lyricsHttp: LyricsHttp = get()
        val amllClient: AmllTtmlDbClient = get()
        val lrclibProvider: LrclibProvider = get()
        // 文本五源链（metadata/match.ts defaultProviders）
        val textProviders = listOf(
            KwProvider(http),
            TxProvider(http),
            WyProvider(http),
            KgProvider(http),
            MgProvider(http),
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
        EditCloudMetaSearch(
            textProviders = textProviders,
            coverProviders = coverProviders,
            lyricsPorts = lyricsPorts,
        )
    }

    // ── 存储 ──────────────────────────────────────────────

    single { RollbackJournalStore(dataStore = get<DataStore<Preferences>>()) }

    single {
        val songRepository: SongRepository = get()
        ScrapeQueueStore(
            dataStore = get(),
            existingSongIds = { songRepository.observeSongs().first().map { it.id }.toSet() },
        )
    }

    single { ScrapeHistoryStore(dataStore = get<DataStore<Preferences>>()) }

    // ── 写回编排 ──────────────────────────────────────────

    single<CoverBytesFetcher> { HttpCoverBytesFetcher(http = get()) }

    single<AudioTagFileWriter> {
        WebDavAudioTagFileWriter(
            sourceRepository = get(),
            credentialsRepository = get<CredentialsRepository>(),
            webDavClientFactory = { get<WebDavClient>() },
            tempDir = File(androidContext().cacheDir, "scrape-writeback").apply { mkdirs() },
        )
    }

    single {
        val audioTagReader: AudioTagReader = get()
        val historyStore: ScrapeHistoryStore = get()
        WritebackOrchestrator(
            songRepository = get(),
            journalStore = get(),
            fileWriter = get(),
            coverBytesFetcher = get(),
            historySink = { entries -> historyStore.append(entries) },
            nowMs = { System.currentTimeMillis() },
            audioTagCacheInvalidator = { path -> audioTagReader.invalidate(path) },
        )
    }
}
