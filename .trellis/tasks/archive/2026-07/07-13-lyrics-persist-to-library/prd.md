# 在线歌词写回曲库

## Goal

Issue #19：在线匹配到的歌词像封面/歌手专辑一样写入曲库（`muses:songs`），下次播放可离线展示，并支持按质量升级。

## Issue

- GitHub #19：`获取到的歌词也可以像其它媒体信息一样，存到数据库里面`

## Confirmed Facts（代码可证）

- `SongItem`：`lyrics?: string`、`lyricsSource?: 'embedded' | 'sidecar'`（尚无 online）。
- 无持久化 `lyricsFormat`；仅在 `playerState.lyricsFormat`。
- 在线匹配只写 `playerState`；spec 现禁止写回 `SongItem`。
- 封面/文本元信息播放时 `upsertSong` 写回。
- storage 校验 `lyricsSource` 白名单（需扩展）。

## Decisions

| 决策 | 结论 |
|------|------|
| 写回策略 | **C：按质量** — 无词写回；TTML/yrc/qrc 可覆盖库内 LRC；同级/更差不覆盖 |
| 质量序 | `ttml` ≈ `yrc`/`qrc` > `lrc` > 空；同级不覆盖 |
| 持久化字段 | **A** — `lyricsSource` 含 `'online'`；`lyricsFormat?: 'lrc'\|'ttml'\|'yrc'\|'qrc'` |
| 再匹配 | **A** — 有词也跑在线匹配；写库仍按质量规则 |

## Requirements

1. **R1** 扩展 `LyricsSource`：`'embedded' \| 'sidecar' \| 'online'`；`SongItem`/`AudioTags` 增加可选 `lyricsFormat`。
2. **R2** storage 校验/合并 `lyricsFormat` 与新 source。
3. **R3** 在线命中后：更新 `playerState`；若质量优于库内则 `upsertSong({ lyrics, lyricsSource: 'online', lyricsFormat })`。
4. **R4** 播放初始化：用库内 `lyrics` + `lyricsFormat`（缺 format 时：online 未知按 lrc 启发式或默认 `lrc`）。
5. **R5** 有库内词仍异步 `matchOnlineLyrics`；token 防串曲；不阻塞播放。
6. **R6** 更新 features-player / state-management：删除「在线歌词禁止写库」，改为质量写回规则。
7. **R7** 单测：写回/不覆盖/覆盖升级/展示 format；lint/tsc。

## Acceptance Criteria

- [ ] AC1：无词歌曲在线命中后 `muses:songs` 有 `lyrics` + `lyricsSource=online` + `lyricsFormat`。
- [ ] AC2：库内 LRC 被在线 TTML/yrc/qrc 覆盖写回。
- [ ] AC3：库内已是 yrc/qrc/ttml 时，在线 LRC 不写回覆盖。
- [ ] AC4：再次播放可从库展示正确 format（无需等网）。
- [ ] AC5：spec + 测试通过。

## Out of Scope

- 手选候选、写回音频内嵌、改 capgo、负缓存长期跳过（可后续）

## Task Type

Complex
