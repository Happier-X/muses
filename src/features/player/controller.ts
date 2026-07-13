import { computed, reactive, readonly } from 'vue'
import type { SongItem } from '@/features/library/types'
import { CURRENT_METADATA_VERSION, loadSongs, upsertSong } from '@/features/library/storage'
import { readLocalAudioTags, readWebDavAudioTags } from '@/features/library/tags'
import { getWebDavPassword, loadSources } from '@/features/sources/storage'
import type { WebDavSourceItem } from '@/features/sources/types'
import { matchAmllTtmlLyrics } from '@/features/lyrics'
import { AudioPlayerNative, prefetchWebDavAudioFile } from './native'
import type { AudioPlayerNativeState, PlayOptions, PlayerState } from './types'
import { createPlayerSongSnapshot, toSafeCoverUri } from './types'
import {
  advanceToNext,
  advanceToPrevious,
  clearQueue as clearQueueInternal,
  enqueueSong as enqueueSongInternal,
  enqueueSongs as enqueueSongsInternal,
  peekNext,
  removeSongFromQueue as removeSongFromQueueInternal,
  setRepeatMode as setRepeatModeInternal,
  syncCurrentIndex,
  toggleShuffle as toggleShuffleInternal,
  type RepeatMode,
} from './queue'
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
  bufferedPosition: null,
  lyrics: null,
  lyricsFormat: null,
  onlineLyricsStatus: 'idle',
  coverUri: null,
  metadataStatus: 'idle',
})

let nativeListenerReady = false
let metadataScanToken = 0
/** 在线歌词匹配 token：切歌递增，回调仅当 token + songId 仍匹配时写 state */
let lyricsMatchToken = 0
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

  // 缓冲：原生上报时单调合并；stop/idle/error 在下方 reset，禁止串曲
  if (
    nativeState.bufferedPosition !== undefined
    && nativeState.status !== 'idle'
    && nativeState.status !== 'stopped'
    && nativeState.status !== 'error'
  ) {
    const nextBuffered = normalizeBufferedPosition(nativeState.bufferedPosition)
    if (nextBuffered != null) {
      const capped = state.duration > 0 ? Math.min(nextBuffered, state.duration) : nextBuffered
      state.bufferedPosition = Math.max(state.bufferedPosition ?? 0, capped)
    }
  }

  if (nativeState.status === 'idle' || nativeState.status === 'stopped') {
    state.currentSong = null
    state.position = 0
    state.duration = 0
    resetBufferState()
    state.lyrics = null
    state.lyricsFormat = null
    state.onlineLyricsStatus = 'idle'
    state.coverUri = null
    state.metadataStatus = 'idle'
    syncMediaSessionState()
    return
  }

  if (nativeState.status === 'error') {
    resetBufferState()
  }

  // duration 晚到：把已有缓冲压回 duration
  if (state.duration > 0 && state.bufferedPosition != null && state.bufferedPosition > state.duration) {
    state.bufferedPosition = state.duration
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

/** 将原生/业务层缓冲秒数归一；未知或非法 → null（不画假缓冲条）。 */
const normalizeBufferedPosition = (value: unknown): number | null => {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0) {
    return null
  }
  return value
}

/**
 * 可 seek 上限：缓冲已知时 min(duration, bufferedPosition)；
 * 缓冲未知时退化为 duration clamp（R6 降级）。
 */
const getMaxSeekablePosition = (): number => {
  if (state.duration > 0 && state.bufferedPosition != null && state.bufferedPosition >= 0) {
    return Math.min(state.duration, state.bufferedPosition)
  }
  if (state.duration > 0) {
    return state.duration
  }
  if (state.bufferedPosition != null && state.bufferedPosition >= 0) {
    return state.bufferedPosition
  }
  return Number.POSITIVE_INFINITY
}

