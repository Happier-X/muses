# KMP 迁移评估与规划

## Goal

评估当前原生安卓工程（`muses-native`）迁移到 Kotlin Multiplatform + Compose Multiplatform、以同时支持 Android 与 Windows 桌面的可行性，输出分层迁移方案、风险清单与工作量估计，为后续是否立项迁移提供决策依据。同时清理已确认的 WebView 构建残留与死代码（P0 阶段，主任务内直接执行）。

## Background

- 工程结构：`app + 6 core（model/ui/data/webdav/media/scrape/lyrics）+ 5 feature`，`src/main` 共 166 个 Kotlin 源文件。
- 技术栈（版本冻结）：Kotlin 2.4.10 / AGP 9.3.2 / Compose BOM 2026.08 / Media3 1.11 / Room 2.8.4 / DataStore / Hilt 2.60 / OkHttp 5.5 / Coil 3.5 / Haze。
- `core:model` 纯 JVM 零安卓依赖；`core:lyrics` / `core:scrape` 守住“禁 Compose/Room/Media3”分层；图标已用 `tabler-*-cmp-android`（CMP 变体）。
- 播放接缝天然存在：UI/ViewModel 只经 `PlaybackController` 接口 + `PlayerConnection` StateFlow 驱动 `PlaybackService`（spec 播放契约），是未来 `PlayerPort` 抽象的落点。
- WebView 已彻底下线：全仓 0 处 `android.webkit` / `androidx.webkit` / `WebViewAssetLoader` 引用，无 Manifest 条目，无 assets HTML（`FullPlayerWebView`/`LyricWebView`/`assets/amll` 已删，Git 可回溯）。

## Requirements

- R1：各模块 KMP 可迁移性分级与方案 → 见 `design.md` §1（S/A/B/C 四档）。
- R2：硬骨头定级 → Media3 桌面重写（C 级，最贵）、Hilt→Koin 约 43 文件、WorkManager/MediaStore/Keystore 桌面替代 → 见 `design.md` §2–§4。
- R3：渐进式路线 P0–P3 + 备选对比（全量 CMP vs 桌面精简 JVM 客户端）→ 见 `design.md` §5–§6、`implement.md`。
- R4：WebView 残留清理（P0，主任务内直接执行）：
  - 删：`feature/player/build.gradle.kts:61` 的 `implementation(libs.androidx.webkit)`（唯一真残留，会打进 APK）+ `:60` 注释；`gradle/libs.versions.toml:46` 的 `androidxWebkit` 版本行与 `androidx-webkit` 库条目；`PlayerScreen.kt:215/249`、`PlayerViewModel.kt:98/139/238` 的 WebView 历史注释；`PlayerViewModel` 的 `_lyricsJson` / `lyricsJson` 死代码及 `refreshTranslationState` 内赋值分支。
  - 留：各 Provider 的 `AppleWebKit` UA 头（HTTP 伪装）；`SaltIconButton` / `SaltNavbar` 设计备注注释。

## Acceptance Criteria

- [x] AC1：`design.md` 覆盖模块分级表、播放器 `expect/actual` 抽象草图、DI 替换策略、数据层方案。（已写）
- [x] AC2：`implement.md` 给出有序阶段清单、每阶段验证命令、风险文件与回滚点。（已写）
- [x] AC3：R4 逐项有删/留结论（含文件行号）。（本 PRD R4 已给）
- [x] AC4：用户显式批准最终规划摘要后，才允许 `task.py start`；P1–P3 需另行立项。（2026-09-04 已批准并执行 P0）

## Out of Scope

- 除 R4 外不动产品代码；P1–P3 不在本任务执行。
- 桌面 MVP 不含后台播放/托盘/SMTC/全局媒体键（二期）。
- 不升级任何依赖版本。

## Key Decisions

- D1（2026-09-04）：scope=B，评估 + 顺手清理 WebView 残留，主任务内直接执行（已批准，不拆子任务）。
- D2（2026-09-04，采纳推荐）：桌面 MVP 只做前台播放（播放/暂停/切歌/进度/音量/歌词展示），托盘/SMTC/全局媒体键放二期。
- D3：渐进式四阶段，禁止一次性翻转；DI 先在安卓侧 Hilt→Koin 再建 KMP 模块。

## Risks / Deferred

- 桌面解码选型（VLCJ vs javax.sound）需原型验证，结论前 P3 不开工。
- Room-KMP migration 测试链、`xmlutil` 大 PROPFIND 性能、桌面打包分发链均为未验证项，已记入 `design.md` §7。
