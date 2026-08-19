import { describe, expect, it } from 'vitest'
import {
  classifyMatch,
  DURATION_TOLERANCE_SEC,
  findBestMatch,
  scoreEntry,
} from '@/features/lyrics/score'
import type { AmllIndexEntry, AmllMatchQuery } from '@/features/lyrics/types'
import { classifyTextMetaConfidence, needsOnlineTextMeta } from '@/features/metadata/util'
import { shouldPersistOnlineLyrics } from '@/features/player/types'
import type { SongItem } from '@/features/library/types'

const makeEntry = (overrides: Partial<AmllIndexEntry> = {}): AmllIndexEntry => ({
  musicName: '晴天',
  artists: ['周杰伦'],
  album: '叶惠美',
  rawLyricFile: 'test.jsonl',
  ...overrides,
})

const makeQuery = (overrides: Partial<AmllMatchQuery> = {}): AmllMatchQuery => ({
  songId: 's1',
  title: '晴天',
  artist: '周杰伦',
  album: '叶惠美',
  ...overrides,
})

const makeSong = (overrides: Partial<Pick<SongItem, 'lyrics' | 'lyricsFormat' | 'userEditedFields'>> = {}): Pick<SongItem, 'lyrics' | 'lyricsFormat' | 'userEditedFields'> => ({
  lyrics: undefined,
  lyricsFormat: undefined,
  userEditedFields: undefined,
  ...overrides,
})

describe('score match quality 门槛分级 (child4)', () => {
  describe('classifyMatch', () => {
    it('title exact + artist 命中 → high', () => {
      const entry = makeEntry()
      const q = makeQuery({ title: '晴天', artist: '周杰伦' })
      expect(classifyMatch(q, entry, { level: 'exact', score: 100 }, 25)).toBe('high')
    })

    it('title exact + 无歌手信息（query 无 artist）→ high', () => {
      const entry = makeEntry()
      const q = makeQuery({ title: '晴天', artist: undefined })
      expect(classifyMatch(q, entry, { level: 'exact', score: 100 }, 0)).toBe('high')
    })

    it('title exact + query 有 artist 但不命中 → low', () => {
      const entry = makeEntry({ artists: ['林俊杰'] })
      const q = makeQuery({ title: '晴天', artist: '周杰伦' })
      expect(classifyMatch(q, entry, { level: 'exact', score: 100 }, 0)).toBe('low')
    })

    it('title contains + artist 命中 → high', () => {
      const entry = makeEntry()
      // 标题包含但不相等（normalize 后 '晴天[现场版]' 仍含 '晴天'；不被 strip live 后缀）
      const q = makeQuery({ title: '晴天[现场版]', artist: '周杰伦', duration: undefined })
      expect(classifyMatch(q, entry, { level: 'contains', score: 60 }, 25)).toBe('high')
    })

    it('title contains + 无歌手信息 → low', () => {
      const entry = makeEntry()
      const q = makeQuery({ title: '晴天', artist: undefined })
      expect(classifyMatch(q, entry, { level: 'contains', score: 60 }, 0)).toBe('low')
    })

    it('title contains + artist 不命中 → low（同名歌/翻唱误配被拒）', () => {
      const entry = makeEntry({ artists: ['林俊杰'] })
      const q = makeQuery({ title: '晴天', artist: '周杰伦' })
      expect(classifyMatch(q, entry, { level: 'contains', score: 60 }, 0)).toBe('low')
    })

    it('时长偏差 ≤ 阈值 → 不降级', () => {
      const entry = makeEntry({ duration: 269 })
      const q = makeQuery({ title: '晴天', artist: '周杰伦', duration: 269 })
      expect(classifyMatch(q, entry, { level: 'exact', score: 100 }, 25)).toBe('high')
    })

    it('时长偏差超阈值 → low', () => {
      const entry = makeEntry({ duration: 200 })
      const q = makeQuery({ title: '晴天', artist: '周杰伦', duration: 200 + DURATION_TOLERANCE_SEC + 1 })
      expect(classifyMatch(q, entry, { level: 'exact', score: 100 }, 25)).toBe('low')
    })
  })

  describe('findBestMatch confidence 门槛', () => {
    const index: AmllIndexEntry[] = [
      // 低置信：title contains 但 artist 不命中（翻唱）
      makeEntry({ musicName: '晴天', artists: ['翻唱歌手'] }),
      // 高置信：title exact + artist 命中
      makeEntry({ musicName: '晴天', artists: ['周杰伦'] }),
    ]

    it('默认 minConfidence=high：仅返回高置信匹配', () => {
      const best = findBestMatch(makeQuery({ title: '晴天', artist: '周杰伦' }), index)
      expect(best).not.toBeNull()
      expect(best!.confidence).toBe('high')
      expect(best!.entry.artists).toEqual(['周杰伦'])
    })

    it('minConfidence=low：放宽到低置信（候选场景）', () => {
      // contains 级 + 查询无 artist 信息 → 低置信但分数足够（title contains=60）
      const lowOnly: AmllIndexEntry[] = [makeEntry({ musicName: '晴天[现场版]' })]
      const q = makeQuery({ title: '晴天', artist: undefined })
      expect(findBestMatch(q, lowOnly)).toBeNull()
      expect(findBestMatch(q, lowOnly, { minConfidence: 'low' })).not.toBeNull()
    })
  })

  describe('scoreEntry 向后兼容', () => {
    it('title none → 0', () => {
      expect(scoreEntry(makeQuery({ title: '完全不同的歌' }), makeEntry({ musicName: '晴天' }))).toBe(0)
    })
    it('exact + artist + album → 满分', () => {
      const score = scoreEntry(makeQuery(), makeEntry())
      expect(score).toBe(100 + 25 + 15) // title exact + artist hit + album exact
    })
  })
})

