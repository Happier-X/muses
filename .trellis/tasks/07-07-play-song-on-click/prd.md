# 点击播放歌曲

## 目标

在歌曲页点击歌曲时开始播放该歌曲，并提供跨页面常驻迷你播放器、后台播放和 Android 通知栏控制。播放器负责根据歌曲的 `uri` 和 `sourceType` 发起播放，支持本地 `content://` URI 和 WebDAV 带认证 URL。

## 背景与已确认事实

- 歌曲页 `src/views/SongsPage.vue:1` 已展示歌曲列表，每条歌曲渲染为一个 `ion-item`，当前不可点击。
- 根组件 `src/App.vue:1` 目前只渲染 `ion-router-outlet`，没有全局播放器区域。
- 歌曲模型 `SongItem` 已包含 `uri`（本地为 `content://` URI，WebDAV 为完整 URL）和 `sourceType`。
- 本地音源的 URI 是 Android SAF `content://` URI，不能直接被浏览器 `<audio>` 或 WebView 播放。
- WebDAV 音源的 URI 是完整服务器 URL，需要携带 Basic Auth 才能访问。
- 当前 `LocalLibraryNative` 插件只提供扫描和元数据读取，没有播放能力。
- 当前 `WebDavNative` 插件提供 `PROPFIND` 和 `readMetadata`，没有 HTTP 流媒体播放入口。
- Android 目前已依赖 OkHttp 和 DocumentFile，但还没有 Media3/ExoPlayer 依赖。
- Android Manifest 当前只有 `INTERNET` 权限，没有前台服务/媒体播放服务声明。
- 项目没有音频播放器组件、播放状态管理或原生媒体播放插件。
- 项目没有全局状态库；规范要求用最小状态机制。

## 需求

- R1：歌曲页每首歌可点击，点击后开始播放该歌曲。
- R2：播放器能同时支持本地 `content://` URI 和 WebDAV 带认证 URL。
- R3：应用内提供跨页面常驻迷你播放器，展示当前播放歌曲标题和播放/暂停控制。
- R4：支持后台播放；用户离开应用或锁屏后，音频可继续播放。
- R5：Android 通知栏提供基础媒体控制，至少支持播放/暂停和停止。
- R6：WebDAV 播放时密码必须从 SecureStorage 读取，不写入 `localStorage`、歌曲库、日志或 UI 状态。
- R7：点击另一首歌时，当前播放停止并从新歌曲开始播放。
- R8：播放失败时向用户展示提示，不崩溃，不泄露认证信息。
- R9：不引入 Pinia/Vuex；若需要跨页面播放器状态，使用 feature-local 的最小响应式状态或 composable。

## 验收标准

- [ ] 歌曲页 `ion-item` 可点击触发播放。
- [ ] 任意 tab 页面都能看到迷你播放器，展示当前歌曲标题和播放/暂停按钮。
- [ ] 本地歌曲（`content://` URI）能通过原生能力播放。
- [ ] WebDAV 歌曲能通过带认证的原生能力播放，且密码不进入 `localStorage`、歌曲库、日志或 UI 状态。
- [ ] 点击另一首歌切换播放，不残留旧播放。
- [ ] 应用进入后台后音频可继续播放。
- [ ] Android 通知栏出现媒体播放通知，并可控制播放/暂停/停止。
- [ ] 播放失败时有用户可见反馈。
- [ ] 单元测试覆盖播放状态变化、本地/WebDAV 播放调用边界、密码不入库和跨页面迷你播放器渲染。

## 技术边界

- 优先使用 AndroidX Media3/ExoPlayer + MediaSession/MediaSessionService 作为 Android 播放后端，避免裸 `MediaPlayer` 难以支持通知栏与后台控制。
- 不在前端引入 `<audio>` 元素或 JS 音频解码方案，因为本地 `content://` URI 和 WebDAV CORS 都不适合 WebView 播放。
- 跨页面迷你播放器可放在 `src/App.vue` 或路由外层组件中；播放器状态放在 `src/features/player/` 的最小响应式模块中。
- 本任务不实现播放队列、上一首/下一首、进度拖动、歌词、封面图、音频焦点复杂策略或多平台 iOS 后台播放。

## 已决策问题

- Q1：播放控制 UI 需要跨页面常驻，并且需要支持后台播放和 Android 通知栏控制。
- Q2：通知栏本轮只需要播放/暂停/停止，不做上一首/下一首，不引入播放队列。

## 待决问题

- 无。
