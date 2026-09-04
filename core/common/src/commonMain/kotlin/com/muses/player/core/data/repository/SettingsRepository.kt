package com.muses.player.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 应用设置（DataStore Preferences） */
interface SettingsRepository {
    /** 上次完成扫描的时间戳（epoch millis，0 = 从未扫描） */
    val lastScanTimestamp: Flow<Long>

    /** M3 自动补缺：扫描入库后对无标签歌曲自动加入刮削队列（默认关，DataStore 手动改） */
    val autoScrapeEnabled: Flow<Boolean>

    suspend fun setAutoScrapeEnabled(enabled: Boolean)

    suspend fun updateLastScanTimestamp(timestampMillis: Long)
}

class DataStoreSettingsRepository constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val lastScanTimestamp: Flow<Long>
        get() = dataStore.data.map { prefs -> prefs[LAST_SCAN_TIMESTAMP] ?: 0L }

    override val autoScrapeEnabled: Flow<Boolean>
        get() = dataStore.data.map { prefs -> prefs[AUTO_SCRAPE_ENABLED] == true }

    override suspend fun updateLastScanTimestamp(timestampMillis: Long) {
        dataStore.edit { prefs -> prefs[LAST_SCAN_TIMESTAMP] = timestampMillis }
    }

    override suspend fun setAutoScrapeEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[AUTO_SCRAPE_ENABLED] = enabled }
    }

    private companion object {
        val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val AUTO_SCRAPE_ENABLED = booleanPreferencesKey("auto_scrape_enabled")
    }
}
