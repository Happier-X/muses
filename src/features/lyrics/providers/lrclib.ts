/**
 * LRCLIB 公开 LRC：仅 syncedLyrics；挂在平台五源之后。
 * 文档：https://lrclib.net/docs
 */
import { httpGetJson } from '@/features/cover/http'
import type { LyricsProvider, OnlineLyricsQuery } from './types'
import { pickBest } from './util'

const LRCLIB_UA = 'Muses/0.1.2 (local music player; https://github.com/Happier-X/muses)'

const LRCLIB_HEADERS = {
  'User-Agent': LRCLIB_UA,
  Accept: 'application/json',
}

type LrclibTrack = {
  id?: number
  trackName?: string
  name?: string
  artistName?: string
  albumName?: string
  duration?: number
  instrumental?: boolean
  syncedLyrics?: string | null
  plainLyrics?: string | null
}

const hasTimedLrc = (text: string): boolean => /\[\d{1,2}:\d{2}/.test(text)

const extractSynced = (track: LrclibTrack | null | undefined): string | null => {
  if (!track || track.instrumental) {
    return null
  }
  const synced = track.syncedLyrics?.trim()
  if (synced && hasTimedLrc(synced)) {
    return synced
  }
  return null
}

const getExact = async (query: OnlineLyricsQuery): Promise<string | null> => {
  const trackName = query.title.trim()
  if (!trackName) {
    return null
  }
  const params = new URLSearchParams()
  params.set('track_name', trackName)
  if (query.artist?.trim()) {
    params.set('artist_name', query.artist.trim())
  }
  if (query.album?.trim()) {
    params.set('album_name', query.album.trim())
  }
  if (typeof query.duration === 'number' && query.duration > 0) {
    params.set('duration', String(Math.round(query.duration)))
  }

  const url = `https://lrclib.net/api/get?${params.toString()}`
  try {
    // 404 时 httpGetJson 抛错 → 视为无精确命中
    const body = await httpGetJson<LrclibTrack>(url, LRCLIB_HEADERS)
    return extractSynced(body)
  } catch {
    return null
  }
}

const searchFallback = async (query: OnlineLyricsQuery): Promise<string | null> => {
  const trackName = query.title.trim()
  if (!trackName) {
    return null
  }
  const params = new URLSearchParams()
  params.set('track_name', trackName)
  if (query.artist?.trim()) {
    params.set('artist_name', query.artist.trim())
  }
  if (query.album?.trim()) {
    params.set('album_name', query.album.trim())
  }

  const url = `https://lrclib.net/api/search?${params.toString()}`
  let list: LrclibTrack[]
  try {
    list = await httpGetJson<LrclibTrack[]>(url, LRCLIB_HEADERS)
  } catch {
    return null
  }
  if (!Array.isArray(list) || list.length === 0) {
    return null
  }

  const candidates = list
    .filter((item) => extractSynced(item))
    .map((item) => ({
      title: item.trackName || item.name,
      artist: item.artistName,
      album: item.albumName,
      track: item,
    }))

  if (candidates.length === 0) {
    return null
  }

  // 有 duration 时优先时长接近的
  if (typeof query.duration === 'number' && query.duration > 0) {
    const target = query.duration
    const withDuration = candidates
      .map((c) => ({
        ...c,
        delta:
          typeof c.track.duration === 'number'
            ? Math.abs(c.track.duration - target)
            : Number.POSITIVE_INFINITY,
      }))
      .sort((a, b) => a.delta - b.delta)
    const tight = withDuration.find((c) => c.delta <= 3)
    if (tight) {
      return extractSynced(tight.track)
    }
  }

  const best = pickBest(candidates, query)
  return best ? extractSynced(best.track) : null
}

export const searchLrclibLyrics = async (
  query: OnlineLyricsQuery,
): Promise<{ text: string; format: 'lrc' } | null> => {
  // 精确 get（含 duration 时更准）
  const exact = await getExact(query)
  if (exact) {
    return { text: exact, format: 'lrc' }
  }
  const searched = await searchFallback(query)
  if (searched) {
    return { text: searched, format: 'lrc' }
  }
  return null
}

export const lrclibLyricsProvider: LyricsProvider = {
  id: 'lrclib',
  searchLyrics: searchLrclibLyrics,
}

/** 供测试断言 UA */
export const LRCLIB_USER_AGENT = LRCLIB_UA
