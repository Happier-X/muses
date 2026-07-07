import type { SourceType } from '@/features/sources/types'

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
  createdAt: string
  updatedAt: string
}

export interface AudioFileEntry {
  path: string
  uri: string
  name: string
}

export interface AudioTags {
  title?: string
  artist?: string
  album?: string
  duration?: number
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
