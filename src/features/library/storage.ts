import type {
  AudioTags,
  FieldSource,
  LyricsSource,
  MetaFieldKey,
  MetaSources,
  SongItem,
  SongLyricsFormat,
  UserEditedField,
} from './types'
import { USER_EDITED_FIELDS } from './types'
import { getTitleFromPath } from './audio'

const SONGS_STORAGE_KEY = 'muses:songs'
export const SONGS_UPDATED_EVENT = 'muses:songs-updated'
/** v4：新增字段来源追踪 metaSources；旧 v3 曲在播放时懒扫自然升级补齐来源 */
export const CURRENT_METADATA_VERSION = 4

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const isString = (value: unknown): value is string => {
  return typeof value === 'string' && value.length > 0
}

const isOptionalString = (value: unknown): value is string | undefined => {
  return value === undefined || typeof value === 'string'
}

const isOptionalNumber = (value: unknown): value is number | undefined => {
  return value === undefined || (typeof value === 'number' && Number.isFinite(value))
}

const isOptionalBoolean = (value: unknown): value is boolean | undefined => {
  return value === undefined || typeof value === 'boolean'
}

const isOptionalLyricsSource = (value: unknown): value is LyricsSource | undefined => {
  return (
    value === undefined
    || value === 'embedded'
    || value === 'sidecar'
    || value === 'scrape'
    // 历史遗留值：播放器已不再产生，读入后由 local-first-v1 迁移清除
    || value === 'online'
  )
}

const isOptionalLyricsFormat = (value: unknown): value is SongLyricsFormat | undefined => {
  return (
    value === undefined
    || value === 'lrc'
    || value === 'ttml'
    || value === 'yrc'
    || value === 'qrc'
  )
}

const isUserEditedFieldValue = (value: unknown): value is UserEditedField => {
  return (
    value === 'title'
    || value === 'artist'
    || value === 'album'
    || value === 'cover'
    || value === 'lyrics'
    || value === 'replayGain'
  )
}

const isMetaFieldKeyValue = (value: unknown): value is MetaFieldKey => {
  return value === 'title' || value === 'artist' || value === 'album' || value === 'cover'
}

const isFieldSourceValue = (value: unknown): value is FieldSource => {
  return (
    value === 'embedded'
    || value === 'scrape'
    || value === 'manual'
    // 历史遗留值：播放器自动补缺已移除，读入后由 local-first-v1 迁移清除
    || value === 'cloud'
  )
}

/** 校验并裁剪 metaSources；非法/空 → undefined */
const sanitizeMetaSources = (value: unknown): MetaSources | undefined => {
  if (!isRecord(value)) {
    return undefined
  }
  const next: MetaSources = {}
  for (const [key, src] of Object.entries(value)) {
    if (isMetaFieldKeyValue(key) && isFieldSourceValue(src)) {
      next[key] = src
    }
  }
  return Object.keys(next).length > 0 ? next : undefined
}

const isOptionalMetaSources = (value: unknown): boolean => {
  if (value === undefined) {
    return true
  }
  if (!isRecord(value)) {
    return false
  }
  return Object.entries(value).every(
    ([key, src]) => isMetaFieldKeyValue(key) && isFieldSourceValue(src),
  )
}

/** 校验并去重 userEditedFields；非法/空 → undefined */
const sanitizeUserEditedFields = (value: unknown): UserEditedField[] | undefined => {
  if (!Array.isArray(value)) {
    return undefined
  }
  const next: UserEditedField[] = []
  for (const item of value) {
    if (isUserEditedFieldValue(item) && !next.includes(item)) {
      next.push(item)
    }
  }
  return next.length > 0 ? next : undefined
}

const isOptionalUserEditedFields = (value: unknown): boolean => {
  if (value === undefined) {
    return true
  }
  if (!Array.isArray(value)) {
    return false
  }
  return value.every((item) => isUserEditedFieldValue(item))
}

