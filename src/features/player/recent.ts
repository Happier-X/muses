/**
 * 最近播放记录（首页展示）：播放一首歌即记录，同曲去重置顶，上限 50。
 * 仅存展示所需元数据；点击播放时按 songId 从歌曲库解析完整 SongItem。
 */
import type { SongItem } from '@/features/library/types'

const RECENT_STORAGE_KEY = 'muses:recent'
const RECENT_LIMIT = 50

export interface RecentPlayEntry {
  songId: string
  title: string
  subtitle: string
  coverUri?: string
  playedAt: number
}

const isRecentPlayEntry = (value: unknown): value is RecentPlayEntry => {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const record = value as Record<string, unknown>
  return (
    typeof record.songId === 'string'
    && typeof record.title === 'string'
    && typeof record.subtitle === 'string'
    && typeof record.playedAt === 'number'
  )
}

export const loadRecentPlays = (): RecentPlayEntry[] => {
  const raw = localStorage.getItem(RECENT_STORAGE_KEY)
  if (!raw) {
    return []
  }
  try {
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.filter(isRecentPlayEntry).slice(0, RECENT_LIMIT)
  } catch {
    return []
  }
}

/** 播放时登记：同曲移到最前，其余保持，超限裁尾。 */
export const recordRecentPlay = (song: SongItem): void => {
  const plays = loadRecentPlays().filter((entry) => entry.songId !== song.id)
  plays.unshift({
    songId: song.id,
    title: song.title,
    subtitle: [song.artist, song.album].filter(Boolean).join(' - '),
    coverUri: song.coverUri,
    playedAt: Date.now(),
  })
  localStorage.setItem(RECENT_STORAGE_KEY, JSON.stringify(plays.slice(0, RECENT_LIMIT)))
}
