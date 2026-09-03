# 实施计划：单曲即时刮削候选浮层

## 任务分解

- [ ] **T1 ViewModel** — `SingleScrapeViewModel` 匹配编排（text+cover (+lyrics 可选)）
- [ ] **T2 浮层 UI** — `SingleScrapeSheet` 三态与候选卡对比、封面/歌词预览
- [ ] **T3 编辑与写回** — 复用编辑 BottomSheet，回填候选，调用 Writeback
- [ ] **T4 入口** — `SongsPage` ⋮ 菜单新增“刮削”拉起浮层
- [ ] **T5 封面可见回归** — 验证 `effectiveCoverUri` 落库后列表/播放侧立即可见

## 依赖顺序

T1 → T2 → T3 → T4 → T5

## T1 ViewModel

**文件**：`feature/scrape/SingleScrapeViewModel.kt`

**步骤**：
1. 注入 `TextMetaMatcher`, `CoverMatcher`, `SongRepository`, `WritebackOrchestrator`
2. `search(song)`：并行 `textMatcher.match` / `coverMatcher.match`，聚合 Candidate
3. `select(index)`, `updateCandidate(index, title, artist, album, lyrics)`, `apply()`

## T2 浮层

**文件**：`feature/scrape/SingleScrapeSheet.kt`

**步骤**：
1. `ModalBottomSheet` + `Searching`/`HasCandidates`/`Empty` 分支
2. 候选卡复用预览行的原/新对比与封面缩略

## T3/T4

**文件**：`feature/library/SongsPage.kt` , `SingleScrapeSheet.kt`

**步骤**：
1. `SongsPage` 持有 `selectedScrapeSong` 状态，点击 ⋮ “刮削” 设值
2. `SingleScrapeSheet(song)` 挂载，`onDismiss` 清空

## 验证

- 单曲刮削→对比可见→编辑→应用→库内更新→播放可见
- 无命中/限流重试无回归
