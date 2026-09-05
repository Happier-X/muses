# core-scrape KMP化-桌面刮削全功能

## Goal

把 `:core:scrape` 刮削引擎从安卓库改为多平台模块，打通桌面端刮削匹配与写回全功能（接续界面共用二期桌面刮削页预留的回调注入点）。

## Background（已实测）

- `:core:scrape` 主源码约 2800 行、25 文件，仅 `ScrapeModule.kt` 用 koin-android，其余为纯 JVM 代码；真正的壁垒是它依赖 4 个安卓模块：core:data（三仓库+AudioTagReader）、core:webdav（WebDavClient 接口）、core:media（TagWriter）、core:lyrics（匹配器与 provider）。
- P2b/P2c 已把地基搬进 `:core:common` commonMain：Room DAO/模型、ScrapeHttp、ScrapeQueueStore/HistoryStore、WritebackJson/RollbackJournalStore/FailureCopy、CoverTypes、TextMetaProvider、WebDavRateLimiter。
- `SongRepository/SourceRepository`（220 行）实现体依赖全为 commonMain 已有类（DAO/model/ErrorLogStore），随迁零安卓改动；`TagWriter/AudioTagReader` 核心是 jaudiotagger 纯 Java 库（桌面可用），仅 AudioTagReader 的 Context/Uri/OkHttp 装载壳为安卓专属。
- `editmeta/LyricsPorts.kt` 已有 Port 模式先例（LyricsSearchPort 包装 core:lyrics 实现）；桌面端歌词匹配维度建议先降级（Port 不注入），标题/专辑/封面写回全功能先行。
- 二期桌面刮削页（composeApp）已有队列管理真实可用 + 匹配/写回回调预留（`onMatchAll`）。
- 约束：全程安卓行为不变；不升级版本线；ErrorLogStore 埋点规则延续。

## Requirements

- R1：仓库地基 KMP 化——SongRepository/SourceRepository/CredentialsRepository 及 ErrorLogStore（如仍在 core:data）移入 `:core:common` commonMain，实现体原样平移。
- R2：WebDavClient 接口移入 commonMain（KtorWebDavClient 实现留 core:webdav；桌面复用或对齐实现）。
- R3：纯逻辑上收——text/cover provider 全家、SuspiciousDetector、NegativeCache、EditCloudMetaSearch 等迁入 commonMain（仅依赖 ScrapeHttp/serialization/coroutines 者全部随迁）。
- R4：写回链 KMP 化——WritebackOrchestrator/SongFileWriters 依赖 Port 化（仓库 Port + 标签 Port），jaudiotagger 实现进 jvmMain/安卓各自 sourceSet。
- R5：桌面装配——DesktopScrapeGraph 接通真实匹配/写回；歌词维度降级（不注入 LyricsSearchPort，UI 明示"桌面暂不支持"）；桌面刮削页去掉回调占位。
- R6：安卓零改动回归（feature:scrape + app 全量）。

## Acceptance Criteria

- [x] AC1：`:core:common` commonMain 零安卓 import（W1-W3 核验）；`:core:scrape` 缩为安卓装配壳（仅 ScrapeModule + LyricsPorts）。
- [x] AC2：桌面刮削页四态机接真实引擎（扫队列→匹配→预览→写回；写回按 sourceType 分流落库落盘；歌词维度 UI 明示降级）；真实 NAS 端到端实测留待人工验收。
- [x] AC3：安卓刮削行为不变（W1-W3 每步 feature:scrape 零业务改动 + 全模块单测全绿）。
- [x] AC4：全量回归通过（:core:common:assemble + allTests 128 用例 + :app:assembleMusesDebug + testDebugUnitTest + :composeApp:compileKotlinJvm）。

## Out of Scope

- core:lyrics 全模块 KMP 化（歌词匹配桌面端降级，留后续任务）。
- 不升级版本线；不做视觉改动。

## Key Decisions

- D1：分层 Port 倒置而非全链 KMP 化（core:lyrics/core:media 不动，控制爆炸半径）。
- D2：桌面歌词维度先降级（LyricsSearchPort 不注入），标题/专辑/封面先行。
- D3：jaudiotagger 双端共用（纯 JVM），AudioTagReader 安卓装载壳留 androidMain。

## Open Questions

- Q1：阶段顺序与范围确认——见最终评审。
