/**
 * WebDAV 下一首元信息预取：只写曲库，不碰 playerState / 当前曲 match token。
 * 由 controller.prefetchNextTrack 在 peekNext 为 WebDAV 且非自身时调度。
 */
import type { SongItem } from '@/features/library/types'
import { loadSongs, upsertSong } from '@/features/library/storage'
import { matchOnlineLyrics } from '@/features/lyrics'
import { matchOnlineCoverRemote } from '@/features/cover'
import {
  matchOnlineTextMeta,
  mergeTextMetaFillEmpty,
  needsOnlineTextMeta,
} from '@/features/metadata'
import { cacheRemoteCover } from './native'
import {
  shouldPersistOnlineLyrics,
  toSafeCoverUri,
} from './types'

export type PrefetchActive = () => boolean

const getLatestSongSnapshot = (song: SongItem): SongItem => {
  return loadSongs().find(
    (item) => item.id === song.id || (item.sourceId === song.sourceId && item.path === song.path),
  ) ?? song
}

/**
 * 串行化预取写库，避免三路并行 upsert 的 read-modify-write 丢字段
 *（后写的 loadSongs 快照不含先写字段）。
 */
let libraryWriteChain: Promise<void> = Promise.resolve()

const enqueueLibraryWrite = (
  isActive: PrefetchActive,
  write: () => void,
): Promise<void> => {
  const run = libraryWriteChain.then(() => {
    if (!isActive()) {
      return
    }
    write()
  })
  // 单次失败不打断后续写库链
  libraryWriteChain = run.catch(() => {})
  return run
}

/** 调度门闸：与音频预取同范围（WebDAV 且非当前曲）。 */
export const shouldPrefetchNextMetadata = (
  next: SongItem | null | undefined,
  currentSongId: string,
): next is SongItem => {
  if (!next) {
    return false
  }
  if (next.id === currentSongId) {
    return false
  }
  return next.sourceType === 'webdav'
}

/**
 * 在线歌词 → 严格更优时 upsert；不写 playerState。
 * 仅写歌词相关字段，避免与封面/文本并行写互相覆盖。
 */
export const prefetchLyricsForLibrary = async (
  song: SongItem,
  isActive: PrefetchActive,
): Promise<void> => {
  try {
    if (!isActive()) {
      return
    }
    const result = await matchOnlineLyrics({
      songId: song.id,
      title: song.title,
      artist: song.artist,
      album: song.album,
      duration: song.duration,
    })
    if (!isActive() || !result.ok) {
      return
    }

    await enqueueLibraryWrite(isActive, () => {
      const latest = getLatestSongSnapshot(song)
      if (!shouldPersistOnlineLyrics(latest, result.format, result.text)) {
        return
      }
      upsertSong({
        sourceId: latest.sourceId,
        sourceType: latest.sourceType,
        path: latest.path,
        uri: latest.uri,
        title: latest.title,
        tags: {
          lyrics: result.text,
          lyricsSource: 'online',
          lyricsFormat: result.format,
        },
      }, loadSongs())
    })
  } catch {
    // 预取失败静默
  }
}

/**
 * 在线封面仅补缺 → cache 安全 URI 后 upsert；不写 playerState。
 */
export const prefetchCoverForLibrary = async (
  song: SongItem,
  isActive: PrefetchActive,
): Promise<void> => {
  try {
    if (!isActive()) {
      return
    }
    const latestBefore = getLatestSongSnapshot(song)
    if (toSafeCoverUri(latestBefore.coverUri)) {
      return
    }
    const remote = await matchOnlineCoverRemote({
      songId: latestBefore.id,
      title: latestBefore.title,
      artist: latestBefore.artist,
      album: latestBefore.album,
    })
    if (!isActive() || !remote.ok) {
      return
    }
    const localUri = await cacheRemoteCover({
      url: remote.remoteUrl,
      cacheKey: `online:${latestBefore.id}`,
    })
    const safeUri = toSafeCoverUri(localUri || undefined)
    if (!safeUri || !isActive()) {
      return
    }

    await enqueueLibraryWrite(isActive, () => {
      // 写库前再确认仍无安全封面，避免覆盖扫描/并发结果
      const again = getLatestSongSnapshot(song)
      if (toSafeCoverUri(again.coverUri)) {
        return
      }
      upsertSong({
        sourceId: again.sourceId,
        sourceType: again.sourceType,
        path: again.path,
        uri: again.uri,
        title: again.title,
        tags: {
          coverUri: safeUri,
        },
      }, loadSongs())
    })
  } catch {
    // 预取失败静默
  }
}

/**
 * 在线文本补缺（弱 title / 空 artist·album）→ upsert；不写 playerState。
 */
export const prefetchTextMetaForLibrary = async (
  song: SongItem,
  isActive: PrefetchActive,
): Promise<void> => {
  try {
    if (!isActive()) {
      return
    }
    const latestBefore = getLatestSongSnapshot(song)
    if (!needsOnlineTextMeta(latestBefore)) {
      return
    }
    const remote = await matchOnlineTextMeta({
      songId: latestBefore.id,
      title: latestBefore.title,
      path: latestBefore.path,
      artist: latestBefore.artist,
      album: latestBefore.album,
    })
    if (!isActive() || !remote.ok) {
      return
    }

    await enqueueLibraryWrite(isActive, () => {
      const latest = getLatestSongSnapshot(song)
      if (!needsOnlineTextMeta(latest)) {
        return
      }
      const { next, changed } = mergeTextMetaFillEmpty(latest, remote.hit)
      if (!changed) {
        return
      }
      upsertSong({
        sourceId: next.sourceId,
        sourceType: next.sourceType,
        path: next.path,
        uri: next.uri,
        title: next.title,
        tags: {
          title: next.title,
          artist: next.artist,
          album: next.album,
        },
      }, loadSongs())
    })
  } catch {
    // 预取失败静默
  }
}

/** 三路网络并行；写库经 enqueueLibraryWrite 串行；单路失败互不影响。 */
export const prefetchNextMetadata = async (
  song: SongItem,
  isActive: PrefetchActive,
): Promise<void> => {
  if (!isActive()) {
    return
  }
  await Promise.all([
    prefetchLyricsForLibrary(song, isActive),
    prefetchCoverForLibrary(song, isActive),
    prefetchTextMetaForLibrary(song, isActive),
  ])
}
