import { NativeAudio } from '@capgo/capacitor-native-audio'
import { registerPlugin } from '@capacitor/core'
import type { PluginListenerHandle } from '@capacitor/core'
import { PLAYBACK_VOLUME_MAX, PLAYBACK_VOLUME_MIN } from './loudness'
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
  /** 渐进下载保留兼容，播放路径禁止调用。 */
  prepareWebDavAudioFile?(options: {
    url: string
    username: string
    password: string
    songId: string
    duration?: number
  }): Promise<{ uri: string; ready?: boolean }>
  /** 仅返回完整缓存 URI；partial / 未命中返回 uri=null。 */
  getCachedWebDavAudioFile?(options: { url: string }): Promise<{ uri: string | null }>
  /** 后台完整预取；cached=已有完整缓存，started=新启动下载。 */
  prefetchWebDavAudioFile?(options: {
    url: string
    username: string
    password: string
    songId: string
  }): Promise<{ cached: boolean; started: boolean }>
  cancelBufferSession?(options?: { songId?: string }): Promise<void>
  prepareArtworkDataUrl?(options: { uri: string }): Promise<{ dataUrl: string | null }>
  /** 下载远程封面到 cache/covers，返回 file://；失败 uri=null */
  cacheRemoteCover?(options: { url: string; cacheKey: string }): Promise<{ uri: string | null }>
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
  /** 对当前 asset 设音量；无当前曲时 no-op。volume 建议已 clamp 到 0.1–1.0 */
  setVolume(volume: number): Promise<void>
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
/** playing 时轮询 getCurrentTime，兜底插件 currentTime 事件丢失/timer 停转（#47）；500ms 降低 WebView 压力（#50） */
const POSITION_POLL_MS = 500
/** 轮询 path 下低于此变化不 emit，减少无意义响应式刷新（#50） */
const POSITION_EMIT_EPSILON_SEC = 0.05
let positionPollTimer: ReturnType<typeof setInterval> | null = null

const isNativeAudioDebugEnabled = (): boolean => {
  try {
    return localStorage.getItem('muses:debug-native-audio') === '1'
  } catch {
    return false
  }
}

