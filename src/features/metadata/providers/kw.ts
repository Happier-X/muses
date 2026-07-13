import { httpGetText } from '@/features/cover/http'
import type { OnlineTextQuery, TextMetaHit, TextMetaProvider } from '../types'
import { buildKeyword, pickBestHit } from '../util'

type KwAbsItem = {
  SONGNAME?: string
  ARTIST?: string
  ALBUM?: string
}

type KwSearchResult = {
  abslist?: KwAbsItem[]
}

const buildSearchUrl = (keyword: string): string =>
  `https://search.kuwo.cn/r.s?client=kt&all=${encodeURIComponent(keyword)}` +
  `&pn=0&rn=10&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1` +
  '&show_copyright_off=1&newver=1&ft=music&cluster=0&strategy=2012' +
  '&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1'

export const searchKwTextMeta = async (query: OnlineTextQuery): Promise<TextMetaHit | null> => {
  const keyword = buildKeyword(query)
  if (!keyword) {
    return null
  }

  const raw = await httpGetText(buildSearchUrl(keyword), {
    'User-Agent': 'Mozilla/5.0',
    Accept: 'application/json,text/plain,*/*',
  })

  let list: KwAbsItem[] = []
  try {
    const body = JSON.parse(raw) as KwSearchResult
    list = Array.isArray(body.abslist) ? body.abslist : []
  } catch {
    return null
  }

  const hits: TextMetaHit[] = list
    .map((item) => ({
      title: item.SONGNAME?.trim() || undefined,
      artist: item.ARTIST?.trim() || undefined,
      album: item.ALBUM?.trim() || undefined,
      source: 'kw' as const,
    }))
    .filter((hit) => hit.artist || hit.album)

  return pickBestHit(hits, query)
}

export const kwTextMetaProvider: TextMetaProvider = {
  id: 'kw',
  search: searchKwTextMeta,
}
