import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

const httpGet = vi.hoisted(() => vi.fn())
const httpPost = vi.hoisted(() => vi.fn())

vi.mock('@capacitor/core', () => ({
  CapacitorHttp: {
    get: httpGet,
    post: httpPost,
  },
}))

import { searchKwLyrics } from '@/features/lyrics/providers/kw'
import { searchTxLyrics } from '@/features/lyrics/providers/tx'
import { searchWyLyrics } from '@/features/lyrics/providers/wy'
import { platformLyricsProviders } from '@/features/lyrics/providers/platform'
import { matchOnlineLyrics, setOnlineLyricsFallbackProvidersForTest } from '@/features/lyrics/match'

const query = {
  songId: 's1',
  title: '晴天',
  artist: '周杰伦',
  album: '叶惠美',
}

describe('平台歌词 providers', () => {
  beforeEach(() => {
    httpGet.mockReset()
    httpPost.mockReset()
    setOnlineLyricsFallbackProvidersForTest(null)
  })

  afterEach(() => {
    setOnlineLyricsFallbackProvidersForTest(null)
  })

  test('默认平台链顺序 kw→tx→wy→kg→mg', () => {
    expect(platformLyricsProviders.map((p) => p.id)).toEqual(['kw', 'tx', 'wy', 'kg', 'mg'])
  })

  test('kw 搜索 + lrclist 转 LRC', async () => {
    httpGet
      .mockResolvedValueOnce({
        status: 200,
        data: JSON.stringify({
          abslist: [{ MUSICRID: 'MUSIC_228908', SONGNAME: '晴天', ARTIST: '周杰伦', ALBUM: '叶惠美' }],
        }),
      })
      .mockResolvedValueOnce({
        status: 200,
        data: JSON.stringify({
          data: {
            lrclist: [
              { lineLyric: '故事的小黄花', time: '28.95' },
              { lineLyric: '从出生那年就飘着', time: '33.0' },
            ],
          },
        }),
      })

    const hit = await searchKwLyrics(query)
    expect(hit?.format).toBe('lrc')
    expect(hit?.text).toContain('[00:28.950]故事的小黄花')
    expect(hit?.text).toContain('[00:33.000]从出生那年就飘着')
  })

  test('tx 无 songid 时返回 plain LRC', async () => {
    httpGet
      .mockResolvedValueOnce({
        status: 200,
        data: JSON.stringify({
          data: {
            song: {
              list: [
                {
                  songname: '晴天',
                  songmid: '0039MnYb0qxYhV',
                  albumname: '叶惠美',
                  singer: [{ name: '周杰伦' }],
                },
              ],
            },
          },
        }),
      })
      .mockResolvedValueOnce({
        status: 200,
        data: JSON.stringify({
          code: 0,
          lyric: '[00:28.95]故事的小黄花\n[00:33.00]从出生那年就飘着\n',
        }),
      })

    await expect(searchTxLyrics(query)).resolves.toEqual({
      format: 'lrc',
      text: '[00:28.95]故事的小黄花\n[00:33.00]从出生那年就飘着',
    })
  })

  test('wy eapi 优先 yrc', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        result: {
          songs: [{ id: 186016, name: '晴天', artists: [{ name: '周杰伦' }], album: { name: '叶惠美' } }],
        },
      }),
    })
    httpPost.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        yrc: { lyric: '[1140,450](1140,120,0)故(1260,120,0)事' },
        lrc: { lyric: '[00:01.14]故事' },
      }),
    })

    await expect(searchWyLyrics(query)).resolves.toEqual({
      format: 'yrc',
      text: '[1140,450](1140,120,0)故(1260,120,0)事',
    })
  })

  test('编排：amll miss 后走平台 kw', async () => {
    const { __setIndexCacheForTests, resetAmllTtmlDbCache } = await import('@/features/lyrics/amllTtmlDb')
    resetAmllTtmlDbCache()
    __setIndexCacheForTests([])

    const kw = {
      id: 'kw' as const,
      searchLyrics: vi.fn().mockResolvedValue({ text: '[00:01.00]平台词', format: 'lrc' as const }),
    }
    setOnlineLyricsFallbackProvidersForTest([kw])

    await expect(matchOnlineLyrics(query)).resolves.toEqual({
      ok: true,
      text: '[00:01.00]平台词',
      format: 'lrc',
      source: 'kw',
    })
  })
})