/** 加载校验：仅硬拒绝 data:/base64，避免整条歌曲被丢弃；http 等在 sanitize 时剥离 */
const isLoadableCoverUri = (value: string | undefined): boolean => {
  if (value === undefined) {
    return true
  }
  const normalized = value.trim().toLowerCase()
  return !normalized.startsWith('data:') && !normalized.includes(';base64,')
}

/** 写库/写回：仅保留本地安全 URI */
const sanitizeCoverUri = (coverUri: string | undefined): string | undefined => {
  if (!coverUri?.trim()) {
    return undefined
  }
  const normalized = coverUri.trim().toLowerCase()
  if (
    normalized.startsWith('data:')
    || normalized.startsWith('blob:')
    || normalized.includes(';base64,')
    || normalized.startsWith('http://')
    || normalized.startsWith('https://')
  ) {
    return undefined
  }
  return coverUri.trim()
}

const isSongItem = (value: unknown): value is SongItem => {
  if (!isRecord(value)) {
    return false
  }

  return (
    isString(value.id) &&
    isString(value.sourceId) &&
    (value.sourceType === 'local' || value.sourceType === 'webdav') &&
    isString(value.path) &&
    isString(value.uri) &&
    isString(value.title) &&
    isString(value.createdAt) &&
    isString(value.updatedAt) &&
    isOptionalString(value.artist) &&
    isOptionalString(value.album) &&
    isOptionalNumber(value.duration) &&
    isOptionalString(value.lyrics) &&
    isOptionalLyricsSource(value.lyricsSource) &&
    isOptionalLyricsFormat(value.lyricsFormat) &&
    isOptionalString(value.coverUri) &&
    isLoadableCoverUri(value.coverUri) &&
    isOptionalNumber(value.replayGainTrackDb) &&
    isOptionalMetaSources(value.metaSources) &&
    isOptionalUserEditedFields(value.userEditedFields) &&
    isOptionalBoolean(value.tagsScanned) &&
    isOptionalString(value.tagsScannedAt) &&
    isOptionalNumber(value.metadataVersion)
  )
}

/** 字段是否被用户手改保护 */
export const isUserEditedField = (
  song: Pick<SongItem, 'userEditedFields'> | null | undefined,
  field: UserEditedField,
): boolean => {
  return !!song?.userEditedFields?.includes(field)
}

/**
 * 读取字段来源（R1-4）：manual 由 userEditedFields 派生；缺省视为 embedded（存量兼容）。
 * cover 字段映射到 userEditedFields 的 'cover'。
 */
export const getFieldSource = (
  song: Pick<SongItem, 'metaSources' | 'userEditedFields'> | null | undefined,
  field: MetaFieldKey,
): FieldSource => {
  if (!song) {
    return 'embedded'
  }
  // manual 优先：用户手改字段永久视为 manual，不读 metaSources
  const editedKey: UserEditedField = field === 'cover' ? 'cover' : field
  if (song.userEditedFields?.includes(editedKey)) {
    return 'manual'
  }
  return song.metaSources?.[field] ?? 'embedded'
}

/**
 * 自动 tags 写入前剥离用户保护字段，避免扫描/在线/预取覆盖手改。
 * 返回新对象，不修改入参。
 */
