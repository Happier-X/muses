package com.muses.player.core.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** 专辑 + 关联歌曲（@Relation + Junction 走 song_album_cross_ref） */
data class AlbumWithSongs(
    @Embedded val album: AlbumEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SongAlbumCrossRef::class,
            parentColumn = "albumId",
            entityColumn = "songId",
        ),
    )
    val songs: List<SongEntity>,
)

/** 艺术家 + 关联歌曲 */
data class ArtistWithSongs(
    @Embedded val artist: ArtistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SongArtistCrossRef::class,
            parentColumn = "artistId",
            entityColumn = "songId",
        ),
    )
    val songs: List<SongEntity>,
)
