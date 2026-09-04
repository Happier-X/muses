package com.muses.player

import android.app.Application
import androidx.work.Configuration
import com.muses.player.core.data.log.CrashHandler
import com.muses.player.core.data.log.ErrorLogCrashPersistence
import com.muses.player.di.appModules
import org.jaudiotagger.tag.TagOptionSingleton
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * 应用入口（P2a Hilt→Koin：改 `startKoin`，字段注入改懒委托；
 * Worker 工厂接线删除，WorkManager 回默认配置——仍经 Configuration.Provider
 * 手动初始化，manifest 保持移除默认 Initializer 不变）。
 */
class MusesApplication : Application(), Configuration.Provider {

    private val errorLogCrashPersistence: ErrorLogCrashPersistence by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MusesApplication)
            modules(appModules)
        }
        // 强制 jaudiotagger 走 Android 分支，避免 StandardArtwork 触发 javax.imageio.ImageIO（Android 不存在）
        try {
            TagOptionSingleton.getInstance().setAndroid(true)
        } catch (_: Throwable) {
            // 兜底：初始化失败不阻断启动，TagWriter 侧已有 SafeArtwork 兜底
        }
        // crash handler 必须最先安装，install 内部已 try-catch，不会把正常启动变成崩溃
        CrashHandler.install(this, errorLogCrashPersistence)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
