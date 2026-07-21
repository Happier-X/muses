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
  test('parseTimedLrcMap 解析多时间戳、厘秒和毫秒格式', () => {
    const map = parseTimedLrcMap('[00:01.0][00:02.00]你好\n[00:03:500]世界')
    expect(map.get(1000)).toBe('你好')
    expect(map.get(2000)).toBe('你好')
    expect(map.get(3500)).toBe('世界')
    expect(parseTimedLrcMap('[00:04,250]').get(4250)).toBeUndefined()
    expect(parseTimedLrcMap('[00:04,250]逗号毫秒').get(4250)).toBe('逗号毫秒')
  })

  test('译文允许小幅偏差但不会跨过容差错配', () => {
    const lines = [makeLine(1000, 'Hello'), makeLine(3000, 'World')]
    const attached = attachTimedLyricsTranslation(lines, '[00:01.07]你好\n[00:03.079]世界')
    expect(attached[0].translatedLyric).toBe('你好')
    expect(attached[1].translatedLyric).toBe('世界')

    const outside = attachTimedLyricsTranslation([makeLine(1000, 'Hello')], '[00:01.081]太远')
    expect(outside[0].translatedLyric).toBe('')
  })

  test('同时间戳双主行合并为 translatedLyric（原文在前）', () => {
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

  test('中文在前、英文在后时仍以英文为主行', () => {
    const merged = mergeDuplicateTimestampTranslations([
      makeLine(1000, '你好'),
      makeLine(1000, 'Hello'),
      makeLine(3000, '世界'),
      makeLine(3000, 'World'),
    ])
    expect(merged).toHaveLength(2)
    expect(merged[0].words[0].word).toBe('Hello')
    expect(merged[0].translatedLyric).toBe('你好')
    expect(merged[1].words[0].word).toBe('World')
    expect(merged[1].translatedLyric).toBe('世界')
  })

  test('合并后 endTime 取配对行较大值', () => {
    const first = makeLine(1000, 'Hello')
    first.endTime = 1500
    first.words[0].endTime = 1500
    const second = makeLine(1000, '你好')
    second.endTime = 2800
    second.words[0].endTime = 2800
    const merged = mergeDuplicateTimestampTranslations([first, second])
    expect(merged).toHaveLength(1)
    expect(merged[0].endTime).toBe(2800)
    expect(merged[0].words[0].word).toBe('Hello')
    expect(merged[0].translatedLyric).toBe('你好')
  })

  test('日文汉字假名混排与中文译文可正确合并', () => {
    const merged = mergeDuplicateTimestampTranslations([
      makeLine(1000, '君の名は'),
      makeLine(1000, '你的名字'),
    ])
    expect(merged).toHaveLength(1)
    expect(merged[0].words[0].word).toBe('君の名は')
    expect(merged[0].translatedLyric).toBe('你的名字')
  })

  test('中文在前、日文假名在后时仍以日文为主行', () => {
    const merged = mergeDuplicateTimestampTranslations([
      makeLine(1000, '你的名字'),
      makeLine(1000, '君の名は'),
    ])
    expect(merged).toHaveLength(1)
    expect(merged[0].words[0].word).toBe('君の名は')
    expect(merged[0].translatedLyric).toBe('你的名字')
  })

  test('已有 tlyric 的行不再被双行合并颠倒', () => {
    const lines = [
      makeLine(1000, '你好', '已有译文'),
      makeLine(1000, 'Hello'),
    ]
    const merged = mergeDuplicateTimestampTranslations(lines)
    expect(merged).toHaveLength(2)
    expect(merged[0].words[0].word).toBe('你好')
    expect(merged[0].translatedLyric).toBe('已有译文')
    expect(merged[1].words[0].word).toBe('Hello')
  })

  test('同语言同时间行不应误合并为翻译', () => {
    const lines = [makeLine(1000, 'First'), makeLine(1000, 'Second')]
    expect(mergeDuplicateTimestampTranslations(lines)).toHaveLength(2)
  })

  test('超出容差的译文不会错挂到其他原歌词行', () => {
    const lines = [makeLine(1000, 'Hello')]
    const attached = attachTimedLyricsTranslation(lines, '[00:01.20]你好')
    expect(attached[0].translatedLyric).toBe('')
  })

  test('attachTimedLyricsTranslation 挂独立 tlyric', () => {
    const lines = [makeLine(1000, 'Hello'), makeLine(3000, 'World')]
    const attached = attachTimedLyricsTranslation(lines, '[00:01.00]你好\n[00:03.00]世界')
    expect(attached[0].translatedLyric).toBe('你好')
    expect(attached[1].translatedLyric).toBe('世界')
  })

  test('合并管线返回新行对象且不修改 AMLL 原始行', () => {
    const original = makeLine(1000, 'Hello')
    const prepared = prepareLyricLinesForDisplay([original], '[00:01.00]你好')
    expect(prepared[0]).not.toBe(original)
    expect(original.translatedLyric).toBe('')
    expect(prepared[0].translatedLyric).toBe('你好')
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

  test('中文在前时关翻译仍保留英文主行', () => {
    const prepared = prepareLyricLinesForDisplay(
      [makeLine(1000, '你好'), makeLine(1000, 'Hello')],
      null,
    )
    expect(prepared).toHaveLength(1)
    expect(prepared[0].words[0].word).toBe('Hello')
    expect(prepared[0].translatedLyric).toBe('你好')

    const hidden = applyLyricTranslationVisibility(prepared, false)
    expect(hidden[0].words[0].word).toBe('Hello')
    expect(hidden[0].translatedLyric).toBe('')
  })

  test('库已填 translatedLyric 时透传且不被 tlyric/双行合并覆盖', () => {
    const fromAml = makeLine(1000, 'Hello', '库内译文')
    const prepared = prepareLyricLinesForDisplay(
      [fromAml, makeLine(1000, '你好')],
      '[00:01.00]错误译文',
    )
    // 首行已有译 → 不与下一行合并，tlyric 也不覆盖
    expect(prepared).toHaveLength(2)
    expect(prepared[0].words[0].word).toBe('Hello')
    expect(prepared[0].translatedLyric).toBe('库内译文')
    expect(prepared[1].words[0].word).toBe('你好')
  })

  test('管线顺序：先 attach tlyric 再双行合并，已挂译的行不再合并', () => {
    // 主词仅英文一行；tlyric 提供中文；不应依赖双行合并
    const prepared = prepareLyricLinesForDisplay(
      [makeLine(1000, 'Hello')],
      '[00:01.00]你好',
    )
    expect(prepared).toHaveLength(1)
    expect(prepared[0].words[0].word).toBe('Hello')
    expect(prepared[0].translatedLyric).toBe('你好')
  })
})
