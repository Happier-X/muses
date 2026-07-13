/**
 * QQ 音乐封面：移动端 search_for_qq_cp 取 albummid，拼 y.gtimg.cn 封面。
 * 不用桌面签名搜索；独立实现。
 */
import { httpGetJson } from '../http'
import type { CoverProvider, OnlineCoverQuery } from '../types'
import { normalizeText } from '@/features/lyrics/normalize'

type TxSinger = {
  name?: string
  mid?: string
}

type TxSongItem = {
  songname?: string
  albumname?: string
  albummid?: string
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

const albumCoverUrl = (albummid: string | undefined): string | null => {
  const mid = albummid?.trim()
  if (!mid || mid === '空' || mid === '0') {
    return null
  }
  return `https://y.gtimg.cn/music/photo_new/T002R500x500M000${mid}.jpg`
}

const singerNames = (item: TxSongItem): string =>
  (item.singer ?? []).map((s) => s.name?.trim() || '').filter(Boolean).join(' ')

const scoreItem = (item: TxSongItem, query: OnlineCoverQuery): number => {
  const title = normalizeText(item.songname)
  const artist = normalizeText(singerNames(item))
  const album = normalizeText(item.albumname)
  const qTitle = normalizeText(query.title)
  const qArtist = normalizeText(query.artist)
  const qAlbum = normalizeText(query.album)

  let score = 0
  if (title && qTitle && (title === qTitle || title.includes(qTitle) || qTitle.includes(title))) {
    score += 10
  }
  if (artist && qArtist && (artist === qArtist || artist.includes(qArtist) || qArtist.includes(artist))) {
    score += 6
  }
  if (album && qAlbum && (album === qAlbum || album.includes(qAlbum) || qAlbum.includes(album))) {
    score += 3
  }
  if (albumCoverUrl(item.albummid)) {
    score += 1
  }
  return score
}

export const searchTxCoverUrl = async (query: OnlineCoverQuery): Promise<string | null> => {
  const keyword = [query.title, query.artist, query.album].filter((part) => part?.trim()).join(' ').trim()
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
  const withCover = list.filter((item) => albumCoverUrl(item.albummid))
  if (withCover.length === 0) {
    return null
  }

  const ranked = [...withCover].sort((a, b) => scoreItem(b, query) - scoreItem(a, query))
  return albumCoverUrl(ranked[0]?.albummid)
}

export const txCoverProvider: CoverProvider = {
  id: 'tx',
  searchCoverUrl: searchTxCoverUrl,
}
