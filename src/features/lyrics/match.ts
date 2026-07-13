/**
 * 在线歌词编排：amll TTML → 可插拔 fallback providers（平台 / LRCLIB）。
 * 不写库；由 controller 写入 playerState。
 */
import { matchAmllTtmlLyrics } from './amllTtmlDb'
import type {
  LyricsProvider,
  OnlineLyricsMatchResult,
  OnlineLyricsQuery,
} from './providers/types'

/** 默认回退链：后续子任务注册平台与 LRCLIB；编排交付时为空数组 */
const defaultFallbackProviders: LyricsProvider[] = []

let fallbackOverride: LyricsProvider[] | null = null

export const setOnlineLyricsFallbackProvidersForTest = (
  providers: LyricsProvider[] | null,
): void => {
  fallbackOverride = providers
}

export const getOnlineLyricsFallbackProviders = (): LyricsProvider[] =>
  fallbackOverride ?? defaultFallbackProviders

/**
 * 注册/替换默认回退 providers（应用启动或子任务挂载时调用）。
 * 测试请用 setOnlineLyricsFallbackProvidersForTest。
 */
export const setDefaultOnlineLyricsFallbackProviders = (
  providers: LyricsProvider[],
): void => {
  defaultFallbackProviders.length = 0
  defaultFallbackProviders.push(...providers)
}

/**
 * amll 优先；miss/失败后串行 fallback；任一命中即停。
 */
export const matchOnlineLyrics = async (
  query: OnlineLyricsQuery,
  fallbackProviders: LyricsProvider[] = getOnlineLyricsFallbackProviders(),
): Promise<OnlineLyricsMatchResult> => {
  const title = query.title?.trim()
  const songId = query.songId?.trim()
  if (!title || !songId) {
    return { ok: false, reason: 'no-match' }
  }

  const amll = await matchAmllTtmlLyrics({
    songId,
    title,
    artist: query.artist,
    album: query.album,
  })

  if (amll.ok) {
    return {
      ok: true,
      text: amll.ttml,
      format: 'ttml',
      source: 'amll',
    }
  }

  let sawNetwork = amll.reason === 'network'
  const sawParse = amll.reason === 'parse'

  for (const provider of fallbackProviders) {
    try {
      const hit = await provider.searchLyrics(query)
      const text = hit?.text?.trim()
      if (text) {
        return {
          ok: true,
          text,
          format: hit!.format === 'ttml' ? 'ttml' : 'lrc',
          source: provider.id,
        }
      }
    } catch {
      sawNetwork = true
    }
  }

  if (sawNetwork) {
    return { ok: false, reason: 'network' }
  }
  if (sawParse && fallbackProviders.length === 0) {
    return { ok: false, reason: 'parse' }
  }
  return { ok: false, reason: 'no-match' }
}
