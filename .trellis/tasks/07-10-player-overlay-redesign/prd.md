# 将播放器页从路由改为全局叠加层

## 背景

当前播放器页（`PlayerPage.vue`）和队列页（`QueuePage.vue`）是独立路由页面。切换到 `/player` 时底层 tab 页面被弹出栈，导致下滑收起时露出黑屏或重复页面，无法实现跟手效果。用户希望二者更像全局 popup/overlay，底层 tab 页始终保留，下滑可露出上一页。

## 目标

- 播放器改为全局叠加层，不再作为独立路由页面。
- 队列也改为全局叠加层或内嵌在播放器内，不再作为独立路由页面。
- 底层 tab 页面始终存在，打开/关闭播放器不改变路由栈。
- 播放器打开/关闭通过状态切换 + 转场动画实现，不依赖路由前进/后退。
- 下滑手势保持跟手效果，露出底层 tab 页面。
- 保持现有 MiniPlayer 底栏逻辑不变。
- 保持现有 `TabsPage.vue` 底部导航逻辑不变。

## 范围

包含：

- 将 `src/views/PlayerPage.vue` 改为 overlay 模式，由 `src/App.vue` 控制显隐。
- 将 `src/views/QueuePage.vue` 改为 overlay 模式或内嵌在播放器内。
- 从 `src/router/index.ts` 移除 `/player` 和 `/queue` 路由。
- 调整 `src/App.vue` 中 `isPlayerPage` 逻辑改为 overlay 显隐。
- 调整 `MiniPlayer` 底栏和 `TabsPage` 底部导航的显隐逻辑适配 overlay。
- 调整 `backButton` 处理改为关闭 overlay 而非路由返回。
- 调整所有跳转 `/player`、`/queue` 的导航逻辑。
- 构建验证。
- 如需要，安装到 MuMu 验证。

不包含：

- 重新设计播放器 UI 内容。
- 新增播放器功能。
- 发布新版本。

## 验收标准

- [ ] 点击 MiniPlayer 底栏进入播放器时，不改变当前路由路径。
- [ ] 播放器打开后底层 tab 页面仍在。
- [ ] 下滑手势收起播放器时，跟手露出底层 tab 页面。
- [ ] 播放器收起后路由仍在当前 tab 页，不跳转。
- [ ] 队列页可从播放器内打开，收起队列回到播放器或底层。
- [ ] 原有 MiniPlayer 和底部导航仍正常显示/隐藏。
- [ ] Android back 键在播放器打开时关闭播放器。
- [ ] `npm run build` 通过。