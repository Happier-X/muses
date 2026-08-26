package com.muses.player.core.model

/**
 * 播放列表（M2 阶段 2）。
 * id 为随机 UUID；songs 顺序即播放顺序（position 升序）。
 */
data class Playlist(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 播放列表详情：列表元信息 + 按播放顺序排列的歌曲 */
data class PlaylistWithSongs(
    val playlist: Playlist,
    val songs: List<Song>,
)
