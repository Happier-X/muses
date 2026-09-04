package com.muses.player.core.data.store

import java.io.File

/**
 * P2b-S2 androidMain actual：filesDir 供给。
 * [initDataStoreDir] 须在首次 [createDataStore] 前调用（DatabaseModule 的 DataStore single 内调用）。
 */
@Volatile
private var filesDirPath: String? = null

/** 幂等初始化：首个值生效（DatabaseModule 的 DataStore single 内调用一次）；重复传入不同值抛错防配错。 */
fun initDataStoreDir(filesDir: File) {
    val path = filesDir.absolutePath
    val cur = filesDirPath
    if (cur == null) {
        filesDirPath = path
    } else {
        check(cur == path) { "initDataStoreDir 重复初始化且路径不一致" }
    }
}

actual fun dataStoreFilePath(fileName: String): String =
    File(
        requireNotNull(filesDirPath) { "initDataStoreDir 未初始化就调用 createDataStore" },
        fileName,
    ).absolutePath
