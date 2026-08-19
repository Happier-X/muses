import { registerPlugin } from '@capacitor/core'
import type { PluginListenerHandle } from '@capacitor/core'
import { PLAYBACK_VOLUME_MAX, PLAYBACK_VOLUME_MIN } from './loudness'
import type { AudioPlayerNativeState, PlaybackStatus, PlayOptions, SeekOptions } from './types'

// --------------- Bridge 接口定义 ---------------

interface LoadOptions {
  uri: string
  songId: string
  volume?: number
  audioHeaders?: Record<string, string>
}

interface PlayerState {
  status: string
  currentSongId?: string
  position: number
  duration: number
  bufferedPosition?: number
  errorMessage?: string
}

interface AudioPlayerBridgePlugin {
  load(options: LoadOptions): Promise<void>
  play(): Promise<void>
  pause(): Promise<void>
  stop(): Promise<void>
  seek(options: { position: number }): Promise<void>
  setVolume(volume: number): Promise<void>
  getState(): Promise<PlayerState>
  setAudioFocus(options: { enabled: boolean }): Promise<void>
  ensureNotificationPermission(): Promise<{ granted: boolean }>
  prepareLocalAudioFile(options: { uri: string; songId: string }): Promise<{ uri: string }>
  getCachedWebDavAudioFile(options: { url: string }): Promise<{ uri: string | null }>
  prefetchWebDavAudioFile(options: {
    url: string
    username: string
    password: string
    songId: string
  }): Promise<{ cached: boolean; started: boolean }>
  prepareArtworkDataUrl(options: { uri: string }): Promise<{ dataUrl: string | null }>
  cacheRemoteCover(options: { url: string; cacheKey: string }): Promise<{ uri: string | null }>
  addListener(
    eventName: 'stateChange',
    listenerFunc: (state: PlayerState) => void,
  ): Promise<PluginListenerHandle>
  addListener(
    eventName: 'playbackComplete',
    listenerFunc: () => void,
  ): Promise<PluginListenerHandle>
}

// --------------- 插件注册 ---------------

export const AudioPlayerBridge = registerPlugin<AudioPlayerBridgePlugin>('AudioPlayer')

// --------------- 状态管理 ---------------

let currentSongId: string | null = null
let currentStatus: PlaybackStatus = 'idle'
let currentPosition = 0
let currentDuration = 0
let currentBufferedPosition: number | null = null
let nativeListenersReady = false
let nativeListenerHandles: PluginListenerHandle[] = []
const stateListeners = new Set<(state: AudioPlayerNativeState) => void>()

// --------------- 辅助函数 ---------------

const normalizePlaybackTime = (value: unknown): number => {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : 0
}

const mapStatus = (status: string): PlaybackStatus => {
  switch (status) {
    case 'playing':
      return 'playing'
    case 'paused':
      return 'paused'
    case 'stopped':
      return 'stopped'
    case 'finished':
      return 'finished'
    case 'loading':
      return 'loading'
    case 'error':
      return 'error'
    case 'idle':
      return 'idle'
    default:
      return currentStatus
  }
}

const notifyState = (state: AudioPlayerNativeState): void => {
  stateListeners.forEach((listener) => listener(state))
}

const emitCurrentState = (status: PlaybackStatus = currentStatus): void => {
  currentStatus = status
  notifyState({
    status,
    currentSongId: currentSongId || undefined,
    position: currentPosition,
    duration: currentDuration,
    bufferedPosition: currentBufferedPosition ?? undefined,
  })
}

// --------------- 监听器设置 ---------------

const ensureNativeListeners = async (): Promise<void> => {
  if (nativeListenersReady) {
    return
  }
  nativeListenersReady = true

  try {
    const handle1 = await AudioPlayerBridge.addListener('stateChange', (state: PlayerState) => {
      // 更新状态
      if (state.currentSongId) {
        currentSongId = state.currentSongId
      }
      currentPosition = normalizePlaybackTime(state.position)
      currentDuration = normalizePlaybackTime(state.duration)
      
      if (state.bufferedPosition !== undefined) {
        currentBufferedPosition = normalizePlaybackTime(state.bufferedPosition)
      }

      const newStatus = mapStatus(state.status)
      
      // 错误状态处理
      if (state.status === 'error') {
        currentBufferedPosition = null
      }

      emitCurrentState(newStatus)
    })

    const handle2 = await AudioPlayerBridge.addListener('playbackComplete', () => {
      emitCurrentState('finished')
    })

    nativeListenerHandles = [handle1, handle2]
  } catch {
    // 监听失败静默
  }
}

