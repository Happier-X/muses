# Component Guidelines

> How components are built in this project.

---

## Overview

Components in this repository are Vue single-file components using the Composition API via `<script setup lang="ts">`. 应用**已完全脱离 Ionic 框架**：路由用 `vue-router`（`createRouter` + `createWebHistory`），页面骨架用自建 `MPage` / `MContent`，UI 全部基于 `happier-ui` + Vue 原生能力。Capacitor 原生壳保持不变。

Reference files:

- `src/App.vue`（自建 app shell + `<RouterView>`）
- `src/components/ui/MPage.vue` / `src/components/ui/MContent.vue`（自建页面骨架）
- `src/views/TabsPage.vue`

---

## Component Structure

Use the standard Vue SFC layout already present in the repo:

1. `<template>` first
2. `<script setup lang="ts">` second
3. All styling uses Tailwind CSS v4 utility classes via `class`/`:class`. 禁止组件 `<style scoped>` 块；所有样式必须通过 Tailwind utility class 表达（含任意值 `[...]` 语法与 `dark:` / `md:` 等变体）。
   - 对于复杂的多层级响应式布局或 `:deep()` 内部 DOM 选择器，可移至全局入口 `src/theme/tailwind.css` 作为作用域手写 CSS。
   - JS 运行时计算的 `:style` 动态绑定（如 `MCover` 的 `--m-cover-size`）允许保留，不属于组件 scoped CSS。

Examples:

- `src/App.vue` shows a minimal shell component: `<div class="app-shell">` + `<RouterView>` + MiniPlayer/PlayerPage/QueuePage 兄弟层级。
- `src/components/ui/MPage.vue` 用 `HNavBar` + `MContent` 组成自建页面骨架，样式全部为 Tailwind utility。
- `src/views/SettingsPage.vue` 使用 `MPage` 原生列表结构，所有 class 使用 Tailwind utility。

---

## Muses 语义组件层 → `happier-ui`

Muses 默认从 npm 使用**精确版本** **`happier-ui@0.0.9`**（不用 `^`），不得提交 `file:../happier-ui`，也不得配置指向相邻仓库源码的 Vite/TypeScript alias。应用通过 `src/components/ui` re-export 库真实导出与 app-only 组件。

- **权威 token**：包内 `happier-ui/tokens.css` 的 **`--h-*`**；`--muses-*` 为兼容别名。0.0.5 起 tokens.css 已彻底去除 `var(--ion-*)` 反向依赖，改为自持有值，并内建双触发独立暗色态（`@media(prefers-color-scheme: dark) :root:not(.light)` 系统跟随 + `:root.dark`/`.dark` 手动强制）。暗色由 happier-ui 承接，**Muses 侧无需任何暗色 workaround**。
- **样式管道（必需）**：宿主必须接入 **Tailwind CSS v4**（`tailwindcss` + `@tailwindcss/vite`）。全局入口 `src/theme/tailwind.css` 使用 `@import 'tailwindcss'` + `@import 'happier-ui/styles'`，由 Vite 的 `tailwindcss()` 插件解析 `@theme` / `@layer components`。**禁止**再直接 `import 'happier-ui/style.css'` 或把 `styles.css` 当普通预编译 CSS 跳过 Tailwind 管道。
- **边界**：真实库导出为 `HBadge`、`HBottomSheet`、`HButton`、`HCard`、`HCell`、`HCellGroup`、`HCheckbox`、`HDialog`、`HEmpty`、`HFloatingBubble`、`HIcon`、`HImage`、`HInput`、`HNavBar`、`HPagination`、`HProgress`、`HRange`、`HSelect`、`HSidebar`、`HSwitch`、`HTable`、`HTabBar`、`HTag`、`HTextarea`、`HToast`；类型含 `HSelectOption`、`HSidebarItem`、`HTableColumn`、`HTableSort`、`HTabBarItem`、`HFloatingBubbleOffset`、`HFloatingBubbleAxis`、`HFloatingBubbleMagnetic`、`HFloatingBubbleGap`；app-only 为 `MCover`（音乐封面）、`MPage`（自建页壳）与 `MContent`（自建滚动容器）。
- **`HIconButton` 已在 0.0.4 移除**，合并进 `HButton`（`is-icon-only` + `shape='square'|'circle'`）。图标按钮统一用 `<h-button is-icon-only shape=... variant=...>`，不得再引用 `HIconButton`。
- **首版视觉**：HeroUI Native 角色与触控节奏；**不**引入 `heroui-native` / RN。
- **禁止**：`MIon*`、整库复刻 Ionic、Material elevation、包内 `@/features` / 业务。

### 组件契约

`src/components/ui/index.ts` 只转出 happier-ui@0.0.9 的真实导出，并附带 `MCover`、`MPage`、`MContent`。不得恢复的历史平行组件为 `HEmptyState`、`HListRow`、`HSettingRow` 及 `MEmptyState`、`MIconButton`、`MListRow`、`MSettingRow`。Muses 不新造这些通用平行组件；库缺口保留业务实现并登记对应任务的 `gaps.md`，未来在 happier-ui 仓库开发后再回迁。

### 使用规则

- 通过 `@/components/ui` 具名导入。组件只表达语义，**不**读取播放、曲库等业务状态。
- 新样式优先 `--h-*` 或已有 `--muses-*` 别名；不得新硬编码主色 / elevation。
- `HEmpty`、`HButton`（含 icon-only）、`HSwitch`、`HInput`、`HCheckbox`、`HToast`、`HRange`、`HProgress`、`HCard`、`HBottomSheet`、`HDialog`、`HFloatingBubble` 等已有能力优先使用；`MCover` 仅用于音乐封面业务。
- **项目已完全脱离 Ionic**（见 `07-25-migrate-off-ionic-core`）：源码零 `ion-*` 标签、零 `@ionic/*` 导入、零 `ionicons`。列表/设置行统一用原生 `<div>` 结构或 `HCellGroup`/`HCell`；虚拟列表行（SongsPage/QueuePage/PlaylistDetailPage）继续用自定义 `<div>` 行（保留 `data-song-id`、`.is-playing` 高亮、`@tanstack/vue-virtual` 行高实测等约定）。
- **可提交表单**统一用 `@tanstack/vue-form` + `HInput` 字段绑定；约定见 [forms.md](./forms.md)。

### `HRange` 进度条（已全面替换 `ion-range`）

`PlayerPage` 进度条已从 `ion-range` 迁移到 `HRange`（happier-ui 0.0.7 起补齐正式 `change` 事件契约）：

- `HRange` emits：`update:modelValue`（拖动中连续 fire，等价原生 `input`）+ `change`（释放/点击轨道/键盘调整时 fire，programmatic 改值不 fire）+ `drag-start` / `drag-end`。
- 拖动中预览用 `@update:model-value`（写 preview + `seekGestureLocked`）；释放提交用 `@change`（`seekPlayback` + 解锁调度）。
- `:model-value` 绑定 `effectiveSeekPosition`（拖动 preview 优先，否则 `playerState.position`）。
- knob 可隐藏（`--h-range-thumb-*`），轨道仍可点击/拖动 seek；已播放填充用 `--h-range-fill`，未播放轨道用 `--h-range-track-bg`。

## 页面骨架 Pattern（MPage / MContent，自建，无 Ionic）

路由页面使用自建骨架，顶部导航栏统一使用 `HNavBar`：

- **`MPage`**（`src/components/ui/MPage.vue`）：自建页壳 `<div class="m-page">`（flex 纵向 + `height: 100%` + `overflow: hidden`，**无 `contain`** 以免重建 fixed 包含块导致浮层偏移）。内含 `HNavBar :fixed="false"` + 可选 `#subnavbar` slot（SongsPage shuffle-bar）+ `MContent`。`title` / `start` / `end` 插槽映射到 `HNavBar` 的 `title` / `left` / `right`。
- **`MContent`**（`src/components/ui/MContent.vue`）：自建滚动容器 `<div class="m-content">`（`flex: 1; min-height: 0; overflow: auto; overscroll-behavior: contain`，**无 `contain`**）。虚拟列表页可传 `overflow: hidden`（内部列表自管滚动）。
- 简单滚动页（SettingsPage、PlaylistsPage、AlbumsPage、ArtistsPage）直接用 `<m-page>`；虚拟列表页（SongsPage、PlaylistDetailPage）用 `m-page` + 内部 `.m-content` 覆盖 `overflow: hidden`。
- overlay 页（PlayerPage、QueuePage）不使用 MPage/MContent，自建骨架自管滚动。
- modal 内的 `HNavBar` 传 `:fixed="false" :safe-area="false"`，避免重复状态栏留白（弹层现统一为 `HBottomSheet` / `HDialog` / `HPopup`，其 `title` prop 或 `#title` slot 承载标题）。

### 全屏浮层：QueuePage 用 HPopup fullscreen（0.0.7+）

`QueuePage`（播放队列）自 `07-31-adopt-lib-components` 起使用 `HPopup position="fullscreen"`，替换原手写 `fixed inset-0 z-[1200]` 容器与 App.vue 的 `<Transition>` 包裹：

