package com.muses.player.feature.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.search.OnlinePlaylist
import com.muses.player.core.ai.localRecommendDay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Serializable
data class CachedFeaturedPlaylists(
    val items: List<OnlinePlaylist>,
    val updatedAt: Long,
    val day: String = "",
) {
    fun isFresh(
        now: Long = Clock.System.now().toEpochMilliseconds(),
        today: String = localRecommendDay(),
    ): Boolean = items.isNotEmpty() && now >= updatedAt && day == today
}

/** 当天保留已选歌单及顺序，只更新信息；隔天从不同候选中重新选择。 */
internal fun selectDailyFeaturedPlaylists(
    candidates: List<OnlinePlaylist>,
    cached: CachedFeaturedPlaylists?,
    today: String,
): List<OnlinePlaylist> {
    val unique = candidates.distinctBy { it.id }
    return if (cached != null && cached.day == today && cached.items.isNotEmpty()) {
        cached.items.take(3).map { selected -> unique.firstOrNull { it.id == selected.id } ?: selected }
    } else unique.shuffled().take(3)
}

/** 网络不可用时继续展示上次加载的真实歌单。 */
class FeaturedPlaylistCacheStore(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("featured_playlists_v1")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): CachedFeaturedPlaylists? {
        val raw = dataStore.data.first()[key] ?: return null
        return runCatching { json.decodeFromString<CachedFeaturedPlaylists>(raw) }.getOrNull()
    }

    suspend fun save(items: List<OnlinePlaylist>, day: String = localRecommendDay()) {
        val value = CachedFeaturedPlaylists(items, Clock.System.now().toEpochMilliseconds(), day)
        dataStore.edit { it[key] = json.encodeToString(CachedFeaturedPlaylists.serializer(), value) }
    }
}