export const applyTagsRespectingUserEdits = (
  song: Pick<SongItem, 'userEditedFields'>,
  tags: AudioTags,
): AudioTags => {
  const protectedFields = song.userEditedFields
  if (!protectedFields || protectedFields.length === 0) {
    return { ...tags }
  }

  const next: AudioTags = { ...tags }
  if (protectedFields.includes('title')) {
    delete next.title
  }
  if (protectedFields.includes('artist')) {
    delete next.artist
  }
  if (protectedFields.includes('album')) {
    delete next.album
  }
  if (protectedFields.includes('cover')) {
    delete next.coverUri
  }
  if (protectedFields.includes('lyrics')) {
    delete next.lyrics
    delete next.lyricsSource
    delete next.lyricsFormat
  }
  if (protectedFields.includes('replayGain')) {
    delete next.replayGainTrackDb
  }
  // 手改字段的来源标记一并剥离：manual 由 userEditedFields 派生，不写 metaSources
  if (next.metaSources) {
    const filtered: MetaSources = { ...next.metaSources }
    if (protectedFields.includes('title')) {
      delete filtered.title
    }
    if (protectedFields.includes('artist')) {
      delete filtered.artist
    }
    if (protectedFields.includes('album')) {
      delete filtered.album
    }
    if (protectedFields.includes('cover')) {
      delete filtered.cover
    }
    next.metaSources = Object.keys(filtered).length > 0 ? filtered : undefined
  }
  return next
}

const sameUserEditedFields = (
  left: UserEditedField[] | undefined,
  right: UserEditedField[] | undefined,
): boolean => {
  const a = left ?? []
  const b = right ?? []
  if (a.length !== b.length) {
    return false
  }
  return a.every((field) => b.includes(field))
}

const unionUserEditedFields = (
  existing: UserEditedField[] | undefined,
  extra: UserEditedField[],
): UserEditedField[] | undefined => {
  const next: UserEditedField[] = [...(existing ?? [])]
  for (const field of extra) {
    if (USER_EDITED_FIELDS.includes(field) && !next.includes(field)) {
      next.push(field)
    }
  }
  return next.length > 0 ? next : undefined
}

const encodeStablePart = (value: string): string => {
  return encodeURIComponent(value).replace(/%/g, '_')
}

export const createSongId = (sourceId: string, path: string): string => {
  return `song:${encodeStablePart(sourceId)}:${encodeStablePart(path)}`
}

/** local-first-v1 一次性迁移标记：清除存量自动在线数据后打标，不重复执行 */
const LOCAL_FIRST_MIGRATION_KEY = 'muses:migration:local-first-v1'

/**
 * 单曲清洗（R6）：清除播放器自动在线匹配的存量数据。
 * - lyricsSource === 'online' 且歌词未被手改 → 清除 lyrics/lyricsFormat/lyricsSource
 * - metaSources[field] === 'cloud' 且字段未被手改 → 清除字段值与标记（cover 同时清 coverUri；
 *   title 为必填字段，回退到文件名兜底标题，与扫描无内嵌标题时的语义一致）
 * userEditedFields 命中的字段跳过（manual 保护优先于清理）；无变化返回原对象。
 */
const migrateSongForLocalFirst = (song: SongItem): SongItem => {
  const edited = song.userEditedFields ?? []
  let next = song
  let changed = false

  if (song.lyricsSource === 'online' && !edited.includes('lyrics')) {
    next = { ...next, lyrics: undefined, lyricsFormat: undefined, lyricsSource: undefined }
    changed = true
  }

  const metaSources = next.metaSources
  if (metaSources) {
    const rest: MetaSources = { ...metaSources }
    const valuePatch: Partial<Pick<SongItem, 'title' | 'artist' | 'album' | 'coverUri'>> = {}
    let metaChanged = false
    for (const field of ['title', 'artist', 'album', 'cover'] as const) {
      if (rest[field] !== 'cloud' || edited.includes(field)) {
        continue
      }
      delete rest[field]
      if (field === 'cover') {
        valuePatch.coverUri = undefined
      } else if (field === 'title') {
        // title 必填：回退文件名兜底标题，避免整条记录因缺 title 被丢弃
        valuePatch.title = getTitleFromPath(song.path)
      } else {
        valuePatch[field] = undefined
      }
      metaChanged = true
    }
    if (metaChanged) {
      next = {
        ...next,
        ...valuePatch,
        metaSources: Object.keys(rest).length > 0 ? rest : undefined,
      }
      changed = true
    }
  }

  return changed ? next : song
}

