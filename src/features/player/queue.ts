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
  /** 根据歌曲 ReplayGain 标签做音量均衡；默认开启 */
  loudnessNormalizeEnabled: boolean
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

const defaultConfig = (): PlayerConfig => ({
  repeatMode: 'all',
  shuffleEnabled: false,
  // 默认开启：缓解曲库响度不一（#46）
  loudnessNormalizeEnabled: true,
})

const loadConfig = (): PlayerConfig => {
  const defaults = defaultConfig()
  const raw = localStorage.getItem(CONFIG_STORAGE_KEY)
  if (!raw) {
    return defaults
  }

  try {
    const parsed: unknown = JSON.parse(raw)
    if (!isRecord(parsed)) {
      return defaults
    }

    const repeatMode = parsed.repeatMode === 'one' ? 'one' : 'all'
    const shuffleEnabled = parsed.shuffleEnabled === true
    // 旧 config 缺键时默认 true；仅显式 false 关闭
    const loudnessNormalizeEnabled = parsed.loudnessNormalizeEnabled !== false
    return { repeatMode, shuffleEnabled, loudnessNormalizeEnabled }
  } catch {
    return defaults
  }
}

const saveConfig = (config: PlayerConfig): void => {
  localStorage.setItem(CONFIG_STORAGE_KEY, JSON.stringify({
    repeatMode: config.repeatMode,
    shuffleEnabled: config.shuffleEnabled,
    loudnessNormalizeEnabled: config.loudnessNormalizeEnabled,
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

const createSongResolver = (songs = loadSongs()): ((queueItems: QueueItem[]) => SongItem[]) => {
  const songsById = new Map<string, SongItem>()
  for (const song of songs) {
    // 与原先 Array.find 一致：异常重复 id 时保留曲库中的第一条记录。
    if (!songsById.has(song.id)) {
      songsById.set(song.id, song)
    }
  }

  return (queueItems: QueueItem[]): SongItem[] => queueItems.flatMap((queueItem) => {
    const song = songsById.get(queueItem.songId)
    return song ? [song] : []
  })
}

const resolveSongsFromQueue = (queueItems: QueueItem[]): SongItem[] => {
  return createSongResolver()(queueItems)
}

const getResolvedActiveItems = (): SongItem[] => {
  return resolveSongsFromQueue(queueData.shuffleOrder ?? queueData.items)
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

const refreshQueueState = (resolvedItems = resolveSongsFromQueue(queueData.shuffleOrder ?? queueData.items)) => {
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
  refreshQueueState(resolvedItems)
  return resolvedItems[index]
}

/**
 * 按当前 repeat/shuffle 规则解析「下一首」，不修改 currentIndex / 不写持久化。
 * 语义与 advanceToNext 目标一致，但无副作用。
 */
export const peekNext = (): SongItem | null => {
  const items = getResolvedActiveItems()

  if (items.length === 0) {
    return null
  }

  if (config.repeatMode === 'one') {
    const index = currentIndex < 0 || currentIndex >= items.length ? 0 : currentIndex
    return items[index] ?? null
  }

  const nextIndex = currentIndex + 1
  return items[nextIndex >= items.length ? 0 : nextIndex] ?? null
}

/**
 * 选择播放失败恢复链的下一首候选。
 *
 * 始终沿当前 active order 向后查找并最多回绕一次；该流程临时忽略单曲循环，
 * 且跳过本次恢复链已经尝试过的歌曲，保证损坏队列不会无限推进。
 */
export const advanceToNextRecoveryCandidate = (attemptedSongIds: ReadonlySet<string>): SongItem | null => {
  const items = queueData.shuffleOrder ?? queueData.items

  if (items.length === 0) {
    return null
  }

  const resolvedItems = getResolvedActiveItems()
  if (resolvedItems.length === 0) {
    return null
  }
  const resolvedById = new Map(resolvedItems.map((song) => [song.id, song]))
  const startIndex = currentIndex >= 0 && currentIndex < items.length ? currentIndex : -1
  for (let offset = 1; offset <= items.length; offset += 1) {
    const candidateIndex = (startIndex + offset) % items.length
    const candidateItem = items[candidateIndex]
    const candidate = resolvedById.get(candidateItem.songId)
    if (!candidate || attemptedSongIds.has(candidate.id)) {
      continue
    }

    currentIndex = candidateIndex
    refreshQueueState(resolvedItems)
    return candidate
  }

  return null
}

export const advanceToNext = (): SongItem | null => {
  const items = queueData.shuffleOrder ?? queueData.items

  if (items.length === 0) {
    currentIndex = -1
    refreshQueueState()
    return null
  }

  const resolvedItems = getResolvedActiveItems()
  if (resolvedItems.length === 0) {
    currentIndex = -1
    refreshQueueState(resolvedItems)
    return null
  }

  if (config.repeatMode === 'one') {
    if (currentIndex < 0 || currentIndex >= resolvedItems.length) {
      currentIndex = 0
    }
    refreshQueueState(resolvedItems)
    return resolvedItems[currentIndex] ?? null
  }

  currentIndex = currentIndex + 1 >= resolvedItems.length ? 0 : currentIndex + 1
  refreshQueueState(resolvedItems)
  return resolvedItems[currentIndex] ?? null
}

export const advanceToPrevious = (): SongItem | null => {
  const items = queueData.shuffleOrder ?? queueData.items

  if (items.length === 0) {
    currentIndex = -1
    refreshQueueState()
    return null
  }

  const resolvedItems = getResolvedActiveItems()
  if (resolvedItems.length === 0) {
    currentIndex = -1
    refreshQueueState(resolvedItems)
    return null
  }

  if (config.repeatMode === 'one') {
    if (currentIndex < 0 || currentIndex >= resolvedItems.length) {
      currentIndex = 0
    }
    refreshQueueState(resolvedItems)
    return resolvedItems[currentIndex] ?? null
  }

  currentIndex = currentIndex - 1 < 0 ? resolvedItems.length - 1 : currentIndex - 1
  refreshQueueState(resolvedItems)
  return resolvedItems[currentIndex] ?? null
}

export const findQueueIndexBySongId = (songId: string): number => {
  return getResolvedActiveItems().findIndex((song) => song.id === songId)
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

export const setLoudnessNormalizeEnabled = (enabled: boolean): void => {
  config.loudnessNormalizeEnabled = enabled
  saveConfig(config)
}

export const isLoudnessNormalizeEnabled = (): boolean => config.loudnessNormalizeEnabled

export const queueState = readonly(queueStateRaw)
export const repeatMode = (): RepeatMode => queueStateRaw.repeatMode

export const shuffleEnabled = (): boolean => queueStateRaw.shuffleEnabled