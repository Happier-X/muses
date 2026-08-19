# 技术设计：迁移音频引擎至原生 ExoPlayer

## 架构概览

**重要约束**：项目历史上尝试过 media3 `MediaSessionService`，但 `DefaultMediaNotificationProvider` 在自定义 Intent 通道下通知不稳定。因此采用混合方案：ExoPlayer 用于播放，保留 `@capgo/capacitor-media-session` 用于通知。

```
┌─────────────────────────────────────────────────────────────┐
│                      JS Layer                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │controller.ts│  │ native.ts   │  │ mediaSession.ts     │ │
│  │ (播放逻辑)   │  │ (桥接层)    │  │ (保留，同步通知)    │ │
│  └──────┬──────┘  └──────┬──────┘  └─────────────────────┘ │
│         │                │                                  │
│         └────────┬───────┘                                  │
│                  │ Capacitor Bridge                         │
├──────────────────┼──────────────────────────────────────────┤
│                  ▼                                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           AudioPlayerPlugin.kt (重构)                │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │   │
│  │  │ ExoPlayer   │  │ DeviceRemoval│  │ WebDAV Cache│ │   │
│  │  │ (播放核心)   │  │ (设备移除)   │  │ (保留)      │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                  │                                          │
│                  ▼                                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  @capgo/capacitor-media-session (保留)               │   │
│  │  - 通知栏播控                                         │   │
│  │  - MediaSession 同步                                  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 模块设计

### 1. AudioPlayerPlugin（重构）

**职责**：Capacitor 插件，封装 ExoPlayer 播放引擎

**关键变化**：
- 移除所有 `callNativeAudio()` 反射调用
- 直接使用 ExoPlayer API
- 保留 `AudioDeviceCallback` 设备移除逻辑
- 保留 `WebDavAudioCache` 缓存逻辑

**ExoPlayer 实例管理**：
- 在 Plugin 中创建和管理 ExoPlayer 实例
- 通过 `MediaController` 连接到 `@capgo/capacitor-media-session` 的 MediaSession
- 或直接在 Plugin 中创建 MediaSession 并同步到 capgo

**生命周期**：
- `load()` 时创建 ExoPlayer
- `destroy()` 时释放资源

### 2. AudioPlayerPlugin（重构）

**职责**：Capacitor 插件，JS 桥接层

**新接口设计**：
```typescript
// 加载音频
load(options: {
  uri: string,
  songId: string,
  title?: string,
  artist?: string,
  album?: string,
  coverUri?: string,
  volume?: number,
  audioHeaders?: Record<string, string>
}): Promise<void>

// 播放控制
play(): Promise<void>
pause(): Promise<void>
stop(): Promise<void>
seek(position: number): Promise<void>
setVolume(volume: number): Promise<void>

// 状态查询
getState(): Promise<{
  status: 'idle' | 'loading' | 'playing' | 'paused' | 'stopped' | 'finished' | 'error',
  position: number,
  duration: number,
  bufferedPosition?: number,
  errorMessage?: string
}>

// 音频焦点
setAudioFocus(enabled: boolean): Promise<void>

// 通知权限
ensureNotificationPermission(): Promise<{ granted: boolean }>

// 元数据更新（切歌时调用）
updateMetadata(options: {
  title: string,
  artist?: string,
  album?: string,
  coverUri?: string
}): Promise<void>
```

**事件监听**：
- `stateChange`：播放状态变化
- `playbackComplete`：歌曲播完（触发 JS 切下一首）

### 3. native.ts（重写）

**职责**：JS 桥接层，封装 Capacitor 插件调用

**关键变化**：
- 移除 `NativeAudio` (capgo) 依赖
- 使用新的 `AudioPlayerBridge` 接口
- 简化状态轮询（ExoPlayer 原生上报）
- 移除预案机制相关代码

### 4. controller.ts（重构）

**职责**：播放状态管理

**简化项**：
- 移除 `reconcileAfterBackground()`（回前台对账）
- 移除 `runBackgroundHeartbeat()`（后台心跳）
- 简化 `applyNativeState()`（状态同步逻辑）
- 移除 `registerAutoNextPlan()` / `clearAutoNextPlan()`（预案机制）

**保留项**：
- `playSong()` / `pausePlayback()` / `resumePlayback()` / `seekPlayback()`
- 歌词匹配、封面匹配、元数据扫描
- 队列管理、切歌逻辑

## 数据流

### 播放流程
```
JS: playSong(song)
  ↓
JS: native.ts → AudioPlayerBridge.load(uri, songId, ...)
  ↓
Kotlin: AudioPlayerPlugin.load()
  ↓
Kotlin: ExoPlayer.setMediaItem(MediaItem)
  ↓
Kotlin: ExoPlayer.prepare() + play()
  ↓
Kotlin: ExoPlayer.Listener.onPlaybackStateChanged(STATE_READY)
  ↓
JS: stateChange 事件 → controller.ts 更新 UI
```

### 切歌流程
```
JS: playSong(nextSong) ← 自动调用
  ↓
Kotlin: ExoPlayer播完 → Listener.onPlaybackStateChanged(STATE_ENDED)
  ↓
Kotlin: notifyListeners("playbackComplete")
  ↓
JS: playbackComplete 事件 → controller.ts → playNextFromQueue()
```

### 缓冲进度
```
Kotlin: ExoPlayer.Listener.onBufferedPositionChanged()
  ↓
Kotlin: notifyListeners("stateChange", { bufferedPosition })
  ↓
JS: native.ts 更新 currentBufferedPosition
  ↓
JS: controller.ts 更新 UI 缓冲条
```

## 依赖变更

### 新增（Android）
```gradle
// ExoPlayer Media3
implementation "androidx.media3:media3-exoplayer:1.5.0"
implementation "androidx.media3:media3-common:1.5.0"
implementation "androidx.media3:media3-datasource-okhttp:1.5.0" // WebDAV HTTP 支持
```

### 保留（npm）
```json
// package.json - 保留用于通知
"@capgo/capacitor-media-session": "^8.0.29"
```

### 移除（npm）
```json
// package.json
"@capgo/capacitor-native-audio": "^8.4.19"  // 移除
```

## AndroidManifest.xml 变更

无需新增 Service。保留现有的：
- `MediaButtonReceiver`（通知栏媒体按钮）
- 前台服务权限（`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MEDIA_PLAYBACK`）
- 通知权限（`POST_NOTIFICATIONS`）

ExoPlayer 在 `AudioPlayerPlugin` 中直接使用，不依赖 `MediaSessionService`。

## 兼容性考虑

- **Android 10+**：MediaSessionService 需要 `foregroundServiceType="mediaPlayback"`
- **Android 13+**：通知权限需要动态申请
- **WebDAV 认证**：通过 `DefaultHttpDataSource` 的 `setDefaultRequestProperty` 传递

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| ExoPlayer 与 Capacitor 生命周期冲突 | 播放中断 | 在 Plugin 中管理 ExoPlayer 实例，Service 仅管理 MediaSession |
| WebDAV 认证 Header 传递失败 | 远程播放失败 | 单元测试验证 Header 编码；降级到本地缓存 |
| 通知栏样式变化 | 用户体验差异 | 使用 MediaStyle 默认样式；后续可自定义 |
| 前台服务被系统杀死 | 后台播放中断 | 使用 WakeLock + ForegroundService；ExoPlayer 自动恢复 |
