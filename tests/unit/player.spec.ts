import { afterEach, describe, expect, test, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import type { SongItem } from '@/features/library/types'
import MiniPlayer from '@/components/MiniPlayer.vue'
import PlayerPage from '@/views/PlayerPage.vue'
import App from '@/App.vue'

const { localLibraryNative, mediaSessionHandlers, mediaSessionNative, nativePlayer, webDavNative } = vi.hoisted(() => {
  const handlers = new Map<string, (details: { action: string; seekTime?: number | null }) => void>()
  return {
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
    mediaSessionHandlers: handlers,
    mediaSessionNative: {
      setMetadata: vi.fn(),
      setPlaybackState: vi.fn(),
      setPositionState: vi.fn(),
      setActionHandler: vi.fn((options: { action: string }, handler: (details: { action: string; seekTime?: number | null }) => void) => {
        handlers.set(options.action, handler)
        return Promise.resolve()
      }),
      getPluginVersion: vi.fn(),
    },
  }
})

vi.mock('@capgo/capacitor-media-session', () => ({
  MediaSession: mediaSessionNative,
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

const waitForMediaSessionSync = async () => {
  await Promise.resolve()
  await Promise.resolve()
}

describe('播放器控制器', () => {
  afterEach(async () => {
    await resetPlayer()
    localStorage.clear()
  })

  test('媒体会话动作注册临时失败后会在下次播放时重试', async () => {
    mediaSessionNative.setActionHandler.mockRejectedValueOnce(new Error('plugin unavailable'))
    const { initializePlayer, playSong } = await import('@/features/player/controller')

    await initializePlayer()
    mediaSessionHandlers.clear()
    mediaSessionNative.setActionHandler.mockClear()

    await playSong(localSong)

    expect(mediaSessionNative.setActionHandler).toHaveBeenCalled()
    expect(mediaSessionHandlers.has('pause')).toBe(true)
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

  test('播放歌曲时同步非敏感媒体会话元数据和状态', async () => {
    const { playSong } = await import('@/features/player/controller')

    await playSong({ ...localSong, duration: 180, coverUri: 'file:///cache/cover.jpg' })
    await waitForMediaSessionSync()

    expect(mediaSessionNative.setMetadata).toHaveBeenCalledWith({
      title: '本地歌曲',
      artist: '本地歌手',
      album: '本地专辑',
      artwork: [{ src: 'file:///cache/cover.jpg' }],
    })
    expect(mediaSessionNative.setPlaybackState).toHaveBeenLastCalledWith({ playbackState: 'playing' })
    expect(mediaSessionNative.setPositionState).toHaveBeenCalledWith({ duration: 180, position: 0, playbackRate: 1 })
  })

  test('媒体会话不会接收 data URL 封面或 WebDAV 密码', async () => {
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

    await playSong({ ...webDavSong, coverUri: 'data:image/jpeg;base64,secret-password' })
    await waitForMediaSessionSync()

    expect(JSON.stringify(mediaSessionNative.setMetadata.mock.calls)).not.toContain('secret-password')
    expect(mediaSessionNative.setMetadata).toHaveBeenCalledWith({
      title: '远程歌曲',
      artist: '远程歌手',
      album: undefined,
    })
    expect(JSON.stringify(playerState)).not.toContain('secret-password')
  })

  test('系统媒体动作会驱动现有播放器控制函数', async () => {
    const { initializePlayer, playSong } = await import('@/features/player/controller')

    await initializePlayer()
    await playSong(localSong)
    mediaSessionHandlers.get('pause')?.({ action: 'pause' })
    await waitForMediaSessionSync()
    expect(nativePlayer.pause).toHaveBeenCalled()

    mediaSessionHandlers.get('play')?.({ action: 'play' })
    await waitForMediaSessionSync()
    expect(nativePlayer.resume).toHaveBeenCalled()

    mediaSessionHandlers.get('seekto')?.({ action: 'seekto', seekTime: 64.5 })
    await waitForMediaSessionSync()
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 64.5 })

    mediaSessionHandlers.get('stop')?.({ action: 'stop' })
    await waitForMediaSessionSync()
    expect(nativePlayer.stop).toHaveBeenCalled()
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
    await waitForMediaSessionSync()
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 42.5 })
    expect(playerState.position).toBe(42.5)
    expect(mediaSessionNative.setPositionState).toHaveBeenLastCalledWith({ duration: 180, position: 42.5, playbackRate: 1 })

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
    const { playSong } = await import('@/features/player/controller')
    await playSong({ ...localSong, duration: 180, coverUri: 'file:///cover.jpg' })

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

    expect(wrapper.text()).toContain('本地歌曲')
    expect(wrapper.text()).toContain('暂无歌词')
    expect(wrapper.get('img[alt="歌曲封面"]').attributes('src')).toBe('webview:file:///cover.jpg')

    const slider = wrapper.get('input[aria-label="播放进度"]')
    await slider.setValue('60')
    await slider.trigger('change')
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 60 })
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
