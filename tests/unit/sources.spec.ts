import { afterEach, describe, expect, test, vi } from 'vitest'
import { createSourceId, getWebDavPasswordKey, loadSources, saveSources, saveWebDavPassword } from '@/features/sources/storage'
import type { SourceItem } from '@/features/sources/types'
import { getParentWebDavPath, getWebDavDisplayName, normalizeWebDavPath } from '@/features/sources/webdav'

vi.mock('@aparajita/capacitor-secure-storage', () => ({
  SecureStorage: {
    set: vi.fn(),
    get: vi.fn(),
    remove: vi.fn(),
  },
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
})
