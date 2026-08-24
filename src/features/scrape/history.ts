/**
 * 刮削历史记录存储（08-24-scrape-history）。
 * 记录每次写回的成功/失败与失败原因，供 ScrapePage「历史」弹层展示。
 * 独立 localStorage key；版本化 snapshot + 滚动清理（上限 200 条）+ 事件广播，
 * 存储模式与 queue.ts 保持一致。历史条目自带歌名快照，删歌后仍可展示。
 */

const SCRAPE_HISTORY_KEY = 'muses:scrape-history'
export const SCRAPE_HISTORY_UPDATED_EVENT = 'muses:scrape-history-updated'

const SCRAPE_HISTORY_VERSION = 1
/** 滚动清理上限：仅保留最新 200 条 */
const MAX_HISTORY_ENTRIES = 200

export type ScrapeHistoryStatus = 'success' | 'file-failed' | 'failed'

export interface ScrapeHistoryEntry {
  /** 唯一 id */
  id: string
  /** 写回批次号（对应回滚 journal 的 journalId，重试会产生新批次） */
  journalId: string
  songId: string
  /** 歌名快照（防删歌后无法展示） */
  songTitle: string
  /** 艺术家快照 */
  songArtist?: string
  /** ISO 时间 */
  at: string
  status: ScrapeHistoryStatus
  /** 失败原因（复用 describeWritebackFailure 文案），成功时缺省 */
  failureReason?: string
  /** 本次写回字段：title/artist/album/cover/lyrics */
  changedFields: string[]
}

export interface ScrapeHistorySnapshot {
  version: number
  entries: ScrapeHistoryEntry[]
}

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const isScrapeHistoryEntry = (value: unknown): value is ScrapeHistoryEntry => {
  if (!isRecord(value)) {
    return false
  }
  return typeof value.id === 'string' && value.id.length > 0
    && typeof value.journalId === 'string'
    && typeof value.songId === 'string'
    && typeof value.songTitle === 'string'
    && typeof value.at === 'string'
    && (value.status === 'success' || value.status === 'file-failed' || value.status === 'failed')
    && Array.isArray(value.changedFields)
    && value.changedFields.every((f) => typeof f === 'string')
}

/** 唯一 id：优先 crypto.randomUUID，老 WebView 降级时间戳 + 随机数 */
const genId = (): string => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `hist-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

const readRawEntries = (): ScrapeHistoryEntry[] => {
  const raw = localStorage.getItem(SCRAPE_HISTORY_KEY)
  if (!raw) {
    return []
  }
  try {
    const parsed: unknown = JSON.parse(raw)
    if (!isRecord(parsed) || !Array.isArray(parsed.entries)) {
      return []
    }
    return parsed.entries.filter(isScrapeHistoryEntry)
  } catch {
    return []
  }
}

const writeEntries = (entries: ScrapeHistoryEntry[]): void => {
  localStorage.setItem(SCRAPE_HISTORY_KEY, JSON.stringify({
    version: SCRAPE_HISTORY_VERSION,
    entries,
  }))
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(SCRAPE_HISTORY_UPDATED_EVENT))
  }
}

/**
 * 加载历史：按 at 时间倒序返回（最新在前）。
 */
export const loadScrapeHistory = (): ScrapeHistoryEntry[] => {
  const entries = readRawEntries()
  return [...entries].sort((a, b) => (a.at < b.at ? 1 : a.at > b.at ? -1 : 0))
}

/**
 * 批量追加历史：补 id 与 at=now；追加后滚动清理只保留最新 200 条。
 */
export const appendScrapeHistory = (
  entries: Array<Omit<ScrapeHistoryEntry, 'id' | 'at'>>,
): void => {
  if (entries.length === 0) {
    return
  }
  const now = new Date().toISOString()
  const next = [
    ...readRawEntries(),
    ...entries.map((entry) => ({
      ...entry,
      id: genId(),
      at: now,
    })),
  ]
  // 滚动清理：按 at 排序保留最新 MAX_HISTORY_ENTRIES 条
  next.sort((a, b) => (a.at < b.at ? 1 : a.at > b.at ? -1 : 0))
  writeEntries(next.slice(0, MAX_HISTORY_ENTRIES))
}

/** 清空历史 */
export const clearScrapeHistory = (): void => {
  if (readRawEntries().length === 0) {
    return
  }
  writeEntries([])
}

/** 订阅历史变化事件（与 queue.ts 同模式），返回退订函数 */
export const onScrapeHistoryChanged = (handler: () => void): (() => void) => {
  if (typeof window === 'undefined') {
    return () => {}
  }
  const listener = () => handler()
  window.addEventListener(SCRAPE_HISTORY_UPDATED_EVENT, listener)
  return () => window.removeEventListener(SCRAPE_HISTORY_UPDATED_EVENT, listener)
}
