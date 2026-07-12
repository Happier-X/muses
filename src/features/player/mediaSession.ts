import { MediaSession } from '@capgo/capacitor-media-session'
import type { MediaSessionAction } from '@capgo/capacitor-media-session'
import { AudioPlayerBridge } from './native'
import type { PlaybackStatus } from './types'

type ActionHandler = () => Promise<void> | void

type SeekHandler = (seconds: number) => Promise<boolean | void> | boolean | void

const ACTIVE_ACTIONS: MediaSessionAction[] = ['play', 'pause', 'previoustrack', 'nexttrack', 'stop']

/**
 * 1×1 中性 JPEG 的 data URL。
 * capgo media-session Android 端在 artwork=[] 时不会清掉旧 Bitmap，
 * 必须用显式 data: 图覆盖，才能避免切到无封面歌曲时残留上一首封面。
 */
export const EMPTY_ARTWORK_DATA_URL =
  'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAn/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAGcP//Z'

/** 丢弃过期封面回调，避免快速切歌串封面。 */
let metadataToken = 0

const emptyArtwork = () => [{ src: EMPTY_ARTWORK_DATA_URL, type: 'image/jpeg' }]

const toMediaImage = (dataUrl: string | null | undefined) => {
  if (!dataUrl) {
    return emptyArtwork()
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

  // 文字先上；同时用占位 data: 强制覆盖旧 Bitmap（空数组清不掉）。
  await MediaSession.setMetadata({
    title: params.title,
    artist: params.artist,
    album: params.album,
    artwork: emptyArtwork(),
  })

  if (!params.coverUri) {
    return
  }

  const artworkDataUrl = await prepareArtworkDataUrl(params.coverUri)
  if (token !== metadataToken) {
    return
  }

  // prepare 失败时保留首帧的占位清空，避免残留上一首封面。
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
  await MediaSession.setMetadata({ title: '', artwork: emptyArtwork() })
}
