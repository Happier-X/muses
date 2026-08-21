/**
 * 刮削写回编排 + 回滚 journal（child3 R3-4~R3-6 / design.md §4.3）。
 *
 * 1. 写前快照旧值到回滚 journal
 * 2. 写文件（本地并行/WebDAV 串行）
 * 3. 写库（upsertSong，来源按文件结果标记 embedded/scrape）
 * 4. 逐行返回成功/失败状态
 * 5. 撤销恢复曲库旧值（文件不可逆）
 */
import { loadSongs, saveSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import { writeLocalAudioMetadata, type WriteMetadataResult } from '@/features/library/native'
import { writeWebDavAudioMetadata } from '@/features/sources/webdav'
import type { ScrapeCandidate } from './matcher'

// ── 回滚 journal ──────────────────────────────────────────

const ROLLBACK_KEY = 'muses:scrape-rollback'
const MAX_ROLLBACK_ENTRIES = 200

export interface RollbackEntry {
  songId: string
  songBefore: Pick<SongItem, 'title' | 'artist' | 'album' | 'coverUri' | 'lyrics' | 'lyricsFormat' | 'lyricsSource' | 'metaSources'>
  createdAt: string
}

export interface RollbackJournal {
  version: number
  journalId: string
  entries: RollbackEntry[]
}

const readRollbackJournal = (): RollbackJournal | null => {
  const raw = localStorage.getItem(ROLLBACK_KEY)
  if (!raw) {
    return null
  }
  try {
    const parsed: unknown = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object' || !Array.isArray((parsed as Record<string, unknown>).entries)) {
      return null
    }
    return parsed as RollbackJournal
  } catch {
    return null
  }
}

const writeRollbackJournal = (journal: RollbackJournal): void => {
  localStorage.setItem(ROLLBACK_KEY, JSON.stringify(journal))
}

const snapshotSong = (song: SongItem): RollbackEntry['songBefore'] => ({
  title: song.title,
  artist: song.artist,
  album: song.album,
  coverUri: song.coverUri,
  lyrics: song.lyrics,
  lyricsFormat: song.lyricsFormat,
  lyricsSource: song.lyricsSource,
  metaSources: song.metaSources,
})

// ── 写回状态 ──────────────────────────────────────────────

export type WritebackStatus = 'success' | 'file-failed' | 'failed'

export interface WritebackResult {
  songId: string
  status: WritebackStatus
  /** 文件写入结果（成功/失败） */
  fileResult: WriteMetadataResult
  /** 库是否更新 */
  libraryUpdated: boolean
  /** 错误信息 */
  error?: string
}

// ── 写回变更 ──────────────────────────────────────────────

export interface ScrapeChanges {
  title?: string
  artist?: string
  album?: string
  coverUri?: string
  coverRemoteUrl?: string
  lyrics?: string
  lyricsFormat?: string
}

// ── 写文件辅助 ────────────────────────────────────────────

/** 缓存远程封面到本地 file:// */
const ensureLocalCover = async (
  remoteUrl: string | undefined,
): Promise<string | undefined> => {
  if (!remoteUrl) {
    return undefined
  }
  try {
    const { cacheRemoteCover } = await import('@/features/player/native')
    const uri = await cacheRemoteCover({ url: remoteUrl, cacheKey: `scrape-cover-${remoteUrl}` })
    return uri ?? undefined
  } catch {
    return undefined
  }
}

/** 写本地音频文件 */
const writeLocalFile = async (
  song: SongItem,
  changes: ScrapeChanges,
): Promise<WriteMetadataResult> => {
  const coverPath = changes.coverRemoteUrl
    ? await ensureLocalCover(changes.coverRemoteUrl)
    : undefined

  return writeLocalAudioMetadata({
    uri: song.uri,
    title: changes.title,
    artist: changes.artist,
    album: changes.album,
    lyrics: changes.lyrics,
    clearLyrics: changes.lyrics === '' ? true : undefined,
    coverPath: coverPath ?? undefined,
    clearCover: changes.coverUri === '' ? true : undefined,
  })
}

