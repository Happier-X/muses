# 刮削体验重构：Tagger 式操作逻辑

## Goal

把刮削操作逻辑改成 Ericwyn/tagger 的「浏览 → 查询 → 审核 → 写入」四段式体验：在歌曲列表就近触发刮削，查询结果多源多候选可切换，逐字段本地值/候选值对比审核，支持改关键词重搜与连续审核；批量流程复用同一审核形态，未命中歌曲可见、可重试。

用户价值：现在刮一首歌要「⋮ 入队 → 切刮削 Tab → 开始匹配 → 滚动预览 → 应用」5 步，且候选黑盒（首个命中即返回，不能换、不能重搜）。目标是一屏之内完成「看差异 → 换候选 → 勾字段 → 写回」，批量时逐首顺滑推进。

## Background / Confirmed Facts

- 平台：Android 原生（Kotlin + Jetpack Compose），刮削分两层：`:core:scrape`（引擎）+ `:feature:scrape`（UI）。规格见 `.trellis/spec/android/features-scrape-engine.md`。
- 现有可复用资产：
  - **三维多候选引擎已存在**：`EditCloudMetaSearch`（`core/scrape/editmeta/EditCloudMetaSearch.kt`）——文本 5 源候选去重粗排（cap 8）、封面多 URL 去重、歌词多候选（含 lrclib，格式粗排）、AbortSignal 可中止、强制搜索不吃负缓存；Hilt 已装配（`ScrapeModule.kt:82`），`EditMetaViewModel` 已示范用法。
  - 快链匹配：`TextMetaMatcher`（五源、首个命中即返回、负缓存 45min）+ `CoverMatcher`（六源）；全局 250ms 限流 + 429 退避。
  - 逐字段 Checkbox + 原值→新值对比 + 置信度角标：单曲浮层（`SingleScrapeSheet.kt` 248 行）与批量预览（`ScrapeScreen.kt`）已有（d3eb3a23/2a5c4b0d/91b7c13c）。
  - 写回：`WritebackOrchestrator`（写文件+写库+回滚 journal）。
- 关键痛点（对照表见 `research/tagger-ux-analysis.md`）：
  1. 单曲即时刮削浮层是孤儿功能：最新提交把 ⋮ 菜单改回「加入待刮削」，`onScrapeSingle`（`SongsPage.kt:108`）无调用点，想刮单曲只能入队绕路。
  2. 快链首个命中即返回：无候选列表、无切换、无改关键词重搜。
  3. 批量匹配失败的歌曲静默消失（`ScrapeViewModel.kt:199-221` 直接 continue），不列出不给重试。
  4. 批量主流程歌词缺席（`matchedLyrics` 恒为 null）。

## Key Decisions（已确认）

- **D1 范围一次到位**（用户 2026-09-03 拍板）：单曲审核页 + 批量接入 + 未命中可见可重试，一个任务内分 4 步交付。
- **D2 引擎分层，引擎层零改动**：快链（`TextMetaMatcher`/`CoverMatcher`）保持命中即停、负缓存、省请求——批量匹配速度不退化；审核页查询与重搜全链复用 `EditCloudMetaSearch`。「单源内多条候选」（provider 返回列表）留作后续优化，不在本任务。
- **D3 审核页为全屏导航页**（非底部浮层）：多候选切换 + 封面网格 + 歌词预览 + 重搜表单在浮层里放不下；由 `SingleScrapeSheet/SingleScrapeViewModel` 升级改造，旧浮层退役。

## Requirements

### R1 单曲审核工作台（Tagger「就地审核」）
- 歌曲列表行 ⋮ 菜单提供「刮削」入口，直接进入**全屏审核页**并自动查询；全程不经过「刮削」Tab。
- 文本字段（标题/歌手/专辑）：本地值 vs 候选值逐字段对比、逐字段勾选；文本候选可整体切换（按源一候选，智能推荐置顶），切后可逐字段手改覆写；显示来源与置信度。
- 改关键词重搜：三个搜索词输入框 + 重新搜索，中止前次查询刷新候选。
- 封面：多候选缩略图网格可挑、点大图预览。
- 歌词：候选列表（来源+格式角标）可挑、可预览。
- 底部「应用（N）」仅写回勾选字段；写回复用 `WritebackOrchestrator`（journal 可撤销）。

### R2 批量流程接入同一审核形态
- 批量预览态增加「逐首审核（N）」：进入审核页逐首连续推进（「应用并下一首」），中途手动返回不强推。
- 预览「全部应用」`confirmWriteback()` 保留（不想逐首审核的捷径），行为不变。

### R3 未命中可见、可重试
- 批量匹配后，未命中歌曲（NO_MATCH）与限流未命中（NETWORK）分组列出；行内可重试、可打开审核页改词重搜。

## Acceptance Criteria

- [ ] AC1 歌曲列表对任意歌曲点 ⋮ →「刮削」，进入全屏审核页并自动开始查询；全程不经过「刮削」Tab。
- [ ] AC2 审核页内，标题/歌手/专辑能看到多源候选并可整体切换；每候选标注来源；默认选中智能推荐候选。
- [ ] AC3 审核页内修改关键词重搜后，候选按新关键词刷新，且前次查询被中止（不残留旧结果覆盖新结果）。
- [ ] AC4 封面多候选可选、可预览；歌词候选可选、可预览；勾选后随「应用」写回。
- [ ] AC5 「应用（N）」只写回勾选字段，写回后文件标签与曲库一致，journal 可撤销（复用现有机制）。
- [ ] AC6 批量匹配完成后可「逐首审核」连续走完队列；未命中与限流歌曲在预览中分组可见、可单独重试。
- [ ] AC7 预览「全部应用」按快链结果直接批量写回，行为与现状一致。
- [ ] AC8 单测通过：审核 VM 候选切换/写回字段范围/重搜取消不吞 CancellationException；批量预览 NO_MATCH 与 NETWORK 归组正确；`:core:scrape` 存量 119 tests 回归。

## Out of Scope

- 播放页「编辑歌曲信息」弹层的统一改造（后续任务）。
- 目录树/文件夹视图、按状态过滤列表（Tagger 的 Web 大屏布局不适配手机；本任务只做审核流）。
- 快链引擎多源聚合改造（`TextMetaMatcher` 保持不变；见 D2）。
- 写入任务中心持久化队列管理。
- 置信度三档化、`auto_scrape_enabled` 设置 UI、`SuspiciousDetector` 接线（独立小任务）。
