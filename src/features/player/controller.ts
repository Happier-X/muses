import { computed, reactive, readonly } from 'vue'
import type { SongItem } from '@/features/library/types'
import { CURRENT_METADATA_VERSION, loadSongs, upsertSong } from '@/features/library/storage'
import { readLocalAudioTags, readWebDavAudioTags } from '@/features/library/tags'
import { getWebDavPassword, loadSources } from '@/features/sources/storage'
import type { WebDavSourceItem } from '@/features/sources/types'
import { AudioPlayerNative } from './native'
import type { AudioPlayerNativeState, PlayOptions, PlayerState } from './types'
import { createPlayerSongSnapshot, toSafeCoverUri } from './types'
import { advanceToNext, advanceToPrevious, syncCurrentIndex } from './queue'
import {
  setupMediaSessionActions,
  updateMediaSessionMetadata,
  updateMediaSessionPlayback,
  updateMediaSessionPosition,
  clearMediaSession,
} from './mediaSession'

const state = reactive<PlayerState>({
  status: 'idle',
  currentSong: null,
  errorMessage: null,
  position: 0,
  duration: 0,
  lyrics: null,
  coverUri: null,
  metadataStatus: 'idle',
})

let nativeListenerReady = false
let metadataScanToken = 0
/** 用户主动 seek 后的时间戳；用于忽略 seek 到未缓冲区间触发的伪 finished */
let lastSeekAt = 0
/** seek 前的播放态，伪 finished 时按此恢复，避免误 advance */
let statusBeforeSeek: 'playing' | 'paused' = 'playing'

const LOCAL_METADATA_SCAN_TIMEOUT_MS = 15_000
const WEBDAV_METADATA_SCAN_TIMEOUT_MS = 120_000
/** seek 后短保护窗：此期间 finished 一律视为伪结束，不自动切歌 */
const SEEK_FINISH_GUARD_MS = 1500
/** 判定自然播完的进度容差（秒） */
const NATURAL_END_EPSILON_SEC = 1.25

const clearSeekGuard = (): void => {
  lastSeekAt = 0
}

const isWithinSeekGuard = (): boolean => {
  return lastSeekAt > 0 && Date.now() - lastSeekAt < SEEK_FINISH_GUARD_MS
}

const isNearNaturalEnd = (position: number, duration: number): boolean => {
  // duration 未知时保守：不视为自然结束，避免远程未就绪时误切歌
  return duration > 0 && position >= duration - NATURAL_END_EPSILON_SEC
}

/** 非自然结束的 finished 不得 advance（seek 保护窗优先，即便目标靠近结尾） */
const shouldIgnoreFinished = (position: number, duration: number): boolean => {
  return isWithinSeekGuard() || !isNearNaturalEnd(position, duration)
}

const setUserSafeError = (message: string) => {
  state.status = 'error'
  state.errorMessage = message
}

const isCurrentNativeState = (nativeState: AudioPlayerNativeState): boolean => {
  return !nativeState.currentSongId || nativeState.currentSongId === state.currentSong?.id
}

const applyNativeState = (nativeState: AudioPlayerNativeState): void => {
  if (!isCurrentNativeState(nativeState)) {
    return
  }

  // finished 需先做「自然结束」判定：seek 到未缓冲区间时插件可能伪报 complete/ENDED。
  if (nativeState.status === 'finished') {
    const nativePosition = normalizePlaybackTime(nativeState.position)
    const nativeDuration = normalizePlaybackTime(nativeState.duration) || state.duration
    // 保护窗内保留 seek 目标进度；窗外取 native/state 较大值，
    // 避免 complete 事件 position 回 0 时误判「未接近结尾」而吞掉真实播完。
    const effectivePosition = isWithinSeekGuard()
      ? state.position
      : Math.max(nativePosition, state.position)
    const effectiveDuration = nativeDuration

    if (shouldIgnoreFinished(effectivePosition, effectiveDuration)) {
      if (!isWithinSeekGuard() && nativePosition > 0) {
        state.position = nativePosition
      }
      state.duration = effectiveDuration
      state.status = isWithinSeekGuard() ? statusBeforeSeek : 'paused'
      state.errorMessage = null
      syncMediaSessionState()
      return
    }

    state.status = 'finished'
    state.errorMessage = null
    state.position = effectivePosition
    state.duration = effectiveDuration
    syncMediaSessionState()
    void handlePlaybackFinished()
    return
  }

  state.status = nativeState.status
  state.errorMessage = nativeState.status === 'error' ? nativeState.errorMessage || '播放失败，请稍后重试。' : null
  state.position = normalizePlaybackTime(nativeState.position)
  state.duration = normalizePlaybackTime(nativeState.duration)

  if (nativeState.status === 'idle' || nativeState.status === 'stopped') {
    state.currentSong = null
    state.position = 0
    state.duration = 0
    state.lyrics = null
    state.coverUri = null
    state.metadataStatus = 'idle'
    syncMediaSessionState()
    return
  }

  syncMediaSessionState()
}