/**
 * 存量清理迁移（R6 / design §3）：首次 loadSongs 时执行一次。
 * 清理不可逆（无备份）；manual 保护字段保留；有变化才写库并广播 SONGS_UPDATED_EVENT。
 */
const runLocalFirstMigrationIfNeeded = (): void => {
  if (typeof localStorage === 'undefined') {
    return
  }
  let done: string | null = null
  try {
    done = localStorage.getItem(LOCAL_FIRST_MIGRATION_KEY)
  } catch {
    return
  }
  if (done === 'done') {
    return
  }

  const songs = parseStoredSongs()
  const cleaned = songs.map(migrateSongForLocalFirst)
  const changed = cleaned.some((song, index) => song !== songs[index])
  if (changed) {
    saveSongs(cleaned)
  }
  try {
    localStorage.setItem(LOCAL_FIRST_MIGRATION_KEY, 'done')
  } catch {
    // 打标失败：下次启动会重跑一次；清洗是幂等的，重复执行无害
  }
}

/** 从 localStorage 解析曲库（校验 + 封面 URI 消毒），不做迁移 */
const parseStoredSongs = (): SongItem[] => {
  const rawValue = localStorage.getItem(SONGS_STORAGE_KEY)
  if (!rawValue) {
    return []
  }

  try {
    const parsedValue: unknown = JSON.parse(rawValue)
    if (!Array.isArray(parsedValue)) {
      return []
    }

    return parsedValue.filter(isSongItem).map((song) => {
      const safeCover = sanitizeCoverUri(song.coverUri)
      if (safeCover === song.coverUri) {
        return song
      }
      return safeCover ? { ...song, coverUri: safeCover } : { ...song, coverUri: undefined }
    })
  } catch {
    return []
  }
}

export const loadSongs = (): SongItem[] => {
  runLocalFirstMigrationIfNeeded()
  return parseStoredSongs()
}

const sanitizeReplayGainTrackDb = (value: number | undefined): number | undefined => {
  if (value === undefined || !Number.isFinite(value)) {
    return undefined
  }
  return value
}

const sanitizeSongForStorage = (song: SongItem): SongItem => {
  const { coverUri, replayGainTrackDb, userEditedFields, metaSources, ...rest } = song
  const safeCover = sanitizeCoverUri(coverUri)
  const safeGain = sanitizeReplayGainTrackDb(replayGainTrackDb)
  const safeEdited = sanitizeUserEditedFields(userEditedFields)
  const safeMetaSources = sanitizeMetaSources(metaSources)
  return {
    ...rest,
    ...(safeCover ? { coverUri: safeCover } : {}),
    ...(safeGain !== undefined ? { replayGainTrackDb: safeGain } : {}),
    ...(safeEdited ? { userEditedFields: safeEdited } : {}),
    ...(safeMetaSources ? { metaSources: safeMetaSources } : {}),
  }
}

const notifySongsUpdated = (): void => {
  if (typeof window === 'undefined') {
    return
  }

  window.dispatchEvent(new CustomEvent(SONGS_UPDATED_EVENT))
}

export const saveSongs = (songs: SongItem[]): void => {
  localStorage.setItem(SONGS_STORAGE_KEY, JSON.stringify(songs.map(sanitizeSongForStorage)))
  notifySongsUpdated()
}

export type UpsertSongStatus = 'inserted' | 'updated' | 'skipped'

export interface UpsertSongInput {
  sourceId: string
  sourceType: SongItem['sourceType']
  path: string
  uri: string
  title: string
  tags?: AudioTags
  now?: string
}

export interface UpsertSongResult {
  status: UpsertSongStatus
  song: SongItem
  songs: SongItem[]
}

export interface StorageMutationOptions {
  /** 批处理时关闭单次写入；默认 true，保持独立调用的既有行为。 */
  persist?: boolean
}

