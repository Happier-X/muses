# Media3 原生媒体通知

## 目标

把 Android 媒体播放通知从 “ExoPlayer 播放 + Capgo Media Session 插件同步通知控制” 改为 “AndroidX Media3 原生一体化方案”。实际播放、系统媒体通知、锁屏控制、耳机/蓝牙媒体键都由同一个 `AudioPlaybackService`、同一个 `ExoPlayer` 和同一个 `MediaSession` 承接。

## 背景

当前实现已具备：

- `AudioPlaybackService.kt` 继承 `androidx.media3.session.MediaSessionService`。
- 服务内部创建 `ExoPlayer` 和 `MediaSession.Builder(this, nextPlayer).build()`。
- WebDAV 认证、Basic Auth header、缓存下载、播放错误映射都在 Android 原生服务内完成。
- 前端通过自有 Capacitor `AudioPlayer` 插件暴露 `play`、`pause`、`resume`、`stop`、`seek`、`getState`、`stateChange`。
- 近期新增了 `@capgo/capacitor-media-session`，由前端 JS 额外同步 metadata、playbackState、positionState、action handler。

重新评估后，Media3 已同时覆盖播放和系统媒体服务能力，继续保留 Capgo 会引入双 `MediaSession`、双 foreground service、双通知和状态同步复杂度。因此本任务改为移除 Capgo，补齐 Media3 官方媒体通知/会话能力。

## 需求

1. 移除 `@capgo/capacitor-media-session` 依赖和所有相关前端同步封装、调用与测试 mock。
2. 保留自有 `AudioPlayer` Capacitor 插件接口，前端播放器 UI 不直接依赖 Media3 或第三方媒体会话插件。
3. `AudioPlaybackService` 继续负责：
   - 本地音频播放。
   - WebDAV 音频播放。
   - WebDAV Basic Auth header 构造。
   - WebDAV 音频缓存。
   - 播放进度广播。
   - seek、pause、resume、stop。
4. 使用 Media3 官方能力提供标准 Android 媒体通知、锁屏媒体面板和媒体键控制。
5. 系统媒体控制必须驱动同一个 `ExoPlayer`，不要再走 JS action handler 转发。
6. WebDAV 密码、Basic Auth header、SecureStorage 值、认证 URL 仍不得进入 UI state、route、logcat、localStorage、`muses:songs` 或任何第三方插件 payload。
7. 停止播放后通知和 foreground service 必须清理，不残留。
8. 不引入新的第三方媒体会话/通知插件。

## 验收标准

- `package.json` / lockfile 不再包含 `@capgo/capacitor-media-session`。
- `src/features/player/media-session.ts` 被删除，`controller.ts` 不再调用 Capgo media session 同步方法。
- Android 编译通过：`cd android && ./gradlew :app:compileDebugKotlin`。
- 前端验证通过：`npm run test:unit -- --run`、`npm run lint`、`npm run build`。
- 播放本地歌曲时，系统通知/锁屏控制显示当前标题、歌手、播放/暂停/停止、进度/seek 能与播放器状态同步。
- 播放 WebDAV 歌曲时，系统通知/锁屏控制同样可用，且 WebDAV 密码不出现在 logcat、localStorage、`playerState`、`muses:songs`。
- 不出现 Capgo 通知与自有播放服务通知的双通知。
- 停止播放后通知消失，服务资源释放。

## 非目标

- 不重写 WebDAV 浏览、认证保存或缓存策略。
- 不迁移到浏览器 `HTMLAudio` / Howler 等 Web 播放方案。
- 不实现播放队列、下一首/上一首，除非 Media3 默认 UI 必须展示时先禁用或隐藏。
- 不处理 Android Auto、Cast、Wear OS 等扩展媒体平台。

## 待设备验证

- 通知栏媒体样式是否由 Media3 官方通知展示。
- 锁屏媒体面板控制是否工作。
- 蓝牙耳机播放/暂停键是否工作。
- WebDAV 播放时 logcat 不泄漏密码或 Authorization header。
