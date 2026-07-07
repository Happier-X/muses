import { computed, reactive, readonly } from 'vue'
import type { SongItem } from '@/features/library/types'
import { CURRENT_METADATA_VERSION, loadSongs, upsertSong } from '@/features/library/storage'
import { readLocalAudioTags, readWebDavAudioTags } from '@/features/library/tags'
import { getWebDavPassword, loadSources } from '@/features/sources/storage'
import type { WebDavSourceItem } from '@/features/sources/types'
import { AudioPlayerNative } from './native'
import type { AudioPlayerNativeState, PlayOptions, PlayerState } from './types'
import { createPlayerSongSnapshot } from './types'

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

const LOCAL_METADATA_SCAN_TIMEOUT_MS = 15_000
const WEBDAV_METADATA_SCAN_TIMEOUT_MS = 120_000

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
  }
}

export const initializePlayer = async (): Promise<void> => {
  if (!nativeListenerReady) {
    nativeListenerReady = true
    await AudioPlayerNative.addListener('stateChange', applyNativeState)
  }

  try {
    applyNativeState(await AudioPlayerNative.getState())
  } catch {
    // 非 Android 或原生插件尚不可用时，保持空闲状态，用户点击播放时再显示明确错误。
  }
}

const normalizePlaybackTime = (value: unknown): number => {
  return typeof value === 'number' && Number.isFinite(value) && value > 0 ? value : 0
}

const syncDisplayStateFromSong = (song: SongItem): void => {
  if (state.currentSong?.id !== song.id) {
    return
  }

  state.currentSong = createPlayerSongSnapshot(song)
  state.lyrics = song.lyrics || null
  state.coverUri = song.coverUri || null
  state.duration = normalizePlaybackTime(song.duration) || state.duration
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

  state.status = 'loading'
  metadataScanToken += 1
  state.currentSong = createPlayerSongSnapshot(latestSong)
  state.errorMessage = null
  state.position = 0
  state.duration = normalizePlaybackTime(latestSong.duration)
  state.lyrics = latestSong.lyrics || null
  state.coverUri = latestSong.coverUri || null
  state.metadataStatus = latestSong.tagsScanned === true ? 'ready' : 'idle'

  try {
    await AudioPlayerNative.play(await buildPlayOptions(latestSong))
    state.status = 'playing'
    void scanSongMetadata(latestSong)
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    setUserSafeError(isSafePlaybackError(message) ? message : '播放失败，请稍后重试。')
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
    await AudioPlayerNative.seek({ position: safePosition })
    state.position = safePosition
    state.errorMessage = null
  } catch {
    setUserSafeError('跳转播放进度失败，请稍后重试。')
  }
}

export const stopPlayback = async (): Promise<void> => {
  try {
    await AudioPlayerNative.stop()
    metadataScanToken += 1
    state.status = 'stopped'
    state.currentSong = null
    state.errorMessage = null
    state.position = 0
    state.duration = 0
    state.lyrics = null
    state.coverUri = null
    state.metadataStatus = 'idle'
  } catch {
    setUserSafeError('停止播放失败，请稍后重试。')
  }
}

export const playerState = readonly(state)
export const isPlaying = computed(() => state.status === 'playing')
export const hasActiveSong = computed(() => Boolean(state.currentSong))
