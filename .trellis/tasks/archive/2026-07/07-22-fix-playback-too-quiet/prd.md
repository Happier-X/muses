# 播放音量偏小 (#51)

## Goal

用户反馈播放时声音偏小；在合理范围内提高可听响度，并保持设置可控。

## Background

- 音量经 `dbToPlaybackVolume` → `NativeAudio.setVolume` / play 时 `volume`。
- 插件约定范围 **0.1–1.0**。
- **音量均衡默认开启**：负 ReplayGain 会明显衰减；正增益不能超过 1.0。

## 业界调研摘要

- 开响度均衡/RG 后整体变安静是常见设计（压响轨为主）。
- Spotify / foobar / Poweramp：保留开关，用 **目标响度或 preamp** 调听感，而非默认关功能。

## Requirements（已确认 R1）

### R1. 产品策略（确认：D 轻量 + preamp +6 dB）

- 保持「音量均衡」开关，**默认仍开启**。
- 均衡开启且有有效 `replayGainTrackDb` 时：
  - `volume = clamp_01(10 ** ((rgDb + PREAMP_DB) / 20))`
  - `PREAMP_DB = 6`
  - clamp 到 **[0.1, 1.0]**。
- 均衡关闭或无 RG：volume = **1.0**（与现网一致）。
- 设置文案补充说明（可关均衡若仍嫌小）。

### R2. 行为约束

- 不改 capgo 源码。
- 开关即时生效（`setLoudnessNormalizeEnabled` 后重算当前曲 volume）。
- 单测覆盖 preamp 计算。

### Out of Scope

- 设置页 preamp 滑条（二期）
- 系统媒体音量劫持
- 真峰值限制器 / 动态压缩
- 发版

## Acceptance Criteria

- [ ] 负 RG 曲在开启均衡时比「无 preamp」更响（例如 -6 dB + 6 dB → 约 1.0）
- [ ] 无 RG / 关均衡仍为 1.0
- [ ] 仍不超插件 1.0；极负增益仍有下限 0.1
- [ ] 单测 + lint / unit / build 通过
- [ ] 关闭 #51
