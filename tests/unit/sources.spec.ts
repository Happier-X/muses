import { afterEach, describe, expect, test, vi } from 'vitest'
import {
  createSourceId,
  deleteSource,
  getWebDavPasswordKey,
  loadSources,
  saveSources,
  saveWebDavPassword,
  updateSource,
} from '@/features/sources/storage'
import type { SourceItem, WebDavConnectionInput } from '@/features/sources/types'
import {
  buildWebDavUrl,
  getParentWebDavPath,
  getWebDavDisplayName,
  listWebDavAudioFiles,
  listWebDavDirectories,
  normalizeWebDavPath,
  parseWebDavEntries,
} from '@/features/sources/webdav'

vi.mock('@aparajita/capacitor-secure-storage', () => ({
  SecureStorage: {
    set: vi.fn(),
    get: vi.fn(),
    remove: vi.fn(),
  },
}))

vi.mock('@capacitor/core', () => ({
  registerPlugin: vi.fn(() => ({
    propfind: vi.fn(),
    readMetadata: vi.fn(),
  })),
}))

describe('音源存储', () => {
  afterEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  test('只把 WebDAV 元数据保存到 localStorage，不保存密码', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    const sourceId = createSourceId()
    const credentialKey = getWebDavPasswordKey(sourceId)
    const source: SourceItem = {
      id: sourceId,
      type: 'webdav',
      name: '音乐',
      serverUrl: 'https://example.com/dav',
      username: 'alice',
      path: '/音乐',
      credentialKey,
      createdAt: '2026-07-06T00:00:00.000Z',
    }

    await saveWebDavPassword(credentialKey, 'secret-password')
    saveSources([source])

    expect(SecureStorage.set).toHaveBeenCalledWith(credentialKey, 'secret-password')
    expect(localStorage.getItem('muses:sources')).not.toContain('secret-password')
    expect(loadSources()).toEqual([source])
  })

  test('忽略 localStorage 中的无效音源记录', () => {
    localStorage.setItem(
      'muses:sources',
      JSON.stringify([
        { id: 'local-1', type: 'local', name: '本地音乐', path: '/music', createdAt: '2026-07-06T00:00:00.000Z' },
        { id: 'invalid-1', type: 'webdav', name: '缺少凭据', path: '/dav', createdAt: '2026-07-06T00:00:00.000Z' },
      ]),
    )

    expect(loadSources()).toEqual([
      { id: 'local-1', type: 'local', name: '本地音乐', path: '/music', createdAt: '2026-07-06T00:00:00.000Z' },
    ])
  })

  test('编辑本地音源保留身份字段、顺序和其他音源', async () => {
    const original: SourceItem = {
      id: 'local-1',
      type: 'local',
      name: '旧名称',
      path: '/old',
      createdAt: '2026-07-18T00:00:00.000Z',
    }
    const other: SourceItem = {
      id: 'local-2',
      type: 'local',
      name: '其他音源',
      path: '/other',
      createdAt: '2026-07-18T01:00:00.000Z',
    }

    const result = await updateSource(original.id, { name: '新名称', path: '/new' }, [original, other])

    expect(result.updated).toEqual({ ...original, name: '新名称', path: '/new' })
    expect(result.sources).toEqual([{ ...original, name: '新名称', path: '/new' }, other])
    expect(loadSources()).toEqual(result.sources)
  })

  test('编辑 WebDAV 非敏感字段且密码留空时不写安全存储', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    const source: SourceItem = {
      id: 'webdav-1',
      type: 'webdav',
      name: '旧名称',
      serverUrl: 'https://old.example.com/dav',
      username: 'alice',
      path: '/old',
      credentialKey: 'credential-1',
      createdAt: '2026-07-18T00:00:00.000Z',
    }

    const result = await updateSource(
      source.id,
      { name: '新名称', serverUrl: 'https://new.example.com/dav', username: 'bob', path: '/new', password: '' },
      [source],
    )

    expect(result.updated).toEqual({
      ...source,
      name: '新名称',
      serverUrl: 'https://new.example.com/dav',
      username: 'bob',
      path: '/new',
    })
    expect(SecureStorage.get).not.toHaveBeenCalled()
    expect(SecureStorage.set).not.toHaveBeenCalled()
    expect(localStorage.getItem('muses:sources')).not.toContain('password')
  })

  test('编辑 WebDAV 新密码时写入原 credentialKey 且不落 localStorage', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    vi.mocked(SecureStorage.get).mockResolvedValueOnce('old-secret')
    const source: SourceItem = {
      id: 'webdav-1',
      type: 'webdav',
      name: '远程',
      serverUrl: 'https://example.com/dav',
      username: 'alice',
      path: '/music',
      credentialKey: 'credential-1',
      createdAt: '2026-07-18T00:00:00.000Z',
    }

    const result = await updateSource(
      source.id,
      { name: '远程音乐', serverUrl: source.serverUrl, username: source.username, path: source.path, password: 'new-secret' },
      [source],
    )

    expect(SecureStorage.set).toHaveBeenCalledWith(source.credentialKey, 'new-secret')
    expect(result.updated).toEqual({ ...source, name: '远程音乐' })
    expect(result.updated).not.toHaveProperty('password')
    expect(localStorage.getItem('muses:sources')).not.toContain('new-secret')
  })

  test('编辑不存在的音源时不改写存储', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    const source: SourceItem = {
      id: 'local-1',
      type: 'local',
      name: '本地',
      path: '/music',
      createdAt: '2026-07-18T00:00:00.000Z',
    }
    saveSources([source])
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem')
    setItemSpy.mockClear()

    const result = await updateSource('missing', { name: '新名称', path: '/new' }, [source])

    expect(result).toEqual({ updated: null, sources: [source] })
    expect(setItemSpy).not.toHaveBeenCalled()
    expect(SecureStorage.set).not.toHaveBeenCalled()
    setItemSpy.mockRestore()
  })

  test('WebDAV 元数据保存失败时尝试恢复旧密码', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    vi.mocked(SecureStorage.get).mockResolvedValueOnce('old-secret')
    const source: SourceItem = {
      id: 'webdav-1',
      type: 'webdav',
      name: '远程',
      serverUrl: 'https://example.com/dav',
      username: 'alice',
      path: '/music',
      credentialKey: 'credential-1',
      createdAt: '2026-07-18T00:00:00.000Z',
    }
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem').mockImplementationOnce(() => {
      throw new Error('metadata save failed')
    })

    await expect(
      updateSource(
        source.id,
        { name: '新名称', serverUrl: source.serverUrl, username: source.username, path: source.path, password: 'new-secret' },
        [source],
      ),
    ).rejects.toThrow('metadata save failed')
    expect(SecureStorage.set).toHaveBeenNthCalledWith(1, source.credentialKey, 'new-secret')
    expect(SecureStorage.set).toHaveBeenNthCalledWith(2, source.credentialKey, 'old-secret')
    setItemSpy.mockRestore()
  })

  test('删除本地音源只更新 sources，不调用 SecureStorage.remove', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    const localSource: SourceItem = {
      id: 'local-1',
      type: 'local',
      name: '本地音乐',
      path: '/music',
      createdAt: '2026-07-18T00:00:00.000Z',
    }
    const otherSource: SourceItem = {
      id: 'local-2',
      type: 'local',
      name: '另一本地',
      path: '/other',
      createdAt: '2026-07-18T00:00:00.000Z',
    }
    saveSources([localSource, otherSource])

    const result = await deleteSource('local-1')

    expect(result.deleted).toEqual(localSource)
    expect(result.sources).toEqual([otherSource])
    expect(loadSources()).toEqual([otherSource])
    expect(SecureStorage.remove).not.toHaveBeenCalled()
  })

  test('删除 WebDAV 音源会移除 SecureStorage 凭据并保留其他音源', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    const webdavId = 'webdav-1'
    const credentialKey = getWebDavPasswordKey(webdavId)
    const webdavSource: SourceItem = {
      id: webdavId,
      type: 'webdav',
      name: '远程',
      serverUrl: 'https://example.com/dav',
      username: 'alice',
      path: '/music',
      credentialKey,
      createdAt: '2026-07-18T00:00:00.000Z',
    }
    const localSource: SourceItem = {
      id: 'local-1',
      type: 'local',
      name: '本地',
      path: '/music',
      createdAt: '2026-07-18T00:00:00.000Z',
    }
    saveSources([webdavSource, localSource])

    const result = await deleteSource(webdavId)

    expect(SecureStorage.remove).toHaveBeenCalledWith(credentialKey)
    expect(result.deleted).toEqual(webdavSource)
    expect(result.sources).toEqual([localSource])
    expect(loadSources()).toEqual([localSource])
    expect(localStorage.getItem('muses:sources')).not.toContain('secret-password')
  })

  test('删除不存在的音源时不改写库', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    const localSource: SourceItem = {
      id: 'local-1',
      type: 'local',
      name: '本地',
      path: '/music',
      createdAt: '2026-07-18T00:00:00.000Z',
    }
    saveSources([localSource])

    const result = await deleteSource('missing-id')

    expect(result.deleted).toBeNull()
    expect(result.sources).toEqual([localSource])
    expect(loadSources()).toEqual([localSource])
    expect(SecureStorage.remove).not.toHaveBeenCalled()
  })

  test('WebDAV 凭据删除失败时不写 sources', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    vi.mocked(SecureStorage.remove).mockRejectedValueOnce(new Error('secure remove failed'))
    const webdavId = 'webdav-fail'
    const credentialKey = getWebDavPasswordKey(webdavId)
    const webdavSource: SourceItem = {
      id: webdavId,
      type: 'webdav',
      name: '远程',
      serverUrl: 'https://example.com/dav',
      username: 'alice',
      path: '/music',
      credentialKey,
      createdAt: '2026-07-18T00:00:00.000Z',
    }
    saveSources([webdavSource])

    await expect(deleteSource(webdavId)).rejects.toThrow('secure remove failed')
    expect(loadSources()).toEqual([webdavSource])
  })
})

