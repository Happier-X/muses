import type { SourceType } from '@/features/sources/types'

export type LyricsSource = 'embedded' | 'sidecar' | 'online'

/** 持久化歌词格式；与 playerState.lyricsFormat 对齐（不含 null） */
export type SongLyricsFormat = 'lrc' | 'ttml' | 'yrc' | 'qrc'

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
