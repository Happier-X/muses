package com.muses.player.core.data.store

import com.muses.player.core.data.platform.PlatformDirs
import java.io.File

/**
 * S1 jvmMain actual：桌面 DataStore 真实路径（design §5）。
 * 落盘 `<appDataDir>/<fileName>`，与安卓侧 filesDir 同文件语义；
 * 文件名 [DATASTORE_NAME] 冻结，老用户数据零迁移。
 */
actual fun dataStoreFilePath(fileName: String): String =
    File(PlatformDirs.appDataDir(), fileName).absolutePath
