package com.muses.player.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.sameSong
import com.muses.player.core.model.download.DownloadTrack
import com.muses.player.core.model.online.OnlineTrackRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FavoriteSelection(val track: DownloadTrack, val liked: Boolean)

data class FavoriteSongsState(
    val selections: List<FavoriteSelection> = emptyList(),
    val library: List<Song> = emptyList(),
    val loaded: Boolean = false,
) {
    fun isInLibrary(song: Song): Boolean = library.any { sameSong(it, song) }

    fun isLiked(song: Song): Boolean = isInLibrary(song) ||
        selections.lastOrNull { sameSong(it.track.toSong(), song) }?.liked == true

    /** 仅手动收藏的在线歌曲需要下载；同曲跨平台只保留一份。 */
    val missingOnlineSongs: List<Song> get() = buildList {
        selections.filter { it.liked }.forEach { selection ->
            val song = selection.track.toSong()
            if (OnlineTrackRef.parse(song.path) != null && library.none { sameSong(it, song) } && none { sameSong(it, song) }) add(song)
        }
    }
}

/** 喜欢标记独立持久化，保存在线曲目快照；曲库歌曲固定喜欢。 */
class FavoriteSongsStore(
    private val dataStore: DataStore<Preferences>,
    songs: SongRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val key = stringPreferencesKey("favorite_songs_v1")
    private val json = Json { ignoreUnknownKeys = true }
    private fun decode(prefs: Preferences): List<FavoriteSelection> =
        prefs[key]?.let { json.decodeFromString(it) } ?: emptyList()
    private val snapshots = combine(dataStore.data, songs.observeSongs()) { prefs, library ->
        FavoriteSongsState(decode(prefs), library.filter { it.sourceType != SourceType.ONLINE }, loaded = true)
    }
    val state = snapshots.stateIn(scope, SharingStarted.Eagerly, FavoriteSongsState())
    suspend fun snapshot(): FavoriteSongsState = snapshots.first()

    suspend fun setLiked(song: Song, liked: Boolean): Boolean {
        if (!liked && snapshot().isInLibrary(song)) return false
        dataStore.edit { prefs ->
            val updated = decode(prefs).filterNot { sameSong(it.track.toSong(), song) } +
                FavoriteSelection(DownloadTrack.from(song), liked)
            prefs[key] = json.encodeToString(updated)
        }
        return true
    }
}
