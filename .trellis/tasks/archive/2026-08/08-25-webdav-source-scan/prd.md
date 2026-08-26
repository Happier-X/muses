# WebDAV 音源扫描（父任务 08-24-native-compose-rewrite 的子任务）

> 背景：音源页扫描功能（08-25-native-salt-ui P5 补充）已交付本地源扫描；WebDAV 源点扫描目前提示「暂未支持」。本任务补齐 WebDAV 音源的完整扫描入库能力。Web 层 `src/features/library/scanner.ts` + 旧 Capacitor `WebDavPlugin.readMetadata` 是功能规格书。

## Goal

在 native 端为 WEBDAV 类型音源提供与 Web 层同语义的扫描：递归 PROPFIND 发现音频文件 → 可选下载读标签（含封面、内嵌歌词、sidecar .lrc）→ 入库 SongRepository，进度实时展示在音源页已有的「扫描进度」弹窗。

## 需求

1. 音源卡片「扫描」对 WebDAV 源可用：打开既有「扫描设置」弹窗（读取音乐标签默认关——网络逐文件下载慢，对齐 Web 默认值）→ 开始扫描
2. 发现阶段：从 `Source.url + Source.path` 起 BFS 递归 PROPFIND（复用 `WebDavClient.list`），按扩展名过滤支持的音频格式
3. 处理阶段（readTags=true）：逐文件经 `WebDavAudioCache` 下载/命中缓存 → jaudiotagger 读标签/封面/歌词 → 同目录同名 `.lrc` sidecar 歌词兜底
4. readTags=false：只列目录取文件名建歌（标题=文件名去扩展名），零下载，快速建库
5. 入库后播放链路可用性不在本任务验收范围（播放接线是独立事项），但 `Song.path` 必须存完整 HTTP URL，为流播/缓存播放预留
6. 扫描完成的文件顺手 putToCache 预热播放 LRU 缓存（读标签已下载，一举两得）

## Acceptance Criteria

- [ ] 本地源扫描行为不回归（LocalLibraryScanner 不受影响）
- [ ] WebDAV 源扫描：readTags 开/关两条路径都能入库，歌曲页可见该源歌曲
- [ ] 密码缺失时明确报错「WebDAV 密码不存在，请重新添加该音源。」（对齐 Web 文案）
- [ ] 单个文件标签读取失败降级为文件名（degraded 不中断整体扫描）
- [ ] 进度弹窗阶段文案正确流转（正在查找文件→正在扫描入库→扫描完成/失败）
- [ ] `cd native && ./gradlew :feature:sources:assembleDebug :app:assembleDebug :core:media:testDebugUnitTest` 全绿

## Out of Scope

- WebDAV 歌曲的播放接线（PlaybackService 流播/缓存命中判断）
- 并行下载加速（先串行对齐 Web 行为）
- Web 端细粒度统计模型（inserted/updated/skipped 等）移植

## 关键决策

- **D1 扫描器放 core:media**：新增 `WebDavLibraryScanner` 与 `LocalLibraryScanner` 同层；core:media 增加对 core:webdav 的依赖（media=业务逻辑层，webdav=基础设施层，方向合法）
