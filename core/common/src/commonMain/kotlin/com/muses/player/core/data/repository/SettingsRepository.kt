package com.muses.player.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 应用设置（DataStore Preferences） */
interface SettingsRepository {
    /** 上次完成扫描的时间戳（epoch millis，0 = 从未扫描） */
    val lastScanTimestamp: Flow<Long>

    /** M3 自动补缺：扫描入库后对无标签歌曲自动加入刮削队列（默认关，DataStore 手动改） */
    val autoScrapeEnabled: Flow<Boolean>

    /** 播放控件（底部迷你条）歌词开关：开启后迷你条第二行显示当前歌词而非艺术家 */
    val miniPlayerLyricsEnabled: Flow<Boolean>

    /** 媒体通知歌词开关：开启后通知标题显示歌词，艺术家显示「标题-艺术家」 */
    val notificationLyricsEnabled: Flow<Boolean>

    /**
     * 小米超级岛/焦点通知开关（MeloX 式可选适配）：仅 HyperOS 设备上有效，
     * 开启后播放通知携带 `miui.focus.param` 岛参数；非小米设备上无任何作用。
     * 默认开（非支持设备/无白名单时 extras 被系统忽略，行为不变）。
     */
    val xiaomiIslandEnabled: Flow<Boolean>

    suspend fun setAutoScrapeEnabled(enabled: Boolean)

    suspend fun setMiniPlayerLyricsEnabled(enabled: Boolean)

    suspend fun setNotificationLyricsEnabled(enabled: Boolean)

    /**
     * 在线音源首选音质（洛雪音质 key，如 `320k`/`flac`/`hires`/`master`）。
     * 仅作**偏好**：脚本未声明该档位时会就近回退（见 LxScriptRepository.pickQuality）。
     */
    val onlinePreferredQuality: Flow<String>

    suspend fun setOnlinePreferredQuality(qualityKey: String)

    suspend fun setXiaomiIslandEnabled(enabled: Boolean)

    suspend fun updateLastScanTimestamp(timestampMillis: Long)
}

class DataStoreSettingsRepository constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val lastScanTimestamp: Flow<Long>
        get() = dataStore.data.map { prefs -> prefs[LAST_SCAN_TIMESTAMP] ?: 0L }

    override val autoScrapeEnabled: Flow<Boolean>
        get() = dataStore.data.map { prefs -> prefs[AUTO_SCRAPE_ENABLED] == true }

    override val miniPlayerLyricsEnabled: Flow<Boolean>
        get() = dataStore.data.map { prefs -> prefs[MINI_PLAYER_LYRICS_ENABLED] == true }

    override val notificationLyricsEnabled: Flow<Boolean>
        get() = dataStore.data.map { prefs -> prefs[NOTIFICATION_LYRICS_ENABLED] == true }

    override val xiaomiIslandEnabled: Flow<Boolean>
        get() = dataStore.data.map { prefs -> prefs[XIAOMI_ISLAND_ENABLED] != false }

    override suspend fun updateLastScanTimestamp(timestampMillis: Long) {
        dataStore.edit { prefs -> prefs[LAST_SCAN_TIMESTAMP] = timestampMillis }
    }

    override suspend fun setAutoScrapeEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[AUTO_SCRAPE_ENABLED] = enabled }
    }

    override suspend fun setMiniPlayerLyricsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[MINI_PLAYER_LYRICS_ENABLED] = enabled }
    }

    override suspend fun setNotificationLyricsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[NOTIFICATION_LYRICS_ENABLED] = enabled }
    }

    override val onlinePreferredQuality: Flow<String>
        get() = dataStore.data.map { prefs -> prefs[ONLINE_PREFERRED_QUALITY] ?: DEFAULT_ONLINE_QUALITY }

    override suspend fun setOnlinePreferredQuality(qualityKey: String) {
        dataStore.edit { prefs -> prefs[ONLINE_PREFERRED_QUALITY] = qualityKey }
    }

    override suspend fun setXiaomiIslandEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[XIAOMI_ISLAND_ENABLED] = enabled }
    }

    private companion object {
        val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val AUTO_SCRAPE_ENABLED = booleanPreferencesKey("auto_scrape_enabled")
        val MINI_PLAYER_LYRICS_ENABLED = booleanPreferencesKey("mini_player_lyrics_enabled")
        val NOTIFICATION_LYRICS_ENABLED = booleanPreferencesKey("notification_lyrics_enabled")
        val XIAOMI_ISLAND_ENABLED = booleanPreferencesKey("xiaomi_island_enabled")
        val ONLINE_PREFERRED_QUALITY = stringPreferencesKey("online_preferred_quality")

        /** 默认 320k：全平台可用、体积与兼容性最稳（高音质档由用户显式选择） */
        const val DEFAULT_ONLINE_QUALITY = "320k"
    }
}
