/**
 * 咪咕封面：使用 m.music.migu.cn 移动搜索接口返回的 cover 字段。
 * 独立实现，不依赖 jadeite 签名链路，也不拷贝 GPL 项目源码。
 */
import { httpGetJson } from '../http'
import type { CoverProvider, OnlineCoverQuery } from '../types'
import { normalizeText } from '@/features/lyrics/normalize'

type MiguMusicItem = {
  songName?: string
  singerName?: string
  albumName?: string
  cover?: string
}

type MiguSearchResponse = {
  musics?: MiguMusicItem[]
}

const MIGU_SEARCH =
  'https://m.music.migu.cn/migu/remoting/scr_search_tag'

const MIGU_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0',
  Referer: 'https://m.music.migu.cn/',
  Accept: 'application/json,text/plain,*/*',
}

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

const scoreItem = (item: MiguMusicItem, query: OnlineCoverQuery): number => {
  const title = normalizeText(item.songName)
  const artist = normalizeText(item.singerName)
  const album = normalizeText(item.albumName)
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
  if (normalizeCoverUrl(item.cover)) {
    score += 1
  }
  return score
}

export const searchMgCoverUrl = async (query: OnlineCoverQuery): Promise<string | null> => {
  const keyword = [query.title, query.artist, query.album].filter((part) => part?.trim()).join(' ').trim()
  if (!keyword) {
    return null
  }

  const url =
    `${MIGU_SEARCH}?rows=10&type=2&keyword=${encodeURIComponent(keyword)}&pgc=1`

  const body = await httpGetJson<MiguSearchResponse>(url, MIGU_HEADERS)
  const list = Array.isArray(body.musics) ? body.musics : []
  const withCover = list.filter((item) => normalizeCoverUrl(item.cover))
  if (withCover.length === 0) {
    return null
  }

  const ranked = [...withCover].sort((a, b) => scoreItem(b, query) - scoreItem(a, query))
  return normalizeCoverUrl(ranked[0]?.cover)
}

export const mgCoverProvider: CoverProvider = {
  id: 'mg',
  searchCoverUrl: searchMgCoverUrl,
}