// --------------- 公开 API ---------------

export const AudioPlayerNative = {
  async play(options: PlayOptions): Promise<void> {
    await ensureNativeListeners()

    const songId = options.songId
    currentSongId = songId
    currentPosition = 0
    currentDuration = 0
    currentBufferedPosition = null
    currentStatus = 'loading'
    emitCurrentState('loading')

    // 构建 headers
    const headers: Record<string, string> = {}
    if (options.sourceType === 'webdav' && options.username && options.password) {
      const bytes = new TextEncoder().encode(`${options.username}:${options.password}`)
      let binary = ''
      bytes.forEach((byte) => {
        binary += String.fromCharCode(byte)
      })
      headers['Authorization'] = `Basic ${btoa(binary)}`
    }

    // 获取 URI
    let uri: string
    if (options.sourceType === 'webdav') {
      // 检查是否有完整缓存
      const cached = await AudioPlayerBridge.getCachedWebDavAudioFile({ url: options.url! })
      if (cached?.uri) {
        uri = cached.uri
        currentBufferedPosition = currentDuration > 0 ? currentDuration : Number.POSITIVE_INFINITY
      } else {
        uri = options.url!
      }
    } else {
      uri = options.uri!
    }

    // 设置音量
    const volume = typeof options.volume === 'number' && Number.isFinite(options.volume)
      ? Math.min(PLAYBACK_VOLUME_MAX, Math.max(PLAYBACK_VOLUME_MIN, options.volume))
      : PLAYBACK_VOLUME_MAX

    // 加载并播放
    await AudioPlayerBridge.load({
      uri,
      songId,
      volume,
      audioHeaders: Object.keys(headers).length > 0 ? headers : undefined,
    })

    await AudioPlayerBridge.play()
    
    currentStatus = 'playing'
    emitCurrentState('playing')
  },

  async pause(): Promise<void> {
    await AudioPlayerBridge.pause()
    currentStatus = 'paused'
    emitCurrentState('paused')
  },

  async resume(): Promise<void> {
    await AudioPlayerBridge.play()
    currentStatus = 'playing'
    emitCurrentState('playing')
  },

  async stop(): Promise<void> {
    await AudioPlayerBridge.stop()
    currentSongId = null
    currentStatus = 'stopped'
    currentPosition = 0
    currentDuration = 0
    currentBufferedPosition = null
    emitCurrentState('stopped')
  },

  async seek(options: SeekOptions): Promise<void> {
    await AudioPlayerBridge.seek({ position: options.position })
    currentPosition = options.position
    emitCurrentState(currentStatus)
  },

  async setVolume(volume: number): Promise<void> {
    const safe = typeof volume === 'number' && Number.isFinite(volume)
      ? Math.min(PLAYBACK_VOLUME_MAX, Math.max(PLAYBACK_VOLUME_MIN, volume))
      : PLAYBACK_VOLUME_MAX
    await AudioPlayerBridge.setVolume(safe)
  },

  async getState(): Promise<AudioPlayerNativeState> {
    try {
      const state = await AudioPlayerBridge.getState()
      currentSongId = state.currentSongId || null
      currentPosition = normalizePlaybackTime(state.position)
      currentDuration = normalizePlaybackTime(state.duration)
      currentBufferedPosition = state.bufferedPosition !== undefined
        ? normalizePlaybackTime(state.bufferedPosition)
        : null
      currentStatus = mapStatus(state.status)
      
      return {
        status: currentStatus,
        currentSongId: state.currentSongId || undefined,
        position: currentPosition,
        duration: currentDuration,
        bufferedPosition: currentBufferedPosition ?? undefined,
      }
    } catch {
      return {
        status: currentStatus,
        currentSongId: currentSongId || undefined,
        position: currentPosition,
        duration: currentDuration,
        bufferedPosition: currentBufferedPosition ?? undefined,
      }
    }
  },

  async ensureNotificationPermission(): Promise<{ granted: boolean }> {
    return AudioPlayerBridge.ensureNotificationPermission()
  },

  async addListener(
    eventName: 'stateChange',
    listenerFunc: (state: AudioPlayerNativeState) => void,
  ): Promise<PluginListenerHandle> {
    stateListeners.add(listenerFunc)
    await ensureNativeListeners().catch(() => undefined)
    return {
      remove: async () => {
        stateListeners.delete(listenerFunc)
        if (stateListeners.size === 0) {
          await Promise.all(nativeListenerHandles.map((handle) => handle.remove()))
          nativeListenerHandles = []
          nativeListenersReady = false
        }
      },
    }
  },
}

