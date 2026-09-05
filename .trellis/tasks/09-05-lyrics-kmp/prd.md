# core-lyrics KMP化-桌面歌词全功能

## Goal

把 `:core:lyrics` 歌词模块搬入 `:core:common`（jvmShared 承接 JVM API），打通桌面刮削歌词维度（二期 D2 降级项）与桌面在线歌词，完成核心业务多平台化收官。

## Background（已实测）

- 模块体量 3624 行、约 30 文件，依赖极简：仅 core:common + serialization + coroutines + koin-core（无 core:data/media/webdav 依赖，无 koin-android）。
- 安卓专属 import 仅 `store/LyricBindingStore.kt`（54 行）：`android.content.Context` + `org.json.JSONObject`——需参数化路径注入 + JSON 换 kotlinx.serialization。
- 其余 9 个文件的 `java.*`/`javax.*`（javax.crypto 加密、javax.xml 解析、java.text/util）均为 JVM API；双 target（android+jvm）都是 JVM 系，经 W3 已建的 `jvmShared` 中间源集可零改动承接。
- P2c 已把 LyricsHttp/LyricsProviderUtil/QrcTables 放入 core:common commonMain 同包名；jvmShared dependsOn commonMain，同包名解析无冲突。
- 消费者：feature:player（安卓播放页/VM）、core:scrape 瘦壳（LyricsPorts）、app DI；同包名 + api 透传可零改动。
- 二期桌面刮削页歌词维度已预留注入点（`lyricsPorts = emptyList()`），本任务接通。
- 约束：全程安卓行为不变；不升级版本线；歌词解析/匹配/加密语义冻结（QRC 解密、AMLL 匹配、五源链序）。

## Requirements

- R1：LyricBindingStore 去安卓化（Context→路径参数注入，org.json→kotlinx.serialization），行为冻结。
- R2：core:lyrics 主源码全量迁 core:common jvmShared（同包名 `com.muses.player.core.lyrics.*`），测试随迁 commonTest/jvmTest。
- R3：core:lyrics 瘦壳清空（DI 模块视依赖上收或留守），消费者零改动。
- R4：桌面刮削歌词维度接通——DesktopScrapeGraph 注入 AmllLyricsPort/ProviderLyricsPort（AmllTtmlDbClient/LrclibProvider/PlatformChain 均为纯 JVM 逻辑），刮削页歌词降级文案移除。
- R5：桌面在线歌词可用（播放页歌词数据链：桌面 JvmPlayerPort 消费侧，最小可用为歌词匹配查询跑通；歌词 UI 特效不在本任务）。
- R6：安卓零改动回归。

## Acceptance Criteria

- [ ] AC1：`:core:common` commonMain/jvmShared 零安卓 import；`:core:lyrics` 瘦壳或清空。
- [ ] AC2：桌面刮削页歌词维度可用（刮削预览出现歌词字段，写回歌词落文件）。
- [ ] AC3：安卓歌词行为不变（全模块单测 + 播放页回归）。
- [ ] AC4：全量回归通过（三端编译 + 单测）。

## Out of Scope

- 桌面歌词 UI 特效（逐词渐变/Blur，二期桌面播放页范围）。
- 不升级版本线。

## Key Decisions

- D1：全量放 jvmShared 而非 commonMain（9 个文件的 JVM API 零改动承接；commonMain 化需逐文件平台化，成本高收益低——双 target 本就都是 JVM）。
- D2：LyricBindingStore JSON 换 kotlinx.serialization（org.json 桌面不可用；kotlinx 已在 commonMain）。

## Open Questions

- Q1：LyricBindingStore 的 Context 具体用途（是否涉及安卓目录）——实现期确认，路径注入即可。