const sameMetaSources = (
  left: MetaSources | undefined,
  right: MetaSources | undefined,
): boolean => {
  const a = left ?? {}
  const b = right ?? {}
  const keys = new Set([...Object.keys(a), ...Object.keys(b)])
  for (const key of keys) {
    if ((a[key as MetaFieldKey] ?? 'embedded') !== (b[key as MetaFieldKey] ?? 'embedded')) {
      return false
    }
  }
  return true
}

const hasSongChanged = (left: SongItem, right: SongItem): boolean => {
  return (
    left.uri !== right.uri ||
    left.title !== right.title ||
    left.artist !== right.artist ||
    left.album !== right.album ||
    left.duration !== right.duration ||
    left.lyrics !== right.lyrics ||
    left.lyricsSource !== right.lyricsSource ||
    left.lyricsFormat !== right.lyricsFormat ||
    left.coverUri !== right.coverUri ||
    left.replayGainTrackDb !== right.replayGainTrackDb ||
    !sameUserEditedFields(left.userEditedFields, right.userEditedFields) ||
    !sameMetaSources(left.metaSources, right.metaSources) ||
    left.tagsScanned !== right.tagsScanned ||
    left.tagsScannedAt !== right.tagsScannedAt ||
    left.metadataVersion !== right.metadataVersion
  )
}

/**
 * 合并字段来源（R1-2）：保留旧来源，仅当 tags 带了新来源且非空才覆盖对应 key。
 * 来源不写空；手改字段来源已在 applyTagsRespectingUserEdits 剥离。
 */
const mergeMetaSources = (
  previous: MetaSources | undefined,
  incoming: MetaSources | undefined,
): MetaSources | undefined => {
  if (!incoming || Object.keys(incoming).length === 0) {
    return previous
  }
  const next: MetaSources = { ...(previous ?? {}) }
  for (const [key, src] of Object.entries(incoming)) {
    if (isMetaFieldKeyValue(key) && isFieldSourceValue(src)) {
      next[key] = src
    }
  }
  return Object.keys(next).length > 0 ? next : undefined
}

