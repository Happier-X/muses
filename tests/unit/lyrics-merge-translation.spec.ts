import { describe, expect, test } from 'vitest'
import type { LyricLine } from '@applemusic-like-lyrics/core'
import {
  attachTimedLyricsTranslation,
  mergeDuplicateTimestampTranslations,
  parseTimedLrcMap,
  prepareLyricLinesForDisplay,
} from '@/features/lyrics/mergeTranslation'
import { applyLyricTranslationVisibility } from '@/features/lyrics/display'

const makeLine = (startTime: number, word: string, translatedLyric = ''): LyricLine => ({
  words: [{ startTime, endTime: startTime + 1000, word }],
  translatedLyric,
  romanLyric: '',
  startTime,
  endTime: startTime + 1000,
  isBG: false,
  isDuet: false,
})

describe('歌词译文合并', () => {
  test('parseTimedLrcMap 解析多时间戳行', () => {
    const map = parseTimedLrcMap('[00:01.00][00:02.00]你好\n[00:03.50]世界')
    expect(map.get(1000)).toBe('你好')
    expect(map.get(2000)).toBe('你好')
    expect(map.get(3500)).toBe('世界')
  })

  test('同时间戳双主行合并为 translatedLyric', () => {
    const lines = [
      makeLine(1000, 'Hello'),
      makeLine(1000, '你好'),
      makeLine(3000, 'World'),
      makeLine(3000, '世界'),
    ]
    const merged = mergeDuplicateTimestampTranslations(lines)
    expect(merged).toHaveLength(2)
    expect(merged[0]).toMatchObject({
      translatedLyric: '你好',
    })
    expect(merged[0].words[0].word).toBe('Hello')
    expect(merged[1]).toMatchObject({
      translatedLyric: '世界',
    })
  })

  test('attachTimedLyricsTranslation 挂独立 tlyric', () => {
    const lines = [makeLine(1000, 'Hello'), makeLine(3000, 'World')]
    const attached = attachTimedLyricsTranslation(lines, '[00:01.00]你好\n[00:03.00]世界')
    expect(attached[0].translatedLyric).toBe('你好')
    expect(attached[1].translatedLyric).toBe('世界')
  })

  test('prepare + 显示开关可隐藏译文', () => {
    const prepared = prepareLyricLinesForDisplay(
      [makeLine(1000, 'Hello'), makeLine(1000, '你好')],
      null,
    )
    expect(prepared).toHaveLength(1)
    expect(prepared[0].translatedLyric).toBe('你好')

    const hidden = applyLyricTranslationVisibility(prepared, false)
    expect(hidden[0].translatedLyric).toBe('')
    expect(hidden[0].words[0].word).toBe('Hello')
  })
})
