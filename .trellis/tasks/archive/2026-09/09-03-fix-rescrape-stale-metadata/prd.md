# 修复重新刮削后播放仍显示旧元信息

## Goal

修复"重新刮削后再次播放仍显示旧元信息"回归问题，保证刮削写入的元信息在播放、列表、迷你条等所有展示入口立即可见且不被后续播放流程覆盖。

## 背景

- 刮削写回 `WritebackOrchestrator` 同时写文件与数据库，数据库为展示源
- 播放时 `PlaybackService` 对 `tagsVersion < 1` 的歌曲执行懒扫描（`AudioTagReader` 读文件标签回填库）
- 播放 UI（沉浸页粘性封面/歌词、迷你条、歌曲列表当前行）存在"优先用 ExoPlayer 容器解析结果 vs 库值"的分支

三者叠加导致：已重新刮削并入库的新标题/歌手/专辑/封面，在下一次播放时被懒扫描或 UI 分支用旧文件标签覆盖。

## Requirements

### R1 播放懒扫描不得覆盖已刮削字段（P0）
- 触发条件保持 `tagsVersion < TAGS_VERSION` 不变
- 对已通过刮削标记来源的字段（`metaTitle/metaArtist/metaAlbum/metaCover` 非空，或 `metaSources` 对应字段非空）跳过覆盖
- 未刮削字段仍允许懒扫描补齐（时长、歌词等未标记字段）
- WebDAV 文件写入失败（`FILE_FAILED` 仅库内 `SCRAPE` 标记）场景下，被标记字段必须保留库内刮削值

### R2 播放 UI 优先展示刮削后的库值（P0）
- 沉浸页/迷你条 `NowPlayingUiState` 与歌曲列表当前行标题副标题展示逻辑：当某字段已被刮削标记（`metaSources.field != null`）时，优先使用库内 `SongEntity` 值而非 `Player.mediaMetadata` / `MediaItem.mediaMetadata`
- 未标记字段保持现有回退链路不变
- 粘性封面 `stickyCover` 同步遵循该优先级

### R3 刮削成功后缓存失效（P0）
- 本地与 WebDAV 写文件成功后，失效 `AudioTagReader` 针对该路径/URL 的内存与磁盘缓存，确保后续懒扫描或主动读标签拿到新文件内容
- 失败/未写文件分支不主动清缓存

### R4 队列快照陈旧（P1，可选增强）
- 若当前播放队列内已存在被重新刮削的歌曲，其 `MediaItem.mediaMetadata` 应在刮削库更新后得到刷新，或至少下一次以该歌曲为起点 `play()` 时使用最新库值
- 不要求对通知栏/系统媒体会话做额外同步，聚焦应用内可见入口

### R5 回归与兼容（P0）
- 未刮削歌曲行为不变：`tagsVersion=0` 首次播放仍能通过懒扫描补齐
- 本地与 WebDAV 两类音源均需覆盖
- 多次重新刮削同一歌曲，以最后一次刮削结果为准

## Constraints

- 仅改动 `core:scrape/writeback`、`core:data/tag`、`core:media/playback`、`feature:player`、`app/navigation` 五处以内模块
- 不提升 `TAGS_VERSION` 数值，不做全量 `tagsVersion` 抬升迁移（避免存量库误判）
- 保持 `WritebackOrchestrator` 五步流程与 `WritebackStatus` 分类不变
- UI 改动仅调整取值优先级，不改布局与交互

## Acceptance Criteria

- [ ] **AC1 懒扫描不覆盖**：准备一首 `tagsVersion=0` 的 WebDAV 歌曲 → 刮削标题为新值（模拟文件写 `FILE_FAILED`，库内 `metaTitle=SCRAPE`）→ 触发播放切歌（`onMediaItemTransition`）→ 库内标题保持刮削新值，未被读文件标签覆盖
- [ ] **AC2a UI 优先库值（迷你条/沉浸页）**：同上歌曲刮削后，不重启应用直接观察迷你条与沉浸页标题，显示为库内新标题而非旧文件标题
- [ ] **AC2b 列表当前行优先库值**：同上，在歌曲列表中该歌曲为当前播放行时，标题/副标题优先显示库内新值
- [ ] **AC3 缓存失效**：本地歌曲刮削含封面写入成功后，`AudioTagReader.readTags(source)` 对该路径能够读到新封面字节（而非旧缓存），或至少下一次懒扫描不返回旧字节
- [ ] **AC4 队列刷新（若实现 R4）**：队列已包含歌曲 A（旧标题）→ 刮削 A 得新标题 → 再次 `play(A.id, 最新 songs 列表)` 或队列内切回 A 时，通知与迷你条标题为新值
- [ ] **AC5 回归**：未刮削的 `tagsVersion=0` WebDAV 歌曲首次播放仍能通过懒扫描写入标题/封面并将 `tagsVersion` 置 1；本地 `readTags=true` 的歌曲无回归
- [ ] **AC6 单测**：`PlaybackService` 懒扫描分支与 `WritebackOrchestrator` 缓存失效分支有单测覆盖，`MusesApp`/`SongsPage` 优先级分支可通过 ViewModel 单测或手测验证
