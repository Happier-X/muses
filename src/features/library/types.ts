import type { SourceType } from '@/features/sources/types'

export type LyricsSource = 'embedded' | 'sidecar' | 'online'

/** 持久化歌词格式；与 playerState.lyricsFormat 对齐（不含 null） */
export type SongLyricsFormat = 'lrc' | 'ttml' | 'yrc' | 'qrc'

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
