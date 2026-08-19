import type { MatchConfidence } from '@/features/lyrics/score'

/** 在线文本元信息查询 */
export type OnlineTextQuery = {
  songId: string
  title: string
  /** 用于弱 title 判定（与去扩展名文件名比较） */
  path?: string
  artist?: string
  album?: string
  /** 查询曲目时长（秒）；可选，参与匹配质量时长约束（child4） */
  duration?: number
  /**
   * 可选：现字段来源标记（child4 R4-2）。
   * 用于 needsOnlineTextMeta 的 cloud 来源再补约束；服务层 search 不读此字段。
   */
  metaSources?: { title?: 'embedded' | 'cloud' | 'manual'; artist?: 'embedded' | 'cloud' | 'manual'; album?: 'embedded' | 'cloud' | 'manual' }
}

export type OnlineTextSource = 'kw' | 'tx' | 'wy' | 'kg' | 'mg'

export type TextMetaHit = {
  title?: string
  artist?: string
  album?: string
  source: OnlineTextSource
}

export type OnlineTextMatchOk = {
  ok: true
  hit: TextMetaHit
  /**
   * 命中置信度（child4 R4-2）：自动写库路径应校验为 'high'，低置信进候选供刮削页人工选择。
   * 字段为可选，向后兼容旧调用方（缺省视为 'high'）。
   */
  confidence?: MatchConfidence
}

export type OnlineTextMatchFail = {
  ok: false
  reason: 'no-match' | 'network' | 'not-needed'
}

export type OnlineTextMatchResult = OnlineTextMatchOk | OnlineTextMatchFail

export type TextMetaProvider = {
  id: OnlineTextSource
  search: (query: OnlineTextQuery) => Promise<TextMetaHit | null>
}
