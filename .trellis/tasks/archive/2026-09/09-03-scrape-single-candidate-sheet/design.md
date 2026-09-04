# 设计：单曲即时刮削候选浮层

## 1. 模块边界

```
feature:scrape/SingleScrapeSheet.kt        新增：BottomSheet 候选浮层
feature:scrape/SingleScrapeViewModel.kt    新增：单曲匹配状态机（idle/searching/result）
feature:library/SongsPage.kt               扩展：⋮ 菜单新增“刮削”入口，拉起浮层
core:scrape 已有 TextMetaMatcher/CoverMatcher/EditCloudMetaSearch 复用
```

## 2. 状态机

```
SingleScrapeUiState
  Idle | Searching(keyword) | HasCandidates(List<Candidate>, selectedIndex) | Empty(reason) | Writing | Success | Error
Candidate
  songId, current{title/artist/album/lyrics/coverUri}, matched{title/artist/album/coverUrl/lyrics}, confidence, coverUrl, lyrics
```

## 3. 匹配流程

- 入口传 `Song`，`viewModel.search(song)` 并行跑 `textMatcher.match` + `coverMatcher.match` (+ 可选 `editCloudMetaSearch` 的 lyrics 维)，超时 8s
- 文本/封面任一命中即视为 HasCandidates，取最优 hit（`pickBestHit` 已排序）与最优封面 URL
- 限流/网络异常归为 Empty(reason=NETWORK) 展示重试

## 4. 展示与编辑

- 浮层顶部：歌名 + 关闭
- 候选卡：原/新逐字段对比，`新` 行变更高亮，封面 `AsyncImage` 48dp 可点放大（Dialog），歌词摘要 2 行预览
- 单选：首版单候选默认选中，多候选时 Radio
- 编辑：卡内“编辑”按钮复用 `PreviewEditSheet` 的三字段+歌词 BottomSheet，编辑回填 `candidate.edit*`

## 5. 写回

- 选中候选的 `resolved*` 组装 `ScrapeChanges(title/artist/album/coverRemoteUrl/lyrics)` 调 `writebackOrchestrator.applyScrapeChanges` 单条
- 成功 Toast，关闭浮层，Room Flow 自动刷新列表/播放侧

## 6. 兼容

- 队列路径保留，单曲浮层不写队列
- 封面落库复用之前 `effectiveCoverUri` 修复，保证 `coverUri` 立即可见
