import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { applyScrapeChanges } from '@/features/scrape/writeback'
import type { ScrapeCandidate } from '@/features/scrape/matcher'
import type { ScrapeChanges } from '@/features/scrape/writeback'
import type { SourceItem } from '@/features/sources/types'
import { buildWebDavUrl } from '@/features/sources/webdav'

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

// Mock native write（本地路径不在此测，仅占位）
vi.mock('@/features/library/native', () => ({
  writeLocalAudioMetadata: vi.fn(async () => ({ ok: true })),
}))

// Mock WebDAV 写入；buildWebDavUrl 用真实实现，保证地址断言与读取链路一致
vi.mock('@/features/sources/webdav', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/sources/webdav')>()
  return {
    ...actual,
    writeWebDavAudioMetadata: vi.fn(async () => ({ ok: true })),
  }
})

// Mock sources storage
const mockSources: SourceItem[] = []
const mockPasswords = new Map<string, string>()
vi.mock('@/features/sources/storage', () => ({
  loadSources: vi.fn(() => [...mockSources]),
  getWebDavPassword: vi.fn(async (credentialKey: string) => mockPasswords.get(credentialKey) ?? null),
}))

// Mock player/native (cacheRemoteCover)
vi.mock('@/features/player/native', () => ({
  cacheRemoteCover: vi.fn(async () => undefined),
}))

const makeWebDavSource = (
  id: string,
  overrides: Partial<Extract<SourceItem, { type: 'webdav' }>> = {},
): Extract<SourceItem, { type: 'webdav' }> => ({
  id,
  type: 'webdav',
  name: `源-${id}`,
  serverUrl: `https://dav.example.com/${id}`,
  username: `user-${id}`,
  path: '/',
  credentialKey: `cred-${id}`,
  createdAt: '2026-01-01T00:00:00Z',
  ...overrides,
})

const makeWebDavSong = (id: string, sourceId: string): Record<string, unknown> => ({
  id,
  title: '晴天',
  artist: undefined,
  album: undefined,
  coverUri: undefined,
  lyrics: undefined,
  lyricsFormat: undefined,
  lyricsSource: undefined,
  metaSources: {},
  sourceType: 'webdav',
  sourceId,
  // 含中文与空格的路径，验证 encodePath 行为
  path: `/音乐/晴 天.mp3`,
  uri: `https://dav.example.com/${sourceId}/音乐/晴 天.mp3`,
  updatedAt: '2026-01-01T00:00:00Z',
})

beforeEach(() => {
  mockSongs.length = 0
  mockSources.length = 0
  mockPasswords.clear()
  memStorage.clear()
})

afterEach(() => {
  vi.clearAllMocks()
})

const makeCandidate = (song: Record<string, unknown>): ScrapeCandidate => ({
  songId: song.id as string,
  song: song as never,
  text: { current: {}, candidates: [], defaultIndex: -1 },
  cover: { currentUri: undefined, candidates: [], defaultIndex: -1 },
  lyrics: { currentText: undefined, currentFormat: undefined, candidates: [], defaultIndex: -1 },
  overallConfidence: 'high',
  defaultChecked: true,
})

