package com.muses.player.core.data.platform

import java.io.File

/**
 * S1 jvmMain actual：Windows 桌面路径策略（design §5）。
 *
 * - 优先 `%APPDATA%/muses`（漫游配置，随用户走，符合 Windows 惯例）；
 * - APPDATA 缺失/空白时回退到用户主目录 `~/.muses`（跨平台兜底，Linux/macOS 同理可用）；
 * - 缓存走本地非漫游：优先 `%LOCALAPPDATA%/muses/cache`，缺失时回退 `appDataDir/cache`。
 *
 * 目录延迟创建：appDataDir/cacheDir/errorLogDir 首次调用即 mkdirs，
 * 调用方无需预建目录；并发下 mkdirs 幂等。
 */
actual object PlatformDirs {

    private const val APP_DIR_NAME = "muses"

    actual fun appDataDir(): String {
        val appdata = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
        val dir = if (appdata != null) {
            File(appdata, APP_DIR_NAME)
        } else {
            File(System.getProperty("user.home"), ".muses")
        }
        return ensureDir(dir)
    }

    actual fun cacheDir(): String {
        val localBase = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
        if (localBase != null) {
            return ensureDir(File(File(localBase, APP_DIR_NAME), "cache"))
        }
        return ensureDir(File(File(appDataDir()), "cache"))
    }

    actual fun errorLogDir(): String =
        ensureDir(File(File(appDataDir()), "error_log"))

    private fun ensureDir(dir: File): String {
        dir.mkdirs()
        return dir.absolutePath
    }
}
