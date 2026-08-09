/**
 * 编辑页强制云端搜索 + 多候选编排。
 * 与播放静默 matchOnline* 分离：不读 not-needed / 补空过滤 / 播放负缓存。
 */
import { itunesCoverProvider } from '@/features/cover/providers/itunes'
import { kgCoverProvider } from '@/features/cover/providers/kg'
import { kwCoverProvider } from '@/features/cover/providers/kw'
import { mgCoverProvider } from '@/features/cover/providers/mg'
import { txCoverProvider } from '@/features/cover/providers/tx'
import { wyCoverProvider } from '@/features/cover/providers/wy'
import type { CoverProvider } from '@/features/cover/types'
import { matchAmllTtmlLyrics } from '@/features/lyrics/amllTtmlDb'
import { getOnlineLyricsFallbackProviders } from '@/features/lyrics/match'
import { normalizeText } from '@/features/lyrics/normalize'
import type { OnlineLyricsQuery, LyricsProvider } from '@/features/lyrics/providers/types'
import { kgTextMetaProvider } from '@/features/metadata/providers/kg'
import { kwTextMetaProvider } from '@/features/metadata/providers/kw'
import { mgTextMetaProvider } from '@/features/metadata/providers/mg'
import { txTextMetaProvider } from '@/features/metadata/providers/tx'
import { wyTextMetaProvider } from '@/features/metadata/providers/wy'
import type { OnlineTextQuery, TextMetaHit, TextMetaProvider } from '@/features/metadata/types'
import { scoreTextHit } from '@/features/metadata/util'
import type {
  CloudPlatformId,
  EditCloudMetaQuery,
  EditCloudMetaResult,
  EditCoverCandidate,
  EditDimResult,
  EditDimStatus,
  EditLyricsCandidate,
  SearchEditCloudMetaOptions,
} from './types'

const DEFAULT_MAX_CANDIDATES = 8

const defaultTextProviders: TextMetaProvider[] = [
  kwTextMetaProvider,
  txTextMetaProvider,
  wyTextMetaProvider,
  kgTextMetaProvider,
  mgTextMetaProvider,
]

const defaultCoverProviders: CoverProvider[] = [
  itunesCoverProvider,
  kwCoverProvider,
  txCoverProvider,
  wyCoverProvider,
  kgCoverProvider,
  mgCoverProvider,
]

/** 平台 → 文本 provider（itunes 无文本） */
const platformTextProviders: Record<Exclude<CloudPlatformId, 'all'>, TextMetaProvider[]> = {
  wy: [wyTextMetaProvider],
  tx: [txTextMetaProvider],
  kg: [kgTextMetaProvider],
  kw: [kwTextMetaProvider],
  mg: [mgTextMetaProvider],
  itunes: [],
}

/** 平台 → 封面 provider */
const platformCoverProviders: Record<Exclude<CloudPlatformId, 'all'>, CoverProvider[]> = {
  wy: [wyCoverProvider],
  tx: [txCoverProvider],
  kg: [kgCoverProvider],
  kw: [kwCoverProvider],
  mg: [mgCoverProvider],
  itunes: [itunesCoverProvider],
}

/** 平台 → 歌词 provider id（tx 平台含 qrc；lrclib 仅全部时使用） */
const platformLyricsIds: Record<Exclude<CloudPlatformId, 'all'>, string[]> = {
  wy: ['wy'],
  tx: ['tx', 'qrc'],
  kg: ['kg'],
  kw: ['kw'],
  mg: ['mg'],
  itunes: [],
}

const throwIfAborted = (signal?: AbortSignal): void => {
  if (signal?.aborted) {
    const err = new Error('aborted')
    err.name = 'AbortError'
    throw err
  }
}

const isAbortError = (error: unknown): boolean => {
  if (!error || typeof error !== 'object') {
    return false
  }
  const name = (error as { name?: string }).name
  return name === 'AbortError'
}

const emptyDim = <T>(): EditDimResult<T> => ({
  status: 'no-match',
  items: [],
  defaultIndex: 0,
})

