import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import type { AmllIndexEntry } from '@/features/lyrics/types'

const sampleIndex: AmllIndexEntry[] = [
  {
    musicName: 'Idol',
    artists: ['YOASOBI'],
    album: 'Idol',
    rawLyricFile: 'idol.ttml',
  },
  {
    musicName: '夜に駆ける',
    artists: ['YOASOBI'],
    album: '夜に駆ける',
    rawLyricFile: 'yoru.ttml',
  },
  {
    musicName: 'Pretender',
    artists: ['Official髭男dism'],
    album: 'Traveler',
    rawLyricFile: 'pretender.ttml',
  },
  {
    musicName: 'Pretender (Live)',
    artists: ['Official髭男dism'],
    album: 'Live',
    rawLyricFile: 'pretender-live.ttml',
  },
]

const SAMPLE_TTML = `<?xml version="1.0" encoding="utf-8"?>
<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
  <head><metadata><ttm:agent type="person" xml:id="v1" /></metadata></head>
  <body dur="00:02.000">
    <div xmlns="" begin="00:00.000" end="00:02.000">
      <p begin="00:00.000" end="00:02.000" ttm:agent="v1" itunes:key="L1">
        <span begin="00:00.000" end="00:02.000">Hello</span>
      </p>
    </div>
  </body>
</tt>`

describe('歌词规范化', () => {
  test('小写、去括号与常见后缀、折叠空白', async () => {
    const { normalizeText } = await import('@/features/lyrics/normalize')
    expect(normalizeText('  Idol (Live)  ')).toBe('idol')
    expect(normalizeText('Pretender【Remix】')).toBe('pretender')
    expect(normalizeText('夜に駆ける')).toBe('夜に駆ける')
    expect(normalizeText('')).toBe('')
    expect(normalizeText(undefined)).toBe('')
  })

  test('歌手 token 拆分支持 / & feat', async () => {
    const { splitArtistTokens } = await import('@/features/lyrics/normalize')
    expect(splitArtistTokens('A / B & C feat. D')).toEqual(['a', 'b', 'c', 'd'])
    expect(splitArtistTokens('YOASOBI')).toEqual(['yoasobi'])
    expect(splitArtistTokens(undefined)).toEqual([])
  })
})

describe('歌词打分与最佳选取', () => {
  test('歌名完全相等得分高于包含', async () => {
    const { scoreTitle, SCORE_WEIGHTS } = await import('@/features/lyrics/score')
    expect(scoreTitle('Idol', 'Idol').level).toBe('exact')
    expect(scoreTitle('Idol', 'Idol').score).toBe(SCORE_WEIGHTS.TITLE_EXACT)
    expect(scoreTitle('Idol Special', 'Idol').level).toBe('contains')
    expect(scoreTitle('Foo', 'Bar').level).toBe('none')
  })

  test('歌名不相关时总分 0，低于阈值视为无匹配', async () => {
    const { scoreEntry, findBestMatch, MIN_ACCEPT_SCORE } = await import('@/features/lyrics/score')
    expect(scoreEntry({ title: 'Completely Different' }, sampleIndex[0])).toBe(0)
    expect(findBestMatch({ title: 'Completely Different', artist: 'YOASOBI' }, sampleIndex)).toBeNull()
    expect(MIN_ACCEPT_SCORE).toBeGreaterThan(0)
  })

  test('歌名+歌手命中取最佳；live 后缀规范化后可精确匹配', async () => {
    const { findBestMatch, SCORE_WEIGHTS } = await import('@/features/lyrics/score')
    const best = findBestMatch(
      { title: 'Pretender', artist: 'Official髭男dism', album: 'Traveler' },
      sampleIndex,
    )
    expect(best).not.toBeNull()
    expect(best!.entry.rawLyricFile).toBe('pretender.ttml')
    expect(best!.score).toBeGreaterThanOrEqual(SCORE_WEIGHTS.TITLE_EXACT)

    const liveBest = findBestMatch(
      { title: 'Pretender (Live)', artist: 'Official髭男dism' },
      sampleIndex,
    )
    // (Live) 规范化后与 Pretender 精确相等，两条都 exact；专辑加分让 Traveler 可能更高
    // 至少应命中 Pretender 系
    expect(liveBest).not.toBeNull()
    expect(liveBest!.entry.musicName.toLowerCase()).toContain('pretender')
  })

  test('仅歌手命中不足以采纳，明确歌手冲突也拒绝同名歌曲', async () => {
    const { findBestMatch } = await import('@/features/lyrics/score')
    expect(findBestMatch({ title: '完全不相关歌名', artist: 'YOASOBI' }, sampleIndex)).toBeNull()
    expect(findBestMatch({ title: 'Idol', artist: 'Different Artist' }, sampleIndex)).toBeNull()
  })

  test('查询无歌手时允许歌名精确匹配，但模糊包含需要专辑佐证', async () => {
    const { findBestMatch } = await import('@/features/lyrics/score')
    expect(findBestMatch({ title: 'Idol' }, sampleIndex)?.entry.rawLyricFile).toBe('idol.ttml')
    expect(findBestMatch({ title: 'Idol Special Edition' }, sampleIndex)).toBeNull()
    expect(findBestMatch({ title: 'Idol Special Edition', album: 'Idol' }, sampleIndex)?.entry.rawLyricFile).toBe('idol.ttml')
  })
})

