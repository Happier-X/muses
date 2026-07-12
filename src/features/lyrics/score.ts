import { normalizeText, splitArtistTokens } from './normalize'
import type { AmllIndexEntry, AmllMatchQuery } from './types'

/** 打分权重 */
export const SCORE_WEIGHTS = {
  TITLE_EXACT: 100,
  TITLE_CONTAINS: 60,
  ARTIST_HIT: 25,
  ALBUM_EXACT: 15,
  ALBUM_CONTAINS: 8,
} as const

/**
 * 最低采纳分：歌名至少达到「包含」级（60）。
 * 仅歌手/专辑命中不足以采纳，避免误匹配。
 */
export const MIN_ACCEPT_SCORE = SCORE_WEIGHTS.TITLE_CONTAINS

export type TitleMatchLevel = 'exact' | 'contains' | 'none'

export const scoreTitle = (queryTitle: string, candidateTitle: string): { level: TitleMatchLevel; score: number } => {
  const q = normalizeText(queryTitle)
  const c = normalizeText(candidateTitle)
  if (!q || !c) {
    return { level: 'none', score: 0 }
  }
  if (q === c) {
    return { level: 'exact', score: SCORE_WEIGHTS.TITLE_EXACT }
  }
  if (q.includes(c) || c.includes(q)) {
    return { level: 'contains', score: SCORE_WEIGHTS.TITLE_CONTAINS }
  }
  return { level: 'none', score: 0 }
}

export const scoreArtists = (queryArtist: string | undefined, candidateArtists: string[]): number => {
  const queryTokens = splitArtistTokens(queryArtist)
  if (queryTokens.length === 0 || candidateArtists.length === 0) {
    // 无歌手信息不重罚
    return 0
  }

  const candidateTokens = candidateArtists.flatMap((artist) => splitArtistTokens(artist))
  if (candidateTokens.length === 0) {
    return 0
  }

  let hits = 0
  for (const q of queryTokens) {
    if (candidateTokens.some((c) => c === q || c.includes(q) || q.includes(c))) {
      hits += 1
    }
  }

  return hits > 0 ? SCORE_WEIGHTS.ARTIST_HIT * Math.min(hits, 2) : 0
}

export const scoreAlbum = (queryAlbum: string | undefined, candidateAlbum: string | undefined): number => {
  const q = normalizeText(queryAlbum)
  const c = normalizeText(candidateAlbum)
  if (!q || !c) {
    return 0
  }
  if (q === c) {
    return SCORE_WEIGHTS.ALBUM_EXACT
  }
  if (q.includes(c) || c.includes(q)) {
    return SCORE_WEIGHTS.ALBUM_CONTAINS
  }
  return 0
}

/** 对单条索引计算总分；歌名未达包含级时直接 0 */
export const scoreEntry = (query: Pick<AmllMatchQuery, 'title' | 'artist' | 'album'>, entry: AmllIndexEntry): number => {
  const title = scoreTitle(query.title, entry.musicName)
  if (title.level === 'none') {
    return 0
  }

  const queryArtists = splitArtistTokens(query.artist)
  const candidateArtists = entry.artists.flatMap((artist) => splitArtistTokens(artist))
  const artistScore = scoreArtists(query.artist, entry.artists)
  if (queryArtists.length > 0 && candidateArtists.length > 0 && artistScore === 0) {
    return 0
  }

  const albumScore = scoreAlbum(query.album, entry.album)
  if (title.level === 'contains' && artistScore === 0 && albumScore === 0) {
    return 0
  }

  return title.score + artistScore + albumScore
}

export interface BestMatch {
  entry: AmllIndexEntry
  score: number
}

/** 在索引中选取最高分且不低于阈值的一条；无则 null */
export const findBestMatch = (
  query: Pick<AmllMatchQuery, 'title' | 'artist' | 'album'>,
  index: AmllIndexEntry[],
  minScore: number = MIN_ACCEPT_SCORE,
): BestMatch | null => {
  let best: BestMatch | null = null

  for (const entry of index) {
    const score = scoreEntry(query, entry)
    if (score < minScore) {
      continue
    }
    if (!best || score > best.score) {
      best = { entry, score }
    }
  }

  return best
}
