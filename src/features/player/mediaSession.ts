import { MediaSession } from '@capgo/capacitor-media-session'
import type { MediaSessionAction } from '@capgo/capacitor-media-session'
import { AudioPlayerBridge } from './native'
import type { PlaybackStatus } from './types'

type ActionHandler = () => Promise<void> | void

type SeekHandler = (seconds: number) => Promise<void> | void

const ACTIVE_ACTIONS: MediaSessionAction[] = ['play', 'pause', 'previoustrack', 'nexttrack', 'stop']

/** 丢弃过期封面回调，避免快速切歌串封面。 */
let metadataToken = 0

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

/**
 * loading/finished 必须保持 active，否则 capgo media-session 会 stop 前台服务再重建：
 * - loading：切歌预加载
 * - finished：队列自动下一首前的短暂窗口
 * idle/stopped/error 才映射 none，由 clear/stop 路径清理。
 */
const toMediaPlaybackState = (status: PlaybackStatus): 'playing' | 'paused' | 'none' => {
  if (status === 'playing' || status === 'loading' || status === 'finished') {
    return 'playing'
  }
  if (status === 'paused') {
    return 'paused'
  }
  return 'none'
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
  const token = ++metadataToken

  // 文字先上，不等待封面转换，缩短首帧通知出现时间。
  await MediaSession.setMetadata({
    title: params.title,
    artist: params.artist,
    album: params.album,
    artwork: [],
  })

  if (!params.coverUri) {
    return
  }

  const artworkDataUrl = await prepareArtworkDataUrl(params.coverUri)
  if (token !== metadataToken) {
    return
  }

  if (!artworkDataUrl) {
    return
  }

  await MediaSession.setMetadata({
    title: params.title,
    artist: params.artist,
    album: params.album,
    artwork: toMediaImage(artworkDataUrl),
  })
}

export const updateMediaSessionPlayback = async (status: PlaybackStatus) => {
  await MediaSession.setPlaybackState({
    playbackState: toMediaPlaybackState(status),
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
  metadataToken += 1
  await MediaSession.setPlaybackState({ playbackState: 'none' })
  await MediaSession.setMetadata({ title: '', artwork: [] })
}
