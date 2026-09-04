# KMP迁移P3-桌面MVP

## Goal

交付 Windows 桌面最小可用版本：前台播放闭环（播放/暂停/切歌/进度/音量/歌词展示）+ 库房/播放/设置三屏最小可用 + 桌面打包分发链。托盘/SMTC/全局媒体键/音频焦点明确放二期。

## Background（已实测）

- 播放接缝已就绪：`core/common` commonMain 有 `PlayerPort` 接口（`PlayerPort.kt`，P1 只定接口、无实现；`playbackState` 取 `StateFlow<Int>` 对齐 Media3 整型现状）；安卓侧 `PlaybackController`（`core/media`）+ `PlayerConnection` StateFlow 链路不动。
- 公共底座已就绪：`core:common` 有 android + jvm 双 target（含 Room-KMP、DataStore 多平台、Ktor-CIO、okio）；P2b/P2c 完成后数据层与网络层均为 commonMain 可用，桌面侧可直接复用。
- 桌面侧现状为零：无 `composeApp(desktop)` 模块、无 Compose Multiplatform 插件、无 `desktopMain` sourceSet、无解码依赖（全仓 0 处 vlcj/javax.sound/javafx 引用）；`settings.gradle.kts` 仅含 app + core + feature 安卓模块。
- 有利条件：Coil3 已是 KMP（图片加载桌面可复用）；Koin 有 `koin-compose` / `koin-compose-viewmodel`（桌面 DI 可复用安卓侧模块）；图标已是 Tabler CMP 变体。
- 曲库含 FLAC（有过 FLAC 刮削崩溃修复记录），解码选型必须覆盖 FLAC/MP3/M4A/OGG/WAV 主流格式。
- 约束：P2c 网络层切换改动仍在同一工作目录热改中，P3 实现开工须等 P2c 停稳提交，避免同目录冲突。

## Requirements

- R1：桌面解码选型原型先行（VLCJ vs JavaFX Media vs javax.sound 系），输出书面结论后才定 `JvmPlayerPort` 实现路线。
- R2：`JvmPlayerPort` 前台播放（play/pause/seek/enqueue/setMode + 状态/错误 StateFlow），复用 commonMain 播放持久化与队列状态机语义（repeat/shuffle 与安卓一致）。
- R3：`composeApp(desktop)` 最小可用：库房/播放/设置三屏；歌词特效（Haze/Blur）桌面降级。
- R4：桌面打包/签名/分发链（msi/exe）+ 首版发布。
- R5：WebDAV 建库/扫库/流播在桌面侧可用（Ktor Range 下载 + okio 本地 spiller，不追求与 Media3 CacheDataSource 首版对等）。

## Acceptance Criteria

- [x] AC1：解码选型有书面原型结论（含格式覆盖、体积、license 三项）。
- [x] AC2：桌面端前台播放闭环可用（播放/暂停/切歌/进度/音量/歌词展示）。
- [x] AC3：库房/播放/设置三屏最小可用；全量回归通过（安卓包不受影响）。
- [x] AC4：msi/exe 打包分发链跑通并产出首版安装包。

## Out of Scope

- 托盘/SMTC/全局媒体键/音频焦点/后台播放（二期）。
- 不升级 Kotlin/AGP/Compose BOM 版本线。
- 不动安卓侧 Media3 播放链路。

## Key Decisions

- D1：解码选 VLCJ（用户已确认方向；S0 三项 gate 全过技术锁定；VLCJ 实测为 GPL 系，用户 2026-09-04 已接受 GPL 分发，回退线关闭）。

- D5：S0/S1 已提交落稳（e18b5f6f）；spike-vlcj 原型目录（约 282MB）不进仓库，已入 .gitignore，结论见 spike.md。

- D2：桌面三屏复用平板形态（viewport 宽 ≥768 断点：TabsLayout 双栏 + 曲库自适应网格 + 沉浸播放横屏布局），桌面窗口默认宽度 Physiology 落在平板断点之上；桌面新增系统标题栏（最小化/最大化/关闭）。

- D3：桌面标题栏用自绘（与深色主题统一外观；拖拽/缩放/贴边等窗口行为自行接，工作量高于原生栏）。

- D4：首版只出安装包（msi/exe），不提供绿色免安装包；签名与 SmartScreen 警告处理随打包链一并解决。

## Open Questions

（本轮已收敛，待最终规划评审一次确认。）
