package com.muses.player.core.media.scanner

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.muses.player.core.model.SourceType
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 后台扫描任务（WorkManager CoroutineWorker + Hilt EntryPoint）。
 * 入参 sourceId（可选）：缺省扫描默认本地库；指定时按该音源目录前缀过滤。
 */
class ScanWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface Deps {
        fun scanner(): LocalLibraryScanner
        fun songRepository(): com.muses.player.core.data.repository.SongRepository
        fun sourceRepository(): com.muses.player.core.data.repository.SourceRepository
        fun settingsRepository(): com.muses.player.core.data.repository.SettingsRepository
    }

    private fun deps(): Deps = EntryPointAccessors.fromApplication(
        applicationContext,
        Deps::class.java,
    )

    override suspend fun doWork(): Result {
        val deps = deps()
        val sourceId = inputData.getString(KEY_SOURCE_ID)
        return try {
            val songs = if (sourceId == null) {
                deps.scanner().scan(null)
            } else {
                val source = deps.sourceRepository().getSource(sourceId)
                when (source?.type) {
                    SourceType.LOCAL -> deps.scanner().scan(source)
                    else -> deps.scanner().scan(null)
                }
            }
            deps.songRepository().replaceSourceSongs(
                sourceId ?: LocalLibraryScanner.DEFAULT_LOCAL_SOURCE_ID,
                songs,
            )
            deps.settingsRepository().updateLastScanTimestamp(System.currentTimeMillis())
            Result.success(workDataOf(KEY_SCANNED_COUNT to songs.size))
        } catch (_: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_SOURCE_ID = "sourceId"
        const val KEY_SCANNED_COUNT = "scannedCount"
        const val WORK_NAME = "library_scan"
        private const val MAX_RETRIES = 2
    }
}

/** 供引导页/音源管理触发后台扫描的便捷封装 */
object ScanWorkScheduler {

    fun enqueue(context: Context, sourceId: String? = null) {
        val request = OneTimeWorkRequestBuilder<ScanWorker>()
            .setConstraints(
                Constraints.Builder().setRequiresStorageNotLow(true).build(),
            )
            .apply {
                if (sourceId != null) {
                    setInputData(Data.Builder().putString(ScanWorker.KEY_SOURCE_ID, sourceId).build())
                }
            }
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ScanWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
