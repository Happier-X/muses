package com.muses.player

import android.content.Context
import androidx.work.*
import com.muses.player.core.lxsdk.store.BuiltinLxSourceUpdater
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/** 有网络时每周检查内置音源；关闭开关时更新器直接跳过。 */
class BuiltinLxUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val updater: BuiltinLxSourceUpdater by inject()
    override suspend fun doWork(): Result = if (updater.checkIfDue()) Result.success() else Result.retry()

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BuiltinLxUpdateWorker>(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInitialDelay(7, TimeUnit.DAYS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("builtin-lx-weekly-update", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
