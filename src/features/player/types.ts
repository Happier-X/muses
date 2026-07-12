import type { SongItem } from '@/features/library/types'

export type PlaybackStatus = 'idle' | 'loading' | 'playing' | 'paused' | 'stopped' | 'finished' | 'error'

export type PlayerSourceType = 'local' | 'webdav'

export type PlayerMetadataStatus = 'idle' | 'scanning' | 'ready' | 'failed'

export interface PlayerSongSnapshot {
  id: string
  sourceId: string
  sourceType: PlayerSourceType
  title: string
  artist?: string
  album?: string
  duration?: number
  lyrics?: string
  lyricsSource?: SongItem['lyricsSource']
  coverUri?: string
}

export interface PlayerState {
  status: PlaybackStatus
  currentSong: PlayerSongSnapshot | null
  errorMessage: string | null
  position: number
  duration: number
  /**
   * 已缓冲终点（秒）。
   * - null：缓冲未知，不画缓冲条，seek 退化为 duration clamp
   * - 有值：可 seek 上限 = min(duration, bufferedPosition)
   */
  bufferedPosition: number | null
  lyrics: string | null
  coverUri: string | null
  metadataStatus: PlayerMetadataStatus
}

export interface LocalPlayOptions {
  sourceType: 'local'
  songId: string
  uri: string
  title: string
  artist?: string
  album?: string
  coverUri?: string
  /** 已知时长（秒），供缓冲换算兜底 */
  duration?: number
}

export interface WebDavPlayOptions {
  sourceType: 'webdav'
  songId: string
  url: string
  username: string
  password: string
  title: string
  artist?: string
  album?: string
  coverUri?: string
  /** 已知时长（秒），渐进下载进度换算 bufferedPosition 用 */
  duration?: number
}

export type PlayOptions = LocalPlayOptions | WebDavPlayOptions

export interface AudioPlayerNativeState {
  status: PlaybackStatus
  currentSongId?: string
  errorMessage?: string
  position?: number
  duration?: number
  bufferedPosition?: number
}

export interface SeekOptions {
  position: number
}

export const toSafeCoverUri = (coverUri?: string): string | undefined => {
  if (!coverUri) {
    return undefined
  }

  const normalized = coverUri.trim().toLowerCase()
  if (normalized.startsWith('data:') || normalized.includes(';base64,')) {
    return undefined
  }

  return coverUri
}

export const createPlayerSongSnapshot = (song: SongItem): PlayerSongSnapshot => ({
  id: song.id,
  sourceId: song.sourceId,
  sourceType: song.sourceType,
  title: song.title,
  artist: song.artist,
  album: song.album,
  duration: song.duration,
  lyrics: song.lyrics,
  lyricsSource: song.lyricsSource,
  coverUri: toSafeCoverUri(song.coverUri),
})
