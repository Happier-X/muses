# 刮削后封面与歌词可见

## Goal

刮削后封面与歌词在播放页、列表与迷你条中立即可见，预览页可视封面缩略与歌词摘要并支持编辑后写回。

## 背景

- 当前预览仅标题/歌手/专辑与封面 URL，歌词维度未参与匹配与写回，刮后仍无词
- 封面虽在日志中 `fileOk=true` 但用户仍看不到，疑似 DB `coverUri` 与展示链路优先级或缓存/回退被旧封面覆盖
- 播放侧 `stickyCover` 与 `lyrics` 依赖 `SongEntity.coverUri/lyrics`，需保证刮后值不被懒扫描或 `mediaMetadata` 抢占

## Requirements

### R1 封面可见（P0）
- 刮削命中封面时 `coverUrl` 正确落库为 `coverUri`，文件嵌入成功后 `AudioTagReader` 缓存已失效，下一次播放不读旧封面
- 播放侧 `MainViewModel.nowPlaying` / `SongsPage` 当前行 / `PlayerViewModel.stickyCover` 对 `metaCover != null` 的已刮削封面优先展示库值，不回退旧 `mediaMetadata.artwork`

### R2 歌词预览与写回（P0）
- 预览每行展示歌词摘要（有则显示前 30 字，无则“—”），并提供编辑入口
- 写回时 `ScrapeChanges.lyrics/lyricsFormat/lyricsSource` 随 `coverRemoteUrl/title/artist/album` 一并落库，成功后 `lyricsSource` 标记 `SCRAPE/EMBEDDED` 按文件结果
- 懒扫描对 `lyricsSource != null` 的已刮削歌词跳过覆盖（已在 09-03-fix-rescrape 中实现，需验证）

### R3 预览编辑（P0）
- 歌词编辑复用标题/歌手/专辑的 BottomSheet 模式，空视为不改该字段

### R4 回归（P1）
- 未命中封面/歌词时不污染原值，仍显示原封面/无词占位
- WebDAV 与本地链路均覆盖

## Constraints

- 仅改 `feature:scrape` 预览模型/视图与 `core:scrape` 写回装配，不改 `WritebackOrchestrator` 五步本体
- 歌词在线匹配复用现有 `core:lyrics` 能力（`LrclibProvider`/`Amll`），不新增网络依赖
- 封面保持远程 URL 直链展示，嵌入字节走已有 `HttpCoverBytesFetcher`

## Acceptance Criteria

- [ ] **AC1 封面**：刮削命中封面的歌曲写回成功后，不重启播放该曲，迷你条/列表/沉浸页封面立即可见为新封面
- [ ] **AC2 歌词**：刮削命中歌词时预览可见摘要，编辑后写回，播放页歌词面板显示新词
- [ ] **AC3 无命中不污染**：未命中封面/歌词的歌曲刮后原封面/歌词保持不变
- [ ] **AC4 编辑取消**：歌词编辑后取消不改行内拟写回值
