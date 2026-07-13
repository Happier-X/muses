/**
 * QQ 歌词：移动搜索 + fcg_query_lyric_new。
 * 优先尝试可解析 qrc；否则 plain/base64 LRC。
 */
import { httpGetJson, httpGetText } from '@/features/cover/http'
import type { LyricsProvider, OnlineLyricsQuery } from './types'
import { buildKeyword, pickBest } from './util'

type TxSongItem = {
  songname?: string
  albumname?: string
  songmid?: string
  singer?: Array<{ name?: string }>
}

type TxSearchResponse = {
  data?: {
    song?: {
      list?: TxSongItem[]
    }
  }
}

type TxLyricResponse = {
  code?: number
  lyric?: string
  qrc?: string
}

const TX_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1',
  Referer: 'https://y.qq.com/',
  Accept: 'application/json,text/plain,*/*',
}

const decodeMaybeBase64 = (raw: string): string => {
  const trimmed = raw.trim()
  if (!trimmed) {
    return ''
  }
  // 已是 LRC
  if (trimmed.includes('[') && /\[\d+:\d+/.test(trimmed)) {
    return trimmed
  }
  try {
    if (typeof atob === 'function') {
      const bin = atob(trimmed)
      // UTF-8
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

const searchTxSongMid = async (query: OnlineLyricsQuery): Promise<string | null> => {
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
    .map((item) => ({
      mid: item.songmid?.trim() || '',
      title: item.songname,
      artist: (item.singer ?? []).map((s) => s.name || '').filter(Boolean).join(' '),
      album: item.albumname,
    }))
    .filter((item) => item.mid)

  return pickBest(list, query)?.mid ?? null
}

export const searchTxLyrics = async (
  query: OnlineLyricsQuery,
): Promise<{ text: string; format: 'lrc' | 'qrc' } | null> => {
  const songmid = await searchTxSongMid(query)
  if (!songmid) {
    return null
  }

  // nobase64=1 直接文本 LRC（最稳）
  const url =
    `https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=${encodeURIComponent(songmid)}` +
    `&format=json&nobase64=1&g_tk=5381&loginUin=0&hostUin=0&inCharset=utf8&outCharset=utf-8&notice=0&platform=yqq.json&needNewCode=0`

  let raw: string
  try {
    raw = await httpGetText(url, TX_HEADERS)
  } catch {
    return null
  }

  // 可能 jsonp
  const jsonText = raw.replace(/^\s*[a-zA-Z0-9_]*\(/, '').replace(/\)\s*;?\s*$/, '')
  let body: TxLyricResponse
  try {
    body = JSON.parse(jsonText) as TxLyricResponse
  } catch {
    return null
  }

  const lyric = decodeMaybeBase64(body.lyric || '')
  if (!lyric.trim() || !/\[\d+:\d+/.test(lyric)) {
    return null
  }
  // QQ 接口常给行级 LRC；若将来带 qrc 明文可走 parseQrc
  if (body.qrc && body.qrc.includes('(') && body.qrc.includes('[')) {
    return { text: body.qrc, format: 'qrc' }
  }
  return { text: lyric, format: 'lrc' }
}

export const txLyricsProvider: LyricsProvider = {
  id: 'tx',
  searchLyrics: searchTxLyrics,
}
