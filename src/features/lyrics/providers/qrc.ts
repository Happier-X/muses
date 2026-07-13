/**
 * QQ QRC：AMLL decryptQrcHex + 从 XML 提取 LyricContent。
 * 文档：https://amll.dev/guides/lyric/formats
 */
import { decryptQrcHex } from '@applemusic-like-lyrics/lyric'

/** 判断是否像明文 QRC / YRC 行时间轴 */
export const looksLikeWordLevelBracket = (text: string): boolean => {
  const line = text.split(/\r?\n/).find((l) => l.trim() && !l.trim().startsWith('{'))
  if (!line) {
    return false
  }
  return /^\[\d+,\d+\]/.test(line.trim())
}

/**
 * 从 decryptQrcHex 结果（XML）抽出 LyricContent 属性内的 QRC 正文。
 */
export const extractQrcLyricContent = (xmlOrPlain: string): string => {
  const raw = xmlOrPlain?.trim() || ''
  if (!raw) {
    return ''
  }
  if (looksLikeWordLevelBracket(raw) && !raw.includes('<Qrc')) {
    return raw
  }
  const match =
    raw.match(/LyricContent="([\s\S]*?)"\s*\/>/i)
    || raw.match(/LyricContent="([\s\S]*?)"/i)
  if (!match?.[1]) {
    return raw
  }
  return match[1]
    .replace(/&quot;/g, '"')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/&#10;/g, '\n')
    .replace(/&#13;/g, '\r')
}

/** hex 加密串 → 明文 QRC；失败返回 null */
export const decryptQrcToPlain = (encryptedHex: string): string | null => {
  const hex = encryptedHex?.trim()
  if (!hex || !/^[0-9a-fA-F]+$/.test(hex) || hex.length % 2 !== 0) {
    return null
  }
  try {
    const xml = decryptQrcHex(hex)
    const plain = extractQrcLyricContent(xml).trim()
    if (!plain || !looksLikeWordLevelBracket(plain)) {
      return null
    }
    return plain
  } catch {
    return null
  }
}
