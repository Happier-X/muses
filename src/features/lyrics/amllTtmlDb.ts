import { findBestMatch } from './score'
import { normalizeText } from './normalize'
import { getBoundedCache, setBoundedCache } from '@/features/runtime/boundedCache'
import type { AmllIndexEntry, AmllMatchQuery, AmllMatchResult, AmllRawIndexLine } from './types'

const INDEX_URL = 'https://cdn.jsdelivr.net/gh/amll-dev/amll-ttml-db@main/metadata/raw-lyrics-index.jsonl'
const TTML_BASE_URL = 'https://cdn.jsdelivr.net/gh/amll-dev/amll-ttml-db@main/raw-lyrics/'

const INDEX_TIMEOUT_MS = 20_000
const TTML_TIMEOUT_MS = 12_000

/** 负缓存时长：避免同曲狂刷 CDN */
const NEGATIVE_CACHE_TTL_MS = 5 * 60_000
const SONG_CACHE_MAX_SIZE = 256

interface TtmlCacheEntry {
  queryKey: string
  ttml: string
  rawLyricFile: string
  score: number
}

interface NegativeCacheEntry {
  queryKey: string
  reason: 'no-match' | 'network' | 'parse'
  expiresAt: number
}

interface AmllSearchIndex {
  entries: AmllIndexEntry[]
  exactTitles: Map<string, number[]>
  titleTrigrams: Map<string, number[]>
}

let indexCache: AmllIndexEntry[] | null = null
let searchIndexCache: AmllSearchIndex | null = null
let indexPromise: Promise<AmllIndexEntry[]> | null = null

const ttmlBySongId = new Map<string, TtmlCacheEntry>()
const negativeBySongId = new Map<string, NegativeCacheEntry>()

/** 测试/回滚用：清空全部运行时缓存 */
export const resetAmllTtmlDbCache = (): void => {
  indexCache = null
  searchIndexCache = null
  indexPromise = null
  ttmlBySongId.clear()
  negativeBySongId.clear()
}

const fetchWithTimeout = async (url: string, timeoutMs: number): Promise<Response> => {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetch(url, { signal: controller.signal })
  } finally {
    clearTimeout(timer)
  }
}

const readMetadataValues = (metadata: AmllRawIndexLine['metadata'], key: string): string[] => {
  if (!Array.isArray(metadata)) {
    return []
  }

  for (const item of metadata) {
    if (!Array.isArray(item) || item.length < 2) {
      continue
    }
    const [metaKey, values] = item as [unknown, unknown]
    if (metaKey !== key) {
      continue
    }
    if (!Array.isArray(values)) {
      return []
    }
    return values.filter((value): value is string => typeof value === 'string' && value.trim().length > 0)
  }

  return []
}

/** 解析 jsonl 一行；字段缺失则跳过 */
export const parseIndexLine = (line: string): AmllIndexEntry | null => {
  const trimmed = line.trim()
  if (!trimmed) {
    return null
  }

  try {
    const raw = JSON.parse(trimmed) as AmllRawIndexLine
    const rawLyricFile = typeof raw.rawLyricFile === 'string' ? raw.rawLyricFile.trim() : ''
    const musicNames = readMetadataValues(raw.metadata, 'musicName')
    const musicName = musicNames[0]?.trim() || ''
    if (!rawLyricFile || !musicName) {
      return null
    }

    const artists = readMetadataValues(raw.metadata, 'artists')
    const albums = readMetadataValues(raw.metadata, 'album')

    return {
      musicName,
      artists,
      album: albums[0],
      rawLyricFile,
    }
  } catch {
    return null
  }
}

export const parseIndexJsonl = (text: string): AmllIndexEntry[] => {
  const entries: AmllIndexEntry[] = []
  for (const line of text.split(/\r?\n/)) {
    const entry = parseIndexLine(line)
    if (entry) {
      entries.push(entry)
    }
  }
  return entries
}

/** 分片解析大索引，定期让出事件循环，避免首次匹配阻塞 UI。 */
export const parseIndexJsonlChunked = async (text: string, chunkSize = 128): Promise<AmllIndexEntry[]> => {
  const entries: AmllIndexEntry[] = []
  const lines = text.split(/\r?\n/)
  for (let offset = 0; offset < lines.length; offset += chunkSize) {
    for (const line of lines.slice(offset, offset + chunkSize)) {
      const entry = parseIndexLine(line)
      if (entry) {
        entries.push(entry)
      }
    }
    if (offset + chunkSize < lines.length) {
      await new Promise<void>((resolve) => setTimeout(resolve, 0))
    }
  }
  return entries
}

