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
/** 相邻两行视为「同时间戳」的容差（毫秒）。 */
const DUPLICATE_TIMESTAMP_TOLERANCE_MS = 50
/** 「译文时间戳打在下一句」交替结构的最低配对数，低于此视为零星现象。 */
const SHIFTED_PAIR_MIN_COUNT = 2
/** 交替结构配对数占配对消耗总行数（配对×2 + 落单）的比例阈值。 */
const SHIFTED_PAIR_MIN_RATIO = 0.6
/** 「译文时间戳打在下一句」配对中，原文与译文行允许的最大时间间隔（毫秒）。 */
const SHIFTED_PAIR_MAX_GAP_MS = 15_000
/** tlyric 序列感知回退允许的最大单行时间偏差（毫秒），覆盖 yrc 的系统性偏移。 */
const SEQUENCE_ALIGN_MAX_DELTA_MS = 2000
/** tlyric 序列感知回退的最低匹配率（匹配数 / 待挂译行数）。 */
const SEQUENCE_ALIGN_MIN_RATIO = 0.6
/** 交替结构判定：译文时间戳须贴近下一窗口原文行时间戳（毫秒），
 *  泄露该属性的窗口视为错位（垫词行打断交替结构），不参与配对。 */
const SHIFTED_CROSS_WINDOW_TOLERANCE_MS = 1000

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

const nearestTranslation = (
  map: Map<number, string>,
  startMs: number,
  toleranceMs = 80,
): { stamp: number; text: string } | null => {
  if (map.has(startMs)) {
    return { stamp: startMs, text: map.get(startMs) as string }
  }
  let best: { stamp: number; text: string } | null = null
  let bestDelta = toleranceMs + 1
  for (const [stamp, text] of map) {
    const delta = Math.abs(stamp - startMs)
    if (delta < bestDelta) {
      bestDelta = delta
      best = { stamp, text }
    }
  }
  return best
}

/**
 * tlyric 序列感知回退：主行与 tlyric 存在**系统性时间偏移**（如网易 yrc
 * 行时间普遍偏差 300~700ms，超出 80ms 容差）时，按行序对 tlyric 时间戳
 * 排序后做双指针顺序对齐（不改变相对顺序）。
 * 仅当匹配率达标（整曲呈系统性偏移而非随机错配）才返回结果，避免破坏
 * 时间戳对齐良好的数据。**边界防误判**：若「首条待挂行无可用 stamp」且
 * 「末 stamp 无主行承接」（tlyric 自身整句错移一段的结构性错位）同时成立，
 * 视为错移而非偏移，放弃对齐，避免在本任务主 bug 的 tlyric 层复现同类错位。
 */
const alignTranslationSequence = (
  pendingLines: { index: number; startTime: number }[],
  map: Map<number, string>,
): Map<number, string> | null => {
  const stamps = [...map.keys()].sort((a, b) => a - b)
  if (pendingLines.length < 2 || stamps.length < 2) {
    return null
  }
  const matches: Array<{ line: number; stamp: number }> = []
  let lineCursor = 0
  let stampCursor = 0
  while (lineCursor < pendingLines.length && stampCursor < stamps.length) {
    const lineTime = pendingLines[lineCursor].startTime
    const stampTime = stamps[stampCursor]
    if (Math.abs(lineTime - stampTime) <= SEQUENCE_ALIGN_MAX_DELTA_MS) {
      matches.push({ line: lineCursor, stamp: stampCursor })
      lineCursor += 1
      stampCursor += 1
    } else if (stampTime < lineTime - SEQUENCE_ALIGN_MAX_DELTA_MS) {
      // tlyric 时间戳明显落后（对应主行已被略过），推进 tlyric 游标。
      stampCursor += 1
    } else {
      lineCursor += 1
    }
  }
  // 边界校验：结构性错移（整体平移一句）时首行无译且末 stamp 无主行承接。
  const firstLineMatched = matches.some((m) => m.line === 0)
  const lastStampMatched = matches.some((m) => m.stamp === stamps.length - 1)
  const structurallyMisShifted = !firstLineMatched && !lastStampMatched
  if (
    structurallyMisShifted
    || matches.length < 2
    || matches.length / pendingLines.length < SEQUENCE_ALIGN_MIN_RATIO
  ) {
    return null
  }
  const aligned = new Map<number, string>()
  for (const { line, stamp } of matches) {
    const text = map.get(stamps[stamp])
    if (text) {
      aligned.set(pendingLines[line].index, text)
    }
  }
  return aligned.size > 0 ? aligned : null
}

