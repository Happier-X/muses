import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { readFileSync } from 'node:fs'
import type { SongItem } from '@/features/library/types'
import MiniPlayer from '@/components/MiniPlayer.vue'
import PlayerPage from '@/views/PlayerPage.vue'
import App from '@/App.vue'
import { listOutline, repeat, repeatOutline, shuffle } from 'ionicons/icons'

/** 播放页 UI 单测不跑真实多源歌词，避免 matching 空态文案抖动 */
vi.mock('@/features/lyrics', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/lyrics')>()
  return {
    ...actual,
    matchOnlineLyrics: vi.fn().mockResolvedValue({ ok: false, reason: 'no-match' }),
  }
})

const { localLibraryNative, nativePlayer, nativeAudio, audioPlayerBridge, webDavNative, statusBarSetStyle } = vi.hoisted(() => ({
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
    ensureNotificationPermission: vi.fn(),
    addListener: vi.fn(),
  },
  statusBarSetStyle: vi.fn().mockResolvedValue(undefined),
  nativeAudio: {
    configure: vi.fn().mockResolvedValue(undefined),
    addListener: vi.fn().mockResolvedValue({ remove: vi.fn() }),
    preload: vi.fn().mockResolvedValue(undefined),
    play: vi.fn().mockResolvedValue(undefined),
    stop: vi.fn().mockResolvedValue(undefined),
    unload: vi.fn().mockResolvedValue(undefined),
    pause: vi.fn().mockResolvedValue(undefined),
    resume: vi.fn().mockResolvedValue(undefined),
    setCurrentTime: vi.fn().mockResolvedValue(undefined),
    getCurrentTime: vi.fn().mockResolvedValue({ currentTime: 0 }),
    getDuration: vi.fn().mockResolvedValue({ duration: 180 }),
    isPlaying: vi.fn().mockResolvedValue({ isPlaying: true }),
  },
  audioPlayerBridge: {
    bufferProgressListener: undefined as ((event: {
      songId?: string
      bufferedPosition?: number
      bufferedRatio?: number
      fullyBuffered?: boolean
    }) => void) | undefined,
    ensureNotificationPermission: vi.fn().mockResolvedValue({ granted: true }),
    prepareLocalAudioFile: vi.fn(),
    prepareWebDavAudioFile: vi.fn(),
    getCachedWebDavAudioFile: vi.fn().mockResolvedValue({ uri: null }),
    prefetchWebDavAudioFile: vi.fn().mockResolvedValue({ cached: false, started: true }),
    cancelBufferSession: vi.fn().mockResolvedValue(undefined),
    addListener: vi.fn(async (_eventName: string, listener: (event: {
      songId?: string
      bufferedPosition?: number
      bufferedRatio?: number
      fullyBuffered?: boolean
    }) => void) => {
      audioPlayerBridge.bufferProgressListener = listener
      return { remove: vi.fn() }
    }),
    prepareArtworkDataUrl: vi.fn(async ({ uri }: { uri: string }) => ({
      dataUrl: `data:image/jpeg;base64,${btoa(uri)}`,
    })),
    cacheRemoteCover: vi.fn().mockResolvedValue({ uri: null }),
  },
}))

vi.mock('@capacitor/core', () => ({
  Capacitor: {
    convertFileSrc: vi.fn((uri: string) => `webview:${uri}`),
  },
  CapacitorHttp: {
    get: vi.fn().mockRejectedValue(new Error('http mock')),
  },
  registerPlugin: vi.fn((name: string) => {
    if (name === 'LocalLibrary') {
      return localLibraryNative
    }
    if (name === 'WebDav') {
      return webDavNative
    }
    if (name === 'AudioPlayer') {
      return audioPlayerBridge
    }
    return nativePlayer
  }),
}))

vi.mock('@capgo/capacitor-native-audio', () => ({
  NativeAudio: nativeAudio,
}))

const prefetchWebDavAudioFileMock = vi.hoisted(() => vi.fn().mockResolvedValue({ cached: false, started: true }))

const cacheRemoteCoverMock = vi.hoisted(() => vi.fn().mockResolvedValue(null))

vi.mock('@/features/player/native', () => ({
  AudioPlayerNative: nativePlayer,
  cacheRemoteCover: cacheRemoteCoverMock,
  AudioPlayerBridge: audioPlayerBridge,
  prefetchWebDavAudioFile: prefetchWebDavAudioFileMock,
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
  LyricPlayer: {
    props: ['lyricLines', 'currentTime', 'alignAnchor', 'alignPosition', 'enableSpring', 'enableBlur', 'enableScale', 'wordFadeWidth'],
    emits: ['lineClick'],
    methods: {
      emitLineClick(startTime: number | undefined) {
        this.$emit('lineClick', {
          lineIndex: 0,
          line: {
            getLine: () => (startTime === undefined ? {} : { startTime }),
          },
          stopPropagation: () => undefined,
          preventDefault: () => undefined,
        })
      },
    },
    template: `
      <div data-test="amll-lyrics" :data-align-position="alignPosition">
        <button
          type="button"
          data-test="lyric-line-click"
          @click="emitLineClick(5000)"
        >
          {{ lyricLines?.[0]?.words?.[0]?.word }}
        </button>
        <span data-test="lyric-translation">{{ lyricLines?.[0]?.translatedLyric }}</span>
      </div>
    `,
  },
}))

vi.mock('@aparajita/capacitor-secure-storage', () => ({
  SecureStorage: {
    get: vi.fn(),
    set: vi.fn(),
    remove: vi.fn(),
  },
}))

vi.mock('@capgo/capacitor-media-session', () => ({
  MediaSession: {
    setMetadata: vi.fn().mockResolvedValue(undefined),
    setPlaybackState: vi.fn().mockResolvedValue(undefined),
    setPositionState: vi.fn().mockResolvedValue(undefined),
    setActionHandler: vi.fn().mockResolvedValue(undefined),
  },
}))

vi.mock('@capacitor/app', () => ({
  App: {
    addListener: vi.fn().mockResolvedValue({ remove: vi.fn() }),
    minimizeApp: vi.fn().mockResolvedValue(undefined),
  },
}))

