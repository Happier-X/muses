import { Capacitor } from '@capacitor/core'

/**
 * WebView JS 保活（08-18-carwith-bg-ctrl-fix，方案 A）
 *
 * CarWith 连接后手机 WebView 页面不可见，Chromium 对隐藏页面冻结/深度节流 JS：
 * 原生 complete 事件与媒体按钮命令都依赖 WebView JS 执行，JS 冻结后
 * 「播完自动切歌」与「媒体通知按钮」全部失效。
 *
 * 这里在播放中常驻一条 gain=0 的静音 Web Audio 轨（ConstantSource → gain 0 → destination），
 * 让页面携带「ongoing media」活动标签，阻止 Chromium 对隐藏页面执行冻结；
 * 输出为数字静音，不影响原生音频播放。
 *
 * 约束：
 * - 仅在 Android 原生且存在播放会话时运行（无播放会话不启动，省电）。
 * - 任何异常静默降级（不抛、不阻塞播放）。
 * - 暂停/停止即停轨；AudioContext 复用，避免反复创建。
 */
let audioContext: AudioContext | null = null
let sourceNode: AudioScheduledSourceNode | null = null
let running = false

const isAndroid = (): boolean => Capacitor.getPlatform() === 'android'

const debugLog = (message: string): void => {
  try {
    if (localStorage.getItem('muses:debug-keepalive') === '1') {
      console.info('[MusesKeepAlive]', message)
    }
  } catch {
    // localStorage 不可用（无痕/隐私模式）时静默
  }
}

const ensureContext = (): AudioContext | null => {
  if (audioContext) {
    if (audioContext.state === 'suspended') {
      void audioContext.resume().catch(() => undefined)
    }
    return audioContext
  }
  try {
    const Ctor = window.AudioContext
      ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!Ctor) {
      debugLog('AudioContext 不可用，降级')
      return null
    }
    audioContext = new Ctor()
    if (audioContext.state === 'suspended') {
      void audioContext.resume().catch(() => undefined)
    }
    debugLog('AudioContext 已创建')
  } catch (error) {
    audioContext = null
    debugLog(`AudioContext 创建失败: ${error instanceof Error ? error.message : String(error)}`)
  }
  return audioContext
}

/**
 * 启动保活轨（幂等）。仅 Android；已有轨直接复用，不重复创建。
 * 若上下文暂处 suspended（autoplay 策略），会尝试 resume；失败静默，
 * 等下一次仍在用户手势窗口内的媒体操作再试。
 */
export const startKeepAlive = (): void => {
  if (!isAndroid() || running) {
    return
  }
  const context = ensureContext()
  if (!context) {
    return
  }
  try {
    const gain = context.createGain()
    gain.gain.value = 0
    const source = context.createConstantSource()
    source.connect(gain)
    gain.connect(context.destination)
    source.start()
    sourceNode = source
    running = true
    debugLog('keepalive 轨已启动')
  } catch (error) {
    stopKeepAlive()
    debugLog(`keepalive 启动失败: ${error instanceof Error ? error.message : String(error)}`)
  }
}

/** 停止保活轨（幂等）：停轨断开，保留 AudioContext 供下次复用。 */
export const stopKeepAlive = (): void => {
  running = false
  const source = sourceNode
  if (source) {
    try {
      source.stop()
      source.disconnect()
    } catch {
      // 源已停止/断开则忽略
    }
    sourceNode = null
  }
  debugLog('keepalive 轨已停止')
}

/** 测试辅助：暴露内部状态，仅供 unit test 断言（勿在业务代码使用）。 */
export const __keepaliveInternals = {
  get isRunning(): boolean {
    return running
  },
  get hasContext(): boolean {
    return audioContext !== null
  },
  reset: (): void => {
    running = false
    sourceNode = null
    audioContext = null
  },
}