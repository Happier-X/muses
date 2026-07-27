# 脱离 Ionic 框架迁移设计

## 1. 架构总览

### 1.1 迁移后依赖树

```
Capacitor (原生壳，不变)
  └─ Vue 3 + vue-router (createWebHistory)
       ├─ happier-ui 0.0.6 (组件 + tokens + 暗色态)
       ├─ Tailwind v4 (@tailwindcss/vite)
       └─ @tanstack/vue-virtual / @formkit/auto-animate (第三方)
```

去掉的：`@ionic/vue`、`@ionic/vue-router`、`ionicons`、Ionic 全局 CSS。

### 1.2 页面骨架映射

| 当前模式 | 文件 | 迁移目标 | 滚动管理 |
|---------|------|---------|---------|
| `<ion-page>` + `<ion-content>` 直接 | PlaylistDetailPage、PlaylistsPage、SongsPage、SourcesPage、SettingsPage | 自建 div 骨架（flex col + 100vh） + `<MContent>` 或自定义滚动区 | MContent overflow auto；虚拟列表页各自管理 |
| `<m-page>`（含 ion-page/ion-content） | AlbumsPage、ArtistsPage | `<m-page>` 改为自建骨架 | MContent overflow auto |
| `<ion-app>` + `<ion-router-outlet>` | App.vue | `<div.app-shell>` + `<RouterView>` | 无直接滚动 |
| overlay 自定义容器 | PlayerPage、QueuePage | 保持自定义，移除 ion-content | 各自管理 |
| TabsPage（ion-list/ion-item 侧栏） | TabsPage.vue | `<nav>` + `<RouterLink>` | 侧栏 overflow auto |

### 1.3 执行顺序（编辑策略）

执行顺序：**先迁所有 `ion-*` 组件 → 再拔根**。细化为：

```
第 1 步：升级 happier-ui 0.0.3 → 0.0.6 + 迁移 HIconButton → HButton
第 2 步：迁移 ion-button/ion-range/ion-fab（纯组件替换，无结构依赖）
第 3 步：迁移 ion-item/ion-label/ion-list（替换为 HCellGroup/HCell/原生结构）
第 4 步：迁移 ion-note → 带 muted 颜色的 `<span>`
第 5 步：迁移 ion-action-sheet/ion-alert → HBottomSheet/HDialog
第 6 步：迁移 ion-modal（SourcesPage ×4）→ HBottomSheet
第 7 步：建 MPage/MContent 自建骨架（替换 ion-page/ion-content）
第 8 步：替换 App.vue（IonApp/IonRouterOutlet → app-shell/RouterView）
第 9 步：改 router/index.ts（@ionic/vue-router → vue-router）
第 10 步：替换 useIonRouter → vue-router（PlaylistDetailPage）
第 11 步：替换 onIonViewWillEnter → onMounted（×5）
第 12 步：safe-area `--ion-*` → `env(safe-area-inset-*)`
第 13 步：颜色变量桥接清理（variables.css + SourcesPage/TabsPage 残留）
第 14 步：移除 main.ts 中 IonicVue 插件 + 全局 CSS 导入
第 15 步：修改 overlay 锁滚动选择器
第 16 步：从 package.json 删除 @ionic/vue/@ionic/vue-router/ionicons
第 17 步：全局搜索确认零残留
第 18 步：验证（tsc + build + 启动 + 各页面回归）
```

**关键原则**：步骤 1–6 可在 Ionic 全局 CSS 仍在的情况下独立完成和验证；步骤 7–14 开始拔根，此时所有 `ion-*` 标签已不存在，不会失去 Ionic 样式而崩。步骤 14（移除 Ionic 插件 + CSS）放在最末尾，确保前面所有组件迁移已完成。

---

## 2. MPage / MContent 自建骨架

### 2.1 MPage.vue 契约（替换当前 `<ion-page>`）

```vue
<!-- 新 MPage.vue -->
<template>
  <div class="m-page">
    <h-nav-bar :fixed="false">
      <template v-if="$slots.start" #left>
        <slot name="start" />
      </template>
      <template #title>
        <slot name="title" />
      </template>
      <template v-if="$slots.end" #right>
        <slot name="end" />
      </template>
    </h-nav-bar>
    <!-- 新增 slot 用于 navbar 与 content 之间的中间层（SongsPage shuffle-bar） -->
    <template v-if="$slots.subnavbar">
      <slot name="subnavbar" />
    </template>
    <m-content :fullscreen="fullscreen">
      <slot />
    </m-content>
  </div>
</template>
```

**样式**（全局未 scoped，因为 `.m-page` 被 `<body>` 级选择器引用）：

```css
.m-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  /* ⚠️ 故意不加 contain，避免重建 fixed 包含块导致浮层偏移 */
}
```

**Props**：保留 `fullscreen?: boolean`（默认 `false`），传给 MContent。

**向后兼容**：AlbumsPage/ArtistsPage 完全不需要改模板——slot 名相同（title/start/end），fullscreen prop 同上。

**新增 `#subnavbar` slot**：SongsPage 有 shuffle-bar 位于 navbar 下方、content 上方，用此 slot 承载。

### 2.2 MContent.vue 契约（替换 `<ion-content>`）

```vue
<!-- 新建 MContent.vue -->
<template>
  <div
    class="m-content"
    :class="{ 'm-content--fullscreen': fullscreen }"
  >
    <slot />
  </div>
</template>
```

