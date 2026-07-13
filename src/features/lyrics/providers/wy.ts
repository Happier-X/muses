/**
 * 网易云歌词：公开 search + /api/song/lyric。
 * 优先 yrc（若返回），否则 lrc。逐字 yrc 用 amll parseYrc。
 */
import { httpGetJson } from '@/features/cover/http'
import type { LyricsProvider, OnlineLyricsQuery } from './types'
import { buildKeyword, pickBest } from './util'

type WySearchSong = {
  id?: number
  name?: string
  artists?: Array<{ name?: string }>
  album?: { name?: string }
}

type WySearchResponse = {
  result?: {
    songs?: WySearchSong[]
  }
}

type WyLyricResponse = {
  lrc?: { lyric?: string }
  yrc?: { lyric?: string }
  tlyric?: { lyric?: string }
  code?: number
}

const WY_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0',
  Referer: 'https://music.163.com/',
  Accept: 'application/json,text/plain,*/*',
}

const searchWySongId = async (query: OnlineLyricsQuery): Promise<number | null> => {
  const keyword = buildKeyword(query)
  if (!keyword) {
    return null
  }
  const url =
    `https://music.163.com/api/search/get/web?s=${encodeURIComponent(keyword)}` +
    `&type=1&offset=0&total=true&limit=10`
  const body = await httpGetJson<WySearchResponse>(url, WY_HEADERS)
  const list = (body.result?.songs ?? [])
    .filter((s) => typeof s.id === 'number' && s.id > 0)
    .map((s) => ({
      id: s.id as number,
      title: s.name,
      artist: (s.artists ?? []).map((a) => a.name || '').filter(Boolean).join(' '),
      album: s.album?.name,
    }))
  return pickBest(list, query)?.id ?? null
}

export const searchWyLyrics = async (
  query: OnlineLyricsQuery,
): Promise<{ text: string; format: 'lrc' | 'yrc' } | null> => {
  const id = await searchWySongId(query)
  if (!id) {
    return null
  }

  // yv=1 请求 yrc；公开 API 不一定返回，有则优先
  const url =
    `https://music.163.com/api/song/lyric?id=${id}&lv=-1&kv=-1&tv=-1&rv=-1&yv=1`
  const body = await httpGetJson<WyLyricResponse>(url, WY_HEADERS)

  const yrc = body.yrc?.lyric?.trim()
  if (yrc && (yrc.includes('(') || yrc.includes('['))) {
    // 网易 yrc 行格式 [start,duration](word times)
    if (/^\[?\d+,\d+\]/.test(yrc.split('\n').find((l) => l.trim()) || '') || yrc.includes('],[')) {
      return { text: yrc, format: 'yrc' }
    }
  }

  const lrc = body.lrc?.lyric?.trim()
  if (lrc && /\[\d+:\d+/.test(lrc)) {
    return { text: lrc, format: 'lrc' }
  }
  return null
}

export const wyLyricsProvider: LyricsProvider = {
  id: 'wy',
  searchLyrics: searchWyLyrics,
}
