# 复刻椒盐播放页图标按钮点击效果

## Goal

沉浸式播放页（PlayerPage）图标按钮（主控三键、mode-bar 四键、歌词页 FAB）的按下反馈从当前 MButton clear 变体的**主色蓝 15% 底**改为椒盐音乐同款：**白色半透明涟漪**（点击瞬间从中心扩散 + 按住保持浅白底）。

## Background

- **现状**：MButton `--clear` 变体 `:active { background-color: rgba(var(--m-primary-rgb), 0.15) }`——蓝色半透明底。在播放页深色背景上呈"发蓝"效果，与设计契约（spec 历史记载"按下态由半透明白底承担"）不符，用户反馈"不是那种半透明的效果"。
- **椒盐实测**（MuMu 12.2.0 + SaltUI `Button.kt` 源码）：椒盐按钮按下反馈 = Compose `clickable` + `LocalIndication`（Material **ripple 涟漪**），按压处白色半透明圆扩散；按钮本身颜色不随 pressed 变化。
- **修复**：PlayerPage scoped SCSS 覆盖播放页图标按钮——`:active` 背景改 `rgba(255,255,255,0.18)`（!important 压过 MButton 蓝底），并新增 `::after` 白色径向渐变涟漪动画（0.42s 从中心 scale 0→1.6 扩散淡出）。MButton 根元素已有 `position: relative; overflow: hidden`，涟漪可被按钮边界裁剪。

## Requirements

- 播放页主控三键（side-btn/play-btn）、mode-bar 四键、歌词页 FAB（lyric-fab/lyric-play-fab）按下时出现**白色半透明反馈**（点击瞬间涟漪扩散 + 按住浅白底）。
- 涟漪为白色系（深色播放页背景上清晰），不改变按钮其余视觉（尺寸/图标/文字色）与事件逻辑。
- 仅作用于播放页图标按钮，不影响 MButton clear 在其他页面的既有行为。
- 保留现有 `:disabled`、`.is-active`（翻译 FAB 开态）语义。

## Acceptance Criteria

- [ ] MuMu 模拟器播放页：按住任一图标按钮出现白色半透明圆底，松开消失；点击瞬间有涟漪扩散感。
- [ ] 主控/模式/歌词 FAB 按钮效果一致；禁用态（loading 时播放键）无反馈。
- [ ] 其他页面（设置页等）clear 按钮行为不变（仍为主色 15% 底）。
- [ ] `vue-tsc` / ESLint / 构建通过。

## Notes

- 轻量任务，PRD-only。改动仅 `src/views/PlayerPage.vue` scoped SCSS。
- 已验证（CDP 强制 :active）：按钮计算背景 `rgba(255,255,255,0.18)`，按钮区域亮度较背景 +13。
- 需同步更新 `.trellis/spec/frontend/component-guidelines.md` 中过时的"按下态由 Konsta active 反馈承担"描述。
