package com.muses.player.core.data.store

/**
 * P2b-S2 jvmMain 占位 actual（P3 桌面实现真实路径）。
 * 桌面 UI 不在 P2b 范围，jvmTest 走内存 DataStore，不触达此路径。
 */
actual fun dataStoreFilePath(fileName: String): String =
    TODO("P3 实现桌面 DataStore 路径")
