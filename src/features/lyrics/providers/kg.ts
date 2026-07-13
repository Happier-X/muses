/**
 * 酷狗歌词：song_search_v2 → lyrics.kugou.com search → download。
 * krc 需解码；MVP 优先取 lrc 明文（contenttype）。
 */
import { httpGetJson } from '@/features/cover/http'
import type { LyricsProvider, OnlineLyricsQuery } from './types'
import { buildKeyword, pickBest } from './util'

type KgMusicItem = {
  SongName?: string
  OriSongName?: string
  SingerName?: string
  AlbumName?: string
  FileHash?: string
  Duration?: number
  FileName?: string
}

type KgSearchResponse = {
  data?: {
    lists?: KgMusicItem[]
  }
}

type KgLyricSearch = {
  candidates?: Array<{
    id?: string
    accesskey?: string
    contenttype?: number
    krctype?: number
  }>
}

type KgLyricDownload = {
  content?: string
  contenttype?: number
  fmt?: string
  status?: number
}

const KG_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0',
  Referer: 'https://www.kugou.com/',
  Accept: 'application/json,text/plain,*/*',
}

const KG_LYRIC_HEADERS = {
  'User-Agent': 'KuGou2012-9020-ExpandSearchManager',
  'KG-RC': '1',
  'KG-THash': 'expand_search_manager.cpp:852736169:451',
}

const decodeBase64Utf8 = (b64: string): string => {
  try {
    if (typeof atob !== 'function') {
      return ''
    }
    const bin = atob(b64)
    try {
      return decodeURIComponent(
        Array.from(bin, (c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`).join(''),
      )
    } catch {
      return bin
    }
  } catch {
    return ''
  }
}

const searchKgTrack = async (
  query: OnlineLyricsQuery,
): Promise<{ hash: string; durationMs: number; keyword: string } | null> => {
  const keyword = buildKeyword(query)
  if (!keyword) {
    return null
  }
  const url =
    `https://songsearch.kugou.com/song_search_v2?keyword=${encodeURIComponent(keyword)}` +
    `&page=1&pagesize=10&userid=0&clientver=&platform=WebFilter` +
    `&filter=2&iscorrection=1&privilege_filter=0&area_code=1`

  const body = await httpGetJson<KgSearchResponse>(url, KG_HEADERS)
  const list = (body.data?.lists ?? [])
    .filter((item) => item.FileHash?.trim())
    .map((item) => ({
      hash: item.FileHash!.trim(),
      durationMs: Math.max(0, Math.round((item.Duration || 0) * 1000)),
      keyword: item.FileName || item.SongName || keyword,
      title: item.SongName || item.OriSongName,
      artist: item.SingerName,
      album: item.AlbumName,
    }))

  const best = pickBest(list, query)
  if (!best) {
    return null
  }
  return { hash: best.hash, durationMs: best.durationMs, keyword: best.keyword }
}

export const searchKgLyrics = async (
  query: OnlineLyricsQuery,
): Promise<{ text: string; format: 'lrc' } | null> => {
  const track = await searchKgTrack(query)
  if (!track) {
    return null
  }

  const searchUrl =
    `http://lyrics.kugou.com/search?ver=1&man=yes&client=pc` +
    `&keyword=${encodeURIComponent(track.keyword)}` +
    `&hash=${encodeURIComponent(track.hash)}` +
    `&timelength=${track.durationMs}&lrctxt=1`

  const searchBody = await httpGetJson<KgLyricSearch>(searchUrl, KG_LYRIC_HEADERS)
  const cand = searchBody.candidates?.[0]
  if (!cand?.id || !cand.accesskey) {
    return null
  }

  // fmt=lrc 优先明文
  const dlUrl =
    `http://lyrics.kugou.com/download?ver=1&client=pc&id=${encodeURIComponent(cand.id)}` +
    `&accesskey=${encodeURIComponent(cand.accesskey)}&fmt=lrc&charset=utf8`

  const dl = await httpGetJson<KgLyricDownload>(dlUrl, KG_LYRIC_HEADERS)
  const content = dl.content?.trim()
  if (!content) {
    return null
  }

  const text = content.includes('[') ? content : decodeBase64Utf8(content)
  if (!text.trim() || !/\[\d+:\d+/.test(text)) {
    return null
  }
  return { text, format: 'lrc' }
}

export const kgLyricsProvider: LyricsProvider = {
  id: 'kg',
  searchLyrics: searchKgLyrics,
}