/** 写 WebDAV 文件（需要 serverUrl/username/credentialKey；从 sources storage 取） */
const writeWebDavFile = async (
  song: SongItem,
  changes: ScrapeChanges,
): Promise<WriteMetadataResult> => {
  const coverPath = changes.coverRemoteUrl
    ? await ensureLocalCover(changes.coverRemoteUrl)
    : undefined

  const { loadSources, getWebDavPassword } = await import('@/features/sources/storage')
  const sources = loadSources()
  const webdavSource = sources.find(
    (s): s is Extract<typeof s, { type: 'webdav' }> => s.type === 'webdav',
  )
  if (!webdavSource?.credentialKey || !webdavSource?.serverUrl) {
    return { ok: false, code: 'no_password', message: 'WebDAV 配置不完整（缺 url 或 credentialKey）。' }
  }

  const password = await getWebDavPassword(webdavSource.credentialKey)
  if (!password) {
    return { ok: false, code: 'no_password', message: 'WebDAV 密码未配置。' }
  }

  return writeWebDavAudioMetadata({
    url: webdavSource.serverUrl,
    username: webdavSource.username,
    password,
    title: changes.title,
    artist: changes.artist,
    album: changes.album,
    lyrics: changes.lyrics,
    clearLyrics: changes.lyrics === '' ? true : undefined,
    coverPath: coverPath ?? undefined,
    clearCover: changes.coverUri === '' ? true : undefined,
  })
}

/** 根据 sourceType 选择写文件方式 */
const writeFile = async (
  song: SongItem,
  changes: ScrapeChanges,
): Promise<WriteMetadataResult> => {
  if (song.sourceType === 'webdav') {
    return writeWebDavFile(song, changes)
  }
  return writeLocalFile(song, changes)
}

// ── 写库辅助 ──────────────────────────────────────────────

const updateSongInLibrary = (
  songId: string,
  changes: ScrapeChanges,
  fileOk: boolean,
): void => {
  const songs = loadSongs()
  const index = songs.findIndex((s) => s.id === songId)
  if (index < 0) {
    return
  }
  const song = songs[index]
  const metaSources = { ...song.metaSources }
  // 文件写入成功 → embedded（已入文件）；失败 → scrape（仅库内展示，值得重刮）
  const fieldSource = fileOk ? 'embedded' : 'scrape'
  if (changes.title !== undefined) {
    metaSources.title = fieldSource
  }
  if (changes.artist !== undefined) {
    metaSources.artist = fieldSource
  }
  if (changes.album !== undefined) {
    metaSources.album = fieldSource
  }
  if (changes.coverUri !== undefined) {
    metaSources.cover = fieldSource
  }

  songs[index] = {
    ...song,
    title: changes.title ?? song.title,
    artist: changes.artist ?? song.artist,
    album: changes.album ?? song.album,
    coverUri: changes.coverUri !== undefined ? (changes.coverUri || undefined) : song.coverUri,
    lyrics: changes.lyrics !== undefined ? changes.lyrics : song.lyrics,
    lyricsFormat: changes.lyricsFormat !== undefined ? (changes.lyricsFormat as SongItem['lyricsFormat']) : song.lyricsFormat,
    lyricsSource: changes.lyrics !== undefined ? fieldSource : song.lyricsSource,
    metaSources,
    updatedAt: new Date().toISOString(),
  }
  saveSongs(songs)
}

// ── 公开 API ──────────────────────────────────────────────

/**
 * 批量写回：逐曲独立结果。
 * 写前自动快照到回滚 journal；返回 journalId 用于撤销。
 */
