# Media3 原生媒体通知设计

## 方案决策

采用单一 AndroidX Media3 原生方案：

- `AudioPlaybackService` 继续继承 `MediaSessionService`。
- 服务内 `ExoPlayer` 是唯一播放源。
- 服务内 `MediaSession` 是唯一系统媒体会话。
- Media3 官方通知能力负责用户可见媒体通知、锁屏媒体面板和媒体键。
- 前端只通过自有 `AudioPlayer` Capacitor 插件下发播放命令并接收状态，不再同步 Capgo media session。

## 为什么移除 Capgo

Capgo 插件适合没有原生媒体服务的 Capacitor 应用快速接入 Media Session。但本项目已经拥有 Android 原生播放服务和 Media3：

- Capgo 会创建额外 `MediaSessionService` / `MediaSessionCompat` / foreground notification。
- 当前服务也需要 foreground service 与 `MediaSession`。
- 两套媒体会话会带来双通知、状态不同步、媒体键路由不确定的问题。
- WebDAV 密码与 Basic Auth 应留在同一个原生服务边界内，减少 JS 同步面。

因此更优架构是用 Media3 自己闭环：播放、状态、系统控制都由同一个 `ExoPlayer` 和 `MediaSession` 驱动。

## 架构

### 前端边界

保留：

- `AudioPlayerNative.play(options)`
- `pause()` / `resume()` / `stop()` / `seek()` / `getState()`
- `stateChange` listener
- `playerState`、mini player、`/player` 页面逻辑

移除：

- `src/features/player/media-session.ts`
- `controller.ts` 中 `initializeMediaSessionControls` / `syncMediaSessionState` / `clearMediaSessionState`
- Capgo action handler 相关测试和 mock

前端仍需保证 `playerState` 不包含 WebDAV 密码和 base64 封面。

### Android 服务边界

`AudioPlaybackService` 负责：

1. `onCreate()` 创建 `ExoPlayer` 和 `MediaSession`。
2. `playFromIntent()` 设置 `MediaItem`：
   - `mediaId = songId`
   - `MediaMetadata.title = title`
   - `MediaMetadata.artist = artist`
   - `MediaMetadata.albumTitle = album`
3. WebDAV：
   - 命中缓存时播放 app-private file URI。
   - 未命中时播放远程 URL，并只在 `DefaultHttpDataSource.Factory` 中设置 Basic Auth header。
4. `MediaSession` 绑定同一个 ExoPlayer，让系统媒体控制直接操作播放器。
5. 播放状态通过现有 `ACTION_STATE_CHANGED` 广播回前端。

### 通知策略

优先使用 Media3 官方通知提供器/默认通知路径，而不是手写媒体控制通知。

设计目标：

- `MediaSessionService` 对外暴露唯一 session。
- 用户可见通知由 Media3 session/player metadata 生成。
- 若 Android 前台服务启动仍要求 bootstrap notification，则仅在服务启动早期使用最小占位通知，播放建立后交由 Media3 媒体通知更新。
- 不保留 Capgo 插件通知。

实施时需基于当前 Media3 `1.7.1` API 验证具体类与构造器，例如 `DefaultMediaNotificationProvider` / `setMediaNotificationProvider(...)`。如 API 不可用，则使用 Media3 推荐的等价通知 provider，不回退到 Capgo。

## 控制流

### App UI 播放

1. 前端 `playSong(song)` 从 SecureStorage 获取 WebDAV 密码。
2. 前端调用自有 `AudioPlayerNative.play(...)`，密码只进入该 native 调用。
3. `AudioPlayerPlugin.kt` 使用 Intent 启动 `AudioPlaybackService`。
4. `AudioPlaybackService` 创建/更新 `MediaItem` 并播放。
5. Media3 根据 session/player 状态展示媒体通知。
6. 服务广播安全播放状态回前端。

### 系统媒体控制

1. 用户点击通知/锁屏/耳机按键。
2. Android 系统把控制命令发给 Media3 `MediaSession`。
3. Media3 控制同一个 `ExoPlayer`。
4. `Player.Listener` 触发 `publishState(...)`。
5. 前端 `playerState` 更新。

不经过 JS action handler，减少竞态和重复状态源。

## 安全契约

- WebDAV 密码只允许出现在：
  - SecureStorage 读取结果。
  - `AudioPlayerNative.play(...)` 原生调用参数。
  - `AudioPlayerPlugin.kt` 发送给服务的 Intent extra。
  - `AudioPlaybackService.kt` 构造 Basic Auth header 的局部变量。
- 禁止进入：
  - `playerState`
  - `muses:songs`
  - localStorage
  - logcat
  - Media3 metadata
  - 通知文本
- Media3 metadata 只放标题、歌手、专辑等非敏感展示字段。
- 封面如果后续接入，只能用 app-private 安全 URI，不传 `data:` / base64。

## 风险与回退

### Media3 通知 API 差异

风险：`media3-session:1.7.1` 的通知 provider API 与样例不同。

处理：以本地依赖源码/编译结果为准，选择 1.7.1 可编译的官方 API；必要时新增 `media3-ui` 或官方通知相关依赖，但不引入第三方插件。

### foreground service 启动时机

风险：移除手写通知后，`startForegroundService` 未及时 `startForeground` 会崩溃。

处理：保留最小 bootstrap notification，确保服务生命周期合规；播放建立后让 Media3 媒体通知承担用户可见控制。

### 默认媒体动作过多

风险：Media3 默认通知展示上一首/下一首，但当前没有队列。

处理：通过 session/player command 或通知 provider 限制动作；若短期不能安全定制，则设备验证记录表现并优先确保播放/暂停/停止/seek 正确。

## 验证

- 单元测试：确认前端移除 Capgo 依赖后播放器控制测试仍覆盖本地/WebDAV/seek/stop/安全状态。
- 编译：Android Kotlin 编译必须通过。
- 设备：检查通知、锁屏、媒体键、WebDAV 安全、停止清理、无双通知。
