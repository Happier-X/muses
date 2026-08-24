import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import {
  loadScrapeHistory,
  appendScrapeHistory,
  clearScrapeHistory,
  onScrapeHistoryChanged,
  SCRAPE_HISTORY_UPDATED_EVENT,
} from '@/features/scrape/history'

// 内存 localStorage mock（与 scrape-writeback.spec.ts 同模式）
const memStorage = new Map<string, string>()
const memStorageApi: Storage = {
  get length() { return memStorage.size },
  clear: () => memStorage.clear(),
  getItem: (key: string) => memStorage.get(key) ?? null,
  setItem: (key: string, value: string) => { memStorage.set(key, value) },
  removeItem: (key: string) => { memStorage.delete(key) },
  key: (index: number) => [...memStorage.keys()][index] ?? null,
}

// 可捕获事件的真实 window mock
const listeners: Record<string, Set<EventListener>> = {}
vi.stubGlobal('window', {
  dispatchEvent: vi.fn((event: Event) => {
    for (const listener of listeners[event.type] ?? []) {
      listener(event)
    }
    return true
  }),
  addEventListener: vi.fn((type: string, listener: EventListener) => {
    if (!listeners[type]) listeners[type] = new Set()
    listeners[type].add(listener)
  }),
  removeEventListener: vi.fn((type: string, listener: EventListener) => {
    listeners[type]?.delete(listener)
  }),
})
vi.stubGlobal('localStorage', memStorageApi)

beforeEach(() => {
  memStorage.clear()
})

afterEach(() => {
  vi.clearAllMocks()
})

const makeEntry = (
  overrides: Partial<Parameters<typeof appendScrapeHistory>[0][number]> = {},
): Parameters<typeof appendScrapeHistory>[0][number] => ({
  journalId: 'journal-1',
  songId: 's1',
  songTitle: '晴天',
  songArtist: '周杰伦',
  status: 'success',
  changedFields: ['title'],
  ...overrides,
})

describe('scrape history 存储', () => {
  it('空存储返回空数组', () => {
    expect(loadScrapeHistory()).toEqual([])
  })

  it('追加后可读取，且按时间倒序（最新在前）', () => {
    appendScrapeHistory([makeEntry({ songId: 's1' })])
    // 模拟稍晚的第二条
    const raw = JSON.parse(memStorage.get('muses:scrape-history')!)
    raw.entries[0].at = '2026-01-01T00:00:00Z'
    memStorage.set('muses:scrape-history', JSON.stringify(raw))
    appendScrapeHistory([makeEntry({ songId: 's2', journalId: 'journal-2' })])

    const entries = loadScrapeHistory()
    expect(entries).toHaveLength(2)
    expect(entries[0].songId).toBe('s2')
    expect(entries[1].songId).toBe('s1')
  })

  it('追加补齐 id 与 at，字段原样保留', () => {
    appendScrapeHistory([makeEntry({
      status: 'file-failed',
      failureReason: 'WebDAV 密码缺失，请到音源设置补全后重试',
      changedFields: ['title', 'cover'],
    })])

    const [entry] = loadScrapeHistory()
    expect(entry.id).toBeTruthy()
    expect(new Date(entry.at).getTime()).not.toBeNaN()
    expect(entry.journalId).toBe('journal-1')
    expect(entry.songTitle).toBe('晴天')
    expect(entry.songArtist).toBe('周杰伦')
    expect(entry.status).toBe('file-failed')
    expect(entry.failureReason).toBe('WebDAV 密码缺失，请到音源设置补全后重试')
    expect(entry.changedFields).toEqual(['title', 'cover'])
  })

  it('滚动清理：超过 200 条只保留最新 200 条', () => {
    // 先写入 205 条旧记录（手工构造，时间递增）
    const base = Date.parse('2026-01-01T00:00:00Z')
    const oldEntries = Array.from({ length: 205 }, (_, i) => ({
      id: `old-${i}`,
      journalId: 'journal-old',
      songId: `song-${i}`,
      songTitle: `歌 ${i}`,
      at: new Date(base + i * 1000).toISOString(),
      status: 'success' as const,
      changedFields: ['title'],
    }))
    memStorage.set('muses:scrape-history', JSON.stringify({ version: 1, entries: oldEntries }))

    // 追加一条新记录触发滚动清理
    appendScrapeHistory([makeEntry({ songId: 'new-song', songTitle: '新歌' })])

    const entries = loadScrapeHistory()
    expect(entries).toHaveLength(200)
    // 最新在前：新追加的排第一
    expect(entries[0].songId).toBe('new-song')
    // 最旧的 6 条（205 - 199 被清理）已丢弃
    expect(entries.some((e) => e.songId === 'song-5')).toBe(false)
    expect(entries.some((e) => e.songId === 'song-6')).toBe(true)
  })

  it('清空历史生效并广播事件', () => {
    appendScrapeHistory([makeEntry()])
    clearScrapeHistory()
    expect(loadScrapeHistory()).toEqual([])
    expect(memStorage.get('muses:scrape-history')).toBe(JSON.stringify({ version: 1, entries: [] }))
  })

  it('空存储清空不写库不广播', () => {
    clearScrapeHistory()
    expect(memStorage.has('muses:scrape-history')).toBe(false)
  })

  it('追加广播 muses:scrape-history-updated；退订后不再接收', () => {
    const handler = vi.fn()
    const unsubscribe = onScrapeHistoryChanged(handler)

    appendScrapeHistory([makeEntry()])
    expect(handler).toHaveBeenCalledTimes(1)
    // 事件名正确
    const dispatched = vi.mocked(window.dispatchEvent).mock.calls[0]?.[0] as CustomEvent
    expect(dispatched.type).toBe(SCRAPE_HISTORY_UPDATED_EVENT)

    unsubscribe()
    appendScrapeHistory([makeEntry({ songId: 's3' })])
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('非法数据忽略：坏 JSON / 非 snapshot 结构 / 非法条目', () => {
    memStorage.set('muses:scrape-history', '{broken json')
    expect(loadScrapeHistory()).toEqual([])

    memStorage.set('muses:scrape-history', JSON.stringify({ version: 1, items: [] }))
    expect(loadScrapeHistory()).toEqual([])

    memStorage.set('muses:scrape-history', JSON.stringify({
      version: 1,
      entries: [
        { id: 'ok', journalId: 'j', songId: 's1', songTitle: 't', at: '2026-01-01T00:00:00Z', status: 'success', changedFields: [] },
        { id: 'bad-status', journalId: 'j', songId: 's2', songTitle: 't', at: '2026-01-01T00:00:00Z', status: 'weird', changedFields: [] },
        { id: 'no-title', journalId: 'j', songId: 's3', at: '2026-01-01T00:00:00Z', status: 'success', changedFields: [] },
        'not-an-object',
      ],
    }))
    const entries = loadScrapeHistory()
    expect(entries).toHaveLength(1)
    expect(entries[0].id).toBe('ok')
  })

  it('append 空数组为 no-op 不广播', () => {
    appendScrapeHistory([])
    expect(vi.mocked(window.dispatchEvent)).not.toHaveBeenCalled()
  })
})
