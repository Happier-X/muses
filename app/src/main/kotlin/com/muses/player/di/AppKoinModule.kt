package com.muses.player.di

import com.muses.player.BuildConfig
import com.muses.player.core.data.di.databaseModule
import com.muses.player.core.data.repository.repositoryModule
import com.muses.player.core.data.tag.tagModule
import com.muses.player.core.lyrics.di.lyricsModule
import com.muses.player.core.ai.di.aiModule
import com.muses.player.core.lxsdk.di.lxSdkModule
import com.muses.player.core.media.playback.playbackModule
import com.muses.player.core.search.di.searchModule
import com.muses.player.core.scrape.di.scrapeModule
// U11：webdavCoreModule（Ktor 客户端/AuthRegistry/限流器跨平台绑定）+ webdavModule（OkHttp 流播绑定）
import com.muses.player.core.webdav.webdavCoreModule
import com.muses.player.core.webdav.webdavModule
import com.muses.player.feature.library.libraryModule
import com.muses.player.feature.player.playerModule
// U19：playlist 全量上收 commonMain，playlistCoreModule 单装配（原 playlistModule 并入后废弃）
import com.muses.player.feature.playlist.playlistCoreModule
import com.muses.player.feature.scrape.scrapeFeatureModule
// U20：sources 全量上收 commonMain——sourcesCoreModule 装配共享 VM，sourcesPlatformModule
// 绑定安卓扫描端口（MediaStore/WebDAV 扫描器）
import com.muses.player.feature.sources.sourcesCoreModule
import com.muses.player.feature.sources.sourcesPlatformModule
// 首页 VM（搜索框 + 排行榜 + 猜你喜欢）
import com.muses.player.feature.home.homeCoreModule
// U22：壳层 VM（MainViewModel/SettingsViewModel）装配移入 :feature:shell shellModule；
// app 侧仅保留平台绑定（版本号注入共享壳）
import com.muses.player.feature.shell.di.shellModule
import com.muses.player.feature.shell.platform.AppVersionProvider
import org.koin.dsl.module

/** app 平台绑定（U22）：BuildConfig 版本号注入共享壳（桌面侧由 DesktopRuntime 同源绑定）。 */
val appPlatformModule = module {
    single<AppVersionProvider> {
        object : AppVersionProvider {
            override val versionName: String = BuildConfig.VERSION_NAME
        }
    }
}

/** 全量模块聚合：`MusesApplication.startKoin` 唯一入口（P2a design §3）。 */
val appModules = listOf(
    databaseModule,
    tagModule,
    repositoryModule,
    lyricsModule,
    playbackModule,
    // 洛雪自定义音源（在线音源）引擎与端口装配
    lxSdkModule(),
    // 在线搜索（5 平台 provider + 聚合服务）
    searchModule(),
    // AI 推荐（曲库画像 → LLM → 平台匹配，首页「猜你喜欢」）
    aiModule(),
    scrapeModule,
    webdavModule,
    webdavCoreModule,
    libraryModule,
    playerModule,
    playlistCoreModule,
    scrapeFeatureModule,
    sourcesPlatformModule,
    sourcesCoreModule,
    homeCoreModule,
    shellModule,
    appPlatformModule,
)