/**
 * 把独立 timed LRC 译文（如网易 tlyric）挂到主行 `translatedLyric`。
 * 已有非空 `translatedLyric` 的行（TTML/库内嵌译等）不覆盖。
 * 先按 80ms 容差逐行就近挂载；挂载率不足且呈系统性偏移时，
 * 退化为序列感知顺序对齐（覆盖 yrc 主词场景）。
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
  const consumedStamps = new Set<number>()
  const staged = lines.map((line) => {
    // 尊重 AMLL 解析或上游已填的翻译，避免二次猜测。
    if (line.translatedLyric?.trim()) {
      return { ...line }
    }
    const hit = nearestTranslation(map, line.startTime, TRANSLATION_MATCH_TOLERANCE_MS)
    if (!hit) {
      return { ...line }
    }
    consumedStamps.add(hit.stamp)
    return {
      ...line,
      translatedLyric: hit.text,
    }
  })
  // 第一遍 80ms 容差命中的时间戳要精确地从回退池中剔除，避免同一译文
  // 经序列回退重复挂到两条主行。
  for (const stamp of consumedStamps) {
    map.delete(stamp)
  }
  // 序列感知回退：仅对第一遍未挂上译的非 BG 有文本行生效。
  const pending = staged
    .map((line, index) => ({ index, startTime: line.startTime, text: linePlainText(line) }))
    .filter((entry) => !staged[entry.index].translatedLyric?.trim() && !staged[entry.index].isBG && entry.text !== '')
  const aligned = alignTranslationSequence(
    pending.map(({ index, startTime }) => ({ index, startTime })),
    map,
  )
  if (aligned) {
    for (const [index, text] of aligned) {
      staged[index] = { ...staged[index], translatedLyric: text }
    }
  }
  return staged
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
 * 探测「译文时间戳打在下一句原文上」的交替结构（主词内嵌双语 LRC 常见变体）：
 * 原文行 A 后紧跟译文行 C，C 的 startTime 与**下一句原文行**几乎相等（而与
 * A 不相等）。此时按文件顺序把译文并入其前一行原文，修复译文整体后移一行。
 *
 * 判定必须整歌成立：配对数达标 + 覆盖率达标 + **结构一致**——所有配对行的
 * 原文侧必须是同一脚本族、译文侧是互补脚本族。若文件中间被同脚本垫词
 * （如副歌哼唱行）打乱交替结构，窗口错位会让配对行的脚本混入两侧，
 * 此时拒绝 shifted 激活，回退到既有「同时间戳相邻配对」逻辑。
 *
 * 回退路径保留原有保护：不吞同时间的独立两句、不回退时不对齐有文本行。
 */
