package com.muses.player.core.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/**
 * DataStore 工厂（P2b-S2）。
 *
 * KMP 口径：PreferenceDataStoreFactory.createWithPath + okio Path；
 * 物理路径由平台供给（expect/actual）：androidMain = filesDir，jvmMain = PlatformDirs.appDataDir。
 * 文件名 [DATASTORE_NAME] 与 key 名全冻结，老用户数据零迁移。
 */
const val DATASTORE_NAME = "muses_settings.preferences_pb"

expect fun dataStoreFilePath(fileName: String): String

fun createDataStore(fileName: String = DATASTORE_NAME): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStoreFilePath(fileName).toPath() },
    )
