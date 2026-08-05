# 设计：编辑用云端多候选 API

## 方案

新增 `src/features/editMeta/` 编排 `searchEditCloudMeta`，**不改**播放 `matchOnline*` 语义。

MVP 多候选策略（满足 D5，控制改动面）：

- **文本**：对每个 text provider 调现有 `search`（每源 1 条最优）→ 合并去重 → 按 `scoreTextHit` 排序；**不**走 `needsOnlineTextMeta` / `hitFillsMissing` / 播放负缓存。
- **封面**：对每个 cover provider 调 `searchCoverUrl`，**不** first-stop → URL 去重；不落盘。
- **歌词**：amll + 各 fallback **全跑**可收集（命中即 push，不因首命中停止）；上限 `MAX_CANDIDATES`（8）。

每维 `defaultIndex = 0`（排序后最优）。`status`: 有 items → ok；全网错误 → network；否则 no-match。

## 取消

可选 `signal?: AbortSignal`；循环中检查 `signal.aborted`。

## 导出

`@/features/editMeta` → `searchEditCloudMeta` + types。
