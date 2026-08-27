package com.muses.player.core.data.tag

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TagModule {
    // AudioTagReader 由 @Inject 构造，无需 @Provides；WorkManager Configuration 已由 MusesApplication 提供
}

/**
 * 标签读取调度器，负责触发后台标签读取任务
 */
@Singleton
class TagReaderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    /**
     * 启动一次性标签读取任务（处理所有未标记的歌曲）
     */
    fun scheduleImmediate() {
        val request = OneTimeWorkRequestBuilder<TagReaderWorker>()
            .build()

        workManager.enqueueUniqueWork(
            TagReaderWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /**
     * 启动定期标签读取任务（每小时检查一次）
     */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<TagReaderWorker>(
            1, TimeUnit.HOURS,
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "${TagReaderWorker.WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * 取消所有标签读取任务
     */
    fun cancel() {
        workManager.cancelUniqueWork(TagReaderWorker.WORK_NAME)
        workManager.cancelUniqueWork("${TagReaderWorker.WORK_NAME}_periodic")
    }
}
