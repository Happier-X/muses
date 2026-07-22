import type { SourceItem } from '@/features/sources/types'
import { getWebDavPassword } from '@/features/sources/storage'
import { listWebDavAudioFiles } from '@/features/sources/webdav'
import { getTitleFromPath, isSupportedAudioFile } from './audio'
import { scanLocalAudioFiles } from './native'
import { loadSongs, reconcileSourceSongs, saveSongs, upsertSong } from './storage'
import { readLocalAudioTags, readWebDavAudioTags } from './tags'
import type { AudioFileEntry, AudioTags, ScanOptions, ScanProgress, ScanProgressCallback, ScanResult, ScanSummary } from './types'

const createEmptySummary = (): ScanSummary => ({
  discovered: 0,
  processed: 0,
  inserted: 0,
  updated: 0,
  skipped: 0,
  failed: 0,
  degraded: 0,
  removed: 0,
})

const createProgressEmitter = (
  onProgress: ScanProgressCallback | undefined,
  summary: ScanSummary,
): ((progress: Pick<ScanProgress, 'stage' | 'currentItem' | 'message'>) => void) => {
  let lastEmittedAt = 0
  let lastStage: ScanProgress['stage'] = 'idle'
  const throttleMs = 50

  return (progress) => {
    if (!onProgress) {
      return
    }

    const now = Date.now()
    const terminal = progress.stage === 'completed' || progress.stage === 'failed'
    const stageChanged = progress.stage !== lastStage
    if (!terminal && !stageChanged && now - lastEmittedAt < throttleMs) {
      return
    }

    lastEmittedAt = now
    lastStage = progress.stage
    onProgress({ ...summary, ...progress })
  }
}

const getAudioFiles = async (source: SourceItem): Promise<{ files: AudioFileEntry[]; webDavPassword?: string }> => {
  if (source.type === 'local') {
    return { files: (await scanLocalAudioFiles(source.path)).filter((file) => isSupportedAudioFile(file.path || file.name)) }
  }

  const password = await getWebDavPassword(source.credentialKey)
  if (!password) {
    throw new Error('WebDAV 密码不存在，请重新添加该音源。')
  }

  return {
    files: await listWebDavAudioFiles(
      {
        serverUrl: source.serverUrl,
        username: source.username,
        password,
      },
      source.path,
    ),
    webDavPassword: password,
  }
}

const readTagsSafely = async (
  source: SourceItem,
  file: AudioFileEntry,
  password: string | undefined,
): Promise<{ tags: AudioTags; degraded: boolean }> => {
  try {
    if (source.type === 'local') {
      return { tags: await readLocalAudioTags(file), degraded: false }
    }

    if (!password) {
      throw new Error('缺少 WebDAV 密码。')
    }

    return { tags: await readWebDavAudioTags(source, file, password), degraded: false }
  } catch {
    return { tags: {}, degraded: true }
  }
}

export const scanSourceLibrary = async (
  source: SourceItem,
  options: ScanOptions,
  onProgress?: ScanProgressCallback,
): Promise<ScanResult> => {
  const summary = createEmptySummary()
  const emitProgress = createProgressEmitter(onProgress, summary)

  emitProgress({ stage: 'discovering', message: '正在查找音频文件…' })

  try {
    const { files, webDavPassword } = await getAudioFiles(source)
    summary.discovered = files.length
    const keepPaths = new Set(files.map((file) => file.path))
    emitProgress({ stage: 'processing', message: '正在入库音频文件…' })

    const originalSongs = loadSongs()
    let songs = originalSongs
    let hasChanges = false
    for (const file of files) {
      const fallbackTitle = getTitleFromPath(file.name || file.path)
      emitProgress({ stage: 'processing', currentItem: file.path })

      const tagResult = options.readTags ? await readTagsSafely(source, file, webDavPassword) : { tags: {}, degraded: false }
      if (tagResult.degraded) {
        summary.degraded += 1
      }

      try {
        const upsertResult = upsertSong(
          {
            sourceId: source.id,
            sourceType: source.type,
            path: file.path,
            uri: file.uri,
            title: fallbackTitle,
            tags: tagResult.tags,
          },
          songs,
          { persist: false },
        )
        songs = upsertResult.songs
        summary[upsertResult.status] += 1
        if (upsertResult.status !== 'skipped') {
          hasChanges = true
        }
      } catch {
        summary.failed += 1
      } finally {
        summary.processed += 1
      }
    }

    // 发现阶段成功后才对账：以本次列出的路径为准清理该音源旧歌曲（含发现 0 文件）
    const reconcileResult = reconcileSourceSongs(source.id, keepPaths, songs, { persist: false })
    summary.removed = reconcileResult.removed
    hasChanges ||= reconcileResult.removed > 0

    // 扫描期间只在内存中修改；成功后若最终曲库有变化，统一写入并广播一次。
    // 直接复用变更摘要，避免提交前为比较新旧曲库额外做两次全量序列化。
    if (hasChanges) {
      saveSongs(reconcileResult.songs)
    }

    emitProgress({ stage: 'completed', message: '扫描完成。' })
    return { summary, songs: reconcileResult.songs }
  } catch (error) {
    const message = error instanceof Error ? error.message : '扫描失败。'
    emitProgress({ stage: 'failed', message })
    throw error
  }
}

export { createEmptySummary }
