# 播放器 Overlay 改造 — 设计

## 当前架构

```
Router:
  / → /tabs/songs
  /tabs/* → TabsPage (Vue shell + RouterView)
  /player → PlayerPage (独立路由)
  /queue  → QueuePage (独立路由)

App.vue:
  MiniPlayer v-if="!isPlayerPage" (隐藏条件: route.path === '/player' || '/queue')
  backButton: /player/queue 时 router.back()
```

## 目标架构

```
Router: 不再有 /player、/queue 路由

App.vue:
  MiniPlayer v-if="!playerOverlayVisible && !queueOverlayVisible"
  <PlayerOverlay v-if="playerOverlayVisible" />
  <QueueOverlay v-if="queueOverlayVisible" />
  backButton: overlay 可见时关闭 overlay

MiniPlayer 点击 → playerOverlayVisible = true（不改变路由）
PlayerPage 队列按钮 → queueOverlayVisible = true
PlayerPage 下滑收起 → playerOverlayVisible = false
```

## 状态管理

新增全局 overlay 状态（放在 App.vue 用 reactive/ref 即可，或新建 composable）：

```ts
// src/features/player/overlay.ts 或直接在 App.vue 内
const playerOverlayVisible = ref(false)
const queueOverlayVisible = ref(false)
```

所有原 `router.push('/player')` 改为 `playerOverlayVisible = true`。
所有原 `router.push('/queue')` 改为 `queueOverlayVisible = true`。
原 `goBack()` 在播放器内的逻辑改为 `playerOverlayVisible = false`。

## 动画

PlayerOverlay 打开时从底部滑入，关闭时向底部滑出。QueueOverlay 同理。

## 队列关联

用户打开队列后关闭队列不应关闭播放器，只关闭队列 overlay。但队列可在没有播放器时单独打开（从 MiniPlayer 队列按钮进入时，可能需要同时打开播放器）。

简化方案：MiniPlayer 队列按钮 → 打开播放器 overlay 并直接跳转到队列面板。

## 文件影响

| 文件 | 变更 |
|------|------|
| `src/router/index.ts` | 删除 `/player` 和 `/queue` 路由 |
| `src/App.vue` | 新增 PlayerOverlay/QueueOverlay 渲染与状态；调整 MiniPlayer 显隐和 backButton |
| `src/views/PlayerPage.vue` | 移除外层 `<ion-page>`，改为 overlay 内容组件（或保留但改显隐逻辑） |
| `src/views/QueuePage.vue` | 同上，或直接嵌入 PlayerPage 内 |
| `src/components/MiniPlayer.vue` | 按钮事件改用 overlay 状态 |
| `src/features/player/controller.ts` | 如有暴露 overlay 状态的需求，新增导出 |

## 兼容性

- 不引入新的全局状态库。
- 不改变 MiniPlayer、TabsPage 的既有样式和结构。
- 下滑手势保持跟手 + 阈值返回逻辑不变。