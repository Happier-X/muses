import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import type { SongItem } from '@/features/library/types'
import MiniPlayer from '@/components/MiniPlayer.vue'
import PlayerPage from '@/views/PlayerPage.vue'
import App from '@/App.vue'

const { localLibraryNative, nativePlayer, webDavNative } = vi.hoisted(() => ({
  localLibraryNative: {
    scanDirectory: vi.fn(),
    readMetadata: vi.fn(),
  },
  webDavNative: {
    propfind: vi.fn(),
    readMetadata: vi.fn(),
  },
  nativePlayer: {
    play: vi.fn(),
    pause: vi.fn(),
    resume: vi.fn(),
    stop: vi.fn(),
    seek: vi.fn(),
    getState: vi.fn(),
    addListener: vi.fn(),
  },
}))

vi.mock('@capacitor/core', () => ({
  Capacitor: {
    convertFileSrc: vi.fn((uri: string) => `webview:${uri}`),
  },
  registerPlugin: vi.fn((name: string) => {
    if (name === 'LocalLibrary') {
      return localLibraryNative
    }
    if (name === 'WebDav') {
      return webDavNative
    }
    return nativePlayer
  }),
}))

const { routerPush, routerBack, routeState } = vi.hoisted(() => ({
  routerPush: vi.fn(),
  routerBack: vi.fn(),
  routeState: { path: '/tabs/songs' },
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({ push: routerPush, back: routerBack }),
}))

vi.mock('@applemusic-like-lyrics/vue', () => ({
  BackgroundRender: { props: ['album'], template: '<div data-test="amll-background" :data-album="album"><slot /></div>' },
  LyricPlayer: { props: ['lyricLines'], template: '<div data-test="amll-lyrics">{{ lyricLines?.[0]?.words?.[0]?.word }}</div>' },
}))

vi.mock('@aparajita/capacitor-secure-storage', () => ({
  SecureStorage: {
    get: vi.fn(),
    set: vi.fn(),
    remove: vi.fn(),
  },
}))

const localSong: SongItem = {
  id: 'song-local',
  sourceId: 'source-local',
  sourceType: 'local',
  path: 'album/local.mp3',
  uri: 'content://music/local',
  title: '本地歌曲',
  artist: '本地歌手',
  album: '本地专辑',
  createdAt: '2026-07-07T00:00:00.000Z',
  updatedAt: '2026-07-07T00:00:00.000Z',
}

const webDavSong: SongItem = {
  id: 'song-webdav',
  sourceId: 'source-webdav',
  sourceType: 'webdav',
  path: '/music/remote.flac',
  uri: 'https://example.com/dav/music/remote.flac',
  title: '远程歌曲',
  artist: '远程歌手',
  createdAt: '2026-07-07T00:00:00.000Z',
  updatedAt: '2026-07-07T00:00:00.000Z',
}

const resetPlayer = async () => {
  const { stopPlayback } = await import('@/features/player/controller')
  await stopPlayback()
  vi.clearAllMocks()
  vi.useRealTimers()
}