const handlePlaybackFinished = async (): Promise<void> => {
  const nextSong = advanceToNext()
  if (nextSong) {
    await playSong(nextSong)
    return
  }

  await stopPlayback()
}

export const playNextFromQueue = async (): Promise<void> => {
  const nextSong = advanceToNext()
  if (nextSong) {
    await playSong(nextSong)
  }
}

export const playPreviousFromQueue = async (): Promise<void> => {
  const previousSong = advanceToPrevious()
  if (previousSong) {
    await playSong(previousSong)
  }
}

export const initializePlayer = async (): Promise<void> => {
  if (!nativeListenerReady) {
    nativeListenerReady = true
    await AudioPlayerNative.addListener('stateChange', applyNativeState)
  }

  await setupMediaSessionActions({
    play: resumePlayback,
    pause: pausePlayback,
    stop: stopPlayback,
    previoustrack: playPreviousFromQueue,
    nexttrack: playNextFromQueue,
    seekto: seekPlayback,
  })

  try {
    applyNativeState(await AudioPlayerNative.getState())
  } catch {
    // 非 Android 或原生插件尚不可用时，保持空闲状态，用户点击播放时再显示明确错误。
  }
}

const normalizePlaybackTime = (value: unknown): number => {
  return typeof value === 'number' && Number.isFinite(value) && value > 0 ? value : 0
}

const syncMediaSessionState = (): void => {
  if (!state.currentSong) {
    void clearMediaSession().catch(() => undefined)
    return
  }

  void updateMediaSessionPlayback(state.status).catch(() => undefined)
  void updateMediaSessionPosition(state.position, state.duration).catch(() => undefined)
}

const syncMediaSessionSong = (song: SongItem): void => {
  void updateMediaSessionMetadata({
    title: song.title,
    artist: song.artist,
    album: song.album,
    coverUri: toSafeCoverUri(song.coverUri),
  }).catch(() => undefined)
  void updateMediaSessionPosition(normalizePlaybackTime(state.position), normalizePlaybackTime(song.duration) || state.duration).catch(() => undefined)
  void updateMediaSessionPlayback(state.status).catch(() => undefined)
}

const syncDisplayStateFromSong = (song: SongItem): void => {
  if (state.currentSong?.id !== song.id) {
    return
  }

  const previous = state.currentSong
  const nextCover = toSafeCoverUri(song.coverUri) || null
  // 懒扫描补全封面/标签后必须 re-sync 媒体会话，否则通知栏永远拿不到新封面。
  const mediaFieldsChanged =
    previous.title !== song.title
    || previous.artist !== song.artist
    || previous.album !== song.album
    || state.coverUri !== nextCover

  state.currentSong = createPlayerSongSnapshot(song)
  state.lyrics = song.lyrics || null
  state.coverUri = nextCover
  state.duration = normalizePlaybackTime(song.duration) || state.duration

  if (mediaFieldsChanged) {
    syncMediaSessionSong(song)
  }
}

