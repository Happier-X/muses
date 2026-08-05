import type { OnlineCoverSource } from '@/features/cover/types'
import type { OnlineLyricsFormat, OnlineLyricsSource } from '@/features/lyrics/providers/types'
import type { TextMetaHit } from '@/features/metadata/types'

/** 编辑页云端查询：以当前表单/种子字段为关键词，强制搜索 */
export type EditCloudMetaQuery = {
  songId: string
  title: string
  artist?: string
  album?: string
  /** 秒；歌词源可用 */
  durationSec?: number
}

export type EditDimStatus = 'ok' | 'no-match' | 'network' | 'aborted'

export type EditCoverCandidate = {
  remoteUrl: string
  source: OnlineCoverSource
}

export type EditLyricsCandidate = {
  text: string
  format: OnlineLyricsFormat
  source: OnlineLyricsSource
  translationText?: string
}

export type EditDimResult<T> = {
  status: EditDimStatus
  items: T[]
  /** 最优下标；无结果为 0 */
  defaultIndex: number
}

export type EditCloudMetaResult = {
  text: EditDimResult<TextMetaHit>
  cover: EditDimResult<EditCoverCandidate>
  lyrics: EditDimResult<EditLyricsCandidate>
}

export type SearchEditCloudMetaOptions = {
  signal?: AbortSignal
  /** 每维最多保留候选数，默认 8 */
  maxCandidates?: number
}
