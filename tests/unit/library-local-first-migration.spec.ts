import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * local-first-v1 一次性迁移单测（R6）：
 * - 存量 lyricsSource='online' 歌词清除
 * - 存量 metaSources.*='cloud' 字段值与标记清除
 * - userEditedFields 保护字段跳过
 * - localStorage 打标后只执行一次
 * 另覆盖 'scrape' 来源标记的 sanitize 往返（R5）。
 * 项目无 jsdom/happy-dom 依赖，用内存 localStorage mock 覆盖全局。
 */

const createMemStorage = () => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => (key in store ? store[key] : null),
    setItem: (key: string, value: string) => { store[key] = String(value) },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} },
    _dump: () => store,
  }
}

type MemStorage = ReturnType<typeof createMemStorage>

let memStorage: MemStorage
const dispatchedEvents: string[] = []

const stubWindow = () => {
  memStorage = createMemStorage()
  const win = {
    localStorage: memStorage,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: (event: Event) => {
      dispatchedEvents.push((event as unknown as { type: string }).type)
      return true
    },
  }
  vi.stubGlobal('window', win)
  vi.stubGlobal('localStorage', memStorage)
}

stubWindow()

const { loadSongs, saveSongs, SONGS_UPDATED_EVENT } = await import('@/features/library/storage')
const MIGRATION_KEY = 'muses:migration:local-first-v1'

const makeSong = (overrides: Record<string, unknown> = {}): Record<string, unknown> => ({
  id: 's1',
  sourceId: 'src1',
  sourceType: 'local',
  path: '/a/晴天.mp3',
  uri: 'file:///a/晴天.mp3',
  title: '晴天',
  artist: '周杰伦',
  album: '叶惠美',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  ...overrides,
})

const seedLibrary = (songs: Record<string, unknown>[]) => {
  memStorage.setItem('muses:songs', JSON.stringify(songs))
}

beforeEach(() => {
  stubWindow()
  dispatchedEvents.length = 0
})

describe('local-first-v1 存量清理迁移 (R6)', () => {
  it('lyricsSource=online 的歌词整体清除', () => {
    seedLibrary([
      makeSong({ lyrics: '[00:00]在线词', lyricsFormat: 'lrc', lyricsSource: 'online' }),
    ])

    const songs = loadSongs()

    expect(songs).toHaveLength(1)
    expect(songs[0].lyrics).toBeUndefined()
    expect(songs[0].lyricsFormat).toBeUndefined()
    expect(songs[0].lyricsSource).toBeUndefined()
  })

  it('metaSources.cloud 补缺字段值与标记一并清除（cover 同时清 coverUri）', () => {
    seedLibrary([
      makeSong({
        title: '在线标题',
        artist: '在线歌手',
        album: '在线专辑',
        coverUri: 'file:///cache/covers/x.jpg',
        metaSources: { title: 'cloud', artist: 'cloud', album: 'cloud', cover: 'cloud' },
      }),
    ])

    const songs = loadSongs()

    expect(songs[0].title).toBe('晴天') // 文件名兑底标题（path: /a/晴天.mp3）
    expect(songs[0].artist).toBeUndefined()
    expect(songs[0].album).toBeUndefined()
    expect(songs[0].coverUri).toBeUndefined()
    expect(songs[0].metaSources).toBeUndefined()
  })

  it('userEditedFields 命中的字段跳过清理（manual 保护优先）', () => {
    seedLibrary([
      makeSong({
        title: '手改标题',
        lyrics: '[00:00]在线词',
        lyricsSource: 'online',
        userEditedFields: ['title', 'lyrics'],
        metaSources: { title: 'cloud' },
      }),
    ])

    const songs = loadSongs()

    expect(songs[0].title).toBe('手改标题')
    expect(songs[0].lyrics).toBe('[00:00]在线词')
    expect(songs[0].lyricsSource).toBe('online')
    // 受保护字段的 cloud 标记原样保留（不清理也不误删）
    expect(songs[0].metaSources).toEqual({ title: 'cloud' })
  })

  it('embedded/sidecar/scrape 歌词与 embedded 字段不受影响', () => {
    seedLibrary([
      makeSong({ id: 'a', lyrics: '[00:00]内嵌', lyricsSource: 'embedded', lyricsFormat: 'lrc' }),
      makeSong({ id: 'b', path: '/a/b.mp3', uri: 'file:///a/b.mp3', lyrics: '[00:00]sidecar', lyricsSource: 'sidecar' }),
      makeSong({ id: 'c', path: '/a/c.mp3', uri: 'file:///a/c.mp3', lyrics: '[00:00]刮削', lyricsSource: 'scrape', metaSources: { title: 'scrape' } }),
      makeSong({ id: 'd', path: '/a/d.mp3', uri: 'file:///a/d.mp3', metaSources: { title: 'embedded' } }),
    ])

    const songs = loadSongs()

    expect(songs.find((s) => s.id === 'a')!.lyrics).toBe('[00:00]内嵌')
    expect(songs.find((s) => s.id === 'b')!.lyricsSource).toBe('sidecar')
    expect(songs.find((s) => s.id === 'c')!.lyricsSource).toBe('scrape')
    expect(songs.find((s) => s.id === 'c')!.metaSources).toEqual({ title: 'scrape' })
    expect(songs.find((s) => s.id === 'd')!.title).toBe('晴天')
  })

  it('有变化时写库并广播 SONGS_UPDATED_EVENT；打标只执行一次', () => {
    seedLibrary([
      makeSong({ lyrics: '[00:00]在线词', lyricsSource: 'online' }),
    ])

    void loadSongs()
    expect(dispatchedEvents.filter((e) => e === SONGS_UPDATED_EVENT)).toHaveLength(1)
    expect(memStorage.getItem(MIGRATION_KEY)).toBe('done')

    // 二次加载：不再触发写库/广播，且残留数据不会被误改
    dispatchedEvents.length = 0
    memStorage.setItem('muses:songs', JSON.stringify([
      makeSong({ lyrics: '[00:00]在线词', lyricsSource: 'online' }),
    ]))
    const songs = loadSongs()
    expect(dispatchedEvents).toHaveLength(0)
    expect(songs[0].lyricsSource).toBe('online')
  })

  it('无存量数据时不写库不广播，但仍打标', () => {
    seedLibrary([makeSong()])

    void loadSongs()

    expect(dispatchedEvents).toHaveLength(0)
    expect(memStorage.getItem(MIGRATION_KEY)).toBe('done')
  })
})

describe("'scrape' 来源标记 sanitize 往返 (R5)", () => {
  beforeEach(() => {
    // 预置已打标，绕过迁移干扰
    memStorage.setItem(MIGRATION_KEY, 'done')
  })

  it('saveSongs 写入 scrape 标记后 loadSongs 原样读回', () => {
    const song = makeSong({
      lyrics: '[00:00]刮削词',
      lyricsSource: 'scrape',
      lyricsFormat: 'lrc',
      metaSources: { title: 'scrape', artist: 'scrape' },
    }) as never

    saveSongs([song])
    const songs = loadSongs()

    expect(songs).toHaveLength(1)
    expect(songs[0].lyricsSource).toBe('scrape')
    expect(songs[0].metaSources).toEqual({ title: 'scrape', artist: 'scrape' })
  })
})
