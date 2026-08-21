# 执行计划

## 前置

- [ ] 阅读 `.trellis/spec/frontend/` 相关 spec（state-management.md、features-player.md、features-scrape.md、quality-guidelines.md）。

## Step 1：来源标记扩展（R5）

- [ ] `src/features/library/types.ts`：`LyricsSource`、`FieldSource` 增加 `'scrape'`，更新注释。
- [ ] `src/features/library/storage.ts`：sanitize/校验接受 `'scrape'`。
- [ ] `src/features/scrape/writeback.ts`（193-213 行附近）：写回失败分支改标 `'scrape'`。
- [ ] `src/features/scrape/suspicious.ts`：可疑判定纳入 `'scrape'`。
- [ ] 全局搜索 `'online'` / `'cloud'` 字面量，确认无遗漏归属。

## Step 2：存量清理迁移（R6）

- [ ] `src/features/library/storage.ts`：新增一次性迁移（localStorage 打标 `muses:migration:local-first-v1`），按 design.md 清洗规则实现，manual 字段跳过。

## Step 3：播放器移除自动在线匹配（R1-R3）

- [ ] 删除 `prefetchMetadata.ts`；controller 中音频预取保留、元信息预取移除。
- [ ] controller.ts：删除 `matchOnlineLyricsForSong`、`matchOnlineTextMetaForSong`、`matchOnlineCoverForSong` 及全部调度点/token。
- [ ] controller.ts：切歌歌词改为直接取库内值；简化 `syncDisplayStateFromSong` 覆盖逻辑。
- [ ] `player/types.ts`：删除无调用方的质量比较函数；`OnlineLyricsStatus` 收敛枚举。
- [ ] `PlayerPage.vue`：空态文案与 matching/error 分支清理；歌词来源 UI 兼容新枚举。

## Step 4：测试与验证

- [ ] 更新受影响单测；新增迁移清洗 + scrape 标记往返单测。
- [ ] 验证命令：
  ```bash
  pnpm lint
  pnpm type-check   # 或 npx vue-tsc --noEmit，以 package.json scripts 为准
  pnpm test         # 或 vitest run
  ```
- [ ] 手工验收 AC1/AC2（断网播放）、AC5（刮削写回两分支）。

## 回滚点

- 每个 Step 独立可提交；Step 2 迁移一旦执行数据不可恢复（已获用户接受），代码回滚只能阻止后续清理。

## 完成前检查

- [ ] AC1-AC7 逐条核对。
- [ ] spec 更新：`state-management.md:247` 歌词优先级描述需改写为本地来源语义。
