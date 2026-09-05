package com.muses.player.core.data.repository

import com.muses.player.core.data.log.ErrorLogCrashPersistence
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.data.log.RingBufferErrorLogStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * 仓库装配（P2a Hilt→Koin：原 `@Module` + `@Binds`）。
 * `@Binds`→`singleOf` + 接口 `single` 委托；同一实现双接口绑定（ErrorLogStore +
 * ErrorLogCrashPersistence）共享同一单例（见 design.md 映射表）。
 *
 * W1 KMP 上收：SongRepository/SourceRepository/AlbumRepository/ArtistRepository 四仓库
 * 与 CredentialsRepository/CryptoEngine 接口已迁 :core:common commonMain（同包名经
 * api(:core:common) 透传解析）；本模块为安卓 Koin 装配，绑定签名与行为不变
 * （含安卓专属 AndroidKeystoreCryptoEngine / AndroidKeyStoreCredentialsRepository 绑定）。
 */
val repositoryModule = module {

    singleOf(::RoomSongRepository)
    single<SongRepository> { get<RoomSongRepository>() }

    singleOf(::RoomSourceRepository)
    single<SourceRepository> { get<RoomSourceRepository>() }

    singleOf(::RoomAlbumRepository)
    single<AlbumRepository> { get<RoomAlbumRepository>() }

    singleOf(::RoomArtistRepository)
    single<ArtistRepository> { get<RoomArtistRepository>() }

    singleOf(::RoomPlaylistRepository)
    single<PlaylistRepository> { get<RoomPlaylistRepository>() }

    singleOf(::DataStoreSettingsRepository)
    single<SettingsRepository> { get<DataStoreSettingsRepository>() }

    singleOf(::AndroidKeyStoreCredentialsRepository)
    single<CredentialsRepository> { get<AndroidKeyStoreCredentialsRepository>() }

    singleOf(::AndroidKeystoreCryptoEngine)
    single<CryptoEngine> { get<AndroidKeystoreCryptoEngine>() }

    /** 错误日志环形缓冲（任务 08-26-settings-log-viewer） */
    singleOf(::RingBufferErrorLogStore)
    single<ErrorLogStore> { get<RingBufferErrorLogStore>() }

    /** 崩溃持久化能力（CrashHandler 专用，同一实现双接口绑定） */
    single<ErrorLogCrashPersistence> { get<RingBufferErrorLogStore>() }

    /** 播放快照/最近播放（PlaybackService 恢复队列用，P2a 补漏：Hilt 时代靠 @Inject 构造自动提供） */
    singleOf(::PlaybackStateRepository)
    singleOf(::RecentPlaysRepository)
}
