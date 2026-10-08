package com.muses.player.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.data.store.platformNowMs
import com.muses.player.core.model.playback.RecentPlayEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * 播放一首歌即记录、同曲去重置顶，仅持久保留最近半年的记录；
 * 仅存展示所需元数据（title/subtitle/coverUri），点击播放时按 songId 从曲库解析。
 * 存储替换 localStorage → DataStore；事件广播 → StateFlow。
 */
class RecentPlaysRepository constructor(private val dataStore: DataStore<Preferences>) {
    private val mutex = Mutex()

    /** 合并历史，按同曲最近时间去重，不删除本机记录。 */
    suspend fun merge(entries: List<RecentPlayEntry>) = mutex.withLock {
        val merged = retainRecent(decode(dataStore.data.first()[KEY]) + entries, platformNowMs())
            .sortedByDescending { it.playedAt }.distinctBy { it.songId }
        write(merged)
    }

    companion object {
        /** Web RECENT_STORAGE_KEY = 'muses:recent' */
        private val KEY = stringPreferencesKey("recent_plays")
        private const val SNAPSHOT_VERSION = 1

        /** 半年按 183 天折算。 */
        const val RETENTION_MS = 183L * 24 * 60 * 60 * 1000
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
        }
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

    /** 清理并读取最近半年记录。 */
    private suspend fun readAndPrune(): List<RecentPlayEntry> = mutex.withLock {
        val entries = decode(dataStore.data.first()[KEY])
        val retained = retainRecent(entries, platformNowMs())
        if (retained.size != entries.size) write(retained)
        retained
    }

    suspend fun load(): List<RecentPlayEntry> = readAndPrune()

    /** 订阅时先清理过期记录，之后持续观察半年内的历史。 */
    fun observe(): Flow<List<RecentPlayEntry>> =
        dataStore.data
            .onStart { readAndPrune() }
            .map { retainRecent(decode(it[KEY]), platformNowMs()) }

    /**
     * 播放时登记：清理半年外记录，同曲移到最前。
     */
    suspend fun record(entry: RecentPlayEntry) = mutex.withLock {
        val cutoff = platformNowMs() - RETENTION_MS
        val plays = decode(dataStore.data.first()[KEY])
            .filter { it.playedAt >= cutoff }
            .filter { it.songId != entry.songId }
            .toMutableList()
        if (entry.playedAt >= cutoff) plays.add(0, entry)
        write(plays)
    }

    /** 清空记录 */
    suspend fun clear() = mutex.withLock {
        if (decode(dataStore.data.first()[KEY]).isEmpty()) return@withLock
        write(emptyList())
    }

    /** 删除指定歌曲的最近播放记录（删源时清理，避免底部栏残留已删歌曲信息） */
    suspend fun removeSongs(songIds: Set<String>) = mutex.withLock {
        if (songIds.isEmpty()) return@withLock
        val filtered = retainRecent(decode(dataStore.data.first()[KEY]), platformNowMs())
            .filter { it.songId !in songIds }
        write(filtered)
    }

    private fun retainRecent(entries: List<RecentPlayEntry>, nowMs: Long): List<RecentPlayEntry> {
        val cutoff = nowMs - RETENTION_MS
        return entries.filter { it.playedAt >= cutoff }
    }
}
