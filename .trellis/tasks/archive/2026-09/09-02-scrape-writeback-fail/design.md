# 设计：刮削写回失败排查与修复

## 背景与边界

- **模块边界**：`feature/scrape`（`ScrapeViewModel`/`ScrapeScreen`）→ `core/scrape`（`WritebackOrchestrator`/`SongFileWriters`/`TagWriter`）→ `core/data`（`SongRepository`/`SourceRepository`/`CredentialsRepository`）→ `core/webdav`（`WebDavClient`/OkHttp）
- **不涉及**：匹配策略（`TextMetaMatcher`/`CoverMatcher`）、封面提供方、历史存储结构变更
- **兼容约束**：历史 `song.path` 已存为完整 URL（`item.url`）与相对路径两种形态需同时兼容；`.trellis/spec` 无刮削专属约束，保持现有 `WritebackResult` 契约

## 数据流与契约

```
Preview(checkedIds) 
  → ScrapeViewModel.confirmWriteback(checkedIds, changesMap)
    → WritebackOrchestrator.applyScrapeChanges(candidates, checkedIds, changesMap)
      ├─ snapshot → RollbackJournal (200 条上限)
      ├─ 分流：localQueue 并行 / webdavQueue 串行
      │   └─ writeOne(song, changes)
      │       ├─ fetchCoverBytes(coverRemoteUrl)
      │       ├─ fileWriter.write(song, TagWriteRequest) → FileWriteResult(ok, code, message)
      │       │   ├─ Local: TagWriter.write(File(song.path))
      │       │   └─ WebDAV: getSource → getPassword → buildUrl → get(tempFile) → TagWriter.write(tempFile) → put(url, tempFile)
      │       └─ updateSongInLibrary(songId, changes, fileOk) → upsert + metaSources标记
      └─ historySink → ScrapeHistoryStore
  → Result(results, journalId)
```

- **WritebackResult 契约**：`status = SUCCESS|FILE_FAILED|FAILED`，`FILE_FAILED` 仅 `fileResult.ok==false`，`libraryUpdated` 仍可为 `true`（`EMBEDDED` vs `SCRAPE` 标记区分）
- **错误码契约**：`no_password` / `download_failed` / `write_failed` / `put_failed` / `unknown`，`Result` 页透出 `fileResult.code:message` 供诊断

## 关键设计决策

### 1. WebDAV URL 双重前缀根因修复

- **现象**：`bf83c4b6` 的 `song.path = https://openlist.happierx.xyz/dav/夸克网盘/.../0321 - space x.mp3`（`WebDavLibraryScanner.filenameSong` 存 `item.url` 完整 URL），而 `WebDavAudioTagFileWriter` 固定 `buildWebDavUrl(serverUrl, song.path)` 将完整 URL 当相对路径编码 → `https://.../dav/https%3A/...` 404。
- **决策**：在 `SongFileWriters.webDavWrite` 中先判 `song.path` 形态：
  - 以 `serverUrl` 开头：抽取后缀 `removePrefix(serverUrl)` 后重走 `buildWebDavUrl`（正确重编码中文/空格）
  - 以 `http(s)://` 开头但非当前 `serverUrl`：按自身 `scheme+authority` 重建
  - 否则按相对路径原逻辑
- **权衡**：不改 `WebDavLibraryScanner` 存量数据（避免大规模迁移与历史路径失效），在写入侧兼容双形态；未来新扫描可考虑存相对路径以减少冗余

### 2. 临时文件扩展名保留

- **现象**：`write_failed: No Reader associated with this extension:tmp`，`File.createTempFile(...,".tmp")` 致 `jaudiotagger` 无法按扩展名选 Reader。
- **决策**：从 `song.path` 提取真实后缀（截 `?`/`#`，长度≤5 且字母数字才视为扩展名），`createTempFile` 时传入该后缀（如 `.mp3`），确保 `AudioFileIO.read` 正确识别容器。

### 3. 临时目录存在性兜底

- **现象**：`ScrapeModule` 仅在 `provide` 时 `mkdirs`，系统清 `cacheDir` 后 `tempDir` 不存在，`createTempFile` 抛 `IOException`。
- **决策**：写入前 `if (!tempDir.exists()) tempDir.mkdirs()`，并包裹 `createTempFile` 异常返回 `download_failed` 带文案，不抛至 `FAILED`。

### 4. UI 遮挡与诊断可观测性

- **按钮遮挡**：`ScrapeScreen` 三处底部固定 `Row`（Queue/Preview/Result）被 `TabsLayout` 的 `MiniPlayerBar`（`BottomCenter` 叠加）遮挡。决策：`Row` 增加 `navigationBarsPadding().padding(bottom=80dp)`（`16dp` 原边距 + `64dp` MiniPlayer 高度，对齐 `SongsPage` 的 `96dp` 口径），`LazyColumn` 保持 `weight(1f)`。
- **诊断**：`WritebackOrchestrator.writeOne` 与 `WebDavAudioTagFileWriter` 各阶段 `Log.w/e`（`Writeback`/`WebDavWrite`），`Result` 页对 `FILE_FAILED` 行增显 `fileResult.code:message`，无需抓 `logcat` 即可定位细分。

## 兼容与回滚

- **兼容**：双形态 `path` 兼容、扩展名回退 `.tmp`、日志仅追加不改契约
- **回滚**：任一写回失败仅影响单首，其余 `SUCCESS` 不回滚；整体可通过 `journalId` `revertScrapeJournal` 恢复曲库（文件不可逆已在 UI 明示）
- **发布**：`assembleMusesDebug` 验证 + MuMu 手工回归 `bf83c4b6` 单首与批量

## 风险

- `jaudiotagger` 对部分容器（如 `wav` 无标签）仍可能 `write_failed`，属预期 `FILE_FAILED`，需在 `Result` 文案引导用户知悉文件格式限制
- WebDAV 服务器对 `PROPFIND` 返回的 `href` 编码不一致可能导致后缀提取偏差，已通过 `Uri.parse` 容错
