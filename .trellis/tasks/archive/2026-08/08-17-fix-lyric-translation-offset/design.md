# 技术设计：修复翻译行错位一行

## 现状与缺陷点

管线（PlayerPage.vue `lyricLines` computed）：

```
parseLrc/parseYrc/parseTTML → attachTimedLyricsTranslation(tlyric) → mergeDuplicateTimestampTranslations → UI
```

`mergeDuplicateTimestampTranslations`（mergeTranslation.ts）逐行扫描，
对 `lines[i]` 与 `lines[i+1]` 判定「同时间（≤50ms）且脚本体系不同」即合并。
该策略只覆盖「译文与原文**同时间戳**成对」的形态；当译文时间戳等于**下一句**
原文时（常见于部分双语 LRC / 部分平台数据），配对整体后移一行，末尾译文落单。

## 修复方案：交替结构感知配对（保持单文件、纯函数）

在 `mergeDuplicateTimestampTranslations` 内，合并前先做一次**整歌结构判定**：

1. 扫描行序列，统计「相邻脚本不同的行对」（candidate 对），不要求同时间戳；
2. 若全歌呈现稳定的原文/译文**交替**结构（即原文行 A 后紧跟译文行 C，
   译文的 startTime ≥ A.startTime 且 ≤ 下一句原文行 startTime），则按**文件顺序**
   将译文行并入其**前一行**原文（主行选择沿用 `pickMainAndTranslation`：
   非 Han 优先主行）；
3. 交替结构不成立（例如只有零星脚本不同的相邻行、或大多数原文行后面跟的还是原文行）
   时，退回现有「同时间戳相邻配对」逻辑，保持既有保护（不吞同时间独立两句）。

### 判定细则（防误合并）

- 交替判定要求覆盖率达标：脚本不同的相邻对占「非空行」比例 ≥ 阈值（如 60%），
  且这些对中「前原文后译文」的顺序一致率足够高；
- 单行纯标点/空文本行不参与判定；
- 已有 `translatedLyric`（tlyric 已挂）的行直接跳过（现状行为不变）；
- 合并后 `startTime = min(两行)`、`endTime = max(两行)`（沿用现状）。

### 与 tlyric 挂载的顺序关系

`attachTimedLyricsTranslation` 先行：tlyric 挂上的行不再参与双行合并
（现状已保证）。交替合并只处理「主词内嵌双语」。

## yrc+tlyric 容差问题（附加项）

现象：yrc 行时间与 tlyric 偏差 300~700ms，80ms 容差导致翻译挂不上。
方案：`nearestTranslation` 匹配失败时增加**序列感知回退**——按主行顺序对
tlyric 时间戳排序后做顺序对齐（类似 LCS/双指针），要求时间偏差在宽容差内
（如 ≤ 2000ms）且不改变相对顺序。仅在该曲 tlyric 时间戳与主行时间戳
整体存在系统性偏移时启用（避免破坏对齐良好的数据）。

若实现复杂度超预期，此项可降级为单独待办任务，不影响主修复验收。

## 兼容性

- 纯函数改动，输入输出均为 `LyricLine[]`，无 UI / 状态层变更；
- PlayerPage 无需改动（管线入口不变）；
- 离线复现脚本（node_modules/.tmp-mergeTest 下的用例）转成可回归的用例集。

## 回滚

单文件改动，revert 即可。
