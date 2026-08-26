package com.muses.player.core.scrape.queue

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.scrape.ScrapeHistoryEntry
import com.muses.player.core.model.scrape.ScrapeHistorySnapshot
import com.muses.player.core.scrape.writeback.WritebackJson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first

/**
 * 刮削历史存储（规格书 = src/features/scrape/history.ts）。
 *
 * 记录每次写回的成功/失败与失败原因，供刮削「历史」弹层展示。
 * 版本化 snapshot + 滚动清理（按 at 倒序保留最新 200 条）+ 事件广播，
 * 存储模式与 [ScrapeQueueStore] 保持一致；条目自带歌名快照，删歌后仍可展示。
 */
class ScrapeHistoryStore(
    private val dataStore: DataStore<Preferences>,
    private val nowIso: () -> String = { java.time.Instant.now().toString() },
) {

    companion object {
        /** Web SCRAPE_HISTORY_KEY = 'muses:scrape-history' */
        private val KEY = stringPreferencesKey("scrape_history")
        private const val SNAPSHOT_VERSION = 1

        /** Web MAX_HISTORY_ENTRIES：滚动清理上限 */
        const val MAX_ENTRIES: Int = 200

        /** Web genId 降级分支风格：时间戳（ISO）+ 批内序号 + 随机段保证批内唯一 */
        private fun genId(nowIso: String, index: Int): String =
            "hist-$nowIso-$index-${(0..Int.MAX_VALUE).random().toString(36)}"
    }

    private val _updated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 历史变化事件 */
    val updated: SharedFlow<Unit> = _updated

    /** 读原始条目；坏数据宽松回退空表（Web readRawEntries 语义） */
    private suspend fun readRaw(): List<ScrapeHistoryEntry> =
        WritebackJson.decodeHistory(dataStore.data.first()[KEY])?.entries ?: emptyList()

    private suspend fun writeEntries(entries: List<ScrapeHistoryEntry>) {
        dataStore.edit { prefs ->
            prefs[KEY] = WritebackJson.encodeHistory(
                ScrapeHistorySnapshot(version = SNAPSHOT_VERSION, entries = entries),
            )
        }
        _updated.tryEmit(Unit)
    }

    /** 加载历史：按 at 时间倒序返回（最新在前，对齐 loadScrapeHistory） */
    suspend fun load(): List<ScrapeHistoryEntry> =
        readRaw().sortedByDescending { it.at }

    /**
     * 批量追加历史：补 id 与 at=now；追加后滚动清理只保留最新 200 条（对齐 appendScrapeHistory）。
     * 空批次不写回。
     */
    suspend fun append(entries: List<ScrapeHistoryEntry>) {
        if (entries.isEmpty()) {
            return
        }
        val now = nowIso()
        val next = buildList {
            addAll(readRaw())
            addAll(entries.mapIndexed { index, entry ->
                entry.copy(id = genId(now, index), at = now)
            })
        }.sortedByDescending { it.at }
        writeEntries(next.take(MAX_ENTRIES))
    }

    /** 清空历史（空表不写回） */
    suspend fun clear() {
        if (readRaw().isEmpty()) {
            return
        }
        writeEntries(emptyList())
    }
}
