/**
 * 咪咕歌词：移动搜索结果中的歌词相关 URL（若有）或 songId 详情。
 * 公开路径不稳定时返回 null，由链上下一源承接。
 */
import { httpGetJson, httpGetText } from '@/features/cover/http'
import type { LyricsProvider, OnlineLyricsQuery } from './types'
import { buildKeyword, pickBest } from './util'

type MiguMusicItem = {
  songName?: string
  singerName?: string
  albumName?: string
  copyrightId?: string
  id?: string
  songId?: string
  lrcUrl?: string
  lyrics?: string
  hasLyric?: boolean
}

type MiguSearchResponse = {
  musics?: MiguMusicItem[]
}

const MIGU_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0',
  Referer: 'https://m.music.migu.cn/',
  Accept: 'application/json,text/plain,*/*',
}

export const searchMgLyrics = async (
  query: OnlineLyricsQuery,
): Promise<{ text: string; format: 'lrc' } | null> => {
  const keyword = buildKeyword(query)
  if (!keyword) {
    return null
  }

  const searchUrl =
    `https://m.music.migu.cn/migu/remoting/scr_search_tag?rows=10&type=2` +
    `&keyword=${encodeURIComponent(keyword)}&pgc=1`

  let body: MiguSearchResponse
  try {
    body = await httpGetJson<MiguSearchResponse>(searchUrl, MIGU_HEADERS)
  } catch {
    return null
  }

  const list = (body.musics ?? []).map((item) => ({
    title: item.songName,
    artist: item.singerName,
    album: item.albumName,
    lrcUrl: item.lrcUrl,
    lyrics: item.lyrics,
    id: item.copyrightId || item.songId || item.id,
  }))

  const best = pickBest(list, query)
  if (!best) {
    return null
  }

  if (best.lyrics?.trim() && /\[\d+:\d+/.test(best.lyrics)) {
    return { text: best.lyrics.trim(), format: 'lrc' }
  }

  if (best.lrcUrl?.trim()) {
    try {
      const text = (await httpGetText(best.lrcUrl.trim(), MIGU_HEADERS)).trim()
      if (text && /\[\d+:\d+/.test(text)) {
        return { text, format: 'lrc' }
      }
    } catch {
      return null
    }
  }

  return null
}

export const mgLyricsProvider: LyricsProvider = {
  id: 'mg',
  searchLyrics: searchMgLyrics,
}