**样式**（不 scoped，因为全局选择器需要引用）：

```css
.m-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  /* 无 contain，不重建包含块 */
}

.m-content--fullscreen {
  /* 当前 fullscreen 在自建骨架中实际即为全高，无 padding-bottom 差异。
     保留 prop 避免破坏 existing caller props 契约。 */
}
```

设计理由：
- **虚拟列表页**（SongsPage、QueuePage、PlaylistDetailPage）：MContent 只提供 flex 空间撑满父容器，不做 scroll。内部 `.song-list`/`.queue-list`/`.playlist-list` 自身 `overflow: auto` + `height: 100%`，是虚拟列表的 `getScrollElement()`。
- **普通滚动页**（SettingsPage、PlaylistsPage、AlbumsPage、ArtistsPage）：MContent 自身 `overflow: auto`，内容超出自动滚动。
- **SourcesPage**（虚拟列表 + HBottomSheet 弹窗）：列表部分用 `.source-list` 自管理滚动，外部 MContent `overflow: auto` 但内容不超过 100% 时不滚动。
- **不使用 MPage/MContent 的页面**：PlayerPage（custom overlay）、QueuePage（custom overlay）依然自建滚动。

### 2.3 不使用 MPage 的页面骨架

| 页面 | 当前骨架 | 新骨架 |
|------|---------|-------|
| **PlaylistDetailPage** | `<ion-page>` + `<h-nav-bar>` + `<ion-content>` | `<div class="m-page">` + `<h-nav-bar>` + `<div class="m-content" style="overflow:hidden">` + 内部虚拟列表 |
| **PlaylistsPage** | `<ion-page>` + `<h-nav-bar>` + `<ion-content>` | `<m-page>`（标准） |
| **SongsPage** | `<ion-page>` + `<h-nav-bar>` + shuffle-bar + `<ion-content>` | `<m-page>` + `#subnavbar` slot（shuffle-bar）+ `<div.m-content style="overflow:hidden">` |
| **SettingsPage** | `<ion-page>` + `<h-nav-bar>` + `<ion-content>` | `<m-page>`（标准） |
| **SourcesPage** | `<ion-page>` + `<h-nav-bar>` + `<ion-content>` | `<m-page>`（标准；HBottomSheet/HDialog 不依赖 content） |
| **AlbumsPage** | `<m-page>`（旧，改内容不变） | `<m-page>`（新，slot 相同 → 零改） |
| **ArtistsPage** | `<m-page>`（旧，改内容不变） | `<m-page>`（新，slot 相同 → 零改） |

SongPage 的 `ion-content` class 选择器 `.songs-content` 改为 `.m-content` 或内联样式。Shuffle-bar 的 background 保持 `--muses-color-surface`。

---

## 3. App.vue 替换

### 3.1 新 App.vue 结构

```vue
<template>
  <div class="app-shell" :class="{ 'has-global-overlay': hasGlobalOverlay }">
    <RouterView class="app-router-view" />
    <MiniPlayer
      class="app-mini-player"
      :class="{ 'is-overlay-active': hasGlobalOverlay }"
      :aria-hidden="hasGlobalOverlay"
    />
    <PlayerPage
      v-if="keepPlayerPageMounted"
      class="app-player-page"
      :class="{ 'is-player-visible': playerOverlayVisible }"
    />
    <Transition name="queue-overlay">
      <QueuePage v-if="queueOverlayVisible" />
    </Transition>
  </div>
</template>
```

**变化点**：
- `<ion-app>` → `<div class="app-shell">`
- `<ion-router-outlet>` → `<RouterView>`
- `has-global-overlay` class 挂到 `.app-shell` 而非 `<ion-app>`
- 删除 `import { IonApp, IonRouterOutlet } from '@ionic/vue'`

### 3.2 样式替代

`ion-app` 提供的功能 → `.app-shell` CSS：

```css
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  color: var(--h-color-ink);
  background: var(--h-color-surface);
}
```

### 3.3 Overlay 锁滚动选择器

当前：
```css
body.muses-overlay-open ion-router-outlet ion-content {
  --overflow: hidden;
  pointer-events: none;
  overscroll-behavior: none;
}
```

改为（适应 RouterView 不产生 DOM 标签的现实）：

```css
body.muses-overlay-open .app-router-view {
  pointer-events: none;
  overflow: hidden;
  overscroll-behavior: none;
}
```

同时保留对 html/body 的：
```css
html.muses-overlay-open,
body.muses-overlay-open {
  overflow: hidden !important;
  overscroll-behavior: none;
}
```

注意：`.app-router-view` 作用于 `<RouterView>` 的 wrapper 容器（Vue 会将其渲染为 comment node + 子组件树）。实际上 `<RouterView>` 本身不产生 DOM 元素，所以需要用 `<div class="app-router-view">` 包一层：

```vue
<div class="app-router-view">
  <RouterView />
</div>
```

### 3.4 保活逻辑 (PlayerPage)

当前 `ion-app` scoped 下的 `.app-player-page:not(.is-player-visible)` 样式维持 `transform: translateY(100%)` + `visibility: hidden` + `contain: paint`。更改后同一逻辑挂到 `.app-shell` 下，CSS 选择器不变（只改父容器类名），样式内容完全保留。

### 3.5 状态栏同步 & 返回键监听

