# 用组件库 0.0.7 组件替换宿主自建实现

## Goal

happier-ui 0.0.7 发布了 HPopup（七形态浮层 + swipe-to-close + useScrollLock）、HScrollbar、HTooltip 等新能力。梳理 Muses 中仍为宿主自建的实现，凡 0.0.7 已提供等价组件且迁移无回归风险的，迁移为库组件，减少宿主维护面并统一交互。

## Background（调研事实）

### Muses 现有自建实现清单

| 宿主实现 | 位置 | 0.0.7 等价能力 | 决策 |
|----------|------|----------------|------|
| QueuePage 全屏浮层（`fixed inset-0 z-[1200]` + App.vue Transition 包裹 + overlay.ts 状态） | `src/views/QueuePage.vue`、`src/App.vue`、`src/features/player/overlay.ts` | `HPopup position="fullscreen"`（inset:0、swipe-down 关闭、自带 transition、useScrollLock） | ✅ 迁移 |
| ExploreContainer（Ionic 遗留死代码） | `src/components/ExploreContainer.vue` | 无引用 | ✅ 删除 |
| App.vue body 滚动锁（`muses-overlay-open` class） | `src/App.vue:71-78`、`src/theme/tailwind.css:61-73` | `useScrollLock`（HPopup fullscreen 已内置 lockScroll） | ⚠️ 适配（PlayerPage 仍需宿主锁） |
| PlayerPage 全屏浮层 | `src/views/PlayerPage.vue` | 0.0.7 HPopup 无 keepAlive/手势开关 | ❌ 保留（用户决策 B，见下） |
| MContent 滚动容器 | `src/components/ui/MContent.vue` | `HScrollbar`（CSS-only thin 滚动条） | ❌ 本次不动（美化非必须） |
| MiniPlayer | `src/components/MiniPlayer.vue` | 无对应组件 | 保留 |
| action-sheet.ts 共享按钮样式 | `src/theme/action-sheet.ts` | 纯 Tailwind class 组合，非组件 | 保留 |

### 关键事实

- HPopup fullscreen：`z-index: var(--h-popup-z, 1200)` 与 QueuePage 现 z-[1200] 一致；panel `inset:0` 无圆角无 padding，正合 QueuePage 全屏需求。
- HPopup fullscreen 自带 swipe-down 关闭（scrollTop=0 时）→ QueuePage 可免费获得下滑关闭手势。
- HPopup 内部 `useScrollLock` 引用计数锁 documentElement（overflow:hidden + paddingRight 补丁），与 Muses 现有 class 锁语义不同：Muses 还锁 `.m-content` 与 pointer-events。
- QueuePage 打开时 PlayerPage 可能同时可见（queue 从 player 内打开），`hasGlobalOverlay` 仍由 PlayerPage 驱动，宿主锁需保留给 PlayerPage；QueuePage 的滚动锁由 HPopup 的 lockScroll 提供。
- `useScrollLock` 锁 `documentElement`（inline overflow），Muses 锁 `html+body`（class+!important）。两者可并存但须确认解锁不互相干扰。
- QueuePage 有虚拟列表（@tanstack/vue-virtual）、HNavBar、清空按钮，slot 内容可整体放入 HPopup panel。

### PlayerPage 迁移 HPopup 的两个硬冲突（已确认，用户决策保留）

1. **保活（#22）回归**：App.vue `keepPlayerPageMounted = playerOverlayVisible || playerState.currentSong`，PlayerPage 关闭时仅 CSS 隐藏（translate-y-full + invisible），不卸载，避免重开重建 BackgroundRender 闪默认底（#22）。但 HPopup 的 slot 是 `v-if="visible"`（modelValue=false 即卸载内容，无 keepAlive/v-show 选项）→ BackgroundRender 会被卸载 → #22 回归。
2. **手势冲突**：PlayerPage 有精细自建手势系统（横向切面板、纵向关闭含歌词面板内禁用 canStartVerticalDismiss、进度条手势隔离 seekGestureLocked、touch-action-none 全局拦截）。HPopup fullscreen 自带 swipe-down 关闭（用 scrollTop 判断，panel 为 touch-action: pan-y）→ 两套手势叠加会误判关闭。

**用户决策：PlayerPage 保留宿主实现（选项 B）。** 如需迁移，须先给组件库提 issue 增加 keepAlive + 手势开关（跨仓库，后续任务）。

### 用户价值

宿主浮层/滚动自建代码减少；Queue 获得库级下滑关闭与转场一致性；死代码清除。

## Requirements

- R1 QueuePage 迁移为 HPopup fullscreen（含下滑关闭、转场、锁滚动），不再手写 fixed 容器
- R2 迁移后 QueuePage 打开/关闭/清空/选歌/删除/返回行为不变
- R3 ExploreContainer 死代码删除
- R4 滚动锁行为在 PlayerPage + QueuePage 并存时正确（开 Queue 锁、关 Queue 解锁、开 Player 锁、相互切换无残留、无双重解锁破坏）
- R5 构建/类型/lint 通过
- R6 不修改组件库源码

## Out of Scope

- PlayerPage 迁移 HPopup（用户决策 B：保留宿主实现；保活 #22 + 手势冲突）
- MContent 叠加 HScrollbar 美化（本次不动）
- MiniPlayer / action-sheet.ts 改造
- 组件库增强（keepAlive / 手势开关）

## Acceptance Criteria

- [ ] AC1 QueuePage 用 HPopup fullscreen 渲染，不再手写 fixed 容器
- [ ] AC2 Queue 下滑可关闭；转场动画正常
- [ ] AC3 Queue 打开时 body 滚动锁生效；关闭后解锁；与 PlayerPage 锁切换无残留
- [ ] AC4 Queue 全功能（虚拟列表滚动/选歌/删除/清空/返回）正常
- [ ] AC5 ExploreContainer 删除后无引用错误
- [ ] AC6 lint/build 通过
- [ ] AC7 PlayerPage 保持宿主实现，保活（#22）与手势行为不回归

## Notes

- 技术设计进 design.md；执行计划进 implement.md。