describe('classifyTextMetaConfidence 文本命中置信度 (child4)', () => {
  const hit = (o: { title?: string; artist?: string; album?: string }) => o
  it('title exact + artist 命中 → high', () => {
    expect(classifyTextMetaConfidence(hit({ title: '晴天', artist: '周杰伦' }), { title: '晴天', artist: '周杰伦' })).toBe('high')
  })
  it('title exact + 无查询 artist → high', () => {
    expect(classifyTextMetaConfidence(hit({ title: '晴天', artist: '路人' }), { title: '晴天' })).toBe('high')
  })
  it('title exact + 有 artist 不命中 → low', () => {
    expect(classifyTextMetaConfidence(hit({ title: '晴天', artist: '林俊杰' }), { title: '晴天', artist: '周杰伦' })).toBe('low')
  })
  it('title contains + artist 命中 → high', () => {
    // 标题不同后缀但子串包含（避免 normalize strip live 后变 exact）
    expect(classifyTextMetaConfidence(hit({ title: '晴天[现场版]', artist: '周杰伦' }), { title: '晴天', artist: '周杰伦' })).toBe('high')
  })
  it('title contains + 有 query.artist 但不命中 → low', () => {
    expect(classifyTextMetaConfidence(hit({ title: '晴天[现场版]', artist: '林俊杰' }), { title: '晴天', artist: '周杰伦' })).toBe('low')
  })
  it('title exact + 无 query.artist → high（文件名占位场景可采纳）', () => {
    expect(classifyTextMetaConfidence(hit({ title: '晴天', artist: '周杰伦' }), { title: '晴天' })).toBe('high')
  })
})

describe('shouldPersistOnlineLyrics 低置信受限 (child4)', () => {
  it('手改歌词永久不写', () => {
    expect(shouldPersistOnlineLyrics(makeSong({ userEditedFields: ['lyrics'] }), 'ttml', 'text', 'high')).toBe(false)
  })
  it('空歌词不写', () => {
    expect(shouldPersistOnlineLyrics(makeSong(), 'ttml', '  ', 'high')).toBe(false)
  })
  it('高置信 ttml 覆盖空库 → true', () => {
    expect(shouldPersistOnlineLyrics(makeSong(), 'ttml', 'text', 'high')).toBe(true)
  })
  it('低置信 + 空库 → true（仅补缺）', () => {
    expect(shouldPersistOnlineLyrics(makeSong(), 'ttml', 'text', 'low')).toBe(true)
  })
  it('低置信 + 现有 lrc → false（不覆盖现有词）', () => {
    expect(shouldPersistOnlineLyrics(makeSong({ lyrics: '现有词', lyricsFormat: 'lrc' }), 'ttml', '新词', 'low')).toBe(false)
  })
  it('缺省 confidence（平台 LRC 向后兼容）+ 现有 lrc 升级 ttml → true', () => {
    expect(shouldPersistOnlineLyrics(makeSong({ lyrics: '现有词', lyricsFormat: 'lrc' }), 'ttml', '新词')).toBe(true)
  })
})

describe('needsOnlineTextMeta cloud 来源再补约束 (child4 R4-2)', () => {
  const base = { songId: 's1', title: 'track', path: '/a/track.mp3' }
  it('artist 空且未保护 → 需要（补空安全）', () => {
    expect(needsOnlineTextMeta({ ...base, artist: undefined, album: 'x' })).toBe(true)
  })
  it('weak title（title=文件名）+ 无 cloud 来源 → 需要补弱标签', () => {
    expect(needsOnlineTextMeta({ ...base, title: 'track', artist: 'a', album: 'x' })).toBe(true)
  })
  it('weak title + 来源 cloud + 缺 duration → 不再补（避免低质量重写）', () => {
    expect(needsOnlineTextMeta({ ...base, title: 'track', artist: 'a', album: 'x', metaSources: { title: 'cloud' }, duration: undefined })).toBe(false)
  })
  it('weak title + 来源 cloud + 齐 duration+artist → 重新补', () => {
    expect(needsOnlineTextMeta({ ...base, title: 'track', artist: 'a', album: 'x', metaSources: { title: 'cloud' }, duration: 200 })).toBe(true)
  })
  it('weak title + 来源 cloud + 有 duration 但缺 artist → 仍补（needArtist 独立触发，补空安全）', () => {
    expect(needsOnlineTextMeta({ ...base, title: 'track', artist: undefined, album: 'x', metaSources: { title: 'cloud' }, duration: 200 })).toBe(true)
  })
  it('来源 embedded（内置权威）不受 cloud 约束阻挡', () => {
    expect(needsOnlineTextMeta({ ...base, title: 'track', artist: 'a', album: 'x', metaSources: { title: 'embedded' }, duration: undefined })).toBe(true)
  })
})
