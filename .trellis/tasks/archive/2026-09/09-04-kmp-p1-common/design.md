# P1 技术设计

## 1. 模块结构

```
:core:common                     ← 新建（KMP，android + jvm）
  src/commonMain/kotlin/com/muses/player/core/
    model/…                      ← 原 :core:model 6 文件（包名不变）
    lyrics/{amll/AmllScore, LyricsMatcher, parser/LrcLyricsParser,
            processor/LyricTimelineProcessor, provider/LyricsProviderUtil,
            provider/qrc/QrcTables}.kt
    scrape/{cover/CoverTypes, cover/provider/CoverProviderCommon,
            editmeta/LyricsPorts, http/ScrapeRateLimiter,
            queue/SuspiciousDetector, text/TextMetaConfidence,
            text/TextMetaProvider, text/NegativeCache(复核),
            writeback/FailureCopy}.kt
    playback/PlayerPort.kt        ← 新建（§3）
  src/commonTest/kotlin/…         ← 6 个迁移测试
  src/androidMain/kotlin/         ← 空（占位，供 P2 actual 用）
  src/jvmMain/kotlin/             ← 空
:core:model                      ← 删除
```

目录归属注意：`ScrapeRateLimiter` 虽在 `http/` 包但 0/0/0 纯净（纯协程限流器），可搬；`LyricsHttp`/`ScrapeHttp` 本体留守。

## 2. 构建配置要点

- version catalog 加插件：`kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }`（2.4.10，与现有 `kotlin-jvm` 同版本线）。
- 模块 `build.gradle.kts`：`kotlin("multiplatform")` + `androidLibrary`（复用 `libs.plugins.android.library` 惯例）+ targets `androidTarget()` / `jvm()`；`commonMain.dependencies` 仅 `kotlinx-coroutines-core` + `kotlinx-serialization-json`（随用随加，禁止超前）；`commonTest` 加 `kotlin("test")`。
- `settings.gradle.kts`：`include(":core:model")` → `include(":core:common")`。
- 消费者 repoint：全仓 `project(":core:model")`（`api`/`implementation` 约 12 处）→ `project(":core:common")`。包名不变故 Kotlin import 零改动。
- minSdk/compileSdk/namespace 沿用 core 系惯例（namespace `com.muses.player.core.common`，compileSdk 37）。

## 3. PlayerPort 最小形状（commonMain 新建，仅接口，无实现）

```kotlin
interface PlayerPort {
    val playbackState: StateFlow<PlaybackState>   // 复用 core:model playback 模型
    val playbackError: StateFlow<String?>
    fun play(); fun pause(); fun seekTo(ms: Long)
    fun enqueue(ids: List<String>, index: Int)
    fun setRepeatMode(mode: Int); fun setShuffleEnabled(e: Boolean)
}
```

- P1 只定接口 + `androidMain` 留空（现有 `PlaybackController`/`PlayerConnection` 不动，P2 再做适配实现）。接口签名以 `core:model/playback/PlaybackModels.kt` 现有模型为输入复核，禁止新模型。
- 二期预留（注释 TODO，不实现）：托盘/SMTC/音频焦点（D2 桌面 MVP 决策）。

## 4. 验证矩阵

| 门禁 | 命令 | 期望 |
|---|---|---|
| KMP 双 target 编译 | `:core:common:assemble`（含 jvm jar + android aar） | 通过 |
| 纯度门禁 | `:core:common:compileKotlinJvm`（CI 新增） | 通过（commonMain 混入平台 API 即红） |
| 迁移测试 | `:core:common:allTests` 或 jvmTest/androidUnitTest | 6 个全过 |
| 安卓回归 | `:app:assembleMusesDebug` + 全仓 `testDebugUnitTest` | 通过 |
| 旧模块清零 | `grep -r "core:model" --include="*.kts" .` | 零命中（排除 build/ 与归档任务文档） |

## 5. 回滚

- 单提交原则：搬迁 + repoint + 验证放同一提交，失败即 `git revert`。
- 文件级回滚点：`settings.gradle.kts`（模块注册）、`gradle/libs.versions.toml`（插件条目）、`:core:common/build.gradle.kts`（新建，出问题整模块删）。
