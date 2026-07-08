import { reactive, readonly } from 'vue'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'

// --------------- 持久化键 ---------------

const QUEUE_STORAGE_KEY = 'muses:queue'
const CONFIG_STORAGE_KEY = 'muses:player-config'

// --------------- 类型 ---------------

export type RepeatMode = 'one' | 'all'

export interface PlayerConfig {
  repeatMode: RepeatMode
  shuffleEnabled: boolean
}

export interface QueueItem {
  songId: string
}

export interface QueueData {
  items: QueueItem[]
  originalOrder: QueueItem[]
  shuffleOrder: QueueItem[] | null
}

export interface QueueState {
  items: SongItem[]
  currentIndex: number
  hasItems: boolean
  repeatMode: RepeatMode
  shuffleEnabled: boolean
}

// --------------- 解析与持久化辅助 ---------------

const isString = (value: unknown): value is string => {
  return typeof value === 'string' && value.length > 0
}

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const isQueueItem = (value: unknown): value is QueueItem => {
  return isRecord(value) && isString(value.songId)
}

const loadConfig = (): PlayerConfig => {
  const raw = localStorage.getItem(CONFIG_STORAGE_KEY)
  if (!raw) {
    return { repeatMode: 'all', shuffleEnabled: false }
  }

  try {
    const parsed: unknown = JSON.parse(raw)
    if (!isRecord(parsed)) {
      return { repeatMode: 'all', shuffleEnabled: false }
    }

    const repeatMode = parsed.repeatMode === 'one' ? 'one' : 'all'
    const shuffleEnabled = parsed.shuffleEnabled === true
    return { repeatMode, shuffleEnabled }
  } catch {
    return { repeatMode: 'all', shuffleEnabled: false }
  }
}

const saveConfig = (config: PlayerConfig): void => {
  localStorage.setItem(CONFIG_STORAGE_KEY, JSON.stringify({
    repeatMode: config.repeatMode,
    shuffleEnabled: config.shuffleEnabled,
  }))
}

const loadQueueData = (): QueueData => {
  const raw = localStorage.getItem(QUEUE_STORAGE_KEY)
  if (!raw) {
    return { items: [], originalOrder: [], shuffleOrder: null }
  }

  try {
    const parsed: unknown = JSON.parse(raw)
    if (!isRecord(parsed) || !Array.isArray(parsed.items)) {
      return { items: [], originalOrder: [], shuffleOrder: null }
    }

    const items: QueueItem[] = (parsed.items as unknown[]).filter(isQueueItem)
    const originalOrder: QueueItem[] = Array.isArray(parsed.originalOrder)
      ? (parsed.originalOrder as unknown[]).filter(isQueueItem)
      : []
    const shuffleOrder: QueueItem[] | null = Array.isArray(parsed.shuffleOrder)
      ? (parsed.shuffleOrder as unknown[]).filter(isQueueItem)
      : null

    return { items: items.length > 0 ? items : [], originalOrder, shuffleOrder }
  } catch {
    return { items: [], originalOrder: [], shuffleOrder: null }
  }
}

const sanitizeQueueItems = (items: QueueItem[]): QueueItem[] => {
  return items.map((item) => ({ songId: item.songId }))
}

const saveQueueData = (data: QueueData): void => {
  localStorage.setItem(QUEUE_STORAGE_KEY, JSON.stringify({
    items: sanitizeQueueItems(data.items),
    originalOrder: sanitizeQueueItems(data.originalOrder),
    shuffleOrder: data.shuffleOrder ? sanitizeQueueItems(data.shuffleOrder) : null,
  }))
}

// --------------- Fisher-Yates 洗牌 ---------------

const fisherYatesShuffle = <T>(array: T[]): T[] => {
  const result = [...array]
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    const temp = result[i]
    result[i] = result[j]
    result[j] = temp
  }
  return result
}

// --------------- 将 QueueItem[] 解析为 SongItem[] ---------------

const resolveSongsFromQueue = (queueItems: QueueItem[]): SongItem[] => {
  const songs = loadSongs()
  return queueItems
    .map((qi) => songs.find((song) => song.id === qi.songId))
    .filter((song): song is SongItem => Boolean(song))
}

// --------------- 内部可变状态 ---------------

let queueData = loadQueueData()
const config = loadConfig()
let currentIndex = -1

const queueStateRaw = reactive<QueueState>({
  items: resolveSongsFromQueue(queueData.shuffleOrder ?? queueData.items),
  currentIndex: -1,
  hasItems: false,
  repeatMode: config.repeatMode,
  shuffleEnabled: config.shuffleEnabled,
})

// --------------- 刷新只读状态 ---------------

const refreshQueueState = () => {
  const resolvedItems = resolveSongsFromQueue(queueData.shuffleOrder ?? queueData.items)
  queueStateRaw.items = resolvedItems
  queueStateRaw.currentIndex = currentIndex
  queueStateRaw.hasItems = resolvedItems.length > 0
  queueStateRaw.repeatMode = config.repeatMode
  queueStateRaw.shuffleEnabled = config.shuffleEnabled
}

// --------------- 队列操作 ---------------

