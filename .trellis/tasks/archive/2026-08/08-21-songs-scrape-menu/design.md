# 设计：歌曲页刮削菜单重构

## 变更边界
- 仅改 `src/views/SongsPage.vue` 工具条区域与对应样式，新增 `isScrapeMenuOpen` 状态
- 不动：`/scrape` 页、`pickSuspiciousSongs`、`enqueueScrapeSongs`、`onScrapeQueueChanged` 队列逻辑、`onShuffleAll` 逻辑

## UI 设计
- 工具条左侧保留：`shuffle` 按钮（随机播放全部） + 歌曲数字 `{{ songs.length }}`
- 新增「刮削」触发：`listChecks` 图标 + 文字「刮削」+ 右上角徽标（`scrapeQueueCount>0` 显示，>99 显示 99+），位于 `toolbar-left` 计数左侧或 `toolbar-right`（建议放 `toolbar-left` 紧邻随机播放，保持左侧聚合）
- 点击触发 `isScrapeMenuOpen=true` 弹出 `m-actions`
- `m-actions` 内两项：
  1. `筛选可疑歌曲` — 显示 `suspiciousCount`，`disabled` 当为 0，点击走原 `onOpenSuspiciousBatch` → 确认框 → `onConfirmSuspiciousBatch`
  2. `刮削队列` — 显示 `scrapeQueueCount` 徽标/计数，点击 `router.push('/scrape')` 并关闭菜单
- 复用现有 `isSuspiciousConfirmOpen` 确认框，无需新增
- 徽标样式复用 `songs-page__scrape-badge`，深色适配保持

## 交互
- 工具条：`songs.length>0 && !isSearching` 时显示简化版
- 菜单：`@backdropclick` 关闭
- 计数：`scrapeQueueCount` 通过 `onScrapeQueueChanged(refreshScrapeQueueCount)` 实时同步

## 风险
- 图标收敛后用户找不到入口：触发按钮保留 `listChecks` 图标+文字+徽标，足够醒目
