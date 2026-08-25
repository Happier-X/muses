package com.muses.player.core.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.muses.player.core.data.db.PlaylistEntity
import com.muses.player.core.data.db.PlaylistSongEntity
import com.muses.player.core.data.db.SongEntity
import kotlinx.coroutines.flow.Flow

/** 两阶段平移用的偏移量：先把全部 position 抬离 0..n-1 区间，避免逐行 UPDATE 撞复合 PK */
private const val REORDER_OFFSET = 100_000

/** 播放列表歌曲行 + 关联歌曲详情（@Relation 按 songId 走 SQL join，songId 外键已保证存在性） */
data class PlaylistSongWithSong(
    @Embedded val ref: PlaylistSongEntity,
    @Relation(parentColumn = "songId", entityColumn = "id")
    val song: SongEntity?,
)

@Dao
interface PlaylistDao {

    // ---- 播放列表本体 ----

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observeById(id: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: String): PlaylistEntity?

    @Upsert
    suspend fun insert(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: String, name: String, updatedAt: Long)

    @Query("UPDATE playlists SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: String, now: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: String)

    /** 各歌单的有效歌曲数（songId 仍在 songs 表中的条目；Web countValidSongs 同语义） */
    @Query(
        "SELECT ps.playlistId AS playlistId, COUNT(*) AS validCount " +
            "FROM playlist_songs ps INNER JOIN songs s ON s.id = ps.songId " +
            "GROUP BY ps.playlistId",
    )
    fun observeValidCounts(): Flow<List<PlaylistValidCount>>

    // ---- 关联行读取 ----

    @Transaction
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeSongsWithSong(playlistId: String): Flow<List<PlaylistSongWithSong>>

    /**
     * position 排序策略：删除后立即紧凑重排（position 始终保持 0..n-1 连续），
     * 不采用稀疏 position；读侧无需运行时排序。
     */
    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeSongIds(playlistId: String): Flow<List<String>>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getSongIds(playlistId: String): List<String>

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: String): Int?

    // ---- 关联行写入 ----

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSong(song: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: String, songId: String)

    /** 两阶段平移第一步：整体抬升 position，避开逐行 UPDATE 撞复合 PK（仅供本 DAO 事务方法使用） */
    @Query("UPDATE playlist_songs SET position = position + $REORDER_OFFSET WHERE playlistId = :playlistId")
    suspend fun shiftAllForReorder(playlistId: String)

    /** 两阶段平移第二步：写回最终 position（同上，仅内部使用） */
    @Query(
        "UPDATE playlist_songs SET position = :position " +
            "WHERE playlistId = :playlistId AND songId = :songId",
    )
    suspend fun setSongPosition(playlistId: String, songId: String, position: Int)

    /** 追加歌曲到列表末尾（调用方负责去重与 touch） */
    @Transaction
    suspend fun appendSongs(items: List<PlaylistSongEntity>) {
        items.forEach { insertSong(it) }
    }

    /**
     * 删除单首并紧凑重排：升序把空洞后的行依次前移一位，
     * 每次写入的目标位置必然刚被腾出，不会撞复合 PK。
     */
    @Transaction
    suspend fun removeSongAndCompact(playlistId: String, songId: String) {
        val ids = getSongIds(playlistId)
        val index = ids.indexOf(songId)
        if (index < 0) return
        removeSong(playlistId, songId)
        ids.drop(index + 1).forEachIndexed { offset, id ->
            setSongPosition(playlistId, id, index + offset)
        }
    }

    /**
     * 拖动排序：单事务内两阶段更新——先整体 +offset 避开 PK 冲突，再按新顺序写回最终 position。
     */
    @Transaction
    suspend fun moveSong(playlistId: String, fromPosition: Int, toPosition: Int) {
        val ids = getSongIds(playlistId)
        if (fromPosition !in ids.indices || toPosition !in ids.indices || fromPosition == toPosition) return
        shiftAllForReorder(playlistId)
        val reordered = ids.toMutableList().apply {
            add(toPosition, removeAt(fromPosition))
        }
        reordered.forEachIndexed { index, id ->
            setSongPosition(playlistId, id, index)
        }
    }
}

/** 歌单有效歌曲数投影（countValidSongs） */
data class PlaylistValidCount(val playlistId: String, val validCount: Int)
