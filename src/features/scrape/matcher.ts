/**
 * 待刮削队列批量匹配聚合（child3 R3-2 / design.md §4.1）。
 *
 * 输入 queue snapshot，resolve 到最新 SongItem，逐曲调用 searchEditCloudMeta，
 * 返回 ScrapeCandidate[]（不写库）。
 *
 * 依赖 child1 queue.ts + child4 置信度函数。
 */
import { searchEditCloudMeta } from '@/features/editMeta/searchEditCloudMeta'
import type { EditCloudMetaResult } from '@/features/editMeta/types'
import type { TextMetaHit } from '@/features/metadata/types'
import { classifyTextMetaConfidence } from '@/features/metadata/util'
import { loadSongs } from '@/features/library/storage'
import type { SongItem } from '@/features/library/types'
import type { ScrapeQueueSnapshot } from './queue'

export type ScrapeCandidateConfidence = 'high' | 'low'

/** 单首歌的匹配结果 */
export interface ScrapeCandidate {
  songId: string
  song: SongItem
  text: {
    current: { title?: string; artist?: string; album?: string }
    candidates: TextMetaHit[]
    /** 当前最优候选下标（-1 = 无候选） */
    defaultIndex: number
  }
  cover: {
    currentUri: string | undefined
    candidates: { remoteUrl: string; source: string }[]
    defaultIndex: number
  }
  lyrics: {
    currentText: string | undefined
    currentFormat: string | undefined
    candidates: { text: string; format: string; source: string }[]
    defaultIndex: number
  }
  /** 三维度综合置信度（用于默认勾选策略） */
  overallConfidence: ScrapeCandidateConfidence
  /** 高置信默认勾选，低置信默认不勾 */
  defaultChecked: boolean
}

/** 匹配进度回调 */
export interface MatchProgress {
  matched: number
  total: number
}

/** 匹配失败的单曲 */
export interface ScrapeMatchError {
  songId: string
  reason: string
}

/** 批量匹配结果 */
export interface ScrapeMatchResult {
  candidates: ScrapeCandidate[]
  errors: ScrapeMatchError[]
}

/** 并发限制（避免同时大量网络请求） */
const MAX_CONCURRENT = 3

/** 单轮最大候选数 */
const MAX_CANDIDATES_PER_DIM = 8

const isTextCandidateHighConfidence = (
  hit: TextMetaHit,
  song: SongItem,
): boolean => {
  const confidence = classifyTextMetaConfidence(
    { title: hit.title, artist: hit.artist, album: hit.album },
    { title: song.title, artist: song.artist },
  )
  return confidence === 'high'
}

/** 综合置信度：文本+封面+歌词三维度中任一高置信即 overall=high */
const deriveOverallConfidence = (
  textCandidates: TextMetaHit[],
  coverCandidates: { remoteUrl: string }[],
  lyricsCandidates: { text: string; format: string }[],
  song: SongItem,
): ScrapeCandidateConfidence => {
  const hasHighText = textCandidates.some((hit) =>
    isTextCandidateHighConfidence(hit, song),
  )
  // 封面/歌词：有候选即视为高质量来源（与文本置信度分开）
  const hasHighCover = coverCandidates.length > 0
  const hasHighLyrics = lyricsCandidates.length > 0
  return hasHighText || hasHighCover || hasHighLyrics ? 'high' : 'low'
}

/** 简易并发限制执行器 */
const runWithConcurrency = async <T>(
  tasks: (() => Promise<T>)[],
  limit: number,
): Promise<T[]> => {
  const results: T[] = new Array(tasks.length)
  let nextIndex = 0

  const worker = async (): Promise<void> => {
    while (nextIndex < tasks.length) {
      const i = nextIndex++
      results[i] = await tasks[i]()
    }
  }

  const workers = Array.from(
    { length: Math.min(limit, tasks.length) },
    () => worker(),
  )
  await Promise.all(workers)
  return results
}

/**
 * 批量匹配：queue → ScrapeCandidate[]（不写库）。
 * 单曲失败不阻塞其他；失败行以 errors 返回。
 */
export const matchScrapeQueue = async (
  queue: ScrapeQueueSnapshot,
  options: { signal?: AbortSignal; onProgress?: (progress: MatchProgress) => void } = {},
): Promise<ScrapeMatchResult> => {
  const songMap = new Map(loadSongs().map((song) => [song.id, song]))
  const validSongs = queue.items
    .map((item) => ({ item, song: songMap.get(item.songId) }))
    .filter((entry): entry is { item: { songId: string; addedAt: string }; song: SongItem } =>
      !!entry.song,
    )

  const candidates: ScrapeCandidate[] = []
  const errors: ScrapeMatchError[] = []
  let matched = 0

  if (validSongs.length === 0) {
    return { candidates: [], errors: [] }
  }

  const tasks = validSongs.map(({ item, song }) => async (): Promise<ScrapeCandidate | ScrapeMatchError> => {
    if (options.signal?.aborted) {
      return { songId: item.songId, reason: 'aborted' }
    }
    try {
      const result: EditCloudMetaResult = await searchEditCloudMeta(
        {
          songId: song.id,
          title: song.title,
          artist: song.artist,
          album: song.album,
          durationSec: song.duration,
        },
        {
          signal: options.signal,
          maxCandidates: MAX_CANDIDATES_PER_DIM,
        },
      )

      const textCandidates = result.text.items
      const coverCandidates = result.cover.items.map((item) => ({
        remoteUrl: item.remoteUrl,
        source: item.source,
      }))
      const lyricsCandidates = result.lyrics.items.map((item) => ({
        text: item.text,
        format: item.format,
        source: item.source,
      }))

      const overallConfidence = deriveOverallConfidence(
        textCandidates,
        coverCandidates,
        lyricsCandidates,
        song,
      )

      matched += 1
      options.onProgress?.({ matched, total: validSongs.length })

      return {
        songId: song.id,
        song,
        text: {
          current: {
            title: song.title,
            artist: song.artist,
            album: song.album,
          },
          candidates: textCandidates,
          defaultIndex: textCandidates.length > 0 ? 0 : -1,
        },
        cover: {
          currentUri: song.coverUri,
          candidates: coverCandidates,
          defaultIndex: coverCandidates.length > 0 ? 0 : -1,
        },
        lyrics: {
          currentText: song.lyrics,
          currentFormat: song.lyricsFormat,
          candidates: lyricsCandidates,
          defaultIndex: lyricsCandidates.length > 0 ? 0 : -1,
        },
        overallConfidence,
        defaultChecked: overallConfidence === 'high',
      }
    } catch (error: unknown) {
      const reason =
        error instanceof Error ? error.message : 'match failed'
      matched += 1
      options.onProgress?.({ matched, total: validSongs.length })
      return { songId: item.songId, reason }
    }
  })

  const results = await runWithConcurrency(tasks, MAX_CONCURRENT)

  for (const result of results) {
    if ('reason' in result) {
      errors.push(result as ScrapeMatchError)
    } else {
      candidates.push(result as ScrapeCandidate)
    }
  }

  return { candidates, errors }
}
