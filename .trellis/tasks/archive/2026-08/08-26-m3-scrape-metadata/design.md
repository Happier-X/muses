# 技术设计 — M3 UI 接线

## 页面结构（feature:scrape 新模块）

```
feature/scrape/
├── ScrapeScreen.kt        # 四态机 pageState: queue/matching/preview/result
├── ScrapeViewModel.kt     # 组合 core:scrape 的 ScrapeQueueStore/HistoryStore/
│                           TextMetaMatcher/CoverMatcher/WritebackOrchestrator
├── EditMetaSheet.kt       # 编辑信息弹窗（editmeta 三维编排 + 预览应用）
```

- feature:scrape 依赖：core:scrape/core:model/core:data/core:ui（不碰实现库）
- 导航：NavDestination.Scrape 路由替换 PlaceholderScreen

## 状态流

```
歌曲页标记 → ScrapeQueueStore.enqueue(songIds)
ScrapePage queue 态「全部开始」
  → 逐曲 TextMetaMatcher/CoverMatcher（进度 StateFlow → matching 态）
  → 候选聚合 → preview 态（用户逐项确认/跳过）
  → WritebackOrchestrator.execute（写文件→写库→journal）→ result 态
撤销 → journal 回放恢复库旧值（文件不动，对齐 Web 撤销语义）
```

## 桥扩展

PlayerWebView onAction 新增 `openEditMeta`（songId 由当前曲携带）→ PlayerScreen 回调导航至
全局 EditMetaSheet（宿主在 MusesApp 层，BottomSheet 承载，避免 WebView 内实现复杂表单）。

## 自动补缺调度

ScanWorker 完成后（或 SourcesViewModel.startScan 成功后）：查 `tagsVersion < 1` 且
`metaSources IS NULL` 的歌 → enqueue。开关存 DataStore `auto_scrape_enabled` 默认 false，
设置页暂不加 UI（DataStore 手动改），避免范围膨胀。

## 已知风险

- 五源/六源 provider 均为公网 API，MuMu 出网需可达；匹配失败路径必须优雅降级（负缓存生效）
- 写回直接改用户网盘文件：preview 必须默认全不选，确认后才执行；journal 是唯一后悔药，测试先行
