import { computed, reactive, readonly } from 'vue'
import type { SongItem } from '@/features/library/types'
import { getWebDavPassword, loadSources } from '@/features/sources/storage'
import type { WebDavSourceItem } from '@/features/sources/types'
import { AudioPlayerNative } from './native'
import type { AudioPlayerNativeState, PlayOptions, PlayerState } from './types'
import { createPlayerSongSnapshot } from './types'

const state = reactive<PlayerState>({
  status: 'idle',
  currentSong: null,
  errorMessage: null,
})

let nativeListenerReady = false

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

  if (nativeState.status === 'idle' || nativeState.status === 'stopped') {
    state.currentSong = null
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
  const password = await getWebDavPassword(source.credentialKey)
  if (!password) {
    throw new Error('WebDAV 密码不存在，请重新添加该音源。')
  }

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
  state.status = 'loading'
  state.currentSong = createPlayerSongSnapshot(song)
  state.errorMessage = null

  try {
    await AudioPlayerNative.play(await buildPlayOptions(song))
    state.status = 'playing'
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

export const stopPlayback = async (): Promise<void> => {
  try {
    await AudioPlayerNative.stop()
    state.status = 'stopped'
    state.currentSong = null
    state.errorMessage = null
  } catch {
    setUserSafeError('停止播放失败，请稍后重试。')
  }
}

export const playerState = readonly(state)
export const isPlaying = computed(() => state.status === 'playing')
export const hasActiveSong = computed(() => Boolean(state.currentSong))
