import { computed, reactive, readonly } from 'vue'
import type { SongItem } from '@/features/library/types'
import { CURRENT_METADATA_VERSION, loadSongs, upsertSong } from '@/features/library/storage'
import { readLocalAudioTags, readWebDavAudioTags } from '@/features/library/tags'
import { getWebDavPassword, loadSources } from '@/features/sources/storage'
import type { WebDavSourceItem } from '@/features/sources/types'
import { matchOnlineLyrics } from '@/features/lyrics'
import { matchOnlineCoverRemote } from '@/features/cover'
import {
  matchOnlineTextMeta,
  mergeTextMetaFillEmpty,
  needsOnlineTextMeta,
} from '@/features/metadata'
import { AudioPlayerNative, cacheRemoteCover, prefetchWebDavAudioFile } from './native'
import { dbToPlaybackVolume } from './loudness'
import type { AudioPlayerNativeState, PlayOptions, PlayerState } from './types'
import {
  createPlayerSongSnapshot,
  resolveStoredLyricsFormat,
  shouldApplyStoredLyricsOverRuntime,
  shouldPersistOnlineLyrics,
  toSafeCoverUri,
} from './types'
import {
  advanceToNext,
  advanceToNextRecoveryCandidate as selectRecoveryCandidate,
  advanceToPrevious,
  clearQueue as clearQueueInternal,
  enqueueSong as enqueueSongInternal,
  enqueueSongs as enqueueSongsInternal,
  isLoudnessNormalizeEnabled,
  peekNext,
  removeSongFromQueue as removeSongFromQueueInternal,
  setLoudnessNormalizeEnabled as setLoudnessNormalizeEnabledInternal,
  findQueueIndexBySongId,
  setRepeatMode as setRepeatModeInternal,
  syncCurrentIndex,
  toggleShuffle as toggleShuffleInternal,
  type RepeatMode,
} from './queue'
import {
  clearPlaybackSession,
  loadPlaybackSession,
  savePlaybackSession,
} from './session'
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
  lyricsTranslation: null,
  onlineLyricsStatus: 'idle',
  coverUri: null,
  metadataStatus: 'idle',
})

let nativeListenerReady = false
let metadataScanToken = 0
/** 在线歌词匹配 token：切歌递增，回调仅当 token + songId 仍匹配时写 state */
let lyricsMatchToken = 0
/** 在线封面匹配 token：切歌递增，回调仅当 token + songId 仍匹配时写 state */
let onlineCoverToken = 0
/** 在线文本元信息 token：与封面分开，防串曲 */
let onlineTextToken = 0
/** playSong 代际：快速连切时仅最新一代可写 playing/error */
let playGeneration = 0
/** 用户主动 seek 后的时间戳；用于忽略 seek 到未缓冲区间触发的伪 finished */
let lastSeekAt = 0
/** seek 前的播放态，伪 finished 时按此恢复，避免误 advance */
let statusBeforeSeek: 'playing' | 'paused' = 'playing'
/** 冷启动恢复的续播起点（秒）；play 成功后 seek 一次并清空（#49） */
let pendingResumePosition: number | null = null
/** 仅 UI 恢复、原生尚未 load 当前曲：忽略原生 idle/stopped 以免冲掉展示（#49） */
let restoredSessionUiOnly = false
/** 会话 position 节流写入 */
const SESSION_POSITION_THROTTLE_MS = 3000
let lastSessionPersistAt = 0

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
  const currentId = state.currentSong?.id
  // 有当前曲时必须 songId 精确匹配；缺 id 的陈旧事件在快速切歌时会把 UI 打成 paused 而音频仍在播（#28/#29）
  if (currentId) {
    return nativeState.currentSongId === currentId
  }
  return !nativeState.currentSongId
}

