import { describe, expect, test } from 'vitest'
import type { LyricLine } from '@applemusic-like-lyrics/core'
import { applyLyricTranslationVisibility } from '@/features/lyrics/display'

const line: LyricLine = {
  words: [{ startTime: 0, endTime: 1000, word: 'Hello' }],
  translatedLyric: '你好',
  romanLyric: 'ni hao',
  startTime: 0,
  endTime: 1000,
  isBG: false,
  isDuet: false,
}

describe('歌词翻译显示开关', () => {
  test('显示时保持原引用，隐藏时清空翻译与音译', () => {
    const visible = applyLyricTranslationVisibility([line], true)
    expect(visible[0].translatedLyric).toBe('你好')
    expect(visible[0].romanLyric).toBe('ni hao')

    const hidden = applyLyricTranslationVisibility([line], false)
    expect(hidden).not.toBe(visible)
    expect(hidden[0].words[0].word).toBe('Hello')
    expect(hidden[0].translatedLyric).toBe('')
    expect(hidden[0].romanLyric).toBe('')
    expect(line.translatedLyric).toBe('你好')
  })
})
