# Component Guidelines

> How components are built in this project.

---

## Overview

Components in this repository are Vue single-file components using the Composition API via `<script setup lang="ts">`. The current codebase is simple and mostly follows the default Ionic Vue starter patterns.

Reference files:

- `src/App.vue`
- `src/components/ExploreContainer.vue`
- `src/views/Tab1Page.vue`
- `src/views/TabsPage.vue`

---

## Component Structure

Use the standard Vue SFC layout already present in the repo:

1. `<template>` first
2. `<script setup lang="ts">` second
3. `<style scoped>` only when the component needs local styles

Examples:

- `src/App.vue` shows a minimal shell component with only Ionic wrapper markup.
- `src/components/ExploreContainer.vue` shows template + script + scoped style.
- `src/views/Tab1Page.vue` shows a route page with Ionic layout components and no local style block.

---

## Ionic Page Pattern

Route-level pages should follow the Ionic page container structure used by the existing views:

- Wrap the page in `<ion-page>`.
- Use `<ion-header>`, `<ion-toolbar>`, and `<ion-title>` for page headers.
- Use `<ion-content :fullscreen="true">` for page content.
- For tab pages, keep the collapsed large-title header pattern when appropriate.

Reference files:

- `src/views/Tab1Page.vue`
- `src/views/Tab2Page.vue`
- `src/views/Tab3Page.vue`
- `src/views/TabsPage.vue`

Avoid building route-level content pages without `ion-page`, because that breaks Ionic layout expectations. Exception: a parent route shell such as `src/views/TabsPage.vue` may be a plain Vue layout container when it exists only to place navigation chrome around child pages rendered by `<RouterView />`; this avoids duplicate Ionic page stacking in nested routes.

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
- Explicit imports for all used Ionic components

Avoid:

- Implicit global component assumptions
- Large untyped prop bags
- Passing unrelated page state through many component layers in this small app

---

## Import Conventions

Import all Ionic components explicitly inside each SFC.

Examples:

- `src/App.vue` imports `IonApp` and `IonRouterOutlet`
- `src/views/Tab1Page.vue` imports `IonPage`, `IonHeader`, `IonToolbar`, `IonTitle`, `IonContent`
- `src/views/TabsPage.vue` imports tab and icon-related Ionic components plus icon symbols from `ionicons/icons`

Also prefer the `@/` alias for application imports from `src/`:

- `import ExploreContainer from '@/components/ExploreContainer.vue'`
- lazy route import `import('@/views/Tab1Page.vue')`

---

## Responsive Breakpoint Convention

项目使用以下全局 CSS 变量在宽屏下适配平板布局：

- `--muses-breakpoint-tablet: 768px` — 平板断点宽度（定义于 `src/theme/variables.css`）
- `--muses-content-max-width: 720px` — 内容最大宽度限位居中

### 断点约定的规则

1. **全局变量统一在 `src/theme/variables.css` 的 `:root` 中定义**，单一来源，所有页面引用 `var(--muses-*)`。
2. **`@media (min-width: XXX)` 条件中不可使用 `var()`** — CSS 标准不允许。在 `@media` 中直接使用硬编码的 `768px`；`var()` 只用于属性值部分（如 `max-width: var(--muses-content-max-width)`）。
3. **宽屏下隐藏元素**：对窄屏专属元素（如 `ion-tab-bar`）使用 `@media (min-width: 768px) { … display: none }`。
4. **窄屏零回归**：所有平板改造限定在 `@media (min-width: 768px)` 内；窄屏下不加任何额外样式。

### 当前平板组件模式

- **导航 Shell**：`src/views/TabsPage.vue` 使用普通 Vue 布局容器作为父级 shell，不使用 `ion-page` 包裹父路由布局；子页面继续保留自己的 Ionic page 结构。
- **侧栏**：宽屏下由固定定位的普通 `<aside>` 提供左侧导航，右侧 `<main>` 渲染 `<RouterView />`；窄屏回落为普通 `<nav>` + `RouterLink` 底部导航。
- **避免 Split Pane**：当前 MuMu / Android WebView 环境中，`ion-split-pane` + `ion-menu` 曾触发白屏；不要在 `TabsPage.vue` 中恢复该结构，除非完成真机与 MuMu 回归验证。
- **列表多列**：`src/views/SongsPage.vue`（及 Albums/Artists 页）的 `<ion-list>` 外包 `<div class="list-grid">`，通过 CSS Grid `repeat(auto-fill, minmax(320px, 1fr))` 自动分列。
- **内容限位居中**：各列表页 `.tablet-content-limit` 和 `.list-grid` 在宽屏下 `max-width: var(--muses-content-max-width); margin-inline: auto`。

## Styling Gotchas

### ion-list 为 Web Component，CSS Grid 在外层无法布局子 ion-item

`ion-list` 是 Ionic Web Component，有 Shadow DOM 隔离。在外层 div 套 CSS Grid 后，`ion-list` 只是 grid 容器的第一个子项，不会将 `ion-item` 暴露为 grid item。

**修复**：在宽屏下给 `ion-list` 加 `display: contents;`，使 `ion-item` 成为 grid 容器的直接子元素：