const getWebDavSource = (song: SongItem): WebDavSourceItem => {
  const source = loadSources().find((item) => item.id === song.sourceId && item.type === 'webdav')
  if (!source || source.type !== 'webdav') {
    throw new Error('找不到这首歌对应的 WebDAV 音源，请重新扫描音源。')
  }

  return source
}

const buildPlayOptions = async (song: SongItem): Promise<PlayOptions> => {
  if (song.sourceType === 'local') {
    return {
      sourceType: 'local',
      songId: song.id,
      uri: song.uri,
      title: song.title,
      artist: song.artist,
      album: song.album,
      coverUri: toSafeCoverUri(song.coverUri),
    }
  }

  const source = getWebDavSource(song)
  const password = await requireWebDavPassword(song)

  return {
    sourceType: 'webdav',
    songId: song.id,
    url: song.uri,
    username: source.username,
    password,
    title: song.title,
    artist: song.artist,
    album: song.album,
    coverUri: toSafeCoverUri(song.coverUri),
  }
}

const buildAudioFileEntry = (song: SongItem) => ({
  path: song.path,
  uri: song.uri,
  name: song.path.split('/').pop() || song.title,
})

const getLatestSongSnapshot = (song: SongItem): SongItem => {
  return loadSongs().find((item) => item.id === song.id || (item.sourceId === song.sourceId && item.path === song.path)) ?? song
}

const shouldRefreshMetadata = (song: SongItem): boolean => {
  return song.tagsScanned !== true || song.metadataVersion !== CURRENT_METADATA_VERSION || (!song.lyrics && !song.coverUri)
}

const withMetadataScanTimeout = async <T>(operation: Promise<T>, timeoutMs: number): Promise<T> => {
  let timeoutId: ReturnType<typeof setTimeout> | undefined
  const timeout = new Promise<never>((_, reject) => {
    timeoutId = setTimeout(() => reject(new Error('歌曲信息补充超时。')), timeoutMs)
  })

  try {
    return await Promise.race([operation, timeout])
  } finally {
    if (timeoutId !== undefined) {
      clearTimeout(timeoutId)
    }
  }
}

const scanSongMetadata = async (song: SongItem): Promise<void> => {
  if (!shouldRefreshMetadata(song)) {
    syncDisplayStateFromSong(song)
    state.metadataStatus = 'ready'
    return
  }

  const token = ++metadataScanToken
  state.metadataStatus = 'scanning'

  try {
    const timeoutMs = song.sourceType === 'webdav' ? WEBDAV_METADATA_SCAN_TIMEOUT_MS : LOCAL_METADATA_SCAN_TIMEOUT_MS
    const tags = await withMetadataScanTimeout(song.sourceType === 'local'
      ? readLocalAudioTags(buildAudioFileEntry(song), song.id)
      : readWebDavAudioTags(getWebDavSource(song), buildAudioFileEntry(song), await requireWebDavPassword(song)), timeoutMs)

    if (token !== metadataScanToken || state.currentSong?.id !== song.id) {
      return
    }

    const result = upsertSong({
      sourceId: song.sourceId,
      sourceType: song.sourceType,
      path: song.path,
      uri: song.uri,
      title: song.title,
      tags: {
        ...tags,
        tagsScanned: true,
        tagsScannedAt: new Date().toISOString(),
        metadataVersion: CURRENT_METADATA_VERSION,
      },
    }, loadSongs())

    syncDisplayStateFromSong(result.song)
    state.metadataStatus = 'ready'
  } catch {
    if (token === metadataScanToken && state.currentSong?.id === song.id) {
      state.metadataStatus = 'failed'
    }
  }
}

const requireWebDavPassword = async (song: SongItem): Promise<string> => {
  const source = getWebDavSource(song)
  const password = await getWebDavPassword(source.credentialKey)
  if (!password) {
    throw new Error('WebDAV 密码不存在，请重新添加该音源。')
  }
  return password
}

