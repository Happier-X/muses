# 歌词翻译管线对齐 AMLL

## Goal

收窄自定义歌词翻译处理：

1. **格式解析优先交给** `@applemusic-like-lyrics/lyric`（`parseLrc` / `parseYrc` / `parseQrc` / `parseTTML` 等）；
2. 业务侧只保留 **AMLL 不会做的适配**：独立 tlyric 挂载、plain 双语 LRC 主译合并、翻译开关；
3. 已由库填好 `translatedLyric` 的行不再被二次猜测颠倒。

## Background

- AMLL `lyric` 支持多格式解析，行结构含 `translatedLyric` / `romanLyric`。
- 实测：`parseLrc` 对「同时间戳双行」**不会**自动合成主行+副行，两行均为主词且 `translatedLyric=""`。
- 在线源常见形态是 **主词 LRC/YRC + 独立 tlyric**，或 **双行 plain LRC**，不是 LQE/TTML 内嵌翻译。
- 因此仍需少量适配；但不应自研整套 LRC 词法，也不应对「库已带翻译」的结果做多余合并。

## Requirements

### R1. 解析职责边界
- `PlayerPage`（或统一入口）继续按 `lyricsFormat` 调用 AMLL parse；禁止新增平行 LRC/YRC/QRC/TTML 解析器。
- `parseTimedLrcMap` 仅用于 **独立 translation LRC 文本 → 时间戳 map**（挂 tlyric），不替代 `parseLrc` 主词解析。

### R2. 自定义逻辑白名单（只保留这些）
| 能力 | 是否保留 | 说明 |
|------|----------|------|
| `attachTimedLyricsTranslation` | 是 | 把 `lyricsTranslation`（tlyric）挂到主行 `translatedLyric` |
| `mergeDuplicateTimestampTranslations` | 是（仅 plain 双行） | 同时间戳两行主词且库未填译时合并；**非 Han 优先主行**；`endTime=max` |
| `applyLyricTranslationVisibility` | 是 | 翻译开关 |
| 对已有 `translatedLyric` 再合并/改主行 | **否** | 库已填或 tlyric 已挂则跳过双行合并 |

### R3. 输入形态与管线
```
AMLL parse(主词) → attachTimed(tlyric?) → mergeDuplicate(仅双主行无译) → applyVisibility
```
- 主词来自库解析结果时：若某行已有非空 `translatedLyric`，`attach`/`merge` 不得覆盖主词语义或颠倒主译。
- TTML 等若解析后已带翻译：应直接可用，不依赖双行脚本猜测。

### R4. 文档与测试
- 更新 `features-player` / `component-guidelines`：写明「AMLL 负责格式；业务只做 tlyric 挂载 + 双行 LRC 主译 + 开关」。
- 单测覆盖：
  - 双行中文在前仍原文为主；
  - 已有 `translatedLyric` 不二次合并；
  - tlyric attach 仍工作；
  - 关翻译只清副行。
- 不扩展为「删除全部 merge」除非验收证明所有线上源都不需要双行合并。

### Out of Scope
- 升级 AMLL 大版本 / 改 `node_modules`。
- 引入 `parseLqe` 作为默认路径（除非后续有 LQE 源）。
- 发版（除非另行要求）。
- 重做在线匹配源优先级。

## Acceptance Criteria

- [ ] 代码与注释明确「解析归 AMLL、适配归 mergeTranslation」边界
- [ ] 无新增平行歌词格式解析器
- [ ] 已有 `translatedLyric` 不被双行合并破坏
- [ ] 双语 plain LRC + tlyric + 开关行为与近期修复一致（主行原文、关翻译保留原文、endTime max）
- [ ] 相关 unit 通过；lint / type-check 通过
- [ ] spec 已同步边界说明

## Notes

- 轻量偏中：以 PRD 驱动小重构；若改动仅注释/条件收紧 + 测试，可不写 design。
- 关联：刚合并的主副行修复（`mergeTranslation` 非 Han 优先）；本任务是**职责对齐与收窄**，不是推倒重来。
