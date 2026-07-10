# 播放器 Overlay 改造 — 实施计划

## 前置清理

- 当前工作区存在上一轮 `player-page-tabbar-hidden` 修补产生的未提交改动：`src/views/PlayerPage.vue`、`src/views/TabsPage.vue`。
- 新 overlay 改造会替代 `PlayerPage.vue` 中的路由页下滑修补逻辑；实现前需要以当前文件状态为准重构，避免保留错误的“路由页下滑关闭”方案。
- `TabsPage.vue` 中“只在 `/tabs/*` 显示 tabbar/侧栏”的改动仍有价值，应保留，防止 Ionic 缓存页残留导航。

## 实施步骤

1. 新增播放器 overlay 状态
   - 新建轻量 composable：`src/features/player/overlay.ts`。
   - 暴露 `playerOverlayVisible`、`queueOverlayVisible`。
   - 暴露 `openPlayerOverlay()`、`closePlayerOverlay()`、`openQueueOverlay()`、`closeQueueOverlay()`。
   - 不引入 Pinia/Vuex 等全局状态库。

2. 改造 `App.vue` 作为 overlay 宿主
   - 固定渲染 `<ion-router-outlet />` 保持 tab 路由页面存在。
   - 根据 overlay 状态渲染播放器和队列 overlay。
   - MiniPlayer 仅在播放器/队列 overlay 都关闭时显示。
   - Android back 键优先关闭队列 overlay，其次关闭播放器 overlay，最后按原逻辑退出应用。

3. 改造 `MiniPlayer.vue`
   - 主体点击调用 `openPlayerOverlay()`，不再 `router.push('/player')`。
   - 队列按钮调用 `openQueueOverlay()`，不再 `router.push('/queue')`。
   - 保留按钮事件穿透防护和现有样式。

4. 改造 `PlayerPage.vue` 为 overlay 内容
   - 保留播放器主体 UI 和左右面板切换。
   - 移除路由返回依赖：`goBack()` 改为 `closePlayerOverlay()`。
   - 队列按钮改为 `openQueueOverlay()`。
   - 下滑手势移动 overlay 内容层，露出底层真实 tab 页面。
   - 移除前一轮为了路由页修补而添加的 underlay/透明路由页方案。
   - 顶部不显示收起按钮。

5. 改造 `QueuePage.vue` 为 overlay 内容
   - 关闭/返回按钮调用 `closeQueueOverlay()`。
   - 不再依赖路由历史。
   - 队列页 overlay 打开时覆盖在播放器或 tab 页上方。

6. 清理路由
   - 从 `src/router/index.ts` 删除 `/player` 和 `/queue`。
   - 搜索并替换所有 `router.push('/player')`、`router.push('/queue')`。

7. 验证路由与 UI
   - 从 `/tabs/songs` 打开 MiniPlayer，确认 URL 不变。
   - 播放器打开时底层 tab 页仍可作为背景存在。
   - 下滑收起时跟手露出底层页面，不黑屏、不重复。
   - 队列按钮打开队列 overlay，关闭后回到播放器或底层。
   - Android back 键关闭 overlay。

8. 质量检查
   - 运行 `npm run build`。
   - 如需要，运行 Android debug 构建并安装到 MuMu 验证。

## 回滚点

- 如果 overlay 状态改造导致路由异常，回滚 `src/features/player/overlay.ts`、`src/App.vue`、`MiniPlayer.vue`、`PlayerPage.vue`、`QueuePage.vue`、`router/index.ts` 的本任务改动。
- 如果仅下滑动画异常，优先回滚 `PlayerPage.vue` 的手势/动画部分，保留 overlay 状态架构。

## 交付检查清单

- [ ] `/player` 和 `/queue` 不再作为路由存在。
- [ ] MiniPlayer 打开播放器不改变 URL。
- [ ] 播放器 overlay 下滑露出真实底层页面。
- [ ] 队列 overlay 可打开和关闭。
- [ ] Android back 键优先关闭 overlay。
- [ ] `npm run build` 通过。
