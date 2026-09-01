# 设计：全量依赖升级

## 现状快照（2025-08-29）

- `agp 9.3.1` / `kotlin 2.4.10` / `ksp 2.3.11` / `hilt 2.60.1`
- `composeBom 2026.08.00` / `activityCompose 1.13.0` / `navigationCompose 2.9.8` / `lifecycle 2.11.0`
- `media3 1.11.0` / `room 2.8.4` / `coil 3.5.0` / `okhttp 5.5.0` / `workManager 2.10.0`
- `haze 2.0.0-alpha03`（已验证 `hazeSource`/`hazeEffect` API）

## 目标版本策略

- 以 `mvnrepository` 最新稳定版为准，`Kotlin` 与 `KSP` 严格对齐（`KSP` 主版本与 `Kotlin` 主版本一致，如 `Kotlin 2.4.x → KSP 2.4.y`，当前 `KSP 2.3.11` 对应 `Kotlin 2.3.x`，需评估是否可同步升至 `KSP 2.4`）
- `Compose BOM` 与 `AGP` 同步：`BOM 2026.08` 已为最新（`2026.08.00`），`AGP 9.3.1` 为 `Gradle 9.6` 对应最新，无需升
- `Hilt` 与 `KSP` 解耦：`hilt 2.60.1` 已为近期最新，`hilt-navigation-compose 1.4.0` 同步
- `Room` / `Media3` / `Lifecycle` 等 `androidx` 优先取 `2026 Q1` 稳定版，需验证 `Room 2.8.4 → 2.9.x` 是否引入 `KSP2` 变更
- `Haze` 保持 `alpha03`，`alpha05` 的 `HazeInput` API 破坏（`hazeEffect` 签名由 `HazeState` 改为 `HazeInput`），不升

## 变更面

- 仅 `gradle/libs.versions.toml` 的 `versions` 段，`libraries` 段无需结构变更
- 影响 `compileSdk 37` / `targetSdk 36`：若 `AGP` 升至 `9.4+` 需同步 `compileSdk 38`，本次保持 `37`
- 风险点：`Kotlin 2.4.10 → 2.5.x` 会带动 `KSP` 大版本，需全量 `ksp` 重新生成；`Media3 1.11.0 → 1.12.x` 的 `MediaItem` API 变更需回归 `PlayerConnection`

## 验证

- `./gradlew :core:ui:compileDebugKotlin`、` :feature:*:compileDebugKotlin`、` :app:assembleMusesDebug` 均 `SUCCESS`
- `adb install` 至 `127.0.0.1:7555` 冒烟