- **双绑 v-model**：`v-model="queueOverlayVisible"`（`src/features/player/overlay.ts` 的 ref），`close-on-overlay` / `close-on-esc` 均关。关闭走 `closeQueueOverlay()`（返回按钮/系统返回），下滑关闭为 HPopup fullscreen 自带增强。
- **常驻挂载**：`App.vue` 直接渲染 `<QueuePage />`（无 v-if/Transition），HPopup 内部 `v-if="visible"` 控制内容显隐与 `h-popup-fullscreen-in/out` 转场；不要再在外层套 v-if 否则 leave 动画被截断。
- **高度链**：`.h-popup__body` 组件库 CSS 仅 `min-width:0`（高度 auto），slot 内容 `h-full` 会解析失效导致虚拟列表全展开。已在 `src/theme/tailwind.css` 补 `.h-popup--position-fullscreen .h-popup__body { height: 100% }`（宿主覆盖，非改库）。
- **列表滚动与下滑关闭隔离**：HPopup fullscreen 的手势监听在 rootEl，以 panel 自身 `scrollTop` 判断是否接管；虚拟列表在内部容器滚动时 panel 不滚（scrollTop 恒 0），会被误判为下滑关闭。已在虚拟列表容器加 `@touchstart.stop @touchmove.stop @touchend.stop @touchcancel.stop` 阻断冒泡，列表滚动完全原生；HNavBar 区域仍可下滑关闭。
- **滚动锁双锁并存**：HPopup `useScrollLock` 锁 `documentElement` inline overflow；宿主 `html/body.muses-overlay-open` class 锁（!important，PlayerPage 用）仍保留。二者独立、均幂等；`hasGlobalOverlay`/`syncBodyOverlayLock` 逻辑不变。
- **z-index**：HPopup fullscreen 默认 `var(--h-popup-z, 1200)`，与原 QueuePage `z-[1200]` 一致，高于 PlayerPage `--h-z-player: 1100`（queue 从 player 内打开叠在其上）。

### 全屏浮层：PlayerPage 用 HPopup fullscreen（0.0.8+，keepAlive + swipeClose）

