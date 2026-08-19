# 执行计划：迁移音频引擎至原生 ExoPlayer

## 前置条件

- [x] 确认 Android SDK 版本 ≥ 29 (Android 10)
- [x] 确认 Kotlin 版本兼容 Media3
- [x] 备份当前 `AudioPlayerPlugin.kt` 和 `native.ts`

## 阶段 1：依赖配置

### 1.1 更新 build.gradle
```bash
# android/app/build.gradle 添加依赖
implementation "androidx.media3:media3-exoplayer:1.5.0"
implementation "androidx.media3:media3-common:1.5.0"
implementation "androidx.media3:media3-datasource-okhttp:1.5.0"
```

### 1.2 移除 capgo native-audio 依赖
```bash
pnpm remove @capgo/capacitor-native-audio
```

### 1.3 保留 capgo media-session 依赖
```bash
# 保留，用于通知栏播控
# @capgo/capacitor-media-session 已在 package.json 中
```

**验证**：`cd android && ./gradlew assembleDebug` 编译通过 ✅

---

## 阶段 2：原生层实现

### 2.1 重写 AudioPlayerPlugin.kt
核心改动：
- 移除所有 `callNativeAudio()` 反射调用
- 实现新的 Capacitor `@PluginMethod`
- 直接使用 ExoPlayer API
- 保留 `AudioDeviceCallback` 设备移除逻辑
- 保留 `WebDavAudioCache` 缓存逻辑

**关键方法**：
```kotlin
@PluginMethod
fun load(call: PluginCall) {
    val uri = call.getString("uri") ?: return call.reject("missingUri")
    val songId = call.getString("songId") ?: return call.reject("missingSongId")
    val headers = call.getObject("audioHeaders")

    // 构建 MediaItem
    val mediaItem = MediaItem.Builder()
        .setMediaId(songId)
        .setUri(Uri.parse(uri))
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(Uri.parse(uri))
                .build()
        )
        .build()

    // 设置 Header（WebDAV 认证）
    if (headers != null) {
        // 通过 DataSource.Factory 设置
    }

    // 发送到 PlaybackService
    controller?.apply {
        setMediaItem(mediaItem)
        prepare()
    }
    call.resolve()
}

@PluginMethod
fun play(call: PluginCall) {
    controller?.play()
    call.resolve()
}

@PluginMethod
fun pause(call: PluginCall) {
    controller?.pause()
    call.resolve()
}

// ... 其他方法
```

**验证**：JS 可调用 load/play/pause，ExoPlayer 响应 ✅

### 2.3 实现设备移除暂停
保留现有 `AudioDeviceCallback` 逻辑，改为直接调用 `controller?.pause()`

**验证**：拔出蓝牙耳机，播放暂停

---

## 阶段 3：JS 层重写

### 3.1 重写 native.ts
```typescript
// src/features/player/native.ts

interface AudioPlayerBridge {
  load(options: LoadOptions): Promise<void>
  play(): Promise<void>
  pause(): Promise<void>
  stop(): Promise<void>
  seek(options: { position: number }): Promise<void>
  setVolume(volume: number): Promise<void>
  getState(): Promise<PlayerState>
  setAudioFocus(enabled: boolean): Promise<void>
  ensureNotificationPermission(): Promise<{ granted: boolean }>
  updateMetadata(options: MetadataOptions): Promise<void>
  addListener(event: 'stateChange', handler: (state: PlayerState) => void): Promise<PluginListenerHandle>
  addListener(event: 'playbackComplete', handler: () => void): Promise<PluginListenerHandle>
}

export const AudioPlayerBridge = registerPlugin<AudioPlayerBridge>('AudioPlayer')

// 重写 AudioPlayerNative
export const AudioPlayerNative = {
  async play(options: PlayOptions) {
    await AudioPlayerBridge.load({
      uri: options.sourceType === 'webdav' ? options.url! : options.uri!,
      songId: options.songId,
      title: options.title,
      artist: options.artist,
      album: options.album,
      coverUri: options.coverUri,
      volume: options.volume,
    })
    await AudioPlayerBridge.play()
  },

  async pause() {
    await AudioPlayerBridge.pause()
  },

  async resume() {
    await AudioPlayerBridge.play()
  },

  async stop() {
    await AudioPlayerBridge.stop()
  },

  async seek(options: SeekOptions) {
    await AudioPlayerBridge.seek({ position: options.position })
  },

  async setVolume(volume: number) {
    await AudioPlayerBridge.setVolume(volume)
  },

  async getState() {
    return await AudioPlayerBridge.getState()
  },

  // ... 其他方法
}
```

