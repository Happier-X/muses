import type { SongItem } from '@/features/library/types'

export type PlaybackStatus = 'idle' | 'loading' | 'playing' | 'paused' | 'stopped' | 'error'

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
  coverUri: song.coverUri,
})
