# 修复安卓本地歌曲写回分流缺口

## Goal

安卓 `ScrapeModule` 的 `AudioTagFileWriter` 绑定只有 `WebDavAudioTagFileWriter`，本地歌曲（sourceType=LOCAL）写回会命中 WebDAV writer 的 `no_password` 失败分支——本地文件根本不该走 WebDAV 下载-写-上传链。按 Web 规格书 `writeback.ts` 的 `writeFile` 分派语义补 sourceType 分流。

## Background（已实测）

- `core/common SongFileWriters.kt`：`AudioTagFileWriter` 为 fun interface，`LocalAudioTagFileWriter`（本地直写，构造只需 tagPort）与 `WebDavAudioTagFileWriter`（下载-写标签-上传）均已 commonMain 化。
- 安卓 `core/scrape di/ScrapeModule.kt:130` 仅 `single<AudioTagFileWriter> { WebDavAudioTagFileWriter(...) }`，无分流。
- 桌面 `DesktopScrapeGraph.kt`（W4）已按正确语义分流：`WEBDAV → webdavWriter; else → localWriter`，可直接参照。
- 缺口在 W3 迁移前后均存在（历史问题），桌面侧已在 W4 修正；本任务收尾安卓侧。

## Requirements

- R1：`ScrapeModule` 的 `AudioTagFileWriter` 绑定改为分流 lambda（`WEBDAV → WebDavAudioTagFileWriter`，其余 → `LocalAudioTagFileWriter(tagPort)`），与桌面分流语义逐字一致。
- R2：写回后行为不变项冻结：WebDAV 路径零改动；`LocalAudioTagFileWriter` 复用既有 `TagPort` 单例；不新增版本线依赖。

## Acceptance Criteria

- [ ] AC1：安卓本地歌曲写回走 `LocalAudioTagFileWriter`（不再命中 no_password）。
- [ ] AC2：安卓 WebDAV 写回行为零变化。
- [ ] AC3：全量回归通过（`:app:assembleMusesDebug` + `testDebugUnitTest` + `:core:scrape:testDebugUnitTest`）。

## Out of Scope

- 不动桌面侧（已正确）；不动写回引擎语义。

## Key Decisions

- D1：分流放 Koin 绑定层（与桌面同构），不进引擎（引擎契约保持单 writer 注入）。
