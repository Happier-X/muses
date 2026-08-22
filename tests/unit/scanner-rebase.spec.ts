import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { SourceItem } from '@/features/sources/types'

/**
 * 批量扫描 rebase 提交单测（08-22-fix-scan-overwrite-lazy-lyrics）：
 * - 基线捕获后、扫描提交前，外部路径（播放器懒扫描）写入的歌词不被覆盖
 * - 新增/更新/skip 计数与对账删除语义不变
 * - 同一首歌双方都写入时，非冲突字段双侧保留
 * scanner.ts 顶层具名 import native/tags/webdav/sources-storage，统一 vi.mock；
 * storage.ts 用真实模块 + 内存 localStorage mock（同 library-local-first-migration.spec.ts）。
 */

const createMemStorage = () => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => (key in store ? store[key] : null),
    setItem: (key: string, value: string) => { store[key] = String(value) },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} },
  }
}

type MemStorage = ReturnType<typeof createMemStorage>

let memStorage: MemStorage

const stubGlobals = () => {
  memStorage = createMemStorage()
  const win = {
    localStorage: memStorage,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => true,
  }
  vi.stubGlobal('window', win)
  vi.stubGlobal('localStorage', memStorage)
}

stubGlobals()

// scanner 的原生/网络依赖全部 mock，避免引入 Capacitor 运行时
vi.mock('@/features/library/native', () => ({
  scanLocalAudioFiles: vi.fn(async () => []),
}))
vi.mock('@/features/library/tags', () => ({
  readLocalAudioTags: vi.fn(async () => ({})),
  readWebDavAudioTags: vi.fn(async () => ({})),
}))
vi.mock('@/features/sources/webdav', () => ({
  listWebDavAudioFiles: vi.fn(async () => []),
}))
vi.mock('@/features/sources/storage', () => ({
  getWebDavPassword: vi.fn(async () => 'password'),
}))

const { loadSongs, upsertSong } = await import('@/features/library/storage')
const { scanSourceLibrary } = await import('@/features/library/scanner')
const { scanLocalAudioFiles } = await import('@/features/library/native')
const { readLocalAudioTags } = await import('@/features/library/tags')

const MIGRATION_KEY = 'muses:migration:local-first-v1'
const SONGS_KEY = 'muses:songs'

const source: SourceItem = {
  id: 'src1',
  type: 'local',
  name: '本地音乐',
  path: 'content://tree/primary',
  createdAt: '2026-01-01T00:00:00Z',
}

const makeFile = (name: string) => ({ path: `/a/${name}`, uri: `file:///a/${name}`, name })

const makeSong = (overrides: Record<string, unknown> = {}): Record<string, unknown> => ({
  id: 's1',
  sourceId: source.id,
  sourceType: 'local',
  path: '/a/晴天.mp3',
  uri: 'file:///a/晴天.mp3',
  title: '晴天',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  ...overrides,
})

const seedLibrary = (songs: Record<string, unknown>[]) => {
  memStorage.setItem(SONGS_KEY, JSON.stringify(songs))
}

/** 模拟播放器懒扫描：读最新曲库 → upsert 写库落盘（与 controller 同路径） */
const lazyScanWriteLyrics = (path: string, lyrics: string) => {
  const song = loadSongs().find((item) => item.path === path)
  if (!song) {
    throw new Error(`lazyScanWriteLyrics: 找不到 ${path}`)
  }
  upsertSong(
    {
      sourceId: song.sourceId,
      sourceType: song.sourceType,
      path: song.path,
      uri: song.uri,
      title: song.title,
      tags: { lyrics, lyricsSource: 'embedded', lyricsFormat: 'lrc' },
    },
    loadSongs(),
  )
}

beforeEach(() => {
  stubGlobals()
  memStorage.setItem(MIGRATION_KEY, 'done')
  vi.mocked(scanLocalAudioFiles).mockReset()
  vi.mocked(scanLocalAudioFiles).mockResolvedValue([])
  vi.mocked(readLocalAudioTags).mockReset()
  vi.mocked(readLocalAudioTags).mockImplementation(async () => ({}))
})