无结构性变化：Capacitor `App.addListener('backButton')` 和 `StatusBar.setStyle()` 独立于 Ionic。App.vue 脚本部分仅移除 `IonApp`/`IonRouterOutlet` 导入。

### 3.6 全局 overlay 锁定 MiniPlayer 交互

当前 `ion-app.has-global-overlay .app-router-outlet` → 改为 `.app-shell.has-global-overlay .app-router-view`。MiniPlayer 自身的 `.is-overlay-active` class 不变。QueuePage Transition 动画不变。

---

## 4. 路由迁移

### 4.1 router/index.ts

```diff
-import { createRouter } from '@ionic/vue-router'
+import { createRouter } from 'vue-router'
import { createWebHistory } from 'vue-router'
```

路由表完全不变。`createWebHistory` 来自 `vue-router`，与 `@ionic/vue-router` 内部的实现一致。

### 4.2 main.ts（Vue 插件）

`IonicVue` 插件执行 `app.use(IonicVue)` → **删除**。IonicVue.install 只做两件事：
1. 调用 `initialize(config)`  
2. 给 `<html>` 加 `ion-ce` 类

这两件事迁移后均不需要：`initialize` 无实际功能（仅检查 Shadow DOM），`ion-ce` 类无 muses 代码依赖。

### 4.3 路由表变更

无变更。当前：
```ts
{ path: '/', redirect: '/tabs/songs' }
{ path: '/tabs', component: TabsPage, children: [
  '/songs', '/albums', '/artists', '/playlists',
  '/playlists/:id', '/sources', '/settings'
] }
```

保持不变。

---

## 5. 生命周期迁移 (`onIonViewWillEnter` ×5)

### 5.1 先决条件确认

TabsPage `<RouterView />` **无 `<KeepAlive>`**。每次 tab 切换或 detail 返回都 unmount→remount 子组件。因此 `onMounted` + `onUnmounted` 行为完全等价于 `onIonViewWillEnter` + `onIonViewDidLeave`。

**PlaylistDetailPage 特例**：路径 `/tabs/playlists/:id`，不同歌单之间切换（A → B）时 vue-router 会**复用**同一组件实例（路径模式相同、仅 param 变化）。此时 `onMounted` 不会再次触发，需要 `watch(route.params.id, refresh)` 补充。

### 5.2 迁移对照表

| 页面 | 当前代码 | 替换为 | 额外处理 |
|------|---------|--------|---------|
| AlbumsPage:55 | `onMounted(refreshSongs)` + `onIonViewWillEnter(refreshSongs)` | 删除 onIonViewWillEnter，保留 onMounted | 无 |
| ArtistsPage:67 | `onMounted(refreshSongs)` + `onIonViewWillEnter(refreshSongs)` | 同 | 无 |
| PlaylistsPage:235 | `onMounted(refresh)` + `window.addEventListener(...)` + `onIonViewWillEnter(refresh)` | 删除 onIonViewWillEnter | 无（事件监听器已在 onMounted/onUnmounted 中负责跨实例刷新） |
| SongsPage:385 | `onMounted(refreshSongs)` + `onIonViewWillEnter(refreshSongs)` | 删除 onIonViewWillEnter | 无 |
| PlaylistDetailPage:208 | `onMounted(refresh)` + `onIonViewWillEnter(refresh)` | 删除 onIonViewWillEnter，**添加** `watch(playlistId, refresh)` | route param 变化时刷新 |

PlaylistDetailPage 的 `watch` 具体写法：
```ts
watch(playlistId, (newId, oldId) => {
  if (newId !== oldId) refresh()
})
```
初始加载由 `onMounted` 触发，`immediate: true` 不需要因为 onMounted 已经调了。

**注意**：`PlaylistDetailPage` 当前已有 `onMounted(refresh())` + 事件监听器（`PLAYLISTS_UPDATED_EVENT`/`SONGS_UPDATED_EVENT`）。只需删除 `onIonViewWillEnter` 的导入和调用即可，`watch` 需要在 script setup 中添加。

---

## 6. `useIonRouter` 替换（PlaylistDetailPage）

### 6.1 当前逻辑

```ts
const ionRouter = useIonRouter()
const goBack = (): void => {
  if (ionRouter.canGoBack()) {
    ionRouter.back()
    return
  }
  ionRouter.navigate('/tabs/playlists', 'back', 'pop')
}
```

### 6.2 替换逻辑

```ts
const router = useRouter()
const goBack = (): void => {
  // window.history.length > 1 表示有前向历史 → 可安全 back
  if (window.history.length > 1) {
    router.back()
    return
  }
  void router.replace('/tabs/playlists')
}
```

`window.history.length` 是检测浏览器会话历史的最简单方法，在 Capacitor WebView 中行为与原生一致。注意：`window.history.length` 在首次加载后总是 ≥1（当前条目），≥2 才有前向条目。

更精确的备选方案（如需要）：
```ts
const router = useRouter()
router.push('/tabs/playlists')
```
直接用 push 替代 back+fallback，因为 vue-router 默认会保留历史记录。但更接近原语义的方案是上述 if-else。

选择 **条件 back / fallback replace** 方案，因为 PlaylistDetailPage 的进入路径可能是直接打开 URL（此时没有「返回」栈），情况与原本 Ionic 的 canGoBack 行为一致。

---

## 7. safe-area 迁移

### 7.1 全局模式

