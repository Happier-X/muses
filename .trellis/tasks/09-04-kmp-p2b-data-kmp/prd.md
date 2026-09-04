# KMP 迁移 P2b：数据层 KMP 化

## Goal

Room（entities/DAO/Migrations）与 DataStore（含各 store）搬入 `:core:common`（commonMain），安卓侧以平台 builder 接线，行为零变化。OKIO 收敛文件路径，为 P3 桌面复用数据层铺路。

## Background（已实测）

- Room：5 DAO + 8 entities + `MusesDatabase`（v6，schemas/v2–v6 JSON 齐全，MIGRATION_1_2…5_6 手写）；DAO 已是 suspend/Flow 口径，无 Cursor；仅 `Migrations.kt` 用 `SupportSQLiteDatabase`（androidx.sqlite，多平台 OK）。
- DataStore：单文件 `muses_settings.preferences_pb` + 单工厂（`DatabaseModule`）；key 集中（playback_snapshot/config、recent_plays、settings 3 key、credential.*、scrape 3 key + rollback）；`createWithPath`（1.1.0+，现 1.2.1）即 commonMain 可用。
- 官方 Room-KMP 路径已确认：`room-runtime` + `sqlite-bundled` 进 commonMain，entities/DAO 原样搬，平台侧 `Room.databaseBuilder` + `BundledSQLiteDriver`；KSP 用 `kspCommonMainMetadata` + room-gradle-plugin。
- 风险点：room-gradle-plugin × AGP9 新 `kmp.library` 插件的 interplay 未验证；Migration 在 commonMain 的编译；`room-testing` 系安卓独占（MigrationTest 须留守改写）。

## Requirements

- R1（S0 spike，先行门禁）：空 entities + 1 DAO 在 `:core:common` 跑通 `kspCommonMainMetadata` 编译 + jvm 侧 BundledSQLite 读写；任一失败即停并改方案（P2c 先行）。
- R2：entities/DAO/`MusesDatabase`/Migrations 搬入 commonMain；`exportSchema` 路径保留；schemas JSON 随模块迁移。
- R3：DataStore 工厂改 `createWithPath` + okio Path；路径供给 `expect/actual`（androidMain=`filesDir`，jvmMain 占位）；全部 store（data 4 + scrape 4）随迁（`CredentialsRepository` 除外：AndroidKeyStore 硬依赖，留守，接口不动）。
- R4：安卓侧接线：`databaseModule` 改平台 builder（`Room.databaseBuilder + BundledSQLiteDriver + addMigrations`，DB 名/ key 名冻结）；Koin 声明不变。
- R5：回归：`:app:assembleMusesDebug` + 全仓单测 + MuMu 实测（启动/扫库/播放恢复/设置读写）。

## Acceptance Criteria

- [x] AC1：spike 结论见 `spike.md`。
- [x] AC2：commonMain 零 `java.*`/`android.*`；`compileKotlinJvm` 通过。
- [x] AC3：DB 名/key/schema v6 冻结（6.json 逐字节一致）；MigrationTest 改 commonTest(jvm)3/3。
- [x] AC4：构建回归全过 + MuMu 冷启动/杀进程重启无崩溃；扫库与设置 UI 环节随用户使用验证。

## Out of Scope

- 不碰 provider/OkHttp（P2c）、不碰 Media3/Hilt（已完）、不建桌面 UI（P3）。
- 不升 Room/DataStore/Kotlin 版本（`sqlite-bundled` 选与 room 2.8.4 同期版本，见 design）。
- `CredentialsRepository` 的桌面替代（P3）。

## Key Decisions

- D1：spike 门禁前置（Room-KMP 插件链是全仓最不确定的构建风险）。
- D2：落点 `:core:common` 而非新建模块（复用 P1 管线；db 包名不变）。
- D3：key/文件名/schema 全冻结（双端行为一致是底线）。

## Risks

- room-gradle-plugin 与 `com.android.kotlin.multiplatform.library` 的兼容（S0 即验证）。
- `sqlite-bundled` 版本与 room 2.8.4 的搭配（官方文档以 2.8.3+sqlite 2.5.x alpha 为例，alpha 后缀是风险）。
- MigrationTest（room-testing）留守改写的工作量。