```css
@media (min-width: 768px) {
  .list-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 1px;
    max-width: var(--muses-content-max-width);
    margin-inline: auto;
  }
  .list-grid > ion-list {
    display: contents;
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

- Global framework/theme CSS is loaded once in `src/main.ts`
- Project theme overrides belong in `src/theme/variables.css`
- Component-local styling can use `<style scoped>` when needed

Reference files:

- `src/main.ts`
- `src/theme/variables.css`
- `src/components/ExploreContainer.vue`

Do not duplicate Ionic core or utility CSS imports inside page components.

---

## MiniPlayer 与播放器 Overlay 约定

`src/components/MiniPlayer.vue` 是应用级固定底栏，由 `src/App.vue` 始终挂载；播放器和队列使用全局 overlay 状态显示，不再通过 `/player` 或 `/queue` 路由打开。

### 样式约定

- 底栏占满屏幕宽度，固定在移动端底部导航栏上方。
- 底栏本身不使用圆角和阴影，仅保留顶部边线分隔内容。
- 封面容器圆角与歌曲列表一致，使用 `border-radius: 10px`。
- 无当前歌曲或无封面时展示稳定占位封面与占位文案，避免播放状态为空时底栏跳动或消失。
- `MiniPlayer` 不要因 overlay 打开而用 `v-if` 卸载；overlay 打开时只禁用交互（例如 `pointer-events: none`），避免下滑关闭时底栏闪烁。

### 交互约定

- 点击底栏主体调用 `openPlayerOverlay()`，不能改变当前路由 URL。
- 点击播放/暂停按钮只控制播放状态，不能触发打开播放器 overlay。
- 点击队列按钮调用 `openQueueOverlay()`，不能改变当前路由 URL，也不能触发打开播放器 overlay。
- 对 Ionic `ion-button` 不要只依赖 `@click.stop`；按钮内部事件可能穿过 Web Component 边界。父级主体点击处理需要检查 `event.composedPath()`，如果事件路径包含 `.player-actions` 就直接忽略。

```ts
const openPlayerPage = (event: MouseEvent | KeyboardEvent) => {
  if (event.composedPath().some((target) => target instanceof Element && target.classList.contains('player-actions'))) {
    return
  }
  openPlayerOverlay()
}
```

### Overlay 页面约定

- `PlayerPage.vue` 和 `QueuePage.vue` 是全局 overlay 内容组件，由 `App.vue` 渲染在 `ion-router-outlet` 之后；不要在 `src/router/index.ts` 中新增 `/player` 或 `/queue` 路由。
- 打开播放器/队列 overlay 时底层 tabs 路由页面必须保持存在，以支持下滑收起露出真实底层页面。
- 播放器 overlay 顶部不显示返回/收起按钮，也不展示「正在播放」标题；顶部仅保留安全区留白。关闭通过下滑手势、Android back 键或显式 overlay 状态完成。
- 下滑收起播放器时移动 overlay 内容层，不要移动 Ionic 路由页或依赖透明路由页露出缓存层，否则容易出现黑屏或重复页面。
- 沉浸式控制页布局自上而下：大封面 → 歌名/歌手 → 进度条 → 主控制（上一曲/播放暂停/下一曲）→ 次要控制（循环/随机/队列）。
- 循环/随机/队列使用纯图标按钮，必须提供 `aria-label`；激活态用高亮或更高不透明度表达，不要依赖可见文字标签。
- 控制页必须一屏适配：`immersive-shell` / panels 固定 `height: 100dvh`，`overflow: hidden`；封面用弹性槽位缩放，禁止页面纵向滚动。
- 打开播放器/队列 overlay 时必须锁定底层路由页交互与滚动：`ion-router-outlet` 设 `pointer-events: none`，`body.muses-overlay-open ion-router-outlet ion-content` 禁用滚动；不要锁住队列 overlay 自己的 `ion-content`。
- 播放器 overlay 自身使用 `touch-action: none`，并在非原生控件（非 input/range）上对 `touchmove` 调用 `preventDefault`，防止滑动穿透到底层歌曲列表；进度条保留可拖动。

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

**Related**: `vite.config.ts` 的 `build.rollupOptions.output.manualChunks` 已将 `@applemusic-like-lyrics` + `@pixi` 锁定到 `amll-pixi` chunk、`@ionic/vue`+`ionicons` 到 `ionic` chunk、`vue`+`vue-router` 到 `vue-vendor` chunk，利于长期缓存。新增任何重量级（>50KB）第三方库到 overlay 页面时，同样适用此约定；不要再用静态 import 把它拽进主 bundle。

**Gotcha**: 异步组件首次解析有极短延迟；如果 `<Transition>` 动画出现时序问题，给 `defineAsyncComponent` 传 `loadingComponent` / `delay` 选项，不要回退到静态 import。MiniPlayer 必须保持静态 import（它依赖很轻且首屏底栏需始终可见，不能等异步加载）。

## Accessibility

The current codebase includes a few baseline patterns that should be preserved:

- Decorative icons use `aria-hidden="true"` in `src/views/TabsPage.vue`
- External links in `src/components/ExploreContainer.vue` include `target="_blank"` with `rel="noopener noreferrer"`
- Tab buttons use visible `IonLabel` text

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
- Forgetting to import Ionic components explicitly in the SFC script block
- Mixing global theme concerns into component-local styles
- Introducing new architectural layers (store, services, composables) without an actual need in the task
- Wrapping a nested parent route shell in `ion-page` when child route pages already provide Ionic page containers; in Android WebView this can cause duplicate navigation chrome or stacked layouts
- Reintroducing `ion-split-pane` / `ion-menu` for the main tabs shell without MuMu regression testing; this previously caused a white screen in the Android emulator
- Using `ion-tab-bar` / `ion-tab-button` outside an `ion-tabs` shell in `TabsPage.vue`; in the custom parent route shell, use a plain `<nav>` with `RouterLink` to avoid missing or duplicated mobile bottom navigation
- Relying only on `@click.stop` for nested `ion-button` controls inside a clickable parent; guard the parent handler with `event.composedPath()` so button clicks do not trigger parent navigation
- Hiding `MiniPlayer` with `v-if` while a player overlay is open; keep it mounted behind the overlay and disable interaction to avoid close-animation flicker
