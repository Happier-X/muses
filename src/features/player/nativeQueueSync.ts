import type { SongItem } from '@/features/library/types'
import { AudioPlayerNative } from './native'
import { queueState, findQueueIndexBySongId, advanceToNext } from './queue'
import { loadSources } from '@/features/sources/storage'
import { getWebDavPassword } from '@/features/sources/storage'

const WINDOW_RADIUS = 5
const WINDOW_REFILL_THRESHOLD = 2

let requestUrlsListenerReady = false

/** 计算滑动窗口并推送到原生 */
export const syncQueueToNative = async (options?: { windowResetFromWrap?: boolean }): Promise<void> => {
  const items = queueState.items
  const currentIndex = queueState.currentIndex
  const repeatMode = queueState.repeatMode

  if (items.length === 0 || currentIndex < 0) {
    await AudioPlayerNative.updateQueueContext({
      windowTracks: [],
      windowCurrentIndex: -1,
      repeatMode,
      hasPreviousOutsideWindow: false,
      hasNextOutsideWindow: false,
    })
    return
  }

  const windowStart = Math.max(0, currentIndex - WINDOW_RADIUS)
  // 充分利用窗口大小，尽量向右扩展到 2*WINDOW_RADIUS+1
  const windowEnd = Math.min(items.length, windowStart + WINDOW_RADIUS * 2 + 1)
  // 若靠近末尾导致窗口未填满，尝试向左扩展
  const desiredSize = WINDOW_RADIUS * 2 + 1
  if (windowEnd - windowStart < desiredSize) {
    const extra = desiredSize - (windowEnd - windowStart)
    const newStart = Math.max(0, windowStart - extra)
    const adjustedStart = newStart
    const adjustedTracks = items.slice(adjustedStart, windowEnd)
    const windowCurrentIndex = currentIndex - adjustedStart
    const hasPreviousOutsideWindow = adjustedStart > 0
    const hasNextOutsideWindow = windowEnd < items.length
    const windowTracks = await Promise.all(adjustedTracks.map((song, idx) => toNativeTrack(song, adjustedStart + idx)))
    await AudioPlayerNative.updateQueueContext({
      windowTracks,
      windowCurrentIndex,
      repeatMode,
      hasPreviousOutsideWindow,
      hasNextOutsideWindow,
      windowResetFromWrap: options?.windowResetFromWrap,
    })
    return
  }

  const windowTracks = await Promise.all(items.slice(windowStart, windowEnd).map((song, idx) => toNativeTrack(song, windowStart + idx)))
  const windowCurrentIndex = currentIndex - windowStart
  const hasPreviousOutsideWindow = windowStart > 0
  const hasNextOutsideWindow = windowEnd < items.length

  await AudioPlayerNative.updateQueueContext({
    windowTracks,
    windowCurrentIndex,
    repeatMode,
    hasPreviousOutsideWindow,
    hasNextOutsideWindow,
    windowResetFromWrap: options?.windowResetFromWrap,
  })
}

const toNativeTrack = async (song: Pick<SongItem, 'id' | 'uri' | 'title' | 'artist' | 'album' | 'coverUri' | 'duration' | 'sourceType' | 'sourceId'>, playListIndex: number) => {
  let authHeader: string | undefined
  if (song.sourceType === 'webdav' && song.uri.startsWith('http')) {
    try {
      const sources = loadSources()
      const src = sources.find((s) => s.id === song.sourceId) as any
      if (src?.type === 'webdav' && src.username) {
        const pwd = await getWebDavPassword(src.credentialKey)
        if (pwd) {
          const bytes = new TextEncoder().encode(`${src.username}:${pwd}`)
          let binary = ''
          bytes.forEach((b) => { binary += String.fromCharCode(b) })
          authHeader = `Basic ${btoa(binary)}`
        }
      }
    } catch { /* ignore */ }
  }
  return {
    songId: song.id,
    url: song.uri,
    authHeader,
    title: song.title,
    artist: song.artist || '',
    album: song.album || '',
    coverUrl: song.coverUri || '',
    durationMs: Math.round((song.duration || 0) * 1000),
    playListIndex,
  }
}

/** 监听原生 requestUrls 事件，触发补窗（重新推送窗口） */
export const setupNativeQueueSyncListener = async (): Promise<void> => {
  if (requestUrlsListenerReady) return
  requestUrlsListenerReady = true
  await AudioPlayerNative.addRequestUrlsListener(() => {
    // 末尾列表循环 wrap：原生窗口耗尽且 ALL 模式已到队尾，JS 需将索引重置到头部再补窗
    const items = queueState.items
    const idx = queueState.currentIndex
    if (queueState.repeatMode === 'all' && items.length > 0 && idx === items.length - 1) {
      // advanceToNext 会 wrap 到 0，同步 queueState
      advanceToNext()
      void syncQueueToNative({ windowResetFromWrap: true })
      return
    }
    void syncQueueToNative()
  })
}

/** 原生已自治切歌时，同步 JS 队列索引 */
export const syncCurrentIndexFromNative = (nativeSongId: string): boolean => {
  const idx = findQueueIndexBySongId(nativeSongId)
  if (idx < 0) return false
  // 通过已有的 syncCurrentIndex 同步，这里需要动态导入避免循环
  // 直接调用 queue 的 syncCurrentIndex（已通过 queueState 暴露）
  // 为避免循环依赖，使用 loadSongs 验证后由调用方处理
  return true
}

export const WINDOW_REFILL_THRESHOLD_EXPORT = WINDOW_REFILL_THRESHOLD
