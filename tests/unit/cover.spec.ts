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
import { searchMgCoverUrl } from '@/features/cover/providers/mg'
import { searchKgCoverUrl } from '@/features/cover/providers/kg'
import { searchTxCoverUrl } from '@/features/cover/providers/tx'
import { searchWyCoverUrl } from '@/features/cover/providers/wy'

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

  test('mg 解析搜索 cover 并升 https', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        musics: [
          {
            songName: '晴天',
            singerName: '周杰伦',
            albumName: '叶惠美',
            cover: 'http://d.musicapp.migu.cn/cover/500.jpg',
          },
        ],
      }),
    })

    await expect(searchMgCoverUrl(sampleQuery)).resolves.toBe(
      'https://d.musicapp.migu.cn/cover/500.jpg',
    )
  })

  test('mg 空列表或无 cover 返回 null', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({ musics: [] }),
    })
    await expect(searchMgCoverUrl(sampleQuery)).resolves.toBeNull()

    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        musics: [{ songName: '晴天', singerName: '周杰伦', cover: '' }],
      }),
    })
    await expect(searchMgCoverUrl(sampleQuery)).resolves.toBeNull()
  })

  test('默认链 iTunes+kw miss 后走 tx（any-listen 国内段顺序）', async () => {
    // itunes 空结果
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({ resultCount: 0, results: [] }),
    })
    // kw 搜索无 id
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({ abslist: [] }),
    })
    // tx 命中（默认链：iTunes → kw → tx → wy → kg → mg）
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        data: {
          song: {
            list: [
              {
                songname: '晴天',
                albumname: '叶惠美',
                albummid: '000MkMni19ClKG',
                singer: [{ name: '周杰伦' }],
              },
            ],
          },
        },
      }),
    })

    await expect(matchOnlineCoverRemote(sampleQuery)).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://y.gtimg.cn/music/photo_new/T002R500x500M000000MkMni19ClKG.jpg',
      source: 'tx',
    })
    expect(httpGet).toHaveBeenCalledTimes(3)
  })
  test('kg 解析搜索 Image（{size}→480，http→https）', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        data: {
          lists: [
            {
              SongName: '晴天',
              SingerName: '周杰伦',
              AlbumName: '叶惠美',
              AlbumID: '966846',
              Image: 'http://imge.kugou.com/stdmusic/{size}/20230920/xxx.jpg',
            },
          ],
        },
      }),
    })

    await expect(searchKgCoverUrl(sampleQuery)).resolves.toBe(
      'https://imge.kugou.com/stdmusic/480/20230920/xxx.jpg',
    )
  })

  test('kg 空列表或无 Image 返回 null', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({ data: { lists: [] } }),
    })
    await expect(searchKgCoverUrl(sampleQuery)).resolves.toBeNull()

    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({ data: { lists: [{ SongName: '晴天' }] } }),
    })
    await expect(searchKgCoverUrl(sampleQuery)).resolves.toBeNull()
  })

  test('前三源 miss 时 kg 回退成功', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const kw: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const mg: CoverProvider = {
      id: 'mg',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const kg: CoverProvider = {
      id: 'kg',
      searchCoverUrl: vi.fn().mockResolvedValue('https://imge.kugou.com/stdmusic/480/cover.jpg'),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw, mg, kg])).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://imge.kugou.com/stdmusic/480/cover.jpg',
      source: 'kg',
    })
    expect(itunes.searchCoverUrl).toHaveBeenCalled()
    expect(kw.searchCoverUrl).toHaveBeenCalled()
    expect(mg.searchCoverUrl).toHaveBeenCalled()
    expect(kg.searchCoverUrl).toHaveBeenCalled()
  })

  test('前源成功时不请求 kg', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockResolvedValue('https://is1-ssl.mzstatic.com/a.jpg'),
    }
    const kw: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockResolvedValue('https://img.kuwo.cn/b.jpg'),
    }
    const mg: CoverProvider = {
      id: 'mg',
      searchCoverUrl: vi.fn().mockResolvedValue('https://d.musicapp.migu.cn/c.jpg'),
    }
    const kg: CoverProvider = {
      id: 'kg',
      searchCoverUrl: vi.fn().mockResolvedValue('https://imge.kugou.com/d.jpg'),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw, mg, kg])).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://is1-ssl.mzstatic.com/a.jpg',
      source: 'itunes',
    })
    expect(kw.searchCoverUrl).not.toHaveBeenCalled()
    expect(mg.searchCoverUrl).not.toHaveBeenCalled()
    expect(kg.searchCoverUrl).not.toHaveBeenCalled()
  })

  test('四源 miss 写入负缓存', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const kw: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const mg: CoverProvider = {
      id: 'mg',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const kg: CoverProvider = {
      id: 'kg',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw, mg, kg])).resolves.toEqual({
      ok: false,
      reason: 'no-match',
    })
    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw, mg, kg])).resolves.toEqual({
      ok: false,
      reason: 'no-match',
    })
    expect(itunes.searchCoverUrl).toHaveBeenCalledTimes(1)
    expect(kw.searchCoverUrl).toHaveBeenCalledTimes(1)
    expect(mg.searchCoverUrl).toHaveBeenCalledTimes(1)
    expect(kg.searchCoverUrl).toHaveBeenCalledTimes(1)
  })

  test('tx 用 albummid 拼 gtimg 封面 URL', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        data: {
          song: {
            list: [
              {
                songname: '晴天',
                albumname: '叶惠美',
                albummid: '000MkMni19ClKG',
                singer: [{ name: '周杰伦' }],
              },
            ],
          },
        },
      }),
    })

    await expect(searchTxCoverUrl(sampleQuery)).resolves.toBe(
      'https://y.gtimg.cn/music/photo_new/T002R500x500M000000MkMni19ClKG.jpg',
    )
  })

  test('tx 无 albummid 返回 null', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        data: { song: { list: [{ songname: '晴天', albummid: '' }] } },
      }),
    })
    await expect(searchTxCoverUrl(sampleQuery)).resolves.toBeNull()
  })

  test('wy 搜索后拉详情取 picUrl 并升 https', async () => {
    httpGet
      .mockResolvedValueOnce({
        status: 200,
        data: JSON.stringify({
          result: {
            songs: [
              {
                id: 186016,
                name: '晴天',
                artists: [{ name: '周杰伦' }],
                album: { name: '叶惠美' },
              },
            ],
          },
        }),
      })
      .mockResolvedValueOnce({
        status: 200,
        data: JSON.stringify({
          songs: [
            {
              album: {
                picUrl: 'http://p2.music.126.net/ZGffiDQZrGj5s_hnR1CNbg==/109951165566379710.jpg',
              },
            },
          ],
        }),
      })

    await expect(searchWyCoverUrl(sampleQuery)).resolves.toBe(
      'https://p2.music.126.net/ZGffiDQZrGj5s_hnR1CNbg==/109951165566379710.jpg',
    )
    expect(httpGet).toHaveBeenCalledTimes(2)
  })

  test('前源 miss 时 tx 回退成功', async () => {
    const miss = (id: CoverProvider['id']): CoverProvider => ({
      id,
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    })
    const itunes = miss('itunes')
    const kw = miss('kw')
    const mg = miss('mg')
    const kg = miss('kg')
    const tx: CoverProvider = {
      id: 'tx',
      searchCoverUrl: vi.fn().mockResolvedValue(
        'https://y.gtimg.cn/music/photo_new/T002R500x500M000000MkMni19ClKG.jpg',
      ),
    }
    const wy: CoverProvider = {
      id: 'wy',
      searchCoverUrl: vi.fn().mockResolvedValue('https://p2.music.126.net/a.jpg'),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw, tx, wy, kg, mg])).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://y.gtimg.cn/music/photo_new/T002R500x500M000000MkMni19ClKG.jpg',
      source: 'tx',
    })
    expect(tx.searchCoverUrl).toHaveBeenCalled()
    expect(wy.searchCoverUrl).not.toHaveBeenCalled()
  })

  test('前源 miss 时 wy 回退成功', async () => {
    const miss = (id: CoverProvider['id']): CoverProvider => ({
      id,
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    })
    const providers: CoverProvider[] = [
      miss('itunes'),
      miss('kw'),
      miss('tx'),
      {
        id: 'wy',
        searchCoverUrl: vi.fn().mockResolvedValue('https://p2.music.126.net/cover.jpg'),
      },
      miss('kg'),
      miss('mg'),
    ]

    await expect(matchOnlineCoverRemote(sampleQuery, providers)).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://p2.music.126.net/cover.jpg',
      source: 'wy',
    })
  })

  test('前源成功时不请求 tx/wy', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockResolvedValue('https://is1-ssl.mzstatic.com/a.jpg'),
    }
    const tx: CoverProvider = {
      id: 'tx',
      searchCoverUrl: vi.fn().mockResolvedValue('https://y.gtimg.cn/b.jpg'),
    }
    const wy: CoverProvider = {
      id: 'wy',
      searchCoverUrl: vi.fn().mockResolvedValue('https://p2.music.126.net/c.jpg'),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, tx, wy])).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://is1-ssl.mzstatic.com/a.jpg',
      source: 'itunes',
    })
    expect(tx.searchCoverUrl).not.toHaveBeenCalled()
    expect(wy.searchCoverUrl).not.toHaveBeenCalled()
  })

  test('六源 miss 写入负缓存', async () => {
    const ids: CoverProvider['id'][] = ['itunes', 'kw', 'tx', 'wy', 'kg', 'mg']
    const providers = ids.map((id) => ({
      id,
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }))

    await expect(matchOnlineCoverRemote(sampleQuery, providers)).resolves.toEqual({
      ok: false,
      reason: 'no-match',
    })
    await expect(matchOnlineCoverRemote(sampleQuery, providers)).resolves.toEqual({
      ok: false,
      reason: 'no-match',
    })
    for (const p of providers) {
      expect(p.searchCoverUrl).toHaveBeenCalledTimes(1)
    }
  })

  test('iTunes+kw miss 时 mg 回退成功', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const kw: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const mg: CoverProvider = {
      id: 'mg',
      searchCoverUrl: vi.fn().mockResolvedValue('https://d.musicapp.migu.cn/cover.jpg'),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw, mg])).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://d.musicapp.migu.cn/cover.jpg',
      source: 'mg',
    })
    expect(itunes.searchCoverUrl).toHaveBeenCalled()
    expect(kw.searchCoverUrl).toHaveBeenCalled()
    expect(mg.searchCoverUrl).toHaveBeenCalled()
  })

  test('前源成功时不请求 mg', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockResolvedValue('https://is1-ssl.mzstatic.com/a.jpg'),
    }
    const kw: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockResolvedValue('https://img.kuwo.cn/b.jpg'),
    }
    const mg: CoverProvider = {
      id: 'mg',
      searchCoverUrl: vi.fn().mockResolvedValue('https://d.musicapp.migu.cn/c.jpg'),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw, mg])).resolves.toEqual({
      ok: true,
      remoteUrl: 'https://is1-ssl.mzstatic.com/a.jpg',
      source: 'itunes',
    })
    expect(kw.searchCoverUrl).not.toHaveBeenCalled()
    expect(mg.searchCoverUrl).not.toHaveBeenCalled()
  })

  test('三源 miss 写入负缓存', async () => {
    const itunes: CoverProvider = {
      id: 'itunes',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const kw: CoverProvider = {
      id: 'kw',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }
    const mg: CoverProvider = {
      id: 'mg',
      searchCoverUrl: vi.fn().mockResolvedValue(null),
    }

    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw, mg])).resolves.toEqual({
      ok: false,
      reason: 'no-match',
    })
    await expect(matchOnlineCoverRemote(sampleQuery, [itunes, kw, mg])).resolves.toEqual({
      ok: false,
      reason: 'no-match',
    })
    expect(itunes.searchCoverUrl).toHaveBeenCalledTimes(1)
    expect(kw.searchCoverUrl).toHaveBeenCalledTimes(1)
    expect(mg.searchCoverUrl).toHaveBeenCalledTimes(1)
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
