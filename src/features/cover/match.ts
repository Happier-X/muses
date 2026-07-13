import { itunesCoverProvider } from './providers/itunes'
import { kwCoverProvider } from './providers/kw'
import { mgCoverProvider } from './providers/mg'
import type { CoverProvider, OnlineCoverMatchResult, OnlineCoverQuery } from './types'

const NEGATIVE_CACHE_TTL_MS = 45 * 60 * 1000

type NegativeEntry = {
  queryKey: string
  expiresAt: number
}

const negativeBySongId = new Map<string, NegativeEntry>()

const buildQueryKey = (query: OnlineCoverQuery): string =>
  JSON.stringify([query.title.trim(), query.artist?.trim() || '', query.album?.trim() || ''])

/** 默认链：iTunes → 酷我 → 咪咕 */
const defaultProviders: CoverProvider[] = [itunesCoverProvider, kwCoverProvider, mgCoverProvider]

/** 测试用：可注入的 provider 列表覆盖；null 表示用默认 */
let providersOverride: CoverProvider[] | null = null

/** 测试用：清空负缓存 */
export const resetOnlineCoverCache = (): void => {
  negativeBySongId.clear()
}

/** 测试用：覆盖默认 provider 顺序 */
export const setOnlineCoverProvidersForTest = (providers: CoverProvider[] | null): void => {
  providersOverride = providers
}

/**
 * 多源编排：iTunes → kw → mg；返回远程封面 URL（不落盘）。
 */
export const matchOnlineCoverRemote = async (
  query: OnlineCoverQuery,
  providers: CoverProvider[] = providersOverride ?? defaultProviders,
): Promise<OnlineCoverMatchResult> => {
  const title = query.title?.trim()
  if (!title) {
    return { ok: false, reason: 'no-match' }
  }

  const queryKey = buildQueryKey(query)
  const cached = negativeBySongId.get(query.songId)
  if (cached && cached.queryKey === queryKey && cached.expiresAt > Date.now()) {
    return { ok: false, reason: 'no-match' }
  }

  let sawNetworkError = false

  for (const provider of providers) {
    try {
      const remoteUrl = await provider.searchCoverUrl(query)
      if (remoteUrl && /^https?:\/\//i.test(remoteUrl)) {
        return { ok: true, remoteUrl, source: provider.id }
      }
    } catch {
      sawNetworkError = true
      // 尝试下一源
    }
  }

  negativeBySongId.set(query.songId, {
    queryKey,
    expiresAt: Date.now() + NEGATIVE_CACHE_TTL_MS,
  })

  return { ok: false, reason: sawNetworkError ? 'network' : 'no-match' }
}
