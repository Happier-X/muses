# 歌词手势与关闭沉浸式手势隔离

## Goal

歌词面板内的上下滑动手势不再误触发沉浸式播放页的下滑关闭手势。

## Issue

- GitHub #13：`歌词手势滚动和关闭沉浸式播放页面的手势冲突了`

## Confirmed Facts

- `PlayerPage.vue` overlay 在根 `.player-overlay` 绑定 `@touchstart/move/end`，实现「下滑关闭」与「左右切换面板」。
- `canStartVerticalDismiss` 通过 `composedPath` 检测可滚动原生元素（`scrollHeight > clientHeight && scrollTop > 0`）来抑制下滑关闭。
- AMLL `LyricPlayer` 内部滚动基于 transform，**非原生 scroll**，无法被该检测识别；因此在歌词区域上下滑动会被 overlay 当作下滑关闭。
- `onTouchMove` 对非原生控件区域 `event.preventDefault()`，也会干扰用户在歌词区的手势意图。
- 横向切换面板的逻辑（`onTouchEnd` 中按 startX/endX）需要保留，方便在歌词页左滑切回控制页。

## Requirements

1. **R1** 在歌词面板（`.lyric-panel` / `.lyric-player`）内，上下滑动不再触发 overlay 下滑关闭（不更新 `dragOffsetY`、不 dismiss）。
2. **R2** 歌词面板内横向滑动仍可切换面板（保留 `activePanel` 横向切换语义）。
3. **R3** 控制页（`.info-panel`）下手势不变，仍可下滑关闭。
4. **R4** 进度条、歌词行点击等既有隔离不回归。
5. **R5** 同步 component-guidelines 手势隔离约定。
6. **R6** lint / type-check 通过；必要时补单测。

## Acceptance Criteria

- [ ] AC1：歌词区域内上下滑动不关闭 overlay、不产生下拉位移。
- [ ] AC2：歌词区域内横向滑动仍可切回控制页。
- [ ] AC3：控制页下滑关闭不回归。
- [ ] AC4：spec 已更新。

## Technical Notes

建议在 `canStartVerticalDismiss` 中检测触点是否位于 `.lyric-panel`（或 `.lyric-player`）内，是则返回 false；或在 `onTouchStart` / `onTouchMove` 中以同样判断抑制 `canDragDown` / `dragOffsetY`。保留 `onTouchEnd` 横向切换逻辑。

## Task Type

Lightweight
