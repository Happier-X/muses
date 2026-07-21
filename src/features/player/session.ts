/** 冷启动播放会话：当前曲 + 进度（#49）。队列本身仍用 muses:queue。 */

const SESSION_STORAGE_KEY = 'muses:playback-session'

export interface PlaybackSession {
  currentSongId: string
  /** 秒，>= 0 */
  position: number
  updatedAt?: number
}

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

export const loadPlaybackSession = (): PlaybackSession | null => {
  const raw = localStorage.getItem(SESSION_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed: unknown = JSON.parse(raw)
    if (!isRecord(parsed) || typeof parsed.currentSongId !== 'string' || parsed.currentSongId.length === 0) {
      return null
    }
    const position = typeof parsed.position === 'number' && Number.isFinite(parsed.position) && parsed.position >= 0
      ? parsed.position
      : 0
    const updatedAt = typeof parsed.updatedAt === 'number' && Number.isFinite(parsed.updatedAt)
      ? parsed.updatedAt
      : undefined
    return {
      currentSongId: parsed.currentSongId,
      position,
      updatedAt,
    }
  } catch {
    return null
  }
}

export const savePlaybackSession = (session: PlaybackSession): void => {
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({
    currentSongId: session.currentSongId,
    position: Math.max(0, Number.isFinite(session.position) ? session.position : 0),
    updatedAt: session.updatedAt ?? Date.now(),
  }))
}

export const clearPlaybackSession = (): void => {
  localStorage.removeItem(SESSION_STORAGE_KEY)
}