describe('writeWebDavFile（刮削回写 WebDAV 目标地址）', () => {
  it('url = serverUrl + encodePath(song.path)，与读取链路一致', async () => {
    const source = makeWebDavSource('src-a')
    mockSources.push(source)
    mockPasswords.set(source.credentialKey, 'secret')
    const song = makeWebDavSong('s1', 'src-a')
    mockSongs.push(song)

    await applyScrapeChanges(
      [makeCandidate(song)],
      new Set(['s1']),
      new Map<string, ScrapeChanges>([['s1', { title: '晴天' }]]),
    )

    const { writeWebDavAudioMetadata } = await import('@/features/sources/webdav')
    expect(writeWebDavAudioMetadata).toHaveBeenCalledTimes(1)
    const options = vi.mocked(writeWebDavAudioMetadata).mock.calls[0][0]
    expect(options.url).toBe(buildWebDavUrl(source.serverUrl, song.path as string))
    expect(options.url).toBe(`${source.serverUrl}/%E9%9F%B3%E4%B9%90/%E6%99%B4%20%E5%A4%A9.mp3`)
  })

  it('多个 WebDAV 源时按 song.sourceId 选源（用户名取所属源）', async () => {
    const sourceA = makeWebDavSource('src-a')
    const sourceB = makeWebDavSource('src-b')
    mockSources.push(sourceA, sourceB)
    mockPasswords.set(sourceB.credentialKey, 'pw-b')
    const song = makeWebDavSong('s1', 'src-b')
    mockSongs.push(song)

    const { results } = await applyScrapeChanges(
      [makeCandidate(song)],
      new Set(['s1']),
      new Map<string, ScrapeChanges>([['s1', { title: '晴天' }]]),
    )

    const { writeWebDavAudioMetadata } = await import('@/features/sources/webdav')
    const options = vi.mocked(writeWebDavAudioMetadata).mock.calls[0][0]
    expect(options.username).toBe('user-src-b')
    expect(results[0].status).toBe('success')
  })

  it('密码取自所属源的 credentialKey', async () => {
    const sourceA = makeWebDavSource('src-a')
    const sourceB = makeWebDavSource('src-b')
    mockSources.push(sourceA, sourceB)
    mockPasswords.set(sourceA.credentialKey, 'pw-a')
    mockPasswords.set(sourceB.credentialKey, 'pw-b')
    const song = makeWebDavSong('s1', 'src-b')
    mockSongs.push(song)

    await applyScrapeChanges(
      [makeCandidate(song)],
      new Set(['s1']),
      new Map<string, ScrapeChanges>([['s1', { title: '晴天' }]]),
    )

    const { getWebDavPassword } = await import('@/features/sources/storage')
    expect(getWebDavPassword).toHaveBeenCalledWith('cred-src-b')
    const { writeWebDavAudioMetadata } = await import('@/features/sources/webdav')
    expect(vi.mocked(writeWebDavAudioMetadata).mock.calls[0][0].password).toBe('pw-b')
  })

  it('sourceId 无匹配音源 → no_password + 引导文案，且不调用原生写入', async () => {
    mockSources.push(makeWebDavSource('src-a'))
    const song = makeWebDavSong('s1', 'src-missing')
    mockSongs.push(song)

    const { results } = await applyScrapeChanges(
      [makeCandidate(song)],
      new Set(['s1']),
      new Map<string, ScrapeChanges>([['s1', { title: '晴天' }]]),
    )

    expect(results[0].status).toBe('file-failed')
    expect(results[0].fileResult).toEqual({
      ok: false,
      code: 'no_password',
      message: '未找到歌曲所属的 WebDAV 音源，请重新扫描后重试。',
    })
    const { writeWebDavAudioMetadata } = await import('@/features/sources/webdav')
    expect(writeWebDavAudioMetadata).not.toHaveBeenCalled()
  })

  it('sourceId 指向非 WebDAV 音源 → 同样返回 no_password 引导文案', async () => {
    mockSources.push(makeWebDavSource('src-a'))
    mockSources.push({
      id: 'src-local',
      type: 'local',
      name: '本地源',
      path: '/music',
      createdAt: '2026-01-01T00:00:00Z',
    })
    const song = makeWebDavSong('s1', 'src-local')
    mockSongs.push(song)

    const { results } = await applyScrapeChanges(
      [makeCandidate(song)],
      new Set(['s1']),
      new Map<string, ScrapeChanges>([['s1', { title: '晴天' }]]),
    )

    expect(results[0].fileResult.code).toBe('no_password')
    expect(results[0].fileResult.message).toBe('未找到歌曲所属的 WebDAV 音源，请重新扫描后重试。')
  })

  it('密码未配置 → no_password 提示补密码', async () => {
    const source = makeWebDavSource('src-a')
    mockSources.push(source)
    const song = makeWebDavSong('s1', 'src-a')
    mockSongs.push(song)

    const { results } = await applyScrapeChanges(
      [makeCandidate(song)],
      new Set(['s1']),
      new Map<string, ScrapeChanges>([['s1', { title: '晴天' }]]),
    )

    expect(results[0].fileResult).toEqual({
      ok: false,
      code: 'no_password',
      message: 'WebDAV 密码未配置。',
    })
  })

  it('回写成功后曲库值仍正常入库（journal 契约不变）', async () => {
    const source = makeWebDavSource('src-a')
    mockSources.push(source)
    mockPasswords.set(source.credentialKey, 'secret')
    const song = makeWebDavSong('s1', 'src-a')
    mockSongs.push(song)

    await applyScrapeChanges(
      [makeCandidate(song)],
      new Set(['s1']),
      new Map<string, ScrapeChanges>([['s1', { title: '晴天 (新)' }]]),
    )

    expect(mockSongs.find((s) => s.id === 's1')!.title).toBe('晴天 (新)')
    expect(mockSongs.find((s) => s.id === 's1')!.metaSources).toEqual({ title: 'embedded' })
  })
})
