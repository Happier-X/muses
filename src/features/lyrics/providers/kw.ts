/**
 * 酷我歌词：搜索 + openapi getlyric（行级 LRC）。
 */
import { httpGetJson } from '@/features/cover/http'
import type { LyricsProvider, OnlineLyricsQuery } from './types'
import { buildKeyword, linesToLrc, pickBest } from './util'

type KwAbsItem = {
  MUSICRID?: string
  SONGNAME?: string
  ARTIST?: string
  ALBUM?: string
}

type KwSearchResult = {
  abslist?: KwAbsItem[]
}

type KwLyricResponse = {
  data?: {
    lrclist?: Array<{ lineLyric?: string; time?: string }>
  }
}

const KW_HEADERS = {
  'User-Agent': 'Mozilla/5.0',
  Accept: 'application/json,text/plain,*/*',
  Referer: 'https://www.kuwo.cn/',
}

const extractId = (rid: string | undefined): string | null => {
  const raw = rid?.trim()
  if (!raw) {
    return null
  }
  return raw.replace(/^MUSIC_/i, '') || null
}

const searchKwMusicId = async (query: OnlineLyricsQuery): Promise<string | null> => {
  const keyword = buildKeyword(query)
  if (!keyword) {
    return null
  }
  const url =
    `https://search.kuwo.cn/r.s?client=kt&all=${encodeURIComponent(keyword)}` +
    `&pn=0&rn=10&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1` +
    '&show_copyright_off=1&newver=1&ft=music&cluster=0&strategy=2012' +
    '&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1'

  const body = await httpGetJson<KwSearchResult>(url, KW_HEADERS)
  const list: Array<{ id: string; title?: string; artist?: string; album?: string }> = []
  for (const item of body.abslist ?? []) {
    const id = extractId(item.MUSICRID)
    if (!id) {
      continue
    }
    list.push({
      id,
      title: item.SONGNAME,
      artist: item.ARTIST,
      album: item.ALBUM,
    })
  }

  return pickBest(list, query)?.id ?? null
}

export const searchKwLyrics = async (
  query: OnlineLyricsQuery,
): Promise<{ text: string; format: 'lrc' } | null> => {
  const musicId = await searchKwMusicId(query)
  if (!musicId) {
    return null
  }

  const lyricUrl = `https://www.kuwo.cn/openapi/v1/www/lyric/getlyric?musicId=${encodeURIComponent(musicId)}`
  const body = await httpGetJson<KwLyricResponse>(lyricUrl, KW_HEADERS)
  const list = body.data?.lrclist ?? []
  const lrc = linesToLrc(
    list.map((row) => ({
      time: row.time,
      text: row.lineLyric,
    })),
  )
  if (!lrc.trim()) {
    return null
  }
  return { text: lrc, format: 'lrc' }
}

export const kwLyricsProvider: LyricsProvider = {
  id: 'kw',
  searchLyrics: searchKwLyrics,
}
