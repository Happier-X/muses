package com.muses.player.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 应用设置（DataStore Preferences） */
interface SettingsRepository {
    /** 首次启动标记：引导完成后置 false */
    val isFirstLaunch: Flow<Boolean>

    /** 上次完成扫描的时间戳（epoch millis，0 = 从未扫描） */
    val lastScanTimestamp: Flow<Long>

    /** 响度均衡开关（默认关；语义见 spec/frontend/features-player.md 响度均衡小节） */
    val loudnessEnabled: Flow<Boolean>

    /** M3 自动补缺：扫描入库后对无标签歌曲自动加入刮削队列（默认关，DataStore 手动改） */
    val autoScrapeEnabled: Flow<Boolean>

    suspend fun setAutoScrapeEnabled(enabled: Boolean)

    suspend fun completeFirstLaunch()

    suspend fun updateLastScanTimestamp(timestampMillis: Long)

    suspend fun setLoudnessEnabled(enabled: Boolean)
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val isFirstLaunch: Flow<Boolean>
        get() = dataStore.data.map { prefs -> prefs[FIRST_LAUNCH_DONE] != true }

    override val lastScanTimestamp: Flow<Long>
        get() = dataStore.data.map { prefs -> prefs[LAST_SCAN_TIMESTAMP] ?: 0L }

    override val loudnessEnabled: Flow<Boolean>
        get() = dataStore.data.map { prefs -> prefs[LOUDNESS_ENABLED] == true }

    override val autoScrapeEnabled: Flow<Boolean>
        get() = dataStore.data.map { prefs -> prefs[AUTO_SCRAPE_ENABLED] == true }

    override suspend fun completeFirstLaunch() {
        dataStore.edit { prefs -> prefs[FIRST_LAUNCH_DONE] = true }
    }

    override suspend fun updateLastScanTimestamp(timestampMillis: Long) {
        dataStore.edit { prefs -> prefs[LAST_SCAN_TIMESTAMP] = timestampMillis }
    }

    override suspend fun setLoudnessEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[LOUDNESS_ENABLED] = enabled }
    }

    override suspend fun setAutoScrapeEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[AUTO_SCRAPE_ENABLED] = enabled }
    }

    private companion object {
        val FIRST_LAUNCH_DONE = booleanPreferencesKey("first_launch_done")
        val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val LOUDNESS_ENABLED = booleanPreferencesKey("loudness_enabled")
        val AUTO_SCRAPE_ENABLED = booleanPreferencesKey("auto_scrape_enabled")
    }
}
