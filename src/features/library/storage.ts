import type { AudioTags, LyricsSource, SongItem } from './types'

const SONGS_STORAGE_KEY = 'muses:songs'
export const SONGS_UPDATED_EVENT = 'muses:songs-updated'
export const CURRENT_METADATA_VERSION = 2

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const isString = (value: unknown): value is string => {
  return typeof value === 'string' && value.length > 0
}

const isOptionalString = (value: unknown): value is string | undefined => {
  return value === undefined || typeof value === 'string'
}

const isOptionalNumber = (value: unknown): value is number | undefined => {
  return value === undefined || (typeof value === 'number' && Number.isFinite(value))
}

const isOptionalBoolean = (value: unknown): value is boolean | undefined => {
  return value === undefined || typeof value === 'boolean'
}

const isOptionalLyricsSource = (value: unknown): value is LyricsSource | undefined => {
  return value === undefined || value === 'embedded' || value === 'sidecar'
}

const isSafeCoverUri = (value: string | undefined): boolean => {
  return value === undefined || !value.startsWith('data:')
}

const isSongItem = (value: unknown): value is SongItem => {
  if (!isRecord(value)) {
    return false
  }

  return (
    isString(value.id) &&
    isString(value.sourceId) &&
    (value.sourceType === 'local' || value.sourceType === 'webdav') &&
    isString(value.path) &&
    isString(value.uri) &&
    isString(value.title) &&
    isString(value.createdAt) &&
    isString(value.updatedAt) &&
    isOptionalString(value.artist) &&
    isOptionalString(value.album) &&
    isOptionalNumber(value.duration) &&
    isOptionalString(value.lyrics) &&
    isOptionalLyricsSource(value.lyricsSource) &&
    isOptionalString(value.coverUri) &&
    isSafeCoverUri(value.coverUri) &&
    isOptionalBoolean(value.tagsScanned) &&
    isOptionalString(value.tagsScannedAt) &&
    isOptionalNumber(value.metadataVersion)
  )
}

const encodeStablePart = (value: string): string => {
  return encodeURIComponent(value).replace(/%/g, '_')
}

export const createSongId = (sourceId: string, path: string): string => {
  return `song:${encodeStablePart(sourceId)}:${encodeStablePart(path)}`
}

export const loadSongs = (): SongItem[] => {
  const rawValue = localStorage.getItem(SONGS_STORAGE_KEY)
  if (!rawValue) {
    return []
  }

  try {
    const parsedValue: unknown = JSON.parse(rawValue)
    if (!Array.isArray(parsedValue)) {
      return []
    }

    return parsedValue.filter(isSongItem)
  } catch {
    return []
  }
}

const sanitizeSongForStorage = (song: SongItem): SongItem => {
  const { coverUri, ...rest } = song
  return {
    ...rest,
    ...(coverUri && !coverUri.startsWith('data:') ? { coverUri } : {}),
  }
}

const notifySongsUpdated = (): void => {
  if (typeof window === 'undefined') {
    return
  }

  window.dispatchEvent(new CustomEvent(SONGS_UPDATED_EVENT))
}

export const saveSongs = (songs: SongItem[]): void => {
  localStorage.setItem(SONGS_STORAGE_KEY, JSON.stringify(songs.map(sanitizeSongForStorage)))
  notifySongsUpdated()
}

export type UpsertSongStatus = 'inserted' | 'updated' | 'skipped'

export interface UpsertSongInput {
  sourceId: string
  sourceType: SongItem['sourceType']
  path: string
  uri: string
  title: string
  tags?: AudioTags
  now?: string
}

export interface UpsertSongResult {
  status: UpsertSongStatus
  song: SongItem
  songs: SongItem[]
}

const hasSongChanged = (left: SongItem, right: SongItem): boolean => {
  return (
    left.uri !== right.uri ||
    left.title !== right.title ||
    left.artist !== right.artist ||
    left.album !== right.album ||
    left.duration !== right.duration ||
    left.lyrics !== right.lyrics ||
    left.lyricsSource !== right.lyricsSource ||
    left.coverUri !== right.coverUri ||
    left.tagsScanned !== right.tagsScanned ||
    left.tagsScannedAt !== right.tagsScannedAt ||
    left.metadataVersion !== right.metadataVersion
  )
}

export const upsertSong = (input: UpsertSongInput, existingSongs = loadSongs()): UpsertSongResult => {
  const now = input.now ?? new Date().toISOString()
  const existingIndex = existingSongs.findIndex((song) => song.sourceId === input.sourceId && song.path === input.path)
  const tags = input.tags ?? {}

  if (existingIndex < 0) {
    const song: SongItem = {
      id: createSongId(input.sourceId, input.path),
      sourceId: input.sourceId,
      sourceType: input.sourceType,
      path: input.path,
      uri: input.uri,
      title: tags.title?.trim() || input.title,
      artist: tags.artist,
      album: tags.album,
      duration: tags.duration,
      lyrics: tags.lyrics,
      lyricsSource: tags.lyricsSource,
      coverUri: tags.coverUri && !tags.coverUri.startsWith('data:') ? tags.coverUri : undefined,
      tagsScanned: tags.tagsScanned,
      tagsScannedAt: tags.tagsScannedAt,
      metadataVersion: tags.metadataVersion,
      createdAt: now,
      updatedAt: now,
    }
    const songs = [song, ...existingSongs]
    saveSongs(songs)
    return { status: 'inserted', song, songs }
  }

  const previousSong = existingSongs[existingIndex]
  const nextSong: SongItem = {
    ...previousSong,
    uri: input.uri,
    title: tags.title?.trim() || input.title,
    artist: tags.artist ?? previousSong.artist,
    album: tags.album ?? previousSong.album,
    duration: tags.duration ?? previousSong.duration,
    lyrics: tags.lyrics ?? previousSong.lyrics,
    lyricsSource: tags.lyricsSource ?? previousSong.lyricsSource,
    coverUri: tags.coverUri === undefined
      ? previousSong.coverUri
      : tags.coverUri && !tags.coverUri.startsWith('data:') ? tags.coverUri : undefined,
    tagsScanned: tags.tagsScanned ?? previousSong.tagsScanned,
    tagsScannedAt: tags.tagsScannedAt ?? previousSong.tagsScannedAt,
    metadataVersion: tags.metadataVersion ?? previousSong.metadataVersion,
    updatedAt: now,
  }

  if (!hasSongChanged(previousSong, nextSong)) {
    return { status: 'skipped', song: previousSong, songs: existingSongs }
  }

  const songs = [...existingSongs]
  songs[existingIndex] = nextSong
  saveSongs(songs)
  return { status: 'updated', song: nextSong, songs }
}
