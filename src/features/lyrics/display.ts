import type { LyricLine } from '@applemusic-like-lyrics/core'

/** 隐藏 AMLL 翻译/音译时保留主歌词与逐字时间轴。 */
export const applyLyricTranslationVisibility = (lines: LyricLine[], visible: boolean): LyricLine[] => {
  if (visible) {
    return lines
  }
  return lines.map((line) => ({
    ...line,
    translatedLyric: '',
    romanLyric: '',
  }))
}
