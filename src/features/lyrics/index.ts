export { matchAmllTtmlLyrics, ensureIndex, resetAmllTtmlDbCache, parseIndexLine, parseIndexJsonl } from './amllTtmlDb'
export { normalizeText, splitArtistTokens } from './normalize'
export { findBestMatch, scoreEntry, scoreTitle, scoreArtists, scoreAlbum, MIN_ACCEPT_SCORE, SCORE_WEIGHTS } from './score'
export type { AmllIndexEntry, AmllMatchQuery, AmllMatchResult, AmllMatchFailReason } from './types'
