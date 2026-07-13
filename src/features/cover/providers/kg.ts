/**
 * 酷狗封面：使用 songsearch.kugou.com 公开搜索接口返回的 Image 字段。
 * 独立实现，不拷贝 GPL 项目源码。
 */
import { httpGetJson } from '../http'
import type { CoverProvider, OnlineCoverQuery } from '../types'
import { normalizeText } from '@/features/lyrics/normalize'

type KgMusicItem = {
  SongName?: string
  OriSongName?: string
  SingerName?: string
  AlbumName?: string
  AlbumID?: string
  Image?: string
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

const KG_COVER_SIZE = '480'

const normalizeCoverUrl = (raw: string | undefined): string | null => {
  const trimmed = raw?.trim()
  if (!trimmed || !/^https?:\/\//i.test(trimmed)) {
    return null
  }
  const withSize = trimmed.includes('{size}') ? trimmed.replace('{size}', KG_COVER_SIZE) : trimmed
  if (withSize.startsWith('http://')) {
    return `https://${withSize.slice('http://'.length)}`
  }
  return withSize
}

const scoreItem = (item: KgMusicItem, query: OnlineCoverQuery): number => {
  const title = normalizeText(item.SongName || item.OriSongName)
  const artist = normalizeText(item.SingerName)
  const album = normalizeText(item.AlbumName)
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
  if (normalizeCoverUrl(item.Image)) {
    score += 1
  }
  return score
}

export const searchKgCoverUrl = async (query: OnlineCoverQuery): Promise<string | null> => {
  const keyword = [query.title, query.artist, query.album].filter((part) => part?.trim()).join(' ').trim()
  if (!keyword) {
    return null
  }

  const url =
    `${KG_SEARCH}?keyword=${encodeURIComponent(keyword)}` +
    `&page=1&pagesize=10&userid=0&clientver=&platform=WebFilter` +
    `&filter=2&iscorrection=1&privilege_filter=0&area_code=1`

  const body = await httpGetJson<KgSearchResponse>(url, KG_HEADERS)
  const list = Array.isArray(body.data?.lists) ? body.data!.lists : []
  const withCover = list.filter((item) => normalizeCoverUrl(item.Image))
  if (withCover.length === 0) {
    return null
  }

  const ranked = [...withCover].sort((a, b) => scoreItem(b, query) - scoreItem(a, query))
  return normalizeCoverUrl(ranked[0]?.Image)
}

export const kgCoverProvider: CoverProvider = {
  id: 'kg',
  searchCoverUrl: searchKgCoverUrl,
}