const applyNativeState = (nativeState: AudioPlayerNativeState): void => {
  if (!isCurrentNativeState(nativeState)) {
    return
  }

  // loading 切歌窗口：忽略无关 paused/stopped，避免旧 unload 把新歌 UI 冻在暂停
  if (
    state.status === 'loading'
    && (nativeState.status === 'paused' || nativeState.status === 'stopped' || nativeState.status === 'idle')
  ) {
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
    // 冷启动 getState / 陈旧 idle：不得冲掉「仅 UI 恢复」的当前曲与 session（#49）
    if (restoredSessionUiOnly) {
      return
    }
    state.currentSong = null
    state.position = 0
    state.duration = 0
    resetBufferState()
    state.lyrics = null
    state.lyricsFormat = null
    state.lyricsTranslation = null
    state.onlineLyricsStatus = 'idle'
    state.coverUri = null
    state.metadataStatus = 'idle'
    syncMediaSessionState()
    return
  }

  // playing 中进度节流写入本地会话（#49）
  if (nativeState.status === 'playing' && state.currentSong) {
    persistPlaybackSessionThrottled()
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

const persistPlaybackSessionNow = (): void => {
  const songId = state.currentSong?.id
  if (!songId) {
    return
  }
  savePlaybackSession({
    currentSongId: songId,
    position: normalizePlaybackTime(state.position),
  })
  lastSessionPersistAt = Date.now()
}

const persistPlaybackSessionThrottled = (): void => {
  if (Date.now() - lastSessionPersistAt < SESSION_POSITION_THROTTLE_MS) {
    return
  }
  persistPlaybackSessionNow()
}

/**
 * 冷启动：从 localStorage 恢复当前曲展示为 paused，不自动出声（#49）。
 * 仅在原生尚无活跃曲时生效。
 */
const restorePlaybackSessionIfNeeded = (): void => {
  if (state.currentSong || state.status === 'playing' || state.status === 'loading') {
    return
  }

  const session = loadPlaybackSession()
  if (!session) {
    return
  }

  const songs = loadSongs()
  const song = songs.find((item) => item.id === session.currentSongId) ?? null
  if (!song || findQueueIndexBySongId(song.id) < 0) {
    // 曲库删除或不在队列：丢弃会话，避免展示幽灵当前曲
    clearPlaybackSession()
    return
  }

  syncCurrentIndex(song.id)
  const duration = normalizePlaybackTime(song.duration)
  const position = duration > 0
    ? Math.min(normalizePlaybackTime(session.position), duration)
    : normalizePlaybackTime(session.position)

  state.status = 'paused'
  state.currentSong = createPlayerSongSnapshot(song)
  state.errorMessage = null
  state.position = position
  state.duration = duration
  resetBufferState()
  state.lyrics = song.lyrics || null
  state.lyricsFormat = resolveStoredLyricsFormat(song)
  state.lyricsTranslation = null
  state.onlineLyricsStatus = 'idle'
  state.coverUri = toSafeCoverUri(song.coverUri) || null
  state.metadataStatus = song.tagsScanned === true ? 'ready' : 'idle'
  // 续播起点记在 state.position；点播放时 resumePlayback 再起 native
  pendingResumePosition = position > 0 ? position : null
  restoredSessionUiOnly = true
  syncMediaSessionSong(song)
  syncMediaSessionState()
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

  // 原生无活跃播放时恢复上次会话为暂停展示（#49）
  restorePlaybackSessionIfNeeded()
}

const normalizePlaybackTime = (value: unknown): number => {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : 0
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
  // 库内词仅在「有文且质量更优」时覆盖运行时；库空绝不抹掉在线已展示词（#21）
  if (shouldApplyStoredLyricsOverRuntime(state.lyrics, state.lyricsFormat, song)) {
    state.lyrics = song.lyrics || null
    state.lyricsFormat = resolveStoredLyricsFormat(song)
  }
  state.coverUri = nextCover
  state.duration = normalizePlaybackTime(song.duration) || state.duration

  if (mediaFieldsChanged) {
    syncMediaSessionSong(song)
  }
}

/**
 * 切歌后异步匹配在线歌词：amll → 平台 → LRCLIB。
 * 成功写 playerState；按质量写回 muses:songs；token 防串曲。
 */
const matchOnlineLyricsForSong = async (song: SongItem, token: number): Promise<void> => {
  const localLyrics = song.lyrics || null
  const localFormat = resolveStoredLyricsFormat(song)
  state.onlineLyricsStatus = 'matching'

  try {
    const result = await matchOnlineLyrics({
      songId: song.id,
      title: song.title,
      artist: song.artist,
      album: song.album,
      duration: song.duration,
    })

    // 快速切歌：过期 token 或已不是当前曲，丢弃结果
    if (token !== lyricsMatchToken || state.currentSong?.id !== song.id) {
      return
    }

    if (result.ok) {
      state.lyrics = result.text
      state.lyricsFormat = result.format
      state.lyricsTranslation = result.translationText?.trim() || null
      state.onlineLyricsStatus = 'ready'

      // 按质量写回曲库（严格更优才 upsert）
      const latest = getLatestSongSnapshot(song)
      if (shouldPersistOnlineLyrics(latest, result.format, result.text)) {
        const written = upsertSong({
          sourceId: latest.sourceId,
          sourceType: latest.sourceType,
          path: latest.path,
          uri: latest.uri,
          title: latest.title,
          tags: {
            title: latest.title,
            artist: latest.artist,
            album: latest.album,
            duration: latest.duration,
            lyrics: result.text,
            lyricsSource: 'online',
            lyricsFormat: result.format,
            coverUri: latest.coverUri,
            tagsScanned: latest.tagsScanned,
            tagsScannedAt: latest.tagsScannedAt,
            metadataVersion: latest.metadataVersion,
          },
        })
        if (token === lyricsMatchToken && state.currentSong?.id === song.id) {
          // 不整表替换 snapshot，避免 upsert 新建条目时 id 与播放态不一致
          state.currentSong = {
            ...state.currentSong,
            lyrics: written.song.lyrics,
            lyricsSource: written.song.lyricsSource,
            lyricsFormat: written.song.lyricsFormat,
          }
        }
      }
      return
    }

    // 失败回退库内/本地；匹配期间标签补扫可能刚补到词
    const stateHasLyrics = !!(state.lyrics?.trim())
    const fallbackLyrics = stateHasLyrics ? state.lyrics : localLyrics
    const fallbackFormat = stateHasLyrics
      ? (state.lyricsFormat ?? localFormat)
      : localFormat
    state.lyrics = fallbackLyrics
    state.lyricsFormat = fallbackLyrics ? (fallbackFormat || 'lrc') : null
    state.lyricsTranslation = null
    state.onlineLyricsStatus = result.reason === 'network' || result.reason === 'parse' ? 'error' : 'miss'
  } catch {
    if (token !== lyricsMatchToken || state.currentSong?.id !== song.id) {
      return
    }
    const stateHasLyrics = !!(state.lyrics?.trim())
    const fallbackLyrics = stateHasLyrics ? state.lyrics : localLyrics
    const fallbackFormat = stateHasLyrics
      ? (state.lyricsFormat ?? localFormat)
      : localFormat
    state.lyrics = fallbackLyrics
    state.lyricsFormat = fallbackLyrics ? (fallbackFormat || 'lrc') : null
    state.lyricsTranslation = null
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

/** 按开关 + 曲目 ReplayGain 计算线性 volume（0.1–1.0） */
const resolvePlaybackVolume = (song: Pick<SongItem, 'replayGainTrackDb'>): number => {
  return dbToPlaybackVolume(song.replayGainTrackDb, isLoudnessNormalizeEnabled())
}

const buildPlayOptions = async (song: SongItem): Promise<PlayOptions> => {
  const volume = resolvePlaybackVolume(song)

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
      volume,
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
    volume,
  }
}

/** 对当前正在播放/暂停的曲目立即重算并应用音量（开关切换时） */
const reapplyCurrentTrackVolume = async (): Promise<void> => {
  if (state.status !== 'playing' && state.status !== 'paused') {
    return
  }
  const currentId = state.currentSong?.id
  if (!currentId) {
    return
  }
  const latest = loadSongs().find((item) => item.id === currentId)
  const volume = resolvePlaybackVolume(latest ?? { replayGainTrackDb: undefined })
  await AudioPlayerNative.setVolume(volume)
}

export const setLoudnessNormalizeEnabled = (enabled: boolean): void => {
  setLoudnessNormalizeEnabledInternal(enabled)
  void reapplyCurrentTrackVolume()
}

export { isLoudnessNormalizeEnabled }

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

/**
 * 在线补文本元信息：artist/album 仅补缺；弱 title（=文件名）可写相关在线 title。
 * 源：kw → tx → wy → kg → mg；失败静默。
 */
const matchOnlineTextMetaForSong = async (song: SongItem, token: number): Promise<void> => {
  try {
    if (token !== onlineTextToken || state.currentSong?.id !== song.id) {
      return
    }

    const latest = getLatestSongSnapshot(song)
    if (!needsOnlineTextMeta(latest)) {
      return
    }

    const remote = await matchOnlineTextMeta({
      songId: latest.id,
      title: latest.title,
      path: latest.path,
      artist: latest.artist,
      album: latest.album,
    })

    if (token !== onlineTextToken || state.currentSong?.id !== song.id) {
      return
    }
    if (!remote.ok) {
      return
    }

    const { next, changed } = mergeTextMetaFillEmpty(latest, remote.hit)
    if (!changed) {
      return
    }

    const result = upsertSong({
      sourceId: next.sourceId,
      sourceType: next.sourceType,
      path: next.path,
      uri: next.uri,
      title: next.title,
      tags: {
        title: next.title,
        artist: next.artist,
        album: next.album,
      },
    }, loadSongs())

    if (token === onlineTextToken && state.currentSong?.id === song.id) {
      syncDisplayStateFromSong(result.song)
    }
  } catch {
    // 在线文本匹配失败静默，不影响播放
  }
}

/**
 * 在线补封面：仅当当前曲仍无安全 coverUri。
 * iTunes → kw → tx → wy → kg → mg；下载到 cache/covers 后 upsert；失败静默。
 */
const matchOnlineCoverForSong = async (song: SongItem, token: number): Promise<void> => {
  try {
    if (token !== onlineCoverToken || state.currentSong?.id !== song.id) {
      return
    }
    if (toSafeCoverUri(state.coverUri || song.coverUri)) {
      return
    }

    const latest = getLatestSongSnapshot(song)
    if (toSafeCoverUri(latest.coverUri)) {
      if (token === onlineCoverToken && state.currentSong?.id === song.id) {
        syncDisplayStateFromSong(latest)
      }
      return
    }

    const remote = await matchOnlineCoverRemote({
      songId: latest.id,
      title: latest.title,
      artist: latest.artist,
      album: latest.album,
    })

    if (token !== onlineCoverToken || state.currentSong?.id !== song.id) {
      return
    }
    if (!remote.ok) {
      return
    }

    const localUri = await cacheRemoteCover({
      url: remote.remoteUrl,
      cacheKey: `online:${latest.id}`,
    })
    const safeUri = toSafeCoverUri(localUri || undefined)
    if (!safeUri) {
      return
    }
    if (token !== onlineCoverToken || state.currentSong?.id !== song.id) {
      return
    }

    const result = upsertSong({
      sourceId: latest.sourceId,
      sourceType: latest.sourceType,
      path: latest.path,
      uri: latest.uri,
      title: latest.title,
      tags: {
        coverUri: safeUri,
      },
    }, loadSongs())

    if (token === onlineCoverToken && state.currentSong?.id === song.id) {
      syncDisplayStateFromSong(result.song)
    }
  } catch {
    // 在线封面失败静默，不影响播放
  }
}

const scanSongMetadata = async (song: SongItem): Promise<void> => {
  const coverToken = ++onlineCoverToken
  const textToken = ++onlineTextToken

  if (!shouldRefreshMetadata(song)) {
    syncDisplayStateFromSong(song)
    state.metadataStatus = 'ready'
    // 本地已扫描但仍可能缺封面/文本 → 在线补
    void matchOnlineCoverForSong(song, coverToken)
    void matchOnlineTextMetaForSong(song, textToken)
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
    // 懒扫补到 ReplayGain 后立即重设当前曲音量，避免首播仍用 1.0
    if (
      (state.status === 'playing' || state.status === 'paused')
      && state.currentSong?.id === result.song.id
    ) {
      void AudioPlayerNative.setVolume(resolvePlaybackVolume(result.song))
    }
    void matchOnlineCoverForSong(result.song, coverToken)
    void matchOnlineTextMetaForSong(result.song, textToken)
  } catch {
    if (token === metadataScanToken && state.currentSong?.id === song.id) {
      state.metadataStatus = 'failed'
      // 本地扫描失败仍尝试在线补封面/文本（仅补缺）
      void matchOnlineCoverForSong(song, coverToken)
      void matchOnlineTextMetaForSong(song, textToken)
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

interface PlaybackRecoveryContext {
  attemptedSongIds: Set<string>
}

const playSongInternal = async (
  song: SongItem,
  recoveryContext?: PlaybackRecoveryContext,
): Promise<void> => {
  const latestSong = getLatestSongSnapshot(song)

  syncCurrentIndex(latestSong.id)
  // 切歌清理 seek 保护，避免新歌首帧误吞真实 finished
  clearSeekGuard()
  // 一旦发起原生 play，不再处于「仅 UI 会话」
  restoredSessionUiOnly = false

  const generation = ++playGeneration
  state.status = 'loading'
  metadataScanToken += 1
  const matchToken = ++lyricsMatchToken
  // 切歌即作废进行中的在线封面/文本匹配，避免上一首结果串到新曲
  onlineCoverToken += 1
  onlineTextToken += 1
  state.currentSong = createPlayerSongSnapshot(latestSong)
  state.errorMessage = null
  // 续播时保留待恢复进度展示，避免 play 前 UI 闪回 0（#49）
  state.position = pendingResumePosition != null && pendingResumePosition > 0
    ? pendingResumePosition
    : 0
  state.duration = normalizePlaybackTime(latestSong.duration)
  // 切歌先清缓冲，禁止继承上一首（R7）；本地 full / 远程增长由 native 再写入
  resetBufferState()
  // 先展示库内歌词（含 format），再异步在线匹配（可按质量升级写回）
  state.lyrics = latestSong.lyrics || null
  state.lyricsFormat = resolveStoredLyricsFormat(latestSong)
  state.lyricsTranslation = null
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
    // 快速连切：被 supersede 的 play 不得回写 status（#28/#29）
    if (generation !== playGeneration || state.currentSong?.id !== latestSong.id) {
      return
    }
    state.status = 'playing'
    // 冷启动续播：play 默认从 0，成功后 seek 到恢复进度（#49）
    if (
      pendingResumePosition != null
      && pendingResumePosition > 0
      && state.currentSong?.id === latestSong.id
    ) {
      const resumeAt = pendingResumePosition
      pendingResumePosition = null
      try {
        await AudioPlayerNative.seek({ position: resumeAt })
        if (generation === playGeneration && state.currentSong?.id === latestSong.id) {
          state.position = resumeAt
          lastSeekAt = Date.now()
        }
      } catch {
        // seek 失败仍保持已起播，从 0 附近继续
      }
    } else {
      pendingResumePosition = null
    }
    persistPlaybackSessionNow()
    void scanSongMetadata(latestSong)
    // 播放成功后后台预取下一首 WebDAV（失败静默）
    void prefetchNextTrack(latestSong.id)
  } catch (error) {
    if (generation !== playGeneration || state.currentSong?.id !== latestSong.id) {
      return
    }
    lyricsMatchToken += 1
    onlineCoverToken += 1
    onlineTextToken += 1
    state.onlineLyricsStatus = 'idle'
    const message = error instanceof Error ? error.message : ''
    setUserSafeError(isSafePlaybackError(message) ? message : '播放失败，请稍后重试。')
    resetBufferState()

    const activeRecovery = recoveryContext ?? { attemptedSongIds: new Set<string>() }
    activeRecovery.attemptedSongIds.add(latestSong.id)
    const nextSong = selectRecoveryCandidate(activeRecovery.attemptedSongIds)
    if (nextSong) {
      // 继续恢复时不清媒体会话，避免异步 clear 覆盖下一首刚写入的 metadata。
      await playSongInternal(nextSong, activeRecovery)
      return
    }

    // loading 会乐观映射为 playing；仅恢复链终止时清掉媒体会话。
    void clearMediaSession().catch(() => undefined)
  }
}

export const playSong = async (song: SongItem): Promise<void> => {
  // 用户主动点播新曲：不继承冷启动续播点
  if (state.currentSong?.id !== song.id) {
    pendingResumePosition = null
  }
  await playSongInternal(song)
}

export const pausePlayback = async (): Promise<void> => {
  try {
    await AudioPlayerNative.pause()
    state.status = 'paused'
    state.errorMessage = null
    persistPlaybackSessionNow()
  } catch {
    setUserSafeError('暂停失败，请稍后重试。')
  }
}

export const resumePlayback = async (): Promise<void> => {
  // 冷启动仅 UI 恢复、原生尚未 load 时，resume 无 asset：改为 play + seek（#49）
  if (state.currentSong && (state.status === 'paused' || state.status === 'stopped' || state.status === 'idle')) {
    const song = loadSongs().find((item) => item.id === state.currentSong?.id)
    if (song) {
      if (pendingResumePosition == null && state.position > 0) {
        pendingResumePosition = state.position
      }
      await playSongInternal(song)
      return
    }
  }

  try {
    await AudioPlayerNative.resume()
    state.status = 'playing'
    state.errorMessage = null
    persistPlaybackSessionNow()
  } catch {
    // resume 失败时若有当前曲，回退 play 路径
    const song = state.currentSong
      ? loadSongs().find((item) => item.id === state.currentSong?.id)
      : null
    if (song) {
      if (pendingResumePosition == null && state.position > 0) {
        pendingResumePosition = state.position
      }
      await playSongInternal(song)
      return
    }
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
    // 仅 UI 恢复、原生未起播时：只更新本地进度与会话，待用户播放时 seek（#49）
    if (restoredSessionUiOnly) {
      state.position = safePosition
      pendingResumePosition = safePosition > 0 ? safePosition : null
      persistPlaybackSessionNow()
      return true
    }
    await AudioPlayerNative.seek({ position: safePosition })
    state.position = safePosition
    state.errorMessage = null
    // seek 成功后开启短保护窗，吞掉未缓冲区间触发的伪 finished
    lastSeekAt = Date.now()
    persistPlaybackSessionNow()
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
    pendingResumePosition = null
    restoredSessionUiOnly = false
    clearPlaybackSession()
    metadataScanToken += 1
    lyricsMatchToken += 1
    onlineCoverToken += 1
    onlineTextToken += 1
    state.status = 'stopped'
    state.currentSong = null
    state.errorMessage = null
    state.position = 0
    state.duration = 0
    resetBufferState()
    state.lyrics = null
    state.lyricsFormat = null
    state.lyricsTranslation = null
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
