package com.muses.player.core.data.repository

import androidx.room.withTransaction
import com.muses.player.core.data.dao.PlaylistDao
import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.PlaylistEntity
import com.muses.player.core.data.db.PlaylistSongEntity
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.model.Playlist
import com.muses.player.core.model.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 播放列表仓库。
 * 注意：入队播放（playQueue）依赖 M1 的 PlayerConnection，本阶段仅暴露 [observePlaylistSongIds]，
 * 接线留 TODO 给阶段 1 之后。
 */
interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>

    fun observePlaylist(id: String): Flow<PlaylistWithSongs?>

    /** 入队播放接口预留：按播放顺序返回 songIds */
    fun observePlaylistSongIds(id: String): Flow<List<String>>

    /** 按播放顺序返回歌曲领域模型（供整体入队播放） */
    suspend fun getSongs(id: String): List<com.muses.player.core.model.Song>

    suspend fun createPlaylist(name: String): String

    suspend fun renamePlaylist(id: String, name: String)

    suspend fun deletePlaylist(id: String)

    /** 追加歌曲；已在列表内的 songId 自动跳过（去重） */
    suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>)

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    suspend fun moveSong(playlistId: String, fromPosition: Int, toPosition: Int)
}

@Singleton
class RoomPlaylistRepository @Inject constructor(
    private val db: MusesDatabase,
) : PlaylistRepository {

    private val dao: PlaylistDao = db.playlistDao()

    override fun observePlaylists(): Flow<List<Playlist>> =
        dao.observePlaylists().map { list -> list.map { it.toDomain() } }

    override fun observePlaylist(id: String): Flow<PlaylistWithSongs?> =
        combine(dao.observeById(id), dao.observeSongsWithSong(id)) { playlist, rows ->
            playlist?.let {
                PlaylistWithSongs(
                    playlist = it.toDomain(),
                    songs = rows.mapNotNull { row -> row.song?.toDomain() },
                )
            }
        }

    override fun observePlaylistSongIds(id: String): Flow<List<String>> =
        dao.observeSongIds(id)

    override suspend fun getSongs(id: String): List<com.muses.player.core.model.Song> =
        dao.observeSongsWithSong(id).first().mapNotNull { it.song?.toDomain() }


    override suspend fun createPlaylist(name: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        db.withTransaction {
            dao.insert(PlaylistEntity(id = id, name = name, createdAt = now, updatedAt = now))
        }
        return id
    }

    override suspend fun renamePlaylist(id: String, name: String) {
        dao.rename(id, name, System.currentTimeMillis())
    }

    override suspend fun deletePlaylist(id: String) {
        dao.deleteById(id)
    }

    override suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>) {
        if (songIds.isEmpty()) return
        db.withTransaction {
            val existing = dao.getSongIds(playlistId).toHashSet()
            var next = (dao.maxPosition(playlistId) ?: -1) + 1
            val toAdd = songIds.distinct()
                .filterNot { it in existing }
                .map { songId -> PlaylistSongEntity(playlistId, songId, next++) }
            if (toAdd.isNotEmpty()) {
                dao.appendSongs(toAdd)
                dao.touch(playlistId, System.currentTimeMillis())
            }
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        db.withTransaction {
            dao.removeSongAndCompact(playlistId, songId)
            dao.touch(playlistId, System.currentTimeMillis())
        }
    }

    override suspend fun moveSong(playlistId: String, fromPosition: Int, toPosition: Int) {
        db.withTransaction {
            dao.moveSong(playlistId, fromPosition, toPosition)
            dao.touch(playlistId, System.currentTimeMillis())
        }
    }
}
