import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import {
  __keepaliveInternals,
  startKeepAlive,
  stopKeepAlive,
} from '../../src/features/player/keepalive'

const { platformMock } = vi.hoisted(() => ({
  platformMock: vi.fn(),
}))

vi.mock('@capacitor/core', () => ({
  Capacitor: { getPlatform: (...args: unknown[]) => platformMock(...args) },
}))

class FakeSource {
  started = false
  stopped = false

  connect() {}

  disconnect() {}

  start() {
    this.started = true
  }

  stop() {
    this.stopped = true
  }
}

class FakeGain {
  gain = { value: 0 }

  connect() {}
}

class FakeAudioContext {
  static newCount = 0

  static defaultState = 'running'

  state = FakeAudioContext.defaultState
  destination = {}

  constructor() {
    FakeAudioContext.newCount += 1
  }

  createGain(): FakeGain {
    return new FakeGain()
  }

  createConstantSource(): FakeSource {
    return new FakeSource()
  }

  resume(): Promise<void> {
    this.state = 'running'
    return Promise.resolve()
  }
}

const installWebGlobals = (options?: { withContext?: boolean; suspended?: boolean }): void => {
  const withContext = options?.withContext !== false
  Object.defineProperty(globalThis, 'window', {
    configurable: true,
    value: withContext ? { AudioContext: FakeAudioContext } : {},
  })
  if (withContext) {
    globalThis.AudioContext = FakeAudioContext
  }
  FakeAudioContext.defaultState = options?.suspended ? 'suspended' : 'running'
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    value: { getItem: () => null },
  })
}

describe('keepalive（08-18-carwith-bg-ctrl-fix）', () => {
  beforeEach(() => {
    platformMock.mockReturnValue('android')
    FakeAudioContext.newCount = 0
    installWebGlobals()
  })

  afterEach(() => {
    __keepaliveInternals.reset()
    platformMock.mockReset()
    // @ts-expect-error 清理测试注入的全局
    delete globalThis.window
    // @ts-expect-error 清理测试注入的全局
    delete globalThis.localStorage
  })

  it('a. 非 Android 平台不启动保活（web/iOS 保持空转）', () => {
    platformMock.mockReturnValue('web')
    startKeepAlive()
    expect(__keepaliveInternals.isRunning).toBe(false)
    expect(__keepaliveInternals.hasContext).toBe(false)

    __keepaliveInternals.reset()
    platformMock.mockReturnValue('ios')
    startKeepAlive()
    expect(__keepaliveInternals.isRunning).toBe(false)
    expect(__keepaliveInternals.hasContext).toBe(false)
  })

  it('b. Android 下启动保活轨：创建 AudioContext 且 running', () => {
    startKeepAlive()
    expect(FakeAudioContext.newCount).toBe(1)
    expect(__keepaliveInternals.hasContext).toBe(true)
    expect(__keepaliveInternals.isRunning).toBe(true)
  })

  it('c. 幂等：重复 startKeepAlive 不重复创建 AudioContext、不重复起轨', () => {
    startKeepAlive()
    startKeepAlive()
    startKeepAlive()
    expect(FakeAudioContext.newCount).toBe(1)
    expect(__keepaliveInternals.isRunning).toBe(true)
  })

  it('d. stopKeepAlive 后停止运行；再次 start 复用同一 AudioContext', () => {
    startKeepAlive()
    stopKeepAlive()
    expect(__keepaliveInternals.isRunning).toBe(false)
    expect(__keepaliveInternals.hasContext).toBe(true)

    startKeepAlive()
    expect(FakeAudioContext.newCount).toBe(1)
    expect(__keepaliveInternals.isRunning).toBe(true)
  })

  it('e. AudioContext 不可用（无 skia/禁用）时静默降级，不抛错、不阻塞', () => {
    installWebGlobals({ withContext: false })
    expect(() => startKeepAlive()).not.toThrow()
    expect(__keepaliveInternals.isRunning).toBe(false)
    expect(__keepaliveInternals.hasContext).toBe(false)
  })

  it('f. suspended 的 AudioContext 会触发 resume（autoplay 策略下自恢复）', async () => {
    const resumeSpy = vi.fn(() => Promise.resolve())
    FakeAudioContext.prototype.resume = resumeSpy
    installWebGlobals({ suspended: true })
    startKeepAlive()
    await Promise.resolve()
    await Promise.resolve()
    expect(resumeSpy).toHaveBeenCalled()
    expect(__keepaliveInternals.isRunning).toBe(true)
  })
})