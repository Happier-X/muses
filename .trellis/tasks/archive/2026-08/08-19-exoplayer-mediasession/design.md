# 技术设计：用 ExoPlayer MediaSession 替换 capgo media-session

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                      JS Layer                               │
│  ┌─────────────┐  ┌─────────────┐                          │
│  │controller.ts│  │ native.ts   │  (移除 mediaSession.ts)  │
│  └──────┬──────┘  └──────┬──────┘                          │
│         │                │                                  │
│         └────────┬───────┘                                  │
│                  │ Capacitor Bridge                         │
├──────────────────┼──────────────────────────────────────────┤
│                  ▼                                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  AudioPlayerPlugin.kt (简化版)                       │   │
│  │  - MediaController 桥接                              │   │
│  │  - load/play/pause/seek/setVolume                   │   │
│  │  - 保留: prepareArtworkDataUrl, cacheRemoteCover    │   │
│  └──────────────────┬──────────────────────────────────┘   │
│                     │ bindService                           │
│                     ▼                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  PlaybackService : MediaSessionService (新增)        │   │
│  │  - ExoPlayer 实例                                    │   │
│  │  - MediaSession 自动同步                             │   │
│  │  - DefaultMediaNotificationProvider 通知栏           │   │
│  │  - AudioDeviceCallback 设备移除暂停                  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 模块设计

### 1. PlaybackService（新增）

**职责**：前台服务，管理 ExoPlayer 生命周期和 MediaSession

**关键类**：
- `PlaybackService : MediaSessionService`
- 单例 ExoPlayer 实例
- MediaSession 自动管理通知栏

**生命周期**：
- `onCreate()`：创建 ExoPlayer + MediaSession
- `onGetSession()`：返回 MediaSession 给外部客户端
- `onDestroy()`：释放资源

**AudioDeviceCallback**：
- 从 `AudioPlayerPlugin` 移动到 `PlaybackService`
- 在 Service 中注册，设备移除时暂停播放

### 2. AudioPlayerPlugin（简化）

**职责**：Capacitor 插件，MediaController 桥接

**关键变化**：
- 移除 ExoPlayer 实例管理
- 移除 AudioDeviceCallback
- 通过 MediaController 控制 Service 中的 ExoPlayer
- 保留: prepareArtworkDataUrl, cacheRemoteCover, getCachedWebDavAudioFile, prefetchWebDavAudioFile

**新接口设计**：
```typescript
// 播放控制（通过 MediaController）
load(options: LoadOptions): Promise<void>
play(): Promise<void>
pause(): Promise<void>
stop(): Promise<void>
seek(position: number): Promise<void>
setVolume(volume: number): Promise<void>
getState(): Promise<PlayerState>

// 音频焦点
setAudioFocus(enabled: boolean): Promise<void>

// 封面转换（保留）
prepareArtworkDataUrl(options: { uri: string }): Promise<{ dataUrl: string | null }>
cacheRemoteCover(options: { url: string; cacheKey: string }): Promise<{ uri: string | null }>

// WebDAV 缓存（保留）
prepareLocalAudioFile(options: { uri: string; songId: string }): Promise<{ uri: string }>
getCachedWebDavAudioFile(options: { url: string }): Promise<{ uri: string | null }>
prefetchWebDavAudioFile(options: WebDavOptions): Promise<{ cached: boolean; started: boolean }>

// 通知权限
ensureNotificationPermission(): Promise<{ granted: boolean }>
```

**事件监听**：
- `stateChange`：播放状态变化（从 MediaController.Listener 转发）
- `playbackComplete`：歌曲播完（从 MediaController.Listener 转发）

### 3. native.ts（重写）

**职责**：JS 桥接层

**关键变化**：
- 移除 MediaSession 相关代码
- 简化状态同步逻辑
- 保留: prepareArtworkDataUrl, cacheRemoteCover, getCachedWebDavAudioFile, prefetchWebDavAudioFile

### 4. mediaSession.ts（移除）

**职责**：MediaSession 状态同步

**关键变化**：
- 完全移除，由 PlaybackService 自动管理

## 数据流

### 播放流程
```
JS: playSong(song)
  ↓
JS: native.ts → AudioPlayerBridge.load(uri, songId, ...)
  ↓
Kotlin: AudioPlayerPlugin.load()
  ↓
Kotlin: MediaController.setMediaItem(mediaItem)
  ↓
Kotlin: MediaController.prepare() + play()
  ↓
Kotlin: MediaSession 自动同步状态
  ↓
Kotlin: DefaultMediaNotificationProvider 自动更新通知栏
  ↓
JS: stateChange 事件 → controller.ts 更新 UI
```

### 切歌流程
```
JS: playSong(nextSong) ← 自动调用
  ↓
Kotlin: ExoPlayer播完 → MediaSession 自动同步
  ↓
Kotlin: 通知栏自动更新为新歌曲
  ↓
JS: playbackComplete 事件 → controller.ts → playNextFromQueue()
```

## 依赖变更

### 新增（Android）
```gradle
// ExoPlayer Media3（已有）
implementation "androidx.media3:media3-exoplayer:1.5.0"
implementation "androidx.media3:media3-common:1.5.0"
implementation "androidx.media3:media3-datasource-okhttp:1.5.0"

// Media3 Session（新增）
implementation "androidx.media3:media3-session:1.5.0"
```

### 移除（npm）
```json
// package.json
"@capgo/capacitor-media-session": "^8.0.29"  // 移除
```

## AndroidManifest.xml 变更

```xml
<!-- 新增 PlaybackService -->
<service
    android:name=".PlaybackService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| MediaSessionService 通知不稳定 | 通知栏不显示 | spec 记录过此问题，但当时架构不同；现在用标准方案，应该可行 |
| MediaController 连接失败 | 无法控制播放 | 重试机制 + 降级到直接 Intent 命令 |
| Service 被系统杀死 | 播放中断 | 使用 ForegroundService + WakeLock |
| 封面转换失败 | 通知栏无封面 | 保留空封面占位图 |