所有 `var(--ion-safe-area-top, 0px)` → `env(safe-area-inset-top, 0px)`
所有 `var(--ion-safe-area-bottom, 0px)` → `env(safe-area-inset-bottom, 0px)`

### 7.2 替换清单

| 文件 | 行数 | 当前 | 替换为 |
|------|------|------|--------|
| MiniPlayer.vue | 110 | `bottom: calc(var(--muses-tab-bar-height) + var(--ion-safe-area-bottom, 0px))` | `env(safe-area-inset-bottom)` |
| MiniPlayer.vue | 169 | `bottom: var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| PlayerPage.vue | 886 | `var(--ion-safe-area-top, 0px)` | `env(safe-area-inset-top)` |
| PlayerPage.vue | 888 | `var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| PlayerPage.vue | 1162 | `var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| PlayerPage.vue | 1323 | `var(--ion-safe-area-top, 0px)` | `env(safe-area-inset-top)` |
| PlayerPage.vue | 1325 | `var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| PlayerPage.vue | 1398 | `var(--ion-safe-area-top, 0px)` | `env(safe-area-inset-top)` |
| PlayerPage.vue | 1400 | `var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| QueuePage.vue | 168 | `var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| SongsPage.vue | 416 | `var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| SongsPage.vue | 448 | `var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| SongsPage.vue | 455 | `var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| SongsPage.vue | 465 | `var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| TabsPage.vue | 112 | `var(--ion-safe-area-bottom, 0px)` | `env(safe-area-inset-bottom)` |
| TabsPage.vue | 141 | `var(--ion-safe-area-top, 0px)` | `env(safe-area-inset-top)` |

### 7.3 注意

`env(safe-area-inset-*)` 是 CSS `env()` 函数，需要 `viewport-fit=cover` 才生效。当前 `index.html` 第 12 行已设 `viewport-fit=cover`，无需调整。

---

## 8. 颜色变量桥接清理

### 8.1 variables.css

当前内容（`src/theme/variables.css`）：
```css
:root {
  --ion-color-primary: var(--h-color-primary);
  --ion-color-primary-rgb: var(--h-color-primary-rgb);
  --ion-color-primary-contrast: var(--h-color-primary-contrast);
  --ion-color-primary-contrast-rgb: var(--h-color-primary-contrast-rgb);
  --ion-color-primary-shade: var(--h-primary-600);
  --ion-color-primary-tint: var(--h-primary-400);
}
@media (prefers-color-scheme: dark) {
  :root {
    --ion-color-primary: var(--h-color-primary);
    /* ... 同上述桥接 */
  }
}
```

**迁移后删除整个文件**（或保留空壳）。前提：确认所有 `var(--ion-color-primary)` 引用已迁移。只有 TabsPage.vue 使用了 `--ion-color-primary`（侧栏激活色）→ 替换为 `--h-color-primary`（happier-ui 令牌）。

### 8.2 SourcesPage `--ion-color-medium`

4 个 `--ion-color-medium`（SourcesPage.vue:913/963/985/991）→ 统一 `--h-color-ink-muted`。

### 8.3 TabsPage `--ion-background-color`

TabsPage.vue:140：
```css
background: var(--ion-background-color, #fff);
```
→ `background: var(--h-color-surface);`

TabsPage.vue:120：
```css
--color: var(--ion-color-primary);
```
→ `--color: var(--h-color-primary);`

---

## 9. 浮层迁移合约

### 9.1 PlaylistsPage

| 当前 | 迁移目标 | 状态管理 | 注意事项 |
|------|---------|---------|---------|
| `ion-action-sheet`（歌单操作） | `<h-bottom-sheet>` | `isActionsOpen` ref，`@close` 清空 `activePlaylistId` | 按钮列表改为原生 `<button>` 行，destructive 用 `class="action-destructive"` |
| `ion-alert`（新建/重命名） | `<h-dialog>` + `<h-input>` | `isNameAlertOpen` ref，`nameAlertMode` 区分 create/rename | default slot 放 HInput，actions slot 放确认/取消 HButton |
| `ion-alert`（删除确认） | `<h-dialog>` | `isDeleteAlertOpen` ref | actions slot 放「取消」+「删除」(variant='danger') |

**HBottomSheet 结构示例**：
```vue
<h-bottom-sheet v-model="isActionsOpen" title="歌单操作">
  <div class="action-sheet-list">
    <button class="action-sheet-item" @click="onRename">重命名</button>
    <button class="action-sheet-item action-destructive" @click="onDelete">删除</button>
    <button class="action-sheet-item action-cancel" @click="isActionsOpen = false">取消</button>
  </div>
</h-bottom-sheet>
```

**HDialog 结构示例**（输入型）：
```vue
<h-dialog v-model="isNameAlertOpen" :title="nameAlertHeader">
  <h-input v-model="nameInput" placeholder="歌单名称" maxlength="80" />
  <template #actions>
    <h-button variant="ghost" @click="isNameAlertOpen = false">取消</h-button>
    <h-button variant="primary" @click="onNameConfirm">确定</h-button>
  </template>
</h-dialog>
```

### 9.2 SongsPage