const addToBucket = (bucket: Map<string, number[]>, key: string, index: number): void => {
  const values = bucket.get(key)
  if (values) {
    values.push(index)
  } else {
    bucket.set(key, [index])
  }
}

const createSearchIndex = (entries: AmllIndexEntry[]): AmllSearchIndex => {
  const exactTitles = new Map<string, number[]>()
  const titleTrigrams = new Map<string, number[]>()
  entries.forEach((entry, index) => {
    const title = normalizeText(entry.musicName)
    if (!title) return
    addToBucket(exactTitles, title, index)
    if (title.length < 3) {
      addToBucket(titleTrigrams, `short:${title}`, index)
    } else {
      for (let offset = 0; offset <= title.length - 3; offset += 1) {
        addToBucket(titleTrigrams, title.slice(offset, offset + 3), index)
      }
    }
  })
  return { entries, exactTitles, titleTrigrams }
}

const selectCandidates = (searchIndex: AmllSearchIndex, title: string): AmllIndexEntry[] => {
  const normalizedTitle = normalizeText(title)
  const candidateIndexes = new Set<number>(searchIndex.exactTitles.get(normalizedTitle) ?? [])
  if (normalizedTitle.length >= 3) {
    for (let offset = 0; offset <= normalizedTitle.length - 3; offset += 1) {
      for (const candidateIndex of searchIndex.titleTrigrams.get(normalizedTitle.slice(offset, offset + 3)) ?? []) {
        candidateIndexes.add(candidateIndex)
      }
    }
    for (const [key, indexes] of searchIndex.titleTrigrams) {
      if (key.startsWith('short:') && normalizedTitle.includes(key.slice(6))) {
        indexes.forEach((candidateIndex) => candidateIndexes.add(candidateIndex))
      }
    }
  } else if (normalizedTitle) {
    for (const candidateIndex of searchIndex.titleTrigrams.get(`short:${normalizedTitle}`) ?? []) {
      candidateIndexes.add(candidateIndex)
    }
  }
  return [...candidateIndexes].map((candidateIndex) => searchIndex.entries[candidateIndex])
}

const loadIndexFromNetwork = async (): Promise<AmllIndexEntry[]> => {
  try {
    const response = await fetchWithTimeout(INDEX_URL, INDEX_TIMEOUT_MS)
    if (!response.ok) {
      throw new Error(`index http ${response.status}`)
    }
    const text = await response.text()
    return parseIndexJsonlChunked(text)
  } catch {
    throw new Error('network')
  }
}

/** 确保索引已加载；进程内单例缓存，并发只拉一次 */
export const ensureIndex = async (): Promise<AmllIndexEntry[]> => {
  if (indexCache) {
    return indexCache
  }
  if (indexPromise) {
    return indexPromise
  }

  indexPromise = loadIndexFromNetwork()
    .then((entries) => {
      indexCache = entries
      searchIndexCache = createSearchIndex(entries)
      return entries
    })
    .catch((error) => {
      indexPromise = null
      throw error
    })

  return indexPromise
}

const isSafeRawLyricFile = (rawLyricFile: string): boolean => {
  return rawLyricFile.length > 0
    && !rawLyricFile.includes('/')
    && !rawLyricFile.includes('\\')
    && rawLyricFile !== '.'
    && rawLyricFile !== '..'
}

const fetchTtml = async (rawLyricFile: string): Promise<string> => {
  if (!isSafeRawLyricFile(rawLyricFile)) {
    throw new Error('parse')
  }
  const url = `${TTML_BASE_URL}${encodeURIComponent(rawLyricFile)}`
  try {
    const response = await fetchWithTimeout(url, TTML_TIMEOUT_MS)
    if (!response.ok) {
      throw new Error(`ttml http ${response.status}`)
    }
    const text = await response.text()
    const trimmed = text.trim()
    if (!trimmed || !/<(?:[a-z][\w.-]*:)?tt(?:\s|>)/i.test(trimmed)) {
      throw new Error('parse')
    }
    return text
  } catch (error) {
    if (error instanceof Error && error.message === 'parse') {
      throw error
    }
    throw new Error('network')
  }
}