describe('WebDAV 路径工具', () => {
  test('规范化路径并处理根目录', () => {
    expect(normalizeWebDavPath('')).toBe('/')
    expect(normalizeWebDavPath('music//rock/')).toBe('/music/rock')
    expect(normalizeWebDavPath('/')).toBe('/')
  })

  test('计算父级路径和展示名称', () => {
    expect(getParentWebDavPath('/')).toBeNull()
    expect(getParentWebDavPath('/music/rock')).toBe('/music')
    expect(getParentWebDavPath('/music')).toBe('/')
    expect(getWebDavDisplayName('/music/rock')).toBe('rock')
  })

  test('构建 WebDAV 请求地址时编码路径并移除服务地址尾部斜杠', () => {
    expect(buildWebDavUrl('https://example.com/dav/', '/音乐/摇滚')).toBe('https://example.com/dav/%E9%9F%B3%E4%B9%90/%E6%91%87%E6%BB%9A')
    expect(buildWebDavUrl('https://example.com/dav/', '/')).toBe('https://example.com/dav/')
  })

  test('展示名称会把百分号编码路径转成人类可读文本', () => {
    expect(getWebDavDisplayName('/%E5%A4%B8%E5%85%8B%E4%B8%8A%E4%BC%A0%E6%96%87%E4%BB%B6')).toBe('夸克上传文件')
  })
})