const resetBufferState = (): void => {
  state.bufferedPosition = null
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
  // 在线 TTML 优先：匹配成功后不再被懒扫描的本地 LRC 覆盖
  if (state.lyricsFormat !== 'ttml') {
    state.lyrics = song.lyrics || null
    state.lyricsFormat = song.lyrics ? 'lrc' : null
  }
  state.coverUri = nextCover
  state.duration = normalizePlaybackTime(song.duration) || state.duration

  if (mediaFieldsChanged) {
    syncMediaSessionSong(song)
  }
}

/**
 * 切歌后异步匹配 amll-ttml-db；
 * 成功写 TTML，失败保持本地 LRC/空态；token 防串曲。
 */
const matchOnlineLyricsForSong = async (song: SongItem, token: number): Promise<void> => {
  const localLyrics = song.lyrics || null
  state.onlineLyricsStatus = 'matching'

  try {
    const result = await matchAmllTtmlLyrics({
      songId: song.id,
      title: song.title,
      artist: song.artist,
      album: song.album,
    })

    // 快速切歌：过期 token 或已不是当前曲，丢弃结果
    if (token !== lyricsMatchToken || state.currentSong?.id !== song.id) {
      return
    }

    if (result.ok) {
      state.lyrics = result.ttml
      state.lyricsFormat = 'ttml'
      state.onlineLyricsStatus = 'ready'
      return
    }

    // 失败回退本地；匹配期间标签补扫可能刚补到 LRC，优先保留 state 中的新本地词
    const fallbackLyrics = state.lyricsFormat === 'lrc' ? state.lyrics : localLyrics
    state.lyrics = fallbackLyrics
    state.lyricsFormat = fallbackLyrics ? 'lrc' : null
    state.onlineLyricsStatus = result.reason === 'network' || result.reason === 'parse' ? 'error' : 'miss'
  } catch {
    if (token !== lyricsMatchToken || state.currentSong?.id !== song.id) {
      return
    }
    const fallbackLyrics = state.lyricsFormat === 'lrc' ? state.lyrics : localLyrics
    state.lyrics = fallbackLyrics
    state.lyricsFormat = fallbackLyrics ? 'lrc' : null
    state.onlineLyricsStatus = 'error'
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
      duration: song.duration,
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
    duration: song.duration,
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

/**
 * 当前曲进入 playing 后调度下一首 WebDAV 完整预取。
 * 跳过：空队列 / 单曲循环自身 / 本地 / 非 webdav。
 * 密码仅传到 bridge；失败静默，不阻塞播放。
 */
const prefetchNextTrack = async (currentSongId: string): Promise<void> => {
  try {
    const next = peekNext()
    if (!next) {
      return
    }
    // 单曲循环下一首是自身：不预取
    if (next.id === currentSongId) {
      return
    }
    if (next.sourceType !== 'webdav') {
      return
    }

    const source = getWebDavSource(next)
    const password = await getWebDavPassword(source.credentialKey)
    if (!password) {
      return
    }

    await prefetchWebDavAudioFile({
      url: next.uri,
      username: source.username,
      password,
      songId: next.id,
    })
  } catch {
    // 预取失败不得影响当前播放或切歌
  }
}

/**
 * 队列/循环/随机变更后重新解析下一首并调度预取。
 * 仅在仍有当前曲且处于 playing/paused 时重调度；旧下载由原生侧不取消。
 */
const reschedulePrefetchAfterQueueChange = (): void => {
  const currentId = state.currentSong?.id
  if (!currentId) {
    return
  }
  if (state.status !== 'playing' && state.status !== 'paused') {
    return
  }
  void prefetchNextTrack(currentId)
}

export const enqueueSongs = (songs: SongItem[]): void => {
  enqueueSongsInternal(songs)
  reschedulePrefetchAfterQueueChange()
}

export const enqueueSong = (song: SongItem): void => {
  enqueueSongInternal(song)
  reschedulePrefetchAfterQueueChange()
}

export const removeSongFromQueue = (songId: string): void => {
  removeSongFromQueueInternal(songId)
  reschedulePrefetchAfterQueueChange()
}

export const clearQueue = (): void => {
  clearQueueInternal()
  reschedulePrefetchAfterQueueChange()
}

export const setRepeatMode = (mode: RepeatMode): void => {
  setRepeatModeInternal(mode)
  reschedulePrefetchAfterQueueChange()
}

export const toggleShuffle = (): void => {
  toggleShuffleInternal()
  reschedulePrefetchAfterQueueChange()
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
  const matchToken = ++lyricsMatchToken
  state.currentSong = createPlayerSongSnapshot(latestSong)
  state.errorMessage = null
  state.position = 0
  state.duration = normalizePlaybackTime(latestSong.duration)
  // 切歌先清缓冲，禁止继承上一首（R7）；本地 full / 远程增长由 native 再写入
  resetBufferState()
  // 先展示本地歌词（可空），再异步匹配在线 TTML（在线优先）
  state.lyrics = latestSong.lyrics || null
  state.lyricsFormat = latestSong.lyrics ? 'lrc' : null
  state.onlineLyricsStatus = 'matching'
  state.coverUri = toSafeCoverUri(latestSong.coverUri) || null
  state.metadataStatus = latestSong.tagsScanned === true ? 'ready' : 'idle'
  syncMediaSessionSong(latestSong)

  // 无论是否有本地歌词，都自动尝试在线匹配（不阻塞播放）
  void matchOnlineLyricsForSong(latestSong, matchToken)

  try {
    try {
      await AudioPlayerNative.ensureNotificationPermission()
    } catch {
      // 权限请求失败（非 Android / 插件不可用）静默忽略，不阻塞播放。
    }
    await AudioPlayerNative.play(await buildPlayOptions(latestSong))
    state.status = 'playing'
    void scanSongMetadata(latestSong)
    // 播放成功后后台预取下一首 WebDAV（失败静默）
    void prefetchNextTrack(latestSong.id)
  } catch (error) {
    lyricsMatchToken += 1
    state.onlineLyricsStatus = 'idle'
    const message = error instanceof Error ? error.message : ''
    setUserSafeError(isSafePlaybackError(message) ? message : '播放失败，请稍后重试。')
    resetBufferState()
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

/**
 * 统一 seek 入口（进度条 / 歌词 / 媒体会话 seekto）。
 * - 缓冲已知：上限 = min(duration, bufferedPosition)；越界目标不 seek（R2/R3 歌词拒绝）
 * - 缓冲未知：退化为 duration clamp（R6）
 * - 返回是否实际发起 seek，供 UI 做轻提示
 */
export const seekPlayback = async (position: number): Promise<boolean> => {
  const requested = normalizePlaybackTime(position)
  const maxSeekable = getMaxSeekablePosition()

  // 歌词/进度条：目标超出已缓冲区间时拒绝，不发起越界 seek（R3）
  if (state.bufferedPosition != null && Number.isFinite(maxSeekable) && requested > maxSeekable + 0.05) {
    return false
  }

  const safePosition = Number.isFinite(maxSeekable)
    ? Math.min(requested, maxSeekable)
    : requested

  try {
    statusBeforeSeek = state.status === 'paused' ? 'paused' : 'playing'
    await AudioPlayerNative.seek({ position: safePosition })
    state.position = safePosition
    state.errorMessage = null
    // seek 成功后开启短保护窗，吞掉未缓冲区间触发的伪 finished
    lastSeekAt = Date.now()
    return true
  } catch {
    setUserSafeError('跳转播放进度失败，请稍后重试。')
    return false
  }
}

export const stopPlayback = async (): Promise<void> => {
  try {
    await AudioPlayerNative.stop()
    clearSeekGuard()
    metadataScanToken += 1
    lyricsMatchToken += 1
    state.status = 'stopped'
    state.currentSong = null
    state.errorMessage = null
    state.position = 0
    state.duration = 0
    resetBufferState()
    state.lyrics = null
    state.lyricsFormat = null
    state.onlineLyricsStatus = 'idle'
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
  peekNext,
  queueState,
  repeatMode,
  selectSongAtIndex,
  shuffleEnabled,
} from './queue'