export const applyScrapeChanges = async (
  candidates: ScrapeCandidate[],
  checkedIds: Set<string>,
  changesMap: Map<string, ScrapeChanges>,
): Promise<{ journalId: string; results: WritebackResult[] }> => {
  const songs = loadSongs()
  const songMap = new Map(songs.map((s) => [s.id, s]))

  // 1. 写前快照
  const journalId = `journal-${Date.now()}`
  const entries: RollbackEntry[] = []
  for (const candidate of candidates) {
    if (!checkedIds.has(candidate.songId)) {
      continue
    }
    const song = songMap.get(candidate.songId)
    if (!song) {
      continue
    }
    entries.push({
      songId: candidate.songId,
      songBefore: snapshotSong(song),
      createdAt: new Date().toISOString(),
    })
  }

  // 截断到上限
  const trimmedEntries = entries.slice(-MAX_ROLLBACK_ENTRIES)
  writeRollbackJournal({
    version: 1,
    journalId,
    entries: trimmedEntries,
  })

  // 2. 逐曲写回（本地可并行，WebDAV 串行）
  const results: WritebackResult[] = []
  const webdavQueue: ScrapeCandidate[] = []
  const localQueue: ScrapeCandidate[] = []

  for (const candidate of candidates) {
    if (!checkedIds.has(candidate.songId)) {
      continue
    }
    if (candidate.song.sourceType === 'webdav') {
      webdavQueue.push(candidate)
    } else {
      localQueue.push(candidate)
    }
  }

  // 本地并行
  const localResults = await Promise.all(
    localQueue.map(async (candidate) => {
      const changes = changesMap.get(candidate.songId) ?? {}
      try {
        const fileResult = await writeFile(candidate.song, changes)
        updateSongInLibrary(candidate.songId, changes, fileResult.ok)
        return {
          songId: candidate.songId,
          status: fileResult.ok ? 'success' as const : 'file-failed' as const,
          fileResult,
          libraryUpdated: true,
        }
      } catch (error) {
        return {
          songId: candidate.songId,
          status: 'failed' as const,
          fileResult: { ok: false, code: 'unknown', message: error instanceof Error ? error.message : '写回失败' },
          libraryUpdated: false,
          error: error instanceof Error ? error.message : '写回失败',
        }
      }
    }),
  )
  results.push(...localResults)

  // WebDAV 串行
  for (const candidate of webdavQueue) {
    const changes = changesMap.get(candidate.songId) ?? {}
    try {
      const fileResult = await writeFile(candidate.song, changes)
      updateSongInLibrary(candidate.songId, changes, fileResult.ok)
      results.push({
        songId: candidate.songId,
        status: fileResult.ok ? 'success' : 'file-failed',
        fileResult,
        libraryUpdated: true,
      })
    } catch (error) {
      results.push({
        songId: candidate.songId,
        status: 'failed',
        fileResult: { ok: false, code: 'unknown', message: error instanceof Error ? error.message : '写回失败' },
        libraryUpdated: false,
        error: error instanceof Error ? error.message : '写回失败',
      })
    }
  }

  return { journalId, results }
}

/**
 * 撤销：恢复曲库旧值（文件不可逆，UI 明示）。
 */
export const revertScrapeJournal = (journalId: string): { reverted: number } => {
  const journal = readRollbackJournal()
  if (!journal || journal.journalId !== journalId) {
    return { reverted: 0 }
  }

  const songs = loadSongs()
  const songMap = new Map(songs.map((s) => [s.id, s]))
  let reverted = 0

  for (const entry of journal.entries) {
    const song = songMap.get(entry.songId)
    if (!song) {
      continue
    }
    reverted += 1
    Object.assign(song, {
      title: entry.songBefore.title,
      artist: entry.songBefore.artist,
      album: entry.songBefore.album,
      coverUri: entry.songBefore.coverUri,
      lyrics: entry.songBefore.lyrics,
      lyricsFormat: entry.songBefore.lyricsFormat,
      lyricsSource: entry.songBefore.lyricsSource,
      metaSources: entry.songBefore.metaSources,
      updatedAt: new Date().toISOString(),
    })
  }

  if (reverted > 0) {
    saveSongs([...songs])
  }

  localStorage.removeItem(ROLLBACK_KEY)
  return { reverted }
}

/** 读取当前回滚 journal（UI 显示用） */
export const getCurrentRollbackJournal = (): RollbackJournal | null => {
  return readRollbackJournal()
}