`PlayerPage` 自 `08-03-player-hpopup-migration` 起完整迁移到 `HPopup position="fullscreen"`（happier-ui **0.0.9**，含 [Happier-X/happier-ui#13](https://github.com/Happier-X/happier-ui/issues/13) 的 `keepAlive` + `swipeClose`；[Happier-X/happier-ui#14](https://github.com/Happier-X/happier-ui/issues/14) 起 `HBottomSheet` 默认全宽）：

- **双绑 v-model**：`v-model="playerOverlayVisible"`，`close-on-overlay` / `close-on-esc` 均关。纵向关闭仍走 PlayerPage 自建手势 → `closePlayerOverlay()`。
- **常驻挂载**：`App.vue` 直接渲染 `<PlayerPage />`（无 `keepPlayerPageMounted` v-if / translate-y 保活）。HPopup 内部 `keep-alive` 控制 slot 显隐。
- **keep-alive**：`:keep-alive="true"` 关闭时 slot 用 `v-show` 隐藏不卸载，保留 AMLL `BackgroundRender`（#22，替代旧 `keepPlayerPageMounted` + `translate-y`）。
- **swipe-close=false**：禁用 HPopup 内置下滑手势，库加 `.h-popup--swipe-disabled` 并把 `touch-action` 交还宿主；PlayerPage 200+ 行自建手势（横向切面板 / 纵向关闭 / `seekGestureLocked` / `touch-action-none`）原样保留。
- **z-index**：HPopup 根是 `<Teleport>`，透传 `class`/`style` 会丢失，**不能**靠内联 `--h-popup-z` 覆盖。宿主在 `src/theme/tailwind.css` 用 `.h-popup--swipe-disabled { --h-popup-z: var(--muses-z-player, 1100) }` 区分 Player（1100）与 Queue（默认 1200）。`--muses-z-player` 来自 happier-ui tokens 的 `var(--h-z-player)`。
- **高度链**：沿用 Queue 的 `.h-popup--position-fullscreen .h-popup__body { height: 100% }` 补丁；内容根 `h-full`（不再 `h-dvh`），HPopup panel 已 `inset:0`。
- **安全区在内容层、不在 HPopup**：fullscreen panel 故意 `inset:0`、无 safe-area 内边距（背景 edge-to-edge）。内容避让由宿主 `.player-overlay .empty-state / .panel` padding 与歌词 FAB `bottom` 负责；**禁止**给 HPopup panel 或 AMLL/fallback 背景加 padding 当「修顶部」。（`08-03-player-top-safe-area`）
- **Player 安全区公式**：一律 `var(--safe-area-inset-*, env(safe-area-inset-*, 0px))` 三级回退；禁止裸 `env()`。宽屏 / 矮屏 media query **只减固定 px**（如 16→10→6），始终 `calc(<px> + var(--safe-area-inset-*, env(...)))`；禁止 `padding: 24px` 这类把 top/bottom safe-area 整段抹掉的写法（平板 ≥768 曾因此顶进状态栏）。
- **滚动锁双锁并存**：同 Queue——HPopup `useScrollLock` + 宿主 `muses-overlay-open` class 锁；`hasGlobalOverlay`/`syncBodyOverlayLock`/`syncPlayerStatusBar`/`backButton` 顺序不变。
- **转场**：HPopup fullscreen 330ms `h-popup-fullscreen-in/out`（原 App.vue 220ms translate-y 已移除，观感变化已获批）。
- navbar 返回行为使用 `HNavBar show-back` 与 `handle-left-click` 显式处理。

### 高度链与 Tabs 视口滚动归属

- **高度链**：`src/theme/tailwind.css` 必须保证 `html, body, #app { height: 100%; }`，否则 `.m-page { height: 100% }` 无确定高度的祖先，`overflow: hidden` 无法裁出内部滚动，顶栏会随外层滚动卷走。不引入 `100dvh`——移动浏览器视口工具栏由 `.m-content` 内部 `overflow` 消化，避免 `position: fixed` 浮层（MiniPlayer / HTabBar / Player / Queue）错位。
- **Tabs 视口不吞滚动**：`src/views/TabsPage.vue` 的 `<main>` 在 `md+` 用 `md:fixed` 铺满侧栏右侧时 **`md:overflow-hidden`**（不可用 `md:overflow-auto`），让纵向滚动回落到业务页 `.m-content`（虚拟列表页内部 list `overflow: auto`）。`main` 在窄屏用 `flex-1 min-h-0` 拿到确定高度；非 tabs 路由分支不引入整页滚动。
- 顶栏 `HNavBar :fixed="false"` 依赖 `.m-page` flex 文档流钉住，不动组件级 `fixed=true`，避免与侧栏 `left` 偏移 / safe-area / overlay 叠加。

### Capacitor Edge-to-Edge 安全区

- `capacitor.config.ts` 必须显式设置 `SystemBars: { insetsHandling: 'css' }`，确保 Capacitor 8 向 `<html>` 注入 `--safe-area-inset-*` 自定义 CSS 变量。
- Android WebView `< 140` 的 `env(safe-area-inset-*)` 可能不正确，因此组件和宿主代码**不得只读 `env()`**；必须优先使用 `var(--safe-area-inset-top, env(…, 0px))` 三级回退（Capacitor 变量 → 标准 env → 0px）。
- **safe-area 由组件库正式接管**（happier-ui ≥ 0.0.7，`c468411`）：`.h-nav-bar--safe-area` / `.h-tab-bar--safe-area` 已在库内实现三级回退。宿主**不得**再持有 `.h-nav-bar--safe-area` 覆盖（历史 workaround 已移除，07-31-upgrade-happier-ui-edge-to-edge）。
- **无 HNavBar 的全屏内容页**（如 PlayerPage）：库不会自动避让，宿主必须自己写三级回退；fullscreen 外壳不加 safe-area 是契约，不是 bug。

参考文件：

- `src/components/ui/MPage.vue`
- `src/components/ui/MContent.vue`
- `src/views/SongsPage.vue`
- `src/views/PlaylistDetailPage.vue`
- `src/views/QueuePage.vue`
- `src/views/SourcesPage.vue`

`src/views/TabsPage.vue` 仅负责导航 chrome 和 `<RouterView />` 的父路由壳，使用普通 Vue 容器（`<nav>` / `<aside>` + `RouterLink`），不套 MPage。

---

## Props Conventions

Current code shows a lightweight runtime prop declaration:

```ts
defineProps({
  name: String,
})
```

Reference file:

- `src/components/ExploreContainer.vue`

For consistency with current code, simple presentational components may use compact `defineProps(...)` declarations. When adding more than trivial props, prefer explicit TypeScript prop typing so the component remains aligned with the repository’s `strict: true` TypeScript mode.

Good fit:

- Small display-only props on reusable components
- Explicit imports for all used happier-ui components via `@/components/ui`

Avoid:

- Implicit global component assumptions
- Large untyped prop bags
- Passing unrelated page state through many component layers in this small app

---

## Import Conventions

**禁止** `import ... from '@ionic/vue'`、`@ionic/vue-router`、`ionicons`。Ionic 依赖已从 `package.json` 删除。

Examples:

- `src/App.vue` 使用原生 `RouterView`（来自 `vue-router`）与自建 `div.app-shell`
- `src/router/index.ts` 使用 `createRouter` / `createWebHistory`（均来自 `vue-router`）
- `src/views/TabsPage.vue` 从 `@/icons` 导入 Lucide Vue 语义组件，并使用 `HIcon` 渲染
- 业务页通过 `@/components/ui` 具名导入 happier-ui 组件与 `MPage`/`MContent`/`MCover`

Also prefer the `@/` alias for application imports from `src/`:

- `import ExploreContainer from '@/components/ExploreContainer.vue'`
- lazy route import `import('@/views/SongsPage.vue')`

### 组件必须显式导入（禁止依赖全局注册）

项目**没有全局组件注册**（`main.ts` 仅 `use(router)`，vue-router 只全局提供 `RouterLink`/`RouterView`）。模板中使用的每一个 `h-*` 组件都必须在 `<script setup>` 中从 `@/components/ui` **显式导入**：

```ts
// ✅ 正确：模板用到的组件全部导入
import { HBottomSheet, HButton, HDialog, HEmpty, HIcon, HInput, HNavBar, MCover } from '@/components/ui'
// ❌ 错误：漏导 HDialog/HInput/HBottomSheet 时，Vue 会把标签当作原生自定义元素渲染——
//    子内容无条件显示在文档流（v-model 失效、无弹窗样式），曾导致歌单页裸显
//    「确定删除该歌单」（见 .trellis/tasks/08-06-playlist-page-fix）
```

ESLint 已启用 `vue/no-undef-components`（error）防回归：模板使用未导入组件会直接报错。vue-tsc 对 kebab-case 未知标签不报错（视为自定义元素），因此**不能**依赖类型检查兜底。

---

## 图标约定（`@lucide/vue` + `HIcon`）

业务侧图标统一使用 `@lucide/vue` 的 Vue 组件，并交给 happier-ui 的 `HIcon` 渲染：

- 语义导出：`src/icons/index.ts`；导入示例：`import { play, pause, shuffle } from '@/icons'`。
- 渲染：使用 `<HIcon :icon="play" />`；业务代码禁止 `ion-icon`、`@/icons/ion-lucide` 与 `ionicons/icons` 直引。
- `package.json` 已删除 `ionicons` 依赖；业务图标全面使用 `@lucide/vue` + `HIcon`。
- 播放模式状态图标必须可区分：`repeatOutline` vs `repeat`、`listOutline` vs `shuffle`，不得两状态共用同一图标。
- 尺寸、颜色与 fill/outline 由 `HIcon` 属性及现有 CSS 控制，保持 `currentColor`。
- **播放主控 fill**：`play` / `pause` / `playSkipBack` / `playSkipForward` 使用 `HIcon` 的 fill 变体，供 `MiniPlayer`、`PlayerPage` 主三键及歌词页浮动播放控件使用。
- **次级仍 outline**：列表「播放全部」用 `playOutline`（与 `play` 解耦，保持线框）；模式键（`shuffle` / `repeat*` / 顺序用 `listOutline`）、队列入口（`list`）、返回、翻译等继续 outline
- **禁止**歌词页浮动播放键再使用圆形 `PlayCircle` / `PauseCircle`；必须与主控同一对 `play` / `pause`（fill）
- **歌词翻译开关必须可区分且同族**：开 = `languageOutline`（Lucide `Captions`），关 = `languageOffOutline`（Lucide `CaptionsOff`）；同一字幕图标族，只差开/关标记。禁止两态共用同一图标只靠透明度，也禁止开态用 `Languages`、关态用 `CaptionsOff` 这种跨族搭配。`aria-label` 仍为「隐藏翻译」/「显示翻译」，并保留 `.is-active` 高亮作为辅助
- 主控按钮统一 `HButton variant="ghost" is-icon-only shape="circle"` 纯图标，不得为 fill 图标另加 solid 圆底阴影

### 同语义同图标（list vs listOutline）

`src/icons` 用不同 Lucide Vue 组件区分历史 ionicons 语义名；业务侧必须按语义选用，**不得混用**：

| 语义 | 导出符号 | Lucide | 调用点示例 |
|------|----------|--------|------------|
| 打开队列 | `list` | ListMusic | `MiniPlayer` 队列键、`PlayerPage` 队列键 |
| 歌单导航 / 歌单列表占位 | `list` | ListMusic | `TabsPage` 歌单 Tab、`PlaylistsPage` 行图标 |
| 顺序播放模式（shuffle off） | `listOutline` | **ListOrdered** | `PlayerPage` 的 `shuffleIcon` 非随机态 |
| 随机播放模式 | `shuffle` | Shuffle | `PlayerPage` 的 `shuffleIcon` 随机态 |
| 列表循环 / 单曲循环 | `repeatOutline` / `repeat` | Repeat / Repeat1 | 播放模式循环键 |
| 播放主控 / 歌词浮动播放 | `play` / `pause` / `playSkip*`（fill） | Play / Pause / Skip* | MiniPlayer、PlayerPage 主三键、歌词页右下角浮动键 |
| 列表次级播放 | `playOutline` | Play outline | 歌单详情「播放全部」等 |
| 专辑 | `albums` | **Disc3** | `TabsPage` 专辑 Tab 等 |
| 艺术家 | `person` | **MicVocal** | `TabsPage` 艺术家 Tab 等 |
| 音源 | `radio` | **Folder** | `TabsPage` 音源 Tab 等 |
| 歌曲占位 / 歌曲 Tab | `musicalNotes` / `musicalNotesOutline` | Music | 歌曲 Tab、列表无封面占位 |
| 歌词翻译开 | `languageOutline` | **Captions** | 歌词页左下翻译键（显示译文） |
| 歌词翻译关 | `languageOffOutline` | **CaptionsOff** | 歌词页左下翻译键（隐藏译文） |

规则：

- **`list`（ListMusic）** 专用于「打开队列」与「歌单」相关入口；`MiniPlayer` 与 `PlayerPage` 打开队列必须同一导出 `list`。
- **`listOutline`（ListOrdered）** **仅**用于顺序播放模式（shuffle off），不得用于队列按钮或歌单行占位；**不得**再映射为无序号的 Lucide `List`。
- 歌词页右下角播放/暂停与主控同图标（`play` / `pause` fill），**不得**使用圆形 outline 变体。
- 歌词页翻译键：`showLyricTranslation` 为 true 用 `languageOutline`，为 false 用 `languageOffOutline`；**不得**两态共用 `languageOutline`。
- **导航 Tab 图标**：`albums` → Lucide `Disc3`（禁止 `DiscAlbum`）；`person` → Lucide `MicVocal`（禁止通用 `User`）；`radio` → Lucide `Folder`（音源是文件夹/目录语义，**禁止** `Radio` 电台图标，尽管导出符号历史名仍为 `radio`）。
- 业务侧继续使用既有导出名 `albums` / `person` / `radio`，**不要**为改 Lucide 几何而重命名调用点；新代码优先与 `TabsPage` 保持一致。
- `musicalNotes` 与 `musicalNotesOutline`、`add` 与 `addOutline` 在 `@/icons` 中已是同一 Lucide 几何的别名，视觉已一致；可不强制改名，但新代码优先与现有调用点保持一致。

---

## Responsive Breakpoint Convention

项目使用以下全局 CSS 变量在宽屏下适配平板布局：

- `--muses-breakpoint-tablet: 768px` — 平板断点宽度（定义于 `src/theme/tokens.css`）
- `--muses-content-max-width: 720px` — 内容最大宽度限位居中

### 断点约定的规则

1. **Muses 视觉变量统一在 `src/theme/tokens.css` 的 `:root` 中定义**，单一来源，所有页面引用 `var(--muses-*)`；`variables.css` 已清空（Ionic 桥接已全部移除）。
2. **`@media (min-width: XXX)` 条件中不可使用 `var()`** — CSS 标准不允许。在 `@media` 中直接使用硬编码的 `768px`；`var()` 只用于属性值部分（如 `max-width: var(--muses-content-max-width)`）。
3. **宽屏下隐藏元素**：对窄屏专属元素（如底部 tab bar）使用 `@media (min-width: 768px) { … display: none }`。
4. **窄屏零回归**：所有平板改造限定在 `@media (min-width: 768px)` 内；窄屏下不加任何额外样式。

### 当前平板组件模式

- **导航 Shell**：`src/views/TabsPage.vue` 使用普通 Vue 布局容器作为父级 shell；子页面使用自建 `MPage`/`MContent` 骨架。
- **侧栏**：宽屏下由固定定位的普通 `<aside>` 提供左侧导航，右侧 `<main>` 渲染 `<RouterView />`；窄屏回落为普通 `<nav>` + `RouterLink` 底部导航。
- **避免 Split Pane**：当前 MuMu / Android WebView 环境中，早期 `ion-split-pane` + `ion-menu` 曾触发白屏；已彻底移除 Ionic，不得回归该结构。
- **专辑卡片网格（Albums）**：`src/views/AlbumsPage.vue` 使用 `<div class="album-grid">` 直接渲染 `<article class="album-card">` 卡片（封面 + 专辑名 + 歌曲数 + 艺术家摘要），不再使用 `ion-list` / `ion-item`。窄屏固定 `grid-template-columns: repeat(2, minmax(0, 1fr))`；宽屏在内容宽度上限内 `repeat(auto-fill, minmax(180px, 1fr))` 自动增列。卡片封面复用 `MCover`，通过 `.album-card > .album-card__cover { --m-cover-size: 100% !important; height: auto; aspect-ratio: 1; flex: 0 0 auto }` 覆盖 MCover 内联默认尺寸；`height: auto` 必须保留，使 `aspect-ratio` 能按卡片宽度计算正方形高度（不改 MCover 全局契约）。
- **艺术家卡片网格（Artists）**：`src/views/ArtistsPage.vue` 使用 `<div class="artist-grid">` 直接渲染 `<article class="artist-card">` 卡片（圆形头像 + 艺术家名 + 歌曲数 + 专辑数），不再使用 `ion-list` / `ion-item`。窄屏固定 `grid-template-columns: repeat(2, minmax(0, 1fr))`；宽屏在内容宽度上限内 `repeat(auto-fill, minmax(180px, 1fr))` 自动增列。头像复用 `MCover`，从艺术家歌曲中选择首张有效封面，无封面时保留占位；通过 `.artist-card > .artist-card__avatar { --m-cover-size: 100% !important; height: auto; aspect-ratio: 1; border-radius: 50% }` 保持正圆。
- **SongsPage 宽屏单列**：`src/views/SongsPage.vue` 宽屏不使用多列 grid，列表始终竖排单列；外层 `.list-grid` / `.tablet-content-limit` 仅做 `max-width: var(--muses-content-max-width); margin-inline: auto` 限位居中（与窄屏一致的一列体验）。
- **内容限位居中**：各列表页 `.tablet-content-limit`、`.artist-grid`（Artists）和 `.album-grid`（Albums）在宽屏下 `max-width: var(--muses-content-max-width); margin-inline: auto`。

### SongsPage Navbar 下方固定随机播放全部

`src/views/SongsPage.vue` 在顶部 Navbar 正下方固定显示随机播放全部入口，歌曲列表在其下方滚动：

- 位置：通过 `MPage` 的 `#subnavbar` 插槽承载，位于 `HNavBar` 与 `MContent` 之间的独立 `.shuffle-bar` flex 项，使入口与 Navbar 一起固定，不随歌曲列表滚动。
- 布局：按钮容器在窄屏左对齐；宽屏使用 `max-width: var(--muses-content-max-width)` 与 `margin-inline: auto` 限宽居中，按钮仍位于内容左侧。
- 样式：优先 `HButton` + `HIcon` 展示 `@/icons` 的 `shuffle` 与「随机播放全部」文案，不得使用整行描边操作条。
- 禁止把入口放入会随列表滚走的普通内容流。
- 无歌曲时按钮仍出现且 `:disabled`，点击不产生副作用；保留 `aria-label="随机播放全部"`。
- 点击语义：`clearQueue()` → `enqueueSongs(allSongs)` → 若 `!shuffleEnabled()` 则 `toggleShuffle()` → `selectSongAtIndex(0)` → `playSong(first)`。
- `toggleShuffle` 会生成 `shuffleOrder`；`selectSongAtIndex(0)` 取乱序首曲。
- 歌曲列表滚动容器的 `padding-bottom` 只需避让 **MiniPlayer**，不再为随机播放操作条额外留位；仍须确保最后一首歌曲滚动到底后完整可见。
- **避让职责边界（勿双算）**：移动端 Tab Bar 与 safe-area 的空间已由 `TabsPage.vue` `<main>` 的 `padding-bottom: calc(var(--muses-tab-bar-height) + env(safe-area-inset-bottom))` 预留，列表自身 `padding-bottom` **不得**再加 `--muses-tab-bar-height`（历史 bug `08-06-songs-bottom-spacing`：曾双算 tab-bar 高度，导致滚到底多出约 64px 空白）。
- **取值约定**（`SongsPage.vue` / `PlaylistDetailPage.vue` / `SourcesPage.vue` 的 `listParentRef`）：
  - 移动端：`padding-bottom: calc(var(--muses-mini-player-height) + var(--muses-space-lg))`（≈80px = MiniPlayer 实际高度 64px + 余量；safe-area 已被 main 消化，**不再加**）；
  - 平板端（`md:`）：`calc(var(--muses-mini-player-height) + var(--muses-space-lg) + env(safe-area-inset-bottom, 0px))`（main `pb-0`、MiniPlayer 贴视口底，需补 safe-area）；
  - `SourcesPage` 保留卡片底部 24px 设计留白，余量用 `--muses-space-xl`（≈88px）。

参考结构：

```vue
<m-page fullscreen>
  <template #title>歌曲</template>
  <template #end><!-- 搜索操作 --></template>
  <template #subnavbar>
    <div class="shuffle-bar">
      <div class="shuffle-actions">
        <HButton variant="ghost" aria-label="随机播放全部">
          <template #leading>
            <HIcon :icon="shuffle" aria-hidden="true" />
          </template>
          随机播放全部
        </HButton>
      </div>
    </div>
  </template>
  <!-- 歌曲虚拟列表 -->
</m-page>
```

### SongsPage 跳转到当前播放 FAB

`src/views/SongsPage.vue` 使用 `HFloatingBubble` 在右下侧放置浮动按钮，用于滚动到当前播放歌曲行：

- 组件：`<h-floating-bubble axis="lock" :offset="fabOffset" :ariaLabel="'跳转到当前播放'" @click="scrollToCurrentSong">`；`axis="lock"` 禁拖拽，`offset` 直接指定绝对坐标（避让 MiniPlayer + tab-bar + safe-area）。图标用 `@/icons` 的 `locateOutline` 经 `HIcon` 渲染。
- 可见性：`v-if="currentPlayingInList"` —— 仅当 `playerState.currentSong?.id` 存在且该 id 出现在当前歌曲列表中时展示；无当前播放或不在列表则隐藏。
- **行定位（虚拟列表）**：`findIndex` 后只调用 `rowVirtualizer.scrollToIndex(index, { align: 'start' })`（可 smooth 一次，layout 后再瞬时兜底一次）。**禁止**再对行做 `scrollIntoView`，也**禁止**给虚拟行加 `scroll-margin-top` / `scroll-mt-*` 去「扣 navbar」。
- **为何不用 scroll-margin**：列表滚动容器是 navbar + shuffle-bar **下方**的 `listParentRef`，顶栏不在滚动端口内；`align: 'start'` 已对齐列表可视区顶。历史 108px `scroll-mt` + `scrollIntoView` 会在**第二次及之后** FAB 点击时把当前行再往下挪（`08-03-songs-jump-current-second-click`）。
- 行上可保留 `data-song-id` 供样式/调试；跳转**不得**依赖查询 DOM 再 `scrollIntoView`。
- 列表末尾无法再滚时停在容器允许的最大位置（不必强行置顶）。宽屏单列同样适用。
- 可选轻高亮：滚动后给目标行加 jump highlight 约 1.2s，再移除；卸载时清理 timer。高亮不依赖二次滚动。
- 安全区：FAB `offset` / 底边需避开底部导航与 MiniPlayer（窄屏约 Tab Bar + MiniPlayer；宽屏无 Tab Bar、MiniPlayer 贴底），不遮挡列表关键操作。勿按「平板 MiniPlayer 抬高 64px」再额外加偏移。
- 不破坏现有列表点击播放与更多按钮交互。
- 对照：`QueuePage` 打开时仅 `scrollToIndex`（`align: 'center'`），同样不走 `scrollIntoView`。

## Styling Gotchas

### navbar 标题统一使用 HNavBar 居中契约

页面与 modal 的顶部导航栏统一使用 `HNavBar`。其 `left / title / right` 三栏布局负责标题相对完整 navbar 居中，业务页面不再维护 Ionic 标题绝对定位补丁。

统一约定：

- 标题优先使用 `title` prop；需要自定义内容时使用 `title` slot。
- 左右操作分别使用 `left` / `right` slot；返回使用 `show-back`、`back-aria-label` 与 `handleLeftClick`。
- 动态长标题依赖 `HNavBar` 内建单行省略，不添加页面级居中 class 或覆盖内部 grid。
- `src/theme/variables.css` 已清空（Ionic 桥接已移除）；navbar 相关样式统一由 `HNavBar` 承接。
- 页面级 `safeArea` 保持默认开启；modal 内显式关闭；固定与否按页面壳规则设置。

### 列表布局使用原生结构

所有列表已从 `ion-list` / `ion-item` 迁移为原生 `<div>` 结构或卡片网格。`ArtistsPage` 与 `AlbumsPage` 直接渲染 `article` 卡片网格；`SongsPage` / `QueuePage` / `PlaylistDetailPage` 使用虚拟列表 + 原生行 `<div>`；`SettingsPage` / `PlaylistsPage` 使用原生 `<div>` 行。宽屏多列直接在原生 div 上写 CSS Grid，不再有 Web Component Shadow DOM 隔离问题。

**SongsPage 宽屏单列**：

```css
@media (min-width: 768px) {
  .list-grid {
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
}
```

### CSS var() 不可用于 @media 断点条件

CSS 变量只能在属性值中解析，不能在 `@media (min-width: …)` 中生效。错误写法：

```css
/* ✗ 错误——CSS 变量在 @media 条件中无法解析 */
@media (min-width: var(--muses-breakpoint-tablet)) { … }

/* ✓ 正确——@media 条件用硬编码，属性值用 var() */
@media (min-width: 768px) {
  .list-grid {
    max-width: var(--muses-content-max-width);
  }
}
```

Current styling is split by scope:

- Global framework/theme CSS is loaded once in `src/main.ts` via `src/theme/tailwind.css`
- All component styling uses Tailwind CSS v4 utility classes via `class`/`:class`
- For complex multi-tier responsive layouts or `:deep()` internal DOM selectors (e.g., AMLL internals),
  component-scoped CSS can be placed in `src/theme/tailwind.css` as global rules prefixed with a
  component-specific selector to avoid leaking.
- JS runtime `:style` dynamic bindings are preserved and not considered scoped CSS.

Reference files:

- `src/main.ts`
- `src/theme/tailwind.css`
- `src/components/MiniPlayer.vue`（Tailwind utility only）

Do not duplicate Ionic core or utility CSS imports inside page components.

---

## MiniPlayer 与播放器 Overlay 约定

`src/components/MiniPlayer.vue` 是应用级固定底栏，由 `src/App.vue` 始终挂载；播放器和队列使用全局 overlay 状态显示，不再通过 `/player` 或 `/queue` 路由打开。

### 样式约定

- 底栏占满屏幕宽度，固定在移动端底部导航栏上方。
- **窄屏**（`<768px`）：`bottom: calc(64px + env(safe-area-inset-bottom, 0px))`，为底部 Tab Bar 留位。
- **宽屏**（`@media (min-width: 768px)`）：平板侧栏布局已隐藏 `.mobile-tab-bar`，底栏贴底，仅保留安全区：`bottom: env(safe-area-inset-bottom, 0px)`；禁止继续抬高 64px，否则会悬空。
- 底栏本身不使用圆角和阴影，仅保留顶部边线分隔内容。
- 封面容器圆角与歌曲列表一致，使用 `border-radius: 10px`。
- 无当前歌曲或无封面时展示稳定占位封面与占位文案，避免播放状态为空时底栏跳动或消失。
- `MiniPlayer` 不要因 overlay 打开而用 `v-if` 卸载；overlay 打开时只禁用交互（例如 `pointer-events: none`），避免下滑关闭时底栏闪烁。

### 交互约定

- 点击底栏主体调用 `openPlayerOverlay()`，不能改变当前路由 URL。
- **无当前歌曲时不可打开沉浸式播放页**：当 `playerState.currentSong` 为 `null` 时，点击主体或键盘 Enter / Space 都不得调用 `openPlayerOverlay()`；主体应标记 `aria-disabled`，并去掉 `cursor: pointer` 误导。
- 点击播放/暂停按钮只控制播放状态，不能触发打开播放器 overlay。
- 播放/暂停图标使用 `@/icons` 的 `play` / `pause` 组件并由 `HIcon` 以 fill 变体渲染，与沉浸页主控一致；不得用 outline、`ion-icon` 或 ionicons 直引。
- 播放/暂停、队列等操作按钮统一用 `HButton`（`variant="ghost"` + `is-icon-only` + `shape="circle"`）。
- 无歌曲时播放/暂停按钮继续禁用；队列按钮行为不受影响，仍可打开队列 overlay。
- 点击队列按钮调用 `openQueueOverlay()`，不能改变当前路由 URL，也不能触发打开播放器 overlay。
- 嵌套在可点击主体内的 `HButton` 必须使用 `@click.stop` 阻止冒泡，父级主体不再依赖 `event.composedPath()` 手动过滤按钮区域。

```ts
const openPlayerPage = () => {
  if (!playerState.currentSong) {
    return
  }
  openPlayerOverlay()
}
```

### Overlay 页面约定

- `PlayerPage.vue` 和 `QueuePage.vue` 是全局 overlay 内容组件，由 `App.vue` 渲染在 `<RouterView>`（`.app-router-view` 包层）之后；不要在 `src/router/index.ts` 中新增 `/player` 或 `/queue` 路由。
- 打开播放器/队列 overlay 时底层 tabs 路由页面必须保持存在，以支持下滑收起露出真实底层页面。
- 播放器 overlay 的系统状态栏样式由 `App.vue` 监听 `playerOverlayVisible` 统一管理：打开时调用 `StatusBar.setStyle({ style: Style.Dark })` 显示白色内容，关闭及 `App.vue` 卸载时用 `Style.Default` 恢复默认；插件失败必须静默忽略，并通过串行化或请求 token 防止快速开关导致异步乱序。不得监听 `hasGlobalOverlay`，队列 overlay 单独打开不修改状态栏，也不要在 `PlayerPage.vue` 内管理状态栏。
- 播放器 overlay 顶部不显示返回/收起按钮，也不展示「正在播放」标题；顶部仅保留安全区留白。关闭通过下滑手势、Android back 键或显式 overlay 状态完成。
- 下滑收起播放器时移动 overlay 内容层，不要移动底层路由页或依赖透明路由页露出缓存层，否则容易出现黑屏或重复页面。
- 沉浸式控制页布局自上而下：大封面 → 歌名/歌手 → 进度条 → 主控制（上一曲/播放暂停/下一曲）→ 次要控制（循环/随机/队列/**更多**）。
- **mode-bar 更多菜单**（`08-04-player-more-edit-song`）：队列键旁增加 `ellipsisVertical`「更多」；`HBottomSheet` 标题「歌曲操作」，菜单项**仅**「编辑歌曲信息」+ 取消（不含加入歌单/加队列）。第二层 sheet「编辑歌曲信息」用 `@tanstack/vue-form` + `HInput`/`HTextarea` 编辑 title/artist/album/封面/歌词/ReplayGain dB；保存走 `saveCurrentSongUserEdit`（必写库 + 尽力写文件，D4 Toast 区分）。`mode-bar` `max-w` 约 320px；四键仍用沉浸 ghost。empty-state 无 mode-bar 故无更多键。
- **mode-bar 循环/随机无选中高亮**（`08-04-player-modebar-no-active`）：循环与随机**不得**绑定 `.is-active` 半透明白底；模式仅靠图标对 + `aria-label`（`repeat`/`repeatOutline`、`shuffle`/`listOutline`）。全局 `.is-active` CSS 仍保留给歌词翻译 FAB。
- **不展示元信息补充过程文案**（#48）：沉浸式页不得渲染「正在补充歌曲信息…」「歌曲信息补充失败…」等 `metadataStatus` 提示；后台扫描/在线补全逻辑可继续运行。
- 沉浸式控制页封面（`.cover` / 占位封面）不加 `box-shadow`；宽屏与窄屏保持一致，避免封面后方出现额外阴影。
- **封面必须保持正方形**：`.cover` / `.placeholder-cover`（与 `.cover` 共用尺寸类）使用 `aspect-ratio: 1; height: auto; object-fit: cover`。正方形边长 = `min(水平上限, 垂直上限)`。
  - **窄屏** `.cover` 的 `width` 也必须同时受 vw 与 cover-slot 的 dvh/`max-height` 约束（默认 `min(72vw, 100%, 340px, 52dvh)`；`max-height: 720px` 时 `min(72vw, 100%, 260px, 42dvh)`；更矮 `max-height: 520px` 时 `min(72vw, 100%, 200px, 38dvh)`）。
  - **宽屏** 同理：`min(40vw, 48dvh, 320px)`；矮屏 `min(40vw, 42dvh, 260px)`；更矮 `min(40vw, 38dvh, 200px)`。
  - 禁止只写 `width: min(72vw, 100%, 340px)` 或 `min(40vw, 320px)` 而仅靠 `max-height: 100%` clamp 高度——当 cover-slot 可用高度小于目标宽度时，高度被夹、宽度仍按 vw → 封面被压成长方形（车机矮屏/手机横屏的典型回归）。
- **矮屏/横屏控制区收紧**（仅控制页 `.info-panel`，不改歌词页）：保持竖排与全部控件可见，不改为左右分栏、不隐藏模式栏/进度。用 `max-height` 分层收紧：
  - 默认（正常竖屏高度，如 `>720px`）：较大 padding / gap / 按钮与进度热区，观感不变。
  - `max-height: 720px`：减小 panel 上下 padding（保留 `safe-area`）、`info-panel-inner` gap、进度 slider 热区（约 20px）、主控与模式栏按钮尺寸，封面槽位拿到更多垂直空间。
  - `max-height: 520px`：再收一档 gap/字号/按钮/热区（约 18px），仍显示全部控件。
  - 不引入 landscape 专用 DOM；横屏通常命中 `max-height` 断点即可。padding 只减固定 px 部分，用 `calc(... + safe-area)`，不得抹掉安全区。
- 主控制三键（上一曲/播放暂停/下一曲）均为 `HButton`（`variant="ghost"` + `is-icon-only` + `shape="circle"`）纯图标按钮，无 solid 圆底与按钮阴影；图标从 `@/icons` 导入并由 `HIcon` 以 fill 变体渲染（`play` / `pause` / `playSkipBack` / `playSkipForward`），不得回退 outline 主控或 ionicons 直引；可保留略大热区（如播放键 68×68），必须提供 `aria-label`，loading 禁用态保留。
- **沉浸页 ghost 按下态必须统一**（`08-04-player-immersive-btn-press-unify`）：`.player-overlay` 内主控 / mode-bar / 歌词 FAB 共用 `.h-button--ghost` 基类覆盖 `color` + `background` + `:hover` / `:active` + 语义 `.is-active`（半透明白底、浅色字）。**禁止**只改 `color` 而依赖库 ghost 浅灰 active（深底上会闪灰块）；**禁止** Ionic `--color`/`--background`。mode-bar / FAB 默认可更淡，但按下与激活机制须与主控同族。
- 循环/随机/队列使用纯图标按钮，必须提供 `aria-label`；**不要**用 `.is-active` 白底/提亮表达循环或随机「选中」，模式只靠图标对 + `aria-label`，不要依赖可见文字标签。播放器模式图标必须与当前状态同步，且一律从 `@/icons` 导入并由 `HIcon` 渲染：列表循环使用 `repeatOutline`（Lucide `Repeat`）、单曲循环使用 `repeat`（Lucide `Repeat1`），顺序播放使用 `listOutline`（Lucide `ListOrdered`）、随机播放使用 `shuffle`（Lucide `Shuffle`）；状态切换后图标和标签应立即更新，禁止两个状态共用同一图标。**打开队列**按钮使用 `list`（Lucide `ListMusic`），与 `MiniPlayer` 队列键一致；不得用 `listOutline` 表示队列（`listOutline` 仅顺序播放）。
- **歌词页浮动播放键**：窄屏歌词页右下角播放/暂停必须使用与主控相同的 `play` / `pause`（fill），禁止圆形 `PlayCircle` / `PauseCircle`。
- 控制页必须一屏适配：`immersive-shell` / panels 固定 `height: 100dvh`，`overflow: hidden`；控制区块 `flex: 0 0 auto`，禁止页面纵向滚动。
- **竖屏控制页垂直节奏（整体居中收紧）**：`.info-panel-inner` 用 `justify-content: center` + 紧凑 `gap`（默认约 12px；矮屏断点再收），将「封面 → 歌名/歌手 → 进度 → 主控 → 模式栏」作为一组垂直居中。**禁止**回退 `justify-between` + `.cover-slot { flex-grow: 1 }`——会把封面顶到上半区并在槽内/块间制造大块松散留白。
- **`.cover-slot` 不 flex-grow**：竖屏与宽屏均为 `flex: 0 1 auto; min-height: 0`，靠自身 `max-height` 与封面 width 的 dvh 上限缩放；矮屏仍可 shrink，但不吞噬剩余高度撑出上下空带。
- 歌词页（AMLL）视觉约定：
  - **窄屏** `.lyric-panel`：顶部 `.lyric-header` 展示歌名（主标题）+ 歌手（副标题，空则不渲染；不拼接专辑、不回退「未知歌手」）；其下为 `flex:1` 的 AMLL `LyricPlayer`；底部仅安全区。
  - **歌词页浮动 chrome 按需显示**：左下翻译、右下播放/暂停（仅非平板）默认 **隐藏**（`opacity: 0` + 容器/按钮 `pointer-events: none`），约 180ms fade。用户在 **歌词面板内** 点击或滑动歌词后显示（`.is-visible`），空闲 **3 秒** 再隐藏；点浮动按钮重置计时。**切回控制页**（`activePanel !== 1`）或 **关闭 overlay** 立即隐藏并清 timer。隐藏态禁止可点热区。竖屏/横屏/宽屏双栏均走同一 `lyricChromeVisible` 路径；宽屏仅隐藏播放键，**不**整区隐藏 chrome。
  - **翻译键仅有译时出现**：`hasLyricTranslation` 为 true（`prepareLyricLinesForDisplay` 后任一行非空 `translatedLyric`/`romanLyric`，或 `playerState.lyricsTranslation` 非空）才渲染翻译 FAB；纯原文无译不占位。无译仅剩窄屏播放键时 `justify-end`；宽屏无译且无播放键时不挂浮动容器。
  - **FAB 颜色（HButton ghost）**：与控制页主控/mode-bar 共用 `.player-overlay .h-button--ghost` 沉浸交互基类（浅色字 + 半透明白 hover/active）；FAB 默认可保留微黑底 + blur。激活翻译键 `.is-active` 用更亮字色 + 略高不透明白底；**禁止** Ionic `--color`/`--background`，也禁止只改字色。
  - **宽屏**（`@media (min-width: 768px)`）：隐藏 `.lyric-header`，右侧只保留歌词；AMLL 视觉参数与窄屏一致。
  - AMLL 参数：`alignAnchor="center"`、`alignPosition=0.5`（当前行位于歌词可视区中心）、`enableBlur` / `enableScale` 开启；字号用 `--amll-lp-font-size`（约 `clamp(22px, 6.5vw, 32px)`）；用 `:deep()` 去掉行左右 padding，使歌词左缘与顶部信息对齐。
  - 翻译副行样式必须使用 AMLL 实际类名：`.FmKaba_lyricLine.FmKaba_active`、`.FmKaba_lyricMainLine.FmKaba_active` 和 `.FmKaba_lyricSubLine`；不要依赖不存在的自定义 active 类。歌词 timed 翻译需支持点号、冒号、逗号毫秒时间戳，匹配容差应保持较小并有超界测试，避免翻译错位。同时间戳双语主行合并时主行须为原文（非 Han 优先于 Han），关翻译后不得只剩中文译文当主行。
  - 继续使用 `@applemusic-like-lyrics` 的 `LyricPlayer`，不自研滚动引擎；主词解析用库内 `parseLrc` / `parseYrc` / `parseQrc` / `parseTTML`，翻译适配仅走 `prepareLyricLinesForDisplay`（tlyric 挂载 + 双行 plain LRC 主译 + 开关），不修改 `node_modules`，不新增平行歌词解析器。
  - 在线歌词匹配期间：若有本地歌词先展示本地；若无本地歌词显示「正在匹配在线歌词…」。匹配无结果、网络失败或解析失败且无本地歌词时，空态需说明「未匹配到在线歌词，且无本地歌词」，不得一直空白或弹错误打断播放。
  - **歌词行点击 seek**：`LyricPlayer` 绑定 `@line-click`（AMLL emit `lineClick` / core `line-click`）。事件类型为 `LyricLineMouseEvent`，其中 `line` 是 `LyricLineBase`，通过 `line.getLine().startTime` 取起始时间（**毫秒**），再调用 `seekPlayback(startTime / 1000)`（秒）。`startTime` 非 number / 非有限数 / `< 0` 时不 seek。处理时 `stopPropagation` + 复用 `seekGestureLocked`，避免点击误触发 overlay 下滑关闭或横向切面板。无歌词空状态不绑定该行为。
  - **歌词区上下滑动手势隔离**：AMLL `LyricPlayer` 内部滚动基于 transform，**非原生 scroll**，`canStartVerticalDismiss` 的原生 `scrollHeight > clientHeight && scrollTop > 0` 检测无法识别。因此 `isLyricPanelTarget` 使用 lyrics panel / player 的 template ref 进行 `Node.contains` 判断触点是否落于歌词区内；`canStartVerticalDismiss` 在歌词区落点返回 `false`，使歌词区上下滑动不更新 `dragOffsetY`、不触发 overlay 下滑关闭。控制页（`.info-panel`）下滑关闭语义不变；`onTouchEnd` 中基于 `startX / endX` 的横向切换面板逻辑保留，歌词页左滑仍可切回控制页。`isNativeInteractiveEvent` 保留 `composedPath` 兜底以穿透 Shadow DOM 内的交互控件（如 `HButton` 原生按钮）。
- **SongsPage 大列表必须虚拟化**（#50）：使用 `@tanstack/vue-virtual` 只渲染可视行（固定约 72px，适量 overscan），自建滚动容器；「跳转当前播放」先 `scrollToIndex` 再高亮挂载行，禁止恢复全量 `v-for="song in songs"`。
- 打开播放器/队列 overlay 时必须锁定底层路由页交互与滚动：`.app-router-view` 设 `pointer-events: none`，`body.muses-overlay-open .app-router-view` / `.m-content` 禁用滚动；不要锁住队列 overlay 自己的滚动容器。
- 播放器 overlay 自身使用 `touch-action: none`，并在非原生可交互控件（含 `input` / `HRange` / `HButton` 等）上对 `touchmove` 调用 `preventDefault`，防止滑动穿透到底层歌曲列表；进度条保留可拖动。
- **进度条手势隔离**：`.progress-area` 必须 `@touchstart.stop` / `@pointerdown.stop`，并配合短 debounce 的 `seekGestureLocked`；seek 期间/刚结束后禁止 `playPreviousFromQueue` / `playNextFromQueue`，也禁止横向切换 `activePanel`，避免松手点穿到上一曲/下一曲或误切歌词面板。`isNativeInteractiveEvent` 必须识别 `HRange` / `.progress-range`（不仅是原生 `input`）。
- **进度条使用 `HRange`（无可见圆点）**：
  - 控件：`<h-range class="progress-range">`，`min=0`，`max=duration`（duration 为 0 时 max 兜底为 1 并禁用），`step` 细粒度（如 `0.1`），`:model-value` 绑定 `effectiveSeekPosition`（拖动 preview 优先，否则 `playerState.position`）。
  - **`onSeekInput` 仅在 `seekGestureLocked` 为 true 时写 preview**：`HRange` 在值变化时会 emit `update:modelValue`（拖动中连续 fire）；无手势锁时必须忽略，否则 preview 冻住填充、播放进度看似不走（#47）。
  - 隐藏 knob：用 `HRange` 的 `--h-range-thumb-*` 令 knob 不可见；桌面与窄屏均不可见圆点，但轨道仍可点击/拖动 seek。具体实现（`src/theme/tailwind.css` 的 `.player-overlay .progress-range`）：`--h-range-thumb: 0px` + `::-webkit-slider-thumb` / `::-moz-range-thumb` 置 `width/height: 0; border: none; opacity: 0; pointer-events: none`（仅置 0 尺寸会残留组件自带的 2px border 圆点，必须同时覆盖 border）。thumb 的 `pointer-events: none` 不影响交互——原生 range 的点击/拖动由 input 元素承载。
  - 轨道视觉用 `HRange` 自带 fill/track：`--h-range-track-bg`（未播放）、`--h-range-fill`（已播放）；**不再维护** `.progress-track-buffered` / 自绘三层缓冲 DOM，也不再注入 UI 用的 `--buffered` CSS 变量。
  - 事件：`update:modelValue` → 更新 preview + `seekGestureLocked`；`change` → `seekPlayback` + 解锁调度（happier-ui 0.0.7 起 `HRange` 补齐了正式 `change` 释放提交事件契约）。缓冲已知时 UI 侧仍将目标 clamp 到 `bufferedPosition`，越界轻提示「缓冲中」；`seekPlayback` 业务 clamp/拒绝语义不变。
  - **缓冲未知**（`playerState.bufferedPosition == null`）时不画假缓冲条；WebDAV 远程直链固定属于此状态，seek 退化为 duration clamp。
  - **歌词行点击**：目标 > `bufferedPosition` 时不 seek（与进度条共用 `seekPlayback` 拒绝语义）。

### 冷启动续播进度保护

冷启动从 `muses:playback-session` 恢复后，`controller.ts` 先把持久化位置展示为 paused；用户点击继续播放时再执行原生 `play`，成功后 seek 到恢复点。恢复 seek 完成前，持久化位置是 UI 的权威进度：

- 保护必须放在播放器 controller 状态协调层，不得在 `PlayerPage.vue` 维护平行进度缓存。
- 只屏蔽当前歌曲明显早于恢复点的原生 position；`status`、`duration`、`bufferedPosition` 等其它字段仍按原生事件更新。
- `playing` 与提前到达的 `finished` 事件都必须遵守保护，避免启动初始位置让进度条回退或触发错误的播放结束状态。
- 原生位置到达恢复点附近，或恢复 seek 成功/失败、播放失败、主动切歌、普通 seek、显式 stop 时，必须清除保护，让后续真实进度正常驱动 UI。
- 单元测试与 e2e 基础设施已全部移除。项目不再包含任何自动化测试；用户通过手动验证确认功能。
- 测试移除后，过去仅因测试存在的 DOM 标记类（`.immersive-shell`、`.mini-player`、`.app-mini-player`、`.app-player-page`、`.m-cover`、`.amll-background{,-render}` 等）已从模板中删除。
- 未来如需恢复测试，优先使用语义选择器（`aria-label`、`role`、元素标签），禁止依赖命名标记类。

### 隐藏播放器渲染降载约定

`App.vue` 在有当前曲时保活 `PlayerPage`，关闭态不得恢复销毁重建；关闭态可用 `visibility: hidden` 与 `contain: paint` 跳过不可见绘制。`PlayerPage.vue` 的 AMLL `current-time` 在可见时跟随 `playerState.position`，隐藏时冻结；重新打开时必须以最新 position 同步，避免白闪和旧歌词位置。后台音频、MediaSession 和播放进度状态不受影响。

### Overlay 组件必须异步加载（首屏性能约定）

**What**: `App.vue` 中 `PlayerPage` / `QueuePage` 必须用 `defineAsyncComponent(() => import(...))` 异步加载，不能用静态 `import`。

**Why**: `PlayerPage` 静态 `import @applemusic-like-lyrics/*` + `@pixi/*` 整套 WebGL 库（gzip 后上百 KB，原体积 ~400KB+）。一旦静态 import 被打进 `App.vue`，这些库就进入首屏必须同步下载/执行的主 bundle，直接导致打开应用白屏几秒。改为 `defineAsyncComponent` 后，Vite/Rollup 把 PlayerPage 及其依赖切成独立异步 chunk，仅在 `v-if="playerOverlayVisible"` 首次为 true（即用户点开播放器全屏页）时才加载。实测主入口 JS 从 1.5MB 降到 38KB。

**Example**:
```ts
// ✓ 正确——异步加载，重量级库进独立 chunk
import { defineAsyncComponent, onMounted } from 'vue'
const PlayerPage = defineAsyncComponent(() => import('@/views/PlayerPage.vue'))
const QueuePage = defineAsyncComponent(() => import('@/views/QueuePage.vue'))
```
```ts
// ✗ 错误——静态 import，drag AMLL/Pixi 进首屏主 bundle，导致白屏
import PlayerPage from '@/views/PlayerPage.vue'
import QueuePage from '@/views/QueuePage.vue'
```

**Related**: `vite.config.ts` 的 `manualChunks` **只**把 `vue` / `@vue` / `vue-router` 归入 `vue-vendor`；**不要**再为 `@applemusic-like-lyrics` / `@pixi` 建 `amll-pixi` 手动 chunk（易与共享 CJS 互操作辅助形成顶层环，Android WebView 白屏）。AMLL/Pixi 依赖 `PlayerPage` 的 `defineAsyncComponent` 留在异步 chunk。新增重量级库时同样优先异步边界，不要静态 import 进首屏。

**Gotcha**: 异步组件首次解析有极短延迟；如果 `<Transition>` 动画出现时序问题，给 `defineAsyncComponent` 传 `loadingComponent` / `delay` 选项，不要回退到静态 import。MiniPlayer 必须保持静态 import（它依赖很轻且首屏底栏需始终可见，不能等异步加载）。

## SourcesPage 扫描默认值约定

`src/views/SourcesPage.vue` 的扫描设置弹窗在 `openScanSettings(source)` 中按音源类型设置 `scanOptions.readTags` 默认值；顶层 `scanOptions = ref<ScanOptions>({ readTags: true })` 仅作初始占位，实际使用前会被 `openScanSettings` 覆写。

### 规则

1. **WebDAV 默认关闭 `readTags`**：WebDAV 读标签需逐文件网络请求读取原生元数据，开启会明显变慢、易卡顿。`src/features/library/scanner.ts` 中 `options.readTags` 决定是否调用 `readWebDavAudioTags` / `WebDavNative.readMetadata`，关闭时回退为文件名标题。
2. **本地音源默认开启 `readTags`**：本地元数据读取无网络开销，无需回归。
3. **用户仍可手动切换**：弹窗中的 `HSwitch v-model="scanOptions.readTags"` 不受默认值影响，用户可随时打开/关闭。

### 正确实现

```ts
const openScanSettings = (source: SourceItem): void => {
  selectedScanSource.value = source
  // WebDAV 默认关闭读标签（避免网络逐文件读取导致慢/卡）；本地默认开启
  scanOptions.value = { readTags: source.type !== 'webdav' }
  resetScanProgress()
  isScanSettingsOpen.value = true
}
```

### 避免

- 在 `openScanSettings` 中对所有音源统一写死 `{ readTags: true }`——会让 WebDAV 扫描默认逐文件读标签。
- 把 WebDAV 默认 `false` 提到 scanner 层（`scanSourceLibrary`/`readTagsSafely`）——读标签与否是扫描期用户可调的偏好，应由调用方在 `ScanOptions` 中明确传入，scanner 只负责如实执行 `options.readTags`。
- 删除顶层 `scanOptions` 初始值或改为函数式默认——弹窗打开前必经 `openScanSettings` 覆写，初始占位值保持 `{ readTags: true }` 即可，无需引入额外抽象。

参考文件：`src/views/SourcesPage.vue`、`src/features/library/scanner.ts`、`src/features/library/types.ts`（`ScanOptions`）。

## QueuePage / PlaylistDetailPage 虚拟列表约定

`QueuePage.vue` 和 `PlaylistDetailPage.vue` 的长队列/歌单列表使用 `@tanstack/vue-virtual`，避免一次挂载全量行。

### 规则

1. 虚拟器必须使用真实原生滚动容器，容器设置 `overflow: auto`、`min-height: 0` 和 `box-sizing: border-box`。
2. 保留原生 HTML 包装行，行带 `data-index`，通过 `measureElement` 测量。
3. 不要为虚拟器未就绪状态回退渲染完整数组，否则大列表首帧仍会创建全量 DOM。
4. 需要删除时使用明确的行尾按钮，并信任 `@click.stop` 阻止按钮事件触发整行播放。
5. 队列必须保留当前项 `aria-current`、当前项定位、播放、删除、清空和空态；歌单必须保留播放全部、单曲播放、移除、封面、空态和数据更新刷新。

参考文件：`src/views/QueuePage.vue`、`src/views/PlaylistDetailPage.vue`、`@tanstack/vue-virtual`。

## SourcesPage 虚拟列表行高测量约定

`src/views/SourcesPage.vue` 使用 `@tanstack/vue-virtual` 渲染音源卡片（已从 `ion-card` 迁移为 `HCard`）。窄屏/竖屏下卡片实际高度会因副标题、内边距等超过固定估算值；若只依赖 `estimateSize` 而不实测行高，后续行的 `translateY` 会偏小，导致卡片互相覆盖并遮挡扫描按钮。

### 规则

1. **保留 `estimateSize` 仅作首屏占位**，挂载后必须用 `measureElement` 用真实高度重算。
2. **测量目标必须是原生 HTML 元素**，不要直接测 Vue 组件实例（组件本身的元素结构与库的 `HTMLElement` 期望不一致）。正确做法是外包一层原生 `div` 作为虚拟行。
3. **虚拟行必须带 `data-index`**（与库默认 `indexAttribute` 一致），并通过 ref 回调调用 `rowVirtualizer.value.measureElement(...)`。
4. **行间距必须进入实测高度**：不要用 `margin` 做绝对定位行的垂直间距（`offsetHeight` 通常不计入 margin）。应把间距放进测量容器的 `padding` / `border-box`。
5. **ref 回调只向 `measureElement` 传入 `HTMLElement | null`**：卸载时传入 `null`，由库清理断开节点。

### 正确实现

```vue
<div
  v-for="virtualRow in virtualRows"
  :key="sources[virtualRow.index].id"
  :ref="measureVirtualRow"
  class="source-card-row"
  :data-index="virtualRow.index"
  :style="{ transform: `translateY(${virtualRow.start}px)` }"
>
  <HCard class="source-card">...</HCard>
</div>
```

```ts
const measureVirtualRow = (element: Element | ComponentPublicInstance | null): void => {
  rowVirtualizer.value.measureElement(element instanceof HTMLElement ? element : null)
}
```

```css
.source-card-row {
  position: absolute;
  top: 0;
  left: 12px;
  right: 12px;
  box-sizing: border-box;
  padding-block: 8px; /* 间距计入实测高度 */
}