export const upsertSong = (
  input: UpsertSongInput,
  existingSongs = loadSongs(),
  options: StorageMutationOptions = {},
): UpsertSongResult => {
  const persist = options.persist !== false
  const now = input.now ?? new Date().toISOString()
  const existingIndex = existingSongs.findIndex((song) => song.sourceId === input.sourceId && song.path === input.path)
  const tags = input.tags ?? {}

  if (existingIndex < 0) {
    const song: SongItem = {
      id: createSongId(input.sourceId, input.path),
      sourceId: input.sourceId,
      sourceType: input.sourceType,
      path: input.path,
      uri: input.uri,
      title: tags.title?.trim() || input.title,
      artist: tags.artist,
      album: tags.album,
      duration: tags.duration,
      lyrics: tags.lyrics,
      lyricsSource: tags.lyricsSource,
      lyricsFormat: tags.lyricsFormat,
      coverUri: sanitizeCoverUri(tags.coverUri),
      replayGainTrackDb: sanitizeReplayGainTrackDb(tags.replayGainTrackDb),
      metaSources: sanitizeMetaSources(tags.metaSources),
      tagsScanned: tags.tagsScanned,
      tagsScannedAt: tags.tagsScannedAt,
      metadataVersion: tags.metadataVersion,
      createdAt: now,
      updatedAt: now,
    }
    const songs = [song, ...existingSongs]
    if (persist) {
      saveSongs(songs)
    }
    return { status: 'inserted', song, songs }
  }

  const previousSong = existingSongs[existingIndex]
  // 自动 upsert：用户手改字段永久保护（扫描/在线/预取统一受益）
  const safeTags = applyTagsRespectingUserEdits(previousSong, tags)
  const nextSong: SongItem = {
    ...previousSong,
    uri: input.uri,
    // title 保护时保留用户值；input.title 常为文件名兜底，不得冲掉手改
    title: isUserEditedField(previousSong, 'title')
      ? previousSong.title
      : (safeTags.title?.trim() || input.title),
    artist: safeTags.artist ?? previousSong.artist,
    album: safeTags.album ?? previousSong.album,
    duration: safeTags.duration ?? previousSong.duration,
    lyrics: safeTags.lyrics ?? previousSong.lyrics,
    lyricsSource: safeTags.lyricsSource ?? previousSong.lyricsSource,
    lyricsFormat: safeTags.lyricsFormat ?? previousSong.lyricsFormat,
    coverUri: safeTags.coverUri === undefined
      ? sanitizeCoverUri(previousSong.coverUri)
      : sanitizeCoverUri(safeTags.coverUri),
    // 有新增益则更新；tags 未带该字段时保留旧值；不写假 0
    replayGainTrackDb: safeTags.replayGainTrackDb === undefined
      ? sanitizeReplayGainTrackDb(previousSong.replayGainTrackDb)
      : sanitizeReplayGainTrackDb(safeTags.replayGainTrackDb),
    metaSources: mergeMetaSources(previousSong.metaSources, safeTags.metaSources),
    // 自动路径不得清除或改写 userEditedFields
    userEditedFields: previousSong.userEditedFields,
    tagsScanned: safeTags.tagsScanned ?? previousSong.tagsScanned,
    tagsScannedAt: safeTags.tagsScannedAt ?? previousSong.tagsScannedAt,
    metadataVersion: safeTags.metadataVersion ?? previousSong.metadataVersion,
    updatedAt: now,
  }

  if (!hasSongChanged(previousSong, nextSong)) {
    return { status: 'skipped', song: previousSong, songs: existingSongs }
  }

  const songs = [...existingSongs]
  songs[existingIndex] = nextSong
  if (persist) {
    saveSongs(songs)
  }
  return { status: 'updated', song: nextSong, songs }
}

/** 用户手改 patch：undefined = 不改该字段；null（仅 RG/封面/歌词）= 清除 */
export interface SongUserEditPatch {
  title?: string
  artist?: string
  album?: string
  /** 安全 file://；null/空串 = 清除封面 */
  coverUri?: string | null
  /** 歌词正文；null/空串 = 清除歌词 */
  lyrics?: string | null
  lyricsFormat?: SongLyricsFormat
  /** track dB；null = 清除 RG 标签语义 */
  replayGainTrackDb?: number | null
}

export interface UpdateSongUserEditResult {
  song: SongItem
  songs: SongItem[]
}

/**
 * 用户编辑写库：更新字段并 union 进 userEditedFields。
 * 仅此路径可改写保护集；扫描/在线 upsert 不得调用。
 */
