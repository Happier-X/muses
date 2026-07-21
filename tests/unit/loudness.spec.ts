import { describe, expect, test } from 'vitest'
import {
  PLAYBACK_VOLUME_MAX,
  PLAYBACK_VOLUME_MIN,
  dbToPlaybackVolume,
  normalizeReplayGainDbValue,
  parseReplayGainDb,
} from '@/features/player/loudness'

describe('parseReplayGainDb', () => {
  test('解析带 dB 单位的字符串', () => {
    expect(parseReplayGainDb('-6.54 dB')).toBeCloseTo(-6.54)
    expect(parseReplayGainDb('+3.2 dB')).toBeCloseTo(3.2)
    expect(parseReplayGainDb('0 dB')).toBe(0)
  })

  test('解析纯数字字符串', () => {
    expect(parseReplayGainDb('-6.54')).toBeCloseTo(-6.54)
    expect(parseReplayGainDb('1.5')).toBeCloseTo(1.5)
  })

  test('非法输入返回 null', () => {
    expect(parseReplayGainDb('')).toBeNull()
    expect(parseReplayGainDb('   ')).toBeNull()
    expect(parseReplayGainDb('not-a-number')).toBeNull()
    expect(parseReplayGainDb('dB')).toBeNull()
  })

  test('R128 Q7.8 整数按 ÷256 换算为 dB', () => {
    // -1792 / 256 = -7 dB（常见 Opus R128_TRACK_GAIN）
    expect(parseReplayGainDb('-1792')).toBeCloseTo(-7)
    expect(parseReplayGainDb('512')).toBeCloseTo(2)
  })

  test('无法换算到合理 dB 区间时丢弃', () => {
    expect(parseReplayGainDb('99999')).toBeNull()
    expect(normalizeReplayGainDbValue(Number.NaN)).toBeNull()
  })
})

describe('dbToPlaybackVolume', () => {
  test('关闭均衡始终 1.0', () => {
    expect(dbToPlaybackVolume(-6, false)).toBe(PLAYBACK_VOLUME_MAX)
    expect(dbToPlaybackVolume(3, false)).toBe(PLAYBACK_VOLUME_MAX)
  })

  test('无标签 / 非有限 dB 为 1.0', () => {
    expect(dbToPlaybackVolume(null, true)).toBe(PLAYBACK_VOLUME_MAX)
    expect(dbToPlaybackVolume(undefined, true)).toBe(PLAYBACK_VOLUME_MAX)
    expect(dbToPlaybackVolume(Number.NaN, true)).toBe(PLAYBACK_VOLUME_MAX)
    expect(dbToPlaybackVolume(Number.POSITIVE_INFINITY, true)).toBe(PLAYBACK_VOLUME_MAX)
  })

  test('负增益衰减响曲', () => {
    const volume = dbToPlaybackVolume(-6, true)
    expect(volume).toBeCloseTo(10 ** (-6 / 20), 5)
    expect(volume).toBeLessThan(1)
    expect(volume).toBeGreaterThanOrEqual(PLAYBACK_VOLUME_MIN)
  })

  test('正增益最多顶到 1.0（无法超满幅放大）', () => {
    expect(dbToPlaybackVolume(6, true)).toBe(PLAYBACK_VOLUME_MAX)
    expect(dbToPlaybackVolume(0, true)).toBe(PLAYBACK_VOLUME_MAX)
  })

  test('极负增益 clamp 到下限 0.1', () => {
    expect(dbToPlaybackVolume(-40, true)).toBe(PLAYBACK_VOLUME_MIN)
  })
})
