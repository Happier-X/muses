/**
 * 可疑歌曲判定函数（child2 R2-3 / parent R5）。
 *
 * 命中任一规则即视为可疑，可批量入待刮削队列：
 * - title 是弱标签（=文件名占位）
 * - artist 缺失
 * - album 缺失
 * - cover 缺失
 * - lyrics 缺失
 * - lyricsSource 为 'scrape'（刮削写回但未入文件）或历史遗留 'online'
 * - title/artist/album 来源为 'scrape' 或历史遗留 'cloud'（未写入文件的值值得重刮）
 *
 * 可疑判定规则集合用于单测与 UI 共用；不写库。
 */
import type { FieldSource, SongItem } from '@/features/library/types'
import { isBlank, isWeakTitle } from '@/features/metadata/util'

export interface SuspiciousSongOptions {
  /** 是否纳入「来源 cloud」字段（默认 true） */
  includeCloudSources?: boolean
}

/** 是否存在 scrape/cloud 来源字段（刮削写回未入文件 / 历史在线补缺） */
const hasUnfiledSource = (song: SongItem): boolean => {
  const sources = song.metaSources
  if (!sources) {
    return false
  }
  const isUnfiled = (src: FieldSource | undefined): boolean => src === 'scrape' || src === 'cloud'
  return isUnfiled(sources.title) || isUnfiled(sources.artist) || isUnfiled(sources.album)
}

/**
 * 判定单首歌是否为「可疑/需要刮削」。
 * 命中即返回 true；多规则同时命中亦视为可疑（去重）。
 */
export const isSuspiciousSongForScrape = (
  song: SongItem,
  options: SuspiciousSongOptions = {},
): boolean => {
  const includeCloud = options.includeCloudSources !== false
  const weakTitle = isWeakTitle(song.title, song.path)

  // 1. artist/album/cover/lyrics 任一缺失 → 可疑
  if (isBlank(song.artist)) {
    return true
  }
  if (isBlank(song.album)) {
    return true
  }
  if (!song.coverUri) {
    return true
  }
  if (!song.lyrics?.trim()) {
    return true
  }
  // 2. 弱 title（=文件名占位）本身不是问题，需配合其他可疑信号才计入（避免库中常态告警）
  // 3. 歌词来源 scrape（写回失败仅库内展示）或历史遗留 online → 低可信，值得重刮
  if (includeCloud && (song.lyricsSource === 'scrape' || song.lyricsSource === 'online')) {
    return true
  }
  // 4. scrape/cloud 补缺字段（任意字段命中即视作可改进）
  if (includeCloud && hasUnfiledSource(song)) {
    return true
  }
  // 5. 弱 title + 其他弱信号（缺 cover 或缺 lyrics）才视为可疑
  if (weakTitle && (!song.coverUri || !song.lyrics?.trim())) {
    return true
  }
  return false
}

/** 批量筛选：返回新数组（不修改原数组） */
export const pickSuspiciousSongs = (
  songs: readonly SongItem[],
  options: SuspiciousSongOptions = {},
): SongItem[] => {
  return songs.filter((song) => isSuspiciousSongForScrape(song, options))
}
