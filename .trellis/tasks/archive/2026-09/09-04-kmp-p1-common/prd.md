# KMP 迁移 P1：common 先行

## Goal

新建 KMP 模块（android + jvm 双 target），搬入严格平台无关的公共代码并定义 `PlayerPort` 接口；安卓侧切换依赖后行为零变化。证明“ KMP 插件链 + 双端编译 + 安卓消费 + CI”整条管线跑通，为 P2（数据层）铺路。

## Background（已实测）

- `core:model` 6 文件真纯 Kotlin（仅自引用 + lyrics/scrape 模型互引），直迁。
- lyrics 26 文件 / scrape 35 文件中，仅 **5 个源文件**满足 commonMain 准入（零 `java.*`、零 okhttp/androidx/Hilt/DataStore、不引用 `LyricsHttp`/`ScrapeHttp`/`core:data`，且无传递性平台污染，S3 逐文件审计结论，纠偏记录见本 PRD「偏离记录」）：
  - lyrics：`LyricsProviderUtil`、`QrcTables`（`internal` 拓宽为 `public`，值表零改动，供留守 `QrcDecoder` 跨模块访问）。
  - scrape：`CoverTypes`、`TextMetaProvider`、`FailureCopy`。
- 留守 10 个的硬证据：`AmllScore`（`java.text.Normalizer`）；`LrcLyricsParser`/`LyricTimelineProcessor`（经 `LyricModels` 传递引用 `BreakIterator`/`Locale`）；`LyricsMatcher`/`LyricsPorts`（经 `AmllTtmlDbClient`→`LyricsHttp` OkHttp 链）；`CoverProviderCommon`/`TextMetaConfidence`/`SuspiciousDetector`（经 `TextMetaUtil.normalizeText`→`Normalizer`）；`ScrapeRateLimiter`（实为 `core:webdav` 别名，搬迁将制造 common→webdav 循环依赖）；`NegativeCache`（`synchronized` 非 common 可用）。
- 排除项（证据确凿，留守 P2/P3）：全部在线 provider（依赖 `LyricsHttp`/`ScrapeHttp` OkHttp 包装器）；`QrcDecoder`/各解析器/`WyCrypto`（`java.*`/`javax.crypto`）；队列/历史/写回 store（DataStore / `core:data`）；`di/*`（Hilt）；`LyricBindingStore`（`Context` + `org.json`）。
- 随源码迁移的测试：`FailureCopyTest`（1 文件 6 用例，进 `commonTest`，JUnit4 import 转 `kotlin.test`）；其余测试随其源码留守。
- 工具链：Gradle 9.6.1 + AGP 9.3.2 + Kotlin 2.4.10，KMP 插件取同版本 `2.4.10`（gradlePluginPortal 已在 pluginManagement）。

## Requirements

- R1：新增 `:core:common` KMP 模块，targets = `android` + `jvm`（jvm  target 在 P1 即验证 commonMain 纯度，`jvmMain` 可为空）。
- R2：`commonMain` 内容 = model 6 文件 + 上述 5 文件（包名保持 `com.muses.player.core.*` 不变，消费者零 import 改动）+ 新建 `PlayerPort` 接口（play/pause/seek/enqueue/setMode + StateFlow，最小形状，见 `design.md`；另含复用旧模型的 `playerConfig`，见偏离 D3，已认领）。
- R3：删除 `:core:model` 模块（文件已搬迁），`settings.gradle.kts` 替换为 `:core:common`；所有引用方 build 脚本 repoint（约 12 处 `project(":core:model")` → KMP 产物）。
- R4：`commonTest` 迁移 6 个测试；安卓侧 `testDebugUnitTest` 全过 + `:app:assembleMusesDebug` 通过。
- R5：CI 加 jvm 编译任务（`compileKotlinJvm`），保证 commonMain 纯度门禁。

## Acceptance Criteria

- [ ] AC1：`:core:common` 的 `compileKotlinAndroid + compileKotlinJvm + commonTest` 全过（commonTest = `FailureCopyTest` 1 文件 6 用例，双端报告）。
- [ ] AC2：`:app:assembleMusesDebug` + 全仓 `testDebugUnitTest` 通过，行为零变化（无产品逻辑改动，纯搬迁）。
- [ ] AC3：`grep -r "core:model" settings.gradle.kts */build.gradle.kts` 零命中（旧模块无残留引用）。
- [ ] AC4：`org.jetbrains.kotlin.multiplatform` 插件版本与 Kotlin 对齐（2.4.10），无版本漂移。

## Out of Scope

- 不引入 Ktor / 不抽象 HttpPort（provider 留守，P2 再议）。
- 不做 Hilt→Koin、不碰 Room/DataStore/Media3。
- 不建 `composeApp(desktop)`，不做桌面 UI（P3）。
- 不升级 Kotlin/AGP/Gradle 版本。

## Key Decisions

- D1：模块命名 `:core:common`（沿用 core:* 惯例；包名不变，diff 半径最小）。
- D2：P1 只搬严格纯净子集（证据驱动：provider 全家依赖 OkHttp 包装器，硬搬会把 Ktor 选型提前拖入 P1，撑破 1–2 周预算）。
- D3：`:core:model` 删除而非保留垫片（避免双源头；repoint 纯机械，编译即验证）。

## Risks

- KMP 插件首次解析需拉取 `kotlin-gradle-plugin` 新工件，离线/代理环境可能失败（首步验证，失败即停）。
- `kotlin.test` 在 commonTest 的 JUnit4 跑通情况（迁移 6 个测试即验证）。
- `NegativeCache` 等 0/0/N 文件若隐含 `okio` 等 JVM 依赖，降级为留守（R2 复核点）。

## 偏离记录（S3/S4 审计结论，已执行）

- D1：AGP 9.3.2 下旧 `com.android.library` + KMP 组合不可用，改用官方新插件 `com.android.kotlin.multiplatform.library`（`android.kmp.library` 别名）+ `kotlin { android { } }` 收敛写法；`builtInKotlin` 旁路方案因破坏 `:core:data` 测试发现已废弃。零全局 flags。
- D2：白名单 15→5（证据见 Background 留守段）。S2 的“先不 repoint”不可行（Gradle 配置期校验），repoint 提前到 S2/S5 一并完成，共 11 处。
- D3：`PlayerPort.playbackState` 取 `StateFlow<Int>`（model 无 `PlaybackState` 类型，对齐 `PlayerConnection` Media3 `STATE_*` 现状，避免新模型）；附带 `setRepeatMode(RepeatMode)` 重载与 `playerConfig: StateFlow<PlayerConfig>`（均复用旧模型，无实现无消费，P2 承接）。