describe('WebDAV 原生插件目录读取', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  test('通过 WebDav 原生插件发送 PROPFIND 并解析目录响应', async () => {
    const { WebDavNative } = await import('@/features/sources/webdav')
    vi.mocked(WebDavNative.propfind).mockResolvedValue({
      status: 207,
      data: `<?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
          <d:response>
            <d:href>/dav/music/</d:href>
            <d:propstat><d:prop><d:displayname>music</d:displayname><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat>
          </d:response>
          <d:response>
            <d:href>/dav/music/rock/</d:href>
            <d:propstat><d:prop><d:displayname>rock</d:displayname><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat>
          </d:response>
          <d:response>
            <d:href>/dav/music/song.mp3</d:href>
            <d:propstat><d:prop><d:displayname>song.mp3</d:displayname><d:resourcetype /></d:prop></d:propstat>
          </d:response>
        </d:multistatus>`,
    })
    const connection: WebDavConnectionInput = {
      serverUrl: 'https://example.com/dav',
      username: 'alice',
      password: 'secret',
    }

    await expect(listWebDavDirectories(connection, '/music')).resolves.toEqual([
      { basename: 'rock', filename: '/dav/music/rock/', path: '/music/rock' },
    ])
    expect(WebDavNative.propfind).toHaveBeenCalledWith({
      url: 'https://example.com/dav/music',
      username: 'alice',
      password: 'secret',
    })
  })

  test('解析 WebDAV 响应时将编码后的 href 转成可读路径', async () => {
    const { WebDavNative } = await import('@/features/sources/webdav')
    vi.mocked(WebDavNative.propfind).mockResolvedValue({
      status: 207,
      data: `<?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
          <d:response>
            <d:href>/dav/</d:href>
            <d:propstat><d:prop><d:displayname>dav</d:displayname><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat>
          </d:response>
          <d:response>
            <d:href>/dav/%E5%A4%B8%E5%85%8B%E4%B8%8A%E4%BC%A0%E6%96%87%E4%BB%B6/</d:href>
            <d:propstat><d:prop><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat>
          </d:response>
        </d:multistatus>`,
    })

    await expect(
      listWebDavDirectories({ serverUrl: 'https://example.com/dav', username: 'alice', password: 'secret' }, '/'),
    ).resolves.toEqual([
      {
        basename: '夸克上传文件',
        filename: '/dav/%E5%A4%B8%E5%85%8B%E4%B8%8A%E4%BC%A0%E6%96%87%E4%BB%B6/',
        path: '/夸克上传文件',
      },
    ])
  })

  test('解析 WebDAV 响应时保留目录和文件类型', () => {
    expect(
      parseWebDavEntries(
        `<?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
          <d:response>
            <d:href>/dav/music/</d:href>
            <d:propstat><d:prop><d:displayname>music</d:displayname><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat>
          </d:response>
          <d:response>
            <d:href>/dav/music/song.mp3</d:href>
            <d:propstat><d:prop><d:displayname>song.mp3</d:displayname><d:resourcetype /></d:prop></d:propstat>
          </d:response>
        </d:multistatus>`,
        'https://example.com/dav',
        '/music',
      ),
    ).toEqual([{ basename: 'song.mp3', filename: '/dav/music/song.mp3', path: '/music/song.mp3', isDirectory: false }])
  })

  test('递归列出 WebDAV 音频文件并过滤非音频文件', async () => {
    const { WebDavNative } = await import('@/features/sources/webdav')
    vi.mocked(WebDavNative.propfind)
      .mockResolvedValueOnce({
        status: 207,
        data: `<?xml version="1.0" encoding="utf-8"?>
          <d:multistatus xmlns:d="DAV:">
            <d:response><d:href>/dav/music/</d:href><d:propstat><d:prop><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat></d:response>
            <d:response><d:href>/dav/music/rock/</d:href><d:propstat><d:prop><d:displayname>rock</d:displayname><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat></d:response>
            <d:response><d:href>/dav/music/a.mp3</d:href><d:propstat><d:prop><d:displayname>a.mp3</d:displayname><d:resourcetype /></d:prop></d:propstat></d:response>
            <d:response><d:href>/dav/music/cover.jpg</d:href><d:propstat><d:prop><d:displayname>cover.jpg</d:displayname><d:resourcetype /></d:prop></d:propstat></d:response>
          </d:multistatus>`,
      })
      .mockResolvedValueOnce({
        status: 207,
        data: `<?xml version="1.0" encoding="utf-8"?>
          <d:multistatus xmlns:d="DAV:">
            <d:response><d:href>/dav/music/rock/</d:href><d:propstat><d:prop><d:resourcetype><d:collection /></d:resourcetype></d:prop></d:propstat></d:response>
            <d:response><d:href>/dav/music/rock/b.flac</d:href><d:propstat><d:prop><d:displayname>b.flac</d:displayname><d:resourcetype /></d:prop></d:propstat></d:response>
          </d:multistatus>`,
      })

    await expect(
      listWebDavAudioFiles({ serverUrl: 'https://example.com/dav', username: 'alice', password: 'secret' }, '/music'),
    ).resolves.toEqual([
      { path: '/music/a.mp3', uri: 'https://example.com/dav/music/a.mp3', name: 'a.mp3' },
      { path: '/music/rock/b.flac', uri: 'https://example.com/dav/music/rock/b.flac', name: 'b.flac' },
    ])
  })

  test('认证失败时给出明确错误', async () => {
    const { WebDavNative } = await import('@/features/sources/webdav')
    vi.mocked(WebDavNative.propfind).mockResolvedValue({ status: 401, data: '' })

    await expect(
      listWebDavDirectories({ serverUrl: 'https://example.com/dav', username: 'alice', password: 'bad' }, '/'),
    ).rejects.toThrow('WebDAV 认证失败')
  })
})
