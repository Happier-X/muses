# 歌词弹簧可见性调试与欠阻尼重调

## Goal
定位并修复 MuMu 上“没有弹簧效果”的可见性问题，将过阻尼 `1.1` 重调为欠阻尼 `0.82~0.88`，使 Apple Music 的弹性在真机上可感知。

## Background
- **现状（已验证）**：
  - 已引入 `LookaheadScope + AppleSpringPlacement(stiffness 170..220, damping 1.1 过阻尼)` 与行 `alpha/blur` 弹簧，但在 MuMu 上仍无体感
  - 初步排查：`NativeLyricsPanel` 将 `isManualScrolling = isUserScrolling || listState.isScrollInProgress` 传入 `appleSpringPlacement`，而 `listState.isScrollInProgress` 在 **自动滚动**（`animateScrollToItem`）期间也为 `true`，导致 `SpringPlacementModifier` 内 `if(isManualScrolling) snap()` 分支命中，自动滚动的位移被 `snap()` 瞬切，无弹簧
  - 另 `dampingRatio 1.1` 为过阻尼（无回弹），Apple Music 真机为欠阻尼 `0.78~0.88` 带 1 次轻微回弹，才有“弹簧”体感
- **用户诉求**：连续三次反馈“没有弹簧效果”“像苹果音乐一样”，要求可见弹性

## Requirements
- **R1 — 可见性调试**：修复 `isManualScrolling` 误判：仅当用户手势 `isUserScrolling==true` 时 `snap()`，自动滚动（`currentIndex` 驱动）时必须走 `spring`；`listState.isScrollInProgress` 不再直接作为 `isManualScrolling` 输入
- **R2 — 欠阻尼重调**：`AppleSpringPlacement` 的 `dampingRatio` 由 `1.1` 改为 `0.82~0.88`（`stiffness 180~220` 保持），`NativeKaraokeLine` 的 `alpha` 保持 `0.92`，`blur` 保持 `0.9`；确保 180ms 内带一次轻回弹可见
- **R3 — 验证可见性**：在 MuMu 上播放快节奏歌曲（字级密集）与慢歌（长句）分别验证：行切换时有可感知的弹性位移与回弹，非瞬切

## Acceptance Criteria
- [ ] AC1 — 自动切行时，列表位移有可见弹簧回弹（非瞬切），手势拖动后回中同样弹簧
- [ ] AC2 — 参数已重调为欠阻尼 `0.82~0.88`，`isManualScrolling` 仅用户手势时为 true
- [ ] AC3 — `assembleMusesDebug` / `lint` / `test` 通过，MuMu 手动验收“有弹簧”

## Out of Scope
- 词级遮罩、背景等
- 重新引入 scale 放大

## Open Questions
（无）

## Decisions
- D1 — 将 `isManualScrolling` 修正为仅 `isUserScrolling`，并重调为欠阻尼（2026-09-02 定位）

## Notes
- 复测时用 `adb shell dumpsys` 或 `withFrameNanos` 日志确认 `offsetAnimation.isIdle` 是否为 false（弹簧进行中）
