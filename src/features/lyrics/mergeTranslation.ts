import type { LyricLine } from '@applemusic-like-lyrics/core'

const TIME_TAG_RE = /\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]/g

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
      const frac = match[3] ?? '0'
      const ms = frac.length === 1 ? Number(frac) * 100 : frac.length === 2 ? Number(frac) * 10 : Number(frac.padEnd(3, '0').slice(0, 3))
      stamps.push(min * 60_000 + sec * 1000 + ms)
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
    return lines
  }
  const map = parseTimedLrcMap(raw)
  if (map.size === 0) {
    return lines
  }
  return lines.map((line) => {
    if (line.translatedLyric?.trim()) {
      return line
    }
    const hit = nearestTranslation(map, line.startTime)
    if (!hit) {
      return line
    }
    return {
      ...line,
      translatedLyric: hit,
    }
  })
}

/**
 * 合并「同时间戳双语主行」：原文 + 紧随的译文行 → translatedLyric。
 * 常见于网易 tlyric 拼进同一 LRC、或平台返回双行。
 */
export const mergeDuplicateTimestampTranslations = (lines: LyricLine[]): LyricLine[] => {
  if (lines.length < 2) {
    return lines
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
    ) {
      result.push({
        ...current,
        translatedLyric: next.words.map((w) => w.word).join(''),
      })
      index += 1
      continue
    }
    result.push(current)
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
