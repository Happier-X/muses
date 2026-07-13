import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

const httpGet = vi.hoisted(() => vi.fn())

vi.mock('@capacitor/core', () => ({
  CapacitorHttp: {
    get: httpGet,
  },
}))

import { matchOnlineCoverRemote, resetOnlineCoverCache } from '@/features/cover'
import type { CoverProvider, OnlineCoverQuery } from '@/features/cover/types'
import { searchItunesCoverUrl } from '@/features/cover/providers/itunes'
import { searchKwCoverUrl } from '@/features/cover/providers/kw'

const sampleQuery: OnlineCoverQuery = {
  songId: 'song-1',
  title: '晴天',
  artist: '周杰伦',
  album: '叶惠美',
}

describe('在线封面匹配', () => {
  beforeEach(() => {
    resetOnlineCoverCache()
    httpGet.mockReset()
  })

  afterEach(() => {
    resetOnlineCoverCache()
  })

  test('iTunes 命中返回放大后的 artwork URL', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        resultCount: 1,
        results: [
          {
            trackName: '晴天',
            artistName: '周杰伦',
            collectionName: '叶惠美',
            artworkUrl100: 'https://is1-ssl.mzstatic.com/image/thumb/x/100x100bb.jpg',
          },
        ],
      }),
    })

    await expect(searchItunesCoverUrl(sampleQuery)).resolves.toBe(
      'https://is1-ssl.mzstatic.com/image/thumb/x/600x600bb.jpg',
    )
  })

  test('iTunes miss 时 kw 回退成功', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const kw: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockResolvedValue('https://img.kuwo.cn/star/cover.jpg'),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw])).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://img.kuwo.cn/star/cover.jpg',
      source: 'kw',
    })
    expect(itunes.searchCoverUrl).toHaveBeenCalled()
    expect(kw.searchCoverUrl).toHaveBeenCalled()
  })

  test('iTunes 命中后不请求 kw', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockResolvedValue('https://is1-ssl.mzstatic.com/a.jpg'),
    }
    const kw: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockResolvedValue('https://img.kuwo.cn/b.jpg'),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw])).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://is1-ssl.mzstatic.com/a.jpg',
      source: 'itunes',
    })
    expect(kw.searchCoverUrl).not.toHaveBeenCalled()
  })

  test('双源 miss 写入负缓存，同查询不再打源', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const kw: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw])).resolves.toEqual({
      ok: false,
      reason: 'no-match',
    })
    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw])).resolves.toEqual({
      ok: false,
      reason: 'no-match',
    })
    expect(itunes.searchCoverUrl).toHaveBeenCalledTimes(1)
    expect(kw.searchCoverUrl).toHaveBeenCalledTimes(1)
  })

  test('kw 解析搜索与 pic 文本 URL', async () => {
    httpGet
      .mockResolvedValueOnce({
        status: 200,
        data: JSON.stringify({
          abslist: [
            {
              MUSICRID: 'MUSIC_12345',
              SONGNAME: '晴天',
              ARTIST: '周杰伦',
              N_MINFO: 'level:h,bitrate:128,format:mp3,size:1M',
            },
          ],
        }),
      })
      .mockResolvedValueOnce({
        status: 200,
        data: 'http://img.kwcdn.kuwo.cn/star/albumcover/500.jpg',
      })

    await expect(searchKwCoverUrl(sampleQuery)).resolves.toBe(
      'https://img.kuwo.cn/star/albumcover/500.jpg',
    )
  })

  test('provider 抛错时尝试下一源；双源网络错误记 network', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockRejectedValue(new Error('timeout')),
    }
    const kw: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockResolvedValue('https://img.kuwo.cn/ok.jpg'),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw])).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://img.kuwo.cn/ok.jpg',
      source: 'kw',
    })

    const itunes2: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockRejectedValue(new Error('timeout')),
    }
    const kw2: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockRejectedValue(new Error('timeout')),
    }
    await expect(matchOnlineCoverRemote({
      ...sampleQuery,
      songId: 'song-net',
      title: '另一首',
    }, [itunes2, kw2])).resolves.toEqual({
      ok: false,
      reason: 'network',
    })
  })
})
