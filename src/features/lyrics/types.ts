/** amll-ttml-db 索引行（解析后的结构化字段） */
export interface AmllIndexEntry {
  musicName: string
  artists: string[]
  album?: string
  rawLyricFile: string
}

/** 匹配查询 */
export interface AmllMatchQuery {
  songId: string
  title: string
  artist?: string
  album?: string
}

export type AmllMatchFailReason = 'no-match' | 'network' | 'parse' | 'aborted'

export type AmllMatchResult =
  | {
      ok: true
      ttml: string
      rawLyricFile: string
      score: number
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
