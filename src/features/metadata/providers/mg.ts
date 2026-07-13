import { httpGetJson } from '@/features/cover/http'
import type { OnlineTextQuery, TextMetaHit, TextMetaProvider } from '../types'
import { buildKeyword, pickBestHit } from '../util'

type MiguMusicItem = {
  songName?: string
  singerName?: string
  albumName?: string
}

type MiguSearchResponse = {
  musics?: MiguMusicItem[]
}

const MIGU_SEARCH = 'https://m.music.migu.cn/migu/remoting/scr_search_tag'

const MIGU_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0',
  Referer: 'https://m.music.migu.cn/',
  Accept: 'application/json,text/plain,*/*',
}

export const searchMgTextMeta = async (query: OnlineTextQuery): Promise<TextMetaHit | null> => {
  const keyword = buildKeyword(query)
  if (!keyword) {
    return null
  }

  const url = `${MIGU_SEARCH}?rows=10&type=2&keyword=${encodeURIComponent(keyword)}&pgc=1`
  const body = await httpGetJson<MiguSearchResponse>(url, MIGU_HEADERS)
  const list = Array.isArray(body.musics) ? body.musics : []

  const hits: TextMetaHit[] = list
    .map((item) => ({
      title: item.songName?.trim() || undefined,
      artist: item.singerName?.trim() || undefined,
      album: item.albumName?.trim() || undefined,
      source: 'mg' as const,
    }))
    .filter((hit) => hit.artist || hit.album)

  return pickBestHit(hits, query)
}

export const mgTextMetaProvider: TextMetaProvider = {
  id: 'mg',
  search: searchMgTextMeta,
}
