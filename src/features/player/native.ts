import { NativeAudio } from '@capgo/capacitor-native-audio'
import { registerPlugin } from '@capacitor/core'
import type { PluginListenerHandle } from '@capacitor/core'
import type { AudioPlayerNativeState, PlaybackStatus, PlayOptions, SeekOptions } from './types'

interface AudioPlayerPermissionBridge {
  ensureNotificationPermission(): Promise<{ granted: boolean }>
  prepareLocalAudioFile?(options: { uri: string; songId: string }): Promise<{ uri: string }>
  prepareArtworkDataUrl?(options: { uri: string }): Promise<{ dataUrl: string | null }>
}

interface NativePlaybackStateEvent {
  assetId?: string
  state?: 'playing' | 'paused' | 'stopped'
  reason?: string
  isPlaying?: boolean
  currentTime?: number
  duration?: number
}

interface NativeCurrentTimeEvent {
  assetId?: string
  currentTime?: number
}

interface NativeCompleteEvent {
  assetId?: string
}

export interface AudioPlayerNativePlugin {
  play(options: PlayOptions): Promise<void>
  pause(): Promise<void>
  resume(): Promise<void>
  stop(): Promise<void>
  seek(options: SeekOptions): Promise<void>
  getState(): Promise<AudioPlayerNativeState>
  ensureNotificationPermission(): Promise<{ granted: boolean }>
  addListener(
    eventName: 'stateChange',
    listenerFunc: (state: AudioPlayerNativeState) => void,
  ): Promise<PluginListenerHandle>
}

export const AudioPlayerBridge = registerPlugin<AudioPlayerPermissionBridge>('AudioPlayer')

let configured = false
let currentAssetId: string | null = null
let currentSongId: string | null = null
let currentStatus: PlaybackStatus = 'idle'
let currentPosition = 0
let currentDuration = 0
let nativeListenersReady = false
let nativeListenerHandles: PluginListenerHandle[] = []
const stateListeners = new Set<(state: AudioPlayerNativeState) => void>()

const logNativeAudio = (message: string, data?: unknown): void => {
  // 保留诊断日志，Android WebView 会输出到 logcat，方便排查 Capgo 插件链路。
  console.info('[MusesNativeAudio]', message, data ?? '')
}

const configureNativeAudio = async (): Promise<void> => {
  if (configured) {
    return
  }

  logNativeAudio('configure:start')
  await NativeAudio.configure({
    focus: true,
    background: true,
    backgroundPlayback: true,
    showNotification: false,
  })
  configured = true
  logNativeAudio('configure:done')
}

const isRemoteUrl = (value: string): boolean => /^https?:\/\//i.test(value)

const encodeBasicAuth = (username: string, password: string): string => {
  const bytes = new TextEncoder().encode(`${username}:${password}`)
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  return btoa(binary)
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
  })
}

const mapPlaybackStatus = (event: NativePlaybackStateEvent): PlaybackStatus => {
  if (event.reason === 'complete') {
    return 'finished'
  }
  if (event.state === 'playing' || event.isPlaying === true) {
    return 'playing'
  }
  if (event.state === 'paused') {
    return 'paused'
  }
  if (event.state === 'stopped') {
    return 'stopped'
  }
  return currentStatus
}

const ensureNativeListeners = async (): Promise<void> => {
  if (nativeListenersReady) {
    return
  }
  nativeListenersReady = true

  nativeListenerHandles = await Promise.all([
    NativeAudio.addListener('playbackState', (event: NativePlaybackStateEvent) => {
      if (event.assetId && event.assetId !== currentAssetId) {
        return
      }
      currentPosition = normalizePlaybackTime(event.currentTime) || currentPosition
      currentDuration = normalizePlaybackTime(event.duration) || currentDuration
      emitCurrentState(mapPlaybackStatus(event))
    }),
    NativeAudio.addListener('currentTime', (event: NativeCurrentTimeEvent) => {
      if (event.assetId && event.assetId !== currentAssetId) {
        return
      }
      currentPosition = normalizePlaybackTime(event.currentTime)
      emitCurrentState(currentStatus)
    }),
    NativeAudio.addListener('complete', (event: NativeCompleteEvent) => {
      if (event.assetId && event.assetId !== currentAssetId) {
        return
      }
      emitCurrentState('finished')
    }),
  ])
}

const normalizePlaybackTime = (value: unknown): number => {
  return typeof value === 'number' && Number.isFinite(value) && value > 0 ? value : 0
}