describe('播放器控制器', () => {
  afterEach(async () => {
    await resetPlayer()
    localStorage.clear()
  })


  test('播放本地歌曲时只把 content URI 和元数据传给原生插件', async () => {
    const { playSong, playerState } = await import('@/features/player/controller')

    await playSong(localSong)

    expect(nativePlayer.play).toHaveBeenCalledWith({
      sourceType: 'local',
      songId: 'song-local',
      uri: 'content://music/local',
      title: '本地歌曲',
      artist: '本地歌手',
      album: '本地专辑',
    })
    expect(playerState.status).toBe('playing')
    expect(playerState.currentSong?.title).toBe('本地歌曲')
  })

  test('播放 WebDAV 歌曲时从 SecureStorage 读取密码，且密码不进入 localStorage 或 UI 状态', async () => {
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    vi.mocked(SecureStorage.get).mockResolvedValue('secret-password')
    localStorage.setItem(
      'muses:sources',
      JSON.stringify([
        {
          id: 'source-webdav',
          type: 'webdav',
          name: '远程音乐',
          serverUrl: 'https://example.com/dav',
          username: 'alice',
          path: '/music',
          credentialKey: 'muses:webdav-password:source-webdav',
          createdAt: '2026-07-07T00:00:00.000Z',
        },
      ]),
    )
    const { playSong, playerState } = await import('@/features/player/controller')

    await playSong(webDavSong)

    expect(SecureStorage.get).toHaveBeenCalledWith('muses:webdav-password:source-webdav')
    expect(nativePlayer.play).toHaveBeenCalledWith({
      sourceType: 'webdav',
      songId: 'song-webdav',
      url: 'https://example.com/dav/music/remote.flac',
      username: 'alice',
      password: 'secret-password',
      title: '远程歌曲',
      artist: '远程歌手',
      album: undefined,
    })
    expect(localStorage.getItem('muses:sources')).not.toContain('secret-password')
    expect(JSON.stringify(playerState)).not.toContain('secret-password')
  })


  test('播放列表旧对象时从歌曲库最新记录补齐歌词和封面', async () => {
    localStorage.setItem(
      'muses:songs',
      JSON.stringify([
        {
          ...localSong,
          lyrics: '[00:01.00]库中歌词',
          lyricsSource: 'embedded',
          coverUri: 'file:///cache/covers/latest.jpg',
          tagsScanned: true,
          metadataVersion: 2,
        },
      ]),
    )
    const { playSong, playerState } = await import('@/features/player/controller')

    await playSong(localSong)

    expect(playerState.lyrics).toBe('[00:01.00]库中歌词')
    expect(playerState.currentSong?.lyrics).toBe('[00:01.00]库中歌词')
    expect(playerState.coverUri).toBe('file:///cache/covers/latest.jpg')
  })

  test('播放失败时不把原生敏感错误展示到 UI 状态', async () => {
    nativePlayer.play.mockRejectedValueOnce(new Error('Authorization: Basic secret-password'))
    const { playSong, playerState } = await import('@/features/player/controller')

    await playSong(localSong)

    expect(playerState.status).toBe('error')
    expect(playerState.errorMessage).toBe('播放失败，请稍后重试。')
    expect(JSON.stringify(playerState)).not.toContain('secret-password')
  })

  test('本地标签补扫超时后不会一直停留在扫描状态', async () => {
    vi.useFakeTimers()
    localLibraryNative.readMetadata.mockReturnValueOnce(new Promise(() => undefined))
    const { playSong, playerState } = await import('@/features/player/controller')

    await playSong(localSong)
    expect(playerState.metadataStatus).toBe('scanning')

    await vi.advanceTimersByTimeAsync(15_000)
    expect(playerState.metadataStatus).toBe('failed')
  })

  test('WebDAV 标签补扫允许更长时间等待缓存下载和解析', async () => {
    vi.useFakeTimers()
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    vi.mocked(SecureStorage.get).mockResolvedValue('secret-password')
    webDavNative.readMetadata.mockReturnValueOnce(new Promise(() => undefined))
    localStorage.setItem(
      'muses:sources',
      JSON.stringify([
        {
          id: 'source-webdav',
          type: 'webdav',
          name: '远程音乐',
          serverUrl: 'https://example.com/dav',
          username: 'alice',
          path: '/music',
          credentialKey: 'muses:webdav-password:source-webdav',
          createdAt: '2026-07-07T00:00:00.000Z',
        },
      ]),
    )
    const { playSong, playerState } = await import('@/features/player/controller')

    await playSong(webDavSong)
    expect(playerState.metadataStatus).toBe('scanning')

    await vi.advanceTimersByTimeAsync(119_999)
    expect(playerState.metadataStatus).toBe('scanning')

    await vi.advanceTimersByTimeAsync(1)
    expect(playerState.metadataStatus).toBe('failed')
  })

  test('暂停、继续、seek 和停止会更新播放状态', async () => {
    const { pausePlayback, playSong, playerState, resumePlayback, seekPlayback, stopPlayback } = await import('@/features/player/controller')

    await playSong({ ...localSong, duration: 180 })
    await pausePlayback()
    expect(playerState.status).toBe('paused')

    await resumePlayback()
    expect(playerState.status).toBe('playing')

    await seekPlayback(42.5)
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 42.5 })
    expect(playerState.position).toBe(42.5)

    await stopPlayback()
    expect(playerState.status).toBe('stopped')
    expect(playerState.currentSong).toBeNull()
  })
})

