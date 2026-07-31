# 沉浸式播放器进度条样式调整

## Goal

沉浸式播放页（PlayerPage）的 HRange 进度条去掉圆球 knob，恢复旧版 ion-range 的 4px 轨道样式，仍复用 HRange 组件。

## Background

- 旧版用 `ion-range`，已由 commit `d1662e4` 隐藏 knob（`--knob-size: 0px`），4px 轨道，颜色使用 `--muses-immersive-track` 和 `--muses-immersive-ink`。
- 当前版本用 `happier-ui` 的 `HRange` 组件，默认有 20px 圆球 knob 和 6px 轨道。
- HRange 通过 CSS 变量（`--h-range-thumb`、`--h-range-track-h`、`--h-range-fill`、`--h-range-track-bg` 等）暴露了完整的自定义能力，**不需要向组件库提 issue**。

## Requirements

- R1 隐藏 knob：进度条圆球不可见
- R2 轨道高度 4px
- R3 填充色匹配沉浸式主题（`var(--muses-immersive-ink)`）
- R4 轨道底色匹配沉浸式主题（`var(--muses-immersive-track)`）
- R5 触摸热区保持 24px min-height，拖动交互不受影响
- R6 仍使用 HRange 组件，不改回 ion-range 或自建

## Out of Scope

- 修改 HRange 组件源码
- 重写进度条交互逻辑
- 向 happier-ui 提 issue

## Acceptance Criteria

- [ ] AC1 沉浸播放器进度条 knob 不可见
- [ ] AC2 轨道 4px 高，pill 圆角
- [ ] AC3 已播放段颜色 = `--muses-immersive-ink`，未播放段 = `--muses-immersive-track`
- [ ] AC4 仍然可拖动/点击 seek
- [ ] AC5 窄屏与宽屏断点下的现有 `.progress-range` touch-target 高度不变
- [ ] AC6 不修改 `node_modules`、不修改 `happier-ui` 仓库

## Notes

- 只需在 `src/theme/tailwind.css` 的 `.player-overlay .progress-range` 区域追加 CSS 变量和 thumb 覆盖即可。
