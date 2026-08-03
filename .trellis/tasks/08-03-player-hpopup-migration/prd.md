# PlayerPage 迁移 HPopup fullscreen（keepAlive 保活 + 手势接管）

## Goal

将 PlayerPage（沉浸式播放器）的宿主自建 overlay 外壳迁移到 `HPopup position="fullscreen"`（happier-ui 0.0.8，含 keepAlive + swipeClose），复用库的保活、滚动锁、转场；PlayerPage 内部 200+ 行自建手势（横向切面板、纵向下滑关闭、进度条 seek 隔离）原样保留。

## Background

- 当前 PlayerPage 由 App.vue `v-if="keepPlayerPageMounted"` + `transform translate-y` + `contain` 保活隐藏（#22：关再开不重建 AMLL BackgroundRender，防闪默认底）。
- PlayerPage 824 行，含复杂自建手势：根 div `touch-action-none`、`onTouchStart/Move/End`、横向切面板（activePanel 0/1）、纵向下滑关闭（`canStartVerticalDismiss` + 歌词面板豁免）、进度条 seek 隔离（`seekGestureLocked` + 进度条 ref 识别）。
- 组件库 0.0.7 无 keepAlive/swipeClose，曾阻碍迁移（issue #13）；**0.0.8 已实现**：
  - `keepAlive`：关闭时 slot 用 `v-show` 隐藏保活（不卸载内容）
  - `swipeClose`（默认 true）：false 时禁用内置下滑手势，`touch-action:auto` 交还宿主
- Muses 当前依赖 `happier-ui@0.0.7`，需升级 0.0.8（精确版本，不用 `^`；不用 file: 链接）。
- HPopup `useScrollLock` 锁 `document.documentElement`（inline overflow + padding-right 补偿）；宿主 `html/body.muses-overlay-open` class 锁（!important overflow + `.m-content{pointer-events:none}`）。两套独立机制，需并存。
- Muses 已有 `.h-popup--position-fullscreen .h-popup__body { height: 100% }` 补丁（QueuePage 迁移时加的），对全屏 HPopup 通用生效。
- HPopup fullscreen 默认 `z-index: var(--h-popup-z, 1200)`；PlayerPage 需 1100（低于 QueuePage 1200）。
- HPopup fullscreen 转场 `h-popup-fullscreen-in/out`（330ms ease），当前 App.vue 用 `translate-y`（220ms ease）——转场观感变化已获批（用户选项 A）。

## Requirements

- R1 PlayerPage 用 `<HPopup position="fullscreen">`，App.vue **常驻挂载** `<PlayerPage />`（无 v-if/Transition），`v-model="playerOverlayVisible"`
- R2 `keep-alive` 开启：关闭时 slot `v-show` 隐藏不卸载，保留 AMLL BackgroundRender（替代 `keepPlayerPageMounted` + translate-y 保活）
- R3 `:swipe-close="false"`：禁用 HPopup 内置下滑手势，`touch-action` 交还，PlayerPage 自建手势完全接管
- R4 PlayerPage 内部手势（横向切面板/纵向关闭/进度条 seek）逻辑原样保留，行为不回归
- R5 升级 `happier-ui@0.0.7→0.0.8`（精确版本锁定）
- R6 `--h-popup-z` 覆盖为 `var(--muses-z-player)`（1100），保持 PlayerPage 低于 QueuePage（1200）层叠
- R7 滚动锁：HPopup lockScroll（documentElement inline overflow + padding 补偿）与宿主 `muses-overlay-open` class 锁（html/body `overflow:hidden !important` + `.m-content{pointer-events:none}`）两套并存；关闭后无残留锁、无双重解锁冲突
- R8 overlay `modelValue` 由 `playerOverlayVisible` ref 驱动；PlayerPage 自建纵向关闭手势仍调用 `closePlayerOverlay()`

## Out of Scope

- 不改组件库源码
- 不重写 PlayerPage 自建手势，不删 200+ 行手势
- 不动 QueuePage（已迁 HPopup fullscreen）
- 不动 MiniPlayer / MContent / MPage / action-sheet
- 不改 `overlay.ts`（`playerOverlayVisible` 仍由宿主打开/关闭）

## Acceptance Criteria

- [ ] AC1 PlayerPage 仍是全屏浮层：HPopup fullscreen `inset:0`、无圆角、无 safe-area 内边距；打开/关闭行为与现状一致
- [ ] AC2 关闭再打开不重建 AMLL 背景（keep-alive 生效，无闪默认底）；无 `currentSong` 时 BackgroundRender 不渲染
- [ ] AC3 纵向下滑关闭、横向切面板、进度条 seek 与现状一致（自建手势未被 HPopup 干扰）
- [ ] AC4 打开时 html/body 滚动锁定（双锁并存）；关闭后无残留锁（inline 与 class 两套归零）
- [ ] AC5 回归：PlayerPage→QueuePage 层叠（1100<1200）、系统返回、空态、无回归
- [ ] AC6 `npm run build` + `npm run lint` 通过；`node_modules/happier-ui` version 0.0.8
- [ ] AC7 PlayerPage 常驻后无 `currentSong` 时 HPopup 保持隐藏（不弹浮层）

## Notes / Known Decisions

- **架构（选项 A）**：完整迁移，PlayerPage 组件根由 HPopup 提供；App.vue 常驻挂载。
- **锁**：`muses-overlay-open` class 锁不删（PlayerPage 与 QueuePage 共用背景禁点 `.m-content`）；HPopup lockScroll 与之独立计数。
- **状态栏/返回**：`syncPlayerStatusBar`（App.vue）与 `backButton` 顺序（queue→player→minimize）保持不变。
- 需要 `git rm`? 否。不动 `overlay.ts`。
- Cover: `h-popup` import 经 `@/components/ui` 统一导出（已含 HPopup）。