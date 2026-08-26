# 设置页日志功能：查看与复制报错日志

## 目标 / 用户价值

用户遇到播放失败、扫描异常等问题时，能一键把报错日志复制到剪贴板反馈给开发者；开发者据此定位修复问题，用户无需连电脑抓 logcat。

## 背景（代码勘察确认）

- 设置页位于 `app/src/main/kotlin/com/muses/player/nativem1/settings/SettingsScreen.kt`，
  使用 `SaltListItem` / `SaltNavbar` 等 Salt 组件，已有「关于」「音频」两个分组
- 全工程无统一日志设施：仅 `core/media/.../NoOpPlaybackController.kt` 有 2 处占位 `Log.d`；
  多处 `catch (_: Exception)` 静默吞异常（如 `WebDavLibraryScanner.kt:56`、`ScanWorker.kt:58`、
  `PlayerConnection.kt:169`），问题发生时无迹可循
- 架构分层：`app → feature:* → core:*`；跨模块通信走接口 + Hilt `@Binds`
  （绑定集中在 `core/data/repository/Repositories.kt` 的 `RepositoryModule`）
- 所有需要埋点的 core 模块（media/webdav/scrape）均已依赖 `core:data`，
  日志设施放 `core:data` 无需新增 Gradle 模块
- `MusesApplication` 为 `@HiltAndroidApp`，是 crash handler 安装点

## 已确认决策（用户拍板）

- **采集范围**：分级采集，MVP 聚焦 warn / error 级别 + 未捕获异常（crash）；
  全量 info/debug 埋点不属于本任务
- **缓冲策略**：内存环形缓冲（最近约 500 条，线程安全）；crash 持久化到文件以便下次启动读取
- **呈现方式**：设置页新增「反馈」分组 + 纯「复制报错日志」条目；
  条目副标题显示最近一次错误摘要；不做内嵌日志查看页（后续可增量添加）

## 需求

1. **R1 日志基础设施**（core:data）：`ErrorLogStore` 接口 + 实现——
   记录 warn/error 到环形缓冲；捕获未捕获异常并将崩溃现场持久化；
   应用启动时读回上次 crash 并入缓冲
2. **R2 关键点位接入**：播放错误回调（`PlaybackService.onPlayerError`）、
   扫描失败（`ScanWorker` / `WebDavLibraryScanner`）等现有静默 catch 处补 error 日志
3. **R3 设置页入口**：「反馈」分组 +「复制报错日志」条目：
   - 副标题：最近错误摘要（无错误显示「暂无报错记录」）
   - 点击复制格式化日志全文到剪贴板 + Toast 确认
   - 无日志时点击给出「暂无可复制的日志」提示

## 验收标准

- [ ] AC1 触发一条 error 日志后，「复制报错日志」可将其连同时间戳/级别/标签复制到剪贴板
- [ ] AC2 人为制造未捕获异常，重启应用后该崩溃信息出现在可复制日志中
- [ ] AC3 设置页副标题正确反映「暂无报错记录」/ 最近错误摘要两种状态
- [ ] AC4 播放失败（onPlayerError）与扫描异常会在日志中留下 error 记录
- [ ] AC5 超过 500 条时最早的日志被丢弃，不 OOM
- [ ] AC6 `gradle :app:assembleDebug`（或项目标准构建命令）通过，现有测试不回归

## 范围外（Out of Scope）

- 全量 info/debug 运行日志埋点
- 内嵌日志查看页 / 日志筛选 UI
- 导出日志文件 / 分享 Intent
- 上传日志到远端
