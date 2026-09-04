# KMP 迁移 P2a：Hilt 切 Koin（安卓侧先行）

## Goal

安卓侧 Hilt 全量替换为 Koin，业务逻辑零改动，全量回归通过。使 DI 层具备 KMP 复用能力（Koin 同时支持 Android/JVM target），为 P2b（Room/DataStore KMP）与 P3（桌面壳）铺路。

## Background（已实测）

- Hilt  surface 共 **51 文件**：`@HiltAndroidApp` 1（`MusesApplication`）、`@AndroidEntryPoint` 2（`MainActivity`、`PlaybackService`）、`@HiltViewModel` 21、`@Module` 7（data 3 + lyrics/media/scrape/webdav 各 1）、`@Binds` 13、`@Provides` 26、`@Inject` 53、`hiltViewModel()` 调用 25 处（含 `MusesApp.kt` 全限定名调用）。
- 特殊 case：`ScanWorker` 用 `EntryPointAccessors` 手动取依赖（非 `@HiltWorker`）；`MusesApplication` 注入 `HiltWorkerFactory` 配 WorkManager；`RingBufferErrorLogStore` 同一实现双接口绑定（`ErrorLogStore` + `ErrorLogCrashPersistence`）。
- 利好：**测试零 Hilt 依赖**（`src/test` 无 hilt 引用），无需迁移测试 DI 基础设施。
- Koin 选型：`4.2.0` BOM（`koin-core/android/compose/compose-viewmodel/androidx-compose-navigation`），纯 Kotlin DSL + 无代码生成 + 已支持 CMP（P3 直接复用）。

## Requirements

- R1：删 Hilt 全家（`hilt-android`、`hilt-compiler`、`hilt-navigation-compose`、`hilt-work`、`androidx.hilt.compiler` 依赖 + `hilt` gradle 插件 + ksp 中 hilt 相关），`MusesApplication` 改 `startKoin`，`MainActivity`/`PlaybackService` 去注解。
- R2：7 个 Module 转 Koin DSL：`@Provides`→`single/factory`，`@Binds`→`singleOf/bind`（双绑定用多 bind 块，见 `design.md`），`@HiltViewModel`→`viewModel { }` 声明，25 处 `hiltViewModel()`→`koinViewModel()`。
- R3：`ScanWorker` 改 `KoinComponent` 懒注入，删 `HiltWorkerFactory` 接线（`MusesApplication.Configuration.Provider` 同步简化）；行为不变（同 trigger 同约束）。
- R4：全量回归：`:app:assembleMusesDebug` + `:app:lintMusesDebug` + `testDebugUnitTest` 通过；启动/播放/扫库/刮削主链路冒烟（安装跑一次）。

## Acceptance Criteria

- [x] AC1：零命中（主会话复核通过）。
- [x] AC2：自动化回归全过（主会话实跑 BUILD SUCCESSFUL）；APK 安装+主链路冒烟待用户在真机/MuMu 执行。
- [x] AC3：BOM 统一 4.2.0（复核通过）。
- [x] AC4：无双轨（复核通过）。

## Out of Scope

- 不改任何业务逻辑（仅 DI 接线）。
- 不碰 Room/DataStore/Media3/OkHttp（P2b/P2c）。
- 不建桌面 target、不升 Kotlin/AGP 版本。

## Key Decisions

- D1：Koin 4.2.0 BOM（CMP 就绪，P3 复用；`koin-compose-viewmodel` + `androidx-compose-navigation` 覆盖现有导航用法）。
- D2：安卓侧先行、单提交切换（风险隔离；编译即验证，双轨期为零）。
- D3：Worker 不用 koin-workmanager 插件，用 `KoinComponent` 懒注入（依赖最少，行为最接近现状）。

## Risks

- Koin 4.2 构建于 Kotlin 2.3.x，跑在 Kotlin 2.4.10 下的兼容性（首步空壳验证，失败即停并重估版本）。
- `koin-androidx-compose-navigation` 与 navigation-compose 2.9.8 的配合（MusesApp 导航即验证）。
- ViewModel 作用域语义差异（Hilt VM-scope vs Koin scope，21 个 VM 逐个冒烟）。
