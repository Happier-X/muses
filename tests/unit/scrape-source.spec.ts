import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * child1 单测：storage 来源追踪 + 待刮削队列。
 * 项目无 jsdom/happy-dom 依赖，这里用内存 localStorage mock 覆盖 window.localStorage。
 */

// 内存 localStorage 实现
const createMemStorage = () => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => (key in store ? store[key] : null),
    setItem: (key: string, value: string) => { store[key] = String(value) },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} },
  }
}

let memStorage: ReturnType<typeof createMemStorage>

const stubWindow = () => {
  memStorage = createMemStorage()
  const listeners: Record<string, EventListener> = {}
  const win = {
    localStorage: memStorage,
    addEventListener: (type: string, listener: EventListener) => { listeners[type] = listener },
    removeEventListener: (type: string) => { delete listeners[type] },
    dispatchEvent: (event: Event) => {
      const type = (event as unknown as { type: string }).type
      listeners[type]?.(event)
      return true
    },
  }
  vi.stubGlobal('window', win)
  vi.stubGlobal('localStorage', memStorage)
}

stubWindow()

// 导入被测模块（必须在 stubGlobal 之后，确保 storage/queue 读取到 mock window）
const {
  CURRENT_METADATA_VERSION,
  applyTagsRespectingUserEdits,
  getFieldSource,
  isUserEditedField,
  loadSongs,
  reconcileSourceSongs,
  saveSongs,
  upsertSong,
  updateSongUserEdit,
} = await import('@/features/library/storage')
const {
  clearScrapeQueue,
  enqueueScrapeSongs,
  isInScrapeQueue,
  loadScrapeQueue,
  onScrapeQueueChanged,
  removeScrapeSongs,
} = await import('@/features/scrape/queue')

// CustomEvent 仅队列事件广播用到；storage.notifySongsUpdated / queue.writeQueue 内部 new CustomEvent
// Node 内置 Event/CustomEvent 全局可用，window.dispatchEvent 走上面的 stub。

const makeSongInput = (overrides: Partial<Parameters<typeof upsertSong>[0]> = {}) => ({
  sourceId: 'src-1',
  sourceType: 'local' as const,
  path: '/music/a.mp3',
  uri: 'file:///music/a.mp3',
  title: '文件名占位',
  ...overrides,
})

describe('storage metaSources 来源追踪', () => {
  beforeEach(() => memStorage.clear())

  it('扫描写入有值字段标 embedded', () => {
    const { songs } = upsertSong(makeSongInput({
      tags: {
        title: '真实标题', artist: '歌手', album: '专辑', coverUri: 'file:///covers/a.png',
        metaSources: { title: 'embedded', artist: 'embedded', album: 'embedded', cover: 'embedded' },
      },
    }), undefined)
    const song = songs[0]
    expect(getFieldSource(song, 'title')).toBe('embedded')
    expect(getFieldSource(song, 'artist')).toBe('embedded')
    expect(getFieldSource(song, 'album')).toBe('embedded')
    expect(getFieldSource(song, 'cover')).toBe('embedded')
  })

  it('在线补缺写字段标 cloud，不覆盖未写字段的旧来源', () => {
    const { songs: afterScan } = upsertSong(makeSongInput({
      tags: {
        title: '真实标题', artist: '歌手', album: '专辑',
        metaSources: { title: 'embedded', artist: 'embedded', album: 'embedded' },
      },
    }), undefined)
    expect(getFieldSource(afterScan[0], 'title')).toBe('embedded')

    const { songs } = upsertSong(makeSongInput({
      tags: { coverUri: 'file:///covers/cloud-a.png', metaSources: { cover: 'cloud' } },
    }), loadSongs())
    const song = songs[0]
    expect(getFieldSource(song, 'cover')).toBe('cloud')
    // title/artist/album 来源保留 embedded（未写字段不覆盖）
    expect(getFieldSource(song, 'title')).toBe('embedded')
    expect(getFieldSource(song, 'artist')).toBe('embedded')
  })

  it('字段无来源标记时 getFieldSource 默认 embedded（存量兼容）', () => {
    saveSongs([{
      id: 'song:src-1:_2Fmusic_2Fa.mp3', sourceId: 'src-1', sourceType: 'local',
      path: '/music/a.mp3', uri: 'file:///music/a.mp3', title: '旧标题',
      createdAt: '2024-01-01T00:00:00.000Z', updatedAt: '2024-01-01T00:00:00.000Z',
    }])
    const song = loadSongs()[0]
    // 无 metaSources → 视同 embedded
    expect(getFieldSource(song, 'title')).toBe('embedded')
  })

  it('用户手改字段后来源派生为 manual，且 metaSources 该 key 被移除', () => {
    const { songs } = upsertSong(makeSongInput({
      tags: { title: '原标题', artist: '原歌手', metaSources: { title: 'embedded', artist: 'embedded' } },
    }), undefined)
    const songId = songs[0].id
    updateSongUserEdit(songId, { artist: '手改歌手' })
    const updated = loadSongs()[0]
    expect(getFieldSource(updated, 'artist')).toBe('manual')
    // artist 来源 key 被从 metaSources 移除（manual 派生，不双写）；title 来源保留
    expect(updated.metaSources?.artist).toBeUndefined()
    expect(updated.metaSources?.title).toBe('embedded')
    expect(isUserEditedField(updated, 'artist')).toBe(true)
  })

  it('applyTagsRespectingUserEdits 剥离手改字段的来源标记', () => {
    const song = { userEditedFields: ['title', 'cover'] as const }
    const tags = {
      title: '新标题', coverUri: 'file:///covers/x.png',
      metaSources: { title: 'cloud', cover: 'cloud', album: 'cloud' } as Partial<Record<'title' | 'artist' | 'album' | 'cover', 'embedded' | 'cloud' | 'manual'>>,
    }
    const result = applyTagsRespectingUserEdits(song, tags)
    expect(result.metaSources?.title).toBeUndefined()
    expect(result.metaSources?.cover).toBeUndefined()
    expect(result.metaSources?.album).toBe('cloud')
    expect(result.title).toBeUndefined()
  })

  it('upsert 不写空来源（tags 无 metaSources 时保留旧来源）', () => {
    upsertSong(makeSongInput({
      tags: { title: '原标题', metaSources: { title: 'embedded' } },
    }), undefined)
    const { songs } = upsertSong(makeSongInput({
      tags: { tagsScanned: true, tagsScannedAt: '2024-01-02' },
    }), loadSongs())
    expect(getFieldSource(songs[0], 'title')).toBe('embedded')
  })

  it('CURRENT_METADATA_VERSION 已升至 4', () => {
    expect(CURRENT_METADATA_VERSION).toBe(4)
  })
})

