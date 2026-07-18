# 技术设计

## 根因

`scanSourceLibrary` 仅对本次发现的文件调用 `upsertSong`，从未删除 `sourceId` 下已不存在的路径。空目录/空 WebDAV 列表时循环不执行，旧记录完整保留。

## 方案

1. 在 `src/features/library/storage.ts` 增加按音源对账 API（例如 `reconcileSourceSongs(sourceId, keepPaths, existingSongs?)`）：
   - 保留：`sourceId` 不同的歌曲，或路径属于 `keepPaths` 的该音源歌曲；
   - 删除：同 `sourceId` 且路径不在 `keepPaths` 的歌曲；
   - 有删除时 `saveSongs` 并返回 `removed` 计数与新列表。
2. 在 `scanSourceLibrary` 中：
   - 发现阶段成功后收集 `file.path` 集合；
   - upsert 循环结束后（含发现 0 文件）调用对账；
   - 发现阶段抛错时不调用对账。
3. `ScanSummary` 增加 `removed`，进度与完成文案展示移除数。
4. 单测覆盖：空扫描清库、部分路径删除、跨音源隔离、发现失败不删。

## 边界

- 对账依据是“本次成功列出的路径”，不是“本次 upsert 成功的路径”。单文件入库失败仍保留该路径旧记录（若有），且不会因失败而整源清空。
- 不在 UI 层手写过滤；同步逻辑集中在 library 层。

## 验证与回滚

验证：`npm run lint` / `build` / `test:unit`。回滚：移除对账调用与 `removed` 字段即可恢复仅 upsert 行为。
