import type { MatchConfidence } from '@/features/lyrics/score'

/** 在线歌词查询（编排层 / 各 provider 共用） */
export type OnlineLyricsQuery = {
  songId: string
  title: string
  artist?: string
  album?: string
  /** 秒；LRCLIB 等可用 */
  duration?: number
}

/** ttml=amll；lrc=行级；yrc/qrc=平台逐字（@applemusic-like-lyrics/lyric 可解析） */
export type OnlineLyricsFormat = 'ttml' | 'lrc' | 'yrc' | 'qrc'

export type OnlineLyricsSource =
  | 'amll'
  | 'kw'
  | 'tx'
  | 'wy'
  | 'kg'
  | 'mg'
  | 'lrclib'

/** 可插拔回退源（平台 / LRCLIB）；amll 在编排层单独调用 */
export type LyricsProvider = {
  id: Exclude<OnlineLyricsSource, 'amll'>
  searchLyrics: (
    query: OnlineLyricsQuery,
  ) => Promise<OnlineLyricsProviderHit | null>
}

/** provider 命中；translationText 为 timed LRC 译文（如网易 tlyric） */
export type OnlineLyricsProviderHit = {
  text: string
  format: OnlineLyricsFormat
  translationText?: string
}

export type OnlineLyricsMatchOk = {
  ok: true
  text: string
  format: OnlineLyricsFormat
  source: OnlineLyricsSource
  translationText?: string
  /**
   * 命中置信度（child4 R4-3）：amll 路径由 findBestMatch 产出；平台 LRC 缺省
   * 视为 'high'（向后兼容）。低置信命中在 shouldPersistOnlineLyrics 阶段被受限。
   */
  confidence?: MatchConfidence
}

export type OnlineLyricsMatchFail = {
  ok: false
  reason: 'no-match' | 'network' | 'parse'
}

export type OnlineLyricsMatchResult = OnlineLyricsMatchOk | OnlineLyricsMatchFail
