import type { SongItem } from '@/features/library/types'
import type { Playlist } from './types'

const PLAYLISTS_STORAGE_KEY = 'muses:playlists'
export const PLAYLISTS_UPDATED_EVENT = 'muses:playlists-updated'

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const isString = (value: unknown): value is string => {
  return typeof value === 'string' && value.length > 0
}

const isPlaylist = (value: unknown): value is Playlist => {
  if (!isRecord(value)) {
    return false
  }
  if (!isString(value.id) || !isString(value.name) || !isString(value.createdAt) || !isString(value.updatedAt)) {
    return false
  }
  if (!Array.isArray(value.songIds)) {
    return false
  }
  return value.songIds.every((id) => typeof id === 'string' && id.length > 0)
}

const notifyPlaylistsUpdated = (): void => {
  if (typeof window === 'undefined') {
    return
  }
  window.dispatchEvent(new CustomEvent(PLAYLISTS_UPDATED_EVENT))
}

export const createPlaylistId = (): string => {
  return `pl-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`
}

export const loadPlaylists = (): Playlist[] => {
  const rawValue = localStorage.getItem(PLAYLISTS_STORAGE_KEY)
  if (!rawValue) {
    return []
  }

  try {
    const parsedValue: unknown = JSON.parse(rawValue)
    if (!Array.isArray(parsedValue)) {
      return []
    }
    return parsedValue.filter(isPlaylist).map((playlist) => ({
      ...playlist,
      name: playlist.name.trim() || playlist.name,
      songIds: [...new Set(playlist.songIds)],
    }))
  } catch {
    return []
  }
}

const savePlaylists = (playlists: Playlist[]): void => {
  localStorage.setItem(PLAYLISTS_STORAGE_KEY, JSON.stringify(playlists))
  notifyPlaylistsUpdated()
}

export const getPlaylist = (id: string): Playlist | undefined => {
  return loadPlaylists().find((playlist) => playlist.id === id)
}

export const createPlaylist = (name: string): Playlist | null => {
  const trimmed = name.trim()
  if (!trimmed) {
    return null
  }

  const now = new Date().toISOString()
  const playlist: Playlist = {
    id: createPlaylistId(),
    name: trimmed,
    songIds: [],
    createdAt: now,
    updatedAt: now,
  }

  const playlists = loadPlaylists()
  playlists.push(playlist)
  savePlaylists(playlists)
  return playlist
}

export const renamePlaylist = (id: string, name: string): boolean => {
  const trimmed = name.trim()
  if (!trimmed) {
    return false
  }

  const playlists = loadPlaylists()
  const index = playlists.findIndex((playlist) => playlist.id === id)
  if (index < 0) {
    return false
  }

  playlists[index] = {
    ...playlists[index],
    name: trimmed,
    updatedAt: new Date().toISOString(),
  }
  savePlaylists(playlists)
  return true
}

export const deletePlaylist = (id: string): boolean => {
  const playlists = loadPlaylists()
  const next = playlists.filter((playlist) => playlist.id !== id)
  if (next.length === playlists.length) {
    return false
  }
  savePlaylists(next)
  return true
}

/** 已存在则 true 且不改序；不存在则追加 */
export const addSongToPlaylist = (playlistId: string, songId: string): boolean => {
  if (!songId.trim()) {
    return false
  }

  const playlists = loadPlaylists()
  const index = playlists.findIndex((playlist) => playlist.id === playlistId)
  if (index < 0) {
    return false
  }

  const current = playlists[index]
  if (current.songIds.includes(songId)) {
    return true
  }

  playlists[index] = {
    ...current,
    songIds: [...current.songIds, songId],
    updatedAt: new Date().toISOString(),
  }
  savePlaylists(playlists)
  return true
}

export const removeSongFromPlaylist = (playlistId: string, songId: string): boolean => {
  const playlists = loadPlaylists()
  const index = playlists.findIndex((playlist) => playlist.id === playlistId)
  if (index < 0) {
    return false
  }

  const current = playlists[index]
  if (!current.songIds.includes(songId)) {
    return false
  }

  playlists[index] = {
    ...current,
    songIds: current.songIds.filter((id) => id !== songId),
    updatedAt: new Date().toISOString(),
  }
  savePlaylists(playlists)
  return true
}

/** 按 songIds 顺序解析仍存在的曲目 */
export const resolvePlaylistSongs = (playlist: Playlist, songs: SongItem[]): SongItem[] => {
  const byId = new Map(songs.map((song) => [song.id, song]))
  const result: SongItem[] = []
  for (const id of playlist.songIds) {
    const song = byId.get(id)
    if (song) {
      result.push(song)
    }
  }
  return result
}

export const countValidSongs = (playlist: Playlist, songs: SongItem[]): number => {
  return resolvePlaylistSongs(playlist, songs).length
}
