import { httpGetJson } from '../http'
import type { CoverProvider, OnlineCoverQuery } from '../types'
import { normalizeText } from '@/features/lyrics/normalize'

type ItunesSongResult = {
  trackName?: string
  artistName?: string
  collectionName?: string
  artworkUrl100?: string
}

type ItunesSearchResponse = {
  resultCount?: number
  results?: ItunesSongResult[]
}

const enlargeArtworkUrl = (url: string): string => {
  // artworkUrl100 形如 .../100x100bb.jpg → 放大到 600
  return url.replace(/\/\d+x\d+([a-z]*)\./i, '/600x600$1.')
}

const scoreResult = (item: ItunesSongResult, query: OnlineCoverQuery): number => {
  const title = normalizeText(item.trackName)
  const artist = normalizeText(item.artistName)
  const album = normalizeText(item.collectionName)
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
  if (item.artworkUrl100) {
    score += 1
  }
  return score
}

export const searchItunesCoverUrl = async (query: OnlineCoverQuery): Promise<string | null> => {
  const term = [query.title, query.artist, query.album].filter((part) => part?.trim()).join(' ').trim()
  if (!term) {
    return null
  }

  const url =
    `https://itunes.apple.com/search?term=${encodeURIComponent(term)}` +
    '&entity=song&limit=5'

  const body = await httpGetJson<ItunesSearchResponse>(url, {
    Accept: 'application/json',
  })

  const results = (body.results ?? []).filter((item) => Boolean(item.artworkUrl100))
  if (results.length === 0) {
    return null
  }

  const ranked = [...results].sort((a, b) => scoreResult(b, query) - scoreResult(a, query))
  const best = ranked[0]
  if (!best?.artworkUrl100 || scoreResult(best, query) < 10) {
    // 至少要求标题有一定相关性；全无关时放弃，交给回退源
    if (!best?.artworkUrl100) {
      return null
    }
    // 若标题完全对不上，仍允许最高分结果（宽松）仅当唯一结果
    if (scoreResult(best, query) < 1 && results.length > 1) {
      return null
    }
  }

  return enlargeArtworkUrl(best.artworkUrl100)
}

export const itunesCoverProvider: CoverProvider = {
  id: 'itunes',
  searchCoverUrl: searchItunesCoverUrl,
}
