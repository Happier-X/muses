package com.muses.player.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.muses.player.core.data.db.SongAlbumCrossRef
import com.muses.player.core.data.db.SongArtistCrossRef
import com.muses.player.core.data.db.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Upsert
    suspend fun insertAll(songs: List<SongEntity>)

    @Upsert
    suspend fun upsert(song: SongEntity)

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<SongEntity>

    /** M3 自动补缺：未读过标签的歌（文件名建库 tagsVersion=0 或本地未扫） */
    @Query("SELECT id FROM songs WHERE tagsVersion < 1")
    suspend fun getUntaggedSongIds(): List<String>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id = :id")
    fun observeById(id: String): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY title COLLATE NOCASE ASC")
    suspend fun searchByTitle(query: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE sourceId = :sourceId")
    suspend fun getBySource(sourceId: String): List<SongEntity>

    @Query("DELETE FROM songs WHERE sourceId = :sourceId AND id NOT IN (:keepIds)")
    suspend fun deleteBySourceExcept(sourceId: String, keepIds: List<String>)

    @Query("DELETE FROM songs WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    /** 删除单曲（playlist_songs 外键 CASCADE 联动清理关联行） */
    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Transaction
    suspend fun replaceSourceSongs(sourceId: String, songs: List<SongEntity>) {
        insertAll(songs)
        deleteBySourceExcept(sourceId, songs.map { it.id })
    }

    /** 清空专辑/艺术家索引（重建前调用） */
    @Query("DELETE FROM song_album_cross_ref")
    suspend fun clearSongAlbumRefs()

    @Query("DELETE FROM song_artist_cross_ref")
    suspend fun clearSongArtistRefs()

    @Upsert
    suspend fun insertSongAlbumRefs(refs: List<SongAlbumCrossRef>)

    @Upsert
    suspend fun insertSongArtistRefs(refs: List<SongArtistCrossRef>)
}