const createQueryKey = (query: AmllMatchQuery): string => {
  return JSON.stringify([query.title.trim(), query.artist?.trim() || '', query.album?.trim() || ''])
}

const getNegative = (songId: string, queryKey: string): NegativeCacheEntry | null => {
  const entry = getBoundedCache(negativeBySongId, songId)
  if (!entry) {
    return null
  }
  if (entry.queryKey !== queryKey || Date.now() > entry.expiresAt) {
    negativeBySongId.delete(songId)
    return null
  }
  return entry
}

/**
 * 从 amll-ttml-db 为歌曲匹配 TTML 歌词。
 * - 索引内存缓存；TTML 按 songId 缓存；负缓存短时
 * - 失败不抛到 UI 层，统一返回 { ok: false, reason }
 */
export const matchAmllTtmlLyrics = async (query: AmllMatchQuery): Promise<AmllMatchResult> => {
  const songId = query.songId?.trim()
  if (!songId || !query.title?.trim()) {
    return { ok: false, reason: 'no-match' }
  }

  const queryKey = createQueryKey(query)
  const cached = getBoundedCache(ttmlBySongId, songId)
  if (cached) {
    if (cached.queryKey === queryKey) {
      return {
        ok: true,
        ttml: cached.ttml,
        rawLyricFile: cached.rawLyricFile,
        score: cached.score,
      }
    }
    ttmlBySongId.delete(songId)
  }

  const negative = getNegative(songId, queryKey)
  if (negative) {
    return { ok: false, reason: negative.reason }
  }

  let index: AmllIndexEntry[]
  try {
    index = await ensureIndex()
  } catch {
    setBoundedCache(negativeBySongId, songId, { queryKey, reason: 'network', expiresAt: Date.now() + NEGATIVE_CACHE_TTL_MS }, SONG_CACHE_MAX_SIZE)
    return { ok: false, reason: 'network' }
  }

  const searchIndex = searchIndexCache ?? createSearchIndex(index)
  searchIndexCache = searchIndex
  const candidates = selectCandidates(searchIndex, query.title)
  const best = findBestMatch(
    {
      title: query.title,
      artist: query.artist,
      album: query.album,
    },
    candidates,
  )

  if (!best) {
    setBoundedCache(negativeBySongId, songId, { queryKey, reason: 'no-match', expiresAt: Date.now() + NEGATIVE_CACHE_TTL_MS }, SONG_CACHE_MAX_SIZE)
    return { ok: false, reason: 'no-match' }
  }

  try {
    const ttml = await fetchTtml(best.entry.rawLyricFile)
    const hit: TtmlCacheEntry = {
      queryKey,
      ttml,
      rawLyricFile: best.entry.rawLyricFile,
      score: best.score,
    }
    setBoundedCache(ttmlBySongId, songId, hit, SONG_CACHE_MAX_SIZE)
    negativeBySongId.delete(songId)
    return {
      ok: true,
      ttml: hit.ttml,
      rawLyricFile: hit.rawLyricFile,
      score: hit.score,
    }
  } catch (error) {
    const reason = error instanceof Error && error.message === 'parse' ? 'parse' : 'network'
    setBoundedCache(negativeBySongId, songId, { queryKey, reason, expiresAt: Date.now() + NEGATIVE_CACHE_TTL_MS }, SONG_CACHE_MAX_SIZE)
    return { ok: false, reason }
  }
}

/** 测试辅助：注入内存索引（跳过网络） */
export const __setIndexCacheForTests = (entries: AmllIndexEntry[] | null): void => {
  indexCache = entries
  searchIndexCache = entries ? createSearchIndex(entries) : null
  indexPromise = null
}

/** 测试辅助：读取常见查询实际进入精确评分的候选数 */
export const __getCandidateCountForTests = (title: string): number => {
  return searchIndexCache ? selectCandidates(searchIndexCache, title).length : 0
}

/** 测试辅助：读取受控歌曲缓存大小 */
export const __getAmllCacheSizesForTests = (): { ttml: number; negative: number } => ({
  ttml: ttmlBySongId.size,
  negative: negativeBySongId.size,
})

/** 测试辅助：读取 TTML 缓存是否命中 */
export const __hasTtmlCacheForTests = (songId: string): boolean => ttmlBySongId.has(songId)
