import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, test } from 'vitest'
import SongsPage from '@/views/SongsPage.vue'
import AlbumsPage from '@/views/AlbumsPage.vue'
import ArtistsPage from '@/views/ArtistsPage.vue'
import type { SongItem } from '@/features/library/types'

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

const saveSongs = (songs: SongItem[]) => {
  localStorage.setItem('muses:songs', JSON.stringify(songs))
}

describe('音乐库标签页', () => {
  afterEach(() => {
    localStorage.clear()
  })

  test('歌曲页展示入库歌曲 metadata', async () => {
    saveSongs([
      createSong({ id: '1', title: '入库歌曲', artist: '歌手甲', album: '专辑甲', duration: 185 }),
    ])

    const wrapper = mount(SongsPage)
    await nextTick()

    expect(wrapper.text()).toContain('入库歌曲')
    expect(wrapper.text()).toContain('歌手甲 · 专辑甲')
    expect(wrapper.text()).toContain('3:05')
  })

  test('专辑页展示专辑聚合和未知专辑', async () => {
    saveSongs([
      createSong({ id: '1', title: '歌一', artist: '歌手甲', album: '专辑甲' }),
      createSong({ id: '2', title: '歌二', artist: '歌手乙', album: '专辑甲' }),
      createSong({ id: '3', title: '歌三', artist: '歌手丙', album: '' }),
    ])

    const wrapper = mount(AlbumsPage)
    await nextTick()

    expect(wrapper.text()).toContain('专辑甲')
    expect(wrapper.text()).toContain('2 首歌曲')
    expect(wrapper.text()).toContain('歌手甲、歌手乙')
    expect(wrapper.text()).toContain('未知专辑')
  })

  test('艺术家页展示艺术家聚合和未知艺术家', async () => {
    saveSongs([
      createSong({ id: '1', title: '歌一', artist: '歌手甲', album: '专辑甲' }),
      createSong({ id: '2', title: '歌二', artist: '歌手甲', album: '' }),
      createSong({ id: '3', title: '歌三', artist: '', album: '专辑乙' }),
    ])

    const wrapper = mount(ArtistsPage)
    await nextTick()

    expect(wrapper.text()).toContain('歌手甲')
    expect(wrapper.text()).toContain('2 首歌曲')
    expect(wrapper.text()).toContain('2 张专辑')
    expect(wrapper.text()).toContain('未知艺术家')
  })

  test('三个页面在无歌曲时展示空状态', async () => {
    const songsPage = mount(SongsPage)
    await nextTick()
    expect(songsPage.text()).toContain('还没有歌曲')

    const albumsPage = mount(AlbumsPage)
    await nextTick()
    expect(albumsPage.text()).toContain('还没有专辑')

    const artistsPage = mount(ArtistsPage)
    await nextTick()
    expect(artistsPage.text()).toContain('还没有艺术家')
  })
})
