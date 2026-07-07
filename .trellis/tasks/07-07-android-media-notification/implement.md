# 安卓媒体通知控制实施计划

## 前置决策

- 使用插件方案：`@capgo/capacitor-media-session@8.0.28`。
- 插件只负责系统媒体会话/媒体通知 metadata、position 和 action handler。
- 当前原生 `AudioPlayer`、`AudioPlaybackService`、ExoPlayer、WebDAV 认证与缓存继续负责实际播放。

## 实施步骤

### 1. 引入插件与类型封装

- 安装 `@capgo/capacitor-media-session@8.0.28`。
- 新增 `src/features/player/media-session.ts` 或同等模块，集中封装 Capgo API。
- 封装层导出：
  - `initializeMediaSessionControls()`
  - `syncMediaSessionState()`
  - `clearMediaSessionState()`
- 封装层过滤敏感值：不允许 `password`、Auth header、`data:` artwork、WebDAV 认证 URL 进入插件 payload。

### 2. 与播放器控制器集成

- 在 `player/controller.ts` 中初始化媒体 action handlers。
- `playSong`、`pausePlayback`、`resumePlayback`、`stopPlayback`、`seekPlayback` 和 native `stateChange` 后同步媒体会话状态。
- Capgo action handler 映射：
  - `play` -> `resumePlayback()`
  - `pause` -> `pausePlayback()`
  - `stop` -> `stopPlayback()`
  - `seekto` -> `seekPlayback(...)`
- 同步 metadata：标题、歌手、专辑、可安全展示的封面。
- 同步 position：duration > 0 时更新 position/duration/playbackRate。

### 3. Android 通知冲突处理

- 运行 `npx cap sync android` 后检查 manifest 合并和插件服务。
- 检查播放时是否出现 Capgo 媒体通知 + 当前 `AudioPlaybackService` 手写通知双通知。
- 若双通知出现：
  - 优先降低或移除当前手写媒体控制 UI，只保留满足 foreground service 的必要最小通知。
  - 确保停止播放时两个服务/通知均可清理。
- 不把 WebDAV 播放能力迁入 Capgo 服务。

### 4. 测试覆盖

- 更新/新增 unit tests：
  - 媒体会话封装只发送非敏感 metadata。
  - `data:` coverUri 被过滤。
  - WebDAV password 不出现在 Capgo payload、localStorage、playerState。
  - action handler 调用现有 `pausePlayback`/`resumePlayback`/`stopPlayback`/`seekPlayback`。
  - 播放进度同步调用 `setPositionState`。
- 保留现有播放器和 WebDAV 测试。

### 5. 验证命令

必须运行：

```bash
npm run test:unit -- --run
npm run lint
npm run build
cd android && ./gradlew :app:compileDebugKotlin
```

设备/模拟器手动验证：

- 本地歌曲播放通知显示系统媒体样式。
- WebDAV 歌曲播放通知显示系统媒体样式，且缓存命中/缓存 miss 均可播放。
- 通知播放、暂停、停止、seek 操作能同步 mini player 和 `/player`。
- 停止后通知消失，不残留。
- 不出现两个可见播放通知；如果平台限制导致无法避免，记录证据并回到设计评审。
- logcat 不包含 WebDAV 密码、Basic Auth header。

## 回滚点

- 如果插件引入导致编译失败或 manifest 冲突：移除依赖和封装层，回到原 Media3 原生通知方案设计。
- 如果无法避免双通知：保留 PRD 和调研结果，暂停实现，要求重新确认是否改用原生 Media3 官方媒体通知。
- 如果插件 action handler 无法可靠驱动当前播放器：只保留 metadata/position 同步，通知控制回退到当前原生服务或重新评审。
