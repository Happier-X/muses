# Implement — 媒体播放通知栏卡片

## 最终实现

最终未继续沿用自研 `AudioPlaybackService + media3 DefaultMediaNotificationProvider` 方案，而是切换为 **`@capgo/capacitor-native-audio` 接管 Android 原生播放与媒体通知**。

## 执行步骤

### Phase 1 — 前端封装切换到 NativeAudio

**文件：`src/features/player/native.ts`**

- 保留既有 `AudioPlayerNative` 接口，业务层无感迁移。
- 内部改为调用 `@capgo/capacitor-native-audio`：
  - `configure`
  - `preload`
  - `play`
  - `pause`
  - `resume`
  - `stop`
  - `setCurrentTime`
- 配置：
  - `focus: true`
  - `background: true`
  - `backgroundPlayback: true`
  - `showNotification: true`
- 事件适配：
  - `playbackState` → `stateChange`
  - `currentTime` → `stateChange`
  - `complete` → `finished`

### Phase 2 — 元数据与封面透传

**文件：`src/features/player/native.ts`**

播放前将歌曲信息透传到 NativeAudio 的 `notificationMetadata`：

- `title`
- `artist`
- `album`
- `artworkUrl`

这样 Android 端通知由 NativeAudio 自动生成 `MediaStyle` 卡片。

### Phase 3 — 本地 content:// 兼容

**文件：`android/app/src/main/java/com/muses/player/AudioPlayerPlugin.kt`**

保留一个最小 Kotlin 桥：

- `prepareLocalAudioFile({ uri, songId })`
- 仅负责把 `content://` 复制到 app cache，并返回 `file://`
- `ensureNotificationPermission()` 继续负责 Android 13+ `POST_NOTIFICATIONS`

### Phase 4 — WebDAV 认证透传

**文件：`src/features/player/native.ts`**

WebDAV 使用 NativeAudio 远程 URL 播放，并传入：

```ts
headers: {
  Authorization: `Basic ${encodeBasicAuth(username, password)}`,
}
```

### Phase 5 — 清理旧原生服务

**文件：`android/app/src/main/java/com/muses/player/AudioPlaybackService.kt`**

清理为兼容空服务：

- 不再包含 ExoPlayer/media3/session/手动通知逻辑
- `onStartCommand` 立即停止自身

**文件：`android/app/src/main/AndroidManifest.xml`**

- 清理旧媒体服务声明
- 保留/补齐 NativeAudio 所需权限（含 `WAKE_LOCK`）

### Phase 6 — 依赖与 Capacitor 同步

**文件：**
- `package.json`
- `package-lock.json`
- `android/capacitor.settings.gradle`
- `android/app/capacitor.build.gradle`
- `capacitor.config.ts`

改动：

- 新增 `@capgo/capacitor-native-audio`
- `npx cap sync android`
- 关闭 NativeAudio 的 HLS 可选依赖：
  - `capacitor.config.ts` 中设置 `NativeAudio.hls = false`

## 验证结果

### 构建验证

已通过：

```bash
npm run build
cd android && ./gradlew :app:assembleDebug
```

### 模拟器验证

MuMu 模拟器实测通过：

- 播放后通知栏出现 `MediaStyle` 媒体通知
- `dumpsys notification` 中记录：
  - `category=transport`
  - `android.template=android.app.Notification$MediaStyle`
  - `android.mediaSession=Token`
  - `android.largeIcon=BITMAP`
  - `actions=3`
- 标题、艺术家、封面正确展示

## 后续增强（未做）

- 当前 NativeAudio 默认通知按钮为：快退 15 秒 / 暂停 / 快进 15 秒。
- 若后续要求通知栏按钮必须是「上一首 / 暂停 / 下一首」，可再评估：
  - 接入 `@capgo/capacitor-media-session`
  - 或扩展 NativeAudio/action 映射