| 当前 | 迁移目标 | 状态管理 | 注意事项 |
|------|---------|---------|---------|
| `ion-action-sheet` ×2（歌曲操作/选歌单） | `<h-bottom-sheet>` | `isSongActionsOpen` / `isPlaylistPickOpen` | 异步开选歌单 sheet 的逻辑（setTimeout 180ms）保留 |
| `ion-alert`（新建歌单） | `<h-dialog>` | `isCreatePlaylistOpen` | 输入型，同上 PlaylistsPage 模式 |
| `ion-fab` + `ion-fab-button` | `<h-floating-bubble>` | v-model:offset 控制定位 | `axis="lock"` 禁拖拽；底部定位需计算 mini-player + tab-bar 偏移；`aria-label` 转移到组件 |

**HFloatingBubble 定位计算**：
```ts
const fabOffset = computed<HFloatingBubbleOffset>(() => {
  const miniPlayerH = 64 // var(--muses-mini-player-height)
  const tabBarH = 64     // var(--muses-tab-bar-height)  
  const safeBottom = 0
  return { left: window.innerWidth - 64, top: window.innerHeight - miniPlayerH - tabBarH - safeBottom - 56 }
})
```
注意：`ionic-fab` 的 `slot="fixed"` 是 IonContent 特性（relative 定位内的 fixed 定位），迁移后需用 `HFloatingBubble` 的 `offset` prop 直接指定绝对坐标。

### 9.3 SourcesPage

| 当前 | 迁移目标 | 状态管理 | 注意事项 |
|------|---------|---------|---------|
| `ion-alert`（删除确认） | `<h-dialog>` | `isDeleteAlertOpen` | 同 PlaylistsPage |
| `ion-modal`（编辑音源） | `<h-bottom-sheet>` + `form` | `isEditModalOpen` → `isEditSheetOpen` | `closeOnOverlay` 在保存中设为 `false`；内容：h-nav-bar（可选）+ 表单 |
| `ion-modal`（扫描设置） | `<h-bottom-sheet>` | `isScanSettingsOpen` → `isScanSettingsSheetOpen` | 内容简单：HInput 描述 + HSwitch + HButton 开始扫描 |
| `ion-modal`（扫描进度） | `<h-bottom-sheet>` | `isScanProgressOpen` → `isScanProgressSheetOpen` | `closeOnOverlay` 在处理中发现/处理阶段为 `false`；内容：h-progress + 统计列表 |
| `ion-modal`（WebDAV 目录浏览） | `<h-bottom-sheet>` | `isWebDavModalOpen` → `isWebDavSheetOpen` |  |
| `ion-action-sheet`（添加音源） | 已完成迁移 | `isAddActionSheetOpen` | 已用 HBottomSheet |

新增 CSS 类用于替代 `ion-padding`（SourcesPage 当前 modal 内部有 `class="ion-padding"`）：
```css
.ion-padding {
  padding: 16px;
}
```
或在各 HBottomSheet 内容区直接写内敛 padding 避免全局污染。

### 9.4 PlayerPage

| 当前 | 迁移目标 | 数量 | 映射 |
|------|---------|------|------|
| `ion-button fill="clear" color="light" shape="round"` | `<h-button variant="ghost" is-icon-only shape="circle">` | 8 | 保留 icon-only 语义，color="light" 在白底上不适用（ghost 默认色） |
| `ion-range` | `<h-range>` | 1 | `:model-value`（v-model）+ `@change`（提交）+ `@drag-start` + `@drag-end` |

**PlayerPage `ion-button` 细节映射**：
```diff
-<ion-button fill="clear" color="light" shape="round" aria-label="上一曲" @click="onPrevious">
-  <h-icon slot="icon-only" :icon="previousIcon" variant="fill" />
-</ion-button>
+<h-button variant="ghost" is-icon-only shape="circle" aria-label="上一曲" @click="onPrevious">
+  <h-icon :icon="previousIcon" variant="fill" />
+</h-button>
```

注意：`color="light"` 在 PlayerPage 暗色大背景下展示为浅色图标（Ionic 全局 CSS 中 `.button-clear.ios` 默认色透明、`color="light"` 使图标变白）；happier-ui 的 `variant="ghost"` 在白色背景上是默认色图标，但 PlayerPage 的 `player-overlay` 背景可能是深色（取决于背景渲染器/fallback）。**需要确认 PlayerPage `.player-overlay` 的背景颜色**：当前 PlayerPage 的 `.immersive-shell` 有 `.fallback-background` 覆盖（暗色），`ion-button` 的 `color="light"` 实际是让图标在暗底上显示白色。迁移后 `variant="ghost"` 在暗色背景上自动为浅色文本，图标颜色由 `currentColor` 继承。若 visual 不正确，可通过 `color="white"` 或自定义 class：

```css
.player-overlay .h-button--ghost {
  color: #fff;
}
```

**PlayerPage `ion-range` 迁移**：
```diff
-<ion-range
-  :min="0" :max="durationForSlider" :step="0.1"
-  :value="effectiveSeekPosition" :disabled="!canSeek"
-  aria-label="播放进度"
-  @ionInput="onSeekInput"
-  @ionChange="onSeek"
-/>
+<h-range
+  :min="0" :max="durationForSlider" :step="0.1"
+  :model-value="effectiveSeekPosition" :disabled="!canSeek"
+  aria-label="播放进度"
+  @update:model-value="onSeekInput"
+  @change="onSeek"
+  @drag-start="onSeekDragStart"
+  @drag-end="onSeekDragEnd"
+/>
```

`onSeekInput` 对应 `update:modelValue`（拖动中实时预览），`onSeek` 对应 `change`（拖动释放后提交）。

