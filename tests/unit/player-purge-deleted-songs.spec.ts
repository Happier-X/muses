import { describe, expect, it } from 'vitest'
import { resolvePurgeOnSongsUpdate } from '../../src/features/player/purge'

const ids = (...list: string[]): Set<string> => new Set(list)

describe('resolvePurgeOnSongsUpdate', () => {
  it('当前曲失效：应停播并移除队列中的失效项', () => {
    const result = resolvePurgeOnSongsUpdate({
      librarySongIds: ids('b'),
      currentSongId: 'a',
      queueSongIds: ['a', 'b'],
    })

    expect(result.shouldStop).toBe(true)
    expect(result.queueIdsToRemove).toEqual(['a'])
  })

  it('仅队列失效：不停播，只返回待移除列表', () => {
    const result = resolvePurgeOnSongsUpdate({
      librarySongIds: ids('a'),
      currentSongId: 'a',
      queueSongIds: ['a', 'x', 'y'],
    })

    expect(result.shouldStop).toBe(false)
    expect(result.queueIdsToRemove).toEqual(['x', 'y'])
  })

  it('全部有效：no-op', () => {
    const result = resolvePurgeOnSongsUpdate({
      librarySongIds: ids('a', 'b', 'c'),
      currentSongId: 'a',
      queueSongIds: ['a', 'b', 'c'],
    })

    expect(result.shouldStop).toBe(false)
    expect(result.queueIdsToRemove).toEqual([])
  })

  it('库为空且无当前曲但队列有残留：仅清理队列，不停播', () => {
    const result = resolvePurgeOnSongsUpdate({
      librarySongIds: new Set<string>(),
      currentSongId: null,
      queueSongIds: ['a', 'b'],
    })

    expect(result.shouldStop).toBe(false)
    expect(result.queueIdsToRemove).toEqual(['a', 'b'])
  })

  it('混合场景：当前曲与队列部分失效，去重保序', () => {
    const result = resolvePurgeOnSongsUpdate({
      librarySongIds: ids('keep-1'),
      currentSongId: 'gone-current',
      queueSongIds: ['gone-current', 'keep-1', 'gone-dup', 'gone-dup', 'keep-1'],
    })

    expect(result.shouldStop).toBe(true)
    expect(result.queueIdsToRemove).toEqual(['gone-current', 'gone-dup'])
  })

  it('无当前曲且队列全部有效：no-op（扫描/编辑触发的 songs-updated 不误伤）', () => {
    const result = resolvePurgeOnSongsUpdate({
      librarySongIds: ids('a', 'b'),
      currentSongId: undefined,
      queueSongIds: ['a', 'b'],
    })

    expect(result.shouldStop).toBe(false)
    expect(result.queueIdsToRemove).toEqual([])
  })
})
