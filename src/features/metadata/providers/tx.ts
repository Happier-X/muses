import { httpGetJson } from '@/features/cover/http'
import type { OnlineTextQuery, TextMetaHit, TextMetaProvider } from '../types'
import { buildKeyword, pickBestHit } from '../util'

type TxSinger = { name?: string }

type TxSongItem = {
  songname?: string
  albumname?: string
  singer?: TxSinger[]
}

type TxSearchResponse = {
  data?: {
    song?: {
      list?: TxSongItem[]
    }
  }
}

const TX_SEARCH = 'https://c.y.qq.com/soso/fcgi-bin/search_for_qq_cp'

const TX_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1',
  Referer: 'https://y.qq.com/',
  Accept: 'application/json,text/plain,*/*',
}

export const searchTxTextMeta = async (query: OnlineTextQuery): Promise<TextMetaHit | null> => {
  const keyword = buildKeyword(query)
  if (!keyword) {
    return null
  }

  const url =
    `${TX_SEARCH}?g_tk=5381&uin=0&format=json&inCharset=utf-8&outCharset=utf-8` +
    `&notice=0&platform=h5&needNewCode=1&w=${encodeURIComponent(keyword)}` +
    `&zhidaqu=1&catZhida=1&t=0&flag=1&ie=utf-8&sem=1&aggr=0` +
    `&perpage=10&n=10&p=1&remoteplace=txt.mqq.all`

  const body = await httpGetJson<TxSearchResponse>(url, TX_HEADERS)
  const list = Array.isArray(body.data?.song?.list) ? body.data!.song!.list! : []

  const hits: TextMetaHit[] = list
    .map((item) => {
      const artist = (item.singer ?? [])
        .map((s) => s.name?.trim() || '')
        .filter(Boolean)
        .join(' ')
      return {
        title: item.songname?.trim() || undefined,
        artist: artist || undefined,
        album: item.albumname?.trim() || undefined,
        source: 'tx' as const,
      }
    })
    .filter((hit) => hit.artist || hit.album)

  return pickBestHit(hits, query)
}

export const txTextMetaProvider: TextMetaProvider = {
  id: 'tx',
  search: searchTxTextMeta,
}
