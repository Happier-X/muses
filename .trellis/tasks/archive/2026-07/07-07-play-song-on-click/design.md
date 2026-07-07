# 点击播放歌曲设计

## 架构边界

本任务实现应用级播放器能力：歌曲页点击播放、跨页面常驻迷你播放器、Android 后台播放和通知栏基础控制。不引入 Pinia/Vuex，不做播放队列、上一首/下一首、进度拖动、歌词、封面图或 iOS 后台播放。

拟新增/扩展：

- `src/features/player/types.ts`：播放源、播放状态、错误类型。
- `src/features/player/native.ts`：声明原生播放器 Capacitor 插件接口和事件。
- `src/features/player/store.ts` 或 `controller.ts`：feature-local 最小响应式播放器状态，负责根据 `SongItem` 解析本地/WebDAV 播放参数、调用原生插件、响应原生事件。
- `src/components/MiniPlayer.vue`：跨页面常驻迷你播放器。
- `src/App.vue`：在 `ion-router-outlet` 外渲染 `MiniPlayer`。
- `src/views/SongsPage.vue`：歌曲条目点击播放，高亮当前歌曲。
- `android/app/src/main/java/ionic/muses/AudioPlayerPlugin.kt`：Capacitor 插件，连接前端和 Android 播放服务。
- `android/app/src/main/java/ionic/muses/AudioPlaybackService.kt`：基于 AndroidX Media3 的 `MediaSessionService`，持有 ExoPlayer 和媒体通知。
- `android/app/build.gradle`：新增 Media3 依赖。
- `android/app/src/main/AndroidManifest.xml`：声明前台服务、媒体播放权限和服务。

## 播放数据流

1. 用户点击歌曲页某个 `SongItem`。
2. 前端 player controller 根据 `sourceType` 构造播放请求：
   - `local`：传入 `uri`、标题、艺术家等 metadata。
   - `webdav`：通过 `sourceId` 找到对应 WebDAV 音源，使用 `credentialKey` 从 SecureStorage 读取密码，只将 URL、用户名、密码传给原生播放边界。
3. `AudioPlayerPlugin.play(...)` 将请求转发给 `AudioPlaybackService`。
4. Android 服务用 Media3/ExoPlayer 停止当前媒体项，设置新媒体项并开始播放：
   - local：使用 `content://` URI。
   - webdav：使用带 Basic Auth Header 的 `MediaItem` / data source 配置。
5. MediaSessionService 负责后台播放、媒体通知和通知栏播放/暂停/停止控制。
6. 原生层向前端发送状态事件：playing、paused、stopped、error、currentSongId 等。
7. 前端全局 mini player 响应状态更新，显示当前歌曲和控制按钮。

## 原生插件接口草案

```ts
interface AudioPlayerNativePlugin {
  play(options: LocalPlayOptions | WebDavPlayOptions): Promise<void>
  pause(): Promise<void>
  resume(): Promise<void>
  stop(): Promise<void>
  getState(): Promise<AudioPlayerNativeState>
  addListener('stateChange', listener: (state: AudioPlayerNativeState) => void): Promise<PluginListenerHandle>
}

interface LocalPlayOptions {
  sourceType: 'local'
  songId: string
  uri: string
  title: string
  artist?: string
  album?: string
}

interface WebDavPlayOptions {
  sourceType: 'webdav'
  songId: string
  url: string
  username: string
  password: string
  title: string
  artist?: string
  album?: string
}
```

密码只作为播放调用参数进入原生边界，不写入任何持久化数据或 UI 状态。`AudioPlayerNativeState` 不允许包含密码或 Authorization Header。

## UI 设计

- 歌曲列表项使用 `button` 或点击 handler，让用户明确可点。
- 当前播放歌曲高亮或显示“正在播放”。
- `MiniPlayer` 常驻在 app shell 中，展示歌曲标题、艺术家、播放/暂停按钮、停止/关闭按钮、错误提示。
- 迷你播放器不阻挡底部 tab bar，必要时给主内容增加底部 padding。
- 通知栏展示基础媒体通知，至少支持播放/暂停/停止。

## 兼容性与安全

- 本地 `content://` URI 必须由 Android 原生层播放。
- WebDAV URL 不能在前端 `<audio>` 中直接播放；认证只通过 SecureStorage -> 原生边界流转。
- Android 13+ 可能需要通知权限；如果未授权，后台播放仍应尽量通过前台服务通知要求给出可理解失败或降级提示。
- 前台服务需要声明媒体播放类型，避免后台播放被系统杀掉。
- 播放失败要转换成用户可见错误，不泄露密码或 Authorization Header。

## 依赖选择

推荐使用 AndroidX Media3：

- `androidx.media3:media3-exoplayer`
- `androidx.media3:media3-session`
- `androidx.media3:media3-ui`（仅当需要通知/辅助组件时）
- 如需自定义 OkHttp data source：`androidx.media3:media3-datasource-okhttp`

相较裸 `MediaPlayer`，Media3 更适合后台播放、媒体通知、媒体会话和远程 URL header。

## 回滚形态

- 前端 player feature 独立在 `src/features/player/`，全局 UI 独立在 `MiniPlayer.vue`。
- Android 播放服务和插件独立新增，Manifest/Gradle 改动可集中回滚。
- 歌曲页点击播放改动集中在 `SongsPage.vue`。
