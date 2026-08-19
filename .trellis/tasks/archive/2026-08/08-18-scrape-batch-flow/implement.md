# child3：批量刮削流程页——实现计划

## 依赖顺序

child1（队列+来源）✅、child4（置信度）✅ 已完成。child3 可直接开始。

## 实施清单

### 阶段 D1：匹配聚合（features/scrape/matcher.ts）
- [ ] D1.1 定义 `ScrapeCandidate` 类型：{ songId, text: { current, candidates, confidence }, cover: { current, candidates }, lyrics: { current, format, confidence } }
- [ ] D1.2 `matchScrapeQueue(queue)`: 输入 queue snapshot，resolve 到 SongItem，逐曲调用 searchEditCloudMeta，返回 `ScrapeCandidate[]`
- [ ] D1.3 并行编排：p-limit(3) 控制并发（复用现有的 simple-concurrency-limit 思路），单曲失败不影响其他
- [ ] D1.4 单测：mock searchEditCloudMeta，验证匹配/失败/并发限制

### 阶段 D2：写回逻辑（features/scrape/writeback.ts）
- [ ] D2.1 `RollbackJournal` 类型 + localStorage 存取（上限 200 条）
- [ ] D2.2 `applyScrapeChanges(song, changes, options)`: 写文件（本地/WebDAV）+ 写库（upsertSong），返回逐行结果
- [ ] D2.3 `revertScrapeJournal(journalId)`: 恢复曲库旧值（文件级不可逆，UI 明示）
- [ ] D2.4 单测：mock writeLocalAudioMetadata + upsertSong，验证成功/失败/回滚

### 阶段 D3：ScrapePage.vue 完整实现
- [ ] D3.1 三态管理：queue → matching → preview → result（ref state machine）
- [ ] D3.2 队列列表：显示待刮削歌曲，可移除/清空
- [ ] D3.3 匹配中：进度条/计数 + 取消（停止后续匹配）
- [ ] D3.4 差异预览表：逐行 title/artist/album/cover/lyrics 当前值 vs 候选
  - 置信度徽标（高=绿/低=黄）
  - 高置信默认勾选，低置信默认不勾
  - 逐行切换 + 全选/全不选
  - 点行展开候选列表（如有多个候选）
- [ ] D3.5 确认写回：调用 applyScrapeChanges，逐行更新状态
- [ ] D3.6 结果态：成功/文件失败/失败 可视化 + 重试按钮
- [ ] D3.7 撤销：revertScrapeJournal + 文件不可逆提示

### 阶段 D4：在线补缺联动（controller.ts / prefetchMetadata.ts）
- [ ] D4.1 刮削写回后作废旧在线补缺 token（防并发覆盖）
- [ ] D4.2 在线补缺写库字段带 cloud 来源（child1 已实现，确认无遗漏）

### 阶段 E：验证
- [ ] E1 vue-tsc + eslint 全绿
- [ ] E2 vitest 新增 writeback/rollback/matcher 用例 ≥ 8 例
- [ ] E3 vite build 通过
- [ ] E4 真机验证：本地写回成功/失败 + WebDAV 写回成功/失败

## 关键文件清单

| 文件 | 变更 |
|------|------|
| `src/features/scrape/matcher.ts`（新建） | 匹配聚合编排 |
| `src/features/scrape/writeback.ts`（新建） | 写回 + 回滚 journal |
| `src/views/ScrapePage.vue`（重写） | 完整三态 UI |
| `src/features/library/storage.ts` | upsertSong（child1 已改，确认） |
| `src/features/player/controller.ts` | token 作废（确认在线补缺联动） |
| `tests/unit/scrape-writeback.spec.ts`（新建） | 写回/回滚单测 |
| `tests/unit/scrape-matcher.spec.ts`（新建） | 匹配聚合单测 |

## 验证命令

```bash
npx vue-tsc --noEmit
npx eslint src/features/scrape/*.ts src/views/ScrapePage.vue
npx vitest run
npx vite build
```
