package com.muses.player.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.muses.player.core.data.db.AlbumEntity
import com.muses.player.core.data.db.AlbumWithSongs
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Upsert
    suspend fun insertAll(albums: List<AlbumEntity>)

    @Query("SELECT * FROM albums ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getById(id: String): AlbumEntity?

    @Transaction
    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumWithSongs(id: String): AlbumWithSongs?

    @Transaction
    @Query("SELECT * FROM albums WHERE id = :id")
    fun observeAlbumWithSongs(id: String): Flow<AlbumWithSongs?>

    @Query("DELETE FROM albums")
    suspend fun deleteAll()
}
