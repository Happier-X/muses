/**
 * 网易云歌词：优先 eapi 拿 yrc，否则公开 API / 行级 LRC。
 * 逐字 yrc 用 amll parseYrc。
 */
import { httpGetJson, httpPostJson } from '@/features/cover/http'
import type { LyricsProvider, OnlineLyricsQuery } from './types'
import { buildKeyword, pickBest } from './util'
import { buildEapiParams } from './wyCrypto'
import { looksLikeWordLevelBracket } from './qrc'

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

const WY_EAPI_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36',
  origin: 'https://music.163.com',
  Referer: 'https://music.163.com/',
  'Content-Type': 'application/x-www-form-urlencoded',
  Accept: '*/*',
}

const isYrcBody = (text: string): boolean => {
  if (!text.trim()) {
    return false
  }
  // 含 [start,dur](t,d,0) 或至少一行 [n,n]
  if (/\(\d+,\d+,\d+\)/.test(text) && looksLikeWordLevelBracket(text)) {
    return true
  }
  return looksLikeWordLevelBracket(text) && text.includes('(')
}

const pickFromBody = (
  body: WyLyricResponse,
): { text: string; format: 'lrc' | 'yrc' } | null => {
  const yrc = body.yrc?.lyric?.trim()
  if (yrc && isYrcBody(yrc)) {
    return { text: yrc, format: 'yrc' }
  }
  const lrc = body.lrc?.lyric?.trim()
  if (lrc && /\[\d+:\d+/.test(lrc)) {
    return { text: lrc, format: 'lrc' }
  }
  return null
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

const fetchWyEapiLyric = async (id: number): Promise<WyLyricResponse | null> => {
  const apiPath = '/api/song/lyric/v1'
  const payload = {
    id,
    cp: false,
    tv: 0,
    lv: 0,
    rv: 0,
    kv: 0,
    yv: 0,
    ytv: 0,
    yrv: 0,
  }
  try {
    const params = buildEapiParams(apiPath, payload)
    return await httpPostJson<WyLyricResponse>(
      'https://interface3.music.163.com/eapi/song/lyric/v1',
      `params=${params}`,
      WY_EAPI_HEADERS,
    )
  } catch {
    return null
  }
}

const fetchWyPublicLyric = async (id: number): Promise<WyLyricResponse | null> => {
  try {
    const url =
      `https://music.163.com/api/song/lyric?id=${id}&lv=-1&kv=-1&tv=-1&rv=-1&yv=1`
    return await httpGetJson<WyLyricResponse>(url, WY_HEADERS)
  } catch {
    return null
  }
}

export const searchWyLyrics = async (
  query: OnlineLyricsQuery,
): Promise<{ text: string; format: 'lrc' | 'yrc' } | null> => {
  const id = await searchWySongId(query)
  if (!id) {
    return null
  }

  const eapiBody = await fetchWyEapiLyric(id)
  if (eapiBody) {
    const hit = pickFromBody(eapiBody)
    if (hit) {
      return hit
    }
  }

  const publicBody = await fetchWyPublicLyric(id)
  if (publicBody) {
    return pickFromBody(publicBody)
  }
  return null
}

export const wyLyricsProvider: LyricsProvider = {
  id: 'wy',
  searchLyrics: searchWyLyrics,
}
