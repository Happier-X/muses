import { MediaSession } from '@capgo/capacitor-media-session'
import type { MediaSessionAction } from '@capgo/capacitor-media-session'
import { AudioPlayerBridge } from './native'
import type { PlaybackStatus } from './types'

type ActionHandler = () => Promise<void> | void

type SeekHandler = (seconds: number) => Promise<void> | void

const ACTIVE_ACTIONS: MediaSessionAction[] = ['play', 'pause', 'previoustrack', 'nexttrack', 'stop']

const toMediaImage = (dataUrl: string | null | undefined) => {
  if (!dataUrl) {
    return undefined
  }
  const mimeMatch = dataUrl.match(/^data:([^;]+);base64,/)
  const mime = mimeMatch ? mimeMatch[1] : 'image/jpeg'
  return [{ src: dataUrl, type: mime }]
}

const prepareArtworkDataUrl = async (coverUri?: string | null): Promise<string | null> => {
  if (!coverUri || !AudioPlayerBridge.prepareArtworkDataUrl) {
    return null
  }
  try {
    const result = await AudioPlayerBridge.prepareArtworkDataUrl({ uri: coverUri })
    return result.dataUrl || null
  } catch {
    return null
  }
}

export const setupMediaSessionActions = async (handlers: {
  play: ActionHandler
  pause: ActionHandler
  stop: ActionHandler
  previoustrack: ActionHandler
  nexttrack: ActionHandler
  seekto?: SeekHandler
}) => {
  await Promise.all(ACTIVE_ACTIONS.map((action) => MediaSession.setActionHandler({ action }, null).catch(() => undefined)))

  await MediaSession.setActionHandler({ action: 'play' }, async () => { await handlers.play() })
  await MediaSession.setActionHandler({ action: 'pause' }, async () => { await handlers.pause() })
  await MediaSession.setActionHandler({ action: 'stop' }, async () => { await handlers.stop() })
  await MediaSession.setActionHandler({ action: 'previoustrack' }, async () => { await handlers.previoustrack() })
  await MediaSession.setActionHandler({ action: 'nexttrack' }, async () => { await handlers.nexttrack() })
  if (handlers.seekto) {
    await MediaSession.setActionHandler({ action: 'seekto' }, async (details) => {
      await handlers.seekto?.(typeof details.seekTime === 'number' ? details.seekTime : 0)
    })
  }
}

export const updateMediaSessionMetadata = async (params: {
  title: string
  artist?: string
  album?: string
  coverUri?: string | null
}) => {
  const artworkDataUrl = await prepareArtworkDataUrl(params.coverUri)
  await MediaSession.setMetadata({
    title: params.title,
    artist: params.artist,
    album: params.album,
    artwork: toMediaImage(artworkDataUrl),
  })
}

export const updateMediaSessionPlayback = async (status: PlaybackStatus) => {
  await MediaSession.setPlaybackState({
    playbackState: status === 'playing' ? 'playing' : status === 'paused' ? 'paused' : 'none',
  })
}

export const updateMediaSessionPosition = async (positionSeconds: number, durationSeconds: number) => {
  await MediaSession.setPositionState({
    duration: durationSeconds,
    position: positionSeconds,
    playbackRate: 1,
  })
}

export const clearMediaSession = async () => {
  await MediaSession.setPlaybackState({ playbackState: 'none' })
  await MediaSession.setMetadata({ title: '', artwork: [] })
}
