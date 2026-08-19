# 曲库 tag 治理（parent）——执行计划（implement）

## 依赖顺序

```
child1 meta-source-tracker（数据模型 + 队列存储）
  └→ child2 scrape-queue-entry（三个入队入口，依赖队列 API）
  └→ child3 scrape-batch-flow（刮削页 + 写回，依赖来源追踪 + 队列）
child4 scrape-match-quality（评分/门槛，独立，但被 child3 消费 → 先合入或并行）
```

推荐执行顺序：**child1 → child2 || child4(并行) → child3 集成**。

## 实施清单（parent 视角有序步骤）

### 阶段 A：child1 数据模型
- [ ] A1 `types.ts`：新增 `MetaFieldKey` / `FieldSource` / `SongItem.metaSources?`；`CURRENT_METADATA_VERSION` → 4；`isSongItem` 校验扩展（缺省合法）。
- [ ] A2 `storage.ts`：`upsertSong` 来源写入规则（扫描/云补 → embedded/cloud；不写空来源）；`updateSongUserEdit` 同步清理 metaSources；导出读取来源辅助函数 `getFieldSource(song, key)`（manual 由 userEditedFields 派生）。
- [ ] A3 `scanner.ts` / `controller.ts` / `prefetchMetadata.ts`：扫描/懒扫写 embedded；在线补缺写 cloud。
- [ ] A4 队列存储 `features/scrape/queue.ts`：load/enqueue/remove/clear/事件广播/懒清理（挂 reconcile 或读取时过滤）。
- [ ] 验证：`npm run test:unit`（新增 storage/queue 单测）；vue-tsc build。

### 阶段 B：child2 入队入口
- [ ] B1 播放页歌曲操作菜单「标记待刮削/取消标记」。
- [ ] B2 歌曲列表长按菜单 + 多选条「标记待刮削」。
- [ ] B3 可疑歌曲筛选批量入队（判定函数 + 确认框 + toast）。
- [ ] 验证：build；手动走查三个入口幂等性。

### 阶段 C：child4 匹配质量（可与 B 并行）
- [ ] C1 `lyrics/score.ts`：门槛重构（exact 无 artist 可采、contains 需 artist、duration 约束）。
- [ ] C2 `metadata/util.ts`：`needsOnlineTextMeta` 约束强化。
- [ ] C3 `player/types.ts` `shouldPersistOnlineLyrics` 同步收紧。
- [ ] 验证：score 单测更新 + 新增用例；回归「播放时在线歌词」行为。

### 阶段 D：child3 刮削流程与写回
- [ ] D1 `features/scrape/matcher.ts`：队列 → 三路匹配聚合（复用 searchEditCloudMeta），返回候选 + 置信度（不写库）。
- [ ] D2 `features/scrape/writeback.ts`：写回编排（文件桥 + upsert + 回滚 journal + 逐行状态 + 重试）。
- [ ] D3 `views/ScrapePage.vue` + 路由 `/scrape`：三态 UI（队列→匹配→差异预览→应用），逐行勾选/候选选择/整批操作/撤销。
- [ ] D4 在线补缺写 cloud 来源 + 刮削后 token 作废（A2/C3 联动收口）。
- [ ] 验证：build；vitest（writeback/rollback）；真机本地+WebDAV 写回成功与失败路径。

### 阶段 E：parent 集成验收
- [ ] E1 端到端走查 AC1–AC6（见 parent prd）。
- [ ] E2 回归 AC7（扫描/懒扫/在线补缺/手动编辑不变；单测、lint、build 全绿；原生单测）。
- [ ] E3 真机验证 AC8：本地文件写回 + WebDAV 写回各一次成功/失败路径。
- [ ] E4 spec 更新：`features-player.md` / 新增 `features-scrape.md` 记录来源语义、队列、写回与回滚边界。

## 验证命令

```bash
npm run lint
npm run build          # vue-tsc + vite
npm run test:unit      # vitest（含新增 storage/queue/score/writeback 用例）
cd android && ./gradlew :app:testDebugUnitTest   # 原生回归（本轮不改原生，确认无破坏）
cd android && ./gradlew :app:assembleDebug       # 打真机包
```

## 风险与回滚点

- **写文件不可逆**：抓实 D2 回滚 journal（库级回滚）；UI 明示「文件已写不可撤」，避免误承诺。
- **匹配门槛收紧影响播放体验**：C1–C3 改动后必须在验收 E2 专项回归「播放时在线歌词自动写库」，若发现高置信不触发再调阈值。
- **metaSources 与 userEditedFields 双轨**：A2 明确派生关系（manual 不单独存储），避免双写不一致。
- **child3 依赖 child4 的置信度函数**：若并行开发，先合入 C1 的评分函数骨架（不动阈值）再 D1。

## 交付物清单

- 4 个 child 各自 prd.md（含各自验收），child 内不再拆分。
- parent 集成验收 + spec 更新 + 归档。