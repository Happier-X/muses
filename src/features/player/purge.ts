/**
 * 删除音源/歌曲后播放器失效曲目对账（纯函数）。
 *
 * 输入曲库当前 songId 集合、当前曲与队列 songId 列表，输出：
 * - shouldStop：当前曲已从曲库消失，需走 stopPlayback 完整清理
 * - queueIdsToRemove：队列中已从曲库消失、需逐个移除的 songId（去重保序）
 *
 * 无任何失效项时返回 no-op 结果，避免扫描/编辑等无关写库触发误动作。
 */

export interface PurgeResolutionInput {
  librarySongIds: ReadonlySet<string>
  currentSongId?: string | null
  queueSongIds: readonly string[]
}

export interface PurgeResolutionResult {
  shouldStop: boolean
  queueIdsToRemove: string[]
}

export const resolvePurgeOnSongsUpdate = (input: PurgeResolutionInput): PurgeResolutionResult => {
  const { librarySongIds, currentSongId, queueSongIds } = input

  // 仅当「歌真的从曲库消失」才触发 stop，不误伤正常播放
  const shouldStop = typeof currentSongId === 'string'
    && currentSongId.length > 0
    && !librarySongIds.has(currentSongId)

  const seen = new Set<string>()
  const queueIdsToRemove: string[] = []
  for (const songId of queueSongIds) {
    if (!librarySongIds.has(songId) && !seen.has(songId)) {
      seen.add(songId)
      queueIdsToRemove.push(songId)
    }
  }

  if (!shouldStop && queueIdsToRemove.length === 0) {
    return { shouldStop: false, queueIdsToRemove: [] }
  }
  return { shouldStop, queueIdsToRemove }
}
