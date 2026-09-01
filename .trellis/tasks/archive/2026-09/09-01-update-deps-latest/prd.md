# 更新所有依赖到最新版本

## Goal

将 `gradle/libs.versions.toml` 中所有依赖（AGP、Kotlin、KSP、Hilt、Compose BOM、Media3、Room 等）更新至 2026-09-01 时点的最新稳定版，并验证 `app:assembleMusesDebug` 与 MuMu 真机运行通过。

## Requirements

- 扫描 `gradle/libs.versions.toml` 全部 `versions` 与 `libraries`，查询 Maven Central 最新稳定版（非 alpha/RC 除非当前已为 alpha 如 Haze）
- 保持 `Kotlin` 与 `KSP`、`Compose BOM` 与 `AGP` 的兼容矩阵（Kotlin 2.4.x ↔ KSP 2.3.y，Compose BOM 2026.08 ↔ AGP 9.3.x 等）
- `Haze` 保持 `2.0.0-alpha03`（当前已验证）或评估升级至 `2.0.0-alpha05` 的 API 兼容性（`hazeSource`/`hazeEffect`）
- 更新后执行 `./gradlew :app:assembleMusesDebug` 与 `core/ui`、`feature:*` 的 `compileDebugKotlin` 均通过，无 `Unresolved reference` 与 `API` 破坏
- 保留 `minSdk 29` / `compileSdk 37` / `Java 17` 基线，必要时同步提升 `compileSdk` 以匹配新 AGP

## Acceptance Criteria

- [ ] `gradle/libs.versions.toml` 所有 `versions` 均对齐最新稳定版（以 `mvnrepository`/`maven central` 为准，记录于 `design.md` 版本映射表）
- [ ] `./gradlew :app:assembleMusesDebug` `BUILD SUCCESSFUL`（`283+` tasks）
- [ ] `adb install -r` 至 `MuMu 7555` 并 `am start` 成功，首屏列表与 `MiniPlayer`/`SaltNavbar` 真磨砂无回归
- [ ] 无 `KSP`/`Hilt` 生成代码破坏，`Room`/`DataStore`/`WorkManager` 运行时无 `ClassNotFound`

## Constraints

- 批量升级一次性提交，便于回滚（单 commit）
- 若某依赖最新版存在破坏性变更（如 `Room 2.8→2.9` 需 `KSP` 同步），则在 `design.md` 中记录 `hold` 版本并说明

## Notes

- 本任务为全量依赖升级，非功能迭代；不涉及 UI/业务逻辑变更
- 使用 `web_search` 与 `gradle/libs.versions.toml` 现状对比确定最新版
