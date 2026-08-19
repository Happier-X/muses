/**
 * WebDAV 下一首元信息预取：只写曲库，不碰 playerState / 当前曲 match token。
 * 由 controller.prefetchNextTrack 在 peekNext 为 WebDAV 且非自身时调度。
 */
import type { SongItem } from '@/features/library/types'
import { isUserEditedField, loadSongs, upsertSong } from '@/features/library/storage'
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
    // 手改歌词：预取在线歌词整段跳过
    if (isUserEditedField(getLatestSongSnapshot(song), 'lyrics')) {
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
      if (!shouldPersistOnlineLyrics(latest, result.format, result.text, result.confidence)) {
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
      // 歌词来源沿用 lyricsSource，不进 metaSources
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
    // 封面手改：预取封面整段跳过
    if (isUserEditedField(latestBefore, 'cover')) {
      return
    }
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
      // 写库前再确认仍无安全封面且未手改，避免覆盖扫描/用户编辑
      const again = getLatestSongSnapshot(song)
      if (isUserEditedField(again, 'cover') || toSafeCoverUri(again.coverUri)) {
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
          metaSources: { cover: 'cloud' },
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
      duration: latestBefore.duration,
      metaSources: latestBefore.metaSources,
    })
    if (!isActive() || !remote.ok) {
      return
    }
    // child4 R4-4：预取自动补缺同样仅保留高置信写库；低置信不写
    if (remote.confidence === 'low') {
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
          metaSources: {
            ...(next.title !== latest.title ? { title: 'cloud' } : {}),
            ...(next.artist !== latest.artist ? { artist: 'cloud' } : {}),
            ...(next.album !== latest.album ? { album: 'cloud' } : {}),
          },
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