vi.mock('@capacitor/status-bar', () => ({
  StatusBar: { setStyle: statusBarSetStyle },
  Style: { Dark: 'DARK', Default: 'DEFAULT', Light: 'LIGHT' },
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
  const { clearQueue, setRepeatMode, stopPlayback } = await import('@/features/player/controller')
  await stopPlayback()
  clearQueue()
  setRepeatMode('all')
  vi.clearAllMocks()
  vi.useRealTimers()
  prefetchWebDavAudioFileMock.mockReset()
  prefetchWebDavAudioFileMock.mockResolvedValue({ cached: false, started: true })
  cacheRemoteCoverMock.mockReset()
  cacheRemoteCoverMock.mockResolvedValue(null)
  try {
    const { setOnlineCoverProvidersForTest } = await import('@/features/cover')
    setOnlineCoverProvidersForTest(null)
  } catch {
    // ignore
  }
  audioPlayerBridge.getCachedWebDavAudioFile.mockReset()
  audioPlayerBridge.getCachedWebDavAudioFile.mockResolvedValue({ uri: null })
  audioPlayerBridge.prefetchWebDavAudioFile.mockReset()
  audioPlayerBridge.prefetchWebDavAudioFile.mockResolvedValue({ cached: false, started: true })
  audioPlayerBridge.cacheRemoteCover.mockReset()
  audioPlayerBridge.cacheRemoteCover.mockResolvedValue({ uri: null })
  audioPlayerBridge.prepareArtworkDataUrl.mockReset()
  audioPlayerBridge.prepareArtworkDataUrl.mockImplementation(async ({ uri }: { uri: string }) => ({
    dataUrl: `data:image/jpeg;base64,${btoa(uri)}`,
  }))
}


describe('原生播放器封装', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    nativeAudio.getDuration.mockResolvedValue({ duration: 180 })
    audioPlayerBridge.getCachedWebDavAudioFile.mockResolvedValue({ uri: null })
    audioPlayerBridge.prefetchWebDavAudioFile.mockResolvedValue({ cached: false, started: true })
  })

  test('WebDAV 未缓存时使用远程 URL 和 Basic Auth，且不调用渐进文件准备', async () => {
    const { AudioPlayerNative } = await vi.importActual<typeof import('@/features/player/native')>('@/features/player/native')

    await AudioPlayerNative.play({
      sourceType: 'webdav',
      songId: 'song-webdav-native',
      url: 'https://example.com/dav/music/remote.flac',
      username: 'alice',
      password: 'secret-password',
      title: '远程歌曲',
    })

    expect(audioPlayerBridge.getCachedWebDavAudioFile).toHaveBeenCalledWith({
      url: 'https://example.com/dav/music/remote.flac',
    })
    expect(audioPlayerBridge.prepareWebDavAudioFile).not.toHaveBeenCalled()
    expect(audioPlayerBridge.cancelBufferSession).toHaveBeenCalled()
    expect(nativeAudio.preload).toHaveBeenCalledWith({
      assetId: 'song-song-webdav-native',
      assetPath: 'https://example.com/dav/music/remote.flac',
      isUrl: true,
      audioChannelNum: 1,
      headers: {
        Authorization: `Basic ${btoa('alice:secret-password')}`,
      },
    })
    expect((await AudioPlayerNative.getState()).bufferedPosition).toBeUndefined()

    audioPlayerBridge.bufferProgressListener?.({
      songId: 'song-webdav-native',
      bufferedPosition: 90,
      bufferedRatio: 0.5,
    })
    expect((await AudioPlayerNative.getState()).bufferedPosition).toBeUndefined()
  })

  test('WebDAV 完整缓存命中时使用 file:// 并视为 full buffer', async () => {
    audioPlayerBridge.getCachedWebDavAudioFile.mockResolvedValueOnce({
      uri: 'file:///cache/webdav-audio/abc.flac',
    })
    const { AudioPlayerNative } = await vi.importActual<typeof import('@/features/player/native')>('@/features/player/native')

    await AudioPlayerNative.play({
      sourceType: 'webdav',
      songId: 'song-webdav-cached',
      url: 'https://example.com/dav/music/remote.flac',
      username: 'alice',
      password: 'secret-password',
      title: '远程歌曲',
    })

    expect(audioPlayerBridge.prepareWebDavAudioFile).not.toHaveBeenCalled()
    expect(nativeAudio.preload).toHaveBeenCalledWith(expect.objectContaining({
      assetPath: 'file:///cache/webdav-audio/abc.flac',
      isUrl: true,
    }))
    // 完整缓存不带 Authorization headers
    const preloadArg = nativeAudio.preload.mock.calls[0][0] as { headers?: Record<string, string> }
    expect(preloadArg.headers).toBeUndefined()
    expect(await AudioPlayerNative.getState()).toEqual(expect.objectContaining({ bufferedPosition: 180 }))
  })

  test('partial 路径不得当作完整缓存命中', async () => {
    audioPlayerBridge.getCachedWebDavAudioFile.mockResolvedValueOnce({
      uri: 'file:///cache/webdav-audio/abc.flac.partial',
    })
    const { AudioPlayerNative } = await vi.importActual<typeof import('@/features/player/native')>('@/features/player/native')

    await AudioPlayerNative.play({
      sourceType: 'webdav',
      songId: 'song-webdav-partial',
      url: 'https://example.com/dav/music/remote.flac',
      username: 'alice',
      password: 'secret-password',
      title: '远程歌曲',
    })

    expect(nativeAudio.preload).toHaveBeenCalledWith(expect.objectContaining({
      assetPath: 'https://example.com/dav/music/remote.flac',
    }))
    expect((await AudioPlayerNative.getState()).bufferedPosition).toBeUndefined()
  })

  test('prefetchWebDavAudioFile 转发 bridge 且失败静默', async () => {
    const native = await vi.importActual<typeof import('@/features/player/native')>('@/features/player/native')

    audioPlayerBridge.prefetchWebDavAudioFile.mockResolvedValueOnce({ cached: true, started: false })
    await expect(native.prefetchWebDavAudioFile({
      url: 'https://example.com/a.flac',
      username: 'u',
      password: 'p',
      songId: 's1',
    })).resolves.toEqual({ cached: true, started: false })

    audioPlayerBridge.prefetchWebDavAudioFile.mockRejectedValueOnce(new Error('network'))
    await expect(native.prefetchWebDavAudioFile({
      url: 'https://example.com/a.flac',
      username: 'u',
      password: 'p',
      songId: 's1',
    })).resolves.toEqual({ cached: false, started: false })
  })

  test('cacheRemoteCover 转发 bridge 且拒绝 data/base64/远程 URL 与失败', async () => {
    const native = await vi.importActual<typeof import('@/features/player/native')>('@/features/player/native')

    audioPlayerBridge.cacheRemoteCover.mockResolvedValueOnce({ uri: 'file:///cache/covers/online.jpg' })
    await expect(native.cacheRemoteCover({
      url: 'https://example.com/a.jpg',
      cacheKey: 'online:s1',
    })).resolves.toBe('file:///cache/covers/online.jpg')

    audioPlayerBridge.cacheRemoteCover.mockResolvedValueOnce({ uri: 'data:image/jpeg;base64,abc' })
    await expect(native.cacheRemoteCover({
      url: 'https://example.com/a.jpg',
      cacheKey: 'online:s1',
    })).resolves.toBeNull()

    audioPlayerBridge.cacheRemoteCover.mockResolvedValueOnce({ uri: 'https://cdn.example.com/remote.jpg' })
    await expect(native.cacheRemoteCover({
      url: 'https://example.com/a.jpg',
      cacheKey: 'online:s1',
    })).resolves.toBeNull()

    audioPlayerBridge.cacheRemoteCover.mockRejectedValueOnce(new Error('network'))
    await expect(native.cacheRemoteCover({
      url: 'https://example.com/a.jpg',
      cacheKey: 'online:s1',
    })).resolves.toBeNull()
  })

  test('本地文件保持完整缓冲', async () => {
    audioPlayerBridge.prepareLocalAudioFile.mockResolvedValueOnce({ uri: 'file:///cache/local.mp3' })
    const { AudioPlayerNative } = await vi.importActual<typeof import('@/features/player/native')>('@/features/player/native')

    await AudioPlayerNative.play({
      sourceType: 'local',
      songId: 'song-local-native',
      uri: 'content://music/local',
      title: '本地歌曲',
    })

    expect(nativeAudio.preload).toHaveBeenCalledWith(expect.objectContaining({
      assetPath: 'file:///cache/local.mp3',
      isUrl: true,
    }))
    expect(await AudioPlayerNative.getState()).toEqual(expect.objectContaining({ bufferedPosition: 180 }))
  })
})

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

  test('快速连切时被 supersede 的 play 不得把 UI 打成旧曲 paused', async () => {
    let resolveA: (() => void) | undefined
    const playA = new Promise<void>((resolve) => {
      resolveA = resolve
    })

    nativePlayer.play
      .mockImplementationOnce(async () => playA)
      .mockImplementationOnce(async () => undefined)

    const { playSong, playerState } = await import('@/features/player/controller')
    const songA = { ...localSong, id: 'song-a', title: '歌曲A', path: 'a.mp3' }
    const songB = {
      ...localSong,
      id: 'song-b',
      title: '歌曲B',
      path: 'b.mp3',
      uri: 'content://music/b',
    }

    const first = playSong(songA)
    await Promise.resolve()
    expect(playerState.status).toBe('loading')
    expect(playerState.currentSong?.id).toBe('song-a')

    await playSong(songB)
    expect(playerState.currentSong?.id).toBe('song-b')
    expect(playerState.status).toBe('playing')

    // 旧 play 晚到完成：不得覆盖新曲状态
    resolveA?.()
    await first
    expect(playerState.currentSong?.id).toBe('song-b')
    expect(playerState.status).toBe('playing')
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

  test('播放成功后为下一首 WebDAV 触发完整预取，密码只到 bridge', async () => {
    prefetchWebDavAudioFileMock.mockClear()
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
    localStorage.setItem('muses:songs', JSON.stringify([localSong, webDavSong]))
    const { clearQueue, enqueueSongs, playSong, playerState, selectSongAtIndex, setRepeatMode } = await import('@/features/player/controller')
    setRepeatMode('all')
    clearQueue()
    enqueueSongs([localSong, webDavSong])
    selectSongAtIndex(0)

    await playSong(localSong)

    await vi.waitFor(() => {
      expect(prefetchWebDavAudioFileMock).toHaveBeenCalled()
    })
    expect(prefetchWebDavAudioFileMock).toHaveBeenCalledWith({
      url: 'https://example.com/dav/music/remote.flac',
      username: 'alice',
      password: 'secret-password',
      songId: 'song-webdav',
    })
    expect(JSON.stringify(playerState)).not.toContain('secret-password')
    expect(localStorage.getItem('muses:songs')).not.toContain('secret-password')
  })

  test('下一首为本地时不预取', async () => {
    prefetchWebDavAudioFileMock.mockClear()
    const secondLocal: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地',
    }
    localStorage.setItem('muses:songs', JSON.stringify([localSong, secondLocal]))
    const { clearQueue, enqueueSongs, playSong, selectSongAtIndex, setRepeatMode } = await import('@/features/player/controller')
    setRepeatMode('all')
    clearQueue()
    enqueueSongs([localSong, secondLocal])
    selectSongAtIndex(0)

    await playSong(localSong)

    // 给异步预取一个 tick
    await Promise.resolve()
    await Promise.resolve()
    expect(prefetchWebDavAudioFileMock).not.toHaveBeenCalled()
  })

  test('单曲循环不预取自身', async () => {
    prefetchWebDavAudioFileMock.mockClear()
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
    localStorage.setItem('muses:songs', JSON.stringify([webDavSong]))
    const { clearQueue, enqueueSongs, playSong, setRepeatMode } = await import('@/features/player/controller')
    clearQueue()
    enqueueSongs([webDavSong])
    setRepeatMode('one')

    await playSong(webDavSong)

    await Promise.resolve()
    await Promise.resolve()
    expect(prefetchWebDavAudioFileMock).not.toHaveBeenCalled()
  })

  test('预取失败静默且不改变当前播放状态', async () => {
    prefetchWebDavAudioFileMock.mockReset()
    prefetchWebDavAudioFileMock.mockRejectedValueOnce(new Error('prefetch boom'))
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
    localStorage.setItem('muses:songs', JSON.stringify([localSong, webDavSong]))
    const { clearQueue, enqueueSongs, playSong, playerState, selectSongAtIndex, setRepeatMode } = await import('@/features/player/controller')
    setRepeatMode('all')
    clearQueue()
    enqueueSongs([localSong, webDavSong])
    selectSongAtIndex(0)

    await playSong(localSong)

    await vi.waitFor(() => {
      expect(prefetchWebDavAudioFileMock).toHaveBeenCalled()
    })
    expect(playerState.status).toBe('playing')
    expect(playerState.currentSong?.id).toBe('song-local')
    expect(playerState.errorMessage).toBeNull()
  })

  test('队列变更后会重新调度下一首预取', async () => {
    prefetchWebDavAudioFileMock.mockClear()
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    vi.mocked(SecureStorage.get).mockResolvedValue('secret-password')
    const anotherWebDav: SongItem = {
      ...webDavSong,
      id: 'song-webdav-2',
      path: '/music/another.flac',
      uri: 'https://example.com/dav/music/another.flac',
      title: '另一首远程',
    }
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
    localStorage.setItem('muses:songs', JSON.stringify([localSong, webDavSong, anotherWebDav]))
    const {
      clearQueue,
      enqueueSongs,
      playSong,
      removeSongFromQueue,
      selectSongAtIndex,
      setRepeatMode,
    } = await import('@/features/player/controller')
    setRepeatMode('all')
    clearQueue()
    enqueueSongs([localSong, webDavSong, anotherWebDav])
    selectSongAtIndex(0)

    await playSong(localSong)
    await vi.waitFor(() => {
      expect(prefetchWebDavAudioFileMock).toHaveBeenCalledWith(expect.objectContaining({
        songId: 'song-webdav',
      }))
    })

    const callsAfterPlay = prefetchWebDavAudioFileMock.mock.calls.length
    removeSongFromQueue('song-webdav')

    await vi.waitFor(() => {
      expect(prefetchWebDavAudioFileMock.mock.calls.length).toBeGreaterThan(callsAfterPlay)
    })
    expect(prefetchWebDavAudioFileMock).toHaveBeenLastCalledWith(expect.objectContaining({
      songId: 'song-webdav-2',
      url: 'https://example.com/dav/music/another.flac',
    }))
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

  test('播放失败时自动跳过下一首并恢复播放', async () => {
    const secondSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([localSong, secondSong]))
    const { clearQueue, enqueueSongs, playSong, playerState } = await import('@/features/player/controller')
    clearQueue()
    enqueueSongs([localSong, secondSong])
    nativePlayer.play.mockRejectedValueOnce(new Error('native secret url'))
      .mockResolvedValueOnce(undefined)

    await playSong(localSong)

    expect(nativePlayer.play).toHaveBeenCalledTimes(2)
    expect(playerState.currentSong?.id).toBe(secondSong.id)
    expect(playerState.status).toBe('playing')
    expect(playerState.errorMessage).toBeNull()
  })

  test('连续失败时每首只尝试一次并保留安全错误', async () => {
    const songs = [0, 1, 2].map((index) => ({
      ...localSong,
      id: `failed-${index}`,
      path: `failed-${index}.mp3`,
      uri: `content://music/failed-${index}`,
    }))
    localStorage.setItem('muses:songs', JSON.stringify(songs))
    const { clearQueue, enqueueSongs, playSong, playerState } = await import('@/features/player/controller')
    clearQueue()
    enqueueSongs(songs)
    nativePlayer.play.mockRejectedValueOnce(new Error('Authorization: password'))
      .mockRejectedValueOnce(new Error('Authorization: password'))
      .mockRejectedValueOnce(new Error('Authorization: password'))

    await playSong(songs[0])

    expect(nativePlayer.play).toHaveBeenCalledTimes(songs.length)
    expect(playerState.status).toBe('error')
    expect(playerState.errorMessage).toBe('播放失败，请稍后重试。')
    expect(JSON.stringify(playerState)).not.toContain('password')
  })

  test('单曲循环失败时不会重试自身', async () => {
    const secondSong = { ...localSong, id: 'song-local-2', path: 'second.mp3', uri: 'content://music/second' }
    localStorage.setItem('muses:songs', JSON.stringify([localSong, secondSong]))
    const { clearQueue, enqueueSongs, playSong, playerState, setRepeatMode } = await import('@/features/player/controller')
    clearQueue()
    setRepeatMode('one')
    enqueueSongs([localSong, secondSong])
    nativePlayer.play.mockRejectedValueOnce(new Error('bad file')).mockResolvedValueOnce(undefined)

    await playSong(localSong)

    expect(nativePlayer.play).toHaveBeenCalledTimes(2)
    expect(playerState.currentSong?.id).toBe(secondSong.id)
    expect(playerState.status).toBe('playing')
    expect(playerState.errorMessage).toBeNull()
    expect(playerState.currentSong?.id).not.toBe(localSong.id)

    const { queueState } = await import('@/features/player/controller')
    expect(queueState.repeatMode).toBe('one')
  })

  test('过期播放失败不会自动推进队列或覆盖新歌曲状态', async () => {
    const secondSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([localSong, secondSong]))
    const { clearQueue, enqueueSongs, playSong, playerState } = await import('@/features/player/controller')
    clearQueue()
    enqueueSongs([localSong, secondSong])

    let rejectFirst!: (error: Error) => void
    const firstPlay = new Promise<void>((_, reject) => { rejectFirst = reject })
    nativePlayer.play.mockImplementationOnce(() => firstPlay).mockResolvedValueOnce(undefined)

    const firstRequest = playSong(localSong)
    await vi.waitFor(() => expect(nativePlayer.play).toHaveBeenCalledTimes(1))
    await playSong(secondSong)
    rejectFirst(new Error('旧请求失败'))
    await firstRequest

    expect(nativePlayer.play).toHaveBeenCalledTimes(2)
    expect(playerState.currentSong?.id).toBe(secondSong.id)
    expect(playerState.status).toBe('playing')
    expect(playerState.errorMessage).toBeNull()
  })

  test('继续失败恢复时不会清理下一首媒体会话', async () => {
    const secondSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([localSong, secondSong]))
    const { MediaSession } = await import('@capgo/capacitor-media-session')
    const { clearQueue, enqueueSongs, playSong, playerState } = await import('@/features/player/controller')
    clearQueue()
    enqueueSongs([localSong, secondSong])
    await flushPromises()
    vi.mocked(MediaSession.setPlaybackState).mockClear()
    vi.mocked(MediaSession.setMetadata).mockClear()
    nativePlayer.play.mockRejectedValueOnce(new Error('第一首失败')).mockResolvedValueOnce(undefined)

    await playSong(localSong)

    expect(playerState.currentSong?.id).toBe(secondSong.id)
    expect(MediaSession.setPlaybackState).not.toHaveBeenCalledWith({ playbackState: 'none' })
    expect(MediaSession.setMetadata).not.toHaveBeenCalledWith(expect.objectContaining({ title: '' }))
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

describe('媒体通知封面同步', () => {
  afterEach(async () => {
    await resetPlayer()
    localStorage.clear()
  })

  test('A→B 不同封面时最终 artwork 使用 B 的 data URL', async () => {
    const { MediaSession } = await import('@capgo/capacitor-media-session')
    const { playSong } = await import('@/features/player/controller')
    const { EMPTY_ARTWORK_DATA_URL } = await import('@/features/player/mediaSession')

    const songA = {
      ...localSong,
      id: 'song-a',
      path: 'album/a.mp3',
      uri: 'content://music/a',
      coverUri: 'file:///cache/covers/a.jpg',
      title: '歌曲A',
      tagsScanned: true,
      metadataVersion: 2,
    }
    const songB = {
      ...localSong,
      id: 'song-b',
      path: 'album/b.mp3',
      uri: 'content://music/b',
      coverUri: 'file:///cache/covers/b.jpg',
      title: '歌曲B',
      tagsScanned: true,
      metadataVersion: 2,
    }
    const expectedB = `data:image/jpeg;base64,${btoa('file:///cache/covers/b.jpg')}`

    await playSong(songA)
    await vi.waitFor(() => {
      expect(audioPlayerBridge.prepareArtworkDataUrl).toHaveBeenCalledWith({ uri: 'file:///cache/covers/a.jpg' })
    })

    vi.mocked(MediaSession.setMetadata).mockClear()
    audioPlayerBridge.prepareArtworkDataUrl.mockClear()

    await playSong(songB)
    await vi.waitFor(() => {
      const calls = vi.mocked(MediaSession.setMetadata).mock.calls.map((call) => call[0])
      expect(calls.some((payload) => payload.title === '歌曲B' && payload.artwork?.[0]?.src === expectedB)).toBe(true)
    })

    const artworkCalls = vi.mocked(MediaSession.setMetadata).mock.calls
      .map((call) => call[0])
      .filter((payload) => payload.title === '歌曲B' && Array.isArray(payload.artwork) && payload.artwork.length > 0)

    expect(artworkCalls.length).toBeGreaterThanOrEqual(2)
    // 首帧占位清空 + 二次真实封面
    expect(artworkCalls[0].artwork?.[0]?.src).toBe(EMPTY_ARTWORK_DATA_URL)
    expect(artworkCalls[artworkCalls.length - 1].artwork?.[0]?.src).toBe(expectedB)
  })

  test('有封面切到无封面时用占位 data: 覆盖，不残留上一首', async () => {
    const { MediaSession } = await import('@capgo/capacitor-media-session')
    const { playSong } = await import('@/features/player/controller')
    const { EMPTY_ARTWORK_DATA_URL } = await import('@/features/player/mediaSession')

    await playSong({
      ...localSong,
      id: 'song-with-cover',
      path: 'album/with-cover.mp3',
      coverUri: 'file:///cache/covers/a.jpg',
      title: '有封面',
      tagsScanned: true,
      metadataVersion: 2,
    })
    await vi.waitFor(() => {
      expect(audioPlayerBridge.prepareArtworkDataUrl).toHaveBeenCalled()
    })

    vi.mocked(MediaSession.setMetadata).mockClear()
    audioPlayerBridge.prepareArtworkDataUrl.mockClear()

    await playSong({
      ...localSong,
      id: 'song-no-cover',
      path: 'album/no-cover.mp3',
      title: '无封面',
      tagsScanned: true,
      metadataVersion: 2,
    })

    const calls = vi.mocked(MediaSession.setMetadata).mock.calls.map((call) => call[0])
    expect(calls.length).toBeGreaterThan(0)
    const last = calls[calls.length - 1]
    expect(last.title).toBe('无封面')
    expect(last.artwork?.[0]?.src).toBe(EMPTY_ARTWORK_DATA_URL)
    expect(audioPlayerBridge.prepareArtworkDataUrl).not.toHaveBeenCalled()
  })

  test('懒扫描补全 coverUri 后会再次 setMetadata', async () => {
    const { MediaSession } = await import('@capgo/capacitor-media-session')
    // 预置库记录，保证 upsert 更新同一 id，而非 createSongId 新 id
    localStorage.setItem('muses:songs', JSON.stringify([localSong]))
    localLibraryNative.readMetadata.mockResolvedValueOnce({
      title: '本地歌曲',
      artist: '本地歌手',
      album: '本地专辑',
      coverUri: 'file:///cache/covers/scanned.jpg',
    })

    const { playSong, playerState } = await import('@/features/player/controller')
    const expectedCover = `data:image/jpeg;base64,${btoa('file:///cache/covers/scanned.jpg')}`

    await playSong({ ...localSong, tagsScanned: false })
    expect(playerState.metadataStatus).toBe('scanning')

    await vi.waitFor(() => {
      expect(playerState.metadataStatus).toBe('ready')
      expect(playerState.coverUri).toBe('file:///cache/covers/scanned.jpg')
    })

    await vi.waitFor(() => {
      expect(audioPlayerBridge.prepareArtworkDataUrl).toHaveBeenCalledWith({
        uri: 'file:///cache/covers/scanned.jpg',
      })
      const hasScannedArtwork = vi.mocked(MediaSession.setMetadata).mock.calls
        .some((call) => call[0].artwork?.[0]?.src === expectedCover)
      expect(hasScannedArtwork).toBe(true)
    })
  })

  test('无封面时在线匹配成功会写回 coverUri 并刷新展示', async () => {
    const { setOnlineCoverProvidersForTest } = await import('@/features/cover')
    const searchCoverUrl = vi.fn().mockResolvedValue('https://is1-ssl.mzstatic.com/image/a.jpg')
    setOnlineCoverProvidersForTest([
      { id: 'itunes', searchCoverUrl },
      { id: 'kw', searchCoverUrl: vi.fn() },
    ])

    cacheRemoteCoverMock.mockResolvedValueOnce('file:///cache/covers/online-matched.jpg')
    localStorage.setItem('muses:songs', JSON.stringify([{
      ...localSong,
      tagsScanned: true,
      metadataVersion: 2,
      coverUri: undefined,
    }]))

    const { playSong, playerState } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      tagsScanned: true,
      metadataVersion: 2,
      coverUri: undefined,
    })

    await vi.waitFor(() => {
      expect(searchCoverUrl).toHaveBeenCalled()
      expect(cacheRemoteCoverMock).toHaveBeenCalledWith({
        url: 'https://is1-ssl.mzstatic.com/image/a.jpg',
        cacheKey: `online:${localSong.id}`,
      })
      expect(playerState.coverUri).toBe('file:///cache/covers/online-matched.jpg')
    })

    const stored = JSON.parse(localStorage.getItem('muses:songs') || '[]') as Array<{ id: string; coverUri?: string }>
    const row = stored.find((item) => item.id === localSong.id)
    expect(row?.coverUri).toBe('file:///cache/covers/online-matched.jpg')
    expect(row?.coverUri?.startsWith('data:')).toBe(false)
    expect(String(localStorage.getItem('muses:songs'))).not.toContain('base64')

    setOnlineCoverProvidersForTest(null)
  })

  test('已有封面时不发起在线匹配', async () => {
    const { setOnlineCoverProvidersForTest } = await import('@/features/cover')
    const searchCoverUrl = vi.fn().mockResolvedValue('https://is1-ssl.mzstatic.com/image/a.jpg')
    setOnlineCoverProvidersForTest([{ id: 'itunes', searchCoverUrl }])

    localStorage.setItem('muses:songs', JSON.stringify([{
      ...localSong,
      coverUri: 'file:///cache/covers/existing.jpg',
      tagsScanned: true,
      metadataVersion: 2,
    }]))

    const { playSong, playerState } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      coverUri: 'file:///cache/covers/existing.jpg',
      tagsScanned: true,
      metadataVersion: 2,
    })

    await new Promise((resolve) => setTimeout(resolve, 30))
    expect(searchCoverUrl).not.toHaveBeenCalled()
    expect(cacheRemoteCoverMock).not.toHaveBeenCalled()
    expect(playerState.coverUri).toBe('file:///cache/covers/existing.jpg')

    setOnlineCoverProvidersForTest(null)
  })

  test('快速切歌时过期在线封面匹配不写回当前曲', async () => {
    const { setOnlineCoverProvidersForTest } = await import('@/features/cover')
    let resolveSlow: ((url: string | null) => void) | undefined
    const slowSearch = new Promise<string | null>((resolve) => {
      resolveSlow = resolve
    })
    const searchCoverUrl = vi.fn().mockReturnValueOnce(slowSearch)
    setOnlineCoverProvidersForTest([
      { id: 'itunes', searchCoverUrl },
      { id: 'kw', searchCoverUrl: vi.fn() },
    ])

    const songA: SongItem = {
      ...localSong,
      id: 'song-online-cover-a',
      path: '/music/a.mp3',
      uri: 'content://local/a.mp3',
      title: '歌曲A',
      tagsScanned: true,
      metadataVersion: 2,
      coverUri: undefined,
    }
    const songB: SongItem = {
      ...localSong,
      id: 'song-online-cover-b',
      path: '/music/b.mp3',
      uri: 'content://local/b.mp3',
      title: '歌曲B',
      tagsScanned: true,
      metadataVersion: 2,
      coverUri: 'file:///cache/covers/b-existing.jpg',
    }

    localStorage.setItem('muses:songs', JSON.stringify([songA, songB]))

    const { playSong, playerState } = await import('@/features/player/controller')
    await playSong(songA)

    await vi.waitFor(() => {
      expect(searchCoverUrl).toHaveBeenCalled()
    })

    // 切到 B（已有封面，不应被 A 的慢结果覆盖）
    setOnlineCoverProvidersForTest([
      { id: 'itunes', searchCoverUrl: vi.fn().mockResolvedValue(null) },
    ])
    await playSong(songB)
    expect(playerState.currentSong?.id).toBe(songB.id)
    expect(playerState.coverUri).toBe('file:///cache/covers/b-existing.jpg')

    cacheRemoteCoverMock.mockResolvedValueOnce('file:///cache/covers/stale-a.jpg')
    resolveSlow?.('https://is1-ssl.mzstatic.com/image/stale-a.jpg')

    await new Promise((resolve) => setTimeout(resolve, 40))

    expect(playerState.currentSong?.id).toBe(songB.id)
    expect(playerState.coverUri).toBe('file:///cache/covers/b-existing.jpg')
    const stored = JSON.parse(localStorage.getItem('muses:songs') || '[]') as Array<{ id: string; coverUri?: string }>
    const rowB = stored.find((item) => item.id === songB.id)
    expect(rowB?.coverUri).toBe('file:///cache/covers/b-existing.jpg')
    // 过期结果不得写回 A 或污染当前曲
    expect(playerState.coverUri).not.toBe('file:///cache/covers/stale-a.jpg')

    setOnlineCoverProvidersForTest(null)
  })

  test('在线封面不得把远程 URL 写回 muses:songs', async () => {
    const { setOnlineCoverProvidersForTest } = await import('@/features/cover')
    const searchCoverUrl = vi.fn().mockResolvedValue('https://is1-ssl.mzstatic.com/image/a.jpg')
    setOnlineCoverProvidersForTest([{ id: 'itunes', searchCoverUrl }])

    // 模拟 bridge 误返回远程 URL（或未落盘）
    cacheRemoteCoverMock.mockResolvedValueOnce('https://is1-ssl.mzstatic.com/image/a.jpg')
    localStorage.setItem('muses:songs', JSON.stringify([{
      ...localSong,
      tagsScanned: true,
      metadataVersion: 2,
      coverUri: undefined,
    }]))

    const { playSong, playerState } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      tagsScanned: true,
      metadataVersion: 2,
      coverUri: undefined,
    })

    await vi.waitFor(() => {
      expect(searchCoverUrl).toHaveBeenCalled()
      expect(cacheRemoteCoverMock).toHaveBeenCalled()
    })
    await new Promise((resolve) => setTimeout(resolve, 30))

    expect(playerState.coverUri).toBeNull()
    const raw = String(localStorage.getItem('muses:songs') || '')
    expect(raw).not.toContain('https://is1-ssl.mzstatic.com')
    expect(raw).not.toContain('base64')

    setOnlineCoverProvidersForTest(null)
  })

  test('在线封面写回后不得清空已展示的在线歌词', async () => {
    const lyricsMod = await import('@/features/lyrics')
    vi.mocked(lyricsMod.matchOnlineLyrics).mockResolvedValueOnce({
      ok: true,
      text: '[00:01.00]在线词',
      format: 'lrc',
      source: 'kw',
    })

    const { setOnlineCoverProvidersForTest } = await import('@/features/cover')
    const searchCoverUrl = vi.fn().mockResolvedValue('https://is1-ssl.mzstatic.com/image/a.jpg')
    setOnlineCoverProvidersForTest([{ id: 'itunes', searchCoverUrl }])
    cacheRemoteCoverMock.mockResolvedValueOnce('file:///cache/covers/online-a.jpg')

    localStorage.setItem('muses:songs', JSON.stringify([{
      ...localSong,
      tagsScanned: true,
      metadataVersion: 2,
      coverUri: undefined,
      lyrics: undefined,
    }]))

    const { playSong, playerState } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      tagsScanned: true,
      metadataVersion: 2,
      coverUri: undefined,
    })

    await vi.waitFor(() => {
      expect(playerState.onlineLyricsStatus).toBe('ready')
      expect(playerState.lyrics).toBe('[00:01.00]在线词')
    })

    await vi.waitFor(() => {
      expect(playerState.coverUri).toBe('file:///cache/covers/online-a.jpg')
    })

    // 封面 sync 后歌词必须仍在
    expect(playerState.lyrics).toBe('[00:01.00]在线词')
    expect(playerState.lyricsFormat).toBe('lrc')

    setOnlineCoverProvidersForTest(null)
    vi.mocked(lyricsMod.matchOnlineLyrics).mockResolvedValue({ ok: false, reason: 'no-match' })
  })

  test('快速切歌时过期 token 丢弃旧封面回调', async () => {
    const { MediaSession } = await import('@capgo/capacitor-media-session')
    let resolveA: ((value: { dataUrl: string | null }) => void) | undefined
    const prepareA = new Promise<{ dataUrl: string | null }>((resolve) => {
      resolveA = resolve
    })

    audioPlayerBridge.prepareArtworkDataUrl.mockImplementationOnce(async () => prepareA)
    audioPlayerBridge.prepareArtworkDataUrl.mockImplementationOnce(async ({ uri }: { uri: string }) => ({
      dataUrl: `data:image/jpeg;base64,${btoa(uri)}`,
    }))

    const { playSong } = await import('@/features/player/controller')
    const expectedB = `data:image/jpeg;base64,${btoa('file:///cache/covers/fast-b.jpg')}`
    const expectedA = `data:image/jpeg;base64,${btoa('file:///cache/covers/slow-a.jpg')}`

    const playA = playSong({
      ...localSong,
      id: 'song-slow-a',
      path: 'album/slow-a.mp3',
      title: '慢封面A',
      coverUri: 'file:///cache/covers/slow-a.jpg',
      tagsScanned: true,
      metadataVersion: 2,
    })
    // 等 A 的首帧 metadata 发出并开始 prepare
    await vi.waitFor(() => {
      expect(audioPlayerBridge.prepareArtworkDataUrl).toHaveBeenCalledWith({ uri: 'file:///cache/covers/slow-a.jpg' })
    })

    await playSong({
      ...localSong,
      id: 'song-fast-b',
      path: 'album/fast-b.mp3',
      title: '快封面B',
      coverUri: 'file:///cache/covers/fast-b.jpg',
      tagsScanned: true,
      metadataVersion: 2,
    })
    await vi.waitFor(() => {
      expect(audioPlayerBridge.prepareArtworkDataUrl).toHaveBeenCalledWith({ uri: 'file:///cache/covers/fast-b.jpg' })
      const hasB = vi.mocked(MediaSession.setMetadata).mock.calls
        .some((call) => call[0].artwork?.[0]?.src === expectedB)
      expect(hasB).toBe(true)
    })

    // A 的慢回调在 B 之后才返回，必须被 token 丢弃
    resolveA?.({ dataUrl: expectedA })
    await playA

    await Promise.resolve()
    await Promise.resolve()

    const allArtwork = vi.mocked(MediaSession.setMetadata).mock.calls
      .map((call) => call[0].artwork?.[0]?.src)
      .filter(Boolean)

    expect(allArtwork.includes(expectedA)).toBe(false)
    expect(allArtwork.includes(expectedB)).toBe(true)
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
          IonButton: { template: '<button v-bind="$attrs"><slot /></button>' },
          IonIcon: true,
        },
      },
    })

    expect(wrapper.text()).toContain('本地歌曲')
    expect(wrapper.text()).toContain('本地歌手')

    const { playerOverlayVisible, closePlayerOverlay, queueOverlayVisible, closeQueueOverlay } = await import('@/features/player/overlay')
    closePlayerOverlay()
    expect(playerOverlayVisible.value).toBe(false)

    await wrapper.trigger('click')
    expect(playerOverlayVisible.value).toBe(true)
    expect(routerPush).not.toHaveBeenCalled()
    closePlayerOverlay()

    await wrapper.get('button[aria-label="暂停播放"]').trigger('click')
    expect(nativePlayer.pause).toHaveBeenCalled()
    expect(playerOverlayVisible.value).toBe(false)
    expect(routerPush).not.toHaveBeenCalled()

    await wrapper.get('button[aria-label="打开播放队列"]').trigger('click')
    expect(queueOverlayVisible.value).toBe(true)
    expect(routerPush).not.toHaveBeenCalled()
    closeQueueOverlay()
  })

  test('无当前歌曲时点击或键盘操作主体不打开沉浸式播放页', async () => {
    const wrapper = mount(MiniPlayer, {
      global: {
        stubs: {
          IonButton: {
            props: ['disabled'],
            template: '<button :aria-label="$attrs[\'aria-label\']" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>',
          },
          IonIcon: true,
        },
      },
    })

    const { playerOverlayVisible, closePlayerOverlay, queueOverlayVisible, closeQueueOverlay } = await import('@/features/player/overlay')
    closePlayerOverlay()
    expect(playerOverlayVisible.value).toBe(false)

    expect(wrapper.text()).toContain('暂无播放歌曲')
    expect(wrapper.attributes('aria-disabled')).toBe('true')
    expect(wrapper.classes()).toContain('is-empty')

    await wrapper.trigger('click')
    expect(playerOverlayVisible.value).toBe(false)

    await wrapper.trigger('keyup.enter')
    expect(playerOverlayVisible.value).toBe(false)

    await wrapper.trigger('keyup.space')
    expect(playerOverlayVisible.value).toBe(false)
    expect(routerPush).not.toHaveBeenCalled()

    const playButton = wrapper.get('button[aria-label="继续播放"]')
    expect(playButton.attributes('disabled')).toBeDefined()

    await wrapper.get('button[aria-label="打开播放队列"]').trigger('click')
    expect(queueOverlayVisible.value).toBe(true)
    expect(routerPush).not.toHaveBeenCalled()
    closeQueueOverlay()
  })
})

