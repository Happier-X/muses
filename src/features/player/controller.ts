import { computed, readonly, ref } from 'vue'
import type { SongItem } from '@/features/library/types'
import {
  CURRENT_METADATA_VERSION,
  loadSongs,
  updateSongUserEdit,
  upsertSong,
  type SongUserEditPatch,
} from '@/features/library/storage'
import { writeLocalAudioMetadata } from '@/features/library/native'
import { readLocalAudioTags, readWebDavAudioTags } from '@/features/library/tags'
import { getWebDavPassword, loadSources } from '@/features/sources/storage'
import { writeWebDavAudioMetadata } from '@/features/sources/webdav'
import type { WebDavSourceItem } from '@/features/sources/types'
import { recordRecentPlay } from './recent'
import { AudioPlayerNative, prefetchWebDavAudioFile } from './native'
import { setupNativeQueueSyncListener, syncQueueToNative } from './nativeQueueSync'
import { dbToPlaybackVolume } from './loudness'
import type { AudioPlayerNativeState, PlaybackStatus, PlayOptions, PlayerState } from './types'
import {
  createPlayerSongSnapshot,
  resolveStoredLyricsFormat,
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
  onQueueChanged,
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
import { startKeepAlive, stopKeepAlive } from './keepalive'

const state = ref<PlayerState>({
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
/** playSong 代际：快速连切时仅最新一代可写 playing/error */
let playGeneration = 0
/** 用户主动 seek 后的时间戳；用于忽略 seek 到未缓冲区间触发的伪 finished */
let lastSeekAt = 0
/** seek 前的播放态，伪 finished 时按此恢复，避免误 advance */
let statusBeforeSeek: 'playing' | 'paused' = 'playing'
/** 冷启动恢复的续播起点（秒）；play 成功后 seek 一次并清空（#49） */
let pendingResumePosition: number | null = null
/**
 * 冷启动恢复 seek 保护（#53）：续播 play 已发起但恢复 seek 尚未完成时，
 * 原生可能先上报同曲 playing + position: 0（或明显早于恢复点的初始位置），
 * 此期间保留恢复位置为权威 UI 位置，避免进度条闪回 0 再跳回。
 */
let resumeSeekGuardSongId: string | null = null
let resumeSeekGuardPosition: number | null = null
/** 仅 UI 恢复、原生尚未 load 当前曲：忽略原生 idle/stopped 以免冲掉展示（#49） */
let restoredSessionUiOnly = false
/** 仅显式 stop 路径允许 native idle/stopped 清空 currentSong（#52） */
let allowNativeClearCurrentSong = false
/** 会话 position 节流写入 */
const SESSION_POSITION_THROTTLE_MS = 3000
let lastSessionPersistAt = 0

/** 自动切歌/对账防重入：心跳与回前台对账可能并发触发 handlePlaybackFinished */
let advanceInFlight = false

/** 后台心跳间隔；hidden 时浏览器会节流到约 1 次/分钟，作为原生兜底失败的最后防线 */
const AUTO_NEXT_HEARTBEAT_MS = 1000
let autoNextHeartbeatTimer: ReturnType<typeof setInterval> | null = null

const LOCAL_METADATA_SCAN_TIMEOUT_MS = 15_000
const WEBDAV_METADATA_SCAN_TIMEOUT_MS = 120_000
/** seek 后短保护窗：此期间 finished 一律视为伪结束，不自动切歌 */
const SEEK_FINISH_GUARD_MS = 1500
/** 冷启动恢复窗内，早于恢复点这一容差以上的同曲原生 position 视为启动初始位置并屏蔽（#53） */
const RESUME_SEEK_GUARD_EPSILON_SEC = 1

const clearResumeSeekGuard = (): void => {
  resumeSeekGuardSongId = null
  resumeSeekGuardPosition = null
}

const clearSeekGuard = (): void => {
  lastSeekAt = 0
}

const isWithinSeekGuard = (): boolean => {
  return lastSeekAt > 0 && Date.now() - lastSeekAt < SEEK_FINISH_GUARD_MS
}

const shouldGuardResumeSeekPosition = (songId: string | undefined, position: number): boolean => {
  return resumeSeekGuardPosition != null
    && resumeSeekGuardSongId === songId
    && position < resumeSeekGuardPosition - RESUME_SEEK_GUARD_EPSILON_SEC
}

const setUserSafeError = (message: string) => {
  state.value.status = 'error'
  state.value.errorMessage = message
}

const isCurrentNativeState = (nativeState: AudioPlayerNativeState): boolean => {
  const currentId = state.value.currentSong?.id
  // 有当前曲时必须 songId 精确匹配；缺 id 的陈旧事件在快速切歌时会把 UI 打成 paused 而音频仍在播（#28/#29）
  if (currentId) {
    return nativeState.currentSongId === currentId
  }
  return !nativeState.currentSongId
}

const applyNativeState = (nativeState: AudioPlayerNativeState): void => {
  if (!isCurrentNativeState(nativeState)) {
    // 原生队列自治：后台已切到新歌（JS冻结期间），前端收到 playing 新歌事件需同步UI
    if (
      nativeState.status === 'playing'
      && nativeState.currentSongId
      && state.value.currentSong
      && nativeState.currentSongId !== state.value.currentSong.id
    ) {
      void syncUiToNativeSong(nativeState.currentSongId)
    }
    return
  }

  // loading 切歌窗口：忽略无关 paused/stopped，避免旧 unload 把新歌 UI 冻在暂停
  if (
    state.value.status === 'loading'
    && (nativeState.status === 'paused' || nativeState.status === 'stopped' || nativeState.status === 'idle')
  ) {
    return
  }

  // finished 处理：complete/STATE_ENDED 的唯一合法来源是播放器真正播完，
  // 不再依赖 position 做「接近结尾」佐证——JS 冻结期间 position 滞后会误判为伪结束而不切歌
  //（08-18-carwith-bg-ctrl-fix）。仅保留 seek 保护窗：seek 到未缓冲区间时插件可能伪报 finished。
  if (nativeState.status === 'finished') {
    const nativePosition = normalizePlaybackTime(nativeState.position)
    const nativeDuration = normalizePlaybackTime(nativeState.duration) || state.value.duration
    if (shouldGuardResumeSeekPosition(nativeState.currentSongId, nativePosition)) {
      state.value.duration = nativeDuration
      state.value.status = state.value.status === 'loading' ? 'playing' : state.value.status
      state.value.errorMessage = null
      syncMediaSessionState()
      return
    }
    const effectiveDuration = nativeDuration

    // 保护窗内：视为 seek 触发的伪 finished，保留 seek 目标进度并恢复先前状态。
    if (isWithinSeekGuard()) {
      state.value.duration = effectiveDuration
      state.value.status = statusBeforeSeek
      state.value.errorMessage = null
      syncMediaSessionState()
      return
    }

    // 窗口外：complete 即自然播完，无条件切下一首。展示位置取 native/state 较大值，
    // 避免 complete 事件 position 回 0 时进度条闪回。
    state.value.status = 'finished'
    state.value.errorMessage = null
    state.value.position = Math.max(nativePosition, state.value.position)
    state.value.duration = effectiveDuration
    syncMediaSessionState()
    void handlePlaybackFinished()
    return
  }

  if (nativeState.status === 'idle' || nativeState.status === 'stopped') {
    // 冷启动 getState / 陈旧 idle：不得冲掉「仅 UI 恢复」的当前曲与 session（#49）
    if (restoredSessionUiOnly) {
      return
    }
    // 非显式 stop：整段忽略 unload/重载 stopped，避免 status 被冲成 stopped 且清空当前曲（#52）
    if (!allowNativeClearCurrentSong && state.value.currentSong) {
      return
    }
    allowNativeClearCurrentSong = false
    state.value.status = nativeState.status
    state.value.errorMessage = null
    state.value.currentSong = null
    state.value.position = 0
    state.value.duration = 0
    resetBufferState()
    state.value.lyrics = null
    state.value.lyricsFormat = null
    state.value.lyricsTranslation = null
    state.value.onlineLyricsStatus = 'idle'
    state.value.coverUri = null
    state.value.metadataStatus = 'idle'
    syncMediaSessionState()
    return
  }

  state.value.status = nativeState.status
  state.value.errorMessage = nativeState.status === 'error' ? nativeState.errorMessage || '播放失败，请稍后重试。' : null
  const nextPosition = normalizePlaybackTime(nativeState.position)
  // 冷启动恢复 seek 完成前：屏蔽同曲明显早于恢复点的原生初始 position（#53）
  if (resumeSeekGuardPosition != null && resumeSeekGuardSongId === nativeState.currentSongId) {
    if (shouldGuardResumeSeekPosition(nativeState.currentSongId, nextPosition)) {
      // 保留恢复位置为权威 UI 位置，其余字段仍按下方规则更新
    } else {
      // 已到达恢复点附近：seek 生效，结束保护并恢复正常原生 position 同步
      clearResumeSeekGuard()
      state.value.position = nextPosition
    }
  } else {
    state.value.position = nextPosition
  }
  // duration 未就绪（native prepare 窗口上报 0）时保留已有值，
  // 避免续播 position 相对 max=1 瞬间显示 100%（进度条爆炸后回跳）
  const nativeDuration = normalizePlaybackTime(nativeState.duration)
  state.value.duration = nativeDuration > 0 ? nativeDuration : state.value.duration

  // 缓冲：原生上报时单调合并；error 在下方 reset，禁止串曲
  if (
    nativeState.bufferedPosition !== undefined
    && nativeState.status !== 'error'
  ) {
    const nextBuffered = normalizeBufferedPosition(nativeState.bufferedPosition)
    if (nextBuffered != null) {
      const capped = state.value.duration > 0 ? Math.min(nextBuffered, state.value.duration) : nextBuffered
      state.value.bufferedPosition = Math.max(state.value.bufferedPosition ?? 0, capped)
    }
  }

  // playing 中进度节流写入本地会话（#49）
  if (nativeState.status === 'playing' && state.value.currentSong) {
    persistPlaybackSessionThrottled()
  }

  if (nativeState.status === 'error') {
    clearResumeSeekGuard()
    resetBufferState()
  }

  // duration 晚到：把已有缓冲压回 duration
  if (state.value.duration > 0 && state.value.bufferedPosition != null && state.value.bufferedPosition > state.value.duration) {
    state.value.bufferedPosition = state.value.duration
  }

  syncMediaSessionState()
}

const handlePlaybackFinished = async (): Promise<void> => {
  // 原生自治已切歌去重：后台 STATE_ENDED 由原生直接 advance+play，JS冻结时不会执行到此；
  // 前台时原生也会自治切歌，JS需检测原生已切则跳过重复 advance，避免跳过一首。
  try {
    const nativeState = await AudioPlayerNative.getState()
    if (nativeState.currentSongId && nativeState.currentSongId !== state.value.currentSong?.id) {
      const idx = findQueueIndexBySongId(nativeState.currentSongId)
      if (idx >= 0) {
        syncCurrentIndex(nativeState.currentSongId)
        const song = loadSongs().find((s) => s.id === nativeState.currentSongId)
        if (song) {
          await syncUiToNativeSong(nativeState.currentSongId)
        }
      }
      void syncQueueToNative()
      return
    }
  } catch {
    // getState 失败则按原逻辑推进
  }
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
  const songId = state.value.currentSong?.id
  if (!songId) {
    return
  }
  savePlaybackSession({
    currentSongId: songId,
    position: normalizePlaybackTime(state.value.position),
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
  if (state.value.currentSong || state.value.status === 'playing' || state.value.status === 'loading') {
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

  state.value.status = 'paused'
  state.value.currentSong = createPlayerSongSnapshot(song)
  state.value.errorMessage = null
  state.value.position = position
  state.value.duration = duration
  resetBufferState()
  state.value.lyrics = song.lyrics || null
  state.value.lyricsFormat = resolveStoredLyricsFormat(song)
  state.value.lyricsTranslation = null
  // 本地来源化：歌词仅来自内嵌/sidecar，有词 ready / 无词 miss，不再发起在线匹配
  state.value.onlineLyricsStatus = song.lyrics?.trim() ? 'ready' : 'miss'
  state.value.coverUri = toSafeCoverUri(song.coverUri) || null
  state.value.metadataStatus = song.tagsScanned === true ? 'ready' : 'idle'
  // 续播起点记在 state.value.position；点播放时 resumePlayback 再起 native
  pendingResumePosition = position > 0 ? position : null
  restoredSessionUiOnly = true
  syncMediaSessionSong(song)
  syncMediaSessionState()
}

/**
 * 同步 UI 到原生已在播的曲目（原生自治切歌后 JS 恢复时调用）。
 * 走 playSongInternal 的 nativeAlreadyPlaying 模式：不重复调原生 play。
 */
async function syncUiToNativeSong(songId: string): Promise<void> {
  if (!songId || songId === state.value.currentSong?.id) {
    return
  }
  const song = loadSongs().find((item) => item.id === songId)
  if (!song) {
    return
  }
  recordRecentPlay(song)
  pendingResumePosition = null
  clearResumeSeekGuard()
  await playSongInternal(song, undefined, { nativeAlreadyPlaying: true })
}

/**
 * 后台心跳兜底（方案 B）：hidden 且应播放时检查原生状态，
 * complete 事件丢失时也能在浏览器节流窗口（约 1 次/分钟）内补切歌。
 */
const runBackgroundHeartbeat = async (): Promise<void> => {
  if (!document.hidden || state.value.status !== 'playing' || advanceInFlight) {
    return
  }
  try {
    const nativeState = await AudioPlayerNative.getState()
    if (nativeState.status !== 'playing') {
      advanceInFlight = true
      try {
        await handlePlaybackFinished()
      } finally {
        advanceInFlight = false
      }
    }
  } catch {
    // 心跳失败静默，不打断播放
  }
}

const startAutoNextHeartbeat = (): void => {
  if (autoNextHeartbeatTimer) {
    return
  }
  autoNextHeartbeatTimer = setInterval(() => {
    void runBackgroundHeartbeat()
  }, AUTO_NEXT_HEARTBEAT_MS)
}

/**
 * 回前台对账：
 * 1. 原生已在播新曲（预案成功但事件丢失）→ 同步 UI；
 * 2. 原生已停止而 JS 仍以为在播（complete 丢失）→ 立即切歌。
 */
export const reconcileAfterBackground = async (): Promise<void> => {
  try {
    const nativeState = await AudioPlayerNative.getState()
    if (nativeState.status === 'playing' && nativeState.currentSongId && nativeState.currentSongId !== state.value.currentSong?.id) {
      await syncUiToNativeSong(nativeState.currentSongId)
      return
    }
    if (advanceInFlight) {
      return
    }
    if (nativeState.status !== 'playing' && (state.value.status === 'playing' || state.value.status === 'finished')) {
      advanceInFlight = true
      try {
        await handlePlaybackFinished()
      } finally {
        advanceInFlight = false
      }
    }
  } catch {
    // 对账失败静默
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

  // 原生队列自治：监听 requestUrls 补窗，并同步队列窗口
  await setupNativeQueueSyncListener()

  // 队列/模式变化后同步原生窗口 + 重调度预取
  onQueueChanged(() => {
    void syncQueueToNative()
    reschedulePrefetchAfterQueueChange()
  })

  // 后台心跳：complete 事件丢失时兜底补切歌
  startAutoNextHeartbeat()

  try {
    applyNativeState(await AudioPlayerNative.getState())
  } catch {
    // 非 Android 或原生插件尚不可用时，保持空闲状态，用户点击播放时再显示明确错误。
  }

  // 原生无活跃播放时恢复上次会话为暂停展示（#49）
  restorePlaybackSessionIfNeeded()

  // 初始化同步一次窗口到原生，确保后台自治可用
  void syncQueueToNative()
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
  if (state.value.duration > 0 && state.value.bufferedPosition != null && state.value.bufferedPosition >= 0) {
    return Math.min(state.value.duration, state.value.bufferedPosition)
  }
  if (state.value.duration > 0) {
    return state.value.duration
  }
  if (state.value.bufferedPosition != null && state.value.bufferedPosition >= 0) {
    return state.value.bufferedPosition
  }
  return Number.POSITIVE_INFINITY
}

const resetBufferState = (): void => {
  state.value.bufferedPosition = null
}

/** media-session position 节流：避免进度 tick 每次跨 bridge（#50） */
const MEDIA_POSITION_THROTTLE_MS = 1000
let lastMediaPositionSyncAt = 0
let lastMediaSyncedStatus: PlaybackStatus | null = null

const syncMediaSessionState = (): void => {
  if (!state.value.currentSong) {
    lastMediaSyncedStatus = null
    lastMediaPositionSyncAt = 0
    void clearMediaSession().catch(() => undefined)
    return
  }

  const statusChanged = lastMediaSyncedStatus !== state.value.status
  if (statusChanged) {
    lastMediaSyncedStatus = state.value.status
    void updateMediaSessionPlayback(state.value.status).catch(() => undefined)
  }

  const now = Date.now()
  const shouldSyncPosition =
    statusChanged
    || now - lastMediaPositionSyncAt >= MEDIA_POSITION_THROTTLE_MS
  if (shouldSyncPosition) {
    lastMediaPositionSyncAt = now
    void updateMediaSessionPosition(state.value.position, state.value.duration).catch(() => undefined)
  }
}

const syncMediaSessionSong = (song: SongItem): void => {
  void updateMediaSessionMetadata({
    title: song.title,
    artist: song.artist,
    album: song.album,
    coverUri: toSafeCoverUri(song.coverUri),
  }).catch(() => undefined)
  void updateMediaSessionPosition(normalizePlaybackTime(state.value.position), normalizePlaybackTime(song.duration) || state.value.duration).catch(() => undefined)
  void updateMediaSessionPlayback(state.value.status).catch(() => undefined)
}

const syncDisplayStateFromSong = (song: SongItem, options?: { forceLyrics?: boolean }): void => {
  if (state.value.currentSong?.id !== song.id) {
    return
  }

  const previous = state.value.currentSong
  const nextCover = toSafeCoverUri(song.coverUri) || null
  // 懒扫描补全封面/标签后必须 re-sync 媒体会话，否则通知栏永远拿不到新封面。
  const mediaFieldsChanged =
    previous.title !== song.title
    || previous.artist !== song.artist
    || previous.album !== song.album
    || state.value.coverUri !== nextCover

  state.value.currentSong = createPlayerSongSnapshot(song)
  // 用户手改歌词：强制用库内值替换运行时（含清空）
  if (options?.forceLyrics) {
    state.value.lyrics = song.lyrics?.trim() ? song.lyrics : null
    state.value.lyricsFormat = resolveStoredLyricsFormat(song)
    state.value.lyricsTranslation = null
  } else if (song.lyrics?.trim()) {
    // 本地词是唯一来源：库内有词即采用（懒扫描补词场景）；库内无词不覆盖运行时
    state.value.lyrics = song.lyrics
    state.value.lyricsFormat = resolveStoredLyricsFormat(song)
  }
  state.value.coverUri = nextCover
  state.value.duration = normalizePlaybackTime(song.duration) || state.value.duration

  if (mediaFieldsChanged || options?.forceLyrics) {
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
  if (state.value.status !== 'playing' && state.value.status !== 'paused') {
    return
  }
  const currentId = state.value.currentSong?.id
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

const scanSongMetadata = async (song: SongItem): Promise<void> => {
  if (!shouldRefreshMetadata(song)) {
    syncDisplayStateFromSong(song)
    state.value.metadataStatus = 'ready'
    return
  }

  const token = ++metadataScanToken
  state.value.metadataStatus = 'scanning'

  try {
    const timeoutMs = song.sourceType === 'webdav' ? WEBDAV_METADATA_SCAN_TIMEOUT_MS : LOCAL_METADATA_SCAN_TIMEOUT_MS
    const tags = await withMetadataScanTimeout(song.sourceType === 'local'
      ? readLocalAudioTags(buildAudioFileEntry(song), song.id)
      : readWebDavAudioTags(getWebDavSource(song), buildAudioFileEntry(song), await requireWebDavPassword(song)), timeoutMs)

    if (token !== metadataScanToken || state.value.currentSong?.id !== song.id) {
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
    state.value.metadataStatus = 'ready'
    // 懒扫补到 ReplayGain 后立即重设当前曲音量，避免首播仍用 1.0
    if (
      (state.value.status === 'playing' || state.value.status === 'paused')
      && state.value.currentSong?.id === result.song.id
    ) {
      void AudioPlayerNative.setVolume(resolvePlaybackVolume(result.song))
    }
  } catch {
    if (token === metadataScanToken && state.value.currentSong?.id === song.id) {
      state.value.metadataStatus = 'failed'
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
 * 当前曲进入 playing 后调度下一首 WebDAV 音频完整预取。
 * 跳过：空队列 / 单曲循环自身 / 本地 / 非 webdav。
 * 密码仅传到 bridge；失败静默，不阻塞播放。
 */
const prefetchNextTrack = async (currentSongId: string): Promise<void> => {
  try {
    const next = peekNext()
    if (!next || next.id === currentSongId || next.sourceType !== 'webdav') {
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
  const currentId = state.value.currentSong?.id
  if (!currentId) {
    return
  }
  if (state.value.status !== 'playing' && state.value.status !== 'paused') {
    return
  }
  void prefetchNextTrack(currentId)
}

export const enqueueSongs = (songs: SongItem[]): void => {
  enqueueSongsInternal(songs)
  void syncQueueToNative()
  reschedulePrefetchAfterQueueChange()
}

export const enqueueSong = (song: SongItem): void => {
  enqueueSongInternal(song)
  void syncQueueToNative()
  reschedulePrefetchAfterQueueChange()
}

export const removeSongFromQueue = (songId: string): void => {
  removeSongFromQueueInternal(songId)
  void syncQueueToNative()
  reschedulePrefetchAfterQueueChange()
}

export const clearQueue = (): void => {
  clearQueueInternal()
  void syncQueueToNative()
  reschedulePrefetchAfterQueueChange()
}

export const setRepeatMode = (mode: RepeatMode): void => {
  setRepeatModeInternal(mode)
  void syncQueueToNative()
  reschedulePrefetchAfterQueueChange()
}

export const toggleShuffle = (): void => {
  toggleShuffleInternal()
  void syncQueueToNative()
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
  options?: { nativeAlreadyPlaying?: boolean },
): Promise<void> => {
  const nativeAlreadyPlaying = options?.nativeAlreadyPlaying === true
  const latestSong = getLatestSongSnapshot(song)

  syncCurrentIndex(latestSong.id)
  // 切歌清理 seek 保护，避免新歌首帧误吞真实 finished
  clearSeekGuard()
  // 一旦发起原生 play，不再处于「仅 UI 会话」
  restoredSessionUiOnly = false

  // 冷启动续播：play 前登记恢复 seek 保护，屏蔽启动阶段同曲早于恢复点的原生 position（#53）
  if (pendingResumePosition != null && pendingResumePosition > 0) {
    resumeSeekGuardSongId = latestSong.id
    resumeSeekGuardPosition = pendingResumePosition
  } else {
    clearResumeSeekGuard()
  }

  const generation = ++playGeneration
  state.value.status = 'loading'
  metadataScanToken += 1
  state.value.currentSong = createPlayerSongSnapshot(latestSong)
  state.value.errorMessage = null
  // 续播时保留待恢复进度展示，避免 play 前 UI 闪回 0（#49）
  state.value.position = pendingResumePosition != null && pendingResumePosition > 0
    ? pendingResumePosition
    : 0
  state.value.duration = normalizePlaybackTime(latestSong.duration)
  // 切歌先清缓冲，禁止继承上一首（R7）；本地 full / 远程增长由 native 再写入
  resetBufferState()
  // 歌词仅取库内本地来源（内嵌/sidecar）：有词 ready / 无词 miss，不再发起在线匹配
  state.value.lyrics = latestSong.lyrics || null
  state.value.lyricsFormat = resolveStoredLyricsFormat(latestSong)
  state.value.lyricsTranslation = null
  state.value.onlineLyricsStatus = latestSong.lyrics?.trim() ? 'ready' : 'miss'
  state.value.coverUri = toSafeCoverUri(latestSong.coverUri) || null
  state.value.metadataStatus = latestSong.tagsScanned === true ? 'ready' : 'idle'
  syncMediaSessionSong(latestSong)

  try {
    if (!nativeAlreadyPlaying) {
      try {
        await AudioPlayerNative.ensureNotificationPermission()
      } catch {
        // 权限请求失败（非 Android / 插件不可用）静默忽略，不阻塞播放。
      }
      await AudioPlayerNative.play(await buildPlayOptions(latestSong))
    }
    // 快速连切：被 supersede 的 play 不得回写 status（#28/#29）
    if (generation !== playGeneration || state.value.currentSong?.id !== latestSong.id) {
      return
    }
    state.value.status = 'playing'
    startKeepAlive()
    if (nativeAlreadyPlaying) {
      // 原生已兜底起播：无续播点、无 seek 恢复
      pendingResumePosition = null
      clearResumeSeekGuard()
    } else if (
      pendingResumePosition != null
      && pendingResumePosition > 0
      && state.value.currentSong?.id === latestSong.id
    ) {
      const resumeAt = pendingResumePosition
      pendingResumePosition = null
      try {
        await AudioPlayerNative.seek({ position: resumeAt })
        if (generation === playGeneration && state.value.currentSong?.id === latestSong.id) {
          state.value.position = resumeAt
          lastSeekAt = Date.now()
        }
      } catch {
        // seek 失败仍保持已起播，从原生实际位置继续
      } finally {
        // 恢复 seek 成功或失败都结束保护窗，让后续原生 position 正常驱动 UI（#53）
        clearResumeSeekGuard()
      }
    } else {
      pendingResumePosition = null
      clearResumeSeekGuard()
    }
    persistPlaybackSessionNow()
    void scanSongMetadata(latestSong)
    // 播放成功后后台预取下一首 WebDAV（失败静默）
    void prefetchNextTrack(latestSong.id)
    // 同步原生队列窗口：原生自治切歌依赖此窗口
    void syncQueueToNative()
  } catch (error) {
    if (generation !== playGeneration || state.value.currentSong?.id !== latestSong.id) {
      return
    }
    state.value.onlineLyricsStatus = 'idle'
    // 播放失败：结束恢复 seek 保护且不让自动跳过的下一曲继承旧恢复点（#53）
    pendingResumePosition = null
    clearResumeSeekGuard()
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

    // 恢复链终止：通知原生窗口为空，避免兜底播放不存在的下一首
    void syncQueueToNative().catch(() => undefined)
    // loading 会乐观映射为 playing；仅恢复链终止时清掉媒体会话。
    stopKeepAlive()
    void clearMediaSession().catch(() => undefined)
  }
}

export const playSong = async (song: SongItem): Promise<void> => {
  recordRecentPlay(song)
  // 用户主动点播新曲：不继承冷启动续播点
  if (state.value.currentSong?.id !== song.id) {
    pendingResumePosition = null
    clearResumeSeekGuard()
  }
  await playSongInternal(song)
}

export const pausePlayback = async (): Promise<void> => {
  try {
    await AudioPlayerNative.pause()
    state.value.status = 'paused'
    state.value.errorMessage = null
    persistPlaybackSessionNow()
    stopKeepAlive()
  } catch {
    setUserSafeError('暂停失败，请稍后重试。')
  }
}

export const resumePlayback = async (): Promise<void> => {
  // 仅冷启动 UI 会话（原生无 asset）才整曲 play + seek（#49）；普通 pause 后走 resume（#52）
  if (
    restoredSessionUiOnly
    && state.value.currentSong
    && (state.value.status === 'paused' || state.value.status === 'stopped' || state.value.status === 'idle')
  ) {
    const song = loadSongs().find((item) => item.id === state.value.currentSong?.id)
    if (song) {
      if (pendingResumePosition == null && state.value.position > 0) {
        pendingResumePosition = state.value.position
      }
      await playSongInternal(song)
      return
    }
  }

  try {
    await AudioPlayerNative.resume()
    state.value.status = 'playing'
    state.value.errorMessage = null
    persistPlaybackSessionNow()
    startKeepAlive()
  } catch {
    // resume 失败时若有当前曲，回退 play 路径
    const song = state.value.currentSong
      ? loadSongs().find((item) => item.id === state.value.currentSong?.id)
      : null
    if (song) {
      if (pendingResumePosition == null && state.value.position > 0) {
        pendingResumePosition = state.value.position
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
  if (state.value.bufferedPosition != null && Number.isFinite(maxSeekable) && requested > maxSeekable + 0.05) {
    return false
  }

  const safePosition = Number.isFinite(maxSeekable)
    ? Math.min(requested, maxSeekable)
    : requested

  try {
    statusBeforeSeek = state.value.status === 'paused' ? 'paused' : 'playing'
    // 仅 UI 恢复、原生未起播时：只更新本地进度与会话，待用户播放时 seek（#49）
    if (restoredSessionUiOnly) {
      state.value.position = safePosition
      pendingResumePosition = safePosition > 0 ? safePosition : null
      persistPlaybackSessionNow()
      return true
    }
    // 普通 seek 优先于冷启动自动恢复；成功或失败都不得遗留恢复保护（#53）
    clearResumeSeekGuard()
    await AudioPlayerNative.seek({ position: safePosition })
    state.value.position = safePosition
    state.value.errorMessage = null
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
    // 显式 stop：允许随后 native stopped 事件清空（自身也会同步清空）
    allowNativeClearCurrentSong = true
    await AudioPlayerNative.stop()
    void syncQueueToNative().catch(() => undefined)
    clearSeekGuard()
    pendingResumePosition = null
    clearResumeSeekGuard()
    restoredSessionUiOnly = false
    clearPlaybackSession()
    metadataScanToken += 1
    state.value.status = 'stopped'
    state.value.currentSong = null
    state.value.errorMessage = null
    state.value.position = 0
    state.value.duration = 0
    resetBufferState()
    state.value.lyrics = null
    state.value.lyricsFormat = null
    state.value.lyricsTranslation = null
    state.value.onlineLyricsStatus = 'idle'
    state.value.coverUri = null
    state.value.metadataStatus = 'idle'
    allowNativeClearCurrentSong = false
    stopKeepAlive()
    syncMediaSessionState()
  } catch {
    allowNativeClearCurrentSong = false
    setUserSafeError('停止播放失败，请稍后重试。')
  }
}

export interface SaveCurrentSongUserEditResult {
  /** 曲库是否写入成功 */
  libraryOk: boolean
  /** 文件内嵌标签是否写入成功（尽力） */
  fileOk: boolean
  /** 文件失败短因，不含密码/路径敏感细节 */
  fileError?: string
  song?: SongItem
}

/**
 * 保存当前曲用户编辑：先写库 → sync 展示/音量/会话 → 再尽力写文件标签（D4）。
 * 文件失败不回滚库。
 */
export const saveCurrentSongUserEdit = async (
  patch: SongUserEditPatch,
): Promise<SaveCurrentSongUserEditResult> => {
  const currentId = state.value.currentSong?.id
  if (!currentId) {
    return { libraryOk: false, fileOk: false, fileError: '当前没有播放中的歌曲。' }
  }

  let written: SongItem
  try {
    written = updateSongUserEdit(currentId, patch).song
  } catch (error) {
    return {
      libraryOk: false,
      fileOk: false,
      fileError: error instanceof Error ? error.message : '保存曲库失败。',
    }
  }

  // 用户歌词强制替换运行时；媒体会话/封面随 sync
  const forceLyrics = patch.lyrics !== undefined
  syncDisplayStateFromSong(written, { forceLyrics })

  // RG 变化且正在播/暂停：立即重设音量
  if (
    patch.replayGainTrackDb !== undefined
    && (state.value.status === 'playing' || state.value.status === 'paused')
    && state.value.currentSong?.id === written.id
  ) {
    void AudioPlayerNative.setVolume(resolvePlaybackVolume(written))
  }

  // 尽力写文件；失败不回滚
  const fileResult = await writeSongFileMetadata(written, patch)
  return {
    libraryOk: true,
    fileOk: fileResult.ok,
    fileError: fileResult.ok ? undefined : (fileResult.message || fileResult.code || '写入音频文件失败'),
    song: written,
  }
}

/** 文件侧写标签：local SAF / webdav PUT；密码不进 state/日志 */
const writeSongFileMetadata = async (
  song: SongItem,
  patch: SongUserEditPatch,
): Promise<{ ok: boolean; code?: string; message?: string }> => {
  // 封面仅在 patch 显式包含时写文件，避免每次保存都重嵌已有图
  const coverInPatch = patch.coverUri !== undefined
  const clearCover = coverInPatch && (patch.coverUri === null || !String(patch.coverUri).trim())
  const coverPath = coverInPatch && !clearCover ? toNativeCoverPath(song.coverUri) : null

  const clearLyrics = patch.lyrics !== undefined && (patch.lyrics === null || !String(patch.lyrics).trim())
  const rgInPatch = patch.replayGainTrackDb !== undefined
  const clearReplayGain = rgInPatch && (
    patch.replayGainTrackDb === null || !Number.isFinite(patch.replayGainTrackDb)
  )

  const common = {
    title: song.title,
    artist: song.artist ?? '',
    album: song.album ?? '',
    lyrics: clearLyrics ? undefined : (patch.lyrics !== undefined ? song.lyrics : undefined),
    clearLyrics: clearLyrics || false,
    coverPath: coverPath || undefined,
    clearCover: clearCover || false,
    replayGainTrackDb: rgInPatch && !clearReplayGain ? song.replayGainTrackDb : undefined,
    clearReplayGain: clearReplayGain || false,
  }

  try {
    if (song.sourceType === 'local') {
      return await writeLocalAudioMetadata({
        uri: song.uri,
        ...common,
      })
    }

    const source = getWebDavSource(song)
    const password = await getWebDavPassword(source.credentialKey)
    if (!password) {
      return { ok: false, code: 'missingCredentials', message: '缺少 WebDAV 密码，无法写回文件。' }
    }
    return await writeWebDavAudioMetadata({
      url: song.uri,
      username: source.username,
      password,
      ...common,
    })
  } catch (error) {
    return {
      ok: false,
      code: 'write_failed',
      message: error instanceof Error ? error.message : '写入音频文件失败。',
    }
  }
}

/** 曲库 coverUri → 原生可打开的 file path / file:// */
const toNativeCoverPath = (coverUri: string | undefined): string | null => {
  const safe = toSafeCoverUri(coverUri)
  if (!safe) {
    return null
  }
  return safe
}

export const playerState = readonly(state.value)
export const isPlaying = computed(() => state.value.status === 'playing')
export const hasActiveSong = computed(() => Boolean(state.value.currentSong))
export const isPlaybackFinished = computed(() => state.value.status === 'finished')

export {
  advanceToNext,
  advanceToPrevious,
  peekNext,
  queueState,
  repeatMode,
  selectSongAtIndex,
  shuffleEnabled,
} from './queue'
