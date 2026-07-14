export { matchAmllTtmlLyrics, ensureIndex, resetAmllTtmlDbCache, parseIndexLine, parseIndexJsonl } from './amllTtmlDb'
export {
  matchOnlineLyrics,
  getOnlineLyricsFallbackProviders,
  setDefaultOnlineLyricsFallbackProviders,
  setOnlineLyricsFallbackProvidersForTest,
} from './match'
export { normalizeText, splitArtistTokens } from './normalize'
export {
  attachTimedLyricsTranslation,
  mergeDuplicateTimestampTranslations,
  parseTimedLrcMap,
  prepareLyricLinesForDisplay,
} from './mergeTranslation'
export { applyLyricTranslationVisibility } from './display'
export { findBestMatch, scoreEntry, scoreTitle, scoreArtists, scoreAlbum, MIN_ACCEPT_SCORE, SCORE_WEIGHTS } from './score'
export type { AmllIndexEntry, AmllMatchQuery, AmllMatchResult, AmllMatchFailReason } from './types'
export type {
  LyricsProvider,
  OnlineLyricsFormat,
  OnlineLyricsMatchResult,
  OnlineLyricsQuery,
  OnlineLyricsSource,
} from './providers/types'
export { platformLyricsProviders } from './providers/platform'
export { lrclibLyricsProvider, searchLrclibLyrics } from './providers/lrclib'