describe('沉浸式播放页', () => {
  afterEach(async () => {
    await resetPlayer()
    localStorage.clear()
  })

  test('没有当前歌曲时展示空状态', () => {
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
    expect(wrapper.text()).not.toContain('正在播放')
    expect(wrapper.text()).not.toContain('返回')
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
          IonIcon: { props: ['icon'], template: '<span data-test="icon" :data-icon="JSON.stringify(icon)" />' },
        },
      },
    })

    expect(wrapper.text()).toContain('本地歌曲')
    expect(wrapper.text()).toContain('暂无歌词')
    expect(wrapper.text()).not.toContain('正在播放')
    expect(wrapper.text()).not.toContain('列表循环')
    expect(wrapper.text()).not.toContain('顺序播放')
    expect(wrapper.get('img[alt="歌曲封面"]').attributes('src')).toBe('webview:file:///cover.jpg')
    expect(wrapper.find('button[aria-label="上一曲"]').exists()).toBe(true)
    expect(wrapper.find('button[aria-label="下一曲"]').exists()).toBe(true)
    expect(wrapper.find('button[aria-label="播放或暂停"]').exists()).toBe(true)
    expect(wrapper.get('button[aria-label="上一曲"]').attributes('fill')).toBe('clear')
    expect(wrapper.get('button[aria-label="下一曲"]').attributes('fill')).toBe('clear')
    expect(wrapper.get('button[aria-label="播放或暂停"]').attributes('fill')).toBe('clear')
    expect(wrapper.find('button[aria-label="列表循环"]').exists()).toBe(true)
    expect(wrapper.find('button[aria-label="顺序播放"]').exists()).toBe(true)
    expect(wrapper.find('button[aria-label="播放队列"]').exists()).toBe(true)

    const modeIcon = (label: string) => wrapper.get(`button[aria-label="${label}"] [data-test="icon"]`).attributes('data-icon')

    // 初始四种状态的标签和图标必须一一对应。
    expect(modeIcon('列表循环')).toBe(JSON.stringify(repeatOutline))
    expect(modeIcon('顺序播放')).toBe(JSON.stringify(listOutline))

    // 循环 / 随机模式按钮应可切换，并立即刷新标签和图标。
    expect(queueState.repeatMode).toBe('all')
    await wrapper.get('button[aria-label="列表循环"]').trigger('click')
    expect(queueState.repeatMode).toBe('one')
    expect(modeIcon('单曲循环')).toBe(JSON.stringify(repeat))
    await wrapper.get('button[aria-label="单曲循环"]').trigger('click')
    expect(queueState.repeatMode).toBe('all')
    expect(modeIcon('列表循环')).toBe(JSON.stringify(repeatOutline))

    expect(queueState.shuffleEnabled).toBe(false)
    await wrapper.get('button[aria-label="顺序播放"]').trigger('click')
    expect(queueState.shuffleEnabled).toBe(true)
    expect(modeIcon('随机播放')).toBe(JSON.stringify(shuffle))
    await wrapper.get('button[aria-label="随机播放"]').trigger('click')
    expect(queueState.shuffleEnabled).toBe(false)
    expect(modeIcon('顺序播放')).toBe(JSON.stringify(listOutline))

    const slider = wrapper.get('input[aria-label="播放进度"]')
    await slider.setValue('60')
    await slider.trigger('input')
    expect(wrapper.get('.progress-track-played').attributes('style')).toContain('width: 33.33333333333333%')
    await slider.trigger('change')
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 60 })

    const track = wrapper.get('.progress-track')
    Object.defineProperty(track.element, 'getBoundingClientRect', {
      configurable: true,
      value: () => ({ left: 10, width: 200, top: 0, right: 210, bottom: 24, height: 24 }),
    })
    nativePlayer.seek.mockClear()
    await track.trigger('click', { clientX: 110 })
    await flushPromises()
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 90 })

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
    expect(wrapper.find('[data-test="amll-lyrics"]').exists()).toBe(true)

    const panels = wrapper.get('.panels')
    await panels.trigger('touchstart', { changedTouches: [{ clientX: 320 }] })
    await panels.trigger('touchend', { changedTouches: [{ clientX: 120 }] })

    expect(panels.attributes('style')).toContain('translateX(-50%)')
    expect(wrapper.get('[data-test="amll-lyrics"]').text()).toContain('第一句歌词')
    expect(wrapper.get('[data-test="amll-lyrics"]').attributes('data-align-position')).toBe('0.5')
    expect(wrapper.get('.lyric-header .lyric-title').text()).toBe('本地歌曲')
    expect(wrapper.get('.lyric-header .lyric-artist').text()).toBe('本地歌手')
    expect(wrapper.find('.lyric-panel .progress-slider').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('暂无歌词')
  })

  test('翻译副行样式跟随 AMLL 实际激活类高亮', () => {
    const source = readFileSync('src/views/PlayerPage.vue', 'utf8')
    expect(source).toContain('.FmKaba_lyricLine.FmKaba_active .FmKaba_lyricSubLine')
    expect(source).toContain('.FmKaba_lyricMainLine.FmKaba_active ~ .FmKaba_lyricSubLine')
  })

  test('歌词页翻译按钮可切换 aria 状态，手机显示播放按钮', async () => {
    const { playSong } = await import('@/features/player/controller')
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 390 })
    window.dispatchEvent(new Event('resize'))
    await playSong({
      ...localSong,
      lyrics: '[00:01.00]第一句歌词',
      lyricsSource: 'embedded',
    })

    const wrapper = mount(PlayerPage, {
      global: {
        stubs: {
          IonPage: { template: '<main><slot /></main>' },
          IonContent: { template: '<section><slot /></section>' },
          IonButton: { template: '<button v-bind="$attrs"><slot /></button>' },
          IonIcon: true,
        },
      },
    })

    expect(wrapper.find('[data-test="amll-lyrics"]').exists()).toBe(true)
    expect(wrapper.find('button[aria-label="暂停播放"]').exists()).toBe(true)
    const translateButton = wrapper.get('button[aria-label="隐藏翻译"]')
    await translateButton.trigger('click')
    await nextTick()
    expect(wrapper.find('button[aria-label="显示翻译"]').exists()).toBe(true)
    await wrapper.get('button[aria-label="显示翻译"]').trigger('click')
    await nextTick()
    expect(wrapper.find('button[aria-label="隐藏翻译"]').exists()).toBe(true)
  })

  test('平板模式歌词页不展示右下播放暂停按钮', async () => {
    const { playSong } = await import('@/features/player/controller')
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 1024 })
    window.dispatchEvent(new Event('resize'))
    await playSong({
      ...localSong,
      lyrics: '[00:01.00]第一句歌词',
      lyricsSource: 'embedded',
    })

    const wrapper = mount(PlayerPage, {
      global: {
        stubs: {
          IonPage: { template: '<main><slot /></main>' },
          IonContent: { template: '<section><slot /></section>' },
          IonButton: { template: '<button v-bind="$attrs"><slot /></button>' },
          IonIcon: true,
        },
      },
    })

    expect(wrapper.find('button[aria-label="隐藏翻译"]').exists()).toBe(true)
    expect(wrapper.find('button[aria-label="暂停播放"]').exists()).toBe(false)
    expect(wrapper.find('button[aria-label="继续播放"]').exists()).toBe(false)
  })

  test('仅有封面无歌词时仍展示 AMLL 动态背景（不依赖 hasLyrics）', async () => {
    const { playSong } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      coverUri: 'file:///cover-only.jpg',
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

    expect(wrapper.find('[data-test="amll-lyrics"]').exists()).toBe(false)
    expect(wrapper.get('.amll-background [data-test="amll-background"]').attributes('data-album')).toBe(
      'webview:file:///cover-only.jpg',
    )
  })

  test('切到无封面歌曲时粘性保留上一首封面作背景', async () => {
    const { playSong } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      coverUri: 'file:///cover-a.jpg',
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

    expect(wrapper.get('.amll-background [data-test="amll-background"]').attributes('data-album')).toBe(
      'webview:file:///cover-a.jpg',
    )

    await playSong({
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首',
      coverUri: undefined,
    })
    await nextTick()

    expect(wrapper.get('.amll-background [data-test="amll-background"]').attributes('data-album')).toBe(
      'webview:file:///cover-a.jpg',
    )
    expect(wrapper.get('img[alt="歌曲封面"]').attributes('src')).toBe('webview:file:///cover-a.jpg')
  })

  test('无歌词时歌词页仍展示顶部歌名歌手与空状态', async () => {
    const { playSong } = await import('@/features/player/controller')
    await playSong(localSong)

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

    expect(wrapper.get('.lyric-header .lyric-title').text()).toBe('本地歌曲')
    expect(wrapper.get('.lyric-header .lyric-artist').text()).toBe('本地歌手')
    expect(wrapper.find('[data-test="amll-lyrics"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('暂无歌词')
  })

  test('无歌手时歌词页只展示歌名不回退未知歌手', async () => {
    const { playSong } = await import('@/features/player/controller')
    await playSong({ ...localSong, artist: '' })

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

    expect(wrapper.get('.lyric-header .lyric-title').text()).toBe('本地歌曲')
    expect(wrapper.find('.lyric-header .lyric-artist').exists()).toBe(false)
    expect(wrapper.get('.lyric-header').text()).not.toContain('未知歌手')
  })

  test('点击歌词行会 seek 到该行起始秒', async () => {
    const { playSong } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      duration: 180,
      lyrics: '[ar:测试歌手]\n[00:01.00]第一句歌词\n[00:05.00]第二句歌词',
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

    expect(wrapper.find('[data-test="amll-lyrics"]').exists()).toBe(true)
    nativePlayer.seek.mockClear()

    await wrapper.get('[data-test="lyric-line-click"]').trigger('click')

    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 5 })
  })

  test('点击 startTime 无效的歌词行不会 seek', async () => {
    const { playSong } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      duration: 180,
      lyrics: '[ar:测试歌手]\n[00:01.00]第一句歌词\n[00:05.00]第二句歌词',
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

    const lyric = wrapper.findComponent('[data-test="amll-lyrics"]')
    nativePlayer.seek.mockClear()

    const emitInvalid = async (startTime?: number) => {
      await lyric.vm.$emit('lineClick', {
        lineIndex: 0,
        line: {
          getLine: () => (startTime === undefined ? {} : { startTime }),
        },
        stopPropagation: () => undefined,
        preventDefault: () => undefined,
      })
    }

    await emitInvalid(Number.NaN)
    await emitInvalid(-1)
    await emitInvalid(undefined)

    expect(nativePlayer.seek).not.toHaveBeenCalled()
  })

  test('拖动进度条期间不会误触上一曲/下一曲，也不会切换歌词面板', async () => {
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([{ ...localSong, duration: 180 }, secondLocalSong]))
    const {
      clearQueue,
      enqueueSongs,
      playSong,
      playerState,
      queueState,
      setRepeatMode,
      toggleShuffle,
    } = await import('@/features/player/controller')
    clearQueue()
    setRepeatMode('all')
    if (queueState.shuffleEnabled) {
      toggleShuffle()
    }
    enqueueSongs([{ ...localSong, duration: 180 }, secondLocalSong])
    await playSong({ ...localSong, duration: 180 })
    expect(playerState.currentSong?.id).toBe('song-local')
    expect(playerState.duration).toBe(180)

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

    const progressArea = wrapper.get('.progress-area')
    const slider = wrapper.get('input[aria-label="播放进度"]')
    const overlay = wrapper.get('.player-overlay')
    const panels = wrapper.get('.panels')

    // seek 本身仍可用（先验证，避免后续锁逻辑掩盖）
    nativePlayer.seek.mockClear()
    await slider.setValue('45')
    await slider.trigger('change')
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 45 })

    // 进度条按下：应隔离 overlay 手势
    await progressArea.trigger('touchstart')
    await progressArea.trigger('pointerdown')

    // 即使横向位移很大，也不应切换到歌词面板
    await overlay.trigger('touchstart', { changedTouches: [{ clientX: 320, clientY: 400 }] })
    await overlay.trigger('touchend', { changedTouches: [{ clientX: 80, clientY: 400 }] })
    expect(panels.attributes('style') || '').not.toContain('translateX(-50%)')

    // 松手瞬间点到上一曲/下一曲应被 seek 锁吞掉
    nativePlayer.play.mockClear()
    await wrapper.get('button[aria-label="下一曲"]').trigger('click')
    await wrapper.get('button[aria-label="上一曲"]').trigger('click')
    expect(nativePlayer.play).not.toHaveBeenCalled()
    expect(playerState.currentSong?.id).toBe('song-local')

    await progressArea.trigger('touchend')
    await progressArea.trigger('pointerup')

    // 保护期内仍不可 prev/next
    nativePlayer.play.mockClear()
    await wrapper.get('button[aria-label="下一曲"]').trigger('click')
    expect(nativePlayer.play).not.toHaveBeenCalled()

    // 保护期结束后恢复正常（略大于 300ms debounce）
    await new Promise((resolve) => setTimeout(resolve, 320))
    await wrapper.get('button[aria-label="下一曲"]').trigger('click')
    expect(nativePlayer.play).toHaveBeenCalledWith(expect.objectContaining({ songId: 'song-local-2' }))
  })

  test('歌词区域内上下滑动不关闭 overlay、不产生下拉位移，横向滑动仍可切回控制页', async () => {
    const { playSong } = await import('@/features/player/controller')
    const { playerOverlayVisible, openPlayerOverlay, closePlayerOverlay } = await import('@/features/player/overlay')
    closePlayerOverlay()
    await playSong({
      ...localSong,
      lyrics: '[00:01.00]第一句歌词\n[00:05.00]第二句歌词',
      lyricsSource: 'embedded',
    })
    openPlayerOverlay()
    expect(playerOverlayVisible.value).toBe(true)

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

    const overlay = wrapper.get('.player-overlay')
    const shell = wrapper.get('.immersive-shell')
    const lyricPlayer = wrapper.get('[data-test="amll-lyrics"]')
    const panels = wrapper.get('.panels')

    // 先右滑到歌词页（activePanel = 1）
    await overlay.trigger('touchstart', { changedTouches: [{ clientX: 320, clientY: 400 }] })
    await overlay.trigger('touchend', { changedTouches: [{ clientX: 80, clientY: 400 }] })
    expect(panels.attributes('style')).toContain('translateX(-50%)')

    // 在歌词区域内大幅上下滑动：不应产生 dragOffsetY，也不应关闭 overlay
    await lyricPlayer.trigger('touchstart', { changedTouches: [{ clientX: 200, clientY: 100 }] })
    await lyricPlayer.trigger('touchmove', { changedTouches: [{ clientX: 200, clientY: 360 }] })
    await lyricPlayer.trigger('touchend', { changedTouches: [{ clientX: 200, clientY: 360 }] })

    expect(shell.attributes('style') || '').toMatch(/translateY\(0px\)/)
    expect(playerOverlayVisible.value).toBe(true)

    // 歌词区域内横向滑动仍可切回控制页
    await lyricPlayer.trigger('touchstart', { changedTouches: [{ clientX: 80, clientY: 400 }] })
    await lyricPlayer.trigger('touchend', { changedTouches: [{ clientX: 320, clientY: 400 }] })
    expect(panels.attributes('style') || '').not.toContain('translateX(-50%)')
    expect(playerOverlayVisible.value).toBe(true)

    closePlayerOverlay()
  })

  test('控制页上下滑动仍可关闭 overlay', async () => {
    const { playSong } = await import('@/features/player/controller')
    const { playerOverlayVisible, openPlayerOverlay, closePlayerOverlay } = await import('@/features/player/overlay')
    closePlayerOverlay()
    await playSong(localSong)
    openPlayerOverlay()
    expect(playerOverlayVisible.value).toBe(true)

    // 固定 dismiss 阈值依赖的 window.innerHeight，保证测试稳定
    Object.defineProperty(window, 'innerHeight', { configurable: true, value: 800 })

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

    const infoPanel = wrapper.get('.info-panel')

    // 控制页大幅下滑超过阈值 → 关闭 overlay
    await infoPanel.trigger('touchstart', { changedTouches: [{ clientX: 200, clientY: 80 }] })
    await infoPanel.trigger('touchmove', { changedTouches: [{ clientX: 200, clientY: 320 }] })
    expect(wrapper.get('.immersive-shell').attributes('style') || '').toContain('translateY(240px)')
    await infoPanel.trigger('touchend', { changedTouches: [{ clientX: 200, clientY: 320 }] })

    expect(playerOverlayVisible.value).toBe(false)
    expect(wrapper.get('.immersive-shell').attributes('style') || '').toContain('translateY(0px)')

    // 保活页面再打开时内部位移仍必须归零，避免只展开一半（#25）
    openPlayerOverlay()
    await nextTick()
    expect(playerOverlayVisible.value).toBe(true)
    expect(wrapper.get('.immersive-shell').attributes('style') || '').toContain('translateY(0px)')
  })

})

