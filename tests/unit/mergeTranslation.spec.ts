import { describe, expect, it } from 'vitest'
import { parseLrc } from '@applemusic-like-lyrics/lyric'
import {
  attachTimedLyricsTranslation,
  mergeDuplicateTimestampTranslations,
  prepareLyricLinesForDisplay,
} from '../../src/features/lyrics/mergeTranslation'

const mainText = (line: { words: Array<{ word: string }> }): string =>
  line.words.map((w) => w.word).join('')

const standaloneHanCount = (
  lines: Array<{ words: Array<{ word: string }>; translatedLyric?: string }>,
): number =>
  lines.filter(
    (l) =>
      !l.translatedLyric?.trim()
      && /[\u4e00-\u9fff]/.test(l.words.map((w) => w.word).join('')),
  ).length

const translatedOf = (
  lines: Array<{ words: Array<{ word: string }>; translatedLyric?: string }>,
  main: string,
): string | undefined => lines.find((l) => mainText(l) === main)?.translatedLyric

describe('mergeTranslation（08-17-fix-lyric-translation-offset）', () => {
  it('a. 译文时间戳打在下一句：整体修复，无孤立中文行，每条译文挂回前一句原文', () => {
    const lrc = `[00:10.00]Hello world
[00:15.00]你好世界
[00:15.00]Second line
[00:20.00]第二行
[00:20.00]Third line
[00:25.00]第三行`
    const lines = prepareLyricLinesForDisplay(parseLrc(lrc), null)
    expect(lines).toHaveLength(3)
    expect(standaloneHanCount(lines)).toBe(0)
    expect(translatedOf(lines, 'Hello world')).toBe('你好世界')
    expect(translatedOf(lines, 'Second line')).toBe('第二行')
    expect(translatedOf(lines, 'Third line')).toBe('第三行')
  })

  it('b. 混合结构：shifted 主导 + 尾部同时间戳对齐对：全部窗口通过交替校验，无错位', () => {
    // shift 对：(Hello,你好)(Second,第二行)(Third,第三行) 译文时间戳均在下一句上；
    // 尾部 (Fourth,第四句) 为同时间戳对齐对。三行共 25s 处合法（C3=E4 时间），
    // 所有窗口均满足交替定义属性（译文时间戳贴近下一窗口原文时间戳）。
    const lrc = `[00:10.00]Hello world
[00:15.00]你好世界
[00:15.00]Second line
[00:20.00]第二行
[00:20.00]Third line
[00:25.00]第三行
[00:25.00]Fourth line
[00:25.00]第四句`
    const lines = prepareLyricLinesForDisplay(parseLrc(lrc), null)
    expect(lines).toHaveLength(4)
    expect(standaloneHanCount(lines)).toBe(0)
    expect(translatedOf(lines, 'Hello world')).toBe('你好世界')
    expect(translatedOf(lines, 'Second line')).toBe('第二行')
    expect(translatedOf(lines, 'Third line')).toBe('第三行')
    expect(translatedOf(lines, 'Fourth line')).toBe('第四句')
  })

  it('c. tlyric 时间戳对齐：逐行挂载且位置正确', () => {
    const lrc = `[00:10.00]Hello world
[00:15.00]Second line
[00:20.00]Third line`
    const tlyric = `[00:10.00]你好世界
[00:15.00]第二行
[00:20.00]第三行`
    const lines = prepareLyricLinesForDisplay(parseLrc(lrc), tlyric)
    expect(lines.filter((l) => l.translatedLyric?.trim())).toHaveLength(3)
    expect(translatedOf(lines, 'Hello world')).toBe('你好世界')
    expect(translatedOf(lines, 'Second line')).toBe('第二行')
    expect(translatedOf(lines, 'Third line')).toBe('第三行')
  })

  it('d. 同时间戳双语成对：英文在前 / 中文在前均合并且主行非 Han', () => {
    const lrcEn = `[00:10.00]Hello world
[00:10.00]你好世界
[00:15.00]Second line
[00:15.00]第二行`
    const en = prepareLyricLinesForDisplay(parseLrc(lrcEn), null)
    expect(en).toHaveLength(2)
    expect(mainText(en[0])).toBe('Hello world')
    expect(en[0].translatedLyric).toBe('你好世界')

    const lrcZh = `[00:10.00]你好世界
[00:10.00]Hello world
[00:15.00]第二行
[00:15.00]Second line`
    const zh = prepareLyricLinesForDisplay(parseLrc(lrcZh), null)
    expect(zh).toHaveLength(2)
    expect(mainText(zh[0])).toBe('Hello world')
    expect(zh[0].translatedLyric).toBe('你好世界')
  })

  it('e. tlyric 已挂载的行不再双行合并（不重复覆盖）', () => {
    const lrc = `[00:10.00]Hello world
[00:15.00]Second line`
    const tlyric = `[00:10.00]你好世界`
    const lines = prepareLyricLinesForDisplay(parseLrc(lrc), tlyric)
    expect(lines).toHaveLength(2)
    expect(standaloneHanCount(lines)).toBe(0)
    expect(translatedOf(lines, 'Hello world')).toBe('你好世界')
    expect(translatedOf(lines, 'Second line') ?? '').toBe('')
  })

  it('f. 同时间戳两句独立歌词（同为 Latin）：不合并', () => {
    const lrc = `[00:10.00]Hello world
[00:10.00]Second voice line`
    const lines = prepareLyricLinesForDisplay(parseLrc(lrc), null)
    expect(lines).toHaveLength(2)
    expect(lines[0].translatedLyric?.trim() ?? '').toBe('')
    expect(lines[1].translatedLyric?.trim() ?? '').toBe('')
  })

  it('g. 零星中文行（交替结构不成立）：触发不了 shifted 合并，保留原行', () => {
    const lrc = `[00:10.00]Hello world
[00:15.00]Second line
[00:20.00]Third line
[00:25.00]Fourth line
[00:30.00]一条单独的注解`
    const lines = prepareLyricLinesForDisplay(parseLrc(lrc), null)
    expect(lines).toHaveLength(5)
    expect(standaloneHanCount(lines)).toBe(1)
  })

  it('h. yrc 系统性偏移（>80ms 容差）：序列感知回退按行序挂载', () => {
    const lines = parseLrc(`[00:10.42]Hello world
[00:15.46]Second line
[00:20.52]Third line
[00:25.57]Fourth line
[00:30.61]Fifth line`)
    const tlyric = `[00:10.00]你好世界
[00:15.00]第二行
[00:20.00]第三行
[00:25.00]第四行
[00:30.00]第五行`
    const out = prepareLyricLinesForDisplay(lines, tlyric)
    expect(out.filter((l) => l.translatedLyric?.trim())).toHaveLength(5)
    expect(translatedOf(out, 'Hello world')).toBe('你好世界')
    expect(translatedOf(out, 'Fifth line')).toBe('第五行')
  })

  it('i. 序列回退不破坏对齐良好的数据（80ms 主路径优先）', () => {
    const lines = parseLrc(`[00:10.00]Hello world
[00:15.00]Second line`)
    const tlyric = `[00:10.00]你好世界
[00:15.00]第二行`
    const out = prepareLyricLinesForDisplay(lines, tlyric)
    expect(out).toHaveLength(2)
    expect(translatedOf(out, 'Second line')).toBe('第二行')
  })

  it('j. attachTimed 消费过的 stamp 不进序列回退池，同译文不重复挂载', () => {
    // 首遍 80ms 精确挂好第一条；第二条偏差 >80ms 靠序列回退——
    // 但 tlyric 只有第一条的 stamp，回退不得把同一译文再挂到第二条。
    const lines = parseLrc(`[00:10.00]Hello world
[00:30.00]Second line`)
    const tlyric = `[00:10.00]你好世界`
    const out = prepareLyricLinesForDisplay(lines, tlyric)
    expect(translatedOf(out, 'Hello world')).toBe('你好世界')
    expect(translatedOf(out, 'Second line') ?? '').toBe('')
  })

  it('l. 交替结构被同脚本垫词行打断（副歌哼唱）：结构不一致，不激活 shifted，不系统性错配', () => {
    // 窗口错位会把 han 配进 first 侧、latin 配进 second 侧 → 结构一致性校验拒绝。
    const lrc = `[00:10.00]A line
[00:15.00]甲一号
[00:17.00]B line
[00:18.00]B2 yeah
[00:20.00]C line
[00:25.00]乙一号
[00:27.00]D line
[00:32.00]丙一号
[00:35.00]丁一号`
    const lines = prepareLyricLinesForDisplay(parseLrc(lrc), null)
    // 结构一致性校验拒绝 shifted 激活：不得出现「翻译配给下一句」的系统性错配。
    expect(standaloneHanCount(lines)).toBeGreaterThanOrEqual(2)
    // 甲一号绝不能挂到 B line。
    const bLine = lines.find((l) => mainText(l) === 'B line')
    expect(bLine?.translatedLyric?.trim() ?? '').not.toBe('甲一号')
    // 乙一号是 C line 的译文，不能挂到 D line。
    const dLine = lines.find((l) => mainText(l) === 'D line')
    expect(dLine?.translatedLyric?.trim() ?? '').not.toBe('乙一号')
  })

  it('m. tlyric 自身整句错移一段（时间戳打在下一句）：序列回退边界校验拒绝，不产生同类错位', () => {
    // 主行 [10,15,20,25]，tlyric 整体偏后半句 [15.x,20.x,25.x,30.x]：
    // 首遍 80ms 全部 miss，若序列回退无边界校验，会把 hello 的译文挂给 Second，
    // 复现「译文整体后移一行」的同类错位（check 报告 R2）。
    const lines = parseLrc(`[00:10.00]Hello world
[00:15.00]Second line
[00:20.00]Third line
[00:25.00]Fourth line`)
    const tlyric = `[00:15.50]你好世界
[00:20.50]第二行
[00:25.50]第三行
[00:30.50]第四行`
    const out = prepareLyricLinesForDisplay(lines, tlyric)
    // 边界校验：首行无译 + 末 stamp 无主行承接 → 结构错移，放弃回退。
    // 不允许「首行译文被挂到第二行」的同类错位（译文整体后移一行）。
    expect(translatedOf(out, 'Hello world') ?? '').toBe('')
    expect(translatedOf(out, 'Second line') ?? '').toBe('')
  })

  it('k. 纯函数：输入行不被原地修改', () => {
    const lrc = `[00:10.00]Hello world`
    const lines = parseLrc(lrc)
    const before = JSON.stringify(lines)
    mergeDuplicateTimestampTranslations(lines)
    attachTimedLyricsTranslation(lines, '[00:10.00]你好世界')
    expect(JSON.stringify(lines)).toBe(before)
  })
})