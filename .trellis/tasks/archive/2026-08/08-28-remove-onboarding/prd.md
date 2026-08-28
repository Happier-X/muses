# 移除引导页残留

## Goal

彻底移除首次启动引导（Onboarding）残留代码与注释，使代码库不再包含无用引导概念，启动路径统一走主框架与音源页。

## 背景

- 引导页 UI 已于 2026-08-26 决策移除（`MusesApp.kt` 注释标记）。
- 残留：`SettingsRepository.isFirstLaunch / completeFirstLaunch / FIRST_LAUNCH_DONE` 接口、实现、DataStore key、相关单元测试及注释引用。
- 需做一次完整清理，避免后续误用与理解成本。

## Requirements

### 功能

- 删除 `SettingsRepository` 中的 `isFirstLaunch: Flow<Boolean>` 与 `suspend fun completeFirstLaunch()`。
- 删除 `DataStoreSettingsRepository` 中的对应实现与 `FIRST_LAUNCH_DONE` key。
- 移除 `MusesApp.kt` 中「首次启动引导已移除」过渡注释。
- 修正注释中指向引导页的表述：
  - `app/src/main/AndroidManifest.xml` 权限注释
  - `core/media/scanner/LocalLibraryScanner.kt` 注释
  - `core/media/scanner/ScanWorker.kt` 注释
  - `core/data/repository/SettingsRepository.kt` 接口注释
- 移除 `core/data/src/test/DataStoreSettingsRepositoryTest` 中对 `isFirstLaunch / completeFirstLaunch` 的断言用例，仅保留 `lastScanTimestamp` 等有效用例（重命名默认值用例）。
- 同步修正 `.trellis/spec` 中涉及引导页的描述：
  - `android/index.md` 中 `app (UI 宿主、导航、引导)` 描述
  - `android/features-webdav-library.md` 中「引导页保存」表述
  - 其他 spec 中若有首次引导相关描述，改为「启动后」或移除

### 非功能

- 不改变现有启动、权限申请、扫描、DataStore 其他键行为。
- 不引入新依赖。

## 约束

- 兼容已安装用户：残留的 `first_launch_done` 键无需迁移，直接废弃（DataStore 中残留键无害）。
- 单次提交完成，避免跨任务遗留。

## Acceptance Criteria

- [ ] `grep -rn "isFirstLaunch\|completeFirstLaunch\|FIRST_LAUNCH_DONE"` 在 `app/core/feature` 源码中零命中（仅 changelog/归档任务除外）
- [ ] `grep -rn "引导页"` 在 `app/core/feature` 源码中零命中；`AndroidManifest.xml`、`LocalLibraryScanner.kt`、`ScanWorker.kt` 注释已改为指向设置页/音源页/权限请求处
- [ ] `SettingsRepository` 接口仅保留 `lastScanTimestamp / loudnessEnabled / autoScrapeEnabled` 及其 setter
- [ ] `DataStoreSettingsRepositoryTest` 通过，测试名与断言不再提及「首次启动/引导」
- [ ] `./gradlew :core:data:test --tests "*DataStoreSettingsRepositoryTest*"` 通过
- [ ] `./gradlew :app:assembleDebug` 可构建（或 at least `:core:data:assemble` 不报错）
- [ ] spec 中 `app (UI 宿主、导航、引导)` 等引导表述已修正且无歧义

## Out of Scope

- 权限申请逻辑改动
- DataStore 键清理迁移
- 引导页 UI 重新设计

## Notes

- 轻量任务：PRD-only 即可，无需 design.md / implement.md。