### 9.5 QueuePage

| 当前 | 迁移目标 | 数量 | 映射 |
|------|---------|------|------|
| `ion-button fill="clear" color="danger"` | `<h-button variant="ghost" is-icon-only>` | 2 | 清空按钮用 danger 语义 → 见下方细节 |
| `ion-note` | `<span class="queue-index">` | 1 | 文本 + `--h-color-ink-muted` |

**QueuePage `ion-button` 细节**：
- 清空按钮（nav bar 右侧）：`color="danger"` → `variant="ghost" color="danger"`? HButton 无 `color` prop。建议：用 `variant="danger-soft"`（happier-ui 提供），或手动加 class `text-danger`。
  - **决策**：HButton 有 `variant="danger"`（纯危险填充色）和 `variant="danger-soft"`（浅红底+红字）。清空按钮适合 `variant="danger-soft"`。
- 删除按钮（每行的 remove）：同上 `variant="danger-soft" is-icon-only shape="square"`。

### 9.6 ion-item/ion-label/ion-list 迁移

**采用的策略**：虚拟列表行（SongsPage/QueuePage/PlaylistDetailPage）保持当前自定义 `<div>` 结构，不引入 HCell。原因：
- 虚拟列表行已经有复杂布局（prefix slot = m-cover、label slot = title+subtitle、suffix slot = action button）
- HCell 不支持内部的 `.is-playing` 高亮背景覆盖（`--background: var(--muses-color-playing-bg)`）
- 改用 div + 内联样式更直接

**SettingsPage**：
```diff
-<ion-list>
-  <ion-item lines="none">
-    <ion-label>
-      <h2>Muses</h2>
-      <p>应用版本 {{ currentVersion }}</p>
-    </ion-label>
-  </ion-item>
-  <ion-item lines="none">
-    <ion-label>
-      <h2>音量均衡</h2>
-      <p>...</p>
-    </ion-label>
-    <h-switch slot="end" ... />
-  </ion-item>
-</ion-list>
+<div class="settings-list">
+  <div class="settings-row">
+    <div class="settings-cell">
+      <h2>Muses</h2>
+      <p>应用版本 {{ currentVersion }}</p>
+    </div>
+  </div>
+  <div class="settings-row">
+    <div class="settings-cell">
+      <h2>音量均衡</h2>
+      <p>...</p>
+    </div>
+    <h-switch ... />
+  </div>
+</div>
```

**TabsPage 侧栏**：
```diff
-<ion-list inset>
-  <ion-item router-link="item.to">
-    <h-icon slot="start" ... />
-    <ion-label>{{ item.label }}</ion-label>
-  </ion-item>
-</ion-list>
+<nav class="sidebar-nav">
+  <RouterLink
+    v-for="item in navItems"
+    :key="item.to"
+    :to="item.to"
+    class="sidebar-link"
+    :class="{ 'is-active': isNavActive(item.to) }"
+  >
+    <h-icon :icon="item.icon" />
+    <span>{{ item.label }}</span>
+  </RouterLink>
+</nav>
```

**PlaylistsPage `<ion-list>`**：
PlaylistsPage 的列表用 `ion-item`/`ion-label` 包裹虚拟 playlist 行（m-cover + label + action button）。不同于虚拟列表（有 `ion-item button` 类），PlaylistsPage 的列表是静态的。直接改为原生 `<div>` 结构，保留现有类名和样式。**不引入 HCell**，因有 `m-cover` 前置 + `h-icon-button` 后置 + `.playlist-item` 类名选择器。简单改为：

```diff
-<ion-list v-else>
-  <ion-item ... class="playlist-item" @click="openDetail(item.id)">
-    <m-cover slot="start" ... />
-    <ion-label>
-      <h2>{{ item.name }}</h2>
-      <p>{{ item.validCount }} 首</p>
-    </ion-label>
-    <h-icon-button slot="end" ... />
-  </ion-item>
-</ion-list>
+<div v-else class="playlist-list-static">
+  <div ... class="playlist-item" @click="openDetail(item.id)">
+    <m-cover class="playlist-item-cover" ... />
+    <div class="playlist-item-label">
+      <h2>{{ item.name }}</h2>
+      <p>{{ item.validCount }} 首</p>
+    </div>
+    <h-icon-button class="playlist-item-actions" ... />
+  </div>
+</div>
```

**SourcesPage 扫描进度列表**（`ion-list`/`ion-item`/`ion-note`）：
直接改为原生 div 结构，保留现有类和统计显示。

### 9.7 ion-note 迁移

5 处全部替换为带 muted 颜色的 `<span>`：

**QueuePage.vue**:48：
```diff
-<ion-note slot="end" class="queue-index">{{ row.virtualRow.index + 1 }}</ion-note>
+<span class="queue-index">{{ row.virtualRow.index + 1 }}</span>
```

SourcesPage.vue:217/221/225/229（扫描进度统计值）：
```diff
-<ion-note slot="end">{{ scanProgress.discovered }} / {{ scanProgress.processed }}</ion-note>
+<span class="source-note">{{ scanProgress.discovered }} / {{ scanProgress.processed }}</span>
```

样式定义 `--h-color-ink-muted` 或 muses 的 `--muses-color-ink-muted`（二者同值 #92949c）：
```css
.queue-index, .source-note {
  color: var(--h-color-ink-muted);
  font-size: var(--muses-font-label);
}
```

---

