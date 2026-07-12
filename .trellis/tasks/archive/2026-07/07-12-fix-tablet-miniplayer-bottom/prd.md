# 平板底部播放条贴底

## Goal

修复平板布局下 MiniPlayer 底栏悬空，使其贴在屏幕最底部。

## Issue

- GitHub #10：`平板上，底部播放条没有在最底部，悬空了`

## Confirmed Facts

- `MiniPlayer.vue` 使用 `bottom: calc(64px + var(--ion-safe-area-bottom, 0px))`，为窄屏底部导航栏留位。
- 平板（`≥768px`）`TabsPage` 隐藏 `.mobile-tab-bar`，改用左侧 sidebar；底栏下方无导航占位。
- 因此平板上 MiniPlayer 仍抬高约 64px，表现为「悬空」。

## Requirements

1. **R1** 宽屏（`min-width: 768px`）下 MiniPlayer `bottom` 贴底（`0` 或仅 safe-area-bottom）。
2. **R2** 窄屏保持现状：仍在 tab bar 上方。
3. **R3** 不破坏 overlay 打开时的交互约定（`is-overlay-active` 等）。
4. **R4** 同步 component-guidelines 中 MiniPlayer 平板定位约定。
5. **R5** lint / type-check 通过；必要时补单测或视觉说明。

## Acceptance Criteria

- [x] AC1：平板上 MiniPlayer 贴屏幕底部，无 64px 悬空。
- [x] AC2：窄屏 MiniPlayer 仍在底部导航上方。
- [x] AC3：spec 已更新。

## Task Type

Lightweight
