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

/** 时长容忍阈值（秒）：匹配质量时长约束（child4 R4-1） */
export const DURATION_TOLERANCE_SEC = 5

/**
 * 置信度分级（child4 R4-1）：
 * - high：自动写库路径采纳门槛
 * - low：仅进候选供刮削页人工选择
 */
export type MatchConfidence = 'high' | 'low'

/**
 * 最低采纳分（child4）：保留导出兼容既有 minScore 调用。
 * 歌名至少达到「包含」级（60）才进入打分；高置信门槛由 classifyMatch 附加判定。
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

/**
 * 判定单条匹配的置信度（child4 R4-1）。
 * - 时长偏差超阈值直接降为 low
 * - title exact 且（无歌手信息 或 artist 命中）→ high
 * - title contains 且 artist 命中（查询与候选都有歌手信息且命中）→ high
 * - 其余 → low（includes contains 无 artist 信息、artist 不命中）
 */
export const classifyMatch = (
  query: Pick<AmllMatchQuery, 'title' | 'artist' | 'album' | 'duration'>,
  entry: AmllIndexEntry,
  titleMatch: { level: TitleMatchLevel; score: number },
  artistScore: number,
): MatchConfidence => {
  // 时长约束：双方都有 duration 且偏差超阈值 → low
  if (query.duration && entry.duration) {
    if (Math.abs(query.duration - entry.duration) > DURATION_TOLERANCE_SEC) {
      return 'low'
    }
  }
  const queryArtists = splitArtistTokens(query.artist)
  const candidateArtists = entry.artists.flatMap((artist) => splitArtistTokens(artist))
  const artistProvided = queryArtists.length > 0 && candidateArtists.length > 0
  const artistHit = artistProvided && artistScore > 0
  if (titleMatch.level === 'exact') {
    // exact 无歌手信息可采纳；有歌手信息需命中
    return artistProvided && !artistHit ? 'low' : 'high'
  }
  if (titleMatch.level === 'contains') {
    // contains 必须 artist 命中才 high
    return artistHit ? 'high' : 'low'
  }
  return 'low'
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
  /** 该最佳匹配的置信度（child4）；自动写库路径应校验为 'high' */
  confidence: MatchConfidence
}

export interface FindBestMatchOptions {
  /** 最低总分，默认 MIN_ACCEPT_SCORE（向后兼容） */
  minScore?: number
  /**
   * 最低置信度门槛（child4 R4-1）：
   * - 'high'（默认）：仅采纳高置信匹配，低置信进候选不自动写库
   * - 'low'：放宽到低置信，供刮削页候选选择场景
   */
  minConfidence?: MatchConfidence
}

/**
 * 在索引中选取最高分且不低于阈值的一条；无则 null。
 * 默认 minConfidence='high'：自动写库路径仅采纳高置信（child4）。
 * 第三参支持 number（旧 minScore）与 FindBestMatchOptions 两种形态以保持向后兼容。
 */
export const findBestMatch = (
  query: Pick<AmllMatchQuery, 'title' | 'artist' | 'album' | 'duration'>,
  index: AmllIndexEntry[],
  options: FindBestMatchOptions | number = {},
): BestMatch | null => {
  const opts: FindBestMatchOptions = typeof options === 'number'
    ? { minScore: options }
    : options
  const minScore = opts.minScore ?? MIN_ACCEPT_SCORE
  const minConfidence: MatchConfidence = opts.minConfidence ?? 'high'

  let best: BestMatch | null = null

  for (const entry of index) {
    const score = scoreEntry(query, entry)
    if (score < minScore) {
      continue
    }
    const titleMatch = scoreTitle(query.title, entry.musicName)
    const artistScore = scoreArtists(query.artist, entry.artists)
    const confidence = classifyMatch(query, entry, titleMatch, artistScore)
    if (minConfidence === 'high' && confidence !== 'high') {
      continue
    }
    if (!best || score > best.score) {
      best = { entry, score, confidence }
    }
  }

  return best
}
