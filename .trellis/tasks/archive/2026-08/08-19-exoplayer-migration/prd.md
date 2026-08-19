# 迁移音频引擎至原生 ExoPlayer

## Goal

将 muses 的音频播放引擎从 `@capgo/capacitor-native-audio`（第三方 Capacitor 插件）迁移到原生 ExoPlayer，参考 SPlayer-for-Android 的实现方式，提升后台播放稳定性、音频焦点处理能力和 gapless 切歌体验。

## Background

### 当前架构
- **音频引擎**：`@capgo/capacitor-native-audio` —— 第三方 Capacitor 插件，封装了 Android/iOS 原生音频播放
- **桥接层**：自定义 `AudioPlayerBridge`（`src/features/player/native.ts`）处理缓冲进度、自动切歌预案、通知权限等
- **状态管理**：JS 层通过 `NativeAudio` API（preload/play/pause/resume/stop/seek）+ 事件监听驱动 UI
- **后台兜底**：自定义预案机制（`setAutoNext`/`clearAutoNext`）在 JS 冻结时由原生兜底播放下一首

### 痛点
1. **后台稳定性**：`capacitor-native-audio` 在长时间后台时偶发状态丢失，需要复杂的 JS 心跳和对账逻辑
2. **音频焦点**：插件仅提供 `focus: true` 配置，无法精细控制与其他应用的交互
3. **Gapless 切歌**：完全依赖 JS 层手动管理，需要预加载、预案、心跳等大量代码
4. **缓冲进度**：需要自定义 Bridge 上报，逻辑复杂且有边界情况

### 参考项目
- **SPlayer-for-Android**：Vue 3 + Capacitor 架构，原生 ExoPlayer + WebView 双引擎
  - 长时间后台稳定
  - seek/gapless 切歌进度条同步
  - 原生 MediaSession 通知栏播控

## Scope

### In Scope
- Android 原生层：集成 ExoPlayer（Media3）库
- Capacitor 自定义插件：替代 `@capgo/capacitor-native-audio`
- 基础播放能力：play/pause/resume/stop/seek/volume
- 缓冲进度上报：原生 ExoPlayer `onBufferedPositionChanged`
- 音频焦点管理：ExoPlayer `AudioFocusManager`
- 通知栏播控：MediaSession + Notification 接入

### Out of Scope
- 桌面歌词（WindowManager 悬浮窗）—— 现有实现保持不变
- iOS 端适配 —— 本次仅 Android
- WebDAV 流媒体播放优化 —— 后续任务
- 在线音乐源（网易云/Jellyfin）播放 —— 不涉及

## Acceptance Criteria

1. **基础播放**：本地音频文件可正常播放、暂停、恢复、停止、seek
2. **后台稳定性**：应用进入后台 30 分钟后回来，播放状态正确同步
3. **音频焦点**：播放时暂停其他应用音频，停止后恢复；可配置"允许同时播放"
4. **Gapless 切歌**：队列切歌无明显间隔，进度条平滑过渡
5. **缓冲进度**：缓冲条正确显示，seek 不越界
6. **通知栏**：显示歌曲信息、播放/暂停/上下首按钮
7. **音量控制**：ReplayGain 响度均衡正常工作
8. **兼容性**：Android 10+ 设备正常运行

## Key Decisions

1. **迁移策略**：完全替换 `@capgo/capacitor-native-audio`，`AudioPlayerPlugin` 直接封装 ExoPlayer（无双引擎共存）
2. **交付方式**：一次性完成全部替换（不渐进式），移除 capgo 依赖
3. **接口设计**：重新设计 JS 桥接接口（按 ExoPlayer 语义：load/play/pause/seek/setVolume），不保持 capgo 旧接口
4. **后台自动切歌**：大幅简化预案机制。ExoPlayer + MediaSession + Foreground Service 天然支持后台播放，原生层仅在歌曲播完时通知 JS 切下一首，不再需要轮询 isMusicActive 的兜底逻辑

5. **通知栏实现**：采用 ExoPlayer 标准方案——`MediaSession.Builder(context, player)` + `MediaSessionService` 前台服务，MediaSession 自动同步播放状态，通知栏使用 MediaStyle

6. **音频焦点**：采用 SPlayer 方案——默认 `handleAudioFocus=true`（抢占焦点），设置中可关闭（`handleAudioFocus=false`，完全不管理焦点，允许同时播放）

7. **WebDAV 播放**：ExoPlayer 直接流播 WebDAV HTTP URL（边下边播），缓冲进度由 ExoPlayer `onBufferedPositionChanged` 原生上报；仍保留本地缓存机制供离线播放

8. **JS 层重构**：`controller.ts` 适度重构——移除轮询/对账/心跳等 ExoPlayer 已原生支持的逻辑，保留核心播放/切歌/歌词/封面逻辑
9. **WebDAV 认证**：使用 `DefaultHttpDataSource.Factory().setDefaultRequestProperty` 传递 Basic Auth Header
10. **设备移除暂停**：保持现有 `AudioDeviceCallback` 实现（支持蓝牙/有线/USB/车机），将反射调用 capgo 改为直接调用 ExoPlayer pause

## Open Questions

无
