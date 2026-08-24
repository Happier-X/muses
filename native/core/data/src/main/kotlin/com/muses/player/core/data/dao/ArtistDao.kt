package com.muses.player.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.muses.player.core.data.db.ArtistEntity
import com.muses.player.core.data.db.ArtistWithSongs
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {

    @Upsert
    suspend fun insertAll(artists: List<ArtistEntity>)

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getById(id: String): ArtistEntity?

    @Transaction
    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getArtistWithSongs(id: String): ArtistWithSongs?

    @Transaction
    @Query("SELECT * FROM artists WHERE id = :id")
    fun observeArtistWithSongs(id: String): Flow<ArtistWithSongs?>

    @Query("DELETE FROM artists")
    suspend fun deleteAll()
}
