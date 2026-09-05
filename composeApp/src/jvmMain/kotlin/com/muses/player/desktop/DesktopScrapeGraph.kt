package com.muses.player.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.muses.player.core.data.platform.PlatformDirs
import com.muses.player.core.data.repository.RoomSongRepository
import com.muses.player.core.data.repository.RoomSourceRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.store.createDataStore
import com.muses.player.core.model.SourceType
import com.muses.player.core.scrape.cover.CoverMatcher
import com.muses.player.core.scrape.editmeta.EditCloudMetaSearch
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.ports.JaudiotaggerTagPort
import com.muses.player.core.scrape.ports.TagPort
import com.muses.player.core.scrape.queue.ScrapeHistoryStore
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.scrape.text.TextMetaMatcher
import com.muses.player.core.scrape.writeback.AudioTagFileWriter
import com.muses.player.core.scrape.writeback.HttpCoverBytesFetcher
import com.muses.player.core.scrape.writeback.LocalAudioTagFileWriter
import com.muses.player.core.scrape.writeback.RollbackJournalStore
import com.muses.player.core.scrape.writeback.WebDavAudioTagFileWriter
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import com.muses.player.core.webdav.WebDavRateLimiter
import com.muses.player.desktop.cache.DesktopWebDavAudioCache
import com.muses.player.desktop.di.DesktopContainer
import com.muses.player.desktop.log.DesktopErrorLogStore
import com.muses.player.desktop.webdav.DesktopWebDavClient
import java.io.File

/**
 * 桌面刮削装配（W4 桌面装配收尾，任务 09-05-scrape-kmp R5；进程内单例）。
 *
 * 引擎全部来自 `:core:common` commonMain（W1-W3 已上收）：
 * - 匹配：[EditCloudMetaSearch] 全链（文本五源 + 封面六源）；歌词维度降级 =
 *   `lyricsPorts = emptyList()`（PRD D2，core:lyrics 未 KMP 化，UI 明示"桌面暂不支持"）；
 * - 写回：[WritebackOrchestrator] + jvmShared [JaudiotaggerTagPort]（jaudiotagger 纯 JVM 双端共用）；
 *   文件写入按 `song.sourceType` 分流（Web writeback.ts writeFile 分派语义）：
 *   WEBDAV → 下载-写标签-上传（[WebDavAudioTagFileWriter]），其余 → 本地直写（[LocalAudioTagFileWriter]）；
 * - 三仓库：[DesktopContainer.database] 的 commonMain 仓库实例 + [DesktopCredentials]；
 * - WebDAV：[DesktopWebDavClient]（desktop 模块，与安卓 KtorWebDavClient 同契约：429/401/Retry-After）。
 *
 * DataStore 用独立文件名：`createDataStore()` 默认文件（muses_settings）已被
 * DesktopCredentials 等各自实例化，同文件多 DataStore 实例会抛
 * 「multiple DataStores active」—— 刮削队列/journal/历史三 store 共用同一
 * `muses_scrape.preferences_pb` 实例（key 互不相交），彻底规避冲突。
 */
internal object DesktopScrapeGraph {

    private val scrapeDataStore: DataStore<Preferences> by lazy {
        createDataStore(fileName = "muses_scrape.preferences_pb")
    }

    val queueStore: ScrapeQueueStore by lazy {
        ScrapeQueueStore(
            dataStore = scrapeDataStore,
            existingSongIds = {
                DesktopContainer.database().songDao().getAll().map { it.id }.toSet()
            },
        )
    }

    private val historyStore: ScrapeHistoryStore by lazy {
        ScrapeHistoryStore(dataStore = scrapeDataStore)
    }

    private val journalStore: RollbackJournalStore by lazy {
        RollbackJournalStore(dataStore = scrapeDataStore)
    }

    /** 共享限流桶：默认 4 rps（文本/封面/封面字节/WebDAV 客户端全链共用，防叠加 burst） */
    private val rateLimiter: WebDavRateLimiter by lazy { WebDavRateLimiter() }

    private val http: ScrapeHttp by lazy { ScrapeHttp(rateLimiter = rateLimiter) }

    /** 编辑页全链云搜（多候选 + 粗排）；桌面批量匹配与单曲重搜共用。歌词维度不参与（降级） */
    val editSearch: EditCloudMetaSearch by lazy {
        EditCloudMetaSearch(
            textProviders = TextMetaMatcher.defaultProviders(http),
            coverProviders = CoverMatcher.defaultProviders(http),
            lyricsPorts = emptyList(),
        )
    }

    private val tagPort: TagPort = JaudiotaggerTagPort

    /** 桌面 WebDAV 客户端（写回上传/下载 + 未来浏览复用；限流与刮削共享同桶） */
    val webDavClient: DesktopWebDavClient by lazy {
        DesktopWebDavClient(rateLimiter = rateLimiter, errorLogStore = DesktopErrorLogStore)
    }

    private val songRepository: SongRepository by lazy {
        val db = DesktopContainer.database()
        RoomSongRepository(songDao = db.songDao(), albumDao = db.albumDao(), artistDao = db.artistDao())
    }

    private val localWriter: LocalAudioTagFileWriter by lazy {
        LocalAudioTagFileWriter(tagPort)
    }

    private val webdavWriter: WebDavAudioTagFileWriter by lazy {
        WebDavAudioTagFileWriter(
            sourceRepository = RoomSourceRepository(DesktopContainer.database().sourceDao()),
            credentialsRepository = DesktopContainer.credentials(),
            webDavClientFactory = { webDavClient },
            tagPort = tagPort,
            tempDir = File(PlatformDirs.cacheDir(), "scrape-writeback"),
        )
    }

    /** 按来源分流（Web writeback.ts writeFile 分派语义）；失败由各 writer 折叠为 FileWriteResult */
    private val fileWriter: AudioTagFileWriter = AudioTagFileWriter { song, changes, coverBytes ->
        when (song.sourceType) {
            SourceType.WEBDAV -> webdavWriter.write(song, changes, coverBytes)
            else -> localWriter.write(song, changes, coverBytes)
        }
    }

    val orchestrator: WritebackOrchestrator by lazy {
        WritebackOrchestrator(
            songRepository = songRepository,
            journalStore = journalStore,
            fileWriter = fileWriter,
            coverBytesFetcher = HttpCoverBytesFetcher(http),
            historySink = { entries -> historyStore.append(entries) },
            nowMs = { System.currentTimeMillis() },
            // 写文件成功后失效桌面 WebDAV 播放缓存（对齐安卓 AudioTagReader.invalidate：
            // 播放链整文件入缓存，写回后必须删旧文件；本地路径 sha256 键不命中为 no-op）
            audioTagCacheInvalidator = { path -> DesktopContainer.audioCache().invalidate(path) },
        )
    }
}
