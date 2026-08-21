import { describe, expect, it } from 'vitest'
import { isSuspiciousSongForScrape, pickSuspiciousSongs } from '@/features/scrape/suspicious'
import type { SongItem } from '@/features/library/types'

const makeSong = (overrides: Partial<SongItem> = {}): SongItem => ({
  id: 's1',
  title: '晴天',
  artist: '周杰伦',
  album: '叶惠美',
  duration: 269,
  path: '/a/晴天.mp3',
  sourceType: 'local',
  uri: 'file:///a/晴天.mp3',
  coverUri: 'file:///cover.jpg',
  lyrics: '[00:00]晴天\n[00:01]故事的小黄花',
  lyricsFormat: 'lrc',
  lyricsSource: 'embedded',
  ...overrides,
})

describe('isSuspiciousSongForScrape (child2 R2-3)', () => {
  it('完整曲库（内置来源）→ false', () => {
    expect(isSuspiciousSongForScrape(makeSong())).toBe(false)
  })

  it('title=文件名占位（弱标签）+ 缺 cover → true', () => {
    const song = makeSong({ title: 'track01', path: '/a/track01.mp3', coverUri: undefined })
    expect(isSuspiciousSongForScrape(song)).toBe(true)
  })

  it('title=文件名占位 + 其他字段都完整 → false（库中常态，避免误报）', () => {
    const song = makeSong({ title: 'track01', path: '/a/track01.mp3' })
    expect(isSuspiciousSongForScrape(song)).toBe(false)
  })

  it('artist 缺失 → true', () => {
    expect(isSuspiciousSongForScrape(makeSong({ artist: undefined }))).toBe(true)
    expect(isSuspiciousSongForScrape(makeSong({ artist: '' }))).toBe(true)
    expect(isSuspiciousSongForScrape(makeSong({ artist: '   ' }))).toBe(true)
  })

  it('album 缺失 → true', () => {
    expect(isSuspiciousSongForScrape(makeSong({ album: undefined }))).toBe(true)
  })

  it('cover 缺失 → true', () => {
    expect(isSuspiciousSongForScrape(makeSong({ coverUri: undefined }))).toBe(true)
    expect(isSuspiciousSongForScrape(makeSong({ coverUri: '' }))).toBe(true)
  })

  it('lyrics 缺失 → true', () => {
    expect(isSuspiciousSongForScrape(makeSong({ lyrics: undefined }))).toBe(true)
    expect(isSuspiciousSongForScrape(makeSong({ lyrics: '' }))).toBe(true)
    expect(isSuspiciousSongForScrape(makeSong({ lyrics: '   ' }))).toBe(true)
  })

  it('lyricsSource=online（在线歌词低可信）→ true', () => {
    expect(isSuspiciousSongForScrape(makeSong({ lyricsSource: 'online' }))).toBe(true)
  })

  it('metaSources.title=cloud（低质量历史）→ true', () => {
    expect(isSuspiciousSongForScrape(makeSong({ metaSources: { title: 'cloud' } }))).toBe(true)
  })

  it('metaSources.artist=cloud → true', () => {
    expect(isSuspiciousSongForScrape(makeSong({ metaSources: { artist: 'cloud' } }))).toBe(true)
  })

  it('metaSources.album=cloud → true', () => {
    expect(isSuspiciousSongForScrape(makeSong({ metaSources: { album: 'cloud' } }))).toBe(true)
  })

  it('lyricsSource=scrape（刮削写回未入文件）→ true', () => {
    expect(isSuspiciousSongForScrape(makeSong({ lyricsSource: 'scrape' }))).toBe(true)
  })

  it('metaSources.title=scrape → true', () => {
    expect(isSuspiciousSongForScrape(makeSong({ metaSources: { title: 'scrape' } }))).toBe(true)
  })

  it('includeCloudSources=false 时同样跳过 scrape 来源规则', () => {
    const song = makeSong({ lyricsSource: 'scrape', metaSources: { title: 'scrape' } })
    expect(isSuspiciousSongForScrape(song)).toBe(true)
    expect(isSuspiciousSongForScrape(song, { includeCloudSources: false })).toBe(false)
  })

  it('includeCloudSources=false 时跳过来源 cloud 规则', () => {
    const song = makeSong({ metaSources: { title: 'cloud' } })
    expect(isSuspiciousSongForScrape(song)).toBe(true)
    expect(isSuspiciousSongForScrape(song, { includeCloudSources: false })).toBe(false)
  })

  it('完整曲库 + includeCloudSources=false 仍 → false', () => {
    expect(isSuspiciousSongForScrape(makeSong(), { includeCloudSources: false })).toBe(false)
  })
})

describe('pickSuspiciousSongs 批量筛选', () => {
  it('混合列表：仅返回命中可疑规则的歌曲', () => {
    const list: SongItem[] = [
      makeSong({ id: 'ok', title: '晴天', path: '/a/晴天.mp3' }),
      makeSong({ id: 'no-artist', artist: undefined }),
      makeSong({ id: 'no-cover', coverUri: undefined }),
      makeSong({ id: 'cloud-title', metaSources: { title: 'cloud' } }),
    ]
    const picked = pickSuspiciousSongs(list)
    expect(picked.map((song) => song.id)).toEqual(['no-artist', 'no-cover', 'cloud-title'])
  })

  it('空列表 → 空数组', () => {
    expect(pickSuspiciousSongs([])).toEqual([])
  })

  it('不修改原数组', () => {
    const list: SongItem[] = [makeSong({ id: 'no-artist', artist: undefined })]
    const snapshot = [...list]
    pickSuspiciousSongs(list)
    expect(list).toEqual(snapshot)
  })
})