// --------------- 辅助导出 ---------------

export const toAssetId = (songId: string): string => `song-${songId.replace(/[^a-zA-Z0-9_-]/g, '-')}`

export const prepareLocalAudioFile = async (uri: string, songId: string): Promise<string> => {
  const result = await AudioPlayerBridge.prepareLocalAudioFile({ uri, songId })
  return result.uri
}

export const getCachedWebDavAudioFile = async (url: string): Promise<string | null> => {
  const result = await AudioPlayerBridge.getCachedWebDavAudioFile({ url })
  return result.uri
}

export const prefetchWebDavAudioFile = async (options: {
  url: string
  username: string
  password: string
  songId: string
}): Promise<{ cached: boolean; started: boolean }> => {
  return AudioPlayerBridge.prefetchWebDavAudioFile(options)
}

export const cacheRemoteCover = async (options: {
  url: string
  cacheKey: string
}): Promise<string | null> => {
  try {
    const result = await AudioPlayerBridge.cacheRemoteCover(options)
    const uri = result?.uri?.trim()
    if (!uri) return null
    const normalized = uri.toLowerCase()
    if (
      normalized.startsWith('data:')
      || normalized.startsWith('blob:')
      || normalized.includes(';base64,')
      || normalized.startsWith('http://')
      || normalized.startsWith('https://')
    ) {
      return null
    }
    return uri
  } catch {
    return null
  }
}

export const prepareArtworkDataUrl = async (uri: string): Promise<string | null> => {
  try {
    const result = await AudioPlayerBridge.prepareArtworkDataUrl({ uri })
    return result.dataUrl
  } catch {
    return null
  }
}

// --------------- 兼容性导出（移除旧接口） ---------------

/** @deprecated 使用 AudioPlayerNative.setVolume 代替 */
export const setVolume = AudioPlayerNative.setVolume

/** @deprecated 使用 AudioPlayerNative.getState 代替 */
export const getState = AudioPlayerNative.getState

// --------------- 移除的接口（预案机制） ---------------

/** @deprecated ExoPlayer 不再需要预案机制 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const setNativeAutoNextPlan = async (_options?: {
  songId: string
  assetId: string
  assetPath: string
  isUrl: boolean
  username?: string
  password?: string
  volume: number
  currentAssetId?: string
  title?: string
  artist?: string
}): Promise<void> => {
  // ExoPlayer + MediaSession 天然支持后台播放，不再需要预案
}

/** @deprecated ExoPlayer 不再需要预案机制 */
export const clearNativeAutoNextPlan = async (): Promise<void> => {
  // ExoPlayer + MediaSession 天然支持后台播放，不再需要预案
}

/** @deprecated ExoPlayer 不再需要预案事件监听 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const addAutoNextEventListeners = async (_handlers?: {
  onStarted?: (songId: string | undefined) => void
  onFailed?: (songId: string | undefined, reason: string | undefined) => void
}): Promise<PluginListenerHandle[]> => {
  // ExoPlayer + MediaSession 天然支持后台播放，不再需要预案事件
  return []
}

/** @deprecated ExoPlayer 不再需要同步 asset 状态 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const syncCurrentAsset = (_songId?: string, _sourceType?: string): void => {
  // ExoPlayer 直接管理状态，不再需要手动同步
}
