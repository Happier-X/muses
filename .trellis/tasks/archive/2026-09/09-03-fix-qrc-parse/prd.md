# 修复 QRC 逐字歌词解析：兼容三元 timing 与行尾字

## Goal

《为何我》的 QRC 歌词在播放页显示为 `(42320,230,0)虽` 这类原始时间轴脏文本。根因在 `QQMusicQrcLyricsParser`：wordTiming 正则只认二元 `(start,dur)`，真实 QRC 三元 `(start,dur,0)` 零匹配，syllables 为空后 text 回退到带标签原文并原样渲染；且即使匹配，末字也因循环收尾缺失而丢失。

## Background / Confirmed Facts

- 真实 QRC 形态：`[行头](timing)字(timing)字…`，字在 timing 之后；timing 三元。
- 脏文本链路：`parseQrcSyllables` 空 → `text = content.trim()`（165 行）→ `LyricCompat.toAmllLyricLine` 把整行包成一个词 → 渲染原样画出；`parse()` 的 LRC fallback 因 primaryLines 非空永不触发。
- 同仓 YRC 解析器三元是对的（`LyricModels.kt:148`），QRC 独有遗漏；`QrcDecoderTest` 真实向量本身就是三元，但 parser 层零单测。

## Requirements

- R1 `wordTiming` 兼容三元：`\((\d+),(\d+)(?:,(\d+))?\)`。
- R2 `parseQrcSyllables` 改后向归属（timing 之后到下一 timing 之前的文本归该 timing），末 timing 收到行尾。
- R3 新增 `QQMusicQrcLyricsParserTest` 4 项（截图真实形态/单 timing/二元兼容/多行）。

## Acceptance Criteria

- [x] AC1 旧代码下 4 项全失败（stash 验证），新代码下全过。
- [x] AC2 `:core:lyrics` + `:core:scrape` + `:feature:scrape` 单测全过。
- [x] AC3 已入库的 QRC 无需重刮（解析实时嗅探，修完直接正常显示）——待用户模拟器确认。

## Out of Scope

- 批量页/编辑页写回丢 `lyricsFormat`（旁路小 bug，另起任务）。
- 翻唱排序、咪咕源（已有任务 PRD 记录）。
