# 纯原生 Kotlin + Jetpack Compose 重写 Android 端（父任务）

## Goal

将 muses 从 Capacitor 混合架构（Vue 3 WebView UI + Kotlin 原生插件）重写为**纯原生 Android 应用**：Kotlin + Jetpack Compose + 最新安卓最佳实践。采用分阶段里程碑交付，每个里程碑产出可安装 APK；全部功能对齐后删除 Web 层。

本任务为父任务：持有需求全集、里程碑路线图与跨子任务验收标准。各里程碑实施时以独立子任务承载。

## 已确认事实（代码库证据）

### 现有架构

- Web 层：Vue 3 SFC + TypeScript strict + Vite，46 个 `.vue` + 81 个 `.ts`，Konsta/Tailwind 已移除改自研 SCSS。
- 原生层已全 Kotlin（`android/app/src/main/java/com/muses/player/`，约 2800 行）：`MainActivity.kt`、`AudioPlaybackService.kt`、`PlaybackService.kt`、`AudioPlayerPlugin.kt`、`PlaybackQueue.kt`、`WebDavPlugin.kt`、`WebDavAudioCache.kt`、`LocalLibraryPlugin.kt`、`AudioMetadataReader/Writer.kt`（jaudiotagger）。
- 播放内核 Media3 ExoPlayer 1.5.0；通知由 `@capgo/capacitor-media-session` 承担（历史上因桥接限制放弃 media3 `MediaSessionService`，见 spec/frontend/features-player.md 通知契约——该约束源于 JS 桥接场景，纯原生下可重新评估）。
- WebDAV 客户端为 OkHttp 手写（PROPFIND/PUT/DELETE 等），含自定义磁盘缓存 `WebDavAudioCache`（约 460 行），纯原生重写可直接复用其设计。
- 构建：Groovy DSL build.gradle、compileSdk 37 / targetSdk 36 / minSdk 24。

### 功能面（复刻清单）

页面（14 个）：Tabs 主框架、Songs、Albums、Artists、LibraryDetail、Player、Playlists、PlaylistDetail、Queue、Scrape、Settings、Sources、SourceWebDav、SourceWebDavBrowse。

功能模块（src/features/*）：

- `player`：播放控制器、keepalive、响度均衡（loudness）、媒体会话
- `library`：本地扫描、音频标签读取、存储（localStorage 键 `muses:songs`，元数据版本 v4 含字段来源追踪）
- `sources`：WebDAV 音源管理、目录浏览会话
- `scrape`：批量刮削队列、匹配器、历史、失败重试、写回 WebDAV
- `cover` / `metadata` / `editMeta`：封面匹配 providers、元数据匹配 providers、云元数据编辑
- `lyrics`：歌词匹配、AMLL TTML DB、翻译合并、归一化、评分
- `playlist`：播放列表存储（`muses:playlists`）

存储现状：库/播放列表/音源存 localStorage（`muses:*` 键）；WebDAV 密码存 SecureStorage（前缀 `muses:webdav-password:`）。

关键视觉特性：AMLL 歌词渲染与背景生命周期治理（spec 有专门契约）、Salt Player 风格 UI 复刻（玻璃拟态、沉浸式播放页、平板双栏布局、侧边栏导航）。

## Requirements

### R1 技术基座（最新安卓最佳实践）

- Kotlin（官方最新稳定版）+ Jetpack Compose + Material 3 基础组件库
- 架构：MVVM（ViewModel + StateFlow）、Hilt 依赖注入、Coroutines/Flow、Room（库数据）、DataStore（设置）、Keystore/EncryptedFile（WebDAV 凭据）
- Gradle：Kotlin DSL + Version Catalog（libs.versions.toml）
- 导航：Compose Navigation（或官方 Navigation 3，design 阶段定版）
- 测试：JUnit + Turbine（Flow）+ Compose UI 测试；lint/type 检查纳入验证命令

### R2 Salt Player 风格视觉体系（D4）

在 Compose 中复刻现有 Salt 风格：玻璃拟态控件、侧边栏导航、沉浸式播放页（全屏封面、模糊背景）、平板双栏布局。不使用 M3 默认观感作为最终样式。

### R3 里程碑交付

- M1 核心播放 + WebDAV（必须可播 WebDAV，D2）：脚手架、本地库扫描、歌曲/专辑/艺术家列表、WebDAV 音源管理与流播（含缓存）、播放页基础 UI、队列、系统媒体通知 + MediaSession
- M2 歌词与播放列表：歌词展示（AMLL 方案落地）、播放列表管理、响度均衡
- M3 元数据与长尾：批量刮削 + 写回 WebDAV、云元数据编辑、封面匹配、设置页完善、平板双栏布局完善

### R4 双轨过渡

过渡期 Web/Vue 版保持可用并照常修 bug；新原生 App 使用独立 applicationId 与旧版并存安装；功能全部对齐后删除 Web 层与 Capacitor 依赖。

## Acceptance Criteria

父任务级验收（跨里程碑）：

- [ ] 三个里程碑各自产出可安装 APK 并通过该里程碑验收标准（标准在各子任务 PRD 中细化）
- [ ] M1 验收核心：全新安装后可配置 WebDAV 音源、浏览并流播放歌曲、通知栏媒体卡片可控（播放/暂停/上一首/下一首）、后台播放不被杀
- [ ] 功能对齐清单（14 页面 × 功能模块）逐项核对完成
- [ ] 对齐完成后删除 Web 层（src Vue 部分、Capacitor 配置、桥接插件），仓库仅剩纯原生工程
- [ ] 新增 `.trellis/spec/android/` 规范层，沉淀 Compose/Kotlin 编码规范与播放器契约

## 关键决策（已确认）

- **D1 迁移策略 = 分阶段里程碑重写**（2026-08-24）：逐阶段交付可用 APK；过渡期双轨维护；对齐后删 Web 层。
- **D2 M1 必须包含 WebDAV 播放**（2026-08-24）：第一版必须能通过 WebDAV 流播放。
- **D3 数据迁移 = 不迁移**（2026-08-24）：不做旧数据导入；首次启动重新配置音源、重新扫描、重建播放列表。
- **D4 UI 设计语言 = 继续 Salt Player 风格复刻**（2026-08-24）：在 Compose 中复刻玻璃拟态/Salt 控件/沉浸式播放页，不用 M3 默认观感作最终样式。

## 技术调研项（M2 前定案，不阻塞本 PRD）

- AMLL 歌词/背景渲染的 Compose 实现方案：**已定案（2026-08-24，M2）** = 内嵌官方 AMLL（Vite 打包 core 进 assets → WebView 桥接，DroidMate 同款），歌词与背景均在 WebView 内渲染；accompanist lyrics-ui spike 达标但弃用为 fallback；详见子任务 M2 design.md。
- 纯原生下媒体通知是否回归 media3 `MediaSessionService`（旧约束源于 JS 桥接缺陷，原生实现无此限制），M1 design 阶段定案。

## Out of Scope

- iOS / 桌面端 / Web PWA 目标
- 旧 localStorage 数据导入导出
- 除 Android 外的新平台特性（Wear OS、Auto 等）

## Notes

- 特大型任务：design.md + implement.md 聚焦 M1（首个实施单元）；M2/M3 在各自子任务中细化。
- 各里程碑开工时用 `task.py create "<标题>" --slug <名称> --parent .trellis/tasks/08-24-native-compose-rewrite` 创建子任务。