describe('迷你播放器', () => {
  afterEach(async () => {
    await resetPlayer()
    localStorage.clear()
  })

  test('跨页面迷你播放器展示当前歌曲，点击主体进入播放页，按钮不触发导航', async () => {
    const { playSong } = await import('@/features/player/controller')
    await playSong(localSong)

    const wrapper = mount(MiniPlayer, {
      global: {
        stubs: {
          IonButton: { template: '<button :aria-label="$attrs[\'aria-label\']" @click="$emit(\'click\', $event)"><slot /></button>' },
          IonIcon: true,
        },
      },
    })

    expect(wrapper.text()).toContain('本地歌曲')
    expect(wrapper.text()).toContain('本地歌手')

    await wrapper.trigger('click')
    expect(routerPush).toHaveBeenCalledWith('/player')
    routerPush.mockClear()

    await wrapper.get('button[aria-label="暂停播放"]').trigger('click')
    expect(nativePlayer.pause).toHaveBeenCalled()
    expect(routerPush).not.toHaveBeenCalled()

    await wrapper.get('button[aria-label="停止播放"]').trigger('click')
    expect(nativePlayer.stop).toHaveBeenCalled()
  })
})

describe('沉浸式播放页', () => {
  afterEach(async () => {
    await resetPlayer()
    localStorage.clear()
  })

  test('没有当前歌曲时展示空状态和返回入口', () => {
    const wrapper = mount(PlayerPage, {
      global: {
        stubs: {
          IonPage: { template: '<main><slot /></main>' },
          IonContent: { template: '<section><slot /></section>' },
          IonButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
          IonIcon: true,
        },
      },
    })

    expect(wrapper.text()).toContain('暂无播放歌曲')
    expect(wrapper.text()).toContain('返回')
  })

  test('展示当前歌曲、控制按钮、无歌词状态并传递 seek 参数', async () => {
    localStorage.setItem('muses:songs', JSON.stringify([{ ...localSong, duration: 180, coverUri: 'file:///cover.jpg' }, webDavSong]))
    localStorage.setItem(
      'muses:sources',
      JSON.stringify([
        {
          id: 'source-webdav',
          type: 'webdav',
          name: '远程音乐',
          serverUrl: 'https://example.com/dav',
          username: 'alice',
          path: '/music',
          credentialKey: 'muses:webdav-password:source-webdav',
          createdAt: '2026-07-07T00:00:00.000Z',
        },
      ]),
    )
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    vi.mocked(SecureStorage.get).mockResolvedValue('secret-password')
    const { clearQueue, enqueueSongs, playSong, queueState, setRepeatMode, toggleShuffle } = await import('@/features/player/controller')
    clearQueue()
    setRepeatMode('all')
    if (queueState.shuffleEnabled) {
      toggleShuffle()
    }
    enqueueSongs([{ ...localSong, duration: 180, coverUri: 'file:///cover.jpg' }, webDavSong])
    await playSong({ ...localSong, duration: 180, coverUri: 'file:///cover.jpg' })
    expect(queueState.currentIndex).toBe(0)
    expect(queueState.shuffleEnabled).toBe(false)

    const wrapper = mount(PlayerPage, {
      global: {
        stubs: {
          IonPage: { template: '<main><slot /></main>' },
          IonContent: { template: '<section><slot /></section>' },
          IonButton: { emits: ['click'], template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot /></button>' },
          IonIcon: true,
        },
      },
    })

    expect(wrapper.text()).toContain('本地歌曲')
    expect(wrapper.text()).toContain('暂无歌词')
    expect(wrapper.text()).toContain('列表循环')
    expect(wrapper.text()).toContain('顺序播放')
    expect(wrapper.get('img[alt="歌曲封面"]').attributes('src')).toBe('webview:file:///cover.jpg')
    expect(wrapper.find('button[aria-label="上一曲"]').exists()).toBe(true)
    expect(wrapper.find('button[aria-label="下一曲"]').exists()).toBe(true)
    expect(wrapper.find('button[aria-label="播放或暂停"]').exists()).toBe(true)
    expect(wrapper.find('button[aria-label="播放队列"]').exists()).toBe(true)

    const slider = wrapper.get('input[aria-label="播放进度"]')
    await slider.setValue('60')
    await slider.trigger('change')
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 60 })

    const queueRaw = localStorage.getItem('muses:queue') || ''
    expect(queueRaw).not.toContain('secret-password')
    expect(queueRaw).not.toContain('https://')
    expect(queueRaw).not.toContain('content://')
  })

  test('不会展示 data URL 封面', async () => {
    const { playSong } = await import('@/features/player/controller')
    await playSong({ ...localSong, coverUri: 'data:image/jpeg;base64,secret-cover' })

    const wrapper = mount(PlayerPage, {
      global: {
        stubs: {
          IonPage: { template: '<main><slot /></main>' },
          IonContent: { template: '<section><slot /></section>' },
          IonButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
          IonIcon: true,
        },
      },
    })

    expect(wrapper.find('img[alt="歌曲封面"]').exists()).toBe(false)
    expect(wrapper.html()).not.toContain('secret-cover')
  })

  test('有封面和歌词时展示封面并渲染 AMLL 歌词', async () => {
    const { playSong } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      coverUri: 'file:///cover.jpg',
      lyrics: '[ar:测试歌手]\n[00:01,00][00:03.00]第一句歌词\n[00:05.00]第二句歌词',
      lyricsSource: 'embedded',
    })

    const wrapper = mount(PlayerPage, {
      global: {
        stubs: {
          IonPage: { template: '<main><slot /></main>' },
          IonContent: { template: '<section><slot /></section>' },
          IonButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
          IonIcon: true,
        },
      },
    })

    expect(wrapper.get('img[alt="歌曲封面"]').attributes('src')).toBe('webview:file:///cover.jpg')
    expect(wrapper.get('.amll-background [data-test="amll-background"]').attributes('data-album')).toBe('webview:file:///cover.jpg')
    expect(wrapper.find('[data-test="amll-lyrics"]').exists()).toBe(false)

    const panels = wrapper.get('.panels')
    await panels.trigger('touchstart', { changedTouches: [{ clientX: 320 }] })
    await panels.trigger('touchend', { changedTouches: [{ clientX: 120 }] })

    expect(panels.attributes('style')).toContain('translateX(-50%)')
    expect(wrapper.get('[data-test="amll-lyrics"]').text()).toContain('第一句歌词')
    expect(wrapper.text()).not.toContain('暂无歌词')
  })

})

