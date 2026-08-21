import type { SourceType } from '@/features/sources/types'

/**
 * 歌词来源：
 * - embedded：音频内嵌歌词（含用户手改粘贴）
 * - sidecar：同目录同名 .lrc 文件
 * - scrape：刮削页写回但文件写入失败（仅库内展示）
 * - online：历史遗留值；播放器已不再产生，存量由 local-first-v1 迁移清除
 */
export type LyricsSource = 'embedded' | 'sidecar' | 'scrape' | 'online'

/** 持久化歌词格式；与 playerState.lyricsFormat 对齐（不含 null） */
export type SongLyricsFormat = 'lrc' | 'ttml' | 'yrc' | 'qrc'

/**
 * 字段来源追踪（R1）：title/artist/album/cover 的最近一次写入方。
 * - embedded：值来自音频文件内置 tag（扫描/懒扫/刮削写回文件成功）
 * - scrape：刮削页写回但尚未写入文件（或写回失败），值得重刮
 * - manual：用户手改；派生自 userEditedFields，不单独存储（见 storage.getFieldSource）
 * - cloud：历史遗留值；播放器自动在线补缺已移除，存量由 local-first-v1 迁移清除
 * 歌词沿用既有 lyricsSource，不在此建模。
 */
export type MetaFieldKey = 'title' | 'artist' | 'album' | 'cover'
export type FieldSource = 'embedded' | 'scrape' | 'manual' | 'cloud'
export type MetaSources = Partial<Record<MetaFieldKey, FieldSource>>

/**
 * 用户手改字段保护键。
 * 凡在数组内的字段，扫描 / 在线补缺 / 预取写库均不得覆盖（含清空也保护，避免扫描写回旧值）。
 */
export type UserEditedField = 'title' | 'artist' | 'album' | 'cover' | 'lyrics' | 'replayGain'

export const USER_EDITED_FIELDS: readonly UserEditedField[] = [
  'title',
  'artist',
  'album',
  'cover',
  'lyrics',
  'replayGain',
] as const

export interface SongItem {
  id: string
  sourceId: string
  sourceType: SourceType
  path: string
  uri: string
  title: string
  artist?: string
  album?: string
  duration?: number
  lyrics?: string
  lyricsSource?: LyricsSource
  /** 有 lyrics 时建议写入；缺省兼容旧数据按 lrc */
  lyricsFormat?: SongLyricsFormat
  coverUri?: string
  /** track 级 ReplayGain（dB）；缺省/非法表示无标签，播放不调整 */
  replayGainTrackDb?: number
  /**
   * 字段来源标记（R1）：title/artist/album/cover 的最近一次写入方。
   * 旧数据缺省视为全部 embedded（存量兼容，见 storage.getFieldSource）。
   */
  metaSources?: MetaSources
  /**
   * 用户手改字段集；旧数据缺省视为 []。
   * 仅用户编辑保存路径写入；自动 upsert 不得清除。
   */
  userEditedFields?: UserEditedField[]
  tagsScanned?: boolean
  tagsScannedAt?: string
  metadataVersion?: number
  createdAt: string
  updatedAt: string
}

export interface AudioFileEntry {
  path: string
  uri: string
  name: string
}

export interface AudioMetadataDiagnostic {
  codes?: string[]
}

export interface AudioTags {
  title?: string
  artist?: string
  album?: string
  duration?: number
  lyrics?: string
  lyricsSource?: LyricsSource
  lyricsFormat?: SongLyricsFormat
  coverUri?: string
  /** track 级 ReplayGain（dB）；仅解析成功时存在 */
  replayGainTrackDb?: number
  tagsScanned?: boolean
  tagsScannedAt?: string
  metadataVersion?: number
  metadataDiagnostic?: AudioMetadataDiagnostic
  /**
   * 写入方携带的字段来源标记（R1）：扫描传 embedded、在线补缺/预取传 cloud。
   * upsert 据此更新 SongItem.metaSources；受 applyTagsRespectingUserEdits 保护
   * （手改字段来源被剥离，避免覆盖 manual 派生语义）。
   */
  metaSources?: MetaSources
}

export interface ScanOptions {
  readTags: boolean
}

export type ScanStage = 'idle' | 'discovering' | 'processing' | 'completed' | 'failed'

export interface ScanSummary {
  discovered: number
  processed: number
  inserted: number
  updated: number
  skipped: number
  failed: number
  degraded: number
  removed: number
}

export interface ScanProgress extends ScanSummary {
  stage: ScanStage
  currentItem?: string
  message?: string
}

export interface ScanResult {
  summary: ScanSummary
  songs: SongItem[]
}

export type ScanProgressCallback = (progress: ScanProgress) => void

export interface NativeAudioMetadata extends AudioTags {
  path?: string
  uri?: string
  name?: string
}
