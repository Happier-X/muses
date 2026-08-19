import { describe, expect, it, vi, beforeEach } from 'vitest'
import { matchScrapeQueue } from '@/features/scrape/matcher'
import type { ScrapeQueueSnapshot } from '@/features/scrape/queue'

vi.mock('@/features/library/storage', () => ({
  loadSongs: vi.fn(() => [
    {
      id: 's1', title: '晴天', artist: '周杰伦', album: '叶惠美',
      duration: 269, path: '/a/晴天.mp3', coverUri: 'file:///cover.jpg',
      lyrics: '旧歌词', lyricsFormat: 'lrc', sourceType: 'local',
    },
    {
      id: 's2', title: 'track02', artist: undefined, album: undefined,
      duration: 180, path: '/a/track02.mp3', sourceType: 'local',
    },
    {
      id: 's3', title: '夜曲', artist: '周杰伦', album: '十一月的萧邦',
      duration: 225, path: '/a/夜曲.mp3', sourceType: 'local',
    },
  ]),
}))

const mockSearchEditCloudMeta = vi.fn()
vi.mock('@/features/editMeta/searchEditCloudMeta', () => ({
  searchEditCloudMeta: (...args: unknown[]) => mockSearchEditCloudMeta(...args),
}))

const makeQueue = (ids: string[]): ScrapeQueueSnapshot => ({
  version: 1,
  items: ids.map((id) => ({ songId: id, addedAt: '2026-01-01T00:00:00Z' })),
})

beforeEach(() => {
  vi.clearAllMocks()
})

describe('matchScrapeQueue (child3)', () => {
  it('空队列 → 空结果', async () => {
    const result = await matchScrapeQueue({ version: 1, items: [] })
    expect(result.candidates).toEqual([])
    expect(result.errors).toEqual([])
  })

  it('逐曲调用 searchEditCloudMeta', async () => {
    mockSearchEditCloudMeta.mockResolvedValue({
      text: { status: 'ok', items: [{ title: '晴天', artist: '周杰伦', source: 'kw' }], defaultIndex: 0 },
      cover: { status: 'no-match', items: [], defaultIndex: 0 },
      lyrics: { status: 'no-match', items: [], defaultIndex: 0 },
    })

    await matchScrapeQueue(makeQueue(['s1']))

    expect(mockSearchEditCloudMeta).toHaveBeenCalledTimes(1)
    expect(mockSearchEditCloudMeta).toHaveBeenCalledWith(
      expect.objectContaining({ songId: 's1', title: '晴天', artist: '周杰伦' }),
      expect.objectContaining({ maxCandidates: 8 }),
    )
  })

  it('返回匹配结果含候选+置信度', async () => {
    mockSearchEditCloudMeta.mockResolvedValue({
      text: { status: 'ok', items: [{ title: '晴天', artist: '周杰伦', source: 'kw' }], defaultIndex: 0 },
      cover: { status: 'no-match', items: [], defaultIndex: 0 },
      lyrics: { status: 'no-match', items: [], defaultIndex: 0 },
    })

    const result = await matchScrapeQueue(makeQueue(['s1']))

    expect(result.candidates).toHaveLength(1)
    expect(result.candidates[0].songId).toBe('s1')
    expect(result.candidates[0].text.candidates).toHaveLength(1)
    expect(result.candidates[0].overallConfidence).toBe('high')
    expect(result.candidates[0].defaultChecked).toBe(true)
  })

  it('高置信（text hit + cover hit）→ defaultChecked=true', async () => {
    mockSearchEditCloudMeta.mockResolvedValue({
      text: { status: 'ok', items: [{ title: '晴天', artist: '周杰伦', source: 'kw' }], defaultIndex: 0 },
      cover: { status: 'ok', items: [{ remoteUrl: 'https://img.jpg', source: 'itunes' }], defaultIndex: 0 },
      lyrics: { status: 'no-match', items: [], defaultIndex: 0 },
    })

    const result = await matchScrapeQueue(makeQueue(['s1']))

    expect(result.candidates[0].overallConfidence).toBe('high')
    expect(result.candidates[0].defaultChecked).toBe(true)
  })

  it('低置信（无候选）→ defaultChecked=false', async () => {
    mockSearchEditCloudMeta.mockResolvedValue({
      text: { status: 'no-match', items: [], defaultIndex: 0 },
      cover: { status: 'no-match', items: [], defaultIndex: 0 },
      lyrics: { status: 'no-match', items: [], defaultIndex: 0 },
    })

    const result = await matchScrapeQueue(makeQueue(['s2']))

    expect(result.candidates[0].overallConfidence).toBe('low')
    expect(result.candidates[0].defaultChecked).toBe(false)
  })

  it('单曲失败不阻塞其他', async () => {
    mockSearchEditCloudMeta
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce({
        text: { status: 'ok', items: [{ title: '夜曲', artist: '周杰伦', source: 'kw' }], defaultIndex: 0 },
        cover: { status: 'no-match', items: [], defaultIndex: 0 },
        lyrics: { status: 'no-match', items: [], defaultIndex: 0 },
      })

    const result = await matchScrapeQueue(makeQueue(['s1', 's3']))

    expect(result.candidates).toHaveLength(1)
    expect(result.candidates[0].songId).toBe('s3')
    expect(result.errors).toHaveLength(1)
    expect(result.errors[0].songId).toBe('s1')
    expect(result.errors[0].reason).toBe('network')
  })

  it('队列中不存在的 songId 被过滤', async () => {
    mockSearchEditCloudMeta.mockResolvedValue({
      text: { status: 'no-match', items: [], defaultIndex: 0 },
      cover: { status: 'no-match', items: [], defaultIndex: 0 },
      lyrics: { status: 'no-match', items: [], defaultIndex: 0 },
    })

    const result = await matchScrapeQueue(makeQueue(['s1', 'nonexistent']))

    expect(result.candidates).toHaveLength(1)
    expect(result.candidates[0].songId).toBe('s1')
  })

  it('onProgress 回调计数正确', async () => {
    mockSearchEditCloudMeta.mockResolvedValue({
      text: { status: 'no-match', items: [], defaultIndex: 0 },
      cover: { status: 'no-match', items: [], defaultIndex: 0 },
      lyrics: { status: 'no-match', items: [], defaultIndex: 0 },
    })

    const onProgress = vi.fn()
    await matchScrapeQueue(makeQueue(['s1', 's3']), { onProgress })

    expect(onProgress).toHaveBeenCalledTimes(2)
    expect(onProgress).toHaveBeenLastCalledWith({ matched: 2, total: 2 })
  })

  it('候选包含 cover 歌词详情', async () => {
    mockSearchEditCloudMeta.mockResolvedValue({
      text: { status: 'ok', items: [{ title: '晴天', artist: '周杰伦', source: 'kw' }], defaultIndex: 0 },
      cover: { status: 'ok', items: [{ remoteUrl: 'https://img.jpg', source: 'itunes' }], defaultIndex: 0 },
      lyrics: { status: 'ok', items: [{ text: '[00:00]新歌词', format: 'ttml', source: 'amll' }], defaultIndex: 0 },
    })

    const result = await matchScrapeQueue(makeQueue(['s1']))
    const candidate = result.candidates[0]

    expect(candidate.text.candidates[0]).toEqual({ title: '晴天', artist: '周杰伦', source: 'kw' })
    expect(candidate.cover.candidates[0]).toEqual({ remoteUrl: 'https://img.jpg', source: 'itunes' })
    expect(candidate.lyrics.candidates[0]).toEqual({ text: '[00:00]新歌词', format: 'ttml', source: 'amll' })
  })
})
