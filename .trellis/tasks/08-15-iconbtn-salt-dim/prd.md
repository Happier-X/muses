# MIconButton 点击效果改为椒盐式图标变暗

## Goal

图标按钮（MIconButton）的点击反馈从「白色半透明圆底涟漪 + 0.88 缩放」改为椒盐音乐实测的「**图标按下变暗**」效果——无背景圆底、无缩放。

## Background

- **用户反馈**：当前图标按钮点击效果（圆底涟漪 + 缩放）不喜欢，要椒盐的点击效果。
- **椒盐实测**（MuMu 12.2.0，screenrecord 24fps 录屏 + 长按驻留态逐帧像素分析）：
  - 上曲按钮长按期间：按钮区域**无圆形涟漪**、无背景变化、无缩放；
  - 仅**图标本身变暗**：RGB 255→230（约 0.9 alpha，亮度 -25 灰度）；
  - 变暗区域与图标形状完全重合（skip-back 三角+竖条）。
- **改动**（`src/components/ui/MIconButton.vue`）：
  - 模板移除 `while-tap`（motion 缩放）与 `__ripple` span；
  - 样式移除 `__ripple` 圆底（含 dark 覆盖）；`__icon` 增加 `transition: opacity 0.15s`；
  - `&:active &__icon { opacity: 0.85 }`（按下图标变暗，约 0.85≈椒盐 0.9，略强一点保证可感知）。

## Requirements

- 图标按钮按下时仅图标变暗（opacity 0.85），无背景圆、无缩放。
- 松开恢复 1.0（0.15s 过渡）；禁用态不变（opacity 0.4 + pointer-events none）。
- 组件全局生效（播放页/导航/列表页图标按钮统一）；不影响 MButton 文字按钮。

## Acceptance Criteria

- [ ] MuMu：任意图标按钮（播放页主控/导航汉堡/列表搜索等）按下图标变暗、松开恢复，无圆底无缩放。
- [ ] 与其他页面既有使用（MiniPlayer/MNavbar/SongsPage）无回归。
- [ ] `vue-tsc` / ESLint / 构建通过。

## Notes

- 轻量任务，PRD-only。改动仅 `src/components/ui/MIconButton.vue`。
- 已验证（CDP 强制 :active）：无 ripple 元素、图标 opacity 0.85、transition 0.15s、松开恢复 1.0。
- 需同步更新 `.trellis/spec/frontend/component-guidelines.md` 的 MIconButton 契约描述（原「涟漪 + 缩放」过时）。
