# 安卓媒体通知控制设计

## 方案决策

采用 `@capgo/capacitor-media-session` 作为系统媒体会话/媒体通知控制层。当前原生 `AudioPlayer`、`AudioPlaybackService`、ExoPlayer、WebDAV 认证与缓存仍负责实际播放，不把 WebDAV 密码或 Basic Auth header 交给第三方插件。

## 现状与约束

- 当前 Android 播放服务已是 `MediaSessionService`，但通知是手写 `NotificationCompat.Builder`，不是系统媒体通知样式。
- Capgo 插件 Android 侧自带 `com.capgo.mediasession.MediaSessionService`：
  - 通过 `MediaSessionCompat` 创建系统媒体会话。
  - 通过 `NotificationCompat.MediaStyle` 创建媒体通知。
  - 在 `setPlaybackState('playing'|'paused')` 时启动/保持 foreground service。
  - `setActionHandler` 支持 `play`、`pause`、`seekto`、`stop` 等动作。
- 现有 `AudioPlaybackService` 需要前台服务通知来满足 Android 后台播放要求；若简单叠加 Capgo，可能出现两个通知/两个媒体会话。
- WebDAV 播放密码只允许进入当前原生播放服务边界，不能进入 Capgo plugin payload。

## 架构边界

### 前端新增媒体会话同步层

新增或扩展 `src/features/player` 内的媒体会话同步模块：

- 输入：只读 `playerState`、当前歌曲非敏感元数据、播放进度。
- 输出：调用 Capgo `MediaSession` 插件：
  - `setMetadata({ title, artist, album, artwork? })`
  - `setPlaybackState({ playbackState })`
  - `setPositionState({ duration, position, playbackRate })`
  - `setActionHandler({ action }, handler)`
- 允许传递：标题、歌手、专辑、封面展示 URI、duration、position、播放状态。
- 禁止传递：WebDAV 密码、Basic Auth header、SecureStorage 值、音频 URL 中的认证信息、任意 `data:` 封面 URI。

### 控制动作流

Capgo 插件收到系统媒体动作后回调 JS：

- `play` -> `resumePlayback()`
- `pause` -> `pausePlayback()`
- `stop` -> `stopPlayback()`
- `seekto` -> `seekPlayback(details.seekTime)`，单位按插件定义为秒/浏览器 Media Session 风格值，实施时需实际确认并单测封装。

前端播放器控制、迷你播放器、系统通知入口都通过同一个 `player/controller.ts` 方法驱动，避免状态分叉。

### Android 原生服务处理

现有 `AudioPlaybackService` 保留 ExoPlayer/WebDAV/缓存能力。为避免双通知，实施时优先探索：

1. 播放时由 Capgo 插件显示用户可见媒体通知。
2. 当前 `AudioPlaybackService` 的手写通知不再承担媒体控制 UI，只保留满足 foreground service 的最小必要通知或改造为不可与 Capgo 媒体通知冲突的低可见通知。
3. 如果 Android 系统仍强制显示两个 foreground service 通知，则在实施阶段记录限制，并评估是否需要进一步把 ExoPlayer 迁入单一服务，或回退到 Media3 官方通知方案。

## 数据流

1. 用户在歌曲列表点击播放。
2. `playSong(song)` 从 `muses:songs` 最新记录补齐元数据，解析 WebDAV 密码并调用当前 `AudioPlayerNative.play(...)`。
3. `AudioPlaybackService` 开始 ExoPlayer 播放、缓存 WebDAV、广播 `stateChange`（status/position/duration）。
4. 前端 `player/controller.ts` 更新 `playerState`。
5. 媒体会话同步层把非敏感 metadata/status/position 推给 Capgo 插件。
6. Android 系统通知/锁屏/媒体键通过 Capgo 回调触发 `pausePlayback`/`resumePlayback`/`stopPlayback`/`seekPlayback`。
7. 控制结果由原生播放服务广播回前端，再同步回 Capgo 状态。

## 依赖与配置

- npm 依赖：`@capgo/capacitor-media-session@8.0.28`。
- 运行 `npx cap sync android` 生成原生插件配置。
- 需要检查 Android manifest 合并后是否有两个 media playback service；如有冲突或双通知，实施中必须验证并记录。

## 安全设计

- Capgo `setMetadata` artwork 不允许传 `data:` URI，避免 base64 封面进入插件解析路径。
- 不把 WebDAV URL、username/password、Authorization header 传入 Capgo。
- 不在 action handler 或日志中输出插件回调的完整对象。
- 维持 `capacitor.config.ts` 的 `loggingBehavior: 'none'`，避免原生 plugin call 参数进入 logcat。

## 风险与回退

### 风险：双通知 / 双 MediaSession

Capgo 插件自带前台服务通知，现有播放服务也需要 foreground notification。若系统显示两个通知，用户体验不达标。

回退策略：

- 优先隐藏/弱化现有播放服务通知，仅让 Capgo 通知承担媒体 UI。
- 如果 Android 平台无法避免两个 foreground 通知，则回到 Media3 原生通知方案，或计划下一阶段合并播放服务与媒体通知服务。

### 风险：seek 单位不一致

Capgo TypeScript 定义 `seekTime?: number`，但 Android callback 源码需确认单位。实施时必须用封装层统一为秒，并用测试覆盖。

### 风险：插件 artwork 解析

Capgo Android 会尝试解析 http 和 base64 artwork；本项目只传非敏感、可展示且非 `data:` 的封面 URI。若 app-private file URI 不被插件解析，先允许无大图通知，不为封面放宽安全边界。

## 验证策略

- 单元测试：
  - 播放状态变化时调用媒体会话同步方法。
  - WebDAV 密码不会进入 Capgo payload。
  - `data:` 封面不会传给 Capgo。
  - action handler 调用现有播放器控制方法。
- Android 编译：`cd android && ./gradlew :app:compileDebugKotlin`。
- 设备/模拟器验证：
  - 播放本地歌曲和 WebDAV 歌曲。
  - 检查通知栏、锁屏/系统媒体面板是否为媒体通知样式。
  - 通知控制播放/暂停/停止/seek 能同步播放器页面和 mini player。
  - 检查是否出现双通知、残留通知、停止后无法清除。
  - 检查 logcat/localStorage/playerState 不包含 WebDAV 密码或 Basic Auth。
