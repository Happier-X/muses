import type { SongItem } from './types'

export const UNKNOWN_ALBUM = '未知专辑'
export const UNKNOWN_ARTIST = '未知艺术家'

export interface AlbumSummary {
  name: string
  songCount: number
  artistSummary: string
  songs: SongItem[]
}

export interface ArtistSummary {
  name: string
  songCount: number
  albumCount: number
  songs: SongItem[]
}

const normalizeText = (value: string | undefined, fallback: string): string => {
  const normalizedValue = value?.trim()
  return normalizedValue || fallback
}

const compareText = (left: string, right: string): number => {
  return left.localeCompare(right, 'zh-Hans-CN', { numeric: true, sensitivity: 'base' })
}

export const sortSongsForDisplay = (songs: SongItem[]): SongItem[] => {
  return [...songs].sort((left, right) => {
    const titleResult = compareText(left.title, right.title)
    if (titleResult !== 0) {
      return titleResult
    }

    return compareText(left.path, right.path)
  })
}

export const formatDuration = (duration: number | undefined): string | undefined => {
  if (duration === undefined || !Number.isFinite(duration) || duration < 0) {
    return undefined
  }

  const totalSeconds = Math.floor(duration)
  const seconds = totalSeconds % 60
  const totalMinutes = Math.floor(totalSeconds / 60)
  const minutes = totalMinutes % 60
  const hours = Math.floor(totalMinutes / 60)

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  }

  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}

export const getSongArtistName = (song: SongItem): string => normalizeText(song.artist, UNKNOWN_ARTIST)

export const getSongAlbumName = (song: SongItem): string => normalizeText(song.album, UNKNOWN_ALBUM)

export const groupSongsByAlbum = (songs: SongItem[]): AlbumSummary[] => {
  const groups = new Map<string, SongItem[]>()

  for (const song of songs) {
    const albumName = getSongAlbumName(song)
    groups.set(albumName, [...(groups.get(albumName) ?? []), song])
  }

  return Array.from(groups.entries())
    .map(([name, albumSongs]) => {
      const artists = Array.from(
        new Set(albumSongs.map((song) => song.artist?.trim()).filter((artist): artist is string => Boolean(artist))),
      )

      return {
        name,
        songCount: albumSongs.length,
        artistSummary: artists.length > 0 ? artists.join('、') : UNKNOWN_ARTIST,
        songs: sortSongsForDisplay(albumSongs),
      }
    })
    .sort((left, right) => compareText(left.name, right.name))
}

export const groupSongsByArtist = (songs: SongItem[]): ArtistSummary[] => {
  const groups = new Map<string, SongItem[]>()

  for (const song of songs) {
    const artistName = getSongArtistName(song)
    groups.set(artistName, [...(groups.get(artistName) ?? []), song])
  }

  return Array.from(groups.entries())
    .map(([name, artistSongs]) => {
      const albums = new Set(artistSongs.map(getSongAlbumName))

      return {
        name,
        songCount: artistSongs.length,
        albumCount: albums.size,
        songs: sortSongsForDisplay(artistSongs),
      }
    })
    .sort((left, right) => compareText(left.name, right.name))
}
