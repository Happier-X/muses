package com.muses.player.core.data.mapper

import com.muses.player.core.data.db.AlbumEntity
import com.muses.player.core.data.db.ArtistEntity
import com.muses.player.core.data.db.PlaylistEntity
import com.muses.player.core.data.db.SongEntity
import com.muses.player.core.data.db.SourceEntity
import com.muses.player.core.model.Album
import com.muses.player.core.model.Artist
import com.muses.player.core.model.Playlist
import com.muses.player.core.model.Song
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.scrape.LyricsFormat
import com.muses.player.core.model.scrape.LyricsSource
import com.muses.player.core.model.scrape.MetaFieldSource
import com.muses.player.core.model.scrape.MetaSources

/** Entity ↔ domain 映射：feature:* 只见 domain model，不接触 Room 类型 */

/** metaSources 平铺列 ↔ 对象；wire 值宽松解析（未知值回退 null，与 Web isRecord 风格一致） */
private fun lyricsFormatOf(raw: String?): LyricsFormat? =
    raw?.let { f -> LyricsFormat.entries.firstOrNull { it.wire == f } }

private fun lyricsSourceOf(raw: String?): LyricsSource? =
    raw?.let { s -> LyricsSource.entries.firstOrNull { it.wire == s } }

private fun fieldSourceOf(raw: String?): MetaFieldSource? =
    raw?.let { s -> MetaFieldSource.entries.firstOrNull { it.wire == s } }

fun SongEntity.toDomain(): Song = Song(
    id = id,
    sourceId = sourceId,
    path = path,
    title = title,
    artist = artist,
    album = albumTitle,
    durationMs = durationMs,
    durationSec = durationSec,
    coverUri = coverUri,
    lyrics = lyrics,
    lyricsFormat = lyricsFormatOf(lyricsFormat),
    lyricsSource = lyricsSourceOf(lyricsSource),
    metaSources = MetaSources(
        title = fieldSourceOf(metaTitle),
        artist = fieldSourceOf(metaArtist),
        album = fieldSourceOf(metaAlbum),
        cover = fieldSourceOf(metaCover),
    ).takeIf { it.title != null || it.artist != null || it.album != null || it.cover != null },
    replayGainTrackDb = replayGainTrackDb,
    sourceType = runCatching { SourceType.valueOf(sourceType) }.getOrDefault(SourceType.LOCAL),
    tagsVersion = tagsVersion,
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    sourceId = sourceId,
    sourceType = sourceType.name,
    path = path,
    title = title,
    artist = artist,
    albumTitle = album,
    durationMs = durationMs,
    durationSec = durationSec,
    coverUri = coverUri,
    lyrics = lyrics,
    lyricsFormat = lyricsFormat?.wire,
    lyricsSource = lyricsSource?.wire,
    metaTitle = metaSources?.title?.wire,
    metaArtist = metaSources?.artist?.wire,
    metaAlbum = metaSources?.album?.wire,
    metaCover = metaSources?.cover?.wire,
    replayGainTrackDb = replayGainTrackDb,
    tagsVersion = tagsVersion,
)

fun AlbumEntity.toDomain(): Album = Album(
    id = id,
    title = title,
    artist = artist,
    year = year,
    songCount = songCount,
)

fun ArtistEntity.toDomain(): Artist = Artist(
    id = id,
    name = name,
    albumCount = albumCount,
    songCount = songCount,
)

fun PlaylistEntity.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SourceEntity.toDomain(): Source = Source(
    id = id,
    name = name,
    type = runCatching { SourceType.valueOf(type) }.getOrDefault(SourceType.LOCAL),
    url = url,
    path = path,
    username = username,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Source.toEntity(): SourceEntity = SourceEntity(
    id = id,
    name = name,
    type = type.name,
    url = url,
    path = path,
    username = username,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
