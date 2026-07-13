import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

const httpGet = vi.hoisted(() => vi.fn())

vi.mock('@capacitor/core', () => ({
  CapacitorHttp: {
    get: httpGet,
  },
}))

import {
  matchOnlineTextMeta,
  mergeTextMetaFillEmpty,
  needsOnlineTextMeta,
  resetOnlineTextMetaCache,
} from '@/features/metadata'
import type { OnlineTextQuery, TextMetaProvider } from '@/features/metadata'
import { searchKwTextMeta } from '@/features/metadata/providers/kw'
import { searchTxTextMeta } from '@/features/metadata/providers/tx'

const sampleQuery: OnlineTextQuery = {
  songId: 'song-1',
  title: '晴天',
  artist: undefined,
  album: undefined,
}

describe('在线文本元信息匹配', () => {
  beforeEach(() => {
    resetOnlineTextMetaCache()
    httpGet.mockReset()
  })

  afterEach(() => {
    resetOnlineTextMetaCache()
  })

  test('needsOnlineTextMeta：任一空即需要', () => {
    expect(needsOnlineTextMeta({ artist: '周杰伦', album: '叶惠美' })).toBe(false)
    expect(needsOnlineTextMeta({ artist: '周杰伦', album: '' })).toBe(true)
    expect(needsOnlineTextMeta({ artist: undefined, album: '叶惠美' })).toBe(true)
    expect(needsOnlineTextMeta({})).toBe(true)
  })

  test('mergeTextMetaFillEmpty 只填空字段且不改 title', () => {
    const base = { title: '本地标题', artist: '已有歌手', album: undefined as string | undefined }
    const { next, changed } = mergeTextMetaFillEmpty(base, {
      artist: '在线歌手',
      album: '叶惠美',
    })
    expect(changed).toBe(true)
    expect(next).toEqual({
      title: '本地标题',
      artist: '已有歌手',
      album: '叶惠美',
    })
  })

  test('merge 无变化时 changed=false', () => {
    const base = { artist: '周杰伦', album: '叶惠美' }
    const { changed } = mergeTextMetaFillEmpty(base, {
      artist: '别人',
      album: '别的专辑',
    })
    expect(changed).toBe(false)
  })

  test('两字段皆有时 match 返回 not-needed', async () => {
    await expect(
      matchOnlineTextMeta({
        songId: 'full',
        title: '晴天',
        artist: '周杰伦',
        album: '叶惠美',
      }),
    ).resolves.toEqual({ ok: false, reason: 'not-needed' })
  })

  test('kw 解析搜索返回 artist/album', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        abslist: [
          {
            SONGNAME: '晴天',
            ARTIST: '周杰伦',
            ALBUM: '叶惠美',
          },
        ],
      }),
    })

    await expect(searchKwTextMeta(sampleQuery)).resolves.toEqual({
      title: '晴天',
      artist: '周杰伦',
      album: '叶惠美',
      source: 'kw',
    })
  })

  test('tx 解析搜索返回 artist/album', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        data: {
          song: {
            list: [
              {
                songname: '晴天',
                albumname: '叶惠美',
                singer: [{ name: '周杰伦' }],
              },
            ],
          },
        },
      }),
    })

    await expect(searchTxTextMeta(sampleQuery)).resolves.toEqual({
      title: '晴天',
      artist: '周杰伦',
      album: '叶惠美',
      source: 'tx',
    })
  })

  test('kw miss 时 tx 回退成功', async () => {
    const kw: TextMetaProvider = {
      id: 'kw',
      search: vi.fn().mockResolvedValue(null),
    }
    const tx: TextMetaProvider = {
      id: 'tx',
      search: vi.fn().mockResolvedValue({
        title: '晴天',
        artist: '周杰伦',
        album: '叶惠美',
        source: 'tx',
      }),
    }

    await expect(matchOnlineTextMeta(sampleQuery, [kw, tx])).resolves.toEqual({
      ok: true,
      hit: {
        title: '晴天',
        artist: '周杰伦',
        album: '叶惠美',
        source: 'tx',
      },
    })
    expect(kw.search).toHaveBeenCalled()
    expect(tx.search).toHaveBeenCalled()
  })

  test('前源成功不请求后源', async () => {
    const kw: TextMetaProvider = {
      id: 'kw',
      search: vi.fn().mockResolvedValue({
        artist: '周杰伦',
        album: '叶惠美',
        source: 'kw',
      }),
    }
    const tx: TextMetaProvider = {
      id: 'tx',
      search: vi.fn().mockResolvedValue({
        artist: '别人',
        album: '别的',
        source: 'tx',
      }),
    }

    await expect(matchOnlineTextMeta(sampleQuery, [kw, tx])).resolves.toMatchObject({
      ok: true,
      hit: { source: 'kw' },
    })
    expect(tx.search).not.toHaveBeenCalled()
  })

  test('全 miss 写入负缓存', async () => {
    const kw: TextMetaProvider = {
      id: 'kw',
      search: vi.fn().mockResolvedValue(null),
    }
    const tx: TextMetaProvider = {
      id: 'tx',
      search: vi.fn().mockResolvedValue(null),
    }

    await expect(matchOnlineTextMeta(sampleQuery, [kw, tx])).resolves.toEqual({
      ok: false,
      reason: 'no-match',
    })
    await expect(matchOnlineTextMeta(sampleQuery, [kw, tx])).resolves.toEqual({
      ok: false,
      reason: 'no-match',
    })
    expect(kw.search).toHaveBeenCalledTimes(1)
    expect(tx.search).toHaveBeenCalledTimes(1)
  })

  test('仅缺 album 时 hit 只有 artist 不算有效（需能填缺字段）', async () => {
    const kw: TextMetaProvider = {
      id: 'kw',
      search: vi.fn().mockResolvedValue({
        artist: '周杰伦',
        source: 'kw',
      }),
    }
    const tx: TextMetaProvider = {
      id: 'tx',
      search: vi.fn().mockResolvedValue({
        album: '叶惠美',
        source: 'tx',
      }),
    }

    await expect(
      matchOnlineTextMeta(
        {
          songId: 'partial',
          title: '晴天',
          artist: '周杰伦',
          album: undefined,
        },
        [kw, tx],
      ),
    ).resolves.toEqual({
      ok: true,
      hit: {
        album: '叶惠美',
        source: 'tx',
      },
    })
  })
})
