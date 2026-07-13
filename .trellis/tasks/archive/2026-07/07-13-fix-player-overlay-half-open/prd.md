# 修复沉浸式再打开只展开一半

## Goal

修复 Issue #25：沉浸式页面关闭后再打开，有时只打开一半，没有覆盖全屏。

## Root Cause（推断）

#22 后 `PlayerPage` 在有当前曲时保活，关闭态由 App 外层 `.app-player-page` transform 隐藏。若通过下滑关闭时内部 `.immersive-shell` 的 `dragOffsetY` 未及时归零，组件不会卸载，重新打开后外层归位但内层仍保留部分下移，导致看起来只展开一半。

## Requirements

1. 打开沉浸式时必须重置内部拖拽状态与 `dragOffsetY`。
2. 关闭沉浸式时也应清理拖拽状态，避免保活状态残留。
3. 不破坏下滑关闭、歌词页禁止下滑关闭、进度条 seek 保护等既有手势。
4. 增加回归单测：下滑关闭后再打开，shell transform 回到 `translateY(0px)`。

## Acceptance Criteria

- [ ] 下滑关闭再打开后沉浸式覆盖全屏。
- [ ] 单测覆盖半开回归。
- [ ] `vitest` / `vue-tsc` / lint 通过。

## Task Type

Lightweight
