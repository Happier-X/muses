import { httpGetJson } from '@/features/cover/http'
import type { OnlineTextQuery, TextMetaHit, TextMetaProvider } from '../types'
import { buildKeyword, pickBestHit } from '../util'

type WySearchSong = {
  name?: string
  artists?: Array<{ name?: string }>
  album?: { name?: string }
}

type WySearchResponse = {
  result?: {
    songs?: WySearchSong[]
  }
}

const WY_SEARCH = 'https://music.163.com/api/search/get/web'

const WY_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0',
  Referer: 'https://music.163.com/',
  Accept: 'application/json,text/plain,*/*',
}

export const searchWyTextMeta = async (query: OnlineTextQuery): Promise<TextMetaHit | null> => {
  const keyword = buildKeyword(query)
  if (!keyword) {
    return null
  }

  const url = `${WY_SEARCH}?s=${encodeURIComponent(keyword)}&type=1&offset=0&total=true&limit=10`
  const body = await httpGetJson<WySearchResponse>(url, WY_HEADERS)
  const list = Array.isArray(body.result?.songs) ? body.result!.songs! : []

  const hits: TextMetaHit[] = list
    .map((item) => {
      const artist = (item.artists ?? [])
        .map((a) => a.name?.trim() || '')
        .filter(Boolean)
        .join(' ')
      return {
        title: item.name?.trim() || undefined,
        artist: artist || undefined,
        album: item.album?.name?.trim() || undefined,
        source: 'wy' as const,
      }
    })
    .filter((hit) => hit.artist || hit.album)

  return pickBestHit(hits, query)
}

export const wyTextMetaProvider: TextMetaProvider = {
  id: 'wy',
  search: searchWyTextMeta,
}