describe('应用壳', () => {
  const mountApp = () => mount(App, {
    global: {
      stubs: {
        IonApp: { template: '<main><slot /></main>' },
        IonRouterOutlet: { template: '<div />' },
        PlayerPage: true,
        QueuePage: true,
      },
    },
  })
  const flushStatusBar = async () => {
    await nextTick()
    for (let index = 0; index < 6; index += 1) {
      await Promise.resolve()
    }
  }

  beforeEach(async () => {
    const { closePlayerOverlay, closeQueueOverlay } = await import('@/features/player/overlay')
    closePlayerOverlay()
    closeQueueOverlay()
    statusBarSetStyle.mockReset()
    statusBarSetStyle.mockResolvedValue(undefined)
  })

  afterEach(async () => {
    routeState.path = '/tabs/songs'
    await resetPlayer()
    localStorage.clear()
  })

  test('打开沉浸式播放页时迷你播放条保持挂载并禁用交互', async () => {
    const { playSong } = await import('@/features/player/controller')
    const { openPlayerOverlay, closePlayerOverlay } = await import('@/features/player/overlay')
    await playSong(localSong)
    openPlayerOverlay()

    const wrapper = mountApp()

    expect(wrapper.find('.mini-player').exists()).toBe(true)
    expect(wrapper.find('.app-mini-player').classes()).toContain('is-overlay-active')
    closePlayerOverlay()
    wrapper.unmount()
  })

  test('有当前曲时关闭沉浸式仍保活 PlayerPage，无曲后可卸载', async () => {
    const { playSong, stopPlayback } = await import('@/features/player/controller')
    const { openPlayerOverlay, closePlayerOverlay } = await import('@/features/player/overlay')
    await playSong(localSong)
    openPlayerOverlay()

    const wrapper = mount(App, {
      global: {
        stubs: {
          IonApp: { template: '<main><slot /></main>' },
          IonRouterOutlet: { template: '<div />' },
          PlayerPage: { template: '<div data-test="player-page-root" />' },
          QueuePage: true,
        },
      },
    })

    expect(wrapper.find('[data-test="player-page-root"]').exists()).toBe(true)
    expect(wrapper.find('.app-player-page').classes()).toContain('is-player-visible')

    closePlayerOverlay()
    await nextTick()
    // 关闭后仍挂载，仅去掉可见 class
    expect(wrapper.find('[data-test="player-page-root"]').exists()).toBe(true)
    expect(wrapper.find('.app-player-page').classes()).not.toContain('is-player-visible')

    await stopPlayback()
    await nextTick()
    expect(wrapper.find('[data-test="player-page-root"]').exists()).toBe(false)
    wrapper.unmount()
  })

  test('播放器 overlay 打开时使用白色状态栏内容，关闭后恢复默认', async () => {
    const { openPlayerOverlay, closePlayerOverlay } = await import('@/features/player/overlay')
    const wrapper = mountApp()

    openPlayerOverlay()
    await flushStatusBar()
    expect(statusBarSetStyle).toHaveBeenLastCalledWith({ style: 'DARK' })

    closePlayerOverlay()
    await flushStatusBar()
    expect(statusBarSetStyle).toHaveBeenLastCalledWith({ style: 'DEFAULT' })
    wrapper.unmount()
  })

  test('单独打开队列 overlay 不修改状态栏', async () => {
    const { openQueueOverlay } = await import('@/features/player/overlay')
    const wrapper = mountApp()

    openQueueOverlay()
    await flushStatusBar()
    expect(statusBarSetStyle).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  test('状态栏插件失败时静默忽略且 overlay 状态正常更新', async () => {
    const { playerOverlayVisible, openPlayerOverlay, closePlayerOverlay } = await import('@/features/player/overlay')
    statusBarSetStyle.mockRejectedValueOnce(new Error('插件不可用'))
    const wrapper = mountApp()

    openPlayerOverlay()
    await flushStatusBar()
    expect(playerOverlayVisible.value).toBe(true)

    closePlayerOverlay()
    await flushStatusBar()
    expect(playerOverlayVisible.value).toBe(false)
    expect(statusBarSetStyle).toHaveBeenLastCalledWith({ style: 'DEFAULT' })
    wrapper.unmount()
  })

  test('快速开关播放器 overlay 后最终应用最新状态栏样式', async () => {
    const { openPlayerOverlay, closePlayerOverlay } = await import('@/features/player/overlay')
    const appliedStyles: string[] = []
    let resolveDarkStyle: (() => void) | undefined
    statusBarSetStyle.mockImplementation(({ style }: { style: string }) => {
      if (style === 'DARK' && !resolveDarkStyle) {
        return new Promise<void>((resolve) => {
          resolveDarkStyle = () => {
            appliedStyles.push(style)
            resolve()
          }
        })
      }

      appliedStyles.push(style)
      return Promise.resolve()
    })
    const wrapper = mountApp()

    openPlayerOverlay()
    await flushStatusBar()
    closePlayerOverlay()
    openPlayerOverlay()
    closePlayerOverlay()
    await flushStatusBar()
    resolveDarkStyle?.()
    await flushStatusBar()

    expect(appliedStyles).toEqual(['DARK', 'DEFAULT'])
    wrapper.unmount()
  })

  test('应用壳卸载时等待在途请求后恢复默认状态栏样式', async () => {
    const { openPlayerOverlay } = await import('@/features/player/overlay')
    const appliedStyles: string[] = []
    let resolveDarkStyle: (() => void) | undefined
    statusBarSetStyle.mockImplementation(({ style }: { style: string }) => {
      if (style === 'DARK') {
        return new Promise<void>((resolve) => {
          resolveDarkStyle = () => {
            appliedStyles.push(style)
            resolve()
          }
        })
      }

      appliedStyles.push(style)
      return Promise.resolve()
    })
    const wrapper = mountApp()
    openPlayerOverlay()
    await flushStatusBar()

    wrapper.unmount()
    await flushStatusBar()
    expect(appliedStyles).toEqual([])

    resolveDarkStyle?.()
    await flushStatusBar()
    expect(appliedStyles).toEqual(['DARK', 'DEFAULT'])
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

  test('peekNext 与 advanceToNext 目标一致但不改 currentIndex', async () => {
    seedSongs()
    const { advanceToNext, enqueueSongs, peekNext, queueState, selectSongAtIndex } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])
    selectSongAtIndex(0)
    expect(queueState.currentIndex).toBe(0)

    const peeked = peekNext()
    expect(peeked?.id).toBe('song-webdav')
    expect(queueState.currentIndex).toBe(0)

    const advanced = advanceToNext()
    expect(advanced?.id).toBe('song-webdav')
    expect(queueState.currentIndex).toBe(1)

    // 列表循环：peek 末尾回绕首曲，索引不变
    const peekedWrap = peekNext()
    expect(peekedWrap?.id).toBe('song-local')
    expect(queueState.currentIndex).toBe(1)
  })

  test('peekNext 单曲循环返回当前歌曲且不改索引', async () => {
    seedSongs()
    const { enqueueSongs, peekNext, queueState, selectSongAtIndex, setRepeatMode } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])
    selectSongAtIndex(1)
    setRepeatMode('one')

    const peeked = peekNext()
    expect(peeked?.id).toBe('song-webdav')
    expect(queueState.currentIndex).toBe(1)
  })

  test('peekNext 空队列返回 null', async () => {
    const { clearQueue, peekNext } = await import('@/features/player/queue')
    clearQueue()
    expect(peekNext()).toBeNull()
  })

  test('peekNext 随机模式下与 advanceToNext 目标一致', async () => {
    seedSongs()
    const { advanceToNext, enqueueSongs, peekNext, queueState, selectSongAtIndex, toggleShuffle } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong])
    toggleShuffle()
    selectSongAtIndex(0)
    const indexBefore = queueState.currentIndex

    const peeked = peekNext()
    expect(queueState.currentIndex).toBe(indexBefore)
    const advanced = advanceToNext()
    expect(advanced?.id).toBe(peeked?.id)
  })

  test('失败恢复按当前 shuffleOrder 推进并跳过已尝试歌曲', async () => {
    const thirdSong: SongItem = {
      ...localSong,
      id: 'song-local-3',
      path: 'album/third.mp3',
      uri: 'content://music/third',
      title: '第三首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([localSong, webDavSong, thirdSong]))
    const {
      advanceToNextRecoveryCandidate,
      enqueueSongs,
      queueState,
      selectSongAtIndex,
      toggleShuffle,
    } = await import('@/features/player/queue')
    enqueueSongs([localSong, webDavSong, thirdSong])
    toggleShuffle()
    selectSongAtIndex(0)
    const activeOrder = queueState.items.map((song) => song.id)

    const next = advanceToNextRecoveryCandidate(new Set([activeOrder[0]]))

    expect(next?.id).toBe(activeOrder[1])
    expect(queueState.currentIndex).toBe(1)
  })

  test('失败恢复遇到缺失曲库记录时有界跳过且不会重复候选', async () => {
    localStorage.setItem('muses:songs', JSON.stringify([localSong]))
    localStorage.setItem('muses:queue', JSON.stringify({
      items: [{ songId: 'missing-song' }, { songId: localSong.id }],
      originalOrder: [{ songId: 'missing-song' }, { songId: localSong.id }],
      shuffleOrder: null,
    }))
    const { advanceToNextRecoveryCandidate, queueState } = await import('@/features/player/queue')

    const candidate = advanceToNextRecoveryCandidate(new Set())
    expect(candidate?.id).toBe(localSong.id)
    expect(queueState.currentIndex).toBe(1)

    expect(advanceToNextRecoveryCandidate(new Set([localSong.id]))).toBeNull()
    expect(queueState.currentIndex).toBe(1)
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
    const { SecureStorage } = await import('@aparajita/capacitor-secure-storage')
    vi.mocked(SecureStorage.get).mockResolvedValue('secret-password')
    localStorage.setItem('muses:sources', JSON.stringify([{
      id: 'source-webdav',
      type: 'webdav',
      name: '远程音乐',
      serverUrl: 'https://example.com/dav',
      username: 'alice',
      path: '/music',
      credentialKey: 'muses:webdav-password:source-webdav',
      createdAt: '2026-07-07T00:00:00.000Z',
    }]))
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

  test('applyNativeState 接收接近结尾的 finished 后调用 advanceToNext 自动播下一首', async () => {
    vi.resetModules()
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([{ ...localSong, duration: 180 }, secondLocalSong]))
    const { enqueueSongs, initializePlayer, playSong, playerState } = await import('@/features/player/controller')
    await initializePlayer()
    enqueueSongs([{ ...localSong, duration: 180 }, secondLocalSong])

    await playSong({ ...localSong, duration: 180 })
    expect(playerState.currentSong?.id).toBe('song-local')

    // 模拟 ExoPlayer 自然结束：进度接近结尾
    const listenerCalls = nativePlayer.addListener.mock.calls
    expect(listenerCalls.length).toBeGreaterThan(0)
    const stateChangeCallback = listenerCalls[0][1] as (state: unknown) => void

    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 179.5,
      duration: 180,
    })

    await vi.waitFor(() => {
      expect(nativePlayer.play).toHaveBeenCalledTimes(2)
    }, { timeout: 1000 })

    expect(nativePlayer.play).toHaveBeenCalledWith(expect.objectContaining({
      songId: 'song-local-2',
    }))
  })

  test('finished 且队列为空时调用 stopPlayback', async () => {
    vi.resetModules()
    const { clearQueue, initializePlayer, playSong, playerState } = await import('@/features/player/controller')
    clearQueue()
    await initializePlayer()

    await playSong({ ...localSong, duration: 180 })
    expect(playerState.currentSong?.id).toBe('song-local')

    const listenerCalls = nativePlayer.addListener.mock.calls
    const stateChangeCallback = listenerCalls[0][1] as (state: unknown) => void

    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 179.8,
      duration: 180,
    })

    await vi.waitFor(() => {
      expect(playerState.status).toBe('stopped')
    }, { timeout: 2000 })

    expect(playerState.currentSong).toBeNull()
  })

  test('seek 后立刻收到 finished 不会切到下一曲', async () => {
    vi.resetModules()
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([{ ...localSong, duration: 180 }, secondLocalSong]))
    const {
      enqueueSongs,
      initializePlayer,
      playSong,
      playerState,
      seekPlayback,
    } = await import('@/features/player/controller')
    await initializePlayer()
    enqueueSongs([{ ...localSong, duration: 180 }, secondLocalSong])
    await playSong({ ...localSong, duration: 180 })

    nativePlayer.play.mockClear()
    await seekPlayback(90)

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: unknown) => void
    // 模拟 seek 到未缓冲区间后插件伪报 ENDED/complete
    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 0,
      duration: 180,
    })

    await Promise.resolve()
    await Promise.resolve()

    expect(nativePlayer.play).not.toHaveBeenCalled()
    expect(playerState.currentSong?.id).toBe('song-local')
    expect(playerState.status).toBe('playing')
    expect(playerState.position).toBe(90)
  })

  test('接近结尾的 finished 仍自动下一曲', async () => {
    vi.resetModules()
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([{ ...localSong, duration: 200 }, secondLocalSong]))
    const { enqueueSongs, initializePlayer, playSong, playerState } = await import('@/features/player/controller')
    await initializePlayer()
    enqueueSongs([{ ...localSong, duration: 200 }, secondLocalSong])
    await playSong({ ...localSong, duration: 200 })

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: unknown) => void
    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 199,
      duration: 200,
    })

    await vi.waitFor(() => {
      expect(playerState.currentSong?.id).toBe('song-local-2')
    }, { timeout: 1000 })
  })

  test('歌词点击 seek 后 finished 不切歌（与进度条共用保护）', async () => {
    vi.resetModules()
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([{ ...localSong, duration: 180 }, secondLocalSong]))
    const {
      enqueueSongs,
      initializePlayer,
      playSong,
      playerState,
      seekPlayback,
    } = await import('@/features/player/controller')
    await initializePlayer()
    enqueueSongs([{ ...localSong, duration: 180 }, secondLocalSong])
    await playSong({ ...localSong, duration: 180 })

    // 歌词点击与进度条都走 seekPlayback
    nativePlayer.play.mockClear()
    await seekPlayback(5)

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: unknown) => void
    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 5,
      duration: 180,
    })

    await Promise.resolve()
    await Promise.resolve()

    expect(nativePlayer.play).not.toHaveBeenCalled()
    expect(playerState.currentSong?.id).toBe('song-local')
    expect(playerState.status).toBe('playing')
  })

  test('duration 为 0 时 finished 不自动 advance', async () => {
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

    nativePlayer.play.mockClear()
    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: unknown) => void
    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 0,
      duration: 0,
    })

    await Promise.resolve()
    await Promise.resolve()

    expect(nativePlayer.play).not.toHaveBeenCalled()
    expect(playerState.currentSong?.id).toBe('song-local')
  })

  test('seek 到接近结尾后保护窗内 finished 仍不切歌', async () => {
    vi.resetModules()
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([{ ...localSong, duration: 180 }, secondLocalSong]))
    const {
      enqueueSongs,
      initializePlayer,
      playSong,
      playerState,
      seekPlayback,
    } = await import('@/features/player/controller')
    await initializePlayer()
    enqueueSongs([{ ...localSong, duration: 180 }, secondLocalSong])
    await playSong({ ...localSong, duration: 180 })

    nativePlayer.play.mockClear()
    // 最后 1s 歌词点击：near-end 为真，但保护窗优先
    await seekPlayback(179.2)

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: unknown) => void
    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 179.2,
      duration: 180,
    })

    await Promise.resolve()
    await Promise.resolve()

    expect(nativePlayer.play).not.toHaveBeenCalled()
    expect(playerState.currentSong?.id).toBe('song-local')
  })

  test('paused 状态下 seek 后伪 finished 恢复为 paused 且不切歌', async () => {
    vi.resetModules()
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([{ ...localSong, duration: 180 }, secondLocalSong]))
    const {
      enqueueSongs,
      initializePlayer,
      pausePlayback,
      playSong,
      playerState,
      seekPlayback,
    } = await import('@/features/player/controller')
    await initializePlayer()
    enqueueSongs([{ ...localSong, duration: 180 }, secondLocalSong])
    await playSong({ ...localSong, duration: 180 })
    await pausePlayback()
    expect(playerState.status).toBe('paused')

    nativePlayer.play.mockClear()
    await seekPlayback(60)

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: unknown) => void
    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 0,
      duration: 180,
    })

    await Promise.resolve()
    await Promise.resolve()

    expect(nativePlayer.play).not.toHaveBeenCalled()
    expect(playerState.currentSong?.id).toBe('song-local')
    expect(playerState.status).toBe('paused')
    expect(playerState.position).toBe(60)
  })

  test('finished 事件 position 为 0 但 state 已接近结尾时仍自动下一曲', async () => {
    vi.resetModules()
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
    }
    localStorage.setItem('muses:songs', JSON.stringify([{ ...localSong, duration: 180 }, secondLocalSong]))
    const { enqueueSongs, initializePlayer, playSong, playerState } = await import('@/features/player/controller')
    await initializePlayer()
    enqueueSongs([{ ...localSong, duration: 180 }, secondLocalSong])
    await playSong({ ...localSong, duration: 180 })

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: unknown) => void
    // 先同步进度到接近结尾，再模拟 complete 把 position 回写成 0
    stateChangeCallback({
      status: 'playing',
      currentSongId: 'song-local',
      position: 179.4,
      duration: 180,
    })
    stateChangeCallback({
      status: 'finished',
      currentSongId: 'song-local',
      position: 0,
      duration: 180,
    })

    await vi.waitFor(() => {
      expect(playerState.currentSong?.id).toBe('song-local-2')
    }, { timeout: 1000 })
  })
})

