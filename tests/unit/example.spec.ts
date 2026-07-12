import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import SongsPage from '@/views/SongsPage.vue'
import AlbumsPage from '@/views/AlbumsPage.vue'
import ArtistsPage from '@/views/ArtistsPage.vue'
import type { SongItem } from '@/features/library/types'

const {
  clearQueue,
  enqueueSongs,
  playSong,
  selectSongAtIndex,
  shuffleEnabled,
  toggleShuffle,
  playerState,
} = vi.hoisted(() => ({
  clearQueue: vi.fn(),
  enqueueSongs: vi.fn(),
  playSong: vi.fn().mockResolvedValue(undefined),
  selectSongAtIndex: vi.fn(),
  shuffleEnabled: vi.fn(() => false),
  toggleShuffle: vi.fn(),
  // 可变 plain object：各用例在 mount 前赋值即可，无需 reactive
  playerState: {
    currentSong: null as SongItem | null,
    status: 'idle',
    errorMessage: null as string | null,
    position: 0,
    duration: 0,
    bufferedPosition: null as number | null,
    lyrics: null as string | null,
    lyricsFormat: null as 'lrc' | 'ttml' | null,
    onlineLyricsStatus: 'idle' as 'idle' | 'matching' | 'ready' | 'miss' | 'error',
    coverUri: null as string | null,
    metadataStatus: 'idle',
  },
}))

vi.mock('@/features/player/controller', async () => {
  const actual = await vi.importActual<typeof import('@/features/player/controller')>('@/features/player/controller')
  return {
    ...actual,
    clearQueue,
    enqueueSongs,
    playSong,
    selectSongAtIndex,
    shuffleEnabled,
    toggleShuffle,
    enqueueSong: vi.fn(),
    playerState,
  }
})

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

const mountSongsPage = () => mount(SongsPage, {
  global: {
    stubs: {
      IonPage: { template: '<main><slot /></main>' },
      IonHeader: { template: '<header><slot /></header>' },
      IonToolbar: { template: '<div><slot /></div>' },
      IonTitle: { template: '<div><slot /></div>' },
      IonContent: { template: '<section><slot /></section>' },
      IonButtons: { template: '<div><slot /></div>' },
      IonButton: {
        emits: ['click'],
        template: '<button v-bind="$attrs" :disabled="$attrs.disabled" @click="$emit(\'click\')"><slot /></button>',
      },
      IonFab: { template: '<div class="ion-fab-stub"><slot /></div>' },
      IonFabButton: {
        emits: ['click'],
        template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>',
      },
      IonIcon: true,
      IonList: { template: '<div><slot /></div>' },
      IonItem: {
        template: '<div v-bind="$attrs" @click="$emit(\'click\')"><slot /><slot name="start" /><slot name="end" /></div>',
      },
      IonLabel: { template: '<div><slot /></div>' },
    },
  },
})

