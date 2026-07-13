/** 在线封面匹配查询 */
export type OnlineCoverQuery = {
  songId: string
  title: string
  artist?: string
  album?: string
}

export type OnlineCoverSource = 'itunes' | 'kw' | 'mg' | 'kg' | 'tx' | 'wy'

export type OnlineCoverMatchOk = {
  ok: true
  remoteUrl: string
  source: OnlineCoverSource
}

export type OnlineCoverMatchFail = {
  ok: false
  reason: 'no-match' | 'network' | 'aborted'
}

export type OnlineCoverMatchResult = OnlineCoverMatchOk | OnlineCoverMatchFail

export type CoverProvider = {
  id: OnlineCoverSource
  searchCoverUrl: (query: OnlineCoverQuery) => Promise<string | null>
}