describe('应用壳', () => {
  afterEach(async () => {
    routeState.path = '/tabs/songs'
    await resetPlayer()
    localStorage.clear()
  })

  test('沉浸式播放页不展示底部迷你播放条', async () => {
    const { playSong } = await import('@/features/player/controller')
    await playSong(localSong)
    routeState.path = '/player'

    const wrapper = mount(App, {
      global: {
        stubs: {
          IonApp: { template: '<main><slot /></main>' },
          IonRouterOutlet: { template: '<div />' },
        },
      },
    })

    expect(wrapper.find('.mini-player').exists()).toBe(false)
  })
})

describe('播放队列与循环/随机模式', () => {
  const seedSongs = () => {
    localStorage.setItem('muses:songs', JSON.stringify([localSong, webDavSong]))
  }

  beforeEach(() => {
    vi.resetModules()
  })

  afterEach(async () => {
    await resetPlayer()
    localStorage.clear()
    vi.resetModules()
  })

  test('enqueueSongs 入队后将歌曲加入队列且不包含密码', async () => {
    seedSongs()
    const { enqueueSongs, queueState } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])

    expect(queueState.items.length).toBe(2)
    expect(queueState.items[0].title).toBe('本地歌曲')
    expect(queueState.items[1].title).toBe('远程歌曲')
    expect(queueState.hasItems).toBe(true)

    const queueRaw = localStorage.getItem('muses:queue')
    expect(queueRaw).not.toBeNull()

    const parsed = JSON.parse(queueRaw!)
    expect(parsed.items.length).toBe(2)
    expect(parsed.items[0].songId).toBe('song-local')
    expect(parsed.items[1].songId).toBe('song-webdav')
    // 队列持久化只存 songId，不存完整 SongItem
    expect(queueRaw).not.toContain('content://')
    expect(queueRaw).not.toContain('https://')
    expect(queueRaw).not.toContain('password')
    expect(queueRaw).not.toContain('secret')
  })

  test('enqueueSong 追加单首歌曲到队列尾部', async () => {
    seedSongs()
    const { enqueueSong, enqueueSongs, queueState } = await import('@/features/player/queue')
    enqueueSongs([localSong])
    enqueueSong(webDavSong)

    expect(queueState.items.length).toBe(2)
    expect(queueState.items[1].id).toBe('song-webdav')
  })

  test('removeSongFromQueue 移除指定歌曲并更新索引', async () => {
    seedSongs()
    const { enqueueSongs, queueState, removeSongFromQueue, selectSongAtIndex } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])
    selectSongAtIndex(0)

    expect(queueState.currentIndex).toBe(0)

    removeSongFromQueue('song-local')
    expect(queueState.items.length).toBe(1)
    expect(queueState.items[0].id).toBe('song-webdav')
    expect(queueState.currentIndex).toBe(-1)
  })

  test('clearQueue 清空队列', async () => {
    seedSongs()
    const { clearQueue, enqueueSongs, queueState } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])
    expect(queueState.hasItems).toBe(true)

    clearQueue()
    expect(queueState.items.length).toBe(0)
    expect(queueState.hasItems).toBe(false)
    expect(queueState.currentIndex).toBe(-1)
  })

  test('selectSongAtIndex 选择队列中的歌曲并返回 SongItem', async () => {
    seedSongs()
    const { enqueueSongs, queueState, selectSongAtIndex } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])

    const song = selectSongAtIndex(1)
    expect(song).not.toBeNull()
    expect(song!.id).toBe('song-webdav')
    expect(queueState.currentIndex).toBe(1)

    const invalid = selectSongAtIndex(99)
    expect(invalid).toBeNull()
  })

  test('顺序播放 advanceToNext 按原始顺序推进', async () => {
    seedSongs()
    const { advanceToNext, enqueueSongs } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])

    const first = advanceToNext()
    expect(first).not.toBeNull()
    expect(first!.id).toBe('song-local')

    const second = advanceToNext()
    expect(second).not.toBeNull()
    expect(second!.id).toBe('song-webdav')

    const third = advanceToNext()
    expect(third).not.toBeNull()
    expect(third!.id).toBe('song-local')
  })

  test('随机模式启用后 shuffleOrder 长度一致且 advanceToNext 按洗牌顺序', async () => {
    seedSongs()
    const { advanceToNext, enqueueSongs, queueState, toggleShuffle } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])
    toggleShuffle()

    expect(queueState.items.length).toBe(2)

    // 洗牌后第一首不一定和原始顺序相同
    const first = advanceToNext()
    expect(first).not.toBeNull()

    const second = advanceToNext()
    expect(second).not.toBeNull()
    expect(second!.id).not.toBe(first!.id)
  })

  test('随机切回顺序后恢复原始队列顺序', async () => {
    seedSongs()
    const { advanceToNext, enqueueSongs, toggleShuffle } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])
    toggleShuffle()
    toggleShuffle()

    const first = advanceToNext()
    expect(first!.id).toBe('song-local')
  })

  test('单曲循环 advanceToNext 重复返回当前歌曲', async () => {
    seedSongs()
    const { advanceToNext, enqueueSongs, setRepeatMode } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])
    setRepeatMode('one')

    const first = advanceToNext()
    expect(first!.id).toBe('song-local')

    const second = advanceToNext()
    expect(second!.id).toBe('song-local')

    const third = advanceToNext()
    expect(third!.id).toBe('song-local')
  })

  test('advanceToPrevious 在队首回到队尾', async () => {
    seedSongs()
    const { advanceToPrevious, enqueueSongs, queueState, selectSongAtIndex } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])
    selectSongAtIndex(0)

    const previous = advanceToPrevious()
    expect(previous).not.toBeNull()
    expect(previous!.id).toBe('song-webdav')
    expect(queueState.currentIndex).toBe(1)
  })

  test('单曲循环 advanceToPrevious 返回当前歌曲', async () => {
    seedSongs()
    const { advanceToPrevious, enqueueSongs, selectSongAtIndex, setRepeatMode } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])
    selectSongAtIndex(1)
    setRepeatMode('one')

    const previous = advanceToPrevious()
    expect(previous).not.toBeNull()
    expect(previous!.id).toBe('song-webdav')
  })

  test('列表循环到最后返回第一首', async () => {
    seedSongs()
    const { advanceToNext, enqueueSongs } = await import('@/features/player/queue')
    localStorage.setItem('muses:player-config', JSON.stringify({ repeatMode: 'all', shuffleEnabled: false }))
    enqueueSongs([localSong, webDavSong])

    advanceToNext()
    advanceToNext()
    const third = advanceToNext()
    expect(third!.id).toBe('song-local')
  })

  test('循环模式和播放模式持久化到 muses:player-config', async () => {
    seedSongs()
    const { enqueueSongs, setRepeatMode, repeatMode, toggleShuffle, shuffleEnabled } = await import('@/features/player/queue')
    enqueueSongs([localSong])

    setRepeatMode('one')
    expect(repeatMode()).toBe('one')

    const config = JSON.parse(localStorage.getItem('muses:player-config')!)
    expect(config.repeatMode).toBe('one')

    toggleShuffle()
    expect(shuffleEnabled()).toBe(true)

    const config2 = JSON.parse(localStorage.getItem('muses:player-config')!)
    expect(config2.shuffleEnabled).toBe(true)
  })

  test('队列持久化到 muses:queue 只存 id 和排序，不含完整 SongItem', async () => {
    seedSongs()
    const { enqueueSongs } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])

    const queueRaw = localStorage.getItem('muses:queue')
    const parsed = JSON.parse(queueRaw!)

    expect(parsed.items).toBeDefined()
    expect(parsed.originalOrder).toBeDefined()
    expect(Array.isArray(parsed.items)).toBe(true)

    // 检查每个队列条目只含 songId 是字符串，不带其他字段
    for (const item of parsed.items) {
      expect(typeof item.songId).toBe('string')
      expect(Object.keys(item).length).toBe(1)
    }

    // 不含密码、URI 等敏感数据
    expect(queueRaw).not.toContain('password')
    expect(queueRaw).not.toContain('secret')
    expect(queueRaw).not.toContain('content://')
    expect(queueRaw).not.toContain('https://')
    expect(queueRaw).not.toContain('data:')
  })

  test('空队列 advanceToNext 返回 null', async () => {
    localStorage.removeItem('muses:queue')
    const { clearQueue, advanceToNext } = await import('@/features/player/queue')
    clearQueue()
    const result = advanceToNext()
    expect(result).toBeNull()
  })

  test('controller 暴露 queue 状态和模式切换', async () => {
    seedSongs()
    localStorage.setItem('muses:player-config', JSON.stringify({ repeatMode: 'all', shuffleEnabled: false }))
    const {
      enqueueSongs,
      enqueueSong,
      clearQueue,
      removeSongFromQueue,
      selectSongAtIndex,
      advanceToNext,
      advanceToPrevious,
      playNextFromQueue,
      playPreviousFromQueue,
      setRepeatMode,
      toggleShuffle,
      repeatMode,
      shuffleEnabled,
      queueState,
    } = await import('@/features/player/controller')

    expect(typeof enqueueSongs).toBe('function')
    expect(typeof enqueueSong).toBe('function')
    expect(typeof clearQueue).toBe('function')
    expect(typeof removeSongFromQueue).toBe('function')
    expect(typeof selectSongAtIndex).toBe('function')
    expect(typeof advanceToNext).toBe('function')
    expect(typeof advanceToPrevious).toBe('function')
    expect(typeof playNextFromQueue).toBe('function')
    expect(typeof playPreviousFromQueue).toBe('function')
    expect(typeof setRepeatMode).toBe('function')
    expect(typeof toggleShuffle).toBe('function')
    expect(typeof repeatMode).toBe('function')
    expect(typeof shuffleEnabled).toBe('function')
    expect(queueState).toBeDefined()
    expect(queueState.repeatMode).toBe('all')
    expect(queueState.shuffleEnabled).toBe(false)

    // 默认值
    expect(repeatMode()).toBe('all')
    expect(shuffleEnabled()).toBe(false)
  })

  test('playSong 自动同步当前歌曲到队列索引', async () => {
    seedSongs()
    const { enqueueSongs, playSong, queueState } = await import('@/features/player/controller')
    enqueueSongs([localSong, webDavSong])

    await playSong(webDavSong)
    expect(queueState.currentIndex).toBe(1)
  })

  test('controller 下一曲和上一曲用户操作按队列播放', async () => {
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([localSong, secondLocalSong]))
    const { clearQueue, enqueueSongs, playNextFromQueue, playPreviousFromQueue, playSong, queueState, setRepeatMode, toggleShuffle } = await import('@/features/player/controller')
    clearQueue()
    setRepeatMode('all')
    if (queueState.shuffleEnabled) {
      toggleShuffle()
    }
    enqueueSongs([localSong, secondLocalSong])
    await playSong(localSong)

    nativePlayer.play.mockClear()
    await playNextFromQueue()
    expect(nativePlayer.play).toHaveBeenCalledWith(expect.objectContaining({ songId: 'song-local-2' }))

    nativePlayer.play.mockClear()
    await playPreviousFromQueue()
    expect(nativePlayer.play).toHaveBeenCalledWith(expect.objectContaining({ songId: 'song-local' }))
  })

  test('applyNativeState 接收 finished 后调用 advanceToNext 自动播下一首', async () => {
    vi.resetModules()
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([localSong, secondLocalSong]))
    const { enqueueSongs, initializePlayer, playSong, playerState } = await import('@/features/player/controller')
    await initializePlayer()
    enqueueSongs([localSong, secondLocalSong])

    await playSong(localSong)
    expect(playerState.currentSong?.id).toBe('song-local')

    // 模拟 ExoPlayer 结束事件：调用 addListener 注册的回调
    const listenerCalls = nativePlayer.addListener.mock.calls
    expect(listenerCalls.length).toBeGreaterThan(0)
    const stateChangeCallback = listenerCalls[0][1] as (state: unknown) => void

    // 广播 finished 事件
    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 0,
      duration: 0,
    })

    // 等待异步处理
    await vi.waitFor(() => {
      expect(nativePlayer.play).toHaveBeenCalledTimes(2)
    }, { timeout: 1000 })

    // 第二首应该已经播了
    expect(nativePlayer.play).toHaveBeenCalledWith(expect.objectContaining({
      songId: 'song-local-2',
    }))
  })

  test('finished 且队列为空时调用 stopPlayback', async () => {
    vi.resetModules()
    const { clearQueue, initializePlayer, playSong, playerState } = await import('@/features/player/controller')
    clearQueue()
    await initializePlayer()

    await playSong(localSong)
    expect(playerState.currentSong?.id).toBe('song-local')

    const listenerCalls = nativePlayer.addListener.mock.calls
    const stateChangeCallback = listenerCalls[0][1] as (state: unknown) => void

    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 0,
      duration: 0,
    })

    await vi.waitFor(() => {
      expect(playerState.status).toBe('stopped')
    }, { timeout: 2000 })

    expect(playerState.currentSong).toBeNull()
  })
})
