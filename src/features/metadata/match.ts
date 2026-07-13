import { kgTextMetaProvider } from './providers/kg'
import { kwTextMetaProvider } from './providers/kw'
import { mgTextMetaProvider } from './providers/mg'
import { txTextMetaProvider } from './providers/tx'
import { wyTextMetaProvider } from './providers/wy'
import type { OnlineTextMatchResult, OnlineTextQuery, TextMetaProvider } from './types'
import { hitFillsMissing, needsOnlineTextMeta } from './util'

const NEGATIVE_CACHE_TTL_MS = 45 * 60 * 1000

type NegativeEntry = {
  queryKey: string
  expiresAt: number
}

const negativeBySongId = new Map<string, NegativeEntry>()

const buildQueryKey = (query: OnlineTextQuery): string =>
  JSON.stringify([
    query.title.trim(),
    query.artist?.trim() || '',
    query.album?.trim() || '',
  ])

/** 默认链：kw → tx → wy → kg → mg（对齐 any-listen 国内段） */
const defaultProviders: TextMetaProvider[] = [
  kwTextMetaProvider,
  txTextMetaProvider,
  wyTextMetaProvider,
  kgTextMetaProvider,
  mgTextMetaProvider,
]

let providersOverride: TextMetaProvider[] | null = null

export const resetOnlineTextMetaCache = (): void => {
  negativeBySongId.clear()
}

export const setOnlineTextMetaProvidersForTest = (providers: TextMetaProvider[] | null): void => {
  providersOverride = providers
}

/**
 * 多源编排：kw → tx → wy → kg → mg；返回可补缺的文本 hit（不写库）。
 */
export const matchOnlineTextMeta = async (
  query: OnlineTextQuery,
  providers: TextMetaProvider[] = providersOverride ?? defaultProviders,
): Promise<OnlineTextMatchResult> => {
  const title = query.title?.trim()
  if (!title) {
    return { ok: false, reason: 'no-match' }
  }

  if (!needsOnlineTextMeta(query)) {
    return { ok: false, reason: 'not-needed' }
  }

  const queryKey = buildQueryKey(query)
  const cached = negativeBySongId.get(query.songId)
  if (cached && cached.queryKey === queryKey && cached.expiresAt > Date.now()) {
    return { ok: false, reason: 'no-match' }
  }

  let sawNetworkError = false

  for (const provider of providers) {
    try {
      const hit = await provider.search(query)
      if (hit && hitFillsMissing(hit, query)) {
        return { ok: true, hit: { ...hit, source: provider.id } }
      }
    } catch {
      sawNetworkError = true
    }
  }

  negativeBySongId.set(query.songId, {
    queryKey,
    expiresAt: Date.now() + NEGATIVE_CACHE_TTL_MS,
  })

  return { ok: false, reason: sawNetworkError ? 'network' : 'no-match' }
}
