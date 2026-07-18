import { afterEach, describe, expect, test, vi } from 'vitest'
import type { SourceItem } from '@/features/sources/types'
import { getTitleFromPath, isSupportedAudioFile } from '@/features/library/audio'
import { loadSongs, reconcileSourceSongs, upsertSong } from '@/features/library/storage'
import { scanSourceLibrary } from '@/features/library/scanner'

vi.mock('@aparajita/capacitor-secure-storage', () => ({
  SecureStorage: {
    get: vi.fn(),
    set: vi.fn(),
    remove: vi.fn(),
  },
}))

vi.mock('@capacitor/core', () => ({
  registerPlugin: vi.fn((name: string) => {
    if (name === 'LocalLibrary') {
      return {
        scanDirectory: vi.fn(),
        readMetadata: vi.fn(),
      }
    }

    return {
      propfind: vi.fn(),
      readMetadata: vi.fn(),
    }
  }),
}))

describe('歌曲库持久化', () => {
  afterEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  test('按 sourceId + path 去重，重复扫描同一路径不会插入重复歌曲', () => {
    const first = upsertSong({
      sourceId: 'source-1',
      sourceType: 'local',
      path: 'album/song.mp3',
      uri: 'content://song',
      title: 'song',
      now: '2026-07-07T00:00:00.000Z',
    })
    const second = upsertSong(
      {
        sourceId: 'source-1',
        sourceType: 'local',
        path: 'album/song.mp3',
        uri: 'content://song',
        title: 'song',
        now: '2026-07-07T00:01:00.000Z',
      },
      first.songs,
    )

    expect(first.status).toBe('inserted')
    expect(second.status).toBe('skipped')
    expect(loadSongs()).toHaveLength(1)
  })

  test('兼容旧记录并持久化歌词、封面引用和标签扫描状态', () => {
    localStorage.setItem(
      'muses:songs',
      JSON.stringify([
        {
          id: 'old-song',
          sourceId: 'source-1',
          sourceType: 'local',
          path: 'album/old.mp3',
          uri: 'content://old',
          title: '旧歌曲',
          createdAt: '2026-07-07T00:00:00.000Z',
          updatedAt: '2026-07-07T00:00:00.000Z',
        },
      ]),
    )

    expect(loadSongs()).toHaveLength(1)

    const result = upsertSong({
      sourceId: 'source-1',
      sourceType: 'local',
      path: 'album/new.mp3',
      uri: 'content://new',
      title: 'new',
      tags: {
        lyrics: '[00:01.00]一句歌词',
        lyricsSource: 'embedded',
        coverUri: 'file:///cache/covers/new.jpg',
        tagsScanned: true,
        tagsScannedAt: '2026-07-07T00:03:00.000Z',
      },
      now: '2026-07-07T00:03:00.000Z',
    })

    expect(result.song).toMatchObject({
      lyrics: '[00:01.00]一句歌词',
      lyricsSource: 'embedded',
      coverUri: 'file:///cache/covers/new.jpg',
      tagsScanned: true,
      tagsScannedAt: '2026-07-07T00:03:00.000Z',
    })
  })

  test('部分标签更新不会清空已有可选元数据', () => {
    const first = upsertSong({
      sourceId: 'source-1',
      sourceType: 'webdav',
      path: '/song.mp3',
      uri: 'https://example.com/song.mp3',
      title: '旧标题',
      tags: {
        artist: '旧歌手',
        album: '旧专辑',
        lyrics: '[00:01.00]旧歌词',
        lyricsSource: 'embedded',
        coverUri: 'file:///cache/covers/old.jpg',
        tagsScanned: true,
        tagsScannedAt: '2026-07-07T00:00:00.000Z',
        metadataVersion: 1,
      },
      now: '2026-07-07T00:00:00.000Z',
    })

    const second = upsertSong(
      {
        sourceId: 'source-1',
        sourceType: 'webdav',
        path: '/song.mp3',
        uri: 'https://example.com/song.mp3',
        title: '文件标题',
        tags: {
          title: '新标题',
          duration: 180,
          tagsScanned: true,
          tagsScannedAt: '2026-07-07T00:02:00.000Z',
          metadataVersion: 2,
        },
        now: '2026-07-07T00:02:00.000Z',
      },
      first.songs,
    )

    expect(second.song).toMatchObject({
      title: '新标题',
      artist: '旧歌手',
      album: '旧专辑',
      duration: 180,
      lyrics: '[00:01.00]旧歌词',
      lyricsSource: 'embedded',
      coverUri: 'file:///cache/covers/old.jpg',
      tagsScanned: true,
      metadataVersion: 2,
    })
  })

  test('可写入 online 歌词与 lyricsFormat', () => {
    const result = upsertSong({
      sourceId: 'source-1',
      sourceType: 'local',
      path: 'album/online.mp3',
      uri: 'content://online',
      title: 'online',
      tags: {
        lyrics: '[00:01.00]在线',
        lyricsSource: 'online',
        lyricsFormat: 'yrc',
      },
      now: '2026-07-13T00:00:00.000Z',
    })

    expect(result.song).toMatchObject({
      lyrics: '[00:01.00]在线',
      lyricsSource: 'online',
      lyricsFormat: 'yrc',
    })
    expect(loadSongs()[0]).toMatchObject({
      lyricsSource: 'online',
      lyricsFormat: 'yrc',
    })
  })

  test('不会把 data URL 封面写入歌曲库', () => {
    upsertSong({
      sourceId: 'source-1',
      sourceType: 'local',
      path: 'album/song.mp3',
      uri: 'content://song',
      title: 'song',
      tags: { coverUri: 'data:image/jpeg;base64,secret-cover' },
      now: '2026-07-07T00:00:00.000Z',
    })

    expect(localStorage.getItem('muses:songs')).not.toContain('data:image')
    expect(loadSongs()[0].coverUri).toBeUndefined()
  })

  test('同一路径标签变化时更新原歌曲记录并保留创建时间', () => {
    const first = upsertSong({
      sourceId: 'source-1',
      sourceType: 'webdav',
      path: '/song.flac',
      uri: 'https://example.com/song.flac',
      title: 'song',
      now: '2026-07-07T00:00:00.000Z',
    })
    const second = upsertSong(
      {
        sourceId: 'source-1',
        sourceType: 'webdav',
        path: '/song.flac',
        uri: 'https://example.com/song.flac',
        title: 'song',
        tags: { title: '新标题', artist: '歌手' },
        now: '2026-07-07T00:02:00.000Z',
      },
      first.songs,
    )

    expect(second.status).toBe('updated')
    expect(loadSongs()).toMatchObject([
      {
        title: '新标题',
        artist: '歌手',
        createdAt: '2026-07-07T00:00:00.000Z',
        updatedAt: '2026-07-07T00:02:00.000Z',
      },
    ])
  })

  test('对账会移除该音源下不在 keepPaths 的歌曲，并保留其他音源', () => {
    upsertSong({
      sourceId: 'source-a',
      sourceType: 'local',
      path: 'a/keep.mp3',
      uri: 'content://a-keep',
      title: 'keep',
      now: '2026-07-18T00:00:00.000Z',
    })
    upsertSong({
      sourceId: 'source-a',
      sourceType: 'local',
      path: 'a/gone.mp3',
      uri: 'content://a-gone',
      title: 'gone',
      now: '2026-07-18T00:00:00.000Z',
    })
    upsertSong({
      sourceId: 'source-b',
      sourceType: 'local',
      path: 'b/other.mp3',
      uri: 'content://b-other',
      title: 'other',
      now: '2026-07-18T00:00:00.000Z',
    })

    const result = reconcileSourceSongs('source-a', ['a/keep.mp3'])

    expect(result.removed).toBe(1)
    expect(result.songs.map((song) => song.path).sort()).toEqual(['a/keep.mp3', 'b/other.mp3'])
    expect(loadSongs().map((song) => song.path).sort()).toEqual(['a/keep.mp3', 'b/other.mp3'])
  })

  test('keepPaths 为空时清空该音源全部歌曲', () => {
    upsertSong({
      sourceId: 'source-a',
      sourceType: 'local',
      path: 'a/one.mp3',
      uri: 'content://a-one',
      title: 'one',
      now: '2026-07-18T00:00:00.000Z',
    })
    upsertSong({
      sourceId: 'source-b',
      sourceType: 'webdav',
      path: '/b/two.mp3',
      uri: 'https://example.com/b/two.mp3',
      title: 'two',
      now: '2026-07-18T00:00:00.000Z',
    })

    const result = reconcileSourceSongs('source-a', [])

    expect(result.removed).toBe(1)
    expect(result.songs).toHaveLength(1)
    expect(result.songs[0]).toMatchObject({ sourceId: 'source-b', path: '/b/two.mp3' })
    expect(loadSongs()).toHaveLength(1)
  })
})

