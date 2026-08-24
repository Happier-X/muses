package com.muses.player.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 播放列表（M2 阶段 2）。
 * id 使用随机 UUID，与歌曲的路径哈希稳定 ID 体系无关。
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
)

/**
 * 播放列表 ↔ 歌曲 关联行。
 *
 * - 复合 PK (playlistId, position)：position 即排序键；
 * - playlistId 外键 CASCADE：删除播放列表时清理全部关联行；
 * - songId 外键 CASCADE：SongEntity 已在 v1 落地，按 design §3.3 直接建外键，
 *   删除曲库歌曲时同步失效清理（对齐 653e466 的语义），不再延后补 FK。
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["songId"])],
)
data class PlaylistSongEntity(
    val playlistId: String,
    val songId: String,
    val position: Int,
)
