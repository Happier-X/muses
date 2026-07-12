# 平板沉浸式封面保持正方形

## Goal

修复平板上沉浸式播放页封面被拉伸成非正方形。

## Issue

- GitHub #12：`平板上沉浸式播放页面的封面不是正方形的，有拉伸`

## Confirmed Facts

- `PlayerPage.vue` `.cover` 默认 `aspect-ratio: 1; height: auto; object-fit: cover; width: min(72vw, 100%, 340px)`。
- 宽屏 `@media (min-width: 768px)` 覆盖 `.cover { width: min(40vw, 320px) }`，但 `.cover-slot` 限制了 `max-height: min(48dvh, 320px)`。
- 当 `40vw` 算出的高度（正方形）超过 `48dvh`（宽屏矮高场景），`max-height` 把封面高度 clamp，width 不变 → 非正方形拉伸。

## Requirements

1. **R1** 宽屏 `.cover` 在所有视口比例下都保持正方形（不拉伸）。
2. **R2** 不超过 `.cover-slot` 的 `max-height`。
3. **R3** 窄屏行为不变。
4. **R4** 同步 component-guidelines（封面正方形约束）。
5. **R5** lint / type-check 通过。

## Acceptance Criteria

- [ ] AC1：宽屏封面始终 1:1。
- [ ] AC2：窄屏无回归。
- [ ] AC3：spec 已更新。

## Technical Notes

修复思路：宽屏 `.cover` 的 `width` 同时受 vw 与 dvh 约束，例如：
```css
.cover {
  width: min(40vw, 48dvh, 320px);
}
```
这样 width = height（aspect-ratio 1）≤ max-height，不会触发 clamp 拉伸。

## Task Type

Lightweight