const finalizeDim = <T>(
  items: T[],
  sawNetwork: boolean,
  aborted: boolean,
): EditDimResult<T> => {
  if (aborted && items.length === 0) {
    return { status: 'aborted', items: [], defaultIndex: 0 }
  }
  if (items.length > 0) {
    return { status: 'ok', items, defaultIndex: 0 }
  }
  if (aborted) {
    return { status: 'aborted', items: [], defaultIndex: 0 }
  }
  return {
    status: sawNetwork ? 'network' : 'no-match',
    items: [],
    defaultIndex: 0,
  }
}

const textDedupKey = (hit: TextMetaHit): string =>
  [
    normalizeText(hit.title),
    normalizeText(hit.artist),
    normalizeText(hit.album),
    hit.source,
  ].join('\u0001')

const rankAndCapText = (
  hits: TextMetaHit[],
  query: OnlineTextQuery,
  max: number,
): TextMetaHit[] => {
  const seen = new Set<string>()
  const unique: TextMetaHit[] = []
  for (const hit of hits) {
    const key = textDedupKey(hit)
    if (seen.has(key)) {
      continue
    }
    seen.add(key)
    unique.push(hit)
  }
  unique.sort((a, b) => scoreTextHit(b, query) - scoreTextHit(a, query))
  return unique.slice(0, max)
}

const searchTextDimension = async (
  query: OnlineTextQuery,
  max: number,
  providers: TextMetaProvider[],
  signal?: AbortSignal,
): Promise<EditDimResult<TextMetaHit>> => {
  const collected: TextMetaHit[] = []
  let sawNetwork = false
  let aborted = false

  for (const provider of providers) {
    try {
      throwIfAborted(signal)
      const hit = await provider.search(query)
      throwIfAborted(signal)
      if (hit && (hit.title?.trim() || hit.artist?.trim() || hit.album?.trim())) {
        collected.push({ ...hit, source: provider.id })
      }
    } catch (error) {
      if (isAbortError(error)) {
        aborted = true
        break
      }
      sawNetwork = true
    }
  }

  const items = rankAndCapText(collected, query, max)
  return finalizeDim(items, sawNetwork, aborted)
}

const searchCoverDimension = async (
  query: EditCloudMetaQuery,
  max: number,
  providers: CoverProvider[],
  signal?: AbortSignal,
): Promise<EditDimResult<EditCoverCandidate>> => {
  const coverQuery = {
    songId: query.songId,
    title: query.title,
    artist: query.artist,
    album: query.album,
  }
  const collected: EditCoverCandidate[] = []
  const seenUrls = new Set<string>()
  let sawNetwork = false
  let aborted = false

  for (const provider of providers) {
    if (collected.length >= max) {
      break
    }
    try {
      throwIfAborted(signal)
      const remoteUrl = await provider.searchCoverUrl(coverQuery)
      throwIfAborted(signal)
      const url = remoteUrl?.trim()
      if (!url || !/^https?:\/\//i.test(url)) {
        continue
      }
      const key = url.toLowerCase()
      if (seenUrls.has(key)) {
        continue
      }
      seenUrls.add(key)
      collected.push({ remoteUrl: url, source: provider.id })
    } catch (error) {
      if (isAbortError(error)) {
        aborted = true
        break
      }
      sawNetwork = true
    }
  }

  return finalizeDim(collected.slice(0, max), sawNetwork, aborted)
}

