import type { AudioTags, SongItem } from './types'

const SONGS_STORAGE_KEY = 'muses:songs'

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
    isOptionalNumber(value.duration)
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

export const saveSongs = (songs: SongItem[]): void => {
  localStorage.setItem(SONGS_STORAGE_KEY, JSON.stringify(songs))
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
    left.duration !== right.duration
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
    artist: tags.artist,
    album: tags.album,
    duration: tags.duration,
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
