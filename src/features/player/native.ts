import { NativeAudio } from '@capgo/capacitor-native-audio'
import { registerPlugin } from '@capacitor/core'
import type { PluginListenerHandle } from '@capacitor/core'
import type { AudioPlayerNativeState, PlaybackStatus, PlayOptions, SeekOptions } from './types'

interface BufferProgressEvent {
  songId?: string
  bufferedPosition?: number
  duration?: number
  fullyBuffered?: boolean
  bufferedRatio?: number
}

interface AudioPlayerPermissionBridge {
  ensureNotificationPermission(): Promise<{ granted: boolean }>
  prepareLocalAudioFile?(options: { uri: string; songId: string }): Promise<{ uri: string }>
  prepareWebDavAudioFile?(options: {
    url: string
    username: string
    password: string
    songId: string
    duration?: number
  }): Promise<{ uri: string; ready?: boolean }>
  cancelBufferSession?(options?: { songId?: string }): Promise<void>
  prepareArtworkDataUrl?(options: { uri: string }): Promise<{ dataUrl: string | null }>
  addListener?(
    eventName: 'bufferProgress',
    listenerFunc: (event: BufferProgressEvent) => void,
  ): Promise<PluginListenerHandle>
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
let currentSourceType: PlayOptions['sourceType'] | null = null
let currentStatus: PlaybackStatus = 'idle'
let currentPosition = 0
let currentDuration = 0
/** 已缓冲终点（秒）；null = 未知，不画缓冲条 */
let currentBufferedPosition: number | null = null
/** 最近一次缓冲比例；duration 晚到时用于换算秒数 */
let lastBufferedRatio: number | null = null
let fullyBufferedPending = false
let nativeListenersReady = false
let bridgeBufferListenerReady = false
let nativeListenerHandles: PluginListenerHandle[] = []
let bridgeBufferListenerHandle: PluginListenerHandle | null = null
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

/** 对外广播用的有限缓冲秒数；Infinity/未知不传，避免 controller 收到非有限值被丢弃。 */
const getEmitBufferedPosition = (): number | undefined => {
  if (currentBufferedPosition == null) {
    return undefined
  }
  if (Number.isFinite(currentBufferedPosition)) {
    return currentBufferedPosition
  }
  // full buffer 占位 Infinity：duration 已知时换算为全长，否则暂不广播（等 reconcile）
  return currentDuration > 0 ? currentDuration : undefined
}

const emitCurrentState = (status: PlaybackStatus = currentStatus): void => {
  currentStatus = status
  notifyState({
    status,
    currentSongId: currentSongId || undefined,
    position: currentPosition,
    duration: currentDuration,
    bufferedPosition: getEmitBufferedPosition(),
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
      reconcileBufferedWithDuration()
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

const ensureBridgeBufferListener = async (): Promise<void> => {
  if (bridgeBufferListenerReady || !AudioPlayerBridge.addListener) {
    return
  }
  bridgeBufferListenerReady = true
  try {
    bridgeBufferListenerHandle = await AudioPlayerBridge.addListener('bufferProgress', (event: BufferProgressEvent) => {
      if (!event.songId || event.songId !== currentSongId || currentSourceType === 'webdav') {
        return
      }

      if (typeof event.bufferedRatio === 'number' && Number.isFinite(event.bufferedRatio) && event.bufferedRatio >= 0) {
        lastBufferedRatio = Math.min(1, Math.max(0, event.bufferedRatio))
      }

      if (event.fullyBuffered === true) {
        fullyBufferedPending = true
        lastBufferedRatio = 1
        // 本地 full 或下载完成：缓冲 = duration（若已知），否则 POSITIVE_INFINITY 等 duration 补齐
        if (currentDuration > 0) {
          currentBufferedPosition = currentDuration
        } else if (typeof event.bufferedPosition === 'number' && Number.isFinite(event.bufferedPosition) && event.bufferedPosition >= 0) {
          currentBufferedPosition = event.bufferedPosition
        } else {
          currentBufferedPosition = Number.POSITIVE_INFINITY
        }
        reconcileBufferedWithDuration()
        emitCurrentState(currentStatus)
        return
      }

      if (typeof event.bufferedPosition === 'number' && Number.isFinite(event.bufferedPosition) && event.bufferedPosition >= 0) {
        // 单调不减
        currentBufferedPosition = Math.max(currentBufferedPosition ?? 0, event.bufferedPosition)
        reconcileBufferedWithDuration()
        emitCurrentState(currentStatus)
        return
      }

      // 仅有 ratio：duration 已知时换算；未知时不画假条（R6），等 duration 后 reconcile
      if (lastBufferedRatio != null && currentDuration > 0) {
        const next = currentDuration * lastBufferedRatio
        currentBufferedPosition = Math.max(currentBufferedPosition ?? 0, next)
        reconcileBufferedWithDuration()
        emitCurrentState(currentStatus)
      }
    })
  } catch {
    bridgeBufferListenerReady = false
  }
}

/** duration 更新后：用 ratio/full 标志补齐秒数，并把超界缓冲压回合法范围。 */
const reconcileBufferedWithDuration = (): void => {
  if (currentDuration <= 0) {
    return
  }
  if (fullyBufferedPending || (currentBufferedPosition != null && !Number.isFinite(currentBufferedPosition))) {
    currentBufferedPosition = currentDuration
    fullyBufferedPending = false
    return
  }
  if (currentBufferedPosition == null && lastBufferedRatio != null) {
    currentBufferedPosition = currentDuration * lastBufferedRatio
  }
  if (currentBufferedPosition != null && currentBufferedPosition > currentDuration) {
    currentBufferedPosition = currentDuration
  }
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

const cancelActiveBufferSession = async (songId?: string | null): Promise<void> => {
  if (!AudioPlayerBridge.cancelBufferSession) {
    return
  }
  await AudioPlayerBridge.cancelBufferSession(songId ? { songId } : undefined).catch(() => undefined)
}

const toAssetId = (songId: string): string => `song-${songId.replace(/[^a-zA-Z0-9_-]/g, '-')}`

const resolveAssetPath = async (options: PlayOptions): Promise<{ assetPath: string; headers?: Record<string, string>; fullBuffer?: boolean }> => {
  if (options.sourceType === 'webdav') {
    currentBufferedPosition = null
    return {
      assetPath: options.url,
      headers: {
        Authorization: `Basic ${encodeBasicAuth(options.username, options.password)}`,
      },
    }
  }

  if (options.uri.startsWith('content://') && AudioPlayerBridge.prepareLocalAudioFile) {
    const result = await AudioPlayerBridge.prepareLocalAudioFile({ uri: options.uri, songId: options.songId })
    // 本地 prepare 完成后由原生 bufferProgress(fullyBuffered) 上报；此处兜底 full
    currentBufferedPosition = Number.POSITIVE_INFINITY
    return { assetPath: result.uri, fullBuffer: true }
  }

  // 已是 file:// 等本地路径
  currentBufferedPosition = Number.POSITIVE_INFINITY
  return { assetPath: options.uri, fullBuffer: true }
}

export const AudioPlayerNative: AudioPlayerNativePlugin = {
  async play(options) {
    await configureNativeAudio()
    await ensureNativeListeners()
    await ensureBridgeBufferListener()
    await cancelActiveBufferSession(currentSongId)
    await unloadCurrentAsset()

    const assetId = toAssetId(options.songId)
    currentAssetId = assetId
    currentSongId = options.songId
    currentSourceType = options.sourceType
    currentStatus = 'loading'
    currentPosition = 0
    currentDuration = 0
    currentBufferedPosition = null
    lastBufferedRatio = null
    fullyBufferedPending = false
    emitCurrentState('loading')

    const { assetPath, headers, fullBuffer } = await resolveAssetPath(options)

    // 若在 resolve 过程中被切歌，放弃后续 play
    if (currentSongId !== options.songId) {
      return
    }

    if (fullBuffer) {
      currentBufferedPosition = Number.POSITIVE_INFINITY
      fullyBufferedPending = true
      lastBufferedRatio = 1
    }

    const isUrl = isRemoteUrl(assetPath) || assetPath.startsWith('file://')
    logNativeAudio('play:resolved', {
      assetId,
      sourceType: options.sourceType,
      assetPathPrefix: assetPath.slice(0, 80),
      isUrl,
      hasHeaders: Boolean(headers),
      buffered: currentBufferedPosition,
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
      reconcileBufferedWithDuration()
      logNativeAudio('duration:done', { assetId, currentDuration, buffered: currentBufferedPosition })
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
    await cancelActiveBufferSession(currentSongId)
    await unloadCurrentAsset()
    currentAssetId = null
    currentSongId = null
    currentSourceType = null
    currentPosition = 0
    currentDuration = 0
    currentBufferedPosition = null
    lastBufferedRatio = null
    fullyBufferedPending = false
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
      return {
        status: currentStatus,
        bufferedPosition: getEmitBufferedPosition(),
      }
    }

    const [position, duration, playing] = await Promise.all([
      NativeAudio.getCurrentTime({ assetId: currentAssetId }).catch(() => ({ currentTime: currentPosition })),
      NativeAudio.getDuration({ assetId: currentAssetId }).catch(() => ({ duration: currentDuration })),
      NativeAudio.isPlaying({ assetId: currentAssetId }).catch(() => ({ isPlaying: currentStatus === 'playing' })),
    ])
    currentPosition = normalizePlaybackTime(position.currentTime)
    currentDuration = normalizePlaybackTime(duration.duration)
    reconcileBufferedWithDuration()
    currentStatus = playing.isPlaying ? 'playing' : currentStatus === 'playing' ? 'paused' : currentStatus
    return {
      status: currentStatus,
      currentSongId: currentSongId || undefined,
      position: currentPosition,
      duration: currentDuration,
      bufferedPosition: getEmitBufferedPosition(),
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
    await ensureBridgeBufferListener().catch(() => undefined)
    return {
      remove: async () => {
        stateListeners.delete(listenerFunc)
        if (stateListeners.size === 0) {
          await Promise.all(nativeListenerHandles.map((handle) => handle.remove()))
          nativeListenerHandles = []
          nativeListenersReady = false
          if (bridgeBufferListenerHandle) {
            await bridgeBufferListenerHandle.remove().catch(() => undefined)
            bridgeBufferListenerHandle = null
            bridgeBufferListenerReady = false
          }
        }
      },
    }
  },
}
