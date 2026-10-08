package com.muses.player.sync

import com.muses.player.core.data.repository.PlayStats
import com.muses.player.core.model.playback.RecentPlayEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WebDavSyncConfig(val url: String = "", val username: String = "", val directory: String = "MusesSync")

enum class SyncContent { PREFERENCES, SOURCES, HISTORY, STATS }

@Serializable
data class SyncSelection(
    val preferences: Boolean = true,
    val sources: Boolean = true,
    val history: Boolean = true,
    val stats: Boolean = true,
) {
    val anyEnabled: Boolean get() = preferences || sources || history || stats

    fun withEnabled(content: SyncContent, enabled: Boolean): SyncSelection = when (content) {
        SyncContent.PREFERENCES -> copy(preferences = enabled)
        SyncContent.SOURCES -> copy(sources = enabled)
        SyncContent.HISTORY -> copy(history = enabled)
        SyncContent.STATS -> copy(stats = enabled)
    }
}

@Serializable
data class SyncValue(val value: JsonElement, val changedAt: Long, val deviceId: String)

@Serializable
data class SyncSource(val id: String, val name: String, val url: String, val path: String?, val username: String?)

@Serializable
data class SyncScript(val id: String, val source: String, val enabled: Boolean, val sourceUrl: String?)

@Serializable
data class SyncTrack(
    val id: String, val sourceId: String, val path: String, val title: String,
    val artist: String?, val album: String?, val durationMs: Long, val coverUri: String?,
    val online: Boolean,
)

@Serializable
data class SyncSnapshot(
    val version: Int = 1,
    val deviceId: String,
    val updatedAt: Long,
    val preferences: Map<String, SyncValue> = emptyMap(),
    val sources: Map<String, SyncValue> = emptyMap(),
    val scripts: Map<String, SyncValue> = emptyMap(),
    val tracks: List<SyncTrack> = emptyList(),
    val history: List<RecentPlayEntry> = emptyList(),
    val stats: PlayStats = PlayStats.Empty,
)

/** 相同字段按同步时记录的修改时间合并，并以设备 ID 解决同毫秒冲突。 */
fun mergeSyncValues(values: Collection<Map<String, SyncValue>>): Map<String, SyncValue> =
    values.flatMap { it.entries }.groupBy { it.key }.mapValues { (_, entries) ->
        entries.maxWith(compareBy<Map.Entry<String, SyncValue>> { it.value.changedAt }.thenBy { it.value.deviceId }).value
    }

fun publicCover(uri: String?): String? = uri?.takeIf(::publicHttpUrl)

fun publicHttpUrl(value: String): Boolean {
    val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return false
    return uri.scheme in setOf("http", "https") && !uri.host.isNullOrEmpty() &&
        uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null
}
