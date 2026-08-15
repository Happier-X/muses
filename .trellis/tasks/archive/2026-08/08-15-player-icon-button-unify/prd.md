# 播放页图标按钮统一改用 MIconButton

## Goal

沉浸式播放页的**纯图标按钮**（主控三键、mode-bar 四键、歌词页 FAB）从通用 `MButton`（clear 变体）改为专用 `MIconButton` 组件——语义分离：「按钮是按钮，图标按钮是图标按钮」，点击反馈统一由 MIconButton 自带涟漪承担。

## Background

- **现状**：播放页 9 个纯图标按钮用 `m-button variant="clear" rounded-full` + 尺寸/颜色 `!important` 覆盖；上一轮（08-15-salt-player-btn-ripple）在 PlayerPage scoped 里手写 `::after` 涟漪 + `:active` 背景覆盖。用户指出应使用图标按钮组件。
- **MIconButton 现状**：motion.button 圆形透明底、`currentColor` 半透明涟漪（active opacity 0.1 + scale 0.5→1）、`while-tap scale 0.88`、默认 40px / sm 36px；`color: inherit` 由调用方控制。
- **本次改动**：
  - `MIconButton` 新增 `size="lg"`（48×48，主控/浮动播放键用）；补上 sm 尺寸样式（注释已有但样式缺失）。
  - PlayerPage 9 个图标按钮换 `m-icon-button`（主控 3×lg、mode-bar 4×md、歌词 FAB 1×md + 1×lg）；颜色 class 去 `!important`（MIconButton `color: inherit` 直接生效）。
  - 移除 PlayerPage 手写 `::after` 涟漪块与 `:active` 背景覆盖（组件自带涟漪 = currentColor 白色，深色播放页上即白色半透明反馈）。
  - 编辑 sheet 的文字按钮（从云端获取/更换文本/应用到表单/选择图片等）保持 `m-button` 不变。

## Requirements

- 播放页纯图标按钮统一使用 `m-icon-button`，带正确 `aria-label`；主控 48px、mode-bar/FAB 40px（播放 FAB 48px）。
- 点击反馈 = MIconButton 自带白色涟漪 + 缩放（currentColor 跟随白色系按钮色）。
- 文字按钮继续用 `MButton`；不改动编辑 sheet、弹层等非播放页区域。
- 保留 disabled（loading）、`is-active`（翻译 FAB）、`tabindex` 透传等既有行为。

## Acceptance Criteria

- [ ] MuMu 播放页：主控 48×48、mode-bar 40×40 圆按钮，白色图标，点击出现白色半透明涟漪 + 轻微缩放。
- [ ] 歌词页 FAB 同样生效；loading 时播放键禁用无反馈。
- [ ] 编辑 sheet 文字按钮视觉/行为无回归。
- [ ] `vue-tsc` / ESLint / 构建通过。

## Notes

- 轻量任务，PRD-only。改动：`src/components/ui/MIconButton.vue`（+lg 尺寸、补 sm）+ `src/views/PlayerPage.vue`（模板换组件、样式清理）。
- 已实测（CDP）：主控 48×48 / mode 40×40 / 白色系 / 透明底 / 涟漪元素存在，强制 :active 涟漪 opacity 0.1 白色。
- 需同步更新 `.trellis/spec/frontend/component-guidelines.md` 的「沉浸页按下态」条目（改为 MIconButton 契约）。
