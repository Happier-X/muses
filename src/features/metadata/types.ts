/** 在线文本元信息查询 */
export type OnlineTextQuery = {
  songId: string
  title: string
  /** 用于弱 title 判定（与去扩展名文件名比较） */
  path?: string
  artist?: string
  album?: string
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
