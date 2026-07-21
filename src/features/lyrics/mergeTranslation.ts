/**
 * 歌词翻译适配层（非格式解析器）。
 *
 * 职责边界（对齐 AMLL）：
 * - 主词格式解析一律用 `@applemusic-like-lyrics/lyric` 的
 *   `parseLrc` / `parseYrc` / `parseQrc` / `parseTTML` 等（见 PlayerPage）。
 * - 本文件只做 AMLL **不会**完成的业务适配：
 *   1) 独立 tlyric（timed LRC 译文串）挂到主行 `translatedLyric`
 *   2) plain 双语 LRC 同时间戳双主行 → 主行+副行（非 Han 优先主行）
 *   3) 供翻译开关使用的行数据准备（与 display.ts 配合）
 * - 已有非空 `translatedLyric`（库解析结果或 tlyric 已挂）不得二次合并/覆盖主译语义。
 * - `parseTimedLrcMap` 仅服务 tlyric 挂载：平台译文时间戳变体（逗号毫秒等）
 *   比 `parseLrc` 更宽，**不**替代主词 LRC 解析。
 */
import type { LyricLine } from '@applemusic-like-lyrics/core'

const TIME_TAG_RE = /\[(\d{1,3}):(\d{2})(?:[.,:](\d{1,3}))?\]/g
const TRANSLATION_MATCH_TOLERANCE_MS = 80

/**
 * 将独立 translation LRC 文本解析为 startMs → 文本（同时间后者覆盖）。
 * 仅用于 attach tlyric，不是主歌词解析入口。
 */
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

/**
 * 把独立 timed LRC 译文（如网易 tlyric）挂到主行 `translatedLyric`。
 * 已有非空 `translatedLyric` 的行（TTML/库内嵌译等）不覆盖。
 */
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
    // 尊重 AMLL 解析或上游已填的翻译，避免二次猜测。
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

const linePlainText = (line: LyricLine): string =>
  line.words.map((w) => w.word).join('').trim()

/**
 * 在可合并的双语对中挑选主行与译文。
 * 常见「中文在前、原文在后」时，Han 应作译文、非 Han 作主行，不能只靠文件顺序。
 */
const pickMainAndTranslation = (
  first: LyricLine,
  second: LyricLine,
): { main: LyricLine; translation: string } => {
  const firstText = linePlainText(first)
  const secondText = linePlainText(second)
  const firstScript = scriptSignature(firstText)
  const secondScript = scriptSignature(secondText)

  // 一对中一行是 Han、另一行是非 Han → 非 Han 为主行，Han 为译文。
  if (firstScript === 'han' && secondScript && secondScript !== 'han') {
    return { main: second, translation: firstText }
  }
  if (secondScript === 'han' && firstScript && firstScript !== 'han') {
    return { main: first, translation: secondText }
  }

  // 其它可区分脚本对：默认前一行主、后一行译。
  return { main: first, translation: secondText }
}

/**
 * 合并「同时间戳双语主行」：原文主行 + 译文 → translatedLyric。
 * 只有文字体系明确不同才合并，避免吞掉同时间的两句独立歌词。
 * 主译按脚本语义选择（非 Han 优先于 Han），不依赖 LRC 行序。
 */
export const mergeDuplicateTimestampTranslations = (lines: LyricLine[]): LyricLine[] => {
  if (lines.length < 2) {
    return lines.map((line) => ({ ...line }))
  }

  const result: LyricLine[] = []
  for (let index = 0; index < lines.length; index += 1) {
    const current = lines[index]
    const next = lines[index + 1]
    const currentText = linePlainText(current)
    const nextText = next ? linePlainText(next) : ''
    if (
      next
      && !current.isBG
      && !next.isBG
      && Math.abs(current.startTime - next.startTime) <= 50
      // 任一侧已有独立 tlyric 挂载则不再双行合并，避免颠倒主译。
      && !current.translatedLyric?.trim()
      && !next.translatedLyric?.trim()
      && nextText
      && currentText
      && currentText !== nextText
      && isLikelyTranslationPair(currentText, nextText)
    ) {
      const { main, translation } = pickMainAndTranslation(current, next)
      result.push({
        ...main,
        startTime: Math.min(current.startTime, next.startTime),
        // 取配对行较长结束时间，避免合并后活跃窗口过短导致高亮只闪一下。
        endTime: Math.max(current.endTime, next.endTime),
        translatedLyric: translation,
      })
      index += 1
      continue
    }
    result.push({ ...current })
  }
  return result
}

/**
 * 显示管线（主词须已由 AMLL parse 完成）：
 * attachTimed(tlyric?) → mergeDuplicate(仅双主行且无译) → 再由 UI 做 visibility。
 */
export const prepareLyricLinesForDisplay = (
  lines: LyricLine[],
  translationLrc?: string | null,
): LyricLine[] => {
  return mergeDuplicateTimestampTranslations(attachTimedLyricsTranslation(lines, translationLrc))
}
