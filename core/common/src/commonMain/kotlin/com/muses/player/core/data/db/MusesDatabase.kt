package com.muses.player.core.data.db

import androidx.room.ConstructedBy
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
 * - v3：songs.replayGainTrackDb（M2 阶段 3 响度均衡，已在 v6 移除，迁移见 [MIGRATION_2_3] / [MIGRATION_5_6]）
 * - v4：songs 刮削写回列 lyricsFormat/lyricsSource/meta*（M3，迁移见 [MIGRATION_3_4]）
 * - v5：sources.username（WebDAV 登录名，迁移见 [MIGRATION_4_5]）
 * - v6：移除 songs.replayGainTrackDb（移除音量均衡，迁移见 [MIGRATION_5_6]）
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
    version = 6,
    exportSchema = true,
)
@ConstructedBy(MusesDatabaseConstructor::class)
abstract class MusesDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun sourceDao(): SourceDao
    abstract fun playlistDao(): PlaylistDao
}
