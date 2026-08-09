/**
 * 跟随系统深浅色：Konsta UI 的暗色模式由 `.dark` class 驱动
 * （Tailwind v4 @custom-variant dark），非 prefers-color-scheme 媒体查询。
 * 此处监听系统主题，同步到 document.documentElement 的 .dark class，
 * 保持迁移前“跟随系统”行为。初始化立即同步，避免首帧闪白/闪黑。
 */
export const useSystemDark = (): void => {
  if (typeof window === 'undefined' || !('matchMedia' in window)) {
    return
  }

  const query = window.matchMedia('(prefers-color-scheme: dark)')

  const apply = () => {
    document.documentElement.classList.toggle('dark', query.matches)
  }

  apply()
  // 现代浏览器统一走 change 事件；addListener 为旧 WebView 兜底
  query.addEventListener?.('change', apply)
  query.addListener?.(apply)
}