const unloadCurrentAsset = async (): Promise<void> => {
  const assetId = currentAssetId
  if (!assetId) {
    return
  }

  await NativeAudio.stop({ assetId }).catch(() => undefined)
  await NativeAudio.unload({ assetId }).catch(() => undefined)
}

const toAssetId = (songId: string): string => `song-${songId.replace(/[^a-zA-Z0-9_-]/g, '-')}`

const resolveAssetPath = async (options: PlayOptions): Promise<{ assetPath: string; headers?: Record<string, string> }> => {
  if (options.sourceType === 'webdav') {
    return {
      assetPath: options.url,
      headers: {
        Authorization: `Basic ${encodeBasicAuth(options.username, options.password)}`,
      },
    }
  }

  if (options.uri.startsWith('content://') && AudioPlayerBridge.prepareLocalAudioFile) {
    const result = await AudioPlayerBridge.prepareLocalAudioFile({ uri: options.uri, songId: options.songId })
    return { assetPath: result.uri }
  }

  return { assetPath: options.uri }
}

export const AudioPlayerNative: AudioPlayerNativePlugin = {
  async play(options) {
    await configureNativeAudio()
    await ensureNativeListeners()
    await unloadCurrentAsset()

    const assetId = toAssetId(options.songId)
    const { assetPath, headers } = await resolveAssetPath(options)

    currentAssetId = assetId
    currentSongId = options.songId
    currentStatus = 'loading'
    currentPosition = 0
    currentDuration = 0
    emitCurrentState('loading')

    const isUrl = isRemoteUrl(assetPath) || assetPath.startsWith('file://')
    logNativeAudio('play:resolved', {
      assetId,
      sourceType: options.sourceType,
      assetPathPrefix: assetPath.slice(0, 80),
      isUrl,
      hasHeaders: Boolean(headers),
    })

    try {
      await NativeAudio.preload({
        assetId,
        assetPath,
        isUrl,
        audioChannelNum: 1,
        headers,
      })
      logNativeAudio('preload:done', { assetId })
      currentDuration = normalizePlaybackTime((await NativeAudio.getDuration({ assetId }).catch(() => ({ duration: 0 }))).duration)
      logNativeAudio('duration:done', { assetId, currentDuration })
      await NativeAudio.play({ assetId })
      logNativeAudio('play:done', { assetId })
      emitCurrentState('playing')
    } catch (error) {
      logNativeAudio('play:error', error instanceof Error ? { message: error.message, stack: error.stack } : error)
      throw error
    }
  },

  async pause() {
    if (!currentAssetId) {
      return
    }
    await NativeAudio.pause({ assetId: currentAssetId })
    emitCurrentState('paused')
  },

  async resume() {
    if (!currentAssetId) {
      return
    }
    await NativeAudio.resume({ assetId: currentAssetId })
    emitCurrentState('playing')
  },

  async stop() {
    await unloadCurrentAsset()
    currentAssetId = null
    currentSongId = null
    currentPosition = 0
    currentDuration = 0
    emitCurrentState('stopped')
  },

  async seek(options) {
    if (!currentAssetId) {
      return
    }
    await NativeAudio.setCurrentTime({ assetId: currentAssetId, time: options.position })
    currentPosition = options.position
    emitCurrentState(currentStatus)
  },

  async getState() {
    if (!currentAssetId) {
      return { status: currentStatus }
    }

    const [position, duration, playing] = await Promise.all([
      NativeAudio.getCurrentTime({ assetId: currentAssetId }).catch(() => ({ currentTime: currentPosition })),
      NativeAudio.getDuration({ assetId: currentAssetId }).catch(() => ({ duration: currentDuration })),
      NativeAudio.isPlaying({ assetId: currentAssetId }).catch(() => ({ isPlaying: currentStatus === 'playing' })),
    ])
    currentPosition = normalizePlaybackTime(position.currentTime)
    currentDuration = normalizePlaybackTime(duration.duration)
    currentStatus = playing.isPlaying ? 'playing' : currentStatus === 'playing' ? 'paused' : currentStatus
    return {
      status: currentStatus,
      currentSongId: currentSongId || undefined,
      position: currentPosition,
      duration: currentDuration,
    }
  },

  async ensureNotificationPermission() {
    return AudioPlayerBridge.ensureNotificationPermission()
  },

  async addListener(eventName, listenerFunc) {
    if (eventName !== 'stateChange') {
      throw new Error('不支持的播放器事件。')
    }
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
