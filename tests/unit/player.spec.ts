import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import type { SongItem } from '@/features/library/types'
import MiniPlayer from '@/components/MiniPlayer.vue'
import PlayerPage from '@/views/PlayerPage.vue'
import App from '@/App.vue'

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

vi.mock('@/features/player/native', () => ({
  AudioPlayerNative: nativePlayer,
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
  const { stopPlayback } = await import('@/features/player/controller')
  await stopPlayback()
  vi.clearAllMocks()
  vi.useRealTimers()
  prefetchWebDavAudioFileMock.mockReset()
  prefetchWebDavAudioFileMock.mockResolvedValue({ cached: false, started: true })
  audioPlayerBridge.getCachedWebDavAudioFile.mockReset()
  audioPlayerBridge.getCachedWebDavAudioFile.mockResolvedValue({ uri: null })
  audioPlayerBridge.prefetchWebDavAudioFile.mockReset()
  audioPlayerBridge.prefetchWebDavAudioFile.mockResolvedValue({ cached: false, started: true })
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
          IonButton: { template: '<button :aria-label="$attrs[\'aria-label\']" @click="$emit(\'click\', $event)"><slot /></button>' },
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
          IonIcon: true,
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
    expect(wrapper.find('[data-test="amll-lyrics"]').exists()).toBe(true)

    const panels = wrapper.get('.panels')
    await panels.trigger('touchstart', { changedTouches: [{ clientX: 320 }] })
    await panels.trigger('touchend', { changedTouches: [{ clientX: 120 }] })

    expect(panels.attributes('style')).toContain('translateX(-50%)')
    expect(wrapper.get('[data-test="amll-lyrics"]').text()).toContain('第一句歌词')
    expect(wrapper.get('[data-test="amll-lyrics"]').attributes('data-align-position')).toBe('0.38')
    expect(wrapper.get('.lyric-header .lyric-title').text()).toBe('本地歌曲')
    expect(wrapper.get('.lyric-header .lyric-artist').text()).toBe('本地歌手')
    expect(wrapper.find('.lyric-panel .progress-slider').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('暂无歌词')
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
    await infoPanel.trigger('touchend', { changedTouches: [{ clientX: 200, clientY: 320 }] })

    expect(playerOverlayVisible.value).toBe(false)
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
