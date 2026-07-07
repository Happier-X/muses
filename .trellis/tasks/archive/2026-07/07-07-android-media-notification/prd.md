# 安卓媒体通知控制

## Goal

让 Android 后台播放使用系统标准媒体通知/媒体会话控制，而不是当前手写的普通前台服务通知体验。用户应能在通知栏、锁屏、耳机/蓝牙媒体键等系统入口看到正确歌曲信息，并可靠控制播放、暂停、停止和进度跳转。

## Background / Confirmed Facts

- 当前项目是 Capacitor 8 + Ionic Vue，Android 原生侧已有自定义 `AudioPlayer` Capacitor 插件。
- 当前播放服务 `android/app/src/main/java/ionic/muses/AudioPlaybackService.kt` 已继承 AndroidX Media3 `MediaSessionService`，并创建 `MediaSession` + `ExoPlayer`。
- 当前通知仍由 `NotificationCompat.Builder` 手写，并通过 `NotificationManager.notify(...)` 更新，不是完整的系统标准媒体通知控制体验。
- 当前 Android 依赖已包含：
  - `androidx.media3:media3-exoplayer:1.7.1`
  - `androidx.media3:media3-session:1.7.1`
- 当前 WebDAV 播放依赖原生服务处理密码、Basic Auth、缓存和远程/本地播放 URI。任何替代方案都不能把 WebDAV 密码或 Auth header 暴露到 JS UI 状态、路由、日志、localStorage 或 `muses:songs`。
- 已调研到 Capacitor 生态候选插件：
  - `@capgo/capacitor-media-session@8.0.28`：支持 Capacitor 8，提供 `setMetadata`、`setPlaybackState`、`setPositionState`、`setActionHandler`，许可证 MPL-2.0。
  - `capacitor-music-controls-plugin@6.1.0`：MIT，但依赖 Capacitor 6，不直接匹配当前 Capacitor 8。
  - `@jofr/capacitor-media-session@4.0.0`：GPL-3.0-or-later 且 peer 为 Capacitor 6，不适合当前项目直接引入。
- `@capgo/capacitor-media-session` 主要暴露系统 Media Session 控制，不替代当前 ExoPlayer/WebDAV 缓存/认证播放服务。

## Requirements

### R1. 系统媒体控制体验

- Android 通知栏应显示标准媒体播放通知样式，而不是看起来像普通服务通知。
- 通知/锁屏/系统媒体面板应展示当前歌曲标题、歌手、播放状态和进度（有 duration 时）。
- 通知控制应支持至少：播放、暂停、停止；如果方案支持且实现成本合理，应支持 seek/进度同步。

### R2. 保留当前播放能力

- 保留本地音频播放。
- 保留 WebDAV 播放、WebDAV Basic Auth、后台下载缓存、缓存命中优先播放。
- 保留前端 `AudioPlayer` 插件接口的核心能力：`play`、`pause`、`resume`、`stop`、`seek`、`getState`、`stateChange`。
- 保留播放器页面和迷你播放器的状态同步。

### R3. 安全边界

- WebDAV 密码、Basic Auth header、SecureStorage 值不得进入：
  - 前端 UI state
  - 路由参数
  - logcat / console 日志
  - `localStorage`
  - `muses:songs`
  - 第三方插件 JS API payload（除非已经确认插件不持久化、不记录，且仍只传必要非敏感元数据）
- 如使用 Capacitor 媒体会话插件，只允许传非敏感媒体 metadata、播放状态、position state 和控制回调。

### R4. 技术方案评估

必须在设计阶段明确选择以下方案之一：

1. **优先方案候选 A：继续使用现有原生 `MediaSessionService` + `ExoPlayer`，改用 Media3 官方媒体通知/MediaSession 推荐控制方式**
   - 优点：保留现有 WebDAV/缓存/认证边界，少引入第三方插件风险。
   - 风险：需要补齐 Media3 通知 API 用法，仍有原生代码维护成本。

2. **候选 B：引入 `@capgo/capacitor-media-session` 只负责系统 Media Session metadata/action handler，播放仍由当前原生插件负责**
   - 优点：符合用户提到的“Capacitor 插件”方向，JS 侧可统一设置 metadata/action。
   - 风险：可能与现有 Android Media3 session 重叠；要验证是否会出现双媒体会话、双通知、状态竞争。

3. **不推荐候选 C：用第三方 Capacitor 音乐控制插件完全替换当前播放服务**
   - 不推荐原因：当前 WebDAV 认证、缓存、ExoPlayer、后台服务边界已经较复杂；完全替换风险高，且候选插件兼容性/许可证不理想。

## Acceptance Criteria

- [ ] 在 Android 设备/模拟器上播放歌曲后，通知栏/系统媒体面板呈现标准媒体播放控制体验。
- [ ] 通知/系统媒体控制的播放、暂停、停止能驱动当前播放器，并同步到 mini player 和 `/player` 页面。
- [ ] 有 duration 时，系统媒体入口能显示或同步播放进度；前端 seek 与系统 seek 不互相打架。
- [ ] WebDAV 歌曲播放仍可用，缓存命中仍优先使用本地缓存；缓存 miss 可继续播放远程并后台下载。
- [ ] 本地歌曲播放仍可用。
- [ ] `localStorage.getItem('muses:songs')`、`localStorage.getItem('muses:sources')`、前端 `playerState`、logcat 不包含 WebDAV 密码或 Basic Auth header。
- [ ] 不出现双通知、残留通知、停止后通知无法消失、前台服务未及时 `startForeground` 导致崩溃等问题。
- [ ] 验证命令至少包括：
  - `npm run test:unit -- --run`
  - `npm run lint`
  - `npm run build`
  - `cd android && ./gradlew :app:compileDebugKotlin`

## Out of Scope

- 本任务不做播放队列、上一首/下一首，除非所选媒体通知方案要求占位禁用。
- 本任务不做桌面端/Web 的 Media Session 优化。
- 本任务不重写 WebDAV 音源管理、扫描、标签提取或歌词展示。
- 本任务不引入 GPL 许可证插件。

## Decisions

- 已确认优先采用 Capacitor 插件方案：引入 `@capgo/capacitor-media-session` 作为系统 Media Session metadata/action handler 控制层。
- 播放能力仍保留在当前原生 `AudioPlayer` + `AudioPlaybackService` + `ExoPlayer` 中，插件不接管 WebDAV 密码、Basic Auth、缓存或实际音频播放。

## Open Question

- 需要在设计阶段验证 `@capgo/capacitor-media-session` 在 Android 上是否会创建独立通知/MediaSession，并确定如何避免与当前 `MediaSessionService` 出现双通知或双媒体会话。
