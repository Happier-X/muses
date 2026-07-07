# Media3 原生媒体通知实施计划

## 实施顺序

### 1. 移除 Capgo 插件集成

- 从 `package.json` / `package-lock.json` 移除 `@capgo/capacitor-media-session`。
- 删除 `src/features/player/media-session.ts`。
- 从 `src/features/player/controller.ts` 移除：
  - media session import
  - `mediaSessionReady`
  - `initializeMediaSessionSafely()`
  - `syncMediaSessionSafely()`
  - `clearMediaSessionSafely()`
  - 各控制函数里的 Capgo 同步调用
- 运行 Capacitor sync，清理 Android 自动生成的 Capgo project/include 配置。

### 2. 调整前端测试

- 删除 `@capgo/capacitor-media-session` mock。
- 删除 Capgo metadata/action handler/初始化重试测试。
- 保留并确保通过：
  - 本地播放只传 content URI。
  - WebDAV 密码只传给原生播放参数，不进入 `playerState`。
  - `data:` 封面不进入播放器快照/UI state。
  - pause/resume/seek/stop 状态更新。

### 3. 补齐 Media3 官方通知能力

- 在 `android/app/src/main/java/ionic/muses/AudioPlaybackService.kt` 中检查 Media3 1.7.1 可用 API。
- 优先使用 `DefaultMediaNotificationProvider` 或 `MediaSessionService.setMediaNotificationProvider(...)` 配置官方媒体通知。
- 保留合法 foreground bootstrap notification，但避免它成为长期用户可见的普通通知。
- 确认 `MediaItem.MediaMetadata` 包含 title/artist/album。
- 确认系统媒体控制直接控制 `ExoPlayer`，并由 `Player.Listener` 广播状态给前端。
- 视 API 需要限制 unsupported actions（上一首/下一首）。

### 4. Android 配置清理

- 检查 `android/capacitor.settings.gradle`、`android/app/capacitor.build.gradle`，移除 Capgo 插件 project。
- 检查 `AndroidManifest.xml` 不再引入 Capgo service，只保留本项目 `AudioPlaybackService`。
- 如 Media3 官方通知需要额外 AndroidX 依赖，添加官方依赖，不添加第三方媒体插件。

### 5. Spec 和任务文档更新

- 更新 `.trellis/spec/frontend/state-management.md`：把 Capgo 媒体会话规则改为 Media3 原生规则。
- 记录安全边界：Media3 metadata 不包含 WebDAV 密码/Auth/header/base64。

## 验证命令

必须运行：

```bash
npm run test:unit -- --run
npm run lint
npm run build
cd android && ./gradlew :app:compileDebugKotlin
```

## 设备验证清单

- 播放本地歌曲：通知显示标题/歌手，播放/暂停可用。
- 播放 WebDAV 歌曲：通知显示标题/歌手，播放/暂停/seek 可用。
- 锁屏媒体面板可控制同一个播放状态。
- 蓝牙耳机播放/暂停键可控制同一个 ExoPlayer。
- 停止播放后通知消失，不残留服务通知。
- 不出现双通知。
- logcat/localStorage/playerState/muses:songs 不包含 WebDAV 密码或 Basic Auth。

## 回滚点

- 若 Media3 官方通知 API 与当前版本不匹配，先只提交 Capgo 移除以外的安全无争议改动前暂停；不要留下半坏通知。
- 若 foreground service 合规与 Media3 通知接管冲突，保留最小 bootstrap notification，并在任务文档记录设备表现。
- 若系统媒体键无法路由到 session，检查 manifest action `androidx.media3.session.MediaSessionService` 和 session controller command 配置。
