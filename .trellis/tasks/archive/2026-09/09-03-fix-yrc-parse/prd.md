# 修复 YRC 解析缺失：播放页加 YRC 分支并兼容无行头行

## Goal

《为何我》刮的是网易源、库里是 YRC，但播放页 `parseDocument` 根本没有 YRC 分支（TTML→KRC→QRC→LRC），且 KRC 行头 `^\[(\d+),(\d+)\]` 与 YRC 行头同形，YRC 被 KRC 误食后走"无 timing 纯文本"分支，脏文本原样显示。之前修 QRC 修错了地方。

## Background / Confirmed Facts

- 库内形态：无行头、每字一行 `(123270,370,0)浮`；`parseYrc` 230 行 `startsWith('[')` 直接跳过这类行。
- 链路：YRC → KRC 误食（行头同形，字 timing `(d,d,d)` vs KRC `<d,d,d>` 零匹配，content 原样返回，lines 非空）→ QRC 到不了 → 脏显示。
- `parseYrc` 本体后向归属与收尾是对的，只需加无行头分支 + 抽共用 `parseYrcSyllables`。

## Requirements

- R1 `LyricModels.parseYrc`：无行头但含 timing 的行也组行（首 timing 为行时间）；逐字切分抽 `parseYrcSyllables` 共用。
- R2 `LyricCompat.parseDocument`：TTML 之后、KRC 之前加 YRC 分支（`parseYrc` 非空即返回，quality=WordSynchronized）。
- R3 新增 `NeteaseYrcParserTest` 3 项（行头三元/无行头/纯文本不误食）。

## Acceptance Criteria

- [x] AC1 YRC 单测 3 项全过。
- [x] AC2 `:feature:player` 编译通过；`:core:lyrics`/`:core:scrape`/`:feature:scrape` 单测全过。
- [ ] AC3 用户模拟器确认《为何我》正常显示（装包待验证）。

## Out of Scope

- 批量页/编辑页写回丢 `lyricsFormat`（另起任务）。
