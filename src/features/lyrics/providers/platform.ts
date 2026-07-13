/**
 * 平台歌词默认链：kw → tx → wy → kg → mg
 * 注册到 matchOnlineLyrics fallback（在 LRCLIB 之前由调用方组合）。
 */
import type { LyricsProvider } from './types'
import { kgLyricsProvider } from './kg'
import { kwLyricsProvider } from './kw'
import { mgLyricsProvider } from './mg'
import { txLyricsProvider } from './tx'
import { wyLyricsProvider } from './wy'

export const platformLyricsProviders: LyricsProvider[] = [
  kwLyricsProvider,
  txLyricsProvider,
  wyLyricsProvider,
  kgLyricsProvider,
  mgLyricsProvider,
]

export {
  kgLyricsProvider,
  kwLyricsProvider,
  mgLyricsProvider,
  txLyricsProvider,
  wyLyricsProvider,
}