## 10. 全局 CSS 替换

### 10.1 main.ts 导入移除

删除的导入行：
```ts
import { IonicVue } from '@ionic/vue'           // 插件
import '@ionic/vue/css/core.css'                 // normalize + box-sizing + html full-height
import '@ionic/vue/css/normalize.css'            // 重复 normalize（core.css 已含）
import '@ionic/vue/css/structure.css'            // body overflow/position
import '@ionic/vue/css/typography.css'           // 默认字体/颜色
import '@ionic/vue/css/padding.css'              // ion-padding/ion-margin 工具类
import '@ionic/vue/css/float-elements.css'       // 浮动工具类
import '@ionic/vue/css/text-alignment.css'        // 文本对齐工具类
import '@ionic/vue/css/text-transformation.css'   // 大小写工具类
import '@ionic/vue/css/flex-utils.css'           // flex 工具类
import '@ionic/vue/css/display.css'              // display 工具类
import '@ionic/vue/css/palettes/dark.system.css' // 暗色模式（移除，由 happier-ui tokens 接管）
```

保留的顺序（`main.ts` 中）：
```ts
import 'virtual:uno.css'               // Tailwind 预检（normalize + 基础重置）
import './theme/tokens.css'            // 仅 @import 'happier-ui/tokens.css'
// import './theme/variables.css'      // 删除（所有 Ionic 桥接已清理）
import './theme/tailwind.css'          // Tailwind layer 定义 + 自定义工具类
```

### 10.2 缺失样式的补充

Ionic `core.css` + `normalize.css` + `structure.css` 提供的基础样式，需要确认 Tailwind preflight 是否已经覆盖。

**html/body 高度**（Ionic `core.css` 设 `html: 100%` + `body: 100%`，Tailwind preflight 设 `html, body: 100%`）→ 已覆盖。

**box-sizing**（Ionic 与 Tailwind 都用 `*, *::before, *::after { box-sizing: border-box }`）→ 已覆盖。

**body 字体/颜色**（Ionic 设 `font-family: -apple-system, ...`，Tailwind preflight 设 `font-family: inherit`）→ **需补充**：

在 `tailwind.css` 或 `tokens.css` 中添加：
```css
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: var(--h-color-ink);
  background: var(--h-color-surface);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
```

**暗色模式**：happier-ui tokens.css 通过双触发（`@media(prefers-color-scheme: dark) :root:not(.light)` + `:root.dark, .dark`）自动处理暗色变量，**无需 muses 侧额外工作**。迁移后测试确认暗色背景/字体/边框颜色与迁移前一致。

---

## 11. 类型导入清理

### 11.1 各页面当前 Ionic 类型导入及迁移

| 文件 | 当前导入 | 迁移后 |
|------|---------|--------|
| SongsPage.vue | `import { type ActionSheetButton, type AlertButton, type AlertInput } from '@ionic/vue'` | **删除**（不再使用 Ionic 组件类型） |
| PlaylistsPage.vue | 同上 | 删除 |
| PlaylistDetailPage.vue | `import { IonContent, IonItem, IonLabel, IonPage, onIonViewWillEnter, useIonRouter } from '@ionic/vue'` | 删除全部；零 Ionic 导入 |
| AlbumsPage.vue | `import { onIonViewWillEnter } from '@ionic/vue'` | 删除 |
| ArtistsPage.vue | 同上 | 删除 |
| MiniPlayer.vue | `import { IonButton } from '@ionic/vue'` | 删除（HButton 替换） |
| PlayerPage.vue | `import { IonButton, IonRange } from '@ionic/vue'` | 删除（HButton + HRange 替换） |
| QueuePage.vue | `import { IonButton, IonContent, IonItem, IonLabel, IonNote } from '@ionic/vue'` | 删除全部 |
| SettingsPage.vue | `import { IonContent, IonItem, IonLabel, IonList, IonPage } from '@ionic/vue'` | 删除全部 |
| SourcesPage.vue | `import { IonAlert, IonContent, IonItem, IonLabel, IonList, IonModal, IonNote, IonPage } from '@ionic/vue'` | 删除全部 |
| TabsPage.vue | `import { IonItem, IonLabel, IonList } from '@ionic/vue'` | 删除全部 |
| App.vue | `import { IonApp, IonRouterOutlet } from '@ionic/vue'` | 删除 |

### 11.2 图标导入迁移

`ionicons` 图标已在 muses 的 `src/icons.ts`（或类似文件）中导入并再导出为 muses 专用图标名。已在 PRD 的阶段中确认：`rg "from 'ionicons'" src/` 零命中，所以只需要检查 `package.json` 删除 `ionicons` 依赖，无源代码变动。

---

## 12. 验证方案

### 12.1 编译验证

```bash
# 类型检查
npx vue-tsc --noEmit

# 生产构建
npm run build

# 单元测试（无 e2e 环境也可先跑 unit）
npm run test:unit
```

### 12.2 手动回归检查点

