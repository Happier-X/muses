# 刮削写回失败排查与修复

## Goal

排查刮削页面 Preview→Result 写回失败原因，覆盖本地与 WebDAV 路径，修复后写回成功且可撤销，Result 页不再出现 file-failed/failed

## Background

- 用户反馈：刮削页勾选候选后点“写回选中”显示失败（`WritebackResult.status != SUCCESS`）
- 涉及链路：`ScrapeViewModel.confirmWriteback` → `WritebackOrchestrator.applyScrapeChanges` → `SongFileWriters` (Local/WebDAV) → `TagWriter.write` (jaudiotagger) → `SongRepository.upsert`
- 成功标准：文件写入 `ok=true` 且 `libraryUpdated=true`，状态为 `SUCCESS`

## Confirmed Facts

- 刮削页四态机为 Queue/Matching/Preview/Result，写回仅对勾选项 `checkedIds` 生效（`ScrapeViewModel.kt: confirmWriteback`）
- 写回分流：本地并行、WebDAV 串行（`WritebackOrchestrator.kt:232-236`）
- 本地路径：`LocalAudioTagFileWriter` 直接 `TagWriter.write(File(song.path))`，失败返回 `write_failed`（`SongFileWriters.kt:51`）
- WebDAV 路径五步：查音源→取密码→构造 URL→下载临时文件→TagWriter→put 上传，失败映射 `no_password/download_failed/write_failed/put_failed`（`SongFileWriters.kt:72-124`）
- `TagWriter.write` 仅对支持的容器返回成功，否则 `write_failed`（`TagWriter.kt:58`）
- 最近一次提交 `31b5cdb5` 未改动刮削写回相关文件，`feature/scrape` 编译通过
- MuMu 上 `adb logcat --pid` 未捕获到 `Writeback/TagWriter` 显式错误，需补充日志与用户现场信息

## Requirements

- R1 诊断：复现失败并获取 `WritebackResult.fileResult.code/message` 与 `error`，区分 `SUCCESS/FILE_FAILED/FAILED`
- R2 修复本地路径：文件不存在、格式不支持、权限不足时给出可操作提示或自动跳过，不阻断其他歌曲
- R3 修复 WebDAV 路径：缺音源/密码、下载/上传失败时返回明确 `code` 并支持单首/批量重试（复用 `retrySingle/retryThrottled`）
- R4 写库一致性：文件成功则 `metaSources` 标记 `EMBEDDED`，文件失败则标记 `SCRAPE`，仅勾选项写库
- R5 回滚可用：写回后 `Result` 页“撤销上次”可恢复曲库旧值（文件不可逆需文案明示）

## Acceptance Criteria

- [ ] AC1 在 MuMu 上对本地与 WebDAV 各至少 1 首可复现歌曲，勾选后“写回选中”返回 `SUCCESS`，`Result` 页显示“成功 1”
- [ ] AC2 人为制造单首失败（如移除本地文件/断开 WebDAV），该首显示 `FILE_FAILED` 且其余成功歌曲仍为 `SUCCESS`，不整体回滚
- [ ] AC3 WebDAV 缺密码场景显示 `no_password` 并引导至音源配置
- [ ] AC4 撤销后曲库标题/歌手/封面/歌词恢复至写回前快照
- [ ] AC5 回归：`./gradlew :feature:scrape:compileDebugKotlin :core:scrape:test` 通过

## Out of Scope

- 新增刮削匹配策略或封面提供方
- WebDAV 文件级事务或增量上传优化

## Open Questions

- Q1 已确认：WebDAV 音源，截图显示 `bf83c4b6` 为 `file-failed`（`WritebackStatus.FILE_FAILED`），具体 `fileResult.code` 待日志确认 —— 已补充详细日志与 UI 详情展示
- Q2 已排查：`tempDir` 仅在 `ScrapeModule` 提供时 `mkdirs`，系统清缓存后可能不存在，已修复为每次写入前 `mkdirs` 并增加 `createTempFile` 异常捕获

## Reproduction Evidence

- 2026-09-02 MuMu 截图 1：`成功 0 · 文件失败 1`，`bf83c4b6` 为 `file-failed`
- 2026-09-02 MuMu 截图 2：`download_failed: 下载失败（HTTP 404）`
- 日志：`WebDavWrite url=https://openlist.happierx.xyz/dav/https%3A/openlist.happierx.xyz/dav/%E5%A4%B8%E5%85%8B.../0321%20-%20space%20x.mp3 serverUrl=https://openlist.happierx.xyz/dav path=https://openlist.happierx.xyz/dav/夸克网盘/我的音乐/0321 - space x.mp3`，确认 `buildWebDavUrl` 将已为完整 URL 的 `song.path` 再次拼接导致双重前缀与 404
- 根因：`WebDavLibraryScanner.filenameSong` 存 `path = item.url`（完整 URL），而 `WebDavAudioTagFileWriter` 始终 `buildWebDavUrl(serverUrl, song.path)` 按相对路径编码，导致 `serverUrl + encode(fullUrl)` 非法
