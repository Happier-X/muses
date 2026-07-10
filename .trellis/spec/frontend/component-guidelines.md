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
- Forgetting to import Ionic components explicitly in the SFC script block
- Mixing global theme concerns into component-local styles
- Introducing new architectural layers (store, services, composables) without an actual need in the task
- Wrapping a nested parent route shell in `ion-page` when child route pages already provide Ionic page containers; in Android WebView this can cause duplicate navigation chrome or stacked layouts
- Reintroducing `ion-split-pane` / `ion-menu` for the main tabs shell without MuMu regression testing; this previously caused a white screen in the Android emulator
- Using `ion-tab-bar` / `ion-tab-button` outside an `ion-tabs` shell in `TabsPage.vue`; in the custom parent route shell, use a plain `<nav>` with `RouterLink` to avoid missing or duplicated mobile bottom navigation
