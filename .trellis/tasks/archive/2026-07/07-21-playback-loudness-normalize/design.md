# 设计：ReplayGain 轻量响度均衡

## 目标与边界

| 做 | 不做 |
|----|------|
| 读 track RG（及易取 R128）写库 | 全文件 R128 测量 |
| 播放时 `NativeAudio` volume 衰减/有限提升 | 动态压缩器 / AGC |
| 设置开关 + player-config 持久化 | 每曲手动增益 UI |

## 能力探测（现状）

1. **capgo** `NativeAudio.preload` / `play` / `setVolume`：`volume` 约 **0.1–1.0**（文档）。业务 `native.ts` 未传 volume。
2. **jaudiotagger 3.x**：`FieldKey.REPLAYGAIN_TRACK_GAIN` 等；项目已依赖。
3. **player-config**：`muses:player-config` 现有 `repeatMode` / `shuffleEnabled`，可扩展 `loudnessNormalizeEnabled: boolean`（默认 `true`）。

## 数据流

```
扫描/懒读标签 (AudioMetadataReader)
  → replayGainTrackDb?: number
  → upsertSong → muses:songs

playSong
  → 若开关开且 currentSong.replayGainTrackDb 有限数
       volume = clampLinear(dbToLinear(db))
     否则 volume = 1.0
  → AudioPlayerNative.play → preload/play 后 setVolume({ assetId, volume })
  → 切歌/stop：按新曲重算或 1.0
```

## dB → 线性

```ts
// ReplayGain dB → 振幅倍率（相对 1.0）
linear = 10 ** (db / 20)
// 插件约束
volume = clamp(linear, 0.1, 1.0)
```

- 典型 RG 在 −15…+5 dB：负值可衰减响曲；正值多数被 1.0 截断（**无法超过满幅放大**）。
- 解析：字符串如 `"-6.54 dB"` / `"-6.54"` → float；非法丢弃。

## 标签读取（Kotlin）

在 `AudioMetadataReader.readFromFile` 的 jaudiotagger 分支：

1. 尝试 `FieldKey.REPLAYGAIN_TRACK_GAIN`（若枚举可用）
2. 回退扫描 TXXX / 通用字段别名：`REPLAYGAIN_TRACK_GAIN`、`replaygain_track_gain`
3. 可选：`FieldKey.REPLAYGAIN_TRACK_PEAK` 本 MVP **不强制**做 peak 防削波（volume≤1 已限制）
4. 结果 `put("replayGainTrackDb", double)` 仅当解析成功

WebDAV 完整缓存 / content 临时文件路径与现有 read 一致。

## 前端类型与存储

`SongItem` 增加可选：

```ts
replayGainTrackDb?: number
```

`sanitizeSongForStorage`：仅持久化有限 number；非法剔除。

## 播放集成点

**推荐**：`native.ts` `AudioPlayerNative.play` 接受可选 `volume?: number`（默认 1），在 `NativeAudio.play` 成功后（或 preload 时带 volume）调用 `NativeAudio.setVolume`。

`controller.playSong` / `buildPlayOptions`：根据 `loadConfig().loudnessNormalizeEnabled` 与 song 字段计算 volume。

开关切换：export `setLoudnessNormalizeEnabled`；若有 `currentAssetId` 且 playing/paused，对当前曲重算 setVolume。

## 设置 UI

`SettingsPage.vue`：`ion-item` + `ion-toggle`「音量均衡」+ 简短说明：

> 根据歌曲自带的 ReplayGain 等标签调整播放音量。无标签的歌曲不会改变。

## 风险

| 风险 | 缓解 |
|------|------|
| 插件 volume 下限 0.1 | clamp；极响曲仍可能偏响 |
| 无法放大过静曲 | PRD/设置文案说明 |
| 读标签增耗时 | 仅在现有 readTags 路径；不单独扫全库 |
| 默认开导致「变小」投诉 | 说明是均衡；可关 |

## 测试

- 纯函数：`dbToPlaybackVolume(db | null, enabled)`
- 可选 mock native setVolume 调用次数
- 不强制 APK 内嵌样例文件

## 回滚

去掉 volume 传参与设置项；库内多字段可忽略。
