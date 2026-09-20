package com.muses.player.core.data.platform

import java.io.File

/**
 * S1 androidMain actual：沿用安卓既有 filesDir/cacheDir 语义。
 * [initPlatformDirs] 须在首次使用前调用（**当前由 MusesApplication.onCreate 调用**，
 * 早于 Koin 启动与任何功能访问；Android 侧首个消费者是音源脚本目录）。
 * 未初始化即调用抛错，重复传入不同值抛错防配错（与 DataStorePath.android 口径一致）。
 *
 * 历史注记：原注释声称「DatabaseModule 装配处调用一次」，但该调用实际从未存在，
 * Android 侧此前也无消费者，故缺陷长期潜伏；2025-09 lxsdk 接入时才暴露为崩溃。
 */
actual object PlatformDirs {

    @Volatile
    private var filesDirPath: String? = null

    @Volatile
    private var cacheDirPath: String? = null

    /** 幂等初始化：首个值生效；重复传入不同值抛错。 */
    fun initPlatformDirs(filesDir: File, cacheDir: File) {
        val filesPath = filesDir.absolutePath
        val cachePath = cacheDir.absolutePath
        val curFiles = filesDirPath
        val curCache = cacheDirPath
        if (curFiles == null && curCache == null) {
            filesDirPath = filesPath
            cacheDirPath = cachePath
        } else {
            check(curFiles == filesPath && curCache == cachePath) {
                "initPlatformDirs 重复初始化且路径不一致"
            }
        }
    }

    actual fun appDataDir(): String =
        requireNotNull(filesDirPath) { "initPlatformDirs 未初始化就调用 appDataDir" }

    actual fun cacheDir(): String =
        requireNotNull(cacheDirPath) { "initPlatformDirs 未初始化就调用 cacheDir" }

    actual fun errorLogDir(): String =
        File(requireNotNull(filesDirPath) { "initPlatformDirs 未初始化就调用 errorLogDir" }, "error_log").absolutePath
}
