# 设计：#51 播放音量偏小

## 决策

D 轻量：默认开均衡 + **+6 dB preamp**，volume 仍 clamp 到 [0.1, 1.0]。

## 改动点

### `loudness.ts`

- 导出 `LOUDNESS_PREAMP_DB = 6`（或 `DEFAULT_LOUDNESS_PREAMP_DB`）。
- `dbToPlaybackVolume(db, enabled)`：
  - `!enabled || db == null` → 1.0
  - 否则 `linear = 10 ** ((db + LOUDNESS_PREAMP_DB) / 20)`，再 clamp。

### 设置文案

- `SettingsPage`「音量均衡」说明：统一曲库响度；可能略降最响曲；若整体偏小可关闭。

### 规范

- `features-player.md` 响度一节写明 preamp +6。

## 示例

| RG dB | 旧 volume | 新 volume (+6) |
|-------|-----------|----------------|
| -6 | ~0.50 | ~1.0 |
| -12 | ~0.25 | ~0.50 |
| 0 | 1.0 | 1.0（clamp） |
| +3 | 1.0 | 1.0 |
| null / off | 1.0 | 1.0 |

## 风险

- 已接近满幅的曲 + 正 RG + preamp 仍被 1.0 顶住，无额外削波路径（插件层）。
- 极响曲仍可能被压，符合均衡目标。
