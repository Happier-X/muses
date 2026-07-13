/**
 * QQ 歌词：优先 GetPlayLyricInfo 加密 QRC（AMLL decryptQrcHex），
 * 失败降级 fcg_query_lyric_new 行级 LRC。
 */
import { httpGetJson, httpGetText, httpPostJson } from '@/features/cover/http'
import { decryptQrcToPlain } from './qrc'
import type { LyricsProvider, OnlineLyricsQuery } from './types'
import { buildKeyword, pickBest } from './util'

type TxSongItem = {
  songname?: string
  albumname?: string
  songmid?: string
  songid?: number | string
  singer?: Array<{ name?: string }>
}

type TxSearchResponse = {
  data?: {
    song?: {
      list?: TxSongItem[]
    }
  }
}

type TxHit = {
  mid: string
  songId: number | null
  title?: string
  artist?: string
  album?: string
}

type TxLyricResponse = {
  code?: number
  lyric?: string
  qrc?: string
}

type TxPlayLyricBody = {
  code?: number
  req?: {
    code?: number
    data?: {
      lyric?: string
      qrc?: number | string
    }
  }
}

const TX_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1',
  Referer: 'https://y.qq.com/',
  Accept: 'application/json,text/plain,*/*',
}

const TX_DESKTOP_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36',
  Referer: 'https://y.qq.com',
  'Content-Type': 'application/json',
  Accept: 'application/json,text/plain,*/*',
}

const decodeMaybeBase64 = (raw: string): string => {
  const trimmed = raw.trim()
  if (!trimmed) {
    return ''
  }
  if (trimmed.includes('[') && /\[\d+:\d+/.test(trimmed)) {
    return trimmed
  }
  try {
    if (typeof atob === 'function') {
      const bin = atob(trimmed)
      try {
        return decodeURIComponent(
          Array.from(bin, (c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`).join(''),
        )
      } catch {
        return bin
      }
    }
  } catch {
    // ignore
  }
  return trimmed
}

const searchTxHit = async (query: OnlineLyricsQuery): Promise<TxHit | null> => {
  const keyword = buildKeyword(query)
  if (!keyword) {
    return null
  }
  const url =
    `https://c.y.qq.com/soso/fcgi-bin/search_for_qq_cp?g_tk=5381&uin=0&format=json` +
    `&inCharset=utf-8&outCharset=utf-8&notice=0&platform=h5&needNewCode=1` +
    `&w=${encodeURIComponent(keyword)}&zhidaqu=1&catZhida=1&t=0&flag=1&ie=utf-8&sem=1&aggr=0` +
    `&perpage=10&n=10&p=1&remoteplace=txt.mqq.all`

  const body = await httpGetJson<TxSearchResponse>(url, TX_HEADERS)
  const list = (body.data?.song?.list ?? [])
    .map((item) => {
      const mid = item.songmid?.trim() || ''
      const rawId = item.songid
      const songId =
        typeof rawId === 'number' && rawId > 0
          ? rawId
          : typeof rawId === 'string' && /^\d+$/.test(rawId)
            ? Number.parseInt(rawId, 10)
            : null
      return {
        mid,
        songId,
        title: item.songname,
        artist: (item.singer ?? []).map((s) => s.name || '').filter(Boolean).join(' '),
        album: item.albumname,
      } satisfies TxHit
    })
    .filter((item) => item.mid)

  return pickBest(list, query)
}

const fetchTxQrc = async (songId: number): Promise<string | null> => {
  const payload = {
    comm: {
      ct: '19',
      cv: '1859',
      uin: '0',
    },
    req: {
      method: 'GetPlayLyricInfo',
      module: 'music.musichallSong.PlayLyricInfo',
      param: {
        format: 'json',
        crypt: 1,
        ct: 19,
        cv: 1873,
        interval: 0,
        lrc_t: 0,
        qrc: 1,
        qrc_t: 0,
        roma: 1,
        roma_t: 0,
        songID: songId,
        trans: 1,
        trans_t: 0,
        type: -1,
      },
    },
  }

  try {
    const body = await httpPostJson<TxPlayLyricBody>(
      'https://u.y.qq.com/cgi-bin/musicu.fcg',
      JSON.stringify(payload),
      TX_DESKTOP_HEADERS,
    )
    if (body.code !== 0 || body.req?.code !== 0) {
      return null
    }
    const hex = body.req?.data?.lyric
    if (typeof hex !== 'string' || !hex.trim()) {
      return null
    }
    return decryptQrcToPlain(hex)
  } catch {
    return null
  }
}

const fetchTxLrcByMid = async (songmid: string): Promise<string | null> => {
  const url =
    `https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=${encodeURIComponent(songmid)}` +
    `&format=json&nobase64=1&g_tk=5381&loginUin=0&hostUin=0&inCharset=utf8&outCharset=utf-8&notice=0&platform=yqq.json&needNewCode=0`

  let raw: string
  try {
    raw = await httpGetText(url, TX_HEADERS)
  } catch {
    return null
  }

  const jsonText = raw.replace(/^\s*[a-zA-Z0-9_]*\(/, '').replace(/\)\s*;?\s*$/, '')
  let body: TxLyricResponse
  try {
    body = JSON.parse(jsonText) as TxLyricResponse
  } catch {
    return null
  }

  if (body.qrc && typeof body.qrc === 'string' && body.qrc.includes('(') && body.qrc.includes('[')) {
    // 若意外拿到明文 qrc
    return body.qrc
  }

  const lyric = decodeMaybeBase64(body.lyric || '')
  if (!lyric.trim() || !/\[\d+:\d+/.test(lyric)) {
    return null
  }
  return lyric
}

export const searchTxLyrics = async (
  query: OnlineLyricsQuery,
): Promise<{ text: string; format: 'lrc' | 'qrc' } | null> => {
  const hit = await searchTxHit(query)
  if (!hit) {
    return null
  }

  if (hit.songId != null) {
    const qrc = await fetchTxQrc(hit.songId)
    if (qrc) {
      return { text: qrc, format: 'qrc' }
    }
  }

  const lrc = await fetchTxLrcByMid(hit.mid)
  if (!lrc) {
    return null
  }
  // 明文 qrc 误入 LRC 接口时
  if (/^\[\d+,\d+\]/m.test(lrc) && /\(\d+,\d+\)/.test(lrc)) {
    return { text: lrc, format: 'qrc' }
  }
  return { text: lrc, format: 'lrc' }
}

export const txLyricsProvider: LyricsProvider = {
  id: 'tx',
  searchLyrics: searchTxLyrics,
}
