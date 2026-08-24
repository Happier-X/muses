package com.muses.player.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 歌曲（与 core:model Song 一一对应） */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["title"]),
        Index(value = ["albumTitle"]),
    ],
)
data class SongEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "sourceId") val sourceId: String,
    @ColumnInfo(name = "sourceType") val sourceType: String,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "artist") val artist: String? = null,
    /** 专辑标题冗余列：列表排序/分组用；正式关联走 song_album_cross_ref */
    @ColumnInfo(name = "albumTitle") val albumTitle: String? = null,
    @ColumnInfo(name = "durationMs") val durationMs: Long = 0L,
    @ColumnInfo(name = "durationSec") val durationSec: Long = 0L,
    @ColumnInfo(name = "coverUri") val coverUri: String? = null,
    @ColumnInfo(name = "lyrics") val lyrics: String? = null,
    @ColumnInfo(name = "tagsVersion") val tagsVersion: Int = 0,
)

/** 专辑索引 */
@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "artist") val artist: String? = null,
    @ColumnInfo(name = "year") val year: Int? = null,
    @ColumnInfo(name = "songCount") val songCount: Int = 0,
)

/** 艺术家索引 */
@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "albumCount") val albumCount: Int = 0,
    @ColumnInfo(name = "songCount") val songCount: Int = 0,
)

/** 歌曲↔专辑 多对一关联（一首歌可归属多张合辑） */
@Entity(
    tableName = "song_album_cross_ref",
    primaryKeys = ["songId", "albumId"],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["albumId"])],
)
data class SongAlbumCrossRef(
    val songId: String,
    val albumId: String,
)

/** 歌曲↔艺术家 关联（支持多艺术家曲目） */
@Entity(
    tableName = "song_artist_cross_ref",
    primaryKeys = ["songId", "artistId"],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["artistId"])],
)
data class SongArtistCrossRef(
    val songId: String,
    val artistId: String,
)

/** 音源配置（密码经 CredentialsRepository 单独加密存储，不入库明文） */
@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "url") val url: String? = null,
    @ColumnInfo(name = "path") val path: String? = null,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
)
