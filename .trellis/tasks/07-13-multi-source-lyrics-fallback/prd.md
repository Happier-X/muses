# 多源歌词回退链（父任务）

## Goal

建立在线歌词串行回退，在本地 LRC 与现有 amll 之上提升命中率：

```
amll TTML → 平台歌词(kw→tx→wy→kg→mg) → LRCLIB LRC → 本地 LRC → 空态
```

- amll 成功即停  
- 匹配中可先显示本地 LRC  
- **所有在线歌词不写回** `SongItem` / `muses:songs`  
- 不阻塞播放；token 防串曲；失败静默  

## Decisions

| 决策 | 结论 |
|------|------|
| 总序 | amll → 平台 → LRCLIB → 本地 |
| 范围 | 五平台 + LRCLIB 全做 |
| 平台序 | kw → tx → wy → kg → mg |
| 写回 | 在线一律不写库 |
| 公开 LRC | LRCLIB |
| 任务结构 | **父 + 三子任务**（见下） |

## 子任务地图

| 子任务 | 交付 | 依赖（文档约定，非树位置） |
|--------|------|---------------------------|
| **lyrics-orchestrator** | 统一 `matchOnlineLyrics` 编排 + controller 接入；amll 为第一段；预留平台/LRCLIB provider 槽 | 无 |
| **lyrics-platform-providers** | kw/tx/wy/kg/mg 搜歌 + 取词，串行 | 编排契约（orchestrator） |
| **lyrics-lrclib** | LRCLIB provider + 挂入链尾在线段 | 编排契约 |

父任务负责：跨子验收、优先级、不写库、与封面/文本并行无冲突。

## Requirements（父级）

1. **R1** 在线优先级：amll TTML > 平台 LRC/逐字转 LRC > LRCLIB LRC > 本地 > 空。  
2. **R2** 任一段成功即停后续在线请求。  
3. **R3** 在线结果只进 `playerState`（`lyrics` + `lyricsFormat`），不 `upsertSong` 歌词。  
4. **R4** 切歌 token；过期丢弃。  
5. **R5** 平台/公开失败不影响 amll 已成功路径；全失败回退本地。  
6. **R6** spec 更新 features-player / state-management。  

## Acceptance Criteria（全链）

- [x] AC1：amll 命中为 TTML，不请求平台/LRCLIB。  
- [x] AC2：amll miss 后平台命中展示在线 LRC（format=lrc）。  
- [x] AC3：平台全 miss 后 LRCLIB 可命中。  
- [x] AC4：全 miss 保留本地 LRC 或空；`onlineLyricsStatus` 合理。  
- [x] AC5：不写 `muses:songs` 歌词字段。  
- [x] AC6：测试/lint/tsc/spec；子任务均完成。  

## Out of Scope

- 指纹取词、手选候选、扩展宿主、改 capgo、MusicBrainz 歌词  

## Task Type

Complex / Parent
