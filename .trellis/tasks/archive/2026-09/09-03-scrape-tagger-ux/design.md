# 技术设计：Tagger 式刮削审核流

## 0. 设计结论（一句话）

不动匹配快链（`TextMetaMatcher`/`CoverMatcher` 保持现状），把现成的三维多候选引擎 `EditCloudMetaSearch`（`core/scrape/editmeta/`）接入一个新的**全屏审核页 `ScrapeReviewScreen`**（由 `SingleScrapeSheet`/`SingleScrapeViewModel` 升级改造而来），单曲从歌曲列表 ⋮ 直达审核页，批量预览态增加「未命中分组可见可重试」与「逐首审核」，写回复用 `WritebackOrchestrator`。

## 1. 架构与边界

### 1.1 快慢双链分层（核心权衡）

| 链路 | 引擎 | 语义 | 用于 |
|---|---|---|---|
| 快链（匹配） | `TextMetaMatcher` + `CoverMatcher` | 首个命中即返回、负缓存、省请求 | 批量队列匹配（20 首 ≈ 命中即停，速度不退化） |
| 全链（审核） | `EditCloudMetaSearch` | 强制搜索、不吃负缓存、三维多候选（文本去重粗排 cap8 / 封面 URL 去重 / 歌词格式粗排）、AbortSignal 可中止 | 审核页查询 + 改关键词重搜 |

理由：
- `EditCloudMetaSearch` 已存在且 Hilt 已装配（`ScrapeModule.kt:82`，歌词端口含 `LrclibProvider`），`EditMetaViewModel` 已示范其用法——**引擎层零改动**即可获得 Tagger 的「原始候选保留 + 智能选择」。
- 若改 `TextMetaMatcher` 为多源聚合，每首在 250ms 限流下最多 5 源 × 250ms 请求，批量速度显著退化，且 119 个存量单测回归面大。MVP 不做；「单源内多条候选」（provider `searchAll`）留作后续优化。
- 审核页内重搜用 AbortSignal 取消前次搜索（引擎已支持 `signal` 参数）。

### 1.2 模块边界（依赖方向不变）

- `feature:scrape` 新增 `ScrapeReviewScreen/ScrapeReviewViewModel`，注入 `EditCloudMetaSearch` + `WritebackOrchestrator` + `SongRepository`（与 `EditMetaViewModel` 同构）。
- `feature:library`（SongsPage）不依赖 core:scrape：入口仍走 `onScrapeSingle: (Song) -> Unit` 回调（修复孤儿回调：`SongsPage.kt:108` 现无调用点）。
- 宿主 `app/MusesApp.kt` 负责导航路由与参数传递。
- 退役：`SingleScrapeSheet.kt`、`SingleScrapeViewModel.kt`（逻辑全部并入审核页，避免双轨维护）；`MusesApp.kt` 的 `singleScrapeSong` 状态清理。

## 2. 审核页设计

### 2.1 导航契约

- 新路由 `NavDestination.ScrapeReview`：`"scrape_review?songId={songId}&queue={queue}"`。
  - `songId`：必传，审核页据此从 `SongRepository` 取歌（避免 Song 对象经路由传递）。
  - `queue`：可选逗号分隔 songId 列表 = 批量模式队列上下文（供「应用并下一首」推进；单曲模式不传）。
- 进入即自动查询（`LaunchedEffect(songId)` → `search()`）。

### 2.2 ScrapeReviewViewModel 状态机

```
sealed interface ReviewState {
    data object Searching
    data class Review(
        val song: Song,                       // 本地当前值来源
        val text: EditDimResult<TextMetaHit>,      // 多候选，defaultIndex 为智能推荐
        val cover: EditDimResult<EditCoverCandidate>,
        val lyrics: EditDimResult<EditLyricsCandidate>,
        val selectedTextIndex: Int,           // 文本候选切换（三个文本字段跟随同一候选，可再逐字段手改覆写）
        val selectedCoverIndex: Int?,
        val selectedLyricsIndex: Int?,
        val checkedFields: Set<String>,       // 默认 = 推荐候选中有差异且有值的字段（title/artist/album/cover/lyrics）
        val editTitle/editArtist/editAlbum: String?,   // 逐字段手改覆写（优先于候选值）
        val keyword: QueryKeyword,            // 当前搜索词（title/artist/album 三输入），重搜可改
        val nextSongId: String?,              // 批量模式：队列中下一首
    )
    data class Empty(val reason: String)      // no-match / network（限流文案沿用 FailureCopy 语义）
    data class Writing
    data class Success(val nextSongId: String?)
}
```

关键行为：
- **查询**：`editCloudMetaSearch.search(EditCloudMetaQuery(...), signal)`；新搜索前 abort 前次（成员持有一个可变更的 abort 标志）。
- **重搜**：改关键词后 `search()` 重跑三维；本地已勾选状态重置为推荐值。
- **候选切换**：文本候选切换 = 三个文本字段整体切换到该候选（Tagger 语义「候选即一首匹配记录」）；切换后可在字段行内手改覆写单字段。
- **写回**：组装 `ScrapeChanges`（仅勾选字段）→ `applyScrapeChanges`（单曲 checkedIds=该 songId）→ journal 可撤销（沿用现有撤销入口，不改）。
- **下一首（批量模式）**：`Success(nextSongId)` + UI 按钮「应用并下一首」；实现采用「popBackStack 回 ScrapeScreen + ScrapeViewModel 推进 pendingReviewIndex + LaunchedEffect 自动 navigate 下一首」，避免跨 VM 持队列导致状态双写。

