# Design — 媒体播放通知栏卡片

## 最终方案概述

最终采用 **`@capgo/capacitor-native-audio` 接管原生播放与媒体通知**，替换原先自研 `AudioPlaybackService + media3 DefaultMediaNotificationProvider` 播放链路。

原因：原方案要求保持 media3 `MediaSessionService` + `DefaultMediaNotificationProvider`，但本项目播放入口是自定义 `ACTION_PLAY` Intent，而不是 media3 标准 `MediaController` 命令通道。实测中，即使封面 URI、运行时权限、通知 channel 和 foreground service 均补齐，`DefaultMediaNotificationProvider` 仍可能因内部 connected controller / timeline 判断不创建通知或移除通知，导致通知栏无法稳定显示官方媒体卡片。

`@capgo/capacitor-native-audio` Android 端使用 `MediaSessionCompat + NotificationCompat.MediaStyle` 创建系统标准媒体通知，已验证通知记录中存在：

- `android.template=android.app.Notification$MediaStyle`
- `category=transport`
- `android.mediaSession=MediaSession$Token`
- `android.largeIcon=BITMAP`
- 3 个媒体 action（默认快退 / 暂停 / 快进）

## 架构

### 前端统一封装

保留业务层使用的 `AudioPlayerNative` 接口不变，内部改为调用 Capgo NativeAudio：

- `src/features/player/native.ts`
  - `NativeAudio.configure({ focus: true, background: true, backgroundPlayback: true, showNotification: true })`
  - `NativeAudio.preload(...)`
  - `NativeAudio.play / pause / resume / stop / setCurrentTime`
  - 监听 `playbackState`、`currentTime`、`complete` 并转换为既有 `stateChange`

这样 `controller.ts`、队列、播放页状态同步逻辑无需大改。

### 元数据与封面

播放前传入 `notificationMetadata`：

```ts
notificationMetadata: {
  title: options.title,
  artist: options.artist,
  album: options.album,
  artworkUrl: options.coverUri,
}
```

封面仍走既有链路：

```text
song.coverUri → toSafeCoverUri → PlayOptions.coverUri → NativeAudio notificationMetadata.artworkUrl
```

`data:` base64 封面仍由 `toSafeCoverUri` 过滤；`file://` 缓存封面会传给 NativeAudio。

### 本地音源

NativeAudio 对 `file://` 更稳定。项目中本地音源可能是 Android SAF 的 `content://`，因此保留一个最小 Kotlin 桥：

- `AudioPlayerPlugin.prepareLocalAudioFile({ uri, songId })`
- 当 URI 为 `content://` 时复制到 `cacheDir/native-audio-playback/<sha256>.audio`
- 返回 `file://` 给 NativeAudio 播放

`file://` 本地音源直接传给 NativeAudio。

### WebDAV 音源

WebDAV 继续使用原始 URL 远程播放，认证通过 NativeAudio 的 `headers` 传入：

```ts
headers: {
  Authorization: `Basic ${encodeBasicAuth(username, password)}`,
}
```

标签扫描和封面缓存仍沿用现有 library metadata 逻辑。

### 通知权限

Android 13+ 运行时权限仍由最小 `AudioPlayerPlugin` 负责：

- `ensureNotificationPermission()`
- `@CapacitorPlugin(permissions = POST_NOTIFICATIONS)`

前端仍在首次播放前调用，授权失败不阻塞播放，只影响通知显示。

### 旧服务清理

`AudioPlaybackService` 清理为兼容旧安装包/残留显式 service 请求的空服务：

- 不再创建 ExoPlayer
- 不再创建 media3 MediaSessionService
- 不再创建手动 NotificationCompat 通知
- `onStartCommand` 立即 `stopSelf(startId)`

避免与 NativeAudio 自身通知重复或互相移除。

## 非目标

- 不自定义 RemoteViews。
- 不继续维护自研 media3 `DefaultMediaNotificationProvider` 通知链路。
- 当前不实现通知栏上一首/下一首按钮；NativeAudio 默认媒体 action 是快退 / 暂停 / 快进。若后续强需求上一首/下一首，可再评估 `@capgo/capacitor-media-session` 或扩展 NativeAudio action。

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| Android 后台长时间播放仍可能受系统限制 | NativeAudio 已开启 `backgroundPlayback` 和媒体通知；后续真机长时后台验证 |
| 本地 `content://` 大文件首次播放复制耗时 | 只在播放前复制到 cache；失败时返回既有安全错误 |
| WebDAV 服务器对 Range/Header 兼容差 | 使用 NativeAudio `headers` 传 Basic Auth；需用实际 WebDAV 源回归 |
| 通知按钮不是上一首/下一首 | 当前满足标准媒体卡片；下一首/上一首作为后续增强 |

## 验证策略

- 自动验证：
  - `npm run build`
  - `cd android && ./gradlew :app:assembleDebug`
- 手动验证（已在 MuMu 模拟器验证）：
  - 播放后通知栏出现 `Notification$MediaStyle` 媒体通知
  - 通知含封面 `largeIcon`
  - 通知含媒体 session token
  - 通知 action 为媒体传输控制
