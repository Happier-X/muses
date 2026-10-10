package com.muses.player.download

import android.app.*
import android.content.Intent
import android.os.IBinder
import com.muses.player.core.model.download.DownloadStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import org.koin.core.context.GlobalContext

actual fun keepDownloadsRunning(running: Boolean) {
    val context = GlobalContext.get().get<android.content.Context>()
    val intent = Intent(context, DownloadService::class.java)
    if (running) context.startForegroundService(intent) else context.stopService(intent)
}

/** 用户主动开始后才启动前台服务；进程退出后保留任务，重启不自动下载。 */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val manager by lazy { GlobalContext.get().get<DownloadManager>() }
    private val notifications get() = getSystemService(NotificationManager::class.java)
    override fun onCreate() {
        super.onCreate()
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, "歌曲下载", NotificationManager.IMPORTANCE_LOW))
        startForeground(ID, notification("准备下载"))
        scope.launch {
            combine(manager.tasks, manager.pendingScrapeUploads, manager.uploadingScrapeId) { tasks, uploads, uploadId ->
                val task = tasks.firstOrNull { it.status.active }
                val text = task?.let { when (it.status) {
                    DownloadStatus.UPLOADING -> "上传中 · ${it.track.title}"
                    DownloadStatus.SAVING -> "保存中 · ${it.track.title}"
                    DownloadStatus.METADATA -> "保存歌曲信息 · ${it.track.title}"
                    else -> "下载中 · ${it.track.title}"
                } } ?: uploads.firstOrNull { it.id == uploadId }?.let { "刮削补传中 · ${it.title}" } ?: "准备下载"
                text
            }.collect { text ->
                notifications.notify(ID, notification(text))
            }
        }
    }
    private fun notification(text: String): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val content = launch?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) }
        val pause = PendingIntent.getService(this, 1, Intent(this, DownloadService::class.java).setAction(PAUSE), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Muses 下载队列").setContentText(text).setContentIntent(content)
            .setOngoing(true).setOnlyAlertOnce(true).addAction(Notification.Action.Builder(null, "全部暂停", pause).build()).build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == PAUSE) manager.pause()
        return START_NOT_STICKY
    }
    override fun onTimeout(startId: Int, fgsType: Int) { manager.pause(); stopSelf() }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    companion object {
        private const val CHANNEL = "muses_downloads"
        private const val ID = 2042
        private const val PAUSE = "com.muses.player.download.PAUSE"
    }
}
