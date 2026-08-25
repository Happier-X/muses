package com.muses.player.core.scrape.writeback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.scrape.RollbackJournal
import kotlinx.coroutines.flow.first

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
    }

    /** 读当前 journal；坏数据/无数据 → null（Web readRollbackJournal 防御语义） */
    suspend fun read(): RollbackJournal? =
        WritebackJson.decodeJournal(dataStore.data.first()[KEY])

    suspend fun write(journal: RollbackJournal) {
        dataStore.edit { prefs ->
            prefs[KEY] = WritebackJson.encodeJournal(journal)
        }
    }

    /** Web revertScrapeJournal 尾部的 localStorage.removeItem */
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY)
        }
    }
}