| 编号 | 检查项 | 验证方法 |
|------|--------|---------|
| AC5 | 应用启动 | `npm run dev` 启动，浏览器打开无控制台报错 |
| AC5 | Tab 切换 | 点击底部各 tab，页面内容正确切换 |
| AC5 | 歌单详情进出 | 点歌单进详情，返回列表，数据正确 |
| AC5 | 页面数据刷新 | 切换 tab 后回到之前的 tab，数据正常 |
| AC6 | PlaylistsPage 歌单操作 | 长按/点击更多 → action sheet 弹出 → 重命名/删除对话框弹出 → 操作生效 |
| AC6 | SongsPage 歌曲操作 | 点歌曲更多 → action sheet → 加入歌单 → 另一个 action sheet → 新建歌单 dialog → 输入名 → 创建 |
| AC6 | SongsPage FAB | 当前播放歌曲在列表中时，底部浮动按钮出现 → 点击跳转到当前歌 |
| AC6 | SourcesPage WebDAV 弹窗 | 加号 → 添加 WebDAV → 底部面板弹出 → 填写表单 → 保存/关闭 |
| AC6 | SourcesPage 扫描进度 | 点扫描 → 设置面板→ 开始扫描 → 进度面板实时更新 |
| AC6 | 浮层不被遮挡 | 所有弹窗不被 MiniPlayer / tab-bar 遮挡 |
| AC7 | safe-area | 刘海屏设备（或 Chrome DevTools 模拟）MiniPlayer/PlayerPage/SongsPage FAB 不被安全区裁切 |
| AC8 | 暗色模式 | 系统切暗色 / `.dark` class 切换，应用颜色正常 |
| AC8 | 主色 token | tab-bar 激活色、侧栏激活色、按钮主色与迁移前一致 |
| AC9 | 返回键 | Android 返回键 → 有弹窗关弹窗 → 无弹窗退后台 |
| AC9 | 状态栏 | 进入 PlayerPage 时状态栏变暗色 |

### 12.3 回归预防脚本

```bash
# AC1：零残留检查
rg "@ionic/" src/ && echo "ERROR: @ionic/* import 残留" && exit 1 || echo "OK: 无 @ionic/* 导入"
rg "<ion-" src/ && echo "ERROR: ion-* 标签残留" && exit 1 || echo "OK: 无 ion-* 标签"
rg "from 'ionicons'" src/ && echo "ERROR: ionicons 引用残留" && exit 1 || echo "OK: 无 ionicons 引用"

# AC2：package.json 确认
rg "@ionic/vue" package.json && echo "ERROR" && exit 1 || echo "OK"
rg "ionicons" package.json && echo "ERROR" && exit 1 || echo "OK"
rg "happier-ui" package.json | rg "0\.0\.6" || echo "ERROR: happier-ui 版本非 0.0.6"
```

---

## 13. 杂项/已知边界

### 13.1 `ion-padding` 类名

当前 SourcesPage 的 modal 内容有 `class="ion-padding"`（Ionic 提供的 padding 工具类）。迁移到 HBottomSheet 后，内容区自身已有 padding 结构，如需要统一 16px 内边距，可在全局定义：

```css
.ion-padding {
  padding: 16px;
}
```

或各面板各自定义。建议：统一加一行为兼容非 SourcesPage 可能引用（搜索确认：仅 SourcesPage modal 内部使用）。

### 13.2 `ion-fab` slot="fixed"

Ionic IonContent 支持 `slot="fixed"` 专用于 FAB 定位（不随内容滚动）。迁移到 HFloatingBubble 后不再依赖此机制，通过 `offset` prop 直接设定位置。需确保 `HFloatingBubble` 的 teleport 行为不影响点击穿透——`axis="lock"` 时无拖拽事件，点击事件正常冒泡。

### 13.3 `ion-content` 的 `--overflow` CSS 变量

IonContent 的自定义 CSS 变量 `--overflow` 用于控制内容区域溢出行为。SongsPage 当前没有覆写 `--overflow`；`ion-content` 默认 `overflow: auto`。迁移后 `.m-content` 默认 `overflow: auto`（`--overflow` 不存在了），如果要保持完全一致的行为，需要确认 `.m-content` 不隐藏溢出。

### 13.4 PlayerPage `closest('button')` 逻辑

PlayerPage 脚本中有 Shadow DOM 穿透逻辑（`event.composedPath()` 查找 `<ion-button>` 的 Shadow host）。HButton 是原生 `<button>`，不再是 Web Component，所以 `closest('button')` 迁移后可简化或移除。

### 13.5 旧 spec 冲突标记

当前 `component-guidelines.md` 仍为 0.0.2 时代内容（HCell 未采用、HRange 保持 ion-range 等），与本设计严重冲突。**在任务完成阶段（Phase 3.3）必须更新**，替代设计以本文档为准。

---

## 14. 风险 & 缓解

| 风险 | 可能性 | 影响 | 缓解 |
|------|--------|------|------|
| 某页面遗漏 `ion-*` 引用，移 Ionic 后崩 | 低 | 高 | 执行步骤 1–6 时逐个页面验证；步骤 17 全局搜索兜底 |
| PlayerPage 图标在暗底上颜色不对 | 中 | 低 | 设计 9.4 已预判，提供 `.player-overlay .h-button--ghost { color: #fff }` 降级 CSS |
| SourcesPage 全屏表单 → 88vh bottom-sheet 观感差异大 | 中 | 中 | 决策 4 已拍板，用户接受此变化 |
| `window.history.length` 在 Capacitor 中行为异常 | 低 | 低 | 备选方案用 `router.push('/tabs/playlists')` 直接跳转 |
| PlaylistDetailPage watch param 未触发（动态路由已缓存） | 低 | 中 | 已验证路由 `{ path: '/playlists/:id' }` 在 param 变化时复用实例 |
