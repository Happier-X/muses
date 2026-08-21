import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { applyScrapeChanges, revertScrapeJournal, getCurrentRollbackJournal } from '@/features/scrape/writeback'
import type { ScrapeCandidate } from '@/features/scrape/matcher'
import type { ScrapeChanges } from '@/features/scrape/writeback'

// 内存 localStorage mock（无 jsdom 依赖）
const memStorage = new Map<string, string>()
const memStorageApi: Storage = {
  get length() { return memStorage.size },
  clear: () => memStorage.clear(),
  getItem: (key: string) => memStorage.get(key) ?? null,
  setItem: (key: string, value: string) => { memStorage.set(key, value) },
  removeItem: (key: string) => { memStorage.delete(key) },
  key: (index: number) => [...memStorage.keys()][index] ?? null,
}
vi.stubGlobal('window', { dispatchEvent: vi.fn() })
vi.stubGlobal('localStorage', memStorageApi)

// Mock storage
const mockSongs: Array<Record<string, unknown>> = []
vi.mock('@/features/library/storage', () => ({
  loadSongs: vi.fn(() => [...mockSongs]),
  saveSongs: vi.fn((songs: unknown[]) => {
    mockSongs.length = 0
    mockSongs.push(...(songs as Array<Record<string, unknown>>))
  }),
}))

// Mock native write
vi.mock('@/features/library/native', () => ({
  writeLocalAudioMetadata: vi.fn(async () => ({ ok: true })),
}))

// Mock WebDAV write
vi.mock('@/features/sources/webdav', () => ({
  writeWebDavAudioMetadata: vi.fn(async () => ({ ok: true })),
}))

// Mock sources storage
vi.mock('@/features/sources/storage', () => ({
  loadSources: vi.fn(() => []),
}))

// Mock player/native (cacheRemoteCover)
vi.mock('@/features/player/native', () => ({
  cacheRemoteCover: vi.fn(async (url: string) => url),
}))

beforeEach(() => {
  mockSongs.length = 0
  mockSongs.push(
    {
      id: 's1', title: '晴天', artist: '周杰伦', album: '叶惠美',
      coverUri: 'file:///old.jpg', lyrics: '旧歌词', lyricsFormat: 'lrc',
      lyricsSource: 'embedded', sourceType: 'local', path: '/a/晴天.mp3',
      uri: 'file:///a/晴天.mp3', metaSources: {},
      updatedAt: '2026-01-01T00:00:00Z',
    },
    {
      id: 's2', title: 'track02', artist: undefined, album: undefined,
      coverUri: undefined, lyrics: undefined, lyricsFormat: undefined,
      lyricsSource: undefined, sourceType: 'local', path: '/a/track02.mp3',
      uri: 'file:///a/track02.mp3', metaSources: {},
      updatedAt: '2026-01-01T00:00:00Z',
    },
  )
  memStorage.clear()
})

afterEach(() => {
  vi.clearAllMocks()
})

const makeCandidate = (id: string, overrides: Partial<ScrapeCandidate> = {}): ScrapeCandidate => ({
  songId: id,
  song: mockSongs.find((s) => s.id === id) as never,
  text: { current: {}, candidates: [], defaultIndex: -1 },
  cover: { currentUri: undefined, candidates: [], defaultIndex: -1 },
  lyrics: { currentText: undefined, currentFormat: undefined, candidates: [], defaultIndex: -1 },
  overallConfidence: 'high',
  defaultChecked: true,
  ...overrides,
})

