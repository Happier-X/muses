# 歌词向上逐级滚动弹簧修复

## Goal
修复 MuMu 上“歌曲不是向上自动滚动吗，没有逐级弹簧”——即 `NativeLyricsPanel` 的 `currentIndex` 驱动的 `LazyColumn` 未逐级向上弹簧滚动，使每行切换都有 Apple Music 的逐级弹簧回弹可见。

## Background
- **现状（已验证）**：
  - 已引入 `LookaheadScope + AppleSpringPlacement`（位置弹簧）与 `animateScrollToItem` 滚动，但在 MuMu 上用户反馈“没有弹簧”“歌曲不是向上自动滚动吗”
  - 初步排查：`NativeLyricsPanel` 的 `LaunchedEffect(currentIndex)` 内 `if(listState.isScrollInProgress) return` 可能在自动滚动进行中跳过后续 `currentIndex` 更新的滚动；`currentIndex` 由 `positionProvider 100ms` 轮询计算，但 `syncedLyrics` 可能为 null 或 `computeCurrentIndexNative` 对 `SyncedLine` 的 `start` 判定不准，导致 `currentIndex` 恒 0/-1，不触发滚动
  - 另 `animateScrollToItem` 默认 spring 过阻尼，手感不明显；需显式欠阻尼使逐级 40dp 位移也可见回弹
- **用户诉求**：明确“逐级的弹簧效果”，即每首歌播放时歌词逐行向上，级间带弹簧

## Requirements
- **R1 — 排查不滚动**：确认 `currentIndex` 是否随 `positionProvider` 正确递增（日志 `currentIndex`/`position`/`lines.size`），修复 `LaunchedEffect` 因 `isScrollInProgress` 误跳过导致的丢帧；确保 `syncedLyrics` 非空时 `currentIndex` 从 0 递增至末行
- **R2 — 逐级弹簧可见**：`animateScrollToItem(currentIndex, animationSpec=spring(stiffness=180f, dampingRatio=0.78f))` 显式欠阻尼（原默认过阻尼不可见），每级 40dp 位移带一次轻回弹；`AppleSpringPlacement` 保持 `170..220/0.85` 作为行位移补充
- **R3 — 居中逐级**：滚动目标保持“当前行居中”语义（`animateScrollToItem` 后 `scrollOffset` 居中或 `LazyColumn` `contentPadding 120dp` 已居中），确保向上逐级而非瞬跳顶部

## Acceptance Criteria
- [ ] AC1 — 播放时歌词逐行向上自动滚动，每级切换有可见弹簧回弹（非瞬切）
- [ ] AC2 — `currentIndex` 日志显示随播放递增，`isManualScrolling` 仅用户手势时 snap，自动逐级一律 spring
- [ ] AC3 — `assembleMusesDebug` / `lint` / `test` 通过，MuMu 手动验收通过

## Out of Scope
- 词级遮罩、放大等
- 背景

## Open Questions
（无）

## Decisions
- D1 — 将逐级滚动显式 `spring(180,0.78)` 使小位移也可见回弹，并修复 `isScrollInProgress` 误跳过（2026-09-02 定位）

## Notes
- 复测时用 `adb logcat -s NativeLyrics` 打印 `pos/index/lines` 确认递增
