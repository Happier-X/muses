import type { LyricLine } from '@applemusic-like-lyrics/core'

const TIME_TAG_RE = /\[(\d{1,3}):(\d{2})(?:[.,:](\d{1,3}))?\]/g
const TRANSLATION_MATCH_TOLERANCE_MS = 80

/** 解析带时间轴的 LRC 为 startMs → 文本（同时间后者覆盖） */
export const parseTimedLrcMap = (text: string): Map<number, string> => {
  const map = new Map<number, string>()
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim()
    if (!line) {
      continue
    }
    TIME_TAG_RE.lastIndex = 0
    const stamps: number[] = []
    let match: RegExpExecArray | null
    let lastIndex = 0
    while ((match = TIME_TAG_RE.exec(line)) !== null) {
      const min = Number(match[1])
      const sec = Number(match[2])
      if (sec >= 60) {
        continue
      }
      const fraction = match[3] ?? ''
      // LRC 的小数部分通常是厘秒/百分秒；平台变体也会使用毫秒。
      // 统一按小数秒换算，而不是把三位数固定当作某一种格式。
      const fractionMs = fraction
        ? Math.round(Number(`0.${fraction}`) * 1000)
        : 0
      stamps.push(min * 60_000 + sec * 1000 + fractionMs)
      lastIndex = TIME_TAG_RE.lastIndex
    }
    if (stamps.length === 0) {
      continue
    }
    const content = line.slice(lastIndex).trim()
    if (!content) {
      continue
    }
    for (const stamp of stamps) {
      map.set(stamp, content)
    }
  }
  return map
}

const nearestTranslation = (map: Map<number, string>, startMs: number, toleranceMs = 80): string | undefined => {
  if (map.has(startMs)) {
    return map.get(startMs)
  }
  let best: string | undefined
  let bestDelta = toleranceMs + 1
  for (const [stamp, text] of map) {
    const delta = Math.abs(stamp - startMs)
    if (delta < bestDelta) {
      bestDelta = delta
      best = text
    }
  }
  return best
}

/** 把 timed LRC 译文挂到主歌词行的 translatedLyric */
export const attachTimedLyricsTranslation = (
  lines: LyricLine[],
  translationLrc: string | null | undefined,
): LyricLine[] => {
  const raw = translationLrc?.trim()
  if (!raw || lines.length === 0) {
    return lines.map((line) => ({ ...line }))
  }
  const map = parseTimedLrcMap(raw)
  if (map.size === 0) {
    return lines.map((line) => ({ ...line }))
  }
  return lines.map((line) => {
    if (line.translatedLyric?.trim()) {
      return { ...line }
    }
    const hit = nearestTranslation(map, line.startTime, TRANSLATION_MATCH_TOLERANCE_MS)
    if (!hit) {
      return { ...line }
    }
    return {
      ...line,
      translatedLyric: hit,
    }
  })
}

/** 判断两行是否明显使用了不同文字体系，避免吞掉同时间的两句独立歌词。 */
const scriptSignature = (text: string): string => {
  // 日文常混用汉字与假名；必须先识别假名，否则会与中文译文都误判为 Han。
  if (/\p{Script=Hiragana}|\p{Script=Katakana}/u.test(text)) return 'kana'
  if (/\p{Script=Han}/u.test(text)) return 'han'
  if (/\p{Script=Hangul}/u.test(text)) return 'hangul'
  if (/\p{Script=Cyrillic}/u.test(text)) return 'cyrillic'
  if (/\p{Script=Arabic}/u.test(text)) return 'arabic'
  if (/[A-Za-z]/.test(text)) return 'latin'
  return ''
}

const isLikelyTranslationPair = (source: string, candidate: string): boolean => {
  const sourceScript = scriptSignature(source)
  const candidateScript = scriptSignature(candidate)
  return !!sourceScript && !!candidateScript && sourceScript !== candidateScript
}

/**
 * 合并「同时间戳双语主行」：原文 + 紧随的译文行 → translatedLyric。
 * 只有文字体系明确不同才合并，避免吞掉同时间的两句独立歌词。
 */
export const mergeDuplicateTimestampTranslations = (lines: LyricLine[]): LyricLine[] => {
  if (lines.length < 2) {
    return lines.map((line) => ({ ...line }))
  }

  const result: LyricLine[] = []
  for (let index = 0; index < lines.length; index += 1) {
    const current = lines[index]
    const next = lines[index + 1]
    if (
      next
      && !current.isBG
      && !next.isBG
      && Math.abs(current.startTime - next.startTime) <= 50
      && !current.translatedLyric?.trim()
      && next.words.map((w) => w.word).join('').trim()
      && current.words.map((w) => w.word).join('').trim()
      && current.words.map((w) => w.word).join('') !== next.words.map((w) => w.word).join('')
      && isLikelyTranslationPair(
        current.words.map((w) => w.word).join('').trim(),
        next.words.map((w) => w.word).join('').trim(),
      )
    ) {
      result.push({
        ...current,
        translatedLyric: next.words.map((w) => w.word).join(''),
      })
      index += 1
      continue
    }
    result.push({ ...current })
  }
  return result
}

/** 显示管线：先挂 timed 译文，再合并双行主词 */
export const prepareLyricLinesForDisplay = (
  lines: LyricLine[],
  translationLrc?: string | null,
): LyricLine[] => {
  return mergeDuplicateTimestampTranslations(attachTimedLyricsTranslation(lines, translationLrc))
}
