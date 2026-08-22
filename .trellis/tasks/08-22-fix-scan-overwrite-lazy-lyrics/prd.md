# PRD：修复批量扫描覆盖播放器懒扫描写入的歌词

## 背景与症状

用户反馈：歌曲明明有内嵌歌词，播放时也正常显示，但重启 App 后歌词消失。

## 根因

两条独立写库路径存在丢失更新竞态：

1. **批量扫描**（`src/features/library/scanner.ts` `scanSourceLibrary`）：开始时 `loadSongs()` 捕获基线快照 → 循环期间仅内存修改（`persist:false`）→ 结束时一次性 `saveSongs(内存数组)`
2. **播放器懒扫描**（`src/features/player/controller.ts` `scanSongMetadata`）：读到内嵌 tag 后立即 `upsertSong(..., loadSongs())` 写库落盘

当批量扫描进行中（WebDAV 可持续数十秒到几分钟）用户播放歌曲触发懒扫描写入后，批量扫描结束时用它基于旧基线的内存数组整体覆盖曲库，把懒扫描写入的歌词冲掉。

已排除：localStorage 配额（实测 599KB/5120KB）、手动编辑清空（有 baseline diff 保护）、upsert 字段覆盖（`?? previous` 合并保护）、v0.3.9 存量迁移（只清 online 来源且幂等）。

## 方案（用户选定 A）

批量扫描最终提交前**重新读取最新曲库并 rebase**：把本次扫描收集到的每个文件的 upsert 输入重放到最新曲库上，再执行对账与统一写库。这样扫描期间其他写入者（懒扫描、用户编辑等）的变更得以保留；同一首歌双方都写过时由 upsert 既有的非破坏性字段合并（`?? previous`）兜底。

### 改动要点（scanner.ts）

- 循环中记录每个文件构造的 upsert 输入（或在拿到最新库后重放）
- 提交阶段：`const latest = loadSongs()` → 将收集的输入逐个 `upsertSong(input, latest, { persist:false })` 重放 → `reconcileSourceSongs(source.id, keepPaths, 重放结果)` → 有变化才 `saveSongs`
- 对账仍以本次列出的路径为准（语义不变）

## 验收标准

1. 新增单测：模拟「基线捕获后、扫描提交前，外部路径为某歌写入歌词」，扫描提交后该歌词仍存在
2. 扫描既有语义不变：新增/更新/skip 计数、对账删除不在 keepPaths 的旧歌
3. 同一歌曲双方都写入时不丢任何一侧的非冲突字段
4. `npm run typecheck`、`npm run test:unit`、`npm run build` 全部通过（禁止管道吞退出码）

## 约束

- 不改播放器懒扫描路径
- 不引入新的持久化机制
