/**
 * 待刮削队列存储（R1-6 / child1）。
 * 独立 localStorage key（与 muses:songs 分离，避免污染曲库存量）。
 * 入队幂等（按 songId 去重）；读取时懒清理已删歌曲；事件广播。
 */
import { loadSongs } from '@/features/library/storage'

const SCRAPE_QUEUE_KEY = 'muses:scrape-queue'
export const SCRAPE_QUEUE_UPDATED_EVENT = 'muses:scrape-queue-updated'

const SCRAPE_QUEUE_VERSION = 1

export interface ScrapeQueueItem {
  songId: string
  addedAt: string
}

export interface ScrapeQueueSnapshot {
  version: number
  items: ScrapeQueueItem[]
}

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const isScrapeQueueItem = (value: unknown): value is ScrapeQueueItem => {
  if (!isRecord(value)) {
    return false
  }
  return typeof value.songId === 'string' && value.songId.length > 0
    && typeof value.addedAt === 'string'
}

const readRawQueue = (): ScrapeQueueSnapshot => {
  const raw = localStorage.getItem(SCRAPE_QUEUE_KEY)
  if (!raw) {
    return { version: SCRAPE_QUEUE_VERSION, items: [] }
  }
  try {
    const parsed: unknown = JSON.parse(raw)
    if (!isRecord(parsed) || !Array.isArray(parsed.items)) {
      return { version: SCRAPE_QUEUE_VERSION, items: [] }
    }
    const items = parsed.items.filter(isScrapeQueueItem)
    return { version: SCRAPE_QUEUE_VERSION, items }
  } catch {
    return { version: SCRAPE_QUEUE_VERSION, items: [] }
  }
}

const writeQueue = (snapshot: ScrapeQueueSnapshot): void => {
  localStorage.setItem(SCRAPE_QUEUE_KEY, JSON.stringify(snapshot))
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(SCRAPE_QUEUE_UPDATED_EVENT))
  }
}

/**
 * 加载队列：懒清理曲库中已不存在的 songId（读取时过滤，不写回）。
 * 返回的 items 仅含当前曲库仍存在的歌曲。
 */
export const loadScrapeQueue = (): ScrapeQueueSnapshot => {
  const raw = readRawQueue()
  const songIds = new Set(loadSongs().map((song) => song.id))
  const items = raw.items.filter((item) => songIds.has(item.songId))
  // 若懒清理移除了脏数据，写回一次保持存储干净
  if (items.length !== raw.items.length) {
    writeQueue({ version: SCRAPE_QUEUE_VERSION, items })
  }
  return { version: SCRAPE_QUEUE_VERSION, items }
}

/** 队列中是否包含某 songId */
export const isInScrapeQueue = (songId: string): boolean => {
  return readRawQueue().items.some((item) => item.songId === songId)
}

/**
 * 批量入队（幂等）：已存在的 songId 只更新 addedAt；返回新增数量。
 */
export const enqueueScrapeSongs = (songIds: string[]): { added: number } => {
  const raw = readRawQueue()
  const existing = new Map(raw.items.map((item) => [item.songId, item]))
  const now = new Date().toISOString()
  let added = 0
  for (const songId of songIds) {
    if (!songId) {
      continue
    }
    if (existing.has(songId)) {
      // 幂等：仅更新时间
      existing.set(songId, { songId, addedAt: now })
    } else {
      existing.set(songId, { songId, addedAt: now })
      added += 1
    }
  }
  writeQueue({ version: SCRAPE_QUEUE_VERSION, items: [...existing.values()] })
  return { added }
}

/** 批量移除 */
export const removeScrapeSongs = (songIds: string[]): { removed: number } => {
  const raw = readRawQueue()
  const removeSet = new Set(songIds)
  const items = raw.items.filter((item) => !removeSet.has(item.songId))
  const removed = raw.items.length - items.length
  if (removed > 0) {
    writeQueue({ version: SCRAPE_QUEUE_VERSION, items })
  }
  return { removed }
}

/** 清空队列 */
export const clearScrapeQueue = (): void => {
  const raw = readRawQueue()
  if (raw.items.length === 0) {
    return
  }
  writeQueue({ version: SCRAPE_QUEUE_VERSION, items: [] })
}

/** 订阅队列变化（与 SONGS_UPDATED_EVENT 同模式） */
export const onScrapeQueueChanged = (handler: () => void): (() => void) => {
  if (typeof window === 'undefined') {
    return () => {}
  }
  const listener = () => handler()
  window.addEventListener(SCRAPE_QUEUE_UPDATED_EVENT, listener)
  return () => window.removeEventListener(SCRAPE_QUEUE_UPDATED_EVENT, listener)
}