const detectShiftedTranslationPairs = (lines: LyricLine[]): Map<number, number> | null => {
  // 参与行：非 BG、有文本、未挂译文（tlyric 已挂的行不参与双行合并）。
  const participants: number[] = []
  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index]
    if (line.isBG || line.translatedLyric?.trim() || !linePlainText(line)) {
      continue
    }
    participants.push(index)
  }
  if (participants.length < SHIFTED_PAIR_MIN_COUNT * 2) {
    return null
  }

  // 固定窗口配对（0,1)(2,3)(4,5)...：译文时间戳打在下一句时，按文件顺序
  // 把译文并回前一行原文。窗口不滑动，避免贪心错位把上一句译文配给下一句。
  const pairs = new Map<number, number>()
  const firstScripts = new Set<string>()
  const secondScripts = new Set<string>()
  let orphans = 0
  for (let cursor = 0; cursor < participants.length; cursor += 2) {
    const firstIndex = participants[cursor]
    const secondIndex = participants[cursor + 1]
    if (secondIndex === undefined) {
      orphans += 1
      break
    }
    const first = lines[firstIndex]
    const second = lines[secondIndex]
    const firstText = linePlainText(first)
    const secondText = linePlainText(second)
    // 窗口内两行脚本体系不同 + 时间先后合理（译文不早于原文且间隔不过长）。
    const pairGap = second.startTime - first.startTime
    const firstScript = scriptSignature(firstText)
    const secondScript = scriptSignature(secondText)
    const pairable =
      firstText !== secondText
      && firstScript
      && secondScript
      && firstScript !== secondScript
      && pairGap >= -DUPLICATE_TIMESTAMP_TOLERANCE_MS
      && pairGap <= SHIFTED_PAIR_MAX_GAP_MS
    // 交替结构的定义属性：译文行时间戳应贴近**下一窗口原文行**的时间戳
    // （译文时间戳打在下一句上）。垫词行打断交替后，窗口错位会让该属性
    // 失效——此时不配对，累计为 orphan 拉低覆盖率，最终整体拒绝 shifted 激活。
    const nextFirstIndex = participants[cursor + 2]
    const crossWindowOk =
      nextFirstIndex === undefined
      || Math.abs(second.startTime - lines[nextFirstIndex].startTime)
        <= SHIFTED_CROSS_WINDOW_TOLERANCE_MS
    if (pairable && crossWindowOk) {
      pairs.set(firstIndex, secondIndex)
      firstScripts.add(firstScript)
      secondScripts.add(secondScript)
    } else {
      orphans += 1
    }
  }

  // 结构一致性：所有配对的主行同属一种脚本，译文行同属其互补脚本。
  // 若窗口错位导致脚本混入另一侧（如垫词行插入后配对跨位），整体放弃。
  const structureConsistent =
    firstScripts.size === 1
    && secondScripts.size === 1
    && [...firstScripts][0] !== [...secondScripts][0]

  if (
    !structureConsistent
    || pairs.size < SHIFTED_PAIR_MIN_COUNT
    || (pairs.size / (pairs.size + orphans)) < SHIFTED_PAIR_MIN_RATIO
  ) {
    return null
  }
  return pairs
}

/** 按交替结构配对表重建行序列：译文行并入前一行原文（非 Han 优先主行）。 */
const applyShiftedTranslationPairs = (
  lines: LyricLine[],
  pairs: Map<number, number>,
): LyricLine[] => {
  const secondIndexes = new Set(pairs.values())
  const result: LyricLine[] = []
  for (let index = 0; index < lines.length; index += 1) {
    if (secondIndexes.has(index)) {
      continue
    }
    const pairIndex = pairs.get(index)
    if (pairIndex === undefined) {
      result.push({ ...lines[index] })
      continue
    }
    const current = lines[index]
    const translationLine = lines[pairIndex]
    const { main, translation } = pickMainAndTranslation(current, translationLine)
    result.push({
      ...main,
      startTime: Math.min(current.startTime, translationLine.startTime),
      // 取配对行较长结束时间，避免合并后活跃窗口过短导致高亮只闪一下。
      endTime: Math.max(current.endTime, translationLine.endTime),
      translatedLyric: translation,
    })
  }
  return result
}

/**
 * 合并「同时间戳双语主行」：原文主行 + 译文 → translatedLyric。
 * 只有文字体系明确不同才合并，避免吞掉同时间的两句独立歌词。
 * 主译按脚本语义选择（非 Han 优先于 Han），不依赖 LRC 行序。
 *
 * 优先探测「译文时间戳打在下一句」的整歌交替结构（修复译文整体后移一行）；
 * 结构不成立时回退到同时间戳相邻配对。
 */
export const mergeDuplicateTimestampTranslations = (lines: LyricLine[]): LyricLine[] => {
  if (lines.length < 2) {
    return lines.map((line) => ({ ...line }))
  }

  const shiftedPairs = detectShiftedTranslationPairs(lines)
  if (shiftedPairs) {
    // shifted 交替合并后，仍可能残留「同时间戳双语对」（混合形态文件）：
    // 对中间结果再跑一遍同时间戳相邻配对，收敛整齐结构的译文。
    return mergeSameTimestampPairs(applyShiftedTranslationPairs(lines, shiftedPairs))
  }

  return mergeSameTimestampPairs(lines)
}

/** 合并「同时间戳双语主行」：原文主行 + 译文 → translatedLyric。 */
const mergeSameTimestampPairs = (lines: LyricLine[]): LyricLine[] => {
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
      && Math.abs(current.startTime - next.startTime) <= DUPLICATE_TIMESTAMP_TOLERANCE_MS
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
