package com.muses.player

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.muses.player.core.data.log.CrashHandler
import com.muses.player.core.data.log.ErrorLogCrashPersistence
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MusesApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var errorLogCrashPersistence: ErrorLogCrashPersistence

    override fun onCreate() {
        super.onCreate()
        // super.onCreate()（HiltAndroidApp）完成字段注入后依赖已可用；
        // crash handler 必须最先安装，install 内部已 try-catch，不会把正常启动变成崩溃
        CrashHandler.install(this, errorLogCrashPersistence)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
