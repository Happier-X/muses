/**
 * 网易云封面：公开搜索 + song/detail 取 album.picUrl。
 * 不用 weapi/eapi 加密；独立实现。
 */
import { httpGetJson } from '../http'
import type { CoverProvider, OnlineCoverQuery } from '../types'
import { normalizeText } from '@/features/lyrics/normalize'

type WyArtist = {
  name?: string
}

type WyAlbum = {
  name?: string
  picUrl?: string
}

type WySearchSong = {
  id?: number
  name?: string
  artists?: WyArtist[]
  album?: WyAlbum
}

type WySearchResponse = {
  result?: {
    songs?: WySearchSong[]
  }
}

type WyDetailResponse = {
  songs?: Array<{
    album?: {
      picUrl?: string
    }
  }>
}

const WY_SEARCH = 'https://music.163.com/api/search/get/web'
const WY_DETAIL = 'https://music.163.com/api/song/detail/'

const WY_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0',
  Referer: 'https://music.163.com/',
  Accept: 'application/json,text/plain,*/*',
}

const MAX_DETAIL_TRIES = 3

const normalizeCoverUrl = (raw: string | undefined): string | null => {
  const trimmed = raw?.trim()
  if (!trimmed || !/^https?:\/\//i.test(trimmed)) {
    return null
  }
  if (trimmed.startsWith('http://')) {
    return `https://${trimmed.slice('http://'.length)}`
  }
  return trimmed
}

const artistNames = (item: WySearchSong): string =>
  (item.artists ?? []).map((a) => a.name?.trim() || '').filter(Boolean).join(' ')

const scoreItem = (item: WySearchSong, query: OnlineCoverQuery): number => {
  const title = normalizeText(item.name)
  const artist = normalizeText(artistNames(item))
  const album = normalizeText(item.album?.name)
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
  if (typeof item.id === 'number' && item.id > 0) {
    score += 1
  }
  return score
}

const fetchDetailPicUrl = async (songId: number): Promise<string | null> => {
  const url = `${WY_DETAIL}?ids=${encodeURIComponent(JSON.stringify([songId]))}`
  const body = await httpGetJson<WyDetailResponse>(url, WY_HEADERS)
  const pic = body.songs?.[0]?.album?.picUrl
  return normalizeCoverUrl(pic)
}

export const searchWyCoverUrl = async (query: OnlineCoverQuery): Promise<string | null> => {
  const keyword = [query.title, query.artist, query.album].filter((part) => part?.trim()).join(' ').trim()
  if (!keyword) {
    return null
  }

  const searchUrl =
    `${WY_SEARCH}?s=${encodeURIComponent(keyword)}&type=1&offset=0&total=true&limit=10`

  const body = await httpGetJson<WySearchResponse>(searchUrl, WY_HEADERS)
  const list = Array.isArray(body.result?.songs) ? body.result!.songs! : []
  const withId = list.filter((item) => typeof item.id === 'number' && item.id > 0)
  if (withId.length === 0) {
    return null
  }

  const ranked = [...withId].sort((a, b) => scoreItem(b, query) - scoreItem(a, query))
  const candidates = ranked.slice(0, MAX_DETAIL_TRIES)

  for (const item of candidates) {
    try {
      const pic = await fetchDetailPicUrl(item.id as number)
      if (pic) {
        return pic
      }
    } catch {
      // 单条详情失败则试下一条
    }
  }

  return null
}

export const wyCoverProvider: CoverProvider = {
  id: 'wy',
  searchCoverUrl: searchWyCoverUrl,
}
