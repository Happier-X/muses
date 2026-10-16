package com.muses.player.core.scrape.writeback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.scrape.RollbackJournal
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 回滚 journal 持久化（规格书 = src/features/scrape/writeback.ts 的
 * readRollbackJournal / writeRollbackJournal / localStorage.removeItem）。
 *
 * 存储介质替换：localStorage key `muses:scrape-rollback` → DataStore Preferences
 * key `scrape_rollback_journal`（JSON snapshot v1）。上限 200 条由编排层截断。
 */
class RollbackJournalStore(private val dataStore: DataStore<Preferences>) {

    companion object {
        /** Web ROLLBACK_KEY = 'muses:scrape-rollback' */
        private val KEY = stringPreferencesKey("scrape_rollback_journal")
        private val HISTORY = stringPreferencesKey("scrape_rollback_history")
    }

    /** 读当前 journal；坏数据/无数据 → null（Web readRollbackJournal 防御语义） */
    suspend fun read(): RollbackJournal? =
        WritebackJson.decodeJournal(dataStore.data.first()[KEY])

    suspend fun write(journal: RollbackJournal) {
        dataStore.edit { prefs ->
            val old = WritebackJson.decodeJournal(prefs[KEY])
            if (old != null && old.journalId != journal.journalId) {
                val confirmed = old.copy(entries = old.entries.filter { it.fileSaved })
                val history = (listOf(confirmed) + decodeHistory(prefs[HISTORY]))
                    .filter { it.entries.isNotEmpty() }.distinctBy { it.journalId }.take(20)
                var total = 0
                val bounded = history.map { WritebackJson.encodeJournal(it) }.takeWhile {
                    total += it.toByteArray(Charsets.UTF_8).size
                    total <= 8 * 1024 * 1024
                }
                prefs[HISTORY] = Json.encodeToString(bounded)
            }
            prefs[KEY] = WritebackJson.encodeJournal(journal)
        }
    }

    private fun decodeHistory(raw: String?): List<RollbackJournal> = try {
        if (raw == null) emptyList() else Json.decodeFromString<List<String>>(raw).mapNotNull(WritebackJson::decodeJournal)
    } catch (_: Exception) { emptyList() }

    /** 一次读取包含当前与旧批次的已确认历史，原始下标与歌曲编号不改写。 */
    suspend fun readHistory(): List<RollbackJournal> {
        val prefs = dataStore.data.first()
        return (listOfNotNull(WritebackJson.decodeJournal(prefs[KEY])) + decodeHistory(prefs[HISTORY]))
            .distinctBy { it.journalId }.map { it.copy(entries = it.entries.filter { entry -> entry.fileSaved }) }
            .filter { it.entries.isNotEmpty() }.take(20)
    }

    /** 在同一次存储事务中核对并清空，避免核对后出现的新记录被误删。 */
    suspend fun clearIfMatches(journalId: String): Boolean {
        var cleared = false
        dataStore.edit { prefs ->
            if (WritebackJson.decodeJournal(prefs[KEY])?.journalId == journalId) {
                prefs.remove(KEY)
                cleared = true
            }
        }
        return cleared
    }

    /** 恢复收尾仅修改最初读取的快照，不能清空或覆盖期间产生的新记录。 */
    suspend fun replaceIfUnchanged(expected: RollbackJournal, replacement: RollbackJournal?): Boolean {
        var replaced = false
        dataStore.edit { prefs ->
            if (WritebackJson.decodeJournal(prefs[KEY]) == expected) {
                if (replacement == null) prefs.remove(KEY)
                else prefs[KEY] = WritebackJson.encodeJournal(replacement)
                replaced = true
            }
        }
        return replaced
    }

    /** Web revertScrapeJournal 尾部的 localStorage.removeItem */
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY)
        }
    }
}
