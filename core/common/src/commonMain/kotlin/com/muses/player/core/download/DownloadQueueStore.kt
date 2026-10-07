package com.muses.player.core.download

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.download.DownloadTask
import com.muses.player.core.model.download.DownloadStatus
import com.muses.player.core.model.download.DownloadTarget
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 与曲库独立的持久队列；在线歌曲快照不会因进程退出而丢失。 */
class DownloadQueueStore(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("download_queue_v1")
    private val targetKey = stringPreferencesKey("download_target_v1")
    private val json = Json { ignoreUnknownKeys = true }
    private fun decode(raw: String?): List<DownloadTask> = if (raw == null) emptyList() else json.decodeFromString(raw)
    val tasks = dataStore.data.map { decode(it[key]) }
    val defaultTarget = dataStore.data.map { prefs -> prefs[targetKey]?.let { json.decodeFromString<DownloadTarget>(it) } ?: DownloadTarget() }
    suspend fun setDefaultTarget(target: DownloadTarget) { dataStore.edit { it[targetKey] = json.encodeToString(target) } }
    suspend fun load() = tasks.first()

    suspend fun update(transform: (List<DownloadTask>) -> List<DownloadTask>) {
        dataStore.edit { it[key] = json.encodeToString(transform(decode(it[key]))) }
    }

    suspend fun enqueue(task: DownloadTask): Boolean {
        var added = false
        update { existing ->
            if (existing.any { it.track.id == task.track.id }) existing
            else (existing + task).also { added = true }
        }
        return added
    }

    /** 进程中断后保留任务；必须由用户手动继续，不自动开始待下载项。 */
    suspend fun recoverInterrupted() = update { tasks -> tasks.map {
        if (it.status.active) it.copy(status = DownloadStatus.PAUSED, error = "上次下载已中断，点击继续") else it
    } }
}