const searchLyricsDimension = async (
  query: EditCloudMetaQuery,
  max: number,
  lyricsProviders: LyricsProvider[],
  includeAmll: boolean,
  signal?: AbortSignal,
): Promise<EditDimResult<EditLyricsCandidate>> => {
  const lyricsQuery: OnlineLyricsQuery = {
    songId: query.songId,
    title: query.title,
    artist: query.artist,
    album: query.album,
    duration: query.durationSec,
  }
  const collected: EditLyricsCandidate[] = []
  const seen = new Set<string>()
  let sawNetwork = false
  let aborted = false

  const pushHit = (item: EditLyricsCandidate): void => {
    const text = item.text?.trim()
    if (!text) {
      return
    }
    const key = `${item.source}\u0001${item.format}\u0001${text.slice(0, 120)}`
    if (seen.has(key)) {
      return
    }
    seen.add(key)
    collected.push({
      text,
      format: item.format,
      source: item.source,
      ...(item.translationText?.trim()
        ? { translationText: item.translationText.trim() }
        : {}),
    })
  }

  if (includeAmll) {
    try {
      throwIfAborted(signal)
      const amll = await matchAmllTtmlLyrics({
        songId: lyricsQuery.songId,
        title: lyricsQuery.title,
        artist: lyricsQuery.artist,
        album: lyricsQuery.album,
      })
      throwIfAborted(signal)
      if (amll.ok) {
        pushHit({
          text: amll.ttml,
          format: 'ttml',
          source: 'amll',
        })
      } else if (amll.reason === 'network') {
        sawNetwork = true
      }
    } catch (error) {
      if (isAbortError(error)) {
        aborted = true
      } else {
        sawNetwork = true
      }
    }
  }

  if (!aborted) {
    for (const provider of lyricsProviders) {
      if (collected.length >= max) {
        break
      }
      try {
        throwIfAborted(signal)
        const hit = await provider.searchLyrics(lyricsQuery)
        throwIfAborted(signal)
        const text = hit?.text?.trim()
        if (text && hit) {
          pushHit({
            text,
            format: hit.format,
            source: provider.id,
            translationText: hit.translationText,
          })
        }
      } catch (error) {
        if (isAbortError(error)) {
          aborted = true
          break
        }
        sawNetwork = true
      }
    }
  }

  // 质量粗排：ttml/yrc/qrc 优先于 lrc，同级保持收集顺序
  const formatRank = (format: string): number => {
    if (format === 'ttml' || format === 'yrc' || format === 'qrc') {
      return 2
    }
    if (format === 'lrc') {
      return 1
    }
    return 0
  }
  collected.sort((a, b) => formatRank(b.format) - formatRank(a.format))

  return finalizeDim(collected.slice(0, max), sawNetwork, aborted)
}

/**
 * 并行拉取文本 / 封面 / 歌词多候选（编辑强制搜）。
 * 不写库、不落盘、不碰 playerState。
 */
export const searchEditCloudMeta = async (
  query: EditCloudMetaQuery,
  options: SearchEditCloudMetaOptions = {},
): Promise<EditCloudMetaResult> => {
  const title = query.title?.trim()
  const songId = query.songId?.trim()
  const max = Math.max(1, options.maxCandidates ?? DEFAULT_MAX_CANDIDATES)
  const signal = options.signal
  const platform = options.platform ?? 'all'

  if (!title || !songId) {
    return {
      text: emptyDim(),
      cover: emptyDim(),
      lyrics: emptyDim(),
    }
  }

  const textQuery: OnlineTextQuery = {
    songId,
    title,
    artist: query.artist?.trim() || undefined,
    album: query.album?.trim() || undefined,
  }

  const editQuery: EditCloudMetaQuery = {
    songId,
    title,
    artist: textQuery.artist,
    album: textQuery.album,
    durationSec: query.durationSec,
  }

  // 平台过滤：选具体平台时只用该平台 provider；
  // 歌词限定平台时跳过 amll 聚合库（保持纯平台来源）
  const textProviders = platform === 'all'
    ? defaultTextProviders
    : platformTextProviders[platform]
  const coverProviders = platform === 'all'
    ? defaultCoverProviders
    : platformCoverProviders[platform]
  let lyricsProviders = getOnlineLyricsFallbackProviders()
  let includeAmll = true
  if (platform !== 'all') {
    const ids = new Set(platformLyricsIds[platform])
    lyricsProviders = lyricsProviders.filter((provider) => ids.has(provider.id))
    includeAmll = false
  }

  const [text, cover, lyrics] = await Promise.all([
    searchTextDimension(textQuery, max, textProviders, signal),
    searchCoverDimension(editQuery, max, coverProviders, signal),
    searchLyricsDimension(editQuery, max, lyricsProviders, includeAmll, signal),
  ])

  // 顶层若已 abort，把仍为 no-match 的空维标成 aborted（可选一致性）
  const markAborted = <T>(dim: EditDimResult<T>): EditDimResult<T> => {
    if (signal?.aborted && dim.items.length === 0 && dim.status === 'no-match') {
      return { ...dim, status: 'aborted' as EditDimStatus }
    }
    return dim
  }

  return {
    text: markAborted(text),
    cover: markAborted(cover),
    lyrics: markAborted(lyrics),
  }
}
