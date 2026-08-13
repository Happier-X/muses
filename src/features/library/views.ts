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

/** 歌曲列表排序模式（对齐椒盐音乐排序菜单可用项） */
export type SongSortMode =
  | 'custom'
  | 'title'
  | 'fileName'
  | 'artist'
  | 'album'
  | 'duration'
  | 'folder'

/** 椒盐排序菜单完整项（含 Muses 无数据字段，置灰展示用） */
export const SONG_SORT_MENU: ReadonlyArray<{ key: SongSortMode | 'size' | 'year' | 'playCount' | 'durationDesc' | 'modifiedAt' | 'addedAt'; label: string; available: boolean }> = [
  { key: 'custom', label: '自定义', available: true },
  { key: 'title', label: '标题', available: true },
  { key: 'album', label: '专辑（音轨）', available: true },
  { key: 'size', label: '大小', available: false },
  { key: 'folder', label: '文件夹（标题）', available: true },
  { key: 'fileName', label: '文件名', available: true },
  { key: 'artist', label: '艺术家（专辑）', available: true },
  { key: 'year', label: '年份', available: false },
  { key: 'playCount', label: '播放次数', available: false },
  { key: 'duration', label: '时长（短→长）', available: true },
  { key: 'durationDesc', label: '时长（长→短）', available: true },
  { key: 'modifiedAt', label: '修改时间', available: false },
  { key: 'addedAt', label: '添加时间', available: false },
]

const getFileName = (song: SongItem): string => {
  const segments = song.path.split('/')
  return segments[segments.length - 1] ?? song.path
}

const getFolderPath = (song: SongItem): string => {
  const segments = song.path.split('/')
  segments.pop()
  return segments.join('/')
}

const byTitle = (left: SongItem, right: SongItem): number => {
  const titleResult = compareText(left.title, right.title)
  if (titleResult !== 0) {
    return titleResult
  }
  return compareText(left.path, right.path)
}

const byFileName = (left: SongItem, right: SongItem): number => compareText(getFileName(left), getFileName(right))

const byArtist = (left: SongItem, right: SongItem): number => {
  const artistResult = compareText(getSongArtistName(left), getSongArtistName(right))
  if (artistResult !== 0) {
    return artistResult
  }
  return byTitle(left, right)
}

const byAlbum = (left: SongItem, right: SongItem): number => {
  const albumResult = compareText(getSongAlbumName(left), getSongAlbumName(right))
  if (albumResult !== 0) {
    return albumResult
  }
  return byTitle(left, right)
}

const byDuration = (left: SongItem, right: SongItem): number => {
  const leftDuration = left.duration ?? 0
  const rightDuration = right.duration ?? 0
  if (leftDuration !== rightDuration) {
    return leftDuration - rightDuration
  }
  return byTitle(left, right)
}

const byFolder = (left: SongItem, right: SongItem): number => {
  const folderResult = compareText(getFolderPath(left), getFolderPath(right))
  if (folderResult !== 0) {
    return folderResult
  }
  return byTitle(left, right)
}

/** 按指定模式排序；custom 保持传入顺序（浅拷贝，不原地修改） */
export const sortSongsByMode = (songs: SongItem[], mode: SongSortMode | 'durationDesc'): SongItem[] => {
  if (mode === 'custom') {
    return [...songs]
  }

  const comparator =
    mode === 'title'
      ? byTitle
      : mode === 'fileName'
        ? byFileName
        : mode === 'artist'
          ? byArtist
          : mode === 'album'
            ? byAlbum
            : mode === 'duration'
              ? byDuration
              : mode === 'durationDesc'
                ? (left: SongItem, right: SongItem) => byDuration(right, left)
                : byFolder

  return [...songs].sort(comparator)
}

export const sortSongsForDisplay = (songs: SongItem[]): SongItem[] => {
  return sortSongsByMode(songs, 'title')
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
