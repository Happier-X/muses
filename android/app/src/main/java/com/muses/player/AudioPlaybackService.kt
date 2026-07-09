package com.muses.player

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * 播放与媒体通知已迁移到 @capgo/capacitor-native-audio。
 * 保留空服务只为兼容旧安装包或系统可能残留的显式服务启动请求，避免旧前台服务/手动通知重复出现。
 */
class AudioPlaybackService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