describe('音乐库标签页', () => {
  beforeEach(() => {
    clearQueue.mockClear()
    enqueueSongs.mockClear()
    playSong.mockClear()
    selectSongAtIndex.mockClear()
    shuffleEnabled.mockClear()
    toggleShuffle.mockClear()
    shuffleEnabled.mockReturnValue(false)
    selectSongAtIndex.mockReturnValue(null)
    playerState.currentSong = null
  })

  afterEach(() => {
    localStorage.clear()
  })

  test('歌曲页展示入库歌曲 metadata', async () => {
    saveSongs([
      createSong({ id: '1', title: '入库歌曲', artist: '歌手甲', album: '专辑甲', duration: 185 }),
    ])

    const wrapper = mountSongsPage()
    await nextTick()

    expect(wrapper.text()).toContain('入库歌曲')
    expect(wrapper.text()).toContain('歌手甲 - 专辑甲')
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
    const songsPage = mountSongsPage()
    await nextTick()
    expect(songsPage.text()).toContain('还没有歌曲')

    const albumsPage = mount(AlbumsPage)
    await nextTick()
    expect(albumsPage.text()).toContain('还没有专辑')

    const artistsPage = mount(ArtistsPage)
    await nextTick()
    expect(artistsPage.text()).toContain('还没有艺术家')
  })

  test('歌曲页顶部有随机播放全部按钮且无歌曲时禁用', async () => {
    const wrapper = mountSongsPage()
    await nextTick()

    const button = wrapper.get('button[aria-label="随机播放全部"]')
    expect(button.attributes('disabled')).toBeDefined()

    await button.trigger('click')
    expect(clearQueue).not.toHaveBeenCalled()
    expect(enqueueSongs).not.toHaveBeenCalled()
    expect(toggleShuffle).not.toHaveBeenCalled()
    expect(selectSongAtIndex).not.toHaveBeenCalled()
    expect(playSong).not.toHaveBeenCalled()
  })

  test('点击随机播放全部：清空队列 → 装入全部 → 开启 shuffle → 播放乱序首曲', async () => {
    const songs = [
      createSong({ id: '1', title: '歌一', artist: '甲', album: 'A' }),
      createSong({ id: '2', title: '歌二', artist: '乙', album: 'B' }),
    ]
    saveSongs(songs)
    const firstShuffled = songs[1]
    selectSongAtIndex.mockReturnValue(firstShuffled)
    shuffleEnabled.mockReturnValue(false)

    const wrapper = mountSongsPage()
    await nextTick()

    const button = wrapper.get('button[aria-label="随机播放全部"]')
    expect(button.attributes('disabled')).toBeUndefined()

    await button.trigger('click')

    expect(clearQueue).toHaveBeenCalledTimes(1)
    expect(enqueueSongs).toHaveBeenCalledTimes(1)
    const enqueued = enqueueSongs.mock.calls[0][0] as SongItem[]
    expect(enqueued.map((song) => song.id).sort()).toEqual(['1', '2'])
    expect(shuffleEnabled).toHaveBeenCalled()
    expect(toggleShuffle).toHaveBeenCalledTimes(1)
    expect(selectSongAtIndex).toHaveBeenCalledWith(0)
    expect(playSong).toHaveBeenCalledWith(firstShuffled)

    // 调用顺序：clear → enqueue → toggle → select → play
    const callOrder = [
      clearQueue.mock.invocationCallOrder[0],
      enqueueSongs.mock.invocationCallOrder[0],
      toggleShuffle.mock.invocationCallOrder[0],
      selectSongAtIndex.mock.invocationCallOrder[0],
      playSong.mock.invocationCallOrder[0],
    ]
    expect(callOrder).toEqual([...callOrder].sort((a, b) => a - b))
  })

  test('shuffle 已开启时点击随机播放全部不再 toggleShuffle', async () => {
    const songs = [
      createSong({ id: '1', title: '歌一' }),
      createSong({ id: '2', title: '歌二' }),
    ]
    saveSongs(songs)
    selectSongAtIndex.mockReturnValue(songs[0])
    shuffleEnabled.mockReturnValue(true)

    const wrapper = mountSongsPage()
    await nextTick()

    await wrapper.get('button[aria-label="随机播放全部"]').trigger('click')

    expect(clearQueue).toHaveBeenCalledTimes(1)
    expect(enqueueSongs).toHaveBeenCalledTimes(1)
    expect(toggleShuffle).not.toHaveBeenCalled()
    expect(selectSongAtIndex).toHaveBeenCalledWith(0)
    expect(playSong).toHaveBeenCalledWith(songs[0])
  })

  test('无当前播放时不展示跳转到当前播放 FAB', async () => {
    saveSongs([createSong({ id: '1', title: '歌一' })])
    playerState.currentSong = null

    const wrapper = mountSongsPage()
    await nextTick()

    expect(wrapper.find('button[aria-label="跳转到当前播放"]').exists()).toBe(false)
  })

  test('当前播放不在列表中时不展示跳转到当前播放 FAB', async () => {
    saveSongs([createSong({ id: '1', title: '歌一' })])
    playerState.currentSong = createSong({ id: 'missing', title: '不在列表' })

    const wrapper = mountSongsPage()
    await nextTick()

    expect(wrapper.find('button[aria-label="跳转到当前播放"]').exists()).toBe(false)
  })

  test('有当前播放且在列表中时点击 FAB 滚动到对应行', async () => {
    const songs = [
      createSong({ id: '1', title: '歌一' }),
      createSong({ id: '2', title: '歌二' }),
      createSong({ id: '3', title: '歌三' }),
    ]
    saveSongs(songs)
    playerState.currentSong = songs[1]

    const scrollIntoView = vi.fn()
    const originalScrollIntoView = Element.prototype.scrollIntoView
    Element.prototype.scrollIntoView = scrollIntoView

    const host = document.createElement('div')
    document.body.appendChild(host)

    try {
      const wrapper = mount(SongsPage, {
        attachTo: host,
        global: {
          stubs: {
            IonPage: { template: '<main><slot /></main>' },
            IonHeader: { template: '<header><slot /></header>' },
            IonToolbar: { template: '<div><slot /></div>' },
            IonTitle: { template: '<div><slot /></div>' },
            IonContent: { template: '<section><slot /></section>' },
            IonButtons: { template: '<div><slot /></div>' },
            IonButton: {
              emits: ['click'],
              template: '<button v-bind="$attrs" :disabled="$attrs.disabled" @click="$emit(\'click\')"><slot /></button>',
            },
            IonFab: { template: '<div class="ion-fab-stub"><slot /></div>' },
            IonFabButton: {
              emits: ['click'],
              template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>',
            },
            IonIcon: true,
            IonList: { template: '<div><slot /></div>' },
            IonItem: {
              template: '<div v-bind="$attrs" @click="$emit(\'click\')"><slot /><slot name="start" /><slot name="end" /></div>',
            },
            IonLabel: { template: '<div><slot /></div>' },
          },
        },
      })
      await nextTick()

      const fab = wrapper.get('button[aria-label="跳转到当前播放"]')
      await fab.trigger('click')
      await nextTick()

      expect(scrollIntoView).toHaveBeenCalledWith({
        behavior: 'smooth',
        block: 'center',
        inline: 'nearest',
      })
      const target = wrapper.find('[data-song-id="2"]')
      expect(target.exists()).toBe(true)
      expect(target.classes()).toContain('jump-highlight')
      wrapper.unmount()
    } finally {
      Element.prototype.scrollIntoView = originalScrollIntoView
      host.remove()
    }
  })
})
