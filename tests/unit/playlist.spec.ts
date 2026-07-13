import { afterEach, describe, expect, test } from 'vitest'
import type { SongItem } from '@/features/library/types'
import {
  addSongToPlaylist,
  createPlaylist,
  deletePlaylist,
  getPlaylist,
  loadPlaylists,
  removeSongFromPlaylist,
  renamePlaylist,
  resolvePlaylistSongs,
} from '@/features/playlist'

const makeSong = (id: string, title: string): SongItem => ({
  id,
  sourceId: 's1',
  sourceType: 'local',
  path: `${title}.mp3`,
  uri: `file:///${title}.mp3`,
  title,
  createdAt: '2026-01-01T00:00:00.000Z',
})

describe('歌单持久化 CRUD', () => {
  afterEach(() => {
    localStorage.clear()
  })

  test('空存储返回空数组；损坏 JSON 不抛', () => {
    expect(loadPlaylists()).toEqual([])
    localStorage.setItem('muses:playlists', '{not-json')
    expect(loadPlaylists()).toEqual([])
  })

  test('创建：trim 后非空；空名失败', () => {
    expect(createPlaylist('   ')).toBeNull()
    expect(createPlaylist('')).toBeNull()

    const created = createPlaylist('  我的歌单  ')
    expect(created).toMatchObject({ name: '我的歌单', songIds: [] })
    expect(created?.id.startsWith('pl-')).toBe(true)
    expect(loadPlaylists()).toHaveLength(1)
  })

  test('允许重名', () => {
    createPlaylist('同名')
    createPlaylist('同名')
    expect(loadPlaylists().filter((p) => p.name === '同名')).toHaveLength(2)
  })

  test('重命名与删除', () => {
    const a = createPlaylist('A')!
    expect(renamePlaylist(a.id, '  B  ')).toBe(true)
    expect(getPlaylist(a.id)?.name).toBe('B')
    expect(renamePlaylist(a.id, '   ')).toBe(false)
    expect(getPlaylist(a.id)?.name).toBe('B')
    expect(deletePlaylist(a.id)).toBe(true)
    expect(getPlaylist(a.id)).toBeUndefined()
    expect(deletePlaylist(a.id)).toBe(false)
  })

  test('加曲去重且保持顺序；移除', () => {
    const pl = createPlaylist('L')!
    expect(addSongToPlaylist(pl.id, 's1')).toBe(true)
    expect(addSongToPlaylist(pl.id, 's2')).toBe(true)
    expect(addSongToPlaylist(pl.id, 's1')).toBe(true)
    expect(getPlaylist(pl.id)?.songIds).toEqual(['s1', 's2'])

    expect(removeSongFromPlaylist(pl.id, 's1')).toBe(true)
    expect(getPlaylist(pl.id)?.songIds).toEqual(['s2'])
    expect(removeSongFromPlaylist(pl.id, 's1')).toBe(false)
  })

  test('resolve 跳过失效 id 并保持顺序', () => {
    const pl = createPlaylist('L')!
    addSongToPlaylist(pl.id, 's1')
    addSongToPlaylist(pl.id, 'gone')
    addSongToPlaylist(pl.id, 's2')
    const latest = getPlaylist(pl.id)!
    const songs = [makeSong('s2', '二'), makeSong('s1', '一')]
    expect(resolvePlaylistSongs(latest, songs).map((s) => s.id)).toEqual(['s1', 's2'])
  })
})