describe('applyScrapeChanges (child3)', () => {
  it('写前创建回滚 journal', async () => {
    const candidates = [makeCandidate('s1')]
    const checkedIds = new Set(['s1'])
    const changes = new Map<string, ScrapeChanges>([['s1', { title: '晴天 (新)' }]])

    const { journalId } = await applyScrapeChanges(candidates, checkedIds, changes)

    expect(journalId).toBeTruthy()
    const journal = getCurrentRollbackJournal()
    expect(journal).not.toBeNull()
    expect(journal!.entries).toHaveLength(1)
    expect(journal!.entries[0].songId).toBe('s1')
    expect(journal!.entries[0].songBefore.title).toBe('晴天')
  })

  it('写入成功后曲库值更新', async () => {
    const candidates = [makeCandidate('s1')]
    const checkedIds = new Set(['s1'])
    const changes = new Map<string, ScrapeChanges>([['s1', { title: '晴天 (新)', artist: '周杰伦 (新)' }]])

    await applyScrapeChanges(candidates, checkedIds, changes)

    const updated = mockSongs.find((s) => s.id === 's1')
    expect(updated!.title).toBe('晴天 (新)')
    expect(updated!.artist).toBe('周杰伦 (新)')
  })

  it('文件写入成功时 metaSources 标记 embedded', async () => {
    const candidates = [makeCandidate('s1')]
    const checkedIds = new Set(['s1'])
    const changes = new Map<string, ScrapeChanges>([['s1', { title: '晴天 (新)' }]])

    await applyScrapeChanges(candidates, checkedIds, changes)

    const updated = mockSongs.find((s) => s.id === 's1')
    expect(updated!.metaSources).toEqual({ title: 'embedded' })
  })

  it('未勾选的歌曲不写入', async () => {
    const candidates = [makeCandidate('s1'), makeCandidate('s2')]
    const checkedIds = new Set(['s1']) // 只勾选 s1
    const changes = new Map<string, ScrapeChanges>([
      ['s1', { title: '晴天 (新)' }],
      ['s2', { title: 'track02 (新)' }],
    ])

    await applyScrapeChanges(candidates, checkedIds, changes)

    expect(mockSongs.find((s) => s.id === 's1')!.title).toBe('晴天 (新)')
    expect(mockSongs.find((s) => s.id === 's2')!.title).toBe('track02') // 未变
  })

  it('写文件失败时 metaSources 标记 scrape', async () => {
    const { writeLocalAudioMetadata } = await import('@/features/library/native')
    vi.mocked(writeLocalAudioMetadata).mockResolvedValueOnce({ ok: false, code: 'write_failed' })

    const candidates = [makeCandidate('s1')]
    const checkedIds = new Set(['s1'])
    const changes = new Map<string, ScrapeChanges>([['s1', { title: '晴天 (新)' }]])

    const { results } = await applyScrapeChanges(candidates, checkedIds, changes)

    expect(results[0].status).toBe('file-failed')
    const updated = mockSongs.find((s) => s.id === 's1')
    expect(updated!.metaSources).toEqual({ title: 'scrape' })
  })

  it('歌词写回文件成功 → lyricsSource=embedded；失败 → lyricsSource=scrape', async () => {
    const { writeLocalAudioMetadata } = await import('@/features/library/native')
    const candidates = [makeCandidate('s1'), makeCandidate('s2')]
    const checkedIds = new Set(['s1', 's2'])
    const changes = new Map<string, ScrapeChanges>([
      ['s1', { lyrics: '[00:00]新歌词' }],
      ['s2', { lyrics: '[00:00]失败歌词' }],
    ])
    vi.mocked(writeLocalAudioMetadata)
      .mockResolvedValueOnce({ ok: true })
      .mockResolvedValueOnce({ ok: false, code: 'write_failed' })

    await applyScrapeChanges(candidates, checkedIds, changes)

    expect(mockSongs.find((s) => s.id === 's1')!.lyricsSource).toBe('embedded')
    expect(mockSongs.find((s) => s.id === 's2')!.lyricsSource).toBe('scrape')
  })

  it('revertScrapeJournal 恢复曲库旧值', async () => {
    const candidates = [makeCandidate('s1')]
    const checkedIds = new Set(['s1'])
    const changes = new Map<string, ScrapeChanges>([['s1', { title: '晴天 (新)' }]])

    const { journalId } = await applyScrapeChanges(candidates, checkedIds, changes)
    expect(mockSongs.find((s) => s.id === 's1')!.title).toBe('晴天 (新)')

    const { reverted } = revertScrapeJournal(journalId)
    expect(reverted).toBe(1)
    expect(mockSongs.find((s) => s.id === 's1')!.title).toBe('晴天')
  })

  it('revert 后 journal 被清除', async () => {
    const candidates = [makeCandidate('s1')]
    const checkedIds = new Set(['s1'])
    const changes = new Map<string, ScrapeChanges>([['s1', { title: '晴天 (新)' }]])

    const { journalId } = await applyScrapeChanges(candidates, checkedIds, changes)
    revertScrapeJournal(journalId)

    expect(getCurrentRollbackJournal()).toBeNull()
  })

  it('错误 journalId → reverted=0', () => {
    const { reverted } = revertScrapeJournal('nonexistent')
    expect(reverted).toBe(0)
  })

  it('多曲批量写回返回逐行结果', async () => {
    const candidates = [makeCandidate('s1'), makeCandidate('s2')]
    const checkedIds = new Set(['s1', 's2'])
    const changes = new Map<string, ScrapeChanges>([
      ['s1', { title: '晴天 (新)' }],
      ['s2', { artist: '未知艺术家' }],
    ])

    const { results } = await applyScrapeChanges(candidates, checkedIds, changes)

    expect(results).toHaveLength(2)
    expect(results.every((r) => r.status === 'success')).toBe(true)
  })
})