const logNativeAudio = (message: string, data?: unknown): void => {
  // 默认静默；调试时 localStorage.setItem('muses:debug-native-audio', '1')（#50）
  if (!isNativeAudioDebugEnabled()) {
    return
  }
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
      // 有当前 asset 时必须 id 匹配；缺 assetId 的陈旧事件在快速切歌时会污染状态（#28/#29）
      if (currentAssetId && event.assetId !== currentAssetId) {
        return
      }
      if (typeof event.currentTime === 'number' && Number.isFinite(event.currentTime) && event.currentTime >= 0) {
        currentPosition = normalizePlaybackTime(event.currentTime)
      }
      if (typeof event.duration === 'number' && Number.isFinite(event.duration) && event.duration >= 0) {
        currentDuration = normalizePlaybackTime(event.duration) || currentDuration
      }
      reconcileBufferedWithDuration()
      emitCurrentState(mapPlaybackStatus(event))
    }),
    NativeAudio.addListener('currentTime', (event: NativeCurrentTimeEvent) => {
      if (currentAssetId && event.assetId !== currentAssetId) {
        return
      }
      currentPosition = normalizePlaybackTime(event.currentTime)
      emitCurrentState(currentStatus)
    }),
    NativeAudio.addListener('complete', (event: NativeCompleteEvent) => {
      if (currentAssetId && event.assetId !== currentAssetId) {
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
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : 0
}

const stopPositionPolling = (): void => {
  if (positionPollTimer != null) {
    clearInterval(positionPollTimer)
    positionPollTimer = null
  }
}

const pollPlaybackPosition = async (): Promise<void> => {
  const assetId = currentAssetId
  if (!assetId || currentStatus !== 'playing') {
    return
  }

  try {
    const result = await NativeAudio.getCurrentTime({ assetId }).catch(() => null)
    if (!result || currentAssetId !== assetId || currentStatus !== 'playing') {
      return
    }
    const next = normalizePlaybackTime(result.currentTime)
    // 与事件路径合并；微小变化不 emit，减少 Vue/media 更新频次（#50）
    if (Math.abs(next - currentPosition) >= POSITION_EMIT_EPSILON_SEC) {
      currentPosition = next
      emitCurrentState('playing')
    }
  } catch {
    // 轮询失败不打断播放
  }
}

const startPositionPolling = (): void => {
  stopPositionPolling()
  if (!currentAssetId || currentStatus !== 'playing') {
    return
  }
  positionPollTimer = setInterval(() => {
    void pollPlaybackPosition()
  }, POSITION_POLL_MS)
}

const unloadCurrentAsset = async (): Promise<void> => {
  stopPositionPolling()
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

/**
 * 查询 WebDAV 完整缓存。仅完整文件可本地播放；partial / 失败一律 null。
 * 禁止调用 prepareWebDavAudioFile 作为播放路径。
 */
const resolveCachedWebDavUri = async (url: string): Promise<string | null> => {
  if (!AudioPlayerBridge.getCachedWebDavAudioFile) {
    return null
  }
  try {
    const result = await AudioPlayerBridge.getCachedWebDavAudioFile({ url })
    const uri = result?.uri
    if (typeof uri === 'string' && uri.length > 0 && !uri.includes('.partial') && !uri.endsWith('.tmp')) {
      return uri
    }
    return null
  } catch {
    return null
  }
}

/**
 * 后台预取 WebDAV 完整文件。失败静默；不阻塞播放。
 * 密码仅传到 bridge，不写日志。
 */
export const prefetchWebDavAudioFile = async (options: {
  url: string
  username: string
  password: string
  songId: string
}): Promise<{ cached: boolean; started: boolean }> => {
  if (!AudioPlayerBridge.prefetchWebDavAudioFile) {
    return { cached: false, started: false }
  }
  try {
    const result = await AudioPlayerBridge.prefetchWebDavAudioFile({
      url: options.url,
      username: options.username,
      password: options.password,
      songId: options.songId,
    })
    return {
      cached: result?.cached === true,
      started: result?.started === true,
    }
  } catch {
    return { cached: false, started: false }
  }
}

/**
 * 缓存远程封面到 app 私有 covers 目录，返回安全 file://。
 * 插件不可用或失败时返回 null（不抛）。
 */
export const cacheRemoteCover = async (options: {
  url: string
  cacheKey: string
}): Promise<string | null> => {
  if (!AudioPlayerBridge.cacheRemoteCover) {
    return null
  }
  try {
    const result = await AudioPlayerBridge.cacheRemoteCover({
      url: options.url,
      cacheKey: options.cacheKey,
    })
    const uri = result?.uri?.trim()
    if (!uri) {
      return null
    }
    const normalized = uri.toLowerCase()
    // 仅接受本地/内容 URI；拒绝 data/base64/远程 URL 误写曲库
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

const resolveAssetPath = async (options: PlayOptions): Promise<{ assetPath: string; headers?: Record<string, string>; fullBuffer?: boolean }> => {
  if (options.sourceType === 'webdav') {
    // 完整缓存优先：命中 file:// + full buffer；未命中远程直链 + Basic Auth
    const cachedUri = await resolveCachedWebDavUri(options.url)
    if (cachedUri) {
      currentBufferedPosition = Number.POSITIVE_INFINITY
      return { assetPath: cachedUri, fullBuffer: true }
    }

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

    // 响度均衡 volume：缺省 1.0；由 controller 按 RG 计算后传入
    const volume = typeof options.volume === 'number' && Number.isFinite(options.volume)
      ? Math.min(PLAYBACK_VOLUME_MAX, Math.max(PLAYBACK_VOLUME_MIN, options.volume))
      : PLAYBACK_VOLUME_MAX

    try {
      await NativeAudio.preload({
        assetId,
        assetPath,
        isUrl,
        audioChannelNum: 1,
        headers,
        volume,
      })
      logNativeAudio('preload:done', { assetId, volume })
      currentDuration = normalizePlaybackTime((await NativeAudio.getDuration({ assetId }).catch(() => ({ duration: 0 }))).duration)
      reconcileBufferedWithDuration()
      logNativeAudio('duration:done', { assetId, currentDuration, buffered: currentBufferedPosition })
      await NativeAudio.play({ assetId, volume })
      // 成功路径再 setVolume，确保部分平台 preload volume 未生效时仍应用
      await NativeAudio.setVolume({ assetId, volume }).catch(() => undefined)
      logNativeAudio('play:done', { assetId, volume })
      emitCurrentState('playing')
      startPositionPolling()
    } catch (error) {
      stopPositionPolling()
      logNativeAudio('play:error', error instanceof Error ? { message: error.message, stack: error.stack } : error)
      throw error
    }
  },

  async setVolume(volume: number) {
    if (!currentAssetId) {
      return
    }
    const safe = typeof volume === 'number' && Number.isFinite(volume)
      ? Math.min(PLAYBACK_VOLUME_MAX, Math.max(PLAYBACK_VOLUME_MIN, volume))
      : PLAYBACK_VOLUME_MAX
    await NativeAudio.setVolume({ assetId: currentAssetId, volume: safe }).catch(() => undefined)
  },

  async pause() {
    if (!currentAssetId) {
      return
    }
    stopPositionPolling()
    await NativeAudio.pause({ assetId: currentAssetId })
    emitCurrentState('paused')
  },

  async resume() {
    if (!currentAssetId) {
      return
    }
    await NativeAudio.resume({ assetId: currentAssetId })
    emitCurrentState('playing')
    startPositionPolling()
  },

  async stop() {
    stopPositionPolling()
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
    // seek 不保证插件重启 timer；playing 时确保轮询兜底（#47）
    if (currentStatus === 'playing') {
      startPositionPolling()
    }
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
