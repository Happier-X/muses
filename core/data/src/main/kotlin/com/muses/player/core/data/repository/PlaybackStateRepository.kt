package com.muses.player.core.data.repository

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.playback.PlaybackSessionInfo
import com.muses.player.core.model.playback.PlayerConfig
import com.muses.player.core.model.playback.QueueItem
import com.muses.player.core.model.playback.QueueSnapshotData
import com.muses.player.core.model.playback.RepeatMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * 播放快照 + 播放器配置持久化（任务 08-25-native-playback-persistence / P0）。
 *
 * 规格书 = src/features/player/queue.ts + session.ts：
 * - 队列+会话合并为单个 JSON snapshot key `playback_snapshot`（原子恢复语义，见 design.md D3）
 * - 配置独立 key `playback_config`
 * - schema 带 version；宽松解析回退默认值（对齐 Web isRecord/isQueueItem 校验风格）
 */
@Singleton
class PlaybackStateRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val SNAPSHOT_KEY = stringPreferencesKey("playback_snapshot")
        private val CONFIG_KEY = stringPreferencesKey("playback_config")

        private const val SNAPSHOT_VERSION = 1
        private val json = Json
    }

    // ── 数据结构 ──────────────────────────────────────────

    /** 队列+会话合并快照（恢复时一次性读取） */
    data class PlaybackSnapshot(
        val items: List<QueueItem> = emptyList(),
        val originalOrder: List<QueueItem> = emptyList(),
        val shuffleOrder: List<QueueItem>? = null,
        val currentIndex: Int = -1,
        val positionMs: Long = 0L,
        val currentSongId: String? = null,
    )

    // ── JSON 编解码 ───────────────────────────────────────

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

    private fun JsonObject.int(key: String): Int? = str(key)?.toIntOrNull()

    private fun JsonObject.long(key: String): Long? = str(key)?.toLongOrNull()

    private fun JsonObject.queueItems(key: String): List<QueueItem> =
        (this[key] as? JsonArray)?.mapNotNull { el ->
            (el as? JsonObject)?.str("songId")?.let { QueueItem(it) }
        }.orEmpty()

    private fun queueItemsJson(items: List<QueueItem>): JsonArray = buildJsonArray {
        for (item in items) {
            add(buildJsonObject { put("songId", JsonPrimitive(item.songId)) })
        }
    }

    private fun encodeSnapshot(snapshot: PlaybackSnapshot): String {
        val body = buildJsonObject {
            put("version", JsonPrimitive(SNAPSHOT_VERSION.toString()))
            put("items", queueItemsJson(snapshot.items))
            put("originalOrder", queueItemsJson(snapshot.originalOrder))
            snapshot.shuffleOrder?.let { put("shuffleOrder", queueItemsJson(it)) }
            put("currentIndex", JsonPrimitive(snapshot.currentIndex.toString()))
            put("positionMs", JsonPrimitive(snapshot.positionMs.toString()))
            snapshot.currentSongId?.let { put("currentSongId", JsonPrimitive(it)) }
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }

    private fun decodeSnapshot(raw: String?): PlaybackSnapshot? = runCatching {
        if (raw.isNullOrEmpty()) return null
        val root = json.parseToJsonElement(raw).jsonObject
        PlaybackSnapshot(
            items = root.queueItems("items"),
            originalOrder = root.queueItems("originalOrder"),
            shuffleOrder = root["shuffleOrder"]?.let { el ->
                (el as? JsonArray)?.mapNotNull { o ->
                    ((o as? JsonObject)?.str("songId"))?.let { QueueItem(it) }
                }
            },
            currentIndex = root.int("currentIndex") ?: -1,
            positionMs = (root.long("positionMs") ?: 0L).coerceAtLeast(0),
            currentSongId = root.str("currentSongId")?.takeIf(String::isNotEmpty),
        )
    }.getOrNull()

    // ── 快照读写 ──────────────────────────────────────────

    suspend fun readSnapshot(): PlaybackSnapshot? =
        decodeSnapshot(dataStore.data.first()[SNAPSHOT_KEY])

    suspend fun writeSnapshot(snapshot: PlaybackSnapshot) {
        dataStore.edit { prefs ->
            prefs[SNAPSHOT_KEY] = encodeSnapshot(snapshot)
        }
    }

    suspend fun clearSnapshot() {
        dataStore.edit { prefs -> prefs.remove(SNAPSHOT_KEY) }
    }

    /** 删除快照中指定歌曲（删源时清理，避免恢复/底部栏残留已删歌曲） */
    suspend fun removeSongs(songIds: Set<String>) {
        if (songIds.isEmpty()) return
        val snapshot = readSnapshot() ?: return
        val filterList: (List<QueueItem>) -> List<QueueItem> = { list -> list.filter { it.songId !in songIds } }
        val newItems = filterList(snapshot.items)
        val newOriginal = filterList(snapshot.originalOrder)
        val newShuffle = snapshot.shuffleOrder?.let { filterList(it) }
        val currentId = snapshot.currentSongId
        val currentDeleted = currentId != null && currentId in songIds
        val newCurrentIndex = if (currentDeleted) -1 else snapshot.currentIndex.let { idx ->
            // 若删除的是当前项之前的歌曲，索引前移；否则保持
            val deletedBefore = snapshot.items.take(idx).count { it.songId in songIds }
            (idx - deletedBefore).coerceAtLeast(-1)
        }
        val newPosition = if (currentDeleted) 0L else snapshot.positionMs
        val newCurrentId: String? = if (currentDeleted) null else currentId
        // 若队列空则直接清空快照
        if (newItems.isEmpty()) {
            clearSnapshot()
            return
        }
        writeSnapshot(
            snapshot.copy(
                items = newItems,
                originalOrder = newOriginal,
                shuffleOrder = newShuffle,
                currentIndex = newCurrentIndex.coerceIn(-1, newItems.size - 1),
                positionMs = newPosition,
                currentSongId = newCurrentId,
            ),
        )
    }

    // ── 播放器配置 ────────────────────────────────────────

    private fun decodeConfig(raw: String?): PlayerConfig? = runCatching {
        if (raw.isNullOrEmpty()) return null
        val root = json.parseToJsonElement(raw).jsonObject
        val repeatWire = root.str("repeatMode")
        PlayerConfig(
            repeatMode = RepeatMode.entries.firstOrNull { it.wire == repeatWire } ?: RepeatMode.ALL,
            shuffleEnabled = root.str("shuffleEnabled") == "true",
        )
    }.getOrNull()

    /** 读配置；坏数据回退默认值（loadConfig 语义） */
    suspend fun readConfig(): PlayerConfig =
        decodeConfig(dataStore.data.first()[CONFIG_KEY]) ?: PlayerConfig()

    suspend fun writeConfig(config: PlayerConfig) {
        val body = buildJsonObject {
            put("version", JsonPrimitive(SNAPSHOT_VERSION.toString()))
            put("repeatMode", JsonPrimitive(config.repeatMode.wire))
            put("shuffleEnabled", JsonPrimitive(config.shuffleEnabled.toString()))
        }
        dataStore.edit { prefs ->
            prefs[CONFIG_KEY] = json.encodeToString(JsonObject.serializer(), body)
        }
    }
}
