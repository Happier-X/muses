import { getTitleFromPath } from '@/features/library/audio'
import { normalizeText } from '@/features/lyrics/normalize'
import type { OnlineTextQuery, TextMetaHit } from './types'

export const isBlank = (value: string | undefined | null): boolean => !value?.trim()

/**
 * 弱 title：与去扩展名文件名 normalize 后相等（扫描无内嵌标题时的兜底形态）。
 * path 无效或基名为空时不视为弱，避免误覆盖。
 */
export const isWeakTitle = (title: string | undefined | null, path: string | undefined | null): boolean => {
  const t = title?.trim()
  const p = path?.trim()
  if (!t || !p) {
    return false
  }
  const base = getTitleFromPath(p).trim()
  if (!base) {
    return false
  }
  return normalizeText(t) === normalizeText(base)
}

/** 标题相关：normalize 后相等或互相包含 */
export const titlesRelated = (
  a: string | undefined | null,
  b: string | undefined | null,
): boolean => {
  const na = normalizeText(a)
  const nb = normalizeText(b)
  if (!na || !nb) {
    return false
  }
  return na === nb || na.includes(nb) || nb.includes(na)
}

export type OnlineTextNeedQuery = Pick<OnlineTextQuery, 'title' | 'artist' | 'album' | 'path'>

/** artist/album 空，或 title 为弱标签时需要匹配 */
export const needsOnlineTextMeta = (query: OnlineTextNeedQuery): boolean =>
  isBlank(query.artist) || isBlank(query.album) || isWeakTitle(query.title, query.path)

export const buildKeyword = (query: OnlineTextQuery): string =>
  [query.title, query.artist, query.album].filter((part) => part?.trim()).join(' ').trim()

/**
 * 命中是否对当前缺口有用：补空 artist/album，或弱 title 的相关 title。
 */
export const hitFillsMissing = (hit: TextMetaHit, query: OnlineTextNeedQuery): boolean => {
  const needArtist = isBlank(query.artist)
  const needAlbum = isBlank(query.album)
  if (needArtist && hit.artist?.trim()) {
    return true
  }
  if (needAlbum && hit.album?.trim()) {
    return true
  }
  if (
    isWeakTitle(query.title, query.path)
    && hit.title?.trim()
    && titlesRelated(hit.title, query.title)
  ) {
    return true
  }
  return false
}

type MergeBase = {
  title: string
  path?: string
  artist?: string
  album?: string
}

/**
 * 合并：弱 title + 相关 hit.title 可改 title；artist/album 仅补空。
 */
export const mergeTextMetaFillEmpty = <T extends MergeBase>(
  latest: T,
  hit: Pick<TextMetaHit, 'title' | 'artist' | 'album'>,
): { next: T; changed: boolean } => {
  const weak = isWeakTitle(latest.title, latest.path)
  const hitTitle = hit.title?.trim()
  const canWriteTitle = weak && !!hitTitle && titlesRelated(hitTitle, latest.title)

  const nextTitle = canWriteTitle ? hitTitle! : latest.title
  const nextArtist = !isBlank(latest.artist) ? latest.artist : (hit.artist?.trim() || latest.artist)
  const nextAlbum = !isBlank(latest.album) ? latest.album : (hit.album?.trim() || latest.album)

  const changed =
    nextTitle !== latest.title
    || (nextArtist || '') !== (latest.artist || '')
    || (nextAlbum || '') !== (latest.album || '')

  return {
    next: {
      ...latest,
      title: nextTitle,
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