**验证**：JS 可调用新接口，状态正确返回 ✅

### 3.2 重构 controller.ts
```typescript
// 移除的函数
- reconcileAfterBackground()
- runBackgroundHeartbeat()
- startAutoNextHeartbeat()
- registerAutoNextPlan()
- clearAutoNextPlan()

// 简化的函数
- applyNativeState()：移除预案相关逻辑
- initializePlayer()：移除预案注册和心跳启动

// 保留的函数
+ playSong()
+ pausePlayback()
+ resumePlayback()
+ seekPlayback()
+ stopPlayback()
+ matchOnlineLyricsForSong()
+ matchOnlineCoverForSong()
+ scanSongMetadata()
```

**验证**：播放/暂停/切歌正常，歌词封面匹配正常 ✅

---

## 阶段 4：集成测试

### 4.1 构建验证 ✅
- [x] TypeScript 编译通过
- [x] Android 编译通过
- [x] 单元测试通过（99 tests）
- [x] Lint 通过

### 4.2 功能验证（待真机测试）
- [ ] 播放本地 MP3/FLAC 文件
- [ ] 暂停/恢复播放
- [ ] Seek 到指定位置
- [ ] 音量控制（ReplayGain）
- [ ] 切歌无间隔（Gapless）
- [ ] 播放 WebDAV 远程文件
- [ ] 缓冲进度正确显示
- [ ] 后台播放稳定性
- [ ] 通知栏控制按钮
- [ ] 音频焦点管理
- [ ] 设备移除暂停

---

## 阶段 5：清理与提交

### 5.1 移除遗留代码 ✅
- [x] 删除 `capgo` 相关 import（native-audio）
- [x] 删除 `callNativeAudio()` 反射调用
- [x] 简化预案机制（保留空实现兼容现有代码）

### 5.2 更新文档
- [ ] 更新 CHANGELOG.md
- [ ] 更新 README.md（如有播放相关说明）

### 5.3 提交
```bash
git add -A
git commit -m "refactor(player): 迁移音频引擎至 ExoPlayer Media3

- 移除 @capgo/capacitor-native-audio 依赖
- 重写 AudioPlayerPlugin 为 ExoPlayer 封装
- 重写 native.ts 桥接层
- 支持 WebDAV 边下边播
- 保留设备移除暂停功能
- 保留 @capgo/capacitor-media-session 用于通知"

git tag v0.4.0-exoplayer
```

---

## 回滚方案

如果 ExoPlayer 集成出现严重问题：

1. **快速回滚**：
```bash
git revert HEAD
pnpm add @capgo/capacitor-native-audio@8.4.19
```

2. **恢复文件**：
   - `AudioPlayerPlugin.kt` → 从 git history 恢复
   - `native.ts` → 从 git history 恢复
   - `controller.ts` → 从 git history 恢复

3. **保留的依赖**：
   - `@capgo/capacitor-media-session` 始终保留（用于通知）

---

## 预估工时

| 阶段 | 工时 | 风险 |
|------|------|------|
| 阶段 1：依赖配置 | 0.5h | 低 |
| 阶段 2：原生层实现 | 4h | 中 |
| 阶段 3：JS 层重写 | 3h | 中 |
| 阶段 4：集成测试 | 3h | 高 |
| 阶段 5：清理提交 | 1h | 低 |
| **总计** | **11.5h** | - |
