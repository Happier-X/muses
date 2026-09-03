# 执行计划：Tagger 式刮削审核流

前置：`design.md` 已评审。引擎层（core:scrape）零改动；全部工作在 `feature:scrape` / `feature:library` / `app` 三处。

## 通用验证命令

```bash
# 编译（Windows / Git Bash）
./gradlew :feature:scrape:compileMusesDebugKotlin :app:compileMusesDebugKotlin
# 单测（core:scrape 存量 119 tests 回归 + feature 层新增）
./gradlew :core:scrape:testDebugUnitTest :feature:scrape:testDebugUnitTest
# Lint（spec 红线）
./gradlew :app:lintMusesDebug
```

## S1 单曲审核页（全屏）+ 入口恢复

- [ ] `app/.../NavDestination.kt`：新增 `ScrapeReview("scrape_review")` 路由（songId 必传 + queue 可选参数，见 design 2.1）。
- [ ] `feature:scrape`：新建 `ScrapeReviewViewModel.kt`（状态机 design 2.2；注入 `EditCloudMetaSearch`/`WritebackOrchestrator`/`SongRepository`；重搜前 abort 前次；catch 前置 rethrow `CancellationException`，`EditSearchAbortedException` 不吞）。
- [ ] `feature:scrape`：新建 `ScrapeReviewScreen.kt`（布局 design 2.3；FieldCheckRow/编辑覆写从 `SingleScrapeSheet.kt` 迁移改造；封面横向候选网格 + 点大图；歌词候选选择 + 预览）。
- [ ] 删除 `SingleScrapeSheet.kt` + `SingleScrapeViewModel.kt`；清理 `MusesApp.kt` 的 `singleScrapeSong` 状态（`MusesApp.kt:308-316` 改为导航调用）。
- [ ] `feature/library/SongsPage.kt`：⋮ 菜单恢复「刮削」项（调 `onScrapeSingle`，`SongsPage.kt:435` 附近），「加入待刮削」保留。
- [ ] 验证：编译 + `:core:scrape:testDebugUnitTest` 回归 + 手测（MuMu）：⋮ → 审核页自动查询 → 切文本候选 → 勾字段 → 应用写回 → 文件标签生效。
- 回滚点：revert 本 commit 回到现状（孤儿浮层、批量流程完好）。

## S2 批量预览：未命中分组可见可重试

- [ ] `ScrapeViewModel`：`ScrapePageState.Preview` 增加 `noMatchIds: List<String>`；`startMatching()` 未命中（非 NETWORK）入组（`:199-221` 的 continue 分支改写）；新增 `retryNoMatch(songId)` 复用单曲匹配路径（与 `retrySingle` 合并实现，参数区分归组）。
- [ ] `ScrapeScreen`：预览态新增「未命中（M 首）」折叠分组，行内「重试」+「去审核」（打开审核页单曲模式）；限流分组沿用现状。
- [ ] 验证：手测——造一首必然无匹配的歌（乱码文件名），确认它出现在未命中分组并可重试/去审核；存量预览行为不变。
- 回滚点：revert 后回到「未命中静默消失」现状。

## S3 批量逐首审核（连续审核）

- [ ] `ScrapeViewModel`：`pendingReviewQueue` + `advanceReview()`（design 3.2）；预览写回/审核页写回后从待审队列剔除。
- [ ] `ScrapeScreen`：预览态操作行加「逐首审核（N）」；`LaunchedEffect(pendingReviewQueue)` 自动 navigate 下一首；审核页返回（非应用路径）清 pending。
- [ ] `ScrapeReviewScreen` 批量模式：TopAppBar 显示 (i/N)；成功态按钮「应用并下一首」→ `popBackStack()`。
- [ ] 验证：手测——5 首队列匹配 → 逐首审核连续推进 → 中途手动返回不弹下一首 → 「全部应用」仍可用。
- 回滚点：revert 后批量回到纯预览确认模式。

## S4 收尾：单测 + lint + 文档

- [ ] `ScrapeReviewViewModelTest`（JVM）：候选切换/字段勾选→`ScrapeChanges` 只含勾选字段/重搜 abort 不吞取消/写回成功 nextSongId 推进。
- [ ] `ScrapeViewModel` 预览分组单测：NO_MATCH 入 noMatchIds、NETWORK 入 throttledIds、重试归组正确。
- [ ] 全量验证：`:core:scrape:testDebugUnitTest` + `:feature:scrape:testDebugUnitTest` + `:app:lintMusesDebug`。
- [ ] `trellis-update-spec`：把「快慢双链分层」「EditCloudMetaSearch 复用为审核引擎」「预览未命中分组」沉淀进 `.trellis/spec/android/features-scrape-engine.md`。

## 风险文件

- `MusesApp.kt`（宿主导航接线，改动集中但文件大——只动 Scrape 相关段落）
- `ScrapeViewModel.kt:199-221`（startMatching 未命中分支，需保持 NETWORK/NO_MATCH 区分语义）
- `SongsPage.kt:435`（⋮ 菜单，注意与「加入待刮削」共存）

## start 前检查

- [ ] prd.md / design.md / implement.md 三件齐
- [ ] implement.jsonl / check.jsonl 已填真实条目
