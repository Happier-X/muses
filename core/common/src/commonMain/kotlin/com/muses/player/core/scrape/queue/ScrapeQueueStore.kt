package com.muses.player.core.scrape.queue

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.scrape.ScrapeQueueItem
import com.muses.player.core.model.scrape.ScrapeQueueSnapshot
import com.muses.player.core.data.store.platformNowIso
import com.muses.player.core.scrape.writeback.WritebackJson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first

/**
 * 待刮削队列存储（规格书 = src/features/scrape/queue.ts）。
 *
 * 独立 DataStore key（与曲库存储分离，避免污染曲库存量）；
 * 入队幂等（按 songId 去重，重入只更新 addedAt）；读取时懒清理已删歌曲；
 * 事件广播 → StateFlow/SharedFlow 替换 Web window CustomEvent。
 */
class ScrapeQueueStore(
    private val dataStore: DataStore<Preferences>,
    /** 懒清理需要判定歌曲是否仍在库 */
    private val existingSongIds: suspend () -> Set<String>,
    private val nowIso: () -> String = { platformNowIso() },
) {

    companion object {
        /** Web SCRAPE_QUEUE_KEY = 'muses:scrape-queue' */
        private val KEY = stringPreferencesKey("scrape_queue")
        private const val SNAPSHOT_VERSION = 1
    }

    private val _updated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 队列变化事件（替代 Web window.dispatchEvent） */
    val updated: SharedFlow<Unit> = _updated

    /** 读原始队列；坏数据宽松回退空表（Web readRawQueue 语义） */
    private suspend fun readRaw(): ScrapeQueueSnapshot =
        WritebackJson.decodeQueue(dataStore.data.first()[KEY])
            ?: ScrapeQueueSnapshot(version = SNAPSHOT_VERSION, items = emptyList())

    private suspend fun write(snapshot: ScrapeQueueSnapshot) {
        dataStore.edit { prefs ->
            prefs[KEY] = WritebackJson.encodeQueue(
                snapshot.copy(version = SNAPSHOT_VERSION),
            )
        }
        _updated.tryEmit(Unit)
    }

    /**
     * 加载队列：懒清理曲库中已不存在的 songId（读取时过滤）。
     * 若懒清理移除了脏数据，写回一次保持存储干净（对齐 loadScrapeQueue）。
     */
    suspend fun load(): List<ScrapeQueueItem> {
        val raw = readRaw()
        val songIds = existingSongIds()
        val items = raw.items.filter { it.songId in songIds }
        if (items.size != raw.items.size) {
            write(ScrapeQueueSnapshot(version = SNAPSHOT_VERSION, items = items))
        }
        return items
    }

    /** 队列中是否包含某 songId */
    suspend fun contains(songId: String): Boolean =
        readRaw().items.any { it.songId == songId }

    /**
     * 批量入队（幂等）：已存在的 songId 只更新 addedAt；返回新增数量。
     */
    suspend fun enqueue(songIds: List<String>): EnqueueResult {
        val raw = readRaw()
        val existing = raw.items.associateBy { it.songId }.toMutableMap()
        val now = nowIso()
        var added = 0
        for (songId in songIds) {
            if (songId.isEmpty()) {
                continue
            }
            if (existing.containsKey(songId)) {
                // 幂等：仅更新时间
                existing[songId] = ScrapeQueueItem(songId = songId, addedAt = now)
            } else {
                existing[songId] = ScrapeQueueItem(songId = songId, addedAt = now)
                added += 1
            }
        }
        write(ScrapeQueueSnapshot(version = SNAPSHOT_VERSION, items = existing.values.toList()))
        return EnqueueResult(added = added)
    }

    /** 批量移除；返回移除数量（无变化不写回，对齐 removeScrapeSongs） */
    suspend fun remove(songIds: List<String>): RemoveResult {
        val raw = readRaw()
        val removeSet = songIds.toSet()
        val items = raw.items.filter { it.songId !in removeSet }
        val removed = raw.items.size - items.size
        if (removed > 0) {
            write(ScrapeQueueSnapshot(version = SNAPSHOT_VERSION, items = items))
        }
        return RemoveResult(removed = removed)
    }

    /** 清空队列（空表不写回） */
    suspend fun clear() {
        if (readRaw().items.isEmpty()) {
            return
        }
        write(ScrapeQueueSnapshot(version = SNAPSHOT_VERSION, items = emptyList()))
    }

    data class EnqueueResult(val added: Int)

    data class RemoveResult(val removed: Int)
}
