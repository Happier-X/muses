package com.muses.player.core.data.platform

/**
 * 桌面/安卓平台目录供给（S1 数据层桌面接线）。
 *
 * commonMain 只声明平台无关的目录语义，不接受桌面专属 API；
 * 具体路径由 androidMain/jvmMain actual 决定。
 */
expect object PlatformDirs {
    /** 应用数据根目录（设置/Room/凭据密钥文件落盘处） */
    fun appDataDir(): String

    /** 音频/图片等缓存根目录（WebDavAudioCache 500MB LRU 落盘处） */
    fun cacheDir(): String

    /** 崩溃日志目录（error_log 收敛处，目录本身；文件名 crash-latest.txt 由调用方拼） */
    fun errorLogDir(): String
}
