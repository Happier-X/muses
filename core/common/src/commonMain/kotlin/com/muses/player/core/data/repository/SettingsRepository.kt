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

    // ── AI 推荐（首页「猜你喜欢」，见 :core:ai）──

    /** AI 推荐开关：关（默认）时首页不展示「猜你喜欢」区块 */
    val aiRecommendEnabled: Flow<Boolean>

    /** AI 服务商 key（见 :core:ai 的 `AiProviderPreset`；`custom` = 自填地址） */
    val aiProviderKey: Flow<String>

    /** AI 服务地址（OpenAI 兼容 baseUrl）；空 = 用所选服务商的预设地址 */
    val aiBaseUrl: Flow<String>

    /** AI 模型名；空 = 用所选服务商的默认模型 */
    val aiModel: Flow<String>

    suspend fun setAiRecommendEnabled(enabled: Boolean)

    suspend fun setAiProviderKey(providerKey: String)

    suspend fun setAiBaseUrl(baseUrl: String)

    suspend fun setAiModel(model: String)
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

    override val aiRecommendEnabled: Flow<Boolean>
        get() = dataStore.data.map { prefs -> prefs[AI_RECOMMEND_ENABLED] == true }

    override val aiProviderKey: Flow<String>
        get() = dataStore.data.map { prefs -> prefs[AI_PROVIDER_KEY] ?: DEFAULT_AI_PROVIDER }

    override val aiBaseUrl: Flow<String>
        get() = dataStore.data.map { prefs -> prefs[AI_BASE_URL].orEmpty() }

    override val aiModel: Flow<String>
        get() = dataStore.data.map { prefs -> prefs[AI_MODEL].orEmpty() }

    override suspend fun setAiRecommendEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[AI_RECOMMEND_ENABLED] = enabled }
    }

    override suspend fun setAiProviderKey(providerKey: String) {
        dataStore.edit { prefs -> prefs[AI_PROVIDER_KEY] = providerKey }
    }

    override suspend fun setAiBaseUrl(baseUrl: String) {
        dataStore.edit { prefs -> prefs[AI_BASE_URL] = baseUrl }
    }

    override suspend fun setAiModel(model: String) {
        dataStore.edit { prefs -> prefs[AI_MODEL] = model }
    }

    private companion object {
        val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val AUTO_SCRAPE_ENABLED = booleanPreferencesKey("auto_scrape_enabled")
        val MINI_PLAYER_LYRICS_ENABLED = booleanPreferencesKey("mini_player_lyrics_enabled")
        val NOTIFICATION_LYRICS_ENABLED = booleanPreferencesKey("notification_lyrics_enabled")
        val XIAOMI_ISLAND_ENABLED = booleanPreferencesKey("xiaomi_island_enabled")
        val ONLINE_PREFERRED_QUALITY = stringPreferencesKey("online_preferred_quality")
        val AI_RECOMMEND_ENABLED = booleanPreferencesKey("ai_recommend_enabled")
        val AI_PROVIDER_KEY = stringPreferencesKey("ai_provider_key")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MODEL = stringPreferencesKey("ai_model")

        /** 默认 320k：全平台可用、体积与兼容性最稳（高音质档由用户显式选择） */
        const val DEFAULT_ONLINE_QUALITY = "320k"

        /** 默认服务商（深寻）：国内可用、OpenAI 兼容、价格低，适合作为开箱默认 */
        const val DEFAULT_AI_PROVIDER = "deepseek"
    }
}
