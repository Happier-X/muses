# 播放响度均衡（ReplayGain 轻量）

## Goal

缓解 GitHub **#46**「歌曲播放音量高低不一」：在**不引入全曲库 R128 扫描**的前提下，对**已带响度标签**的歌曲在播放时应用增益调整，并由设置开关控制。

## Background

- 曲库来自用户本地 / WebDAV，母带响度差异大，系统音量固定时体感忽大忽小。
- 现状：`@capgo/capacitor-native-audio` 支持 `volume` / `setVolume`（约 0.1–1.0），业务层未使用。
- 元数据读取：`AudioMetadataReader` 已用 **jaudiotagger**，可读 ID3 等标签，但当前只取 title/artist/album/lyrics/cover。
- 设置页仅有版本与检查更新，无播放相关开关。

## Related Issues

- https://github.com/Happier-X/muses/issues/46

## Requirements

### R1. 标签来源（有则用，无则不动）

- 扫描/读标签时尽量解析：
  - **ReplayGain track gain**（优先）：常见 `REPLAYGAIN_TRACK_GAIN`、jaudiotagger `FieldKey.REPLAYGAIN_TRACK_GAIN` 等
  - 可选次级：**R128 track gain**（若标签易取且不显著增复杂度）
- 解析失败或缺失 → **不写假增益**，播放按 1.0（不调整）
- **不**在 MVP 做：全文件 EBU R128 分析、专辑级 RG 优先策略可简化为 track-only

### R2. 持久化

- 将可用 track gain（dB，number）安全写入曲库 `SongItem`（字段名实现定，如 `replayGainTrackDb`）
- 遵守现有 sanitize：不写无关大字段；缺省 `undefined`/`null` 与旧数据兼容
- 重扫/读标签时有新值可更新；无标签不覆盖为 0 除非明确「清除」

### R3. 播放应用

- 播放开始后（preload/play 成功路径）根据当前曲增益 + 开关设置 `NativeAudio` 音量
- 线性增益：`volume = clamp(10^(db/20), min, max)`，遵守插件范围（文档写明 capgo 约 0.1–1.0，**无法真放大超过 1.0**）
- 开均衡且 gain 为负（偏响）→ 降低 volume；gain 为正（偏静）→ 最多顶到 1.0
- 关均衡或无标签 → volume **1.0**
- 切歌必须重算，禁止串曲增益；stop/无当前曲时恢复 1.0 或等价无副作用状态

### R4. 设置

- 设置页增加「音量均衡」开关（文案可微调）
- 默认建议：**开启**（用户来自 #46 诉求）；若担心兼容可默认关——**决策：默认开启**
- 持久化到既有 player config（`muses:player-config`）或等价键
- 开关切换时：若正在播放，立即对当前曲重设 volume

### R5. 规范与测试

- `features-player` 或 component/settings 相关 spec 写清边界与禁止「假缓冲式」全库测响度
- 单测：dB→linear 公式、无标签、开关关、clamp 边界
- 手工：有 RG 的响/静曲对比；无标签曲不炸；切歌不串

### Out of Scope

- 全曲库离线 loudness 扫描 / ffmpeg
- 用户可调目标 LUFS / 预放大滑条（可后续）
- iOS 专项（项目当前以 Android 为主；API 仍走 capgo 统一接口则顺带兼容）
- 发版

## Acceptance Criteria

- [x] 有 ReplayGain（或约定次级标签）的歌曲在开启均衡时播放响度更接近
- [x] 无标签歌曲行为与现网一致（volume 1.0）
- [x] 关闭开关后不应用标签增益
- [x] 切歌/stop 不串增益；不引入无限扫描卡顿
- [x] 设置可切换并持久化
- [x] lint / 相关 unit 通过
- [x] 关闭或评论 #46

## Notes

- **复杂任务**：必须 `design.md` + `implement.md`，评审后再 `task.py start`。
- 能力边界需对用户诚实：无标签则无法均衡；MediaPlayer 不能把过静歌曲放大超过系统满幅。
