/**
 * 深浅主题三态模式：跟随系统 / 亮 / 暗。
 *
 * - `useSystemDark()` 在 main.ts 初始化时调用：读持久化模式并应用（初始化立即同步，避免首帧闪白/闪黑）
 * - `themeMode` 模块级 Ref（单例），组件可直接 import 读取/展示当前模式
 * - `cycleThemeMode()` 循环 跟随系统 → 亮 → 暗 → 跟随系统，并持久化到 localStorage
 *
 * 亮/暗模式下不监听系统主题；跟随系统模式下监听 prefers-color-scheme
 * 同步 document.documentElement 的 .dark class（Konsta/本项目暗色机制）。
 */
import { ref, type Ref } from 'vue'

export type ThemeMode = 'system' | 'light' | 'dark'

const STORAGE_KEY = 'muses-theme'
const ORDER: ThemeMode[] = ['system', 'light', 'dark']

function readStoredMode(): ThemeMode {
  if (typeof window === 'undefined' || !('localStorage' in window)) return 'system'
  const value = window.localStorage.getItem(STORAGE_KEY)
  return value === 'light' || value === 'dark' ? value : 'system'
}

function systemPrefersDark(): boolean {
  return typeof window !== 'undefined' && 'matchMedia' in window
    ? window.matchMedia('(prefers-color-scheme: dark)').matches
    : false
}

/** 当前主题模式（模块级单例；无组件树内 provide/inject 依赖） */
export const themeMode: Ref<ThemeMode> = ref(readStoredMode())

let mediaQuery: MediaQueryList | null = null

function apply(): void {
  if (typeof document === 'undefined') return
  const isDark =
    themeMode.value === 'dark' || (themeMode.value === 'system' && systemPrefersDark())
  document.documentElement.classList.toggle('dark', isDark)
}

function persist(): void {
  if (typeof window === 'undefined' || !('localStorage' in window)) return
  window.localStorage.setItem(STORAGE_KEY, themeMode.value)
}

/** 仅跟随系统模式绑定系统主题变化监听；亮/暗模式解绑（手动模式优先） */
function rebind(): void {
  if (typeof window === 'undefined' || !('matchMedia' in window)) return
  if (mediaQuery) {
    mediaQuery.removeEventListener?.('change', apply)
    // 旧 WebView 兜底：addListener/addEventListener 双注册本身无害，但手动模式需成对解绑
    if ('removeListener' in mediaQuery) mediaQuery.removeListener(apply)
  }
  if (themeMode.value !== 'system') return
  mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.addEventListener?.('change', apply)
  mediaQuery.addListener?.(apply)
}

/** 应用初始化（main.ts 调用一次） */
export const useSystemDark = (): void => {
  if (typeof window === 'undefined') return
  apply()
  rebind()
}

/** 循环切换 跟随系统 → 亮 → 暗 → 跟随系统，持久化并返回新模式 */
export const cycleThemeMode = (): ThemeMode => {
  const next = ORDER[(ORDER.indexOf(themeMode.value) + 1) % ORDER.length]
  themeMode.value = next
  persist()
  rebind()
  apply()
  return next
}