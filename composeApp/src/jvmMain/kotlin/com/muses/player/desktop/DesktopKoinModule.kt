package com.muses.player.desktop

import com.muses.player.core.data.log.RingBufferErrorLogStore
import com.muses.player.core.data.repository.AlbumRepository
import com.muses.player.core.data.repository.ArtistRepository
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.RoomAlbumRepository
import com.muses.player.core.data.repository.RoomArtistRepository
import com.muses.player.core.data.repository.RoomSongRepository
import com.muses.player.core.data.repository.RoomSourceRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.desktop.di.DesktopContainer
import com.muses.player.desktop.playback.DesktopPlayerHook
import com.muses.player.desktop.di.DesktopCredentials
import com.muses.player.core.webdav.webdavCoreModule
import com.muses.player.feature.library.libraryModule
import com.muses.player.feature.player.playerModule
import com.muses.player.feature.scrape.scrapeFeatureModule
import com.muses.player.feature.sources.sourcesCoreModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * 桌面运行时单例（U12）：播放 hook 唯一实例——Main 壳/托盘/SMTC/Koin 端口绑定共用，
 * 维持「Screens 与托盘共享同一播放状态源」契约（hook 依赖 composeApp 层，不能落 :desktop）。
 */
object DesktopRuntime {

    @Volatile private var playerHook: DesktopPlayerHook? = null

    fun playerHook(): DesktopPlayerHook =
        playerHook ?: synchronized(this) {
            playerHook ?: DesktopPlayerHook().also { playerHook = it }
        }

    /** U15：打开外部链接（系统默认浏览器；无桌面会话时静默失败） */
    fun openUrl(url: String) {
        runCatching {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(java.net.URI(url))
            }
        }
    }

    /** U15：写入系统剪贴板（awt Toolkit，与安卓 ClipboardManager 对等） */
    fun copyToClipboard(text: String) {
        runCatching {
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
        }
    }

    /**
     * U15：运行时版本号（构建期写入 muses-desktop-version.txt 资源，与 jpackage
     * packageVersion 同源）；资源缺失（异常环境）回落 1.0.0。
     */
    fun appVersion(): String =
        runCatching {
            DesktopRuntime::class.java.getResourceAsStream("/muses-desktop-version.txt")
                ?.bufferedReader()?.use { it.readText() }?.trim()
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: "1.0.0"
}

/**
 * U23 桌面曲库扫描端口：WebDAV 走上收至 :core:common jvmShared 的真扫描器
 *（[WebDavLibraryScanner]，与安卓同实现：BFS 递归 PROPFIND + 文件名建库，零下载）；
 * 本地目录扫描桌面暂无能力（MediaStore 为安卓专属），LOCAL 类型仍返回空列表。
 *
 * 进度约定：真扫描器 scan 内自行管理进度流（开始重置、结束推 finished=true）；
 * 占位分支（LOCAL）同样先重置再推终态，否则扫描进度弹窗会卡在「正在查找文件」关不掉。
 */
class DesktopLibraryScanPort(
    private val webDavScanner: com.muses.player.core.media.scanner.WebDavLibraryScanner,
) : com.muses.player.feature.sources.LibraryScanPort {

    private val localIdle = kotlinx.coroutines.flow.MutableStateFlow(
        com.muses.player.core.media.scanner.ScanProgress(),
    )

    override fun progressFor(type: com.muses.player.core.model.SourceType) =
        if (type == com.muses.player.core.model.SourceType.WEBDAV) {
            webDavScanner.scanProgress
        } else {
            localIdle as kotlinx.coroutines.flow.Flow<com.muses.player.core.media.scanner.ScanProgress>
        }

    override suspend fun scan(
        source: com.muses.player.core.model.Source,
        readTags: Boolean,
    ): List<com.muses.player.core.model.Song> {
        if (source.type == com.muses.player.core.model.SourceType.WEBDAV) {
            return webDavScanner.scan(source)
        }
        localIdle.value = com.muses.player.core.media.scanner.ScanProgress()
        localIdle.value = com.muses.player.core.media.scanner.ScanProgress(finished = true)
        return emptyList()
    }
}

