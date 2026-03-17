package com.muses.data.model

data class User(val id: Int, val username: String)

data class SongDto(
    val id: Int,
    val title: String,
    val duration: Int,
    val artist: ArtistDto,
    val album: AlbumDto
) {
    fun getStreamUrl(baseUrl: String) = "$baseUrl/api/songs/$id/stream"
}

data class ArtistDto(val id: Int, val name: String)
data class AlbumDto(val id: Int, val title: String, val artist: ArtistDto)

data class PlaylistDto(
    val id: Int,
    val name: String,
    val songs: List<PlaylistSongDto>
)

data class PlaylistSongDto(val song: SongDto)

data class FavoriteDto(val song: SongDto)
