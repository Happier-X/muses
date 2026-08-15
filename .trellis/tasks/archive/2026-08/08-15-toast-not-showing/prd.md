# 修复 Toast 被底部播放条遮挡

## Goal

修复普通页面（设置页「检查更新」等）触发的 toast 被底部 MiniPlayer 播放条完全遮挡、不可见的问题。

## Background

- **现象**：设置页点「检查更新」（成功/失败均弹 toast），toast 显示在屏幕底部 16px 处，但被 MiniPlayer（固定底部 64px 高、z-1000）盖住，完全看不到。播放页内 toast 正常（popup z-1100 高于 MiniPlayer）。
- **根因（已定位）**：TabsPage 的 `.tabs-layout__track`（motion.div 推屏轨道）**恒有 `transform: translateX(-50vw)` + `will-change: transform`**，包裹 `<main><RouterView /></main>`：
  1. transform 使 track 成为页面内 `position: fixed` 元素的**包含块**（toast 相对 track 定位）；
  2. transform 使 track **创建层叠上下文**，且 `z-index: auto` ≈ 0；
  3. MiniPlayer 在根层叠上下文 z-1000 > 0 → 整个页面层叠上下文（含 z-1300 的 toast）都低于 MiniPlayer → toast 被盖住。
- **修复方向**：MToast 内容 `<Teleport to="body">` 渲染，脱离 track 层叠上下文与包含块；toast 回到根层叠上下文（z-1300 > MiniPlayer z-1000），fixed 相对视口定位。

## Requirements

- 普通页面（设置页/歌曲列表等 tab 页）的 toast 完整可见，不被 MiniPlayer 或导航抽屉轨道遮挡。
- 播放页（popup 内）toast 行为不回归：仍显示在视口底部中间，不受下滑拖拽 transform 影响。
- toast 的定位语义不变：视口底部居中（`bottom: calc(16px + safe-area)`），z 阶梯 1300 不变。
- 不改 MiniPlayer 的 z-index 与布局；不改页面组件与调用点。

## Acceptance Criteria

- [ ] 设置页点「检查更新」：toast（"已是最新版本"/"检查更新失败…"）完整显示在 MiniPlayer 之上，可读。
- [ ] 歌曲列表等 tab 页触发的 toast 同样可见。
- [ ] 播放页内 toast（编辑保存、翻译提示等）显示位置/行为不变。
- [ ] 侧栏抽屉打开/关闭状态下 toast 仍固定在视口底部中间（不随轨道位移）。
- [ ] `vue-tsc` / ESLint 通过；MuMu 模拟器实测截图确认 toast 可见。

## Notes

- 轻量任务，PRD-only。改动点：`src/components/ui/MToast.vue` 增加 `<Teleport to="body">` 包裹。
- 需同步更新 `.trellis/spec/frontend/component-guidelines.md` 弹层 z 阶梯段落，记录「页面内 fixed 浮层会落入 track 层叠上下文（z≈0）被 MiniPlayer 盖住」这一防坑经验。
