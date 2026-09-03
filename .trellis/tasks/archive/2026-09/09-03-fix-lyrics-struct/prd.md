# 修复歌词五源搜索解析错位

## Goal

审核页歌词候选只剩 LRCLIB 一个来源。根因：kw/tx/kg/wy 四源搜索接口陆续把外层 `{key: {key: [...]}}` 改版为 `{key: [...]}` 直接数组，老双层解析全返回空。加 `arrCompat` 双层/单层兼容解析。

## Background / Confirmed Facts

- 真实网络探测：四源搜索接口正常返回，取词接口正常，唯解析错位导致全 null。
- wy：`result.songs.songs` → `result.songs`（已验证）。
- kw：顶层 `abslist` 直接数组；取词 `data.lrclist` 同理兼容。
- tx：`data.song.list` 直接数组。
- kg：`data.lists` 直接数组；`candidates` 同理兼容。
- mg：搜索接口返回 HTML（反爬/下线），非解析问题，本任务不动。
- LRCLIB 中文覆盖弱，中文歌歌词基本不可用，故国内源必须修。

## Requirements

- R1 `WyLyricsProvider.kt` 新增 `internal fun JsonObject.arrCompat(key)`（双层/单层兼容）。
- R2 kw/tx/kg/wy 四源搜索解析 + kw 取词 `lrclist` + kg `candidates` 切到 `arrCompat`。

## Acceptance Criteria

- [x] AC1 真实网络验证：中文歌（七里香/周杰伦）kw/tx/kg 取词 OK；英文歌（2002/Anne-Marie）wy 取词 OK（YRC）。
- [x] AC2 `:core:lyrics:testDebugUnitTest` + `:core:scrape:testDebugUnitTest` 全过。
- [ ] AC3 咪咕源修复（接口挂）——独立任务。

## Out of Scope

- 咪咕源修复。
- pickBest 翻唱排序问题（中文歌搜出翻唱排第一，如七里香搜出 Montagem 翻唱无词）——另起任务（可加时长/热度辅助排序）。
