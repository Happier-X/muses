import type { MatchConfidence } from './score'

/** amll-ttml-db 索引行（解析后的结构化字段） */
export interface AmllIndexEntry {
  musicName: string
  artists: string[]
  album?: string
  /** 曲目时长（秒）；索引行可选携带，用于匹配质量时长约束（child4） */
  duration?: number
  rawLyricFile: string
}

/** 匹配查询 */
export interface AmllMatchQuery {
  songId: string
  title: string
  artist?: string
  album?: string
  /** 查询曲目时长（秒）；可选，参与匹配质量时长约束（child4） */
  duration?: number
}

export type AmllMatchFailReason = 'no-match' | 'network' | 'parse' | 'aborted'

export type AmllMatchResult =
  | {
      ok: true
      ttml: string
      rawLyricFile: string
      score: number
      /** 命中置信度（child4 R4-3）：来自 findBestMatch；自动写库路径应校验为 'high' */
      confidence?: MatchConfidence
    }
  | {
      ok: false
      reason: AmllMatchFailReason
    }

/** 原始 jsonl 行：metadata 为 [key, values[]] 对 */
export interface AmllRawIndexLine {
  metadata?: Array<[string, string[]] | unknown>
  rawLyricFile?: string
}
