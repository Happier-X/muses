# PRD：刮削历史记录功能

## 目标

新增刮削历史：能看到每次写回成功/失败、失败原因、哪些歌曲刮削过。

## 方案（用户已确认）

### 数据层：新增 `src/features/scrape/history.ts`

- 存储 key：`muses:scrape-history`（localStorage）
- 记录结构（每首歌一条）：
  ```ts
  interface ScrapeHistoryEntry {
    id: string                // 唯一 id
    journalId: string         // 批次号
    songId: string
    songTitle: string         // 歌名快照（防删歌）
    songArtist?: string       // 艺术家快照
    at: string                // ISO 时间
    status: 'success' | 'file-failed' | 'failed'
    failureReason?: string    // 复用 describeWritebackFailure 文案
    changedFields: string[]   // 本次写回字段：title/artist/album/cover/lyrics
  }
  ```
- API：`loadScrapeHistory()`（时间倒序）、`appendScrapeHistory(entries)`（批量追加 + 滚动清理超 200 条的旧记录）、`clearScrapeHistory()`
- 变更广播事件 `muses:scrape-history-updated`（与 queue.ts 的模式一致）

### 写入时机

- `writeback.ts` `applyScrapeChanges` 返回 results 后，由调用方（ScrapePage 的 onWriteback / onRetryFailed）或 writeback 内部按 results 落记录——放 writeback.ts 内部统一收口（确认写回与重试都经过它，不漏记）
- changedFields 从 changesMap 取 Object.keys 映射为可读字段名

### UI：ScrapePage 顶部栏「历史」入口

- 队列态顶部加「历史」按钮 → 打开弹层（MDialog/MSheet 或页内状态切换，取实现最简者）
- 列表：时间倒序；每行 = 状态标记（✓/⚠/✗ 颜色区分）+ 歌名（副行艺术家）+ 相对时间；失败的显示失败原因
- 弹层底部「清空历史」带确认（复用 MDialog 确认模式）
- MVP 不做从历史重试

## 验收标准

1. 单测：history.ts 的追加/滚动清理（>200 条）/清空/事件广播
2. MuMu 实测：写回成功与失败各产生一条记录；历史列表展示正确；清空生效；删歌后历史仍显示快照信息
3. 重试产生的记录也入史（同一首歌多条记录并存）
4. lint / test:unit / build 全过（禁止管道吞退出码）

## 范围外

- 历史重试、筛选/搜索、导出
- 不改 writeback 编排语义（只在完成点旁路落记录）

## 约束

- 遵循 queue.ts 既有存储模式（版本化 snapshot + 懒清理风格一致）
- 样式遵循 Salt token 与 ScrapePage 现有体系
