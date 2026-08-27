package com.muses.player.core.data.repository

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.playback.RecentPlayEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * 最近播放记录（任务 08-25-native-playback-persistence / P2）。
 *
 * 规格书 = src/features/player/recent.ts：播放一首歌即记录，同曲去重置顶，上限 50；
 * 仅存展示所需元数据（title/subtitle/coverUri），点击播放时按 songId 从曲库解析。
 * 存储替换 localStorage → DataStore；事件广播 → StateFlow。
 */
@Singleton
class RecentPlaysRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        /** Web RECENT_STORAGE_KEY = 'muses:recent' */
        private val KEY = stringPreferencesKey("recent_plays")
        private const val SNAPSHOT_VERSION = 1

        /** Web RECENT_LIMIT */
        const val RECENT_LIMIT = 50
    }

    private val _updated = MutableStateFlow(0L)

    /** 记录变化信号（值单调递增，观察者据此刷新） */
    val updated: StateFlow<Long> = _updated

    private fun decode(raw: String?): List<RecentPlayEntry> = runCatching {
        if (raw.isNullOrEmpty()) return emptyList()
        val root = Json.parseToJsonElement(raw).jsonObject
        val entries = root["entries"] as? JsonArray ?: return emptyList()
        entries.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            fun str(key: String): String? =
                (o[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
            RecentPlayEntry(
                songId = str("songId") ?: return@mapNotNull null,
                title = str("title") ?: return@mapNotNull null,
                subtitle = str("subtitle") ?: "",
                coverUri = str("coverUri"),
                playedAt = str("playedAt")?.toLongOrNull() ?: return@mapNotNull null,
            )
        }.take(RECENT_LIMIT)
    }.getOrDefault(emptyList())

    private suspend fun write(entries: List<RecentPlayEntry>) {
        val body = buildJsonObject {
            put("version", JsonPrimitive(SNAPSHOT_VERSION.toString()))
            put("entries", buildJsonArray {
                for (e in entries) {
                    add(buildJsonObject {
                        put("songId", JsonPrimitive(e.songId))
                        put("title", JsonPrimitive(e.title))
                        put("subtitle", JsonPrimitive(e.subtitle))
                        e.coverUri?.let { put("coverUri", JsonPrimitive(it)) }
                        put("playedAt", JsonPrimitive(e.playedAt.toString()))
                    })
                }
            })
        }
        dataStore.edit { prefs ->
            prefs[KEY] = Json.encodeToString(JsonObject.serializer(), body)
        }
        _updated.value += 1
    }

    /** 加载最近播放：最新在前（loadRecentPlays） */
    suspend fun load(): List<RecentPlayEntry> =
        decode(dataStore.data.first()[KEY]).take(RECENT_LIMIT)

    /** 响应式读取（供未来首页消费） */
    fun observe(): Flow<List<RecentPlayEntry>> =
        dataStore.data.map { decode(it[KEY]).take(RECENT_LIMIT) }

    /**
     * 播放时登记（recordRecentPlay）：同曲移到最前，其余保持，超限裁尾。
     */
    suspend fun record(entry: RecentPlayEntry) {
        val plays = decode(dataStore.data.first()[KEY])
            .filter { it.songId != entry.songId }
            .toMutableList()
        plays.add(0, entry)
        write(plays.take(RECENT_LIMIT))
    }

    /** 清空记录 */
    suspend fun clear() {
        if (decode(dataStore.data.first()[KEY]).isEmpty()) return
        write(emptyList())
    }

    /** 删除指定歌曲的最近播放记录（删源时清理，避免底部栏残留已删歌曲信息） */
    suspend fun removeSongs(songIds: Set<String>) {
        if (songIds.isEmpty()) return
        val filtered = decode(dataStore.data.first()[KEY]).filter { it.songId !in songIds }
        write(filtered)
    }
}