describe('音频文件工具', () => {
  test('识别支持的音频扩展名并从文件名回退标题', () => {
    expect(isSupportedAudioFile('/music/Track 01.MP3')).toBe(true)
    expect(isSupportedAudioFile('/music/cover.jpg')).toBe(false)
    expect(isSupportedAudioFile('/music/no-extension')).toBe(false)
    expect(getTitleFromPath('/music/Track 01.MP3')).toBe('Track 01')
  })
})

describe('扫描器摘要', () => {
  afterEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  test('标签读取失败时回退文件名并计入降级摘要，不中断扫描', async () => {
    const { LocalLibraryNative } = await import('@/features/library/native')
    vi.mocked(LocalLibraryNative.scanDirectory).mockResolvedValue({
      files: [
        { path: 'album/good.mp3', uri: 'content://good', name: 'good.mp3' },
        { path: 'album/bad.flac', uri: 'content://bad', name: 'bad.flac' },
      ],
    })
    vi.mocked(LocalLibraryNative.readMetadata)
      .mockResolvedValueOnce({ title: '真实标题', artist: '真实歌手' })
      .mockRejectedValueOnce(new Error('无法解析'))

    const source: SourceItem = {
      id: 'local-source',
      type: 'local',
      name: '本地音乐',
      path: 'content://tree/music',
      createdAt: '2026-07-07T00:00:00.000Z',
    }

    const result = await scanSourceLibrary(source, { readTags: true })

    expect(result.summary).toMatchObject({ discovered: 2, processed: 2, inserted: 2, degraded: 1, failed: 0 })
    expect(loadSongs().map((song) => song.title).sort()).toEqual(['bad', '真实标题'])
  })

  test('WebDAV 扫描通过安全存储密码读取标签且歌曲库不保存密码', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    const { WebDavNative } = await import('@/features/sources/webdav')
    vi.mocked(SecureStorage.get).mockResolvedValue('secret-password')
    vi.mocked(WebDavNative.propfind).mockResolvedValue({
      status: 207,
      data: `<?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
          <d:response><d:href>/dav/music/</d:href><d:propstat><d:prop><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat></d:response>
          <d:response><d:href>/dav/music/song.mp3</d:href><d:propstat><d:prop><d:displayname>song.mp3</d:displayname><d:resourcetype /></d:prop></d:propstat></d:response>
        </d:multistatus>`,
    })
    vi.mocked(WebDavNative.readMetadata).mockResolvedValue({ title: '远程标题', album: '远程专辑' })

    const source: SourceItem = {
      id: 'webdav-source',
      type: 'webdav',
      name: '远程音乐',
      serverUrl: 'https://example.com/dav',
      username: 'alice',
      path: '/music',
      credentialKey: 'muses:webdav-password:webdav-source',
      createdAt: '2026-07-07T00:00:00.000Z',
    }

    const result = await scanSourceLibrary(source, { readTags: true })

    expect(result.summary).toMatchObject({ discovered: 1, processed: 1, inserted: 1, degraded: 0, failed: 0, removed: 0 })
    expect(SecureStorage.get).toHaveBeenCalledWith('muses:webdav-password:webdav-source')
    expect(WebDavNative.readMetadata).toHaveBeenCalledWith({
      url: 'https://example.com/dav/music/song.mp3',
      username: 'alice',
      password: 'secret-password',
      songId: '/music/song.mp3',
    })
    expect(localStorage.getItem('muses:songs')).not.toContain('secret-password')
    expect(loadSongs()).toMatchObject([{ title: '远程标题', album: '远程专辑' }])
  })

  test('空扫描会清除该音源旧歌曲，并保留其他音源', async () => {
    const { LocalLibraryNative } = await import('@/features/library/native')
    upsertSong({
      sourceId: 'local-source',
      sourceType: 'local',
      path: 'album/old.mp3',
      uri: 'content://old',
      title: 'old',
      now: '2026-07-18T00:00:00.000Z',
    })
    upsertSong({
      sourceId: 'other-source',
      sourceType: 'local',
      path: 'other/keep.mp3',
      uri: 'content://other',
      title: 'keep',
      now: '2026-07-18T00:00:00.000Z',
    })

    vi.mocked(LocalLibraryNative.scanDirectory).mockResolvedValue({ files: [] })

    const source: SourceItem = {
      id: 'local-source',
      type: 'local',
      name: '本地音乐',
      path: 'content://tree/music',
      createdAt: '2026-07-18T00:00:00.000Z',
    }

    const result = await scanSourceLibrary(source, { readTags: false })

    expect(result.summary).toMatchObject({
      discovered: 0,
      processed: 0,
      inserted: 0,
      removed: 1,
    })
    expect(loadSongs()).toMatchObject([{ sourceId: 'other-source', path: 'other/keep.mp3' }])
  })

  test('再次扫描文件减少时仅移除该音源未再出现的路径', async () => {
    const { LocalLibraryNative } = await import('@/features/library/native')
    upsertSong({
      sourceId: 'local-source',
      sourceType: 'local',
      path: 'album/keep.mp3',
      uri: 'content://keep',
      title: 'keep',
      now: '2026-07-18T00:00:00.000Z',
    })
    upsertSong({
      sourceId: 'local-source',
      sourceType: 'local',
      path: 'album/gone.mp3',
      uri: 'content://gone',
      title: 'gone',
      now: '2026-07-18T00:00:00.000Z',
    })
    upsertSong({
      sourceId: 'other-source',
      sourceType: 'local',
      path: 'other/stay.mp3',
      uri: 'content://stay',
      title: 'stay',
      now: '2026-07-18T00:00:00.000Z',
    })

    vi.mocked(LocalLibraryNative.scanDirectory).mockResolvedValue({
      files: [{ path: 'album/keep.mp3', uri: 'content://keep', name: 'keep.mp3' }],
    })

    const source: SourceItem = {
      id: 'local-source',
      type: 'local',
      name: '本地音乐',
      path: 'content://tree/music',
      createdAt: '2026-07-18T00:00:00.000Z',
    }

    const result = await scanSourceLibrary(source, { readTags: false })

    expect(result.summary).toMatchObject({
      discovered: 1,
      processed: 1,
      removed: 1,
    })
    expect(loadSongs().map((song) => song.path).sort()).toEqual(['album/keep.mp3', 'other/stay.mp3'])
  })

  test('发现阶段失败时不清理旧歌曲', async () => {
    const { LocalLibraryNative } = await import('@/features/library/native')
    upsertSong({
      sourceId: 'local-source',
      sourceType: 'local',
      path: 'album/old.mp3',
      uri: 'content://old',
      title: 'old',
      now: '2026-07-18T00:00:00.000Z',
    })

    vi.mocked(LocalLibraryNative.scanDirectory).mockRejectedValue(new Error('无法列出目录'))

    const source: SourceItem = {
      id: 'local-source',
      type: 'local',
      name: '本地音乐',
      path: 'content://tree/music',
      createdAt: '2026-07-18T00:00:00.000Z',
    }

    await expect(scanSourceLibrary(source, { readTags: false })).rejects.toThrow('无法列出目录')
    expect(loadSongs()).toHaveLength(1)
    expect(loadSongs()[0]).toMatchObject({ sourceId: 'local-source', path: 'album/old.mp3' })
  })
})
