import type { OnlineLyricsQuery } from './types'
import { normalizeText } from '../normalize'

export const buildKeyword = (query: OnlineLyricsQuery): string =>
  [query.title, query.artist, query.album].filter((part) => part?.trim()).join(' ').trim()

export const scoreSearchHit = (
  hit: { title?: string; artist?: string; album?: string },
  query: OnlineLyricsQuery,
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
  return score
}

export const pickBest = <T extends { title?: string; artist?: string; album?: string }>(
  items: T[],
  query: OnlineLyricsQuery,
): T | null => {
  if (items.length === 0) {
    return null
  }
  return [...items].sort((a, b) => scoreSearchHit(b, query) - scoreSearchHit(a, query))[0] ?? null
}

/** lrclist → 标准 LRC */
export const linesToLrc = (lines: Array<{ time?: string | number; text?: string }>): string => {
  const out: string[] = []
  for (const line of lines) {
    const text = line.text?.trim()
    if (!text) {
      continue
    }
    const t = line.time
    if (t === undefined || t === null || t === '') {
      continue
    }
    const seconds = typeof t === 'number' ? t : Number.parseFloat(String(t))
    if (!Number.isFinite(seconds)) {
      continue
    }
    const totalMs = Math.max(0, Math.round(seconds * 1000))
    const m = Math.floor(totalMs / 60000)
    const s = Math.floor((totalMs % 60000) / 1000)
    const ms = totalMs % 1000
    const tag =
      `[${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}.${String(ms).padStart(3, '0')}]`
    out.push(`${tag}${text}`)
  }
  return out.join('\n')
}
