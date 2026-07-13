import { beforeEach, describe, expect, test, vi } from 'vitest'

const httpGet = vi.hoisted(() => vi.fn())
const httpPost = vi.hoisted(() => vi.fn())

vi.mock('@capacitor/core', () => ({
  CapacitorHttp: {
    get: httpGet,
    post: httpPost,
  },
}))

import { decryptQrcHex } from '@applemusic-like-lyrics/lyric'
import { decryptQrcToPlain, extractQrcLyricContent } from '@/features/lyrics/providers/qrc'
import { buildEapiParams, __md5ForTest } from '@/features/lyrics/providers/wyCrypto'
import { searchTxLyrics } from '@/features/lyrics/providers/tx'
import { searchWyLyrics } from '@/features/lyrics/providers/wy'

const query = {
  songId: 's1',
  title: '晴天',
  artist: '周杰伦',
  album: '叶惠美',
}

describe('QRC 工具', () => {
  test('extractQrcLyricContent 从 XML 取正文', () => {
    const xml =
      '<?xml version="1.0"?><QrcInfos><Lyric_1 LyricContent="[0,1000]测(0,500)试(500,500)"/></QrcInfos>'
    expect(extractQrcLyricContent(xml)).toContain('[0,1000]')
  })
})

describe('网易 eapi 加密', () => {
  test('md5 与 golden eapi params 对齐 node crypto', () => {
    const url = '/api/song/lyric/v1'
    const object = { id: 439915614, cp: false, tv: 0, lv: 0, rv: 0, kv: 0, yv: 0, ytv: 0, yrv: 0 }
    const text = JSON.stringify(object)
    expect(__md5ForTest(`nobody${url}use${text}md5forencrypt`)).toBe(
      '0300af2aa97f181475faba3a7b0a297f',
    )
    expect(buildEapiParams(url, object)).toBe(
      '04AE33D34A93FE3EC22DA8FA305D290AB337D0FE5F36D211DE0D338CC6AA89D047B8B536D9F2A921E289F53E344F32582DB068E92E0C3ECB971167AD86992BEA0E10321E54D5A8684809D3043F85F9BB499F7B77CDCD5C37DB18D4A81006EF4C9EF919C7515093D7FAE53E91911A4CA9D5D8C1C323DD95A0BB3C0EAAC0C326F3B949F0DF8A87D1C22EF1237685424ADC885FCBB2961185E56F621C13B4A0E75E',
    )
  })
})

describe('tx 优先 QRC', () => {
  beforeEach(() => {
    httpGet.mockReset()
    httpPost.mockReset()
  })

  test('GetPlayLyricInfo 解密成功返回 qrc', async () => {
    // 构造可被 decrypt 的最小路径：用 encrypt 逆不方便，直接 mock decrypt 路径
    // 改为 mock post 返回假 hex，并 spy decrypt — 改用真实 amll encrypt 若可用
    const plain =
      '<?xml version="1.0" encoding="utf-8"?>\n<QrcInfos>\n<Lyric_1 LyricContent="[0,1000]测(0,500)试(500,500)"/>\n</QrcInfos>'
    // 若包有 encryptQrcHex 可 roundtrip
    const { encryptQrcHex } = await import('@applemusic-like-lyrics/lyric')
    const hex = encryptQrcHex(plain)

    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        data: {
          song: {
            list: [
              {
                songname: '晴天',
                songmid: '0039MnYb0qxYhV',
                songid: 97773,
                albumname: '叶惠美',
                singer: [{ name: '周杰伦' }],
              },
            ],
          },
        },
      }),
    })
    httpPost.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        code: 0,
        req: { code: 0, data: { lyric: hex } },
      }),
    })

    const hit = await searchTxLyrics(query)
    expect(hit?.format).toBe('qrc')
    expect(hit?.text).toContain('[0,1000]')
    expect(decryptQrcToPlain(hex)).toContain('测')
    // 确保真的用了 decrypt
    expect(decryptQrcHex(hex)).toContain('LyricContent')
  })

  test('解密失败降级 LRC', async () => {
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
                  songid: 97773,
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
          lyric: '[00:28.95]故事的小黄花\n',
        }),
      })
    httpPost.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        code: 0,
        req: { code: 0, data: { lyric: 'DEADBEEF' } },
      }),
    })

    await expect(searchTxLyrics(query)).resolves.toEqual({
      format: 'lrc',
      text: '[00:28.95]故事的小黄花',
    })
  })
})

describe('wy 优先 yrc', () => {
  beforeEach(() => {
    httpGet.mockReset()
    httpPost.mockReset()
  })

  test('eapi 返回 yrc', async () => {
    httpGet.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        result: {
          songs: [{ id: 439915614, name: '晴天', artists: [{ name: '周杰伦' }], album: { name: '叶惠美' } }],
        },
      }),
    })
    httpPost.mockResolvedValueOnce({
      status: 200,
      data: JSON.stringify({
        code: 200,
        yrc: { lyric: '[12540,2270](12540,420,0)我(12960,340,0)们\n' },
        lrc: { lyric: '[00:12.54]我们\n' },
      }),
    })

    await expect(searchWyLyrics(query)).resolves.toEqual({
      format: 'yrc',
      text: '[12540,2270](12540,420,0)我(12960,340,0)们',
    })
  })

  test('eapi 无 yrc 时用 lrc', async () => {
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
        code: 200,
        lrc: { lyric: '[00:28.95]故事的小黄花\n' },
      }),
    })

    await expect(searchWyLyrics(query)).resolves.toEqual({
      format: 'lrc',
      text: '[00:28.95]故事的小黄花',
    })
  })
})