/**
 * U9 桌面装配：共享 ViewModel（:feature:library commonMain）依赖的 DAO/Repository
 * 接 :core:common JVM 库（[DesktopContainer.database] 单例，P2b 后数据栈全 KMP）。
 * 仓库实现同为 commonMain RoomXxxRepository，安卓侧 Koin 装配（core:data）同构。
 *
 * U11 扩充：SourceRepository/CredentialsRepository/ErrorLogStore（WebDAV 链路依赖，
 * [webdavCoreModule] 的 AuthRegistry/KtorWebDavClient 构造需要）+ webdavCoreModule +
 * sourcesCoreModule（共享 WebDAV 浏览/表单 ViewModel，桌面复用共享浏览页）。
 *
 * U23 扩充（切共享壳）：settingsStore 共享单例 + Settings/PlaybackState/RecentPlays
 * 三仓库（sourcesCoreModule 的 SourcesViewModel 依赖）+ LibraryScanPort 占位 +
 * AppVersionProvider（DesktopRuntime 构建期资源版本）。
 */
fun desktopLibraryModule(): Module = module {
    // U12：端口绑定——共享 PlayerViewModel 经 PlaybackPort 消费桌面播放栈
    single<com.muses.player.core.playback.PlaybackPort> { DesktopRuntime.playerHook() }
    single { DesktopContainer.database().songDao() }
    single { DesktopContainer.database().albumDao() }
    single { DesktopContainer.database().artistDao() }
    single { DesktopContainer.database().sourceDao() }
    single<SongRepository> { RoomSongRepository(get(), get(), get()) }
    single<AlbumRepository> { RoomAlbumRepository(get()) }
    single<ArtistRepository> { RoomArtistRepository(get()) }
    single<SourceRepository> { RoomSourceRepository(get()) }
    // 歌单仓库（桌面库与安卓同实现：RoomPlaylistRepository；缺失会导致歌单页
    // PlaylistsViewModel 构造失败 "Could not create instance"）
    single<com.muses.player.core.data.repository.PlaylistRepository> {
        com.muses.player.core.data.repository.RoomPlaylistRepository(
            com.muses.player.desktop.di.DesktopContainer.database(),
        )
    }
    // WebDAV 凭据（DPAPI，见 [DesktopCredentials]）
    single<CredentialsRepository> { DesktopCredentials() }
    // WebDAV 链路日志（桌面无 CrashHandler 落盘链，环形缓冲即可）
    single<ErrorLogStore> { RingBufferErrorLogStore() }

    // ── U23：共享壳（音源页/设置页）依赖补齐 ──
    single { DesktopContainer.settingsStore }
    single<com.muses.player.core.data.repository.SettingsRepository> {
        com.muses.player.core.data.repository.DataStoreSettingsRepository(get())
    }
    single { com.muses.player.core.data.repository.PlaybackStateRepository(get()) }
    single { com.muses.player.core.data.repository.RecentPlaysRepository(get()) }
    single<com.muses.player.feature.sources.LibraryScanPort> { DesktopLibraryScanPort(get()) }
    single<com.muses.player.feature.shell.platform.AppVersionProvider> {
        object : com.muses.player.feature.shell.platform.AppVersionProvider {
            override val versionName: String = DesktopRuntime.appVersion()
        }
    }

    // ── U14 刮削共享 VM 依赖（实例来自 DesktopScrapeGraph，引擎装配与安卓 ScrapeModule 同口径）──
    single { com.muses.player.desktop.DesktopScrapeGraph.queueStore }
    single { com.muses.player.desktop.DesktopScrapeGraph.textMetaMatcher }
    single { com.muses.player.desktop.DesktopScrapeGraph.coverMatcher }
    single { com.muses.player.desktop.DesktopScrapeGraph.orchestrator }
    single { com.muses.player.desktop.DesktopScrapeGraph.editSearch }
    // ScrapeReviewViewModel 构造首参（安卓由 Koin 导航参数供给，桌面无路由栈显式给空 handle）
    single { androidx.lifecycle.SavedStateHandle() }
}

/** 桌面全量模块：共享 ViewModel 装配 + 桌面 DAO/Repository 接线。 */
val desktopAppModules: List<Module> = listOf(
    desktopLibraryModule(),
    libraryModule,
    playerModule,
    // U23：共享壳路由消费的歌单 VM + 壳层 VM（Main/Settings）
    com.muses.player.feature.playlist.playlistCoreModule,
    com.muses.player.feature.shell.di.shellModule,
    scrapeFeatureModule,
    webdavCoreModule,
    sourcesCoreModule,
)