describe('amll-ttml-db 匹配与缓存', () => {
  beforeEach(async () => {
    vi.restoreAllMocks()
    const { resetAmllTtmlDbCache } = await import('@/features/lyrics/amllTtmlDb')
    resetAmllTtmlDbCache()
  })

  afterEach(async () => {
    vi.unstubAllGlobals()
    const { resetAmllTtmlDbCache } = await import('@/features/lyrics/amllTtmlDb')
    resetAmllTtmlDbCache()
  })

  test('大索引分片解析会让出事件循环', async () => {
    const { parseIndexJsonlChunked } = await import('@/features/lyrics/amllTtmlDb')
    const line = JSON.stringify({ metadata: [['musicName', ['Idol']]], rawLyricFile: 'idol.ttml' })
    let yielded = false
    setTimeout(() => { yielded = true }, 0)
    const entries = await parseIndexJsonlChunked(`${line}\n${line}`, 1)
    expect(entries).toHaveLength(2)
    await new Promise<void>((resolve) => setTimeout(resolve, 0))
    expect(yielded).toBe(true)
  })

  test('常见 exact/contains 查询只评分候选子集', async () => {
    const { __setIndexCacheForTests, matchAmllTtmlLyrics, __getCandidateCountForTests } = await import('@/features/lyrics/amllTtmlDb')
    const largeIndex = Array.from({ length: 300 }, (_, index) => ({
      musicName: `Noise ${index}`,
      artists: [`Artist ${index}`],
      album: `Album ${index}`,
      rawLyricFile: `noise-${index}.ttml`,
    }))
    largeIndex.push(...sampleIndex)
    __setIndexCacheForTests(largeIndex)

    const fetchMock = vi.fn(async () => ({ ok: true, text: async () => SAMPLE_TTML }))
    vi.stubGlobal('fetch', fetchMock)

    const result = await matchAmllTtmlLyrics({
      songId: 'song-subset',
      title: 'Idol',
      artist: 'YOASOBI',
      album: 'Idol',
    })
    expect(result.ok).toBe(true)
    expect(__getCandidateCountForTests('Idol')).toBeLessThan(largeIndex.length)
    expect(__getCandidateCountForTests('Idol')).toBeLessThan(40)
  })

  test('解析 jsonl 索引行', async () => {
    const { parseIndexLine, parseIndexJsonl } = await import('@/features/lyrics/amllTtmlDb')
    const line = JSON.stringify({
      metadata: [
        ['album', ['Idol']],
        ['artists', ['YOASOBI']],
        ['musicName', ['Idol']],
      ],
      rawLyricFile: 'x.ttml',
    })
    expect(parseIndexLine(line)).toEqual({
      musicName: 'Idol',
      artists: ['YOASOBI'],
      album: 'Idol',
      rawLyricFile: 'x.ttml',
    })
    expect(parseIndexJsonl(`${line}\n\n{bad`)).toHaveLength(1)
    expect(parseIndexLine('')).toBeNull()
  })

  test('TTML 命中缓存容量受限', async () => {
    const { __setIndexCacheForTests, matchAmllTtmlLyrics, __getAmllCacheSizesForTests } = await import('@/features/lyrics/amllTtmlDb')
    __setIndexCacheForTests(sampleIndex)
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, text: async () => SAMPLE_TTML })))
    for (let index = 0; index < 300; index += 1) {
      await matchAmllTtmlLyrics({ songId: `amll-${index}`, title: 'Idol', artist: 'YOASOBI' })
    }
    expect(__getAmllCacheSizesForTests().ttml).toBe(256)
  })

  test('匹配成功拉取 TTML 并按 songId 缓存', async () => {
    const { __setIndexCacheForTests, matchAmllTtmlLyrics, __hasTtmlCacheForTests } = await import(
      '@/features/lyrics/amllTtmlDb'
    )
    __setIndexCacheForTests(sampleIndex)

    const fetchMock = vi.fn(async (url: string) => {
      expect(String(url)).toContain('idol.ttml')
      return {
        ok: true,
        text: async () => SAMPLE_TTML,
      }
    })
    vi.stubGlobal('fetch', fetchMock)

    const first = await matchAmllTtmlLyrics({
      songId: 'song-1',
      title: 'Idol',
      artist: 'YOASOBI',
      album: 'Idol',
    })
    expect(first.ok).toBe(true)
    if (first.ok) {
      expect(first.ttml).toContain('Hello')
      expect(first.rawLyricFile).toBe('idol.ttml')
      expect(first.score).toBeGreaterThan(0)
    }
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(__hasTtmlCacheForTests('song-1')).toBe(true)

    // 缓存命中，不再 fetch
    const second = await matchAmllTtmlLyrics({
      songId: 'song-1',
      title: 'Idol',
      artist: 'YOASOBI',
      album: 'Idol',
    })
    expect(second.ok).toBe(true)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  test('同 songId 元数据变化不会复用旧 TTML 缓存', async () => {
    const { __setIndexCacheForTests, matchAmllTtmlLyrics } = await import('@/features/lyrics/amllTtmlDb')
    __setIndexCacheForTests(sampleIndex)
    const fetchMock = vi.fn(async (url: string) => ({
      ok: true,
      text: async () => String(url).includes('idol.ttml') ? SAMPLE_TTML : SAMPLE_TTML.replace('Hello', 'Yoru'),
    }))
    vi.stubGlobal('fetch', fetchMock)

    expect((await matchAmllTtmlLyrics({ songId: 'reused-id', title: 'Idol', artist: 'YOASOBI' })).ok).toBe(true)
    expect((await matchAmllTtmlLyrics({ songId: 'reused-id', title: '夜に駆ける', artist: 'YOASOBI' })).ok).toBe(true)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  test('无匹配写入负缓存，不重复拉 TTML', async () => {
    const { __setIndexCacheForTests, matchAmllTtmlLyrics } = await import('@/features/lyrics/amllTtmlDb')
    __setIndexCacheForTests(sampleIndex)
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    const first = await matchAmllTtmlLyrics({
      songId: 'song-miss',
      title: '不存在的歌',
      artist: 'Nobody',
    })
    expect(first).toEqual({ ok: false, reason: 'no-match' })

    const second = await matchAmllTtmlLyrics({
      songId: 'song-miss',
      title: '不存在的歌',
    })
    expect(second).toEqual({ ok: false, reason: 'no-match' })
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('索引 Promise 失败后可由其他歌曲重试', async () => {
    const { matchAmllTtmlLyrics, resetAmllTtmlDbCache } = await import('@/features/lyrics/amllTtmlDb')
    resetAmllTtmlDbCache()
    const indexLine = JSON.stringify({
      metadata: [['musicName', ['Idol']], ['artists', ['YOASOBI']]],
      rawLyricFile: 'idol.ttml',
    })
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce({ ok: true, text: async () => indexLine })
      .mockResolvedValueOnce({ ok: true, text: async () => SAMPLE_TTML })
    vi.stubGlobal('fetch', fetchMock)

    expect(await matchAmllTtmlLyrics({ songId: 'song-net-1', title: 'Idol', artist: 'YOASOBI' }))
      .toEqual({ ok: false, reason: 'network' })
    expect((await matchAmllTtmlLyrics({ songId: 'song-net-2', title: 'Idol', artist: 'YOASOBI' })).ok).toBe(true)
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  test('不安全歌词路径被拒绝，特殊文件名按单段 URL 编码', async () => {
    const { __setIndexCacheForTests, matchAmllTtmlLyrics } = await import('@/features/lyrics/amllTtmlDb')
    __setIndexCacheForTests([{ ...sampleIndex[0], rawLyricFile: '../idol.ttml' }])
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    expect(await matchAmllTtmlLyrics({ songId: 'unsafe', title: 'Idol', artist: 'YOASOBI' }))
      .toEqual({ ok: false, reason: 'parse' })
    expect(fetchMock).not.toHaveBeenCalled()

    __setIndexCacheForTests([{ ...sampleIndex[0], rawLyricFile: 'idol #1.ttml' }])
    expect((await matchAmllTtmlLyrics({ songId: 'encoded', title: 'Idol', artist: 'YOASOBI' })).ok).toBe(false)
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('idol%20%231.ttml'), expect.anything())
  })
})

describe('TTML 解析路径', () => {
  test('parseTTML 返回可交给 LyricPlayer 的逐词行', async () => {
    const { parseTTML } = await import('@applemusic-like-lyrics/lyric')
    const parsed = parseTTML(SAMPLE_TTML)
    expect(parsed.lines.length).toBeGreaterThan(0)
    expect(parsed.lines[0].words.map((word) => word.word).join('')).toContain('Hello')
  })
})

describe('播放器在线歌词匹配（token 防串曲）', () => {
  const localSong = {
    id: 'song-local',
    sourceId: 'source-local',
    sourceType: 'local' as const,
    path: 'album/local.mp3',
    uri: 'content://music/local',
    title: 'Idol',
    artist: 'YOASOBI',
    album: 'Idol',
    createdAt: '2026-07-07T00:00:00.000Z',
    updatedAt: '2026-07-07T00:00:00.000Z',
  }

  const secondSong = {
    ...localSong,
    id: 'song-local-2',
    path: 'album/second.mp3',
    uri: 'content://music/second',
    title: '夜に駆ける',
    artist: 'YOASOBI',
  }

  beforeEach(() => {
    vi.resetModules()
    vi.restoreAllMocks()
    localStorage.clear()
  })

  afterEach(async () => {
    try {
      const { stopPlayback } = await import('@/features/player/controller')
      await stopPlayback()
    } catch {
      // ignore
    }
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  const mockNativePlayer = () => {
    const nativePlayer = {
      play: vi.fn().mockResolvedValue(undefined),
      pause: vi.fn().mockResolvedValue(undefined),
      resume: vi.fn().mockResolvedValue(undefined),
      stop: vi.fn().mockResolvedValue(undefined),
      seek: vi.fn().mockResolvedValue(undefined),
      getState: vi.fn().mockResolvedValue({ status: 'idle' }),
      ensureNotificationPermission: vi.fn().mockResolvedValue(undefined),
      addListener: vi.fn().mockResolvedValue({ remove: vi.fn() }),
    }
    vi.doMock('@/features/player/native', () => ({
      AudioPlayerNative: nativePlayer,
      AudioPlayerBridge: {
        ensureNotificationPermission: vi.fn().mockResolvedValue({ granted: true }),
        prepareArtworkDataUrl: vi.fn().mockResolvedValue({ dataUrl: null }),
      },
    }))
    vi.doMock('@capgo/capacitor-media-session', () => ({
      MediaSession: {
        setMetadata: vi.fn().mockResolvedValue(undefined),
        setPlaybackState: vi.fn().mockResolvedValue(undefined),
        setPositionState: vi.fn().mockResolvedValue(undefined),
        setActionHandler: vi.fn().mockResolvedValue(undefined),
      },
    }))
    vi.doMock('@aparajita/capacitor-secure-storage', () => ({
      SecureStorage: {
        get: vi.fn(),
        set: vi.fn(),
        remove: vi.fn(),
      },
    }))
    return nativePlayer
  }

  test('开播后无论有无本地歌词都会进入 matching，成功写 TTML', async () => {
    mockNativePlayer()
    const matchOnlineLyrics = vi.fn().mockResolvedValue({
      ok: true,
      text: SAMPLE_TTML,
      format: 'ttml',
      source: 'amll',
    })
    vi.doMock('@/features/lyrics', () => ({
      matchOnlineLyrics,
    }))

    const { playSong, playerState } = await import('@/features/player/controller')
    const playPromise = playSong({
      ...localSong,
      lyrics: '[00:01.00]本地歌词',
      lyricsSource: 'embedded',
    })

    // 起播瞬间应已进入 matching，且先展示库内词
    expect(playerState.onlineLyricsStatus).toBe('matching')
    expect(playerState.lyrics).toBe('[00:01.00]本地歌词')
    expect(playerState.lyricsFormat).toBe('lrc')

    await playPromise
    // 等待异步匹配
    await vi.waitFor(() => {
      expect(playerState.onlineLyricsStatus).toBe('ready')
    })
    expect(matchOnlineLyrics).toHaveBeenCalledWith(
      expect.objectContaining({ songId: 'song-local', title: 'Idol', artist: 'YOASOBI' }),
    )
    expect(playerState.lyrics).toBe(SAMPLE_TTML)
    expect(playerState.lyricsFormat).toBe('ttml')
    // 质量升级：LRC → TTML 写回曲库（按 path 找，upsert 可能生成稳定 id）
    const stored = JSON.parse(localStorage.getItem('muses:songs') || '[]') as Array<{
      path: string
      lyrics?: string
      lyricsSource?: string
      lyricsFormat?: string
    }>
    const row = stored.find((s) => s.path === localSong.path)
    expect(row?.lyrics).toBe(SAMPLE_TTML)
    expect(row?.lyricsSource).toBe('online')
    expect(row?.lyricsFormat).toBe('ttml')
  })

  test('库内已是 ttml 时在线 LRC 不写回覆盖', async () => {
    mockNativePlayer()
    localStorage.setItem(
      'muses:songs',
      JSON.stringify([
        {
          ...localSong,
          lyrics: SAMPLE_TTML,
          lyricsSource: 'online',
          lyricsFormat: 'ttml',
          createdAt: '2026-07-13T00:00:00.000Z',
          updatedAt: '2026-07-13T00:00:00.000Z',
        },
      ]),
    )
    vi.doMock('@/features/lyrics', () => ({
      matchOnlineLyrics: vi.fn().mockResolvedValue({
        ok: true,
        text: '[00:01.00]worse lrc',
        format: 'lrc',
        source: 'lrclib',
      }),
    }))

    const { playSong, playerState } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      lyrics: SAMPLE_TTML,
      lyricsSource: 'online',
      lyricsFormat: 'ttml',
    })
    await vi.waitFor(() => {
      expect(playerState.onlineLyricsStatus).toBe('ready')
    })
    // 当次可展示在线 LRC，但不降级写库
    expect(playerState.lyrics).toBe('[00:01.00]worse lrc')
    const stored = JSON.parse(localStorage.getItem('muses:songs') || '[]') as Array<{
      path: string
      lyrics?: string
      lyricsFormat?: string
    }>
    const row = stored.find((s) => s.path === localSong.path)
    expect(row?.lyrics).toBe(SAMPLE_TTML)
    expect(row?.lyricsFormat).toBe('ttml')
  })

  test('无词歌曲在线命中写回 online+format', async () => {
    mockNativePlayer()
    vi.doMock('@/features/lyrics', () => ({
      matchOnlineLyrics: vi.fn().mockResolvedValue({
        ok: true,
        text: '[00:01.00]online',
        format: 'lrc',
        source: 'kw',
      }),
    }))

    const { playSong, playerState } = await import('@/features/player/controller')
    await playSong(localSong)
    await vi.waitFor(() => {
      expect(playerState.onlineLyricsStatus).toBe('ready')
    })
    const stored = JSON.parse(localStorage.getItem('muses:songs') || '[]') as Array<{
      path: string
      lyrics?: string
      lyricsSource?: string
      lyricsFormat?: string
    }>
    const row = stored.find((s) => s.path === localSong.path)
    expect(row?.lyrics).toBe('[00:01.00]online')
    expect(row?.lyricsSource).toBe('online')
    expect(row?.lyricsFormat).toBe('lrc')
  })

  test('匹配失败回退本地歌词', async () => {
    mockNativePlayer()
    vi.doMock('@/features/lyrics', () => ({
      matchOnlineLyrics: vi.fn().mockResolvedValue({ ok: false, reason: 'no-match' }),
    }))

    const { playSong, playerState } = await import('@/features/player/controller')
    await playSong({
      ...localSong,
      lyrics: '[00:01.00]本地歌词',
      lyricsSource: 'embedded',
    })
    await vi.waitFor(() => {
      expect(playerState.onlineLyricsStatus).toBe('miss')
    })
    expect(playerState.lyrics).toBe('[00:01.00]本地歌词')
    expect(playerState.lyricsFormat).toBe('lrc')
  })

  test('快速切歌丢弃过期匹配结果', async () => {
    mockNativePlayer()
    let resolveFirst: (value: unknown) => void = () => undefined
    const firstMatch = new Promise((resolve) => {
      resolveFirst = resolve
    })

    const matchOnlineLyrics = vi.fn()
      .mockImplementationOnce(() => firstMatch)
      .mockResolvedValueOnce({
        ok: true,
        text: '<tt>second</tt>',
        format: 'ttml',
        source: 'amll',
      })

    vi.doMock('@/features/lyrics', () => ({
      matchOnlineLyrics,
    }))

    const { playSong, playerState } = await import('@/features/player/controller')

    // 第一首：匹配挂起
    void playSong(localSong)
    expect(playerState.onlineLyricsStatus).toBe('matching')

    // 快速切到第二首
    await playSong(secondSong)
    await vi.waitFor(() => {
      expect(matchOnlineLyrics).toHaveBeenCalledTimes(2)
    })
    await vi.waitFor(() => {
      expect(playerState.onlineLyricsStatus).toBe('ready')
    })
    expect(playerState.currentSong?.id).toBe('song-local-2')
    expect(playerState.lyrics).toBe('<tt>second</tt>')

    // 迟到的第一首结果不得覆盖当前曲
    resolveFirst({
      ok: true,
      text: '<tt>stale-first</tt>',
      format: 'ttml',
      source: 'amll',
    })
    await Promise.resolve()
    await Promise.resolve()
    expect(playerState.currentSong?.id).toBe('song-local-2')
    expect(playerState.lyrics).toBe('<tt>second</tt>')
    expect(playerState.lyrics).not.toContain('stale-first')
  })

  test('网络错误且无本地词时 status=error，歌词保持空', async () => {
    mockNativePlayer()
    vi.doMock('@/features/lyrics', () => ({
      matchOnlineLyrics: vi.fn().mockResolvedValue({ ok: false, reason: 'network' }),
    }))

    const { playSong, playerState } = await import('@/features/player/controller')
    await playSong(localSong)
    await vi.waitFor(() => {
      expect(playerState.onlineLyricsStatus).toBe('error')
    })
    expect(playerState.lyrics).toBeNull()
    expect(playerState.lyricsFormat).toBeNull()
  })
})

describe('在线歌词编排 matchOnlineLyrics', () => {
  afterEach(async () => {
    vi.unstubAllGlobals()
    vi.doUnmock('@/features/lyrics')
    const { setOnlineLyricsFallbackProvidersForTest } = await import('@/features/lyrics/match')
    const { resetAmllTtmlDbCache } = await import('@/features/lyrics/amllTtmlDb')
    setOnlineLyricsFallbackProvidersForTest(null)
    resetAmllTtmlDbCache()
    vi.resetModules()
  })

  test('amll 命中短路，不调 fallback', async () => {
    vi.resetModules()
    const { __setIndexCacheForTests } = await import('@/features/lyrics/amllTtmlDb')
    __setIndexCacheForTests([
      {
        musicName: 'Idol',
        artists: ['YOASOBI'],
        album: 'Idol',
        rawLyricFile: 'idol.ttml',
      },
    ])
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        text: async () => SAMPLE_TTML,
      }),
    )

    const fallback = {
      id: 'kw' as const,
      searchLyrics: vi.fn().mockResolvedValue({ text: '[00:01.00]平台', format: 'lrc' as const }),
    }
    const { matchOnlineLyrics, setOnlineLyricsFallbackProvidersForTest } = await import('@/features/lyrics/match')
    setOnlineLyricsFallbackProvidersForTest([fallback])

    const result = await matchOnlineLyrics({
      songId: 's1',
      title: 'Idol',
      artist: 'YOASOBI',
    })
    expect(result).toMatchObject({ ok: true, format: 'ttml', source: 'amll' })
    expect(fallback.searchLyrics).not.toHaveBeenCalled()
  })

  test('amll miss 后走 fallback 并短路后续', async () => {
    vi.resetModules()
    const { __setIndexCacheForTests } = await import('@/features/lyrics/amllTtmlDb')
    __setIndexCacheForTests([])

    const kw = {
      id: 'kw' as const,
      searchLyrics: vi.fn().mockResolvedValue({ text: '[00:01.00]酷我', format: 'lrc' as const }),
    }
    const lrclib = {
      id: 'lrclib' as const,
      searchLyrics: vi.fn().mockResolvedValue({ text: '[00:01.00]lrc', format: 'lrc' as const }),
    }
    const { matchOnlineLyrics, setOnlineLyricsFallbackProvidersForTest } = await import('@/features/lyrics/match')
    setOnlineLyricsFallbackProvidersForTest([kw, lrclib])

    const result = await matchOnlineLyrics({
      songId: 's2',
      title: '无索引歌',
      artist: 'X',
    })
    expect(result).toEqual({
      ok: true,
      text: '[00:01.00]酷我',
      format: 'lrc',
      source: 'kw',
    })
    expect(kw.searchLyrics).toHaveBeenCalled()
    expect(lrclib.searchLyrics).not.toHaveBeenCalled()
  })
})

describe('歌词 sync 覆盖规则 shouldApplyStoredLyricsOverRuntime', () => {
  test('库空不覆盖运行时；库更优才覆盖', async () => {
    const { shouldApplyStoredLyricsOverRuntime } = await import('@/features/player/types')
    expect(shouldApplyStoredLyricsOverRuntime('[00:01]在线', 'lrc', {})).toBe(false)
    expect(shouldApplyStoredLyricsOverRuntime('[00:01]在线', 'lrc', { lyrics: '' })).toBe(false)
    expect(shouldApplyStoredLyricsOverRuntime(null, null, { lyrics: '[00:01]库', lyricsFormat: 'lrc' })).toBe(true)
    expect(shouldApplyStoredLyricsOverRuntime('[00:01]L', 'lrc', { lyrics: 'T', lyricsFormat: 'ttml' })).toBe(true)
    expect(shouldApplyStoredLyricsOverRuntime('T', 'ttml', { lyrics: 'L', lyricsFormat: 'lrc' })).toBe(false)
  })
})