export const enqueueSongs = (songs: SongItem[]): void => {
  if (songs.length === 0) {
    return
  }

  const currentSongs = loadSongs()
  const currentSongIds = new Set(currentSongs.map((song) => song.id))
  const existingIds = new Set(queueData.items
    .filter((qi) => currentSongIds.has(qi.songId))
    .map((qi) => qi.songId))
  const newItems = songs
    .filter((song) => !existingIds.has(song.id))
    .map((song) => ({ songId: song.id }))

  if (newItems.length === 0) {
    return
  }

  queueData.items = [
    ...queueData.items.filter((qi) => currentSongIds.has(qi.songId)),
    ...newItems,
  ]
  queueData.originalOrder = [...queueData.items]

  if (config.shuffleEnabled) {
    queueData.shuffleOrder = fisherYatesShuffle(queueData.items)
  } else {
    queueData.shuffleOrder = null
  }

  saveQueueData(queueData)
  refreshQueueState()
}

export const enqueueSong = (song: SongItem): void => {
  enqueueSongs([song])
}

export const removeSongFromQueue = (songId: string): void => {
  const activeItems = queueData.shuffleOrder ?? queueData.items
  const activeIndex = activeItems.findIndex((qi) => qi.songId === songId)
  if (activeIndex < 0) {
    return
  }

  queueData.items = queueData.items.filter((qi) => qi.songId !== songId)
  queueData.originalOrder = queueData.originalOrder.filter((qi) => qi.songId !== songId)

  if (config.shuffleEnabled && queueData.shuffleOrder) {
    queueData.shuffleOrder = queueData.shuffleOrder.filter((qi) => qi.songId !== songId)
  }

  if (activeIndex < currentIndex) {
    currentIndex = Math.max(-1, currentIndex - 1)
  } else if (activeIndex === currentIndex) {
    currentIndex = -1
  }

  saveQueueData(queueData)
  refreshQueueState()
}

export const clearQueue = (): void => {
  currentIndex = -1
  queueData = { items: [], originalOrder: [], shuffleOrder: null }
  saveQueueData(queueData)
  refreshQueueState()
}

export const selectSongAtIndex = (index: number): SongItem | null => {
  const resolvedItems = resolveSongsFromQueue(queueData.shuffleOrder ?? queueData.items)
  if (index < 0 || index >= resolvedItems.length) {
    return null
  }

  currentIndex = index
  refreshQueueState()
  return resolvedItems[index]
}

export const advanceToNext = (): SongItem | null => {
  const items = queueData.shuffleOrder ?? queueData.items

  if (items.length === 0) {
    currentIndex = -1
    refreshQueueState()
    return null
  }

  if (config.repeatMode === 'one') {
    if (currentIndex < 0 || currentIndex >= items.length) {
      currentIndex = 0
    }
    refreshQueueState()
    return resolveSongsFromQueue([items[currentIndex]])[0] ?? null
  }

  const nextIndex = currentIndex + 1
  if (nextIndex >= items.length) {
    currentIndex = 0
  } else {
    currentIndex = nextIndex
  }

  refreshQueueState()
  return resolveSongsFromQueue([items[currentIndex]])[0] ?? null
}

export const advanceToPrevious = (): SongItem | null => {
  const items = queueData.shuffleOrder ?? queueData.items

  if (items.length === 0) {
    currentIndex = -1
    refreshQueueState()
    return null
  }

  if (config.repeatMode === 'one') {
    if (currentIndex < 0 || currentIndex >= items.length) {
      currentIndex = 0
    }
    refreshQueueState()
    return resolveSongsFromQueue([items[currentIndex]])[0] ?? null
  }

  const previousIndex = currentIndex - 1
  if (previousIndex < 0) {
    currentIndex = items.length - 1
  } else {
    currentIndex = previousIndex
  }

  refreshQueueState()
  return resolveSongsFromQueue([items[currentIndex]])[0] ?? null
}

export const findQueueIndexBySongId = (songId: string): number => {
  const items = queueData.shuffleOrder ?? queueData.items
  return items.findIndex((qi) => qi.songId === songId)
}

export const syncCurrentIndex = (songId: string): void => {
  const index = findQueueIndexBySongId(songId)
  if (index >= 0) {
    currentIndex = index
    refreshQueueState()
  }
}

// --------------- 模式切换 ---------------

export const setRepeatMode = (mode: RepeatMode): void => {
  config.repeatMode = mode
  saveConfig(config)
  refreshQueueState()
}

export const toggleShuffle = (): void => {
  const wasShuffleEnabled = config.shuffleEnabled
  const activeItemsBeforeToggle = wasShuffleEnabled && queueData.shuffleOrder ? queueData.shuffleOrder : queueData.items
  const currentSongId = currentIndex >= 0 ? activeItemsBeforeToggle[currentIndex]?.songId : undefined

  config.shuffleEnabled = !config.shuffleEnabled

  if (config.shuffleEnabled) {
    queueData.originalOrder = [...queueData.items]
    queueData.shuffleOrder = fisherYatesShuffle(queueData.items)

    if (currentSongId && queueData.shuffleOrder.length > 0) {
      const newIndex = queueData.shuffleOrder.findIndex((qi) => qi.songId === currentSongId)
      currentIndex = newIndex >= 0 ? newIndex : 0
    }
  } else {
    queueData.shuffleOrder = null

    if (currentSongId && queueData.items.length > 0) {
      const newIndex = queueData.items.findIndex((qi) => qi.songId === currentSongId)
      currentIndex = newIndex >= 0 ? newIndex : 0
    }
  }

  saveQueueData(queueData)
  saveConfig(config)
  refreshQueueState()
}

// --------------- 只读导出 ---------------

export const queueState = readonly(queueStateRaw)
export const repeatMode = (): RepeatMode => queueStateRaw.repeatMode

export const shuffleEnabled = (): boolean => queueStateRaw.shuffleEnabled