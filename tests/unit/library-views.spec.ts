import { describe, expect, test } from 'vitest'
import type { SongItem } from '@/features/library/types'
import { formatDuration, groupSongsByAlbum, groupSongsByArtist, sortSongsForDisplay } from '@/features/library/views'

const createSong = (overrides: Partial<SongItem>): SongItem => ({
  id: overrides.id ?? `song-${overrides.path ?? 'path'}`,
  sourceId: overrides.sourceId ?? 'source-1',
  sourceType: overrides.sourceType ?? 'local',
  path: overrides.path ?? '/music/song.mp3',
  uri: overrides.uri ?? 'content://song',
  title: overrides.title ?? '歌曲',
  artist: overrides.artist,
  album: overrides.album,
  duration: overrides.duration,
  createdAt: overrides.createdAt ?? '2026-07-07T00:00:00.000Z',
  updatedAt: overrides.updatedAt ?? '2026-07-07T00:00:00.000Z',
})

describe('歌曲库展示 helper', () => {
  test('按标题和路径排序歌曲，并格式化时长', () => {
    const songs = [
      createSong({ id: 'b', title: '同名', path: '/b.mp3' }),
      createSong({ id: 'a', title: '同名', path: '/a.mp3' }),
      createSong({ id: 'c', title: '另一首', path: '/c.mp3' }),
    ]

    expect(sortSongsForDisplay(songs).map((song) => song.path)).toEqual(['/c.mp3', '/a.mp3', '/b.mp3'])
    expect(formatDuration(65)).toBe('1:05')
    expect(formatDuration(3661)).toBe('1:01:01')
    expect(formatDuration(undefined)).toBeUndefined()
  })

  test('按专辑聚合歌曲，缺失专辑归入未知专辑并生成艺术家摘要', () => {
    const albums = groupSongsByAlbum([
      createSong({ id: '1', title: '歌一', album: ' 专辑甲 ', artist: '歌手甲' }),
      createSong({ id: '2', title: '歌二', album: '专辑甲', artist: '歌手乙' }),
      createSong({ id: '3', title: '歌三', album: '', artist: '' }),
    ])

    expect(albums).toEqual([
      expect.objectContaining({ name: '未知专辑', songCount: 1, artistSummary: '未知艺术家' }),
      expect.objectContaining({ name: '专辑甲', songCount: 2, artistSummary: '歌手甲、歌手乙' }),
    ])
  })

  test('按艺术家聚合歌曲，缺失艺术家归入未知艺术家并统计专辑数', () => {
    const artists = groupSongsByArtist([
      createSong({ id: '1', title: '歌一', artist: ' 歌手甲 ', album: '专辑甲' }),
      createSong({ id: '2', title: '歌二', artist: '歌手甲', album: '' }),
      createSong({ id: '3', title: '歌三', artist: '', album: '专辑乙' }),
    ])

    expect(artists).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ name: '未知艺术家', songCount: 1, albumCount: 1 }),
        expect.objectContaining({ name: '歌手甲', songCount: 2, albumCount: 2 }),
      ]),
    )
  })
})