### 2.3 ScrapeReviewScreen 布局（全屏，TopAppBar 可返回）

```
[TopAppBar: 刮削审核 | 批量模式显示 (i/N)]
[歌曲头：本地标题·歌手·专辑 + 封面小图]
[搜索词行：三个输入框 + 「重新搜索」按钮]
─ 文本字段审核（复用 FieldCheckRow 模式）─
  标题  [✓] 本地值 → 候选值（来源角标 kw/wy/…，置信度角标）
  歌手  [✓] …
  专辑  [✓] …
  [候选切换条：横向 chip 列出文本候选（源名+标题），当前高亮]
─ 封面 ─
  [✓] 横向候选缩略图网格（多源），点选 + 点大图预览
─ 歌词 ─
  [✓] 候选下拉/列表（来源+格式角标），展开预览前几行
[底部：应用并下一首（批量）/ 应用（N）（单曲）]
```

### 2.4 UI 状态（查询中/空态）

- Searching：进度 + 歌名。
- Empty：`no-match` → 「暂无匹配」+ 重试 + 改词引导；`network` → 「触发限流，稍后重试」+ 重试（沿用现有文案）。
- Writing：进度反馈（WebDAV 写回需数秒）。

## 3. 批量流程改造（ScrapeViewModel / ScrapeScreen）

### 3.1 预览态分组（修复「未命中静默消失」）

- `ScrapePageState.Preview` 扩展：增加 `noMatchIds: List<String>`（双链 NO_MATCH，非负缓存型）与既有 `throttledIds` 分开。`startMatching` 中 `continue` 前把未命中 songId 归入 `noMatchIds`。
- `ScrapeScreen` 预览态渲染两组：命中列表（现状逐字段 Checkbox 卡片不动）+ 折叠的「未命中（M 首）」分组（行内重试，复用 `retrySingle`；限流组沿用现有文案与 `retryThrottled`）。

### 3.2 逐首审核接入

- 预览态顶部操作行新增「逐首审核（N）」：navigate 到 `ScrapeReview?songId=首 songId&queue=预览列表`。
- `ScrapeViewModel` 增加 `pendingReviewQueue: List<String>` + `advanceReview()`：审核页写回成功 popBack 后，`LaunchedEffect` 自动打开队列中下一首（从队列剔除已写回者；用户手动返回则清除 pending，不强推）。
- 批量审核里每首的写回走审核页自己的 `applyScrapeChanges`（单曲批次），不与预览「全部应用」的批量批次混用；预览列表在返回后按 `queueStore`/库刷新。
- 预览「全部应用」`confirmWriteback()` 保留不动（不想逐首审核的捷径）。

## 4. 兼容与红线

- **写回安全基线演进**：spec 旧红线「预览候选默认全不选」已被 2a5c4b0d/91b7c13c 演进为「默认勾选有值字段、按钮 enabled 绑定非空勾选」；本设计沿用现行行为（审核页默认勾选推荐候选中有差异字段），不做额外变更。
- **协程红线**：所有 matcher/search 外包 catch 必须前置 rethrow `CancellationException`（spec 陷阱）；AbortSignal 的 `EditSearchAbortedException` 是 CancellationException 子类，重搜取消路径需按引擎既有模式区分处理，不得吞取消。
- **负缓存**：审核页全链不吃负缓存（引擎行为），单曲重试前的 `invalidateNegativeCache` 语义只作用于快链，保持不变。
- **导航回退**：审核页返回不丢 ScrapeScreen 预览态（VM 与页面态都在 ScrapeViewModel）。

## 5. 回滚设计

分四个独立 commit（见 implement.md S1–S4），任一步可单独 revert：
- S1 revert 后回到「孤儿浮层」现状（入口没了但批量流程完好）；
- S2/S3 只增不改预览主体，revert 无副作用；
- 引擎层零改动，无数据迁移（无 Room/DataStore schema 变更）。

## 6. 风险

| 风险 | 缓解 |
|---|---|
| 审核页查询 5 源 + 封面 6 源 + 歌词，单首请求数多 | 仅在用户主动进入审核页时发生；重搜有 abort；限流器全局兜底 |
| 批量逐首审核每首全链查询，20 首很慢 | 逐首审核是用户主动逐首推进的，可随时退出走「全部应用」快链；预览卡片本身仍展示快链结果 |
| popBack 自动下一首与用户手动返回竞态 | pendingReviewIndex 仅在「应用并下一首」路径推进；手动返回即清 pending |
| SingleScrapeSheet 退役影响 | 宿主 `singleScrapeSong` 状态同 commit 内一并清理，无外部引用（grep 验证） |