export const updateSongUserEdit = (
  songId: string,
  patch: SongUserEditPatch,
  existingSongs = loadSongs(),
  options: StorageMutationOptions = {},
): UpdateSongUserEditResult => {
  const persist = options.persist !== false
  const index = existingSongs.findIndex((song) => song.id === songId)
  if (index < 0) {
    throw new Error('找不到要编辑的歌曲。')
  }

  const previous = existingSongs[index]
  const now = new Date().toISOString()
  const edited: UserEditedField[] = []

  let title = previous.title
  if (patch.title !== undefined) {
    const nextTitle = patch.title.trim()
    if (!nextTitle) {
      throw new Error('歌曲标题不能为空。')
    }
    title = nextTitle
    edited.push('title')
  }

  let artist = previous.artist
  if (patch.artist !== undefined) {
    const nextArtist = patch.artist.trim()
    artist = nextArtist || undefined
    edited.push('artist')
  }

  let album = previous.album
  if (patch.album !== undefined) {
    const nextAlbum = patch.album.trim()
    album = nextAlbum || undefined
    edited.push('album')
  }

  let coverUri = previous.coverUri
  if (patch.coverUri !== undefined) {
    if (patch.coverUri === null || !String(patch.coverUri).trim()) {
      coverUri = undefined
    } else {
      coverUri = sanitizeCoverUri(patch.coverUri) ?? undefined
    }
    edited.push('cover')
  }

  let lyrics = previous.lyrics
  let lyricsSource = previous.lyricsSource
  let lyricsFormat = previous.lyricsFormat
  if (patch.lyrics !== undefined) {
    if (patch.lyrics === null || !String(patch.lyrics).trim()) {
      lyrics = undefined
      lyricsSource = undefined
      lyricsFormat = undefined
    } else {
      lyrics = patch.lyrics
      // 用户粘贴视为手改内嵌语义；质量序不得再被在线覆盖
      lyricsSource = 'embedded'
      lyricsFormat = patch.lyricsFormat ?? 'lrc'
    }
    edited.push('lyrics')
  } else if (patch.lyricsFormat !== undefined && previous.lyrics?.trim()) {
    lyricsFormat = patch.lyricsFormat
    edited.push('lyrics')
  }

  let replayGainTrackDb = previous.replayGainTrackDb
  if (patch.replayGainTrackDb !== undefined) {
    if (patch.replayGainTrackDb === null || !Number.isFinite(patch.replayGainTrackDb)) {
      replayGainTrackDb = undefined
    } else {
      replayGainTrackDb = patch.replayGainTrackDb
    }
    edited.push('replayGain')
  }

  // 手改字段从 metaSources 移除对应 key：manual 由 userEditedFields 派生，不双写
  const nextMetaSources: MetaSources | undefined = (() => {
    if (!previous.metaSources) {
      return undefined
    }
    const filtered: MetaSources = { ...previous.metaSources }
    if (edited.includes('title')) {
      delete filtered.title
    }
    if (edited.includes('artist')) {
      delete filtered.artist
    }
    if (edited.includes('album')) {
      delete filtered.album
    }
    if (edited.includes('cover')) {
      delete filtered.cover
    }
    return Object.keys(filtered).length > 0 ? filtered : undefined
  })()

  const nextSong: SongItem = {
    ...previous,
    title,
    artist,
    album,
    coverUri: sanitizeCoverUri(coverUri),
    lyrics,
    lyricsSource,
    lyricsFormat,
    replayGainTrackDb: sanitizeReplayGainTrackDb(replayGainTrackDb),
    metaSources: nextMetaSources,
    userEditedFields: unionUserEditedFields(previous.userEditedFields, edited),
    updatedAt: now,
  }

  const songs = [...existingSongs]
  songs[index] = nextSong
  if (persist) {
    saveSongs(songs)
  }
  return { song: nextSong, songs }
}

export interface ReconcileSourceSongsResult {
  removed: number
  songs: SongItem[]
}

/**
 * 按音源对账：保留其他音源歌曲，以及本音源中 path 属于 keepPaths 的歌曲；
 * 删除本音源中 path 不在 keepPaths 的旧歌曲。有删除时才写库。
 */
export const reconcileSourceSongs = (
  sourceId: string,
  keepPaths: Iterable<string>,
  existingSongs = loadSongs(),
  options: StorageMutationOptions = {},
): ReconcileSourceSongsResult => {
  const keepPathSet = keepPaths instanceof Set ? keepPaths : new Set(keepPaths)
  const nextSongs: SongItem[] = []
  let removed = 0

  for (const song of existingSongs) {
    if (song.sourceId !== sourceId || keepPathSet.has(song.path)) {
      nextSongs.push(song)
      continue
    }
    removed += 1
  }

  if (removed === 0) {
    return { removed: 0, songs: existingSongs }
  }

  if (options.persist !== false) {
    saveSongs(nextSongs)
  }
  return { removed, songs: nextSongs }
}
