# 技术设计 — 纯原生重写（聚焦 M1）

> 范围：全局架构决策 + M1（核心播放 + WebDAV）详细设计。M2/M3 设计在各子任务中补充。

## 1. 工程结构

```
android/                     # 现有 Capacitor 工程保留不动（过渡期双轨）
native/                      # 新纯原生工程根（Gradle KTS + Version Catalog）
  settings.gradle.kts
  gradle/libs.versions.toml
  app/                       # 主应用模块
  core:model/                # 领域模型（纯 Kotlin）
  core:data/                 # Repository 实现（Room/DataStore/网络）
  core:webdav/               # WebDAV 客户端（OkHttp 手写，移植自 WebDavPlugin/WebDavAudioCache）
  core:media/                # 播放服务、队列、MediaSession、扫描器、元数据（jaudiotagger）
  feature:library/           # 歌曲/专辑/艺术家列表 UI
  feature:player/            # 播放页 + MiniPlayer + 队列 UI
  feature:sources/           # 音源管理 + WebDAV 浏览 UI
```

- 多模块以 API/实现边界强制分层：`feature:*` 不直接依赖 Room/OkHttp，只依赖 `core:*` 的接口与 `core:model`。
- 新工程 applicationId 过渡期用 `com.muses.player.native`（R4 双轨并存；对齐后切回 `com.muses.player` 发布）。

## 2. 技术栈定版（M1）

| 项 | 选型 |
|---|---|
| Kotlin / Compose / AGP | 最新稳定版，Version Catalog 统一管理 |
| DI | Hilt |
| 异步 | Coroutines + Flow；UI 层 StateFlow |
| 持久化 | Room（歌曲库、专辑/艺术家索引）；DataStore Preferences（设置）；EncryptedSharedPreferences 或 Keystore 包裹（WebDAV 凭据） |
| 网络 | OkHttp 5（沿用现有版本线） |
| 播放 | Media3 ExoPlayer 最新稳定版（≥1.5） |
| 图片 | Coil 3（封面加载，含 file/content/http 来源） |
| 导航 | androidx.navigation:navigation-compose（Navigation 3 尚未稳定，不冒险） |

## 3. 核心数据流

```
Scanner(库扫描) ─┐
WebDavClient ────┼─→ Repository(Room) ←─ ViewModel(StateFlow) ←─ Compose Screen
DataStore ───────┘
                    ↑
PlaybackController ⇄ MediaPlaybackService(ExoPlayer + MediaSession)
```

### 3.1 库扫描

- 复用 `LocalLibraryPlugin` 的扫描思路但原生化：MediaStore + SAF 起点扫描 → jaudiotagger 读标签（复用 `AudioMetadataReader` 逻辑，去 Capacitor 化）。
- 扫描为 WorkManager 后台任务 + 前台手动触发；进度经 Flow 暴露给 UI。
- 歌曲实体带稳定 ID（路径哈希），供播放列表（M2）引用。

### 3.2 WebDAV

- 移植 `WebDavPlugin.kt`（PROPFIND/GET/PUT/MKCOL/DELETE，OkHttp Basic Auth）到 `core:webdav`，接口化：`list/put/delete/move/get`。
- 缓存：移植 `WebDavAudioCache` 设计——远端文件按 ETag/Last-Modified 落盘缓存，播放走本地缓存文件，上限 LRU 清理。缓存目录 `context.cacheDir/webdav-cache`。
- 密码存 Keystore 加密；内存中仅解密后的短生命周期副本。

### 3.3 播放与媒体会话

- **回归 media3 `MediaSessionService`**（关键决策）：旧的「不用 MediaSessionService」约束源于 capgo JS 桥接下通知条件无法满足（spec/frontend/features-player.md），纯原生无此限制，官方组件是最佳实践且自动处理通知/媒体按钮/蓝牙耳机断连暂停等场景。
- `PlaybackService : MediaSessionService`：持有 ExoPlayer，队列操作（playNext/playPrevious/seek/shuffle）经 `MediaController` 由 UI 调用；`core:media` 提供 `PlayerConnection` 封装，把 Player/Position/Queue 以 Flow 暴露给 ViewModel。
- WebDAV 曲目以 `ResolvingDataSource`/缓存文件 URI 注入 ExoPlayer；音频焦点交给 ExoPlayer 默认处理。
- 通知 artwork 用 Coil 同步取 bitmap，不再需要 data: URL 变通。

## 4. Salt 风格视觉体系（Compose 落地）

- 自定义主题层：`SaltTheme { }` 提供 colors（玻璃透明度梯度、表面层级）、shapes、typography；不依赖 MaterialTheme 默认配色做最终观感，但仍挂在其上以继承无障碍行为。
- 玻璃拟态：`Modifier.blur()` + 半透明 surface 组合封装成 `GlassSurface` 组件；模糊背景用封面 bitmap 的 RenderEffect blur（性能预算：播放页独占，列表页禁用）。
- 侧边栏导航（手机端抽屉 + 平板常驻栏）自绘，参照现版 sidebar 行为。
- 沉浸式播放页：全屏封面 + enableEdgeToEdge + 手势返回；平板双栏在 M3 完善，M1 先保证手机形态。
- AMLL 背景/歌词渲染延后至 M2（见 PRD 调研项）。

## 5. 兼容与迁移

- 不迁移旧数据（D3）。首次启动引导：添加音源（本地目录 / WebDAV）→ 扫描。
- 过渡期旧工程 `android/` 保持可构建发布；新工程独立于 CI/构建脚本之外并行存在。
- 回滚：新 App 独立 applicationId，任何阶段可直接卸载回退，不影响旧版数据。

## 6. 主要风险

| 风险 | 缓解 |
|---|---|
| MediaSessionService 通知在部分 ROM 上不稳定 | M1 验收专项真机测试（含后台播放、蓝牙断连暂停场景）；保留手写 Notification 兜底方案 |
| GlassSurface 模糊性能（低端机） | 列表页禁用全屏模糊；播放页降级为静态渐变背景开关 |
| WebDAV 缓存一致性 | 沿用已验证的 ETag/Last-Modified 校验设计，移植而非重写 |
| 双轨期间两套 spec 漂移 | 新增 `.trellis/spec/android/` 规范层，播放器契约在新侧重新固化 |
