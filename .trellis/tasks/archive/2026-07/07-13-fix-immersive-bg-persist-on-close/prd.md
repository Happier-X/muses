# 关闭沉浸式时背景保持动态

## Goal

Issue #22：关闭沉浸式后/再打开时，不应先闪默认背景再出现动态背景；有播放曲目时动态背景应保持连续。

## Root Cause

`App.vue` 用 `v-if="playerOverlayVisible"` 挂载 `PlayerPage`，关闭即销毁 `BackgroundRender`，再打开需重建 → 默认底闪现。

## Decisions

| 决策 | 结论 |
|------|------|
| 保活 | 有 `currentSong` 时保持 PlayerPage 挂载；仅用 CSS 隐藏关闭态 |
| 无曲 | 无当前曲时可卸载以省资源 |

## Requirements

1. **R1** 有当前曲时关闭沉浸式不销毁 AMLL 背景层。
2. **R2** 再打开无「默认→动态」闪烁（背景已在）。
3. **R3** 关闭态不可交互；返回键/下滑关闭逻辑不变。
4. **R4** 单测/现有 overlay 行为不回归。

## Task Type

Lightweight
