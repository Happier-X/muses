import { httpGetJson } from '@/features/cover/http'
import type { OnlineTextQuery, TextMetaHit, TextMetaProvider } from '../types'
import { buildKeyword, pickBestHit } from '../util'

type KgMusicItem = {
  SongName?: string
  OriSongName?: string
  SingerName?: string
  AlbumName?: string
}

type KgSearchResponse = {
  data?: {
    lists?: KgMusicItem[]
  }
}

const KG_SEARCH = 'https://songsearch.kugou.com/song_search_v2'

const KG_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0',
  Referer: 'https://www.kugou.com/',
  Accept: 'application/json,text/plain,*/*',
}

export const searchKgTextMeta = async (query: OnlineTextQuery): Promise<TextMetaHit | null> => {
  const keyword = buildKeyword(query)
  if (!keyword) {
    return null
  }

  const url =
    `${KG_SEARCH}?keyword=${encodeURIComponent(keyword)}` +
    `&page=1&pagesize=10&userid=0&clientver=&platform=WebFilter` +
    `&filter=2&iscorrection=1&privilege_filter=0&area_code=1`

  const body = await httpGetJson<KgSearchResponse>(url, KG_HEADERS)
  const list = Array.isArray(body.data?.lists) ? body.data!.lists! : []

  const hits: TextMetaHit[] = list
    .map((item) => ({
      title: (item.SongName || item.OriSongName)?.trim() || undefined,
      artist: item.SingerName?.trim() || undefined,
      album: item.AlbumName?.trim() || undefined,
      source: 'kg' as const,
    }))
    .filter((hit) => hit.artist || hit.album)

  return pickBestHit(hits, query)
}

export const kgTextMetaProvider: TextMetaProvider = {
  id: 'kg',
  search: searchKgTextMeta,
}