const SAFE_PLAYBACK_ERRORS = new Set([
  '找不到这首歌对应的 WebDAV 音源，请重新扫描音源。',
  'WebDAV 密码不存在，请重新添加该音源。',
  'WebDAV 播放缺少认证信息。',
  '本地音频文件不可访问，请重新扫描或重新授权。',
  '本地音频文件无访问权限，请重新授权音源目录。',
  'WebDAV 认证失败，请检查账号或重新添加音源。',
  '音频文件不存在或已失效，请重新扫描音源。',
  '播放失败，请检查音频文件或网络连接。',
])

const isSafePlaybackError = (message: string): boolean => {
  return SAFE_PLAYBACK_ERRORS.has(message)
}

export const playSong = async (song: SongItem): Promise<void> => {
  const latestSong = getLatestSongSnapshot(song)

  syncCurrentIndex(latestSong.id)
  // 切歌清理 seek 保护，避免新歌首帧误吞真实 finished
  clearSeekGuard()

  state.status = 'loading'
  metadataScanToken += 1
  state.currentSong = createPlayerSongSnapshot(latestSong)
  state.errorMessage = null
  state.position = 0
  state.duration = normalizePlaybackTime(latestSong.duration)
  state.lyrics = latestSong.lyrics || null
  state.coverUri = toSafeCoverUri(latestSong.coverUri) || null
  state.metadataStatus = latestSong.tagsScanned === true ? 'ready' : 'idle'
  syncMediaSessionSong(latestSong)

  try {
    try {
      await AudioPlayerNative.ensureNotificationPermission()
    } catch {
      // 权限请求失败（非 Android / 插件不可用）静默忽略，不阻塞播放。
    }
    await AudioPlayerNative.play(await buildPlayOptions(latestSong))
    state.status = 'playing'
    void scanSongMetadata(latestSong)
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    setUserSafeError(isSafePlaybackError(message) ? message : '播放失败，请稍后重试。')
    // loading 会乐观映射为 playing；播放失败时必须清掉媒体会话，避免残留通知/封面回调。
    void clearMediaSession().catch(() => undefined)
  }
}

export const pausePlayback = async (): Promise<void> => {
  try {
    await AudioPlayerNative.pause()
    state.status = 'paused'
    state.errorMessage = null
  } catch {
    setUserSafeError('暂停失败，请稍后重试。')
  }
}

export const resumePlayback = async (): Promise<void> => {
  try {
    await AudioPlayerNative.resume()
    state.status = 'playing'
    state.errorMessage = null
  } catch {
    setUserSafeError('继续播放失败，请稍后重试。')
  }
}

export const seekPlayback = async (position: number): Promise<void> => {
  const safePosition = state.duration > 0
    ? Math.min(normalizePlaybackTime(position), state.duration)
    : normalizePlaybackTime(position)
  try {
    statusBeforeSeek = state.status === 'paused' ? 'paused' : 'playing'
    await AudioPlayerNative.seek({ position: safePosition })
    state.position = safePosition
    state.errorMessage = null
    // seek 成功后开启短保护窗，吞掉未缓冲区间触发的伪 finished
    lastSeekAt = Date.now()
  } catch {
    setUserSafeError('跳转播放进度失败，请稍后重试。')
  }
}

export const stopPlayback = async (): Promise<void> => {
  try {
    await AudioPlayerNative.stop()
    clearSeekGuard()
    metadataScanToken += 1
    state.status = 'stopped'
    state.currentSong = null
    state.errorMessage = null
    state.position = 0
    state.duration = 0
    state.lyrics = null
    state.coverUri = null
    state.metadataStatus = 'idle'
    syncMediaSessionState()
  } catch {
    setUserSafeError('停止播放失败，请稍后重试。')
  }
}

export const playerState = readonly(state)
export const isPlaying = computed(() => state.status === 'playing')
export const hasActiveSong = computed(() => Boolean(state.currentSong))
export const isPlaybackFinished = computed(() => state.status === 'finished')

export {
  advanceToNext,
  advanceToPrevious,
  clearQueue,
  enqueueSong,
  enqueueSongs,
  queueState,
  removeSongFromQueue,
  repeatMode,
  selectSongAtIndex,
  shuffleEnabled,
  toggleShuffle,
  setRepeatMode,
} from './queue'
