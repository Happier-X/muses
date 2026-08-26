package com.muses.player.core.media.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaybackModule {

    @Binds
    abstract fun bindPlaybackController(impl: NoOpPlaybackController): PlaybackController

    companion object {
        /** 播放流播磁盘缓存上限（与 DiskWebDavAudioCache 同量级） */
        private const val PLAYBACK_CACHE_BYTES = 500L * 1024L * 1024L

        /**
         * Media3 SimpleCache 单例：进程内同目录只允许一个实例。
         * CacheDataSource 边播边缓存，探测性重复 Range 请求命中本地不再发网络（防网关限流）。
         */
        @Provides
        @Singleton
        fun providePlaybackCache(@ApplicationContext context: Context): SimpleCache =
            SimpleCache(
                File(context.cacheDir, "exoplayer-playback-cache"),
                LeastRecentlyUsedCacheEvictor(PLAYBACK_CACHE_BYTES),
                StandaloneDatabaseProvider(context),
            )
    }
}
