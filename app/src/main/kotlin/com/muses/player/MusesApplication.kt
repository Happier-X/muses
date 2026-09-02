package com.muses.player

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.muses.player.core.data.log.CrashHandler
import com.muses.player.core.data.log.ErrorLogCrashPersistence
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.jaudiotagger.tag.TagOptionSingleton

@HiltAndroidApp
class MusesApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var errorLogCrashPersistence: ErrorLogCrashPersistence

    override fun onCreate() {
        super.onCreate()
        // 强制 jaudiotagger 走 Android 分支，避免 StandardArtwork 触发 javax.imageio.ImageIO（Android 不存在）
        try {
            TagOptionSingleton.getInstance().setAndroid(true)
        } catch (_: Throwable) {
            // 兜底：初始化失败不阻断启动，TagWriter 侧已有 SafeArtwork 兜底
        }
        // super.onCreate()（HiltAndroidApp）完成字段注入后依赖已可用；
        // crash handler 必须最先安装，install 内部已 try-catch，不会把正常启动变成崩溃
        CrashHandler.install(this, errorLogCrashPersistence)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