describe('已缓冲进度与 seek 限制', () => {
  afterEach(async () => {
    await resetPlayer()
    localStorage.clear()
  })

  test('缓冲已知时 seek 越界拒绝且不调用原生 seek', async () => {
    vi.resetModules()
    const { initializePlayer, playSong, playerState, seekPlayback } = await import('@/features/player/controller')
    await initializePlayer()
    await playSong({ ...localSong, duration: 180 })

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: {
      status: string
      currentSongId?: string
      position?: number
      duration?: number
      bufferedPosition?: number
    }) => void
    stateChangeCallback({
      status: 'playing',
      currentSongId: 'song-local',
      position: 10,
      duration: 180,
      bufferedPosition: 40,
    })
    expect(playerState.bufferedPosition).toBe(40)

    nativePlayer.seek.mockClear()
    const ok = await seekPlayback(90)
    expect(ok).toBe(false)
    expect(nativePlayer.seek).not.toHaveBeenCalled()
    expect(playerState.position).toBe(10)
  })

  test('缓冲已知时 seek 在已缓冲区间内成功', async () => {
    vi.resetModules()
    const { initializePlayer, playSong, playerState, seekPlayback } = await import('@/features/player/controller')
    await initializePlayer()
    await playSong({ ...localSong, duration: 180 })

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: {
      status: string
      currentSongId?: string
      position?: number
      duration?: number
      bufferedPosition?: number
    }) => void
    stateChangeCallback({
      status: 'playing',
      currentSongId: 'song-local',
      position: 5,
      duration: 180,
      bufferedPosition: 60,
    })

    nativePlayer.seek.mockClear()
    const ok = await seekPlayback(42.5)
    expect(ok).toBe(true)
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 42.5 })
    expect(playerState.position).toBe(42.5)
  })

  test('歌词点击未缓冲时间码不发起 seek', async () => {
    vi.resetModules()
    const { initializePlayer, playSong, seekPlayback } = await import('@/features/player/controller')
    await initializePlayer()
    await playSong({
      ...localSong,
      duration: 180,
      lyrics: '[00:01.00]第一句\n[00:50.00]未缓冲句',
      lyricsSource: 'embedded',
    })

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: {
      status: string
      currentSongId?: string
      duration?: number
      bufferedPosition?: number
    }) => void
    stateChangeCallback({
      status: 'playing',
      currentSongId: 'song-local',
      duration: 180,
      bufferedPosition: 30,
    })

    nativePlayer.seek.mockClear()
    // 模拟歌词点击 50s（> 30s 缓冲）
    const ok = await seekPlayback(50)
    expect(ok).toBe(false)
    expect(nativePlayer.seek).not.toHaveBeenCalled()
  })

  test('本地就绪 full buffer 时可全长 seek', async () => {
    vi.resetModules()
    const { initializePlayer, playSong, playerState, seekPlayback } = await import('@/features/player/controller')
    await initializePlayer()
    await playSong({ ...localSong, duration: 180 })

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: {
      status: string
      currentSongId?: string
      duration?: number
      bufferedPosition?: number
    }) => void
    stateChangeCallback({
      status: 'playing',
      currentSongId: 'song-local',
      duration: 180,
      bufferedPosition: 180,
    })
    expect(playerState.bufferedPosition).toBe(180)

    nativePlayer.seek.mockClear()
    const ok = await seekPlayback(175)
    expect(ok).toBe(true)
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 175 })
  })

  test('切歌后缓冲重置且不继承上一首', async () => {
    vi.resetModules()
    const secondLocalSong: SongItem = {
      ...localSong,
      id: 'song-local-2',
      path: 'album/second.mp3',
      uri: 'content://music/second',
      title: '第二首本地歌曲',
      duration: 200,
    }
    localStorage.setItem('muses:songs', JSON.stringify([{ ...localSong, duration: 180 }, secondLocalSong]))
    const { initializePlayer, playSong, playerState } = await import('@/features/player/controller')
    await initializePlayer()
    await playSong({ ...localSong, duration: 180 })

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: {
      status: string
      currentSongId?: string
      duration?: number
      bufferedPosition?: number
    }) => void
    stateChangeCallback({
      status: 'playing',
      currentSongId: 'song-local',
      duration: 180,
      bufferedPosition: 90,
    })
    expect(playerState.bufferedPosition).toBe(90)

    await playSong(secondLocalSong)
    // 新歌起播时缓冲先清零，禁止串曲
    expect(playerState.bufferedPosition).toBeNull()
    expect(playerState.currentSong?.id).toBe('song-local-2')
  })

  test('stop 后缓冲重置为 null', async () => {
    vi.resetModules()
    const { initializePlayer, playSong, playerState, stopPlayback } = await import('@/features/player/controller')
    await initializePlayer()
    await playSong({ ...localSong, duration: 180 })

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: {
      status: string
      currentSongId?: string
      duration?: number
      bufferedPosition?: number
    }) => void
    stateChangeCallback({
      status: 'playing',
      currentSongId: 'song-local',
      duration: 180,
      bufferedPosition: 50,
    })
    expect(playerState.bufferedPosition).toBe(50)

    await stopPlayback()
    expect(playerState.bufferedPosition).toBeNull()
  })

  test('缓冲增长会单调合并到 playerState', async () => {
    vi.resetModules()
    const { initializePlayer, playSong, playerState } = await import('@/features/player/controller')
    await initializePlayer()
    await playSong({ ...localSong, duration: 180 })

    const stateChangeCallback = nativePlayer.addListener.mock.calls[0][1] as (state: {
      status: string
      currentSongId?: string
      duration?: number
      bufferedPosition?: number
    }) => void

    stateChangeCallback({
      status: 'playing',
      currentSongId: 'song-local',
      duration: 180,
      bufferedPosition: 20,
    })
    expect(playerState.bufferedPosition).toBe(20)

    stateChangeCallback({
      status: 'playing',
      currentSongId: 'song-local',
      duration: 180,
      bufferedPosition: 55,
    })
    expect(playerState.bufferedPosition).toBe(55)

    // 回退上报不得把缓冲拉低
    stateChangeCallback({
      status: 'playing',
      currentSongId: 'song-local',
      duration: 180,
      bufferedPosition: 40,
    })
    expect(playerState.bufferedPosition).toBe(55)
  })

  test('缓冲未知时 seek 退化为 duration clamp', async () => {
    vi.resetModules()
    const { initializePlayer, playSong, playerState, seekPlayback } = await import('@/features/player/controller')
    await initializePlayer()
    await playSong({ ...localSong, duration: 180 })
    // 不注入 bufferedPosition → null 降级
    expect(playerState.bufferedPosition).toBeNull()

    nativePlayer.seek.mockClear()
    const ok = await seekPlayback(200)
    expect(ok).toBe(true)
    expect(nativePlayer.seek).toHaveBeenCalledWith({ position: 180 })
    expect(playerState.position).toBe(180)
  })

  test('PlayerPage 在有缓冲数据时设置 --buffered 样式', async () => {
    // 不 resetModules：PlayerPage 静态 import 的 controller 需与本测试同一实例
    const { playSong, playerState } = await import('@/features/player/controller')
    await playSong({ ...localSong, duration: 100 })

    // 直接通过 stateChange 路径不可用时，用多次 play 后的 reactive 状态驱动 UI：
    // 模拟缓冲写入——通过再次 play + 手动注入（若 listener 已注册）
    const listenerCall = nativePlayer.addListener.mock.calls.find((call) => call[0] === 'stateChange')
    if (listenerCall) {
      const stateChangeCallback = listenerCall[1] as (state: {
        status: string
        currentSongId?: string
        position?: number
        duration?: number
        bufferedPosition?: number
      }) => void
      stateChangeCallback({
        status: 'playing',
        currentSongId: 'song-local',
        position: 10,
        duration: 100,
        bufferedPosition: 40,
      })
    } else {
      // initializePlayer 可能已在先前用例执行且 mock 被 clear；至少保证 currentSong 存在
      expect(playerState.currentSong?.id).toBe('song-local')
    }

    // 若缓冲未能写入（无 listener），用 controller 再走一遍 initialize 保证链路
    if (playerState.bufferedPosition == null) {
      vi.resetModules()
      const mod = await import('@/features/player/controller')
      await mod.initializePlayer()
      await mod.playSong({ ...localSong, duration: 100 })
      const cb = nativePlayer.addListener.mock.calls[0][1] as (state: {
        status: string
        currentSongId?: string
        position?: number
        duration?: number
        bufferedPosition?: number
      }) => void
      cb({
        status: 'playing',
        currentSongId: 'song-local',
        position: 10,
        duration: 100,
        bufferedPosition: 40,
      })
      // 动态 re-import PlayerPage 以绑定同一 controller 模块
      const { default: FreshPlayerPage } = await import('@/views/PlayerPage.vue')
      const wrapper = mount(FreshPlayerPage, {
        global: {
          stubs: {
            IonPage: { template: '<main><slot /></main>' },
            IonContent: { template: '<section><slot /></section>' },
            IonButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
            IonIcon: true,
          },
        },
      })
      await wrapper.vm.$nextTick()
      const slider = wrapper.get('.progress-slider')
      const style = slider.attributes('style') || ''
      expect(style.includes('--buffered') || wrapper.html().includes('--buffered')).toBe(true)
      return
    }

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

    await wrapper.vm.$nextTick()
    const slider = wrapper.get('.progress-slider')
    const style = slider.attributes('style') || ''
    expect(style.includes('--buffered') || wrapper.html().includes('--buffered')).toBe(true)
  })
})