.source-card {
  min-height: 100px;
  margin: 0; /* 不要用 margin 承担行间距 */
  /* HCard 默认 margin:0，无需额外重置 */
}
```

### 避免

- 只写 `estimateSize: () => 148`、不接 `measureElement` / `data-index`。
- 把 `position: absolute` + `translateY` 直接绑在 `HCard` 上并指望固定估算值在窄屏仍准确。
- 用 `margin: 8px 0` 做绝对定位行间距，导致测量高度小于实际占位。
- 在 ref 回调里直接访问组件实例的 `$el` 而不做 `HTMLElement` 收窄（类型不安全，且测量目标应优先选原生包装节点）。

参考文件：`src/views/SourcesPage.vue`、`@tanstack/vue-virtual`。

—

## Accessibility

The current codebase includes a few baseline patterns that should be preserved:

- Decorative icons use `aria-hidden="true"` in `src/views/TabsPage.vue`
- External links in `src/components/ExploreContainer.vue` include `target="_blank"` with `rel="noopener noreferrer"`
- Tab / 导航按钮使用可见文本标签（`<span>` / 原生文案），不依赖 `IonLabel`

Preserve these practices when extending the UI.

Avoid:

- Icon-only controls without accessible labels
- External links opened in new tabs without `rel="noopener noreferrer"`
- Replacing visible labels with only decorative icons in navigation

---

## Common Mistakes

Given the current app shape, common mistakes to avoid are:

- Putting route logic inside page components instead of `src/router/index.ts`
- Adding `/player` or `/queue` routes for immersive playback; these surfaces are global overlays, not route pages
- Reintroducing any `@ionic/*` / `ionicons` 依赖或 `ion-*` 标签（本任务已完全脱离 Ionic）
- Mixing global theme concerns into component styles (all styles must be Tailwind utility classes or tailwind.css-scoped rules)
- Introducing new architectural layers (store, services, composables) without an actual need in the task
- 给 `MPage` / `.m-page` 加 `contain`（会重建 fixed 包含块，导致 HToast/HBottomSheet 等浮层偏移）
- 在 TabsPage 父路由壳上再套一层 `MPage` 导致重复导航 chrome 或堆叠布局
- 用 `HIconButton`（0.0.4 已移除）代替 `HButton is-icon-only`
- Using `@click.stop` on nested `HButton` controls inside a clickable parent; the parent handler does not need manual event-path filtering.
- Hiding `MiniPlayer` with `v-if` while a player overlay is open; keep it mounted behind the overlay and disable interaction to avoid close-animation flicker
- Setting immersive `.cover` width without a height-based cap（窄屏只写 `min(72vw, 100%, 340px)` 或宽屏只写 `min(40vw, 320px)`）while `.cover-slot` clamps height via `max-height: min(…dvh, …)`；矮高/横屏时正方形高度被 clamp、宽度不变 → 封面被压成长方形。窄屏与宽屏 `.cover` width 都必须同步含 dvh/`max-height` 对齐的上限
- 竖屏控制页用 `justify-between` + `.cover-slot { flex-grow: 1 }`（或 `flex: 1 1 auto`）把封面顶到上半区并在槽内制造松散留白；应 `justify-content: center` + `.cover-slot { flex: 0 1 auto }` 整体居中收紧
- 矮屏控制页只缩按钮却不收 panel padding / `info-panel-inner` gap / 进度热区，导致控制区仍占过多垂直空间、封面槽位被挤；或为腾空间隐藏模式栏/进度——应分层 `max-height` 收紧尺寸，保留全部控件
- SongsPage FAB 跳转在 `scrollToIndex` 后再 `scrollIntoView`，或给虚拟行加 `scroll-mt`/`scroll-margin-top`「扣 navbar」——滚动端口已在 chrome 下方，二次偏移会在连点时把当前行下移；只保留 virtualizer `scrollToIndex`

---

## 标记类与 Tailwind Utility 共存原则

组件的 `class` 属性中有两类类名：以 utility 为主的 Tailwind 语义类（无前缀或有 `[` `(` 任意值 syntax）传给 class variable，另有少量非 Tailwind 自定义标记类。零 scoped CSS 的规则对这些标记类提出了留存标准：

- **保留**的标记类必须满足至少一项：
  1. `src/theme/tailwind.css` 中存在对应的全局级联锚点（e.g. `.player-overlay .cover`、`.lyric-panel`、`.controls`、`.info-panel`、`.m-page`、`.m-content`、`.empty-state` 等全局规则依赖）。
  2. Vue 动态 `:class` 绑定产生的状态标识（如 `is-playing`、`is-active`、`is-dragging`、`is-overlay-active`、`is-player-visible`、`is-visible`、`is-empty`）。

- **移除**的条件：标记类同时满足：
  a. 不是 tailwind.css 全局规则锚点
  b. 没有运行时代码依赖（项目已无 `classList.contains` / `querySelector` 标记类查询，无 `composedPath` 代理）
  c. 不是 Vue 动态状态类

- **项目已删除全部自动化测试**。标记类清理判定不再考虑测试依赖维度。

**判定参考表（当前已执行）**：

| 标记类 | 锚定变量 | 判定 | 状态 |
|---|---|---|---|
| `.player-overlay` | tailwind.css 全局规则 | ✅ 保留 | — |
| `.lyric-panel`、`.lyric-player`、`.lyric-*` | tailwind.css 全局规则 | ✅ 保留 | 已清手势 class 查询 |
| `.progress-range`、`.progress-area` | tailwind.css 全局规则 | ✅ 保留 | 已清 class 查询 |
| `.cover`、`.cover-slot`、`.controls`、`.mode-bar`、`.info-panel*`、`.panel`、`.panels`、`.empty-state`、`.fallback-background`、`.song-info`、`.time-row`、`.play-toggle` | tailwind.css 全局规则 | ✅ 保留 | — |
| `.m-page`、`.m-content`、`.m-content--fullscreen` | tailwind.css 全局规则 | ✅ 保留 | — |
| `is-playing`、`is-empty`、`is-overlay-active`、`is-player-visible`、`is-dragging`、`is-active`、`is-visible` | Vue `:class` 状态绑定 | ✅ 保留 | — |
|data-song-id`, `data-index` | Vue 属性绑定，非 class | — | 保留（行身份标识）|
| `.player-actions` / `.more-button` / `.remove-button` | 原 JS `composedPath` 代理（已改用 `@click.stop`） | ❌ 移除 | 已删 |
| `.mini-player` / `.app-mini-player` / `.app-player-page` | 原测试断言 | ❌ 移除 | 已删 |
| `.immersive-shell` / `.amll-background{,-render}` | 原测试断言 | ❌ 移除 | 已删 |
| `.m-cover` | 原测试断言 + 组件根 class（无 CSS/js 锚点） | ❌ 移除 | 已删 |
