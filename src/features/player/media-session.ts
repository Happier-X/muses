import { MediaSession } from '@capgo/capacitor-media-session'
import type { MediaSessionAction, MediaSessionPlaybackState, MetadataOptions } from '@capgo/capacitor-media-session'
import type { PlaybackStatus, PlayerState } from './types'

interface ActionDetails {
  action: MediaSessionAction
  seekTime?: number | null
}

interface MediaSessionControls {
  play: () => Promise<void>
  pause: () => Promise<void>
  stop: () => Promise<void>
  seekTo: (position: number) => Promise<void>
}

let controlsInitialized = false

const toMediaSessionPlaybackState = (status: PlaybackStatus): MediaSessionPlaybackState => {
  if (status === 'playing') {
    return 'playing'
  }

  if (status === 'paused' || status === 'loading') {
    return 'paused'
  }

  return 'none'
}

const isSafeArtworkUri = (value: unknown): value is string => {
  if (typeof value !== 'string') {
    return false
  }

  const uri = value.trim()
  if (!uri) {
    return false
  }

  const lowerUri = uri.toLowerCase()
  if (lowerUri.startsWith('data:') || lowerUri.startsWith('blob:') || lowerUri.includes(';base64,')) {
    return false
  }

  if (lowerUri.includes('authorization=') || lowerUri.includes('basic ')) {
    return false
  }

  try {
    const parsed = new URL(uri)
    if (parsed.username || parsed.password) {
      return false
    }
  } catch {
    return true
  }

  return true
}

const buildMetadata = (state: PlayerState): MetadataOptions | null => {
  const song = state.currentSong
  if (!song) {
    return null
  }

  const artworkUri = isSafeArtworkUri(state.coverUri || song.coverUri) ? state.coverUri || song.coverUri : null
  return {
    title: song.title,
    artist: song.artist,
    album: song.album,
    ...(artworkUri ? { artwork: [{ src: artworkUri }] } : {}),
  }
}

const runControl = (operation: () => Promise<void>): void => {
  void operation().catch(() => undefined)
}

export const initializeMediaSessionControls = async (controls: MediaSessionControls): Promise<void> => {
  if (controlsInitialized) {
    return
  }

  const handlers: Array<[MediaSessionAction, (details: ActionDetails) => void]> = [
    ['play', () => runControl(controls.play)],
    ['pause', () => runControl(controls.pause)],
    ['stop', () => runControl(controls.stop)],
    ['seekto', (details) => {
      const seekTime = typeof details.seekTime === 'number' && Number.isFinite(details.seekTime) ? details.seekTime : null
      if (seekTime !== null) {
        runControl(() => controls.seekTo(seekTime))
      }
    }],
  ]

  await Promise.all(handlers.map(([action, handler]) => MediaSession.setActionHandler({ action }, handler)))
  controlsInitialized = true
}

export const syncMediaSessionState = async (state: PlayerState): Promise<void> => {
  const metadata = buildMetadata(state)
  if (!metadata) {
    await clearMediaSessionState()
    return
  }

  await MediaSession.setMetadata(metadata)
  await MediaSession.setPlaybackState({ playbackState: toMediaSessionPlaybackState(state.status) })

  if (state.duration > 0) {
    await MediaSession.setPositionState({
      duration: state.duration,
      position: Math.min(Math.max(state.position, 0), state.duration),
      playbackRate: state.status === 'playing' ? 1 : 0,
    })
  }
}

export const clearMediaSessionState = async (): Promise<void> => {
  await MediaSession.setPlaybackState({ playbackState: 'none' })
  await MediaSession.setPositionState({ duration: 0, position: 0, playbackRate: 0 })
}
