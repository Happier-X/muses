package com.muses.player.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muses.player.core.data.dao.AlbumDao
import com.muses.player.core.data.dao.ArtistDao
import com.muses.player.core.data.dao.PlaylistDao
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.dao.SourceDao

/**
 * 曲库主库。
 *
 * - v1：songs / albums / artists / song_album_cross_ref / song_artist_cross_ref / sources
 * - v2：playlists / playlist_songs（M2 阶段 2，迁移见 [MIGRATION_1_2]）
 * - v3：songs.replayGainTrackDb（M2 阶段 3 响度均衡，迁移见 [MIGRATION_2_3]）
 */
@Database(
    entities = [
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        SongAlbumCrossRef::class,
        SongArtistCrossRef::class,
        SourceEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class MusesDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun sourceDao(): SourceDao
    abstract fun playlistDao(): PlaylistDao
}
