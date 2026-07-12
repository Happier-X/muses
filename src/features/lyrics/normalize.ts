/**
 * 规范化歌名/歌手/专辑文本，便于模糊匹配：
 * - NFKC 全角半角统一
 * - 小写
 * - 去掉常见括号与装饰符号
 * - 去掉 live/remix 等常见后缀词
 * - 折叠空白
 */
export const normalizeText = (input: string | undefined | null): string => {
  if (!input) {
    return ''
  }

  return input
    .normalize('NFKC')
    .toLowerCase()
    .replace(/[「」『』【】[\]()（）{}<>《》""'']/g, ' ')
    .replace(/\b(live|remix|remaster(?:ed)?|ver\.?|version|acoustic|instrumental|off\s*vocal|karaoke|edit|mix)\b/gi, ' ')
    .replace(/[-–—_:|/\\·•~,，.。!！?？+]+/g, ' ')
    .replace(/[\u3000\s]+/g, ' ')
    .trim()
}

/** 将歌手字符串拆成可匹配 token（支持 /、&、feat. 等分隔） */
export const splitArtistTokens = (artist: string | undefined | null): string[] => {
  if (!artist?.trim()) {
    return []
  }

  // 先按原始分隔符拆分，再分别 normalize；否则 normalizeText 会把 / 等符号抹成空格。
  return artist
    .split(/\s*(?:\/|&|\bfeat\.?\b|\bft\.?\b|\bfeaturing\b|\bx\b|\band\b|、|；|;)\s*/i)
    .map((token) => normalizeText(token))
    .filter((token) => token.length > 0)
}