describe('scrape queue 待刮削队列', () => {
  beforeEach(() => memStorage.clear())

  it('空队列加载返回空 items', () => {
    const queue = loadScrapeQueue()
    expect(queue.items).toEqual([])
    expect(queue.version).toBe(1)
  })

  it('入队幂等：重复 songId 只更新时间不新增', () => {
    // 使用 isInScrapeQueue（读 raw，不过滤曲库）验证幂等，避免 loadScrapeQueue 懒清理干扰
    expect(enqueueScrapeSongs(['s1', 's2']).added).toBe(2)
    expect(isInScrapeQueue('s1')).toBe(true)
    expect(isInScrapeQueue('s2')).toBe(true)
    const { added } = enqueueScrapeSongs(['s1', 's3'])
    expect(added).toBe(1)
    expect(isInScrapeQueue('s3')).toBe(true)
  })

  it('loadScrapeQueue 懒清理掉曲库不存在的游离 id', () => {
    enqueueScrapeSongs(['ghost-1', 'ghost-2'])
    expect(loadScrapeQueue().items).toEqual([])
  })

  it('批量移除（用 isInScrapeQueue 验证，避免懒清理干扰）', () => {
    enqueueScrapeSongs(['s1', 's2', 's3'])
    expect(removeScrapeSongs(['s2']).removed).toBe(1)
    expect(isInScrapeQueue('s1')).toBe(true)
    expect(isInScrapeQueue('s2')).toBe(false)
    expect(isInScrapeQueue('s3')).toBe(true)
  })

  it('清空队列', () => {
    enqueueScrapeSongs(['s1', 's2'])
    clearScrapeQueue()
    expect(isInScrapeQueue('s1')).toBe(false)
    expect(loadScrapeQueue().items).toEqual([])
  })

  it('isInScrapeQueue 判定', () => {
    enqueueScrapeSongs(['s1'])
    expect(isInScrapeQueue('s1')).toBe(true)
    expect(isInScrapeQueue('s2')).toBe(false)
  })

  it('曲库已删歌曲在 loadScrapeQueue 时懒清理', () => {
    upsertSong({
      sourceId: 'src-1', sourceType: 'local', path: '/a.mp3', uri: 'file:///a.mp3', title: 'A',
    }, undefined)
    enqueueScrapeSongs(['song:src-1:_2Fa.mp3', 'song-b'])
    const queue = loadScrapeQueue()
    // 只保留曲库中存在的 song:src-1:_2Fa.mp3
    expect(queue.items.map((i) => i.songId)).toEqual(['song:src-1:_2Fa.mp3'])
  })

  it('onScrapeQueueChanged 事件广播生效', () => {
    let calls = 0
    const off = onScrapeQueueChanged(() => { calls += 1 })
    enqueueScrapeSongs(['s1'])
    expect(calls).toBe(1)
    removeScrapeSongs(['s1'])
    expect(calls).toBe(2)
    clearScrapeQueue()
    expect(calls).toBeGreaterThanOrEqual(2)
    off()
  })

  it('reconcile 删除歌曲后队列通过 loadScrapeQueue 懒清理', () => {
    upsertSong({
      sourceId: 'src-1', sourceType: 'local', path: '/a.mp3', uri: 'file:///a.mp3', title: 'A',
    }, undefined)
    const songId = 'song:src-1:_2Fa.mp3'
    enqueueScrapeSongs([songId])
    expect(isInScrapeQueue(songId)).toBe(true)
    reconcileSourceSongs('src-1', [])
    // 队列存储里还有该 id，但 loadScrapeQueue 懒清理后应为空
    expect(loadScrapeQueue().items).toHaveLength(0)
  })
})
