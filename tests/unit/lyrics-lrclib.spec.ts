import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

const httpGet = vi.hoisted(() => vi.fn())

vi.mock('@capacitor/core', () => ({
  CapacitorHttp: {
    get: httpGet,
  },
}))

import {
  LRCLIB_USER_AGENT,
  searchLrclibLyrics,
} from '@/features/lyrics/providers/lrclib'
import { getOnlineLyricsFallbackProviders, setOnlineLyricsFallbackProvidersForTest } from '@/features/lyrics/match'

const query = {
  songId: 's1',
  title: '晴天',
  artist: '周杰伦',
  album: '叶惠美',
  duration: 269,
}

describe('LRCLIB 歌词', () => {
  beforeEach(() => {
    httpGet.mockReset()
    setOnlineLyricsFallbackProvidersForTest(null)
  })

  afterEach(() => {
    setOnlineLyricsFallbackProvidersForTest(null)
  })

  test('默认链末尾为 lrclib', () => {
    const ids = getOnlineLyricsFallbackProviders().map((p) => p.id)
    expect(ids[ids.length - 1]).toBe('lrclib')
    expect(ids.slice(0, 5)).toEqual(['kw', 'tx', 'wy', 'kg', 'mg'])
  })

  test('精确 get 返回 syncedLyrics', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        trackName: '晴天',
        artistName: '周杰伦',
        duration: 269,
        syncedLyrics: '[00:29.36] 故事的小黄花\n[00:32.77] 从出生那年就飘着\n',
        plainLyrics: '故事的小黄花',
      }),
    })

    await expect(searchLrclibLyrics(query)).resolves.toEqual({
      format: 'lrc',
      text: '[00:29.36] 故事的小黄花\n[00:32.77] 从出生那年就飘着',
    })

    const firstCall = httpGet.mock.calls[0][0]
    expect(firstCall.url).toContain('lrclib.net/api/get')
    expect(firstCall.url).toContain('duration=269')
    expect(firstCall.headers['User-Agent']).toBe(LRCLIB_USER_AGENT)
  })

  test('get 404 后走 search，且忽略仅 plain', async () => {
    httpGet
      .mockResolvedValueOnce({ status: 404, data: '{"statusCode":404}' })
      .mockResolvedValueOnce({
        status: 200,
        data: JSON.stringify([
          {
            trackName: '晴天',
            artistName: '周杰伦',
            albumName: '叶惠美',
            duration: 269,
            plainLyrics: '无时间轴',
            syncedLyrics: null,
          },
          {
            trackName: '晴天',
            artistName: '周杰伦',
            albumName: '叶惠美',
            duration: 269,
            syncedLyrics: '[00:01.00]有时间轴\n',
          },
        ]),
      })

    await expect(searchLrclibLyrics(query)).resolves.toEqual({
      format: 'lrc',
      text: '[00:01.00]有时间轴',
    })
  })

  test('无 synced 返回 null', async () => {
    httpGet
      .mockResolvedValueOnce({ status: 404, data: '{"statusCode":404}' })
      .mockResolvedValueOnce({
        status: 200,
        data: JSON.stringify([
          {
            trackName: '晴天',
            artistName: '周杰伦',
            plainLyrics: 'only plain',
            syncedLyrics: null,
          },
        ]),
      })

    await expect(searchLrclibLyrics(query)).resolves.toBeNull()
  })


  test('编排：平台 miss 后 LRCLIB 命中', async () => {
    const { __setIndexCacheForTests, resetAmllTtmlDbCache } = await import('@/features/lyrics/amllTtmlDb')
    const { matchOnlineLyrics } = await import('@/features/lyrics/match')
    resetAmllTtmlDbCache()
    __setIndexCacheForTests([])

    const kw = {
      id: 'kw' as const,
      searchLyrics: vi.fn().mockResolvedValue(null),
    }
    const lrclib = {
      id: 'lrclib' as const,
      searchLyrics: vi.fn().mockResolvedValue({ text: '[00:01.00]lrclib', format: 'lrc' as const }),
    }
    setOnlineLyricsFallbackProvidersForTest([kw, lrclib])

    await expect(matchOnlineLyrics(query)).resolves.toEqual({
      ok: true,
      text: '[00:01.00]lrclib',
      format: 'lrc',
      source: 'lrclib',
    })
    expect(kw.searchLyrics).toHaveBeenCalled()
    expect(lrclib.searchLyrics).toHaveBeenCalled()
  })

})
