package com.muses.player.core.media.playback

import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * 播放装配（P2a Hilt→Koin：原 `@Module @InstallIn(SingletonComponent)` + `@Binds`）。
 */
val playbackModule = module {

    singleOf(::NoOpPlaybackController)
    single<PlaybackController> { get<NoOpPlaybackController>() }

    /**
     * Media3 SimpleCache 单例：进程内同目录只允许一个实例。
     * CacheDataSource 边播边缓存，探测性重复 Range 请求命中本地不再发网络（防网关限流）。
     */
    single<SimpleCache> {
        SimpleCache(
            File(androidContext().cacheDir, "exoplayer-playback-cache"),
            LeastRecentlyUsedCacheEvictor(PLAYBACK_CACHE_BYTES),
            StandaloneDatabaseProvider(androidContext()),
        )
    }
}

/** 播放流播磁盘缓存上限（与 DiskWebDavAudioCache 同量级） */
private const val PLAYBACK_CACHE_BYTES = 500L * 1024L * 1024L
