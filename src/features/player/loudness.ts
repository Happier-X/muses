/** capgo NativeAudio 文档约定的音量范围 */
export const PLAYBACK_VOLUME_MIN = 0.1
export const PLAYBACK_VOLUME_MAX = 1.0

/**
 * 响度均衡开启时叠加的 preamp（dB）。
 * 纯 ReplayGain 目标偏安静；+6 dB 抬高听感，仍 clamp 到插件 [0.1, 1.0]（#51）。
 */
export const LOUDNESS_PREAMP_DB = 6

/** 合理 track gain 范围（dB）；超出则视为非法或需 Q7.8 换算 */
const REPLAY_GAIN_DB_ABS_MAX = 30

/**
 * 将解析出的数值规范为 track gain dB。
 * 常规 ReplayGain 已是 dB；Opus 等 R128_TRACK_GAIN 常为 Q7.8 整数（÷256 → dB）。
 * 无法落入合理 dB 区间时返回 null，禁止写假增益。
 */
export const normalizeReplayGainDbValue = (value: number): number | null => {
  if (!Number.isFinite(value)) {
    return null
  }

  if (Math.abs(value) <= REPLAY_GAIN_DB_ABS_MAX) {
    return value
  }

  // Q7.8：典型 |value| 数百～数千，换算后应落在合理 dB 区间
  const asQ78 = value / 256
  if (Number.isFinite(asQ78) && Math.abs(asQ78) <= REPLAY_GAIN_DB_ABS_MAX) {
    return asQ78
  }

  return null
}

/**
 * 解析 ReplayGain 等标签中的 dB 字符串。
 * 支持 `"-6.54 dB"` / `"-6.54"`；R128 Q7.8 整数按 ÷256；非法返回 null。
 */
export const parseReplayGainDb = (raw: string): number | null => {
  const trimmed = raw.trim()
  if (!trimmed) {
    return null
  }

  const withoutUnit = trimmed.replace(/\s*dB\s*$/i, '').trim()
  if (!withoutUnit) {
    return null
  }

  const value = Number.parseFloat(withoutUnit)
  return normalizeReplayGainDbValue(value)
}

const clampPlaybackVolume = (linear: number): number => {
  if (!Number.isFinite(linear)) {
    return PLAYBACK_VOLUME_MAX
  }
  return Math.min(PLAYBACK_VOLUME_MAX, Math.max(PLAYBACK_VOLUME_MIN, linear))
}

/**
 * ReplayGain dB → 播放线性音量（相对 1.0），并 clamp 到插件范围 [0.1, 1.0]。
 * 开启均衡时叠加 LOUDNESS_PREAMP_DB（#51）；关闭均衡、无标签或非有限 dB 时返回 1.0。
 */
export const dbToPlaybackVolume = (
  db: number | null | undefined,
  enabled: boolean,
): number => {
  if (!enabled || db == null || !Number.isFinite(db)) {
    return PLAYBACK_VOLUME_MAX
  }

  const linear = 10 ** ((db + LOUDNESS_PREAMP_DB) / 20)
  return clampPlaybackVolume(linear)
}