describe('批量扫描 rebase 提交（方案 A）', () => {
  it('基线捕获后、提交前外部写入的歌词在扫描后仍存在（丢失更新回归）', async () => {
    seedLibrary([makeSong()])

    // 文件 A 已存在且扫描读到新 artist；文件 B 触发"懒扫描写歌词"后再返回
    vi.mocked(scanLocalAudioFiles).mockResolvedValue([makeFile('晴天.mp3'), makeFile('七里香.mp3')])
    vi.mocked(readLocalAudioTags).mockImplementation(async (file) => {
      if (file.path === '/a/晴天.mp3') {
        return { artist: '周杰伦' }
      }
      if (file.path === '/a/七里香.mp3') {
        // 模拟扫描进行中用户播放歌曲，播放器懒扫描把内嵌歌词写入曲库
        lazyScanWriteLyrics('/a/晴天.mp3', '[00:01.00]窗外的麻雀在电线杆上多嘴')
        return {}
      }
      return {}
    })

    const result = await scanSourceLibrary(source, { readTags: true })

    // 懒扫描写入的歌词未被扫描整体覆盖
    const song = result.songs.find((item) => item.path === '/a/晴天.mp3')!
    expect(song.lyrics).toBe('[00:01.00]窗外的麻雀在电线杆上多嘴')
    expect(song.lyricsSource).toBe('embedded')
    // 扫描自身的字段更新同样生效（rebase 重放基于含歌词的最新库）
    expect(song.artist).toBe('周杰伦')
    // 且已落盘
    const persisted = JSON.parse(memStorage.getItem(SONGS_KEY) ?? '[]') as Array<{ lyrics?: string }>
    expect(persisted.find((item) => item.path === '/a/晴天.mp3')?.lyrics).toBe('[00:01.00]窗外的麻雀在电线杆上多嘴')

    expect(result.summary.updated).toBe(1)
    expect(result.summary.inserted).toBe(1)
    void loadSongs
  })

  it('新增/更新/skip 计数与对账删除语义不变', async () => {
    // 库内已有：skipSong（与文件名兜底标题一致、无新字段 → 应 skip）、
    // staleSong（不在本次列表应对账删除）
    seedLibrary([
      makeSong({ path: '/a/skip.mp3', uri: 'file:///a/skip.mp3', title: 'skip' }),
      makeSong({ path: '/a/stale.mp3', uri: 'file:///a/stale.mp3', title: '旧歌' }),
    ])

    vi.mocked(scanLocalAudioFiles).mockResolvedValue([
      makeFile('new.mp3'),
      makeFile('skip.mp3'),
    ])
    vi.mocked(readLocalAudioTags).mockImplementation(async (file) => {
      if (file.path === '/a/new.mp3') {
        return { title: '新歌', artist: '歌手' }
      }
      return {}
    })

    const result = await scanSourceLibrary(source, { readTags: true })

    expect(result.summary.discovered).toBe(2)
    expect(result.summary.processed).toBe(2)
    expect(result.summary.inserted).toBe(1)
    expect(result.summary.skipped).toBe(1)
    expect(result.summary.removed).toBe(1)

    const paths = result.songs.map((song) => song.path)
    expect(paths).toContain('/a/new.mp3')
    expect(paths).toContain('/a/skip.mp3')
    expect(paths).not.toContain('/a/stale.mp3')

    // 有变化时已统一落盘
    const persisted = JSON.parse(memStorage.getItem(SONGS_KEY) ?? '[]') as Array<{ path: string }>
    expect(persisted.map((song) => song.path).sort()).toEqual(['/a/new.mp3', '/a/skip.mp3'])
  })

  it('无变化时不重复落盘', async () => {
    const stored = JSON.stringify([makeSong()])
    memStorage.setItem(SONGS_KEY, stored)

    vi.mocked(scanLocalAudioFiles).mockResolvedValue([makeFile('晴天.mp3')])

    const result = await scanSourceLibrary(source, { readTags: true })

    expect(result.summary.skipped).toBe(1)
    expect(result.summary.inserted).toBe(0)
    expect(result.summary.updated).toBe(0)
    expect(result.summary.removed).toBe(0)
    expect(memStorage.getItem(SONGS_KEY)).toBe(stored)
  })

  it('同一首歌双方都写入时，非冲突字段双侧保留', async () => {
    // 库内歌曲只有标题；扫描带 album，外部懒扫描带歌词 + duration
    seedLibrary([makeSong({ path: '/a/稻香.mp3', uri: 'file:///a/稻香.mp3', title: '稻香' })])

    vi.mocked(scanLocalAudioFiles).mockResolvedValue([makeFile('稻香.mp3'), makeFile('other.mp3')])
    vi.mocked(readLocalAudioTags).mockImplementation(async (file) => {
      if (file.path === '/a/other.mp3') {
        // 扫描过程中懒扫描先写入歌词与时长
        lazyScanWriteLyrics('/a/稻香.mp3', '[00:01.00]对这个世界如果你有太多的抱怨')
        upsertSong(
          {
            sourceId: source.id,
            sourceType: 'local',
            path: '/a/稻香.mp3',
            uri: 'file:///a/稻香.mp3',
            title: '稻香',
            tags: { duration: 223 },
          },
          loadSongs(),
        )
        return {}
      }
      if (file.path === '/a/稻香.mp3') {
        return { album: '魔杰座' }
      }
      return {}
    })

    const result = await scanSourceLibrary(source, { readTags: true })

    const song = result.songs.find((item) => item.path === '/a/稻香.mp3')!
    // 外部写入侧
    expect(song.lyrics).toBe('[00:01.00]对这个世界如果你有太多的抱怨')
    expect(song.duration).toBe(223)
    // 扫描写入侧
    expect(song.album).toBe('魔杰座')
    expect(song.title).toBe('稻香')
    expect(result.summary.updated).toBe(1)
  })
})
