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
  ) => Promise<{ text: string; format: OnlineLyricsFormat } | null>
}

export type OnlineLyricsMatchOk = {
  ok: true
  text: string
  format: OnlineLyricsFormat
  source: OnlineLyricsSource
}

export type OnlineLyricsMatchFail = {
  ok: false
  reason: 'no-match' | 'network' | 'parse'
}

export type OnlineLyricsMatchResult = OnlineLyricsMatchOk | OnlineLyricsMatchFail
