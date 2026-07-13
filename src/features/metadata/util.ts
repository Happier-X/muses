import type { OnlineTextQuery, TextMetaHit } from './types'
import { normalizeText } from '@/features/lyrics/normalize'

export const isBlank = (value: string | undefined | null): boolean => !value?.trim()

/** artist 或 album 任一为空则需要匹配 */
export const needsOnlineTextMeta = (query: Pick<OnlineTextQuery, 'artist' | 'album'>): boolean =>
  isBlank(query.artist) || isBlank(query.album)

export const buildKeyword = (query: OnlineTextQuery): string =>
  [query.title, query.artist, query.album].filter((part) => part?.trim()).join(' ').trim()

/**
 * 命中是否对「当前仍缺字段」有用：至少补上一个空字段。
 */
export const hitFillsMissing = (
  hit: TextMetaHit,
  query: Pick<OnlineTextQuery, 'artist' | 'album'>,
): boolean => {
  const needArtist = isBlank(query.artist)
  const needAlbum = isBlank(query.album)
  if (needArtist && hit.artist?.trim()) {
    return true
  }
  if (needAlbum && hit.album?.trim()) {
    return true
  }
  return false
}

/**
 * 仅补缺合并：不覆盖已有非空 artist/album；永不改 title。
 */
export const mergeTextMetaFillEmpty = <T extends { artist?: string; album?: string }>(
  latest: T,
  hit: Pick<TextMetaHit, 'artist' | 'album'>,
): { next: T; changed: boolean } => {
  const nextArtist = !isBlank(latest.artist) ? latest.artist : (hit.artist?.trim() || latest.artist)
  const nextAlbum = !isBlank(latest.album) ? latest.album : (hit.album?.trim() || latest.album)
  const changed =
    (nextArtist || '') !== (latest.artist || '') || (nextAlbum || '') !== (latest.album || '')
  return {
    next: {
      ...latest,
      artist: nextArtist,
      album: nextAlbum,
    },
    changed,
  }
}

export const scoreTextHit = (
  hit: Pick<TextMetaHit, 'title' | 'artist' | 'album'>,
  query: OnlineTextQuery,
): number => {
  const title = normalizeText(hit.title)
  const artist = normalizeText(hit.artist)
  const album = normalizeText(hit.album)
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
  if (artist?.trim()) {
    score += 1
  }
  if (album?.trim()) {
    score += 1
  }
  return score
}

export const pickBestHit = (hits: TextMetaHit[], query: OnlineTextQuery): TextMetaHit | null => {
  if (hits.length === 0) {
    return null
  }
  const ranked = [...hits].sort((a, b) => scoreTextHit(b, query) - scoreTextHit(a, query))
  return ranked[0] ?? null
}
