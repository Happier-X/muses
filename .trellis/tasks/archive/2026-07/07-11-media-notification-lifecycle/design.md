# Design — 媒体通知延迟与退出后丢失

## 目标与边界

在 **不 fork `@capgo/*`、不恢复 media3 通知链路** 的前提下，修复：

1. 通知展示延迟 / 切歌闪断
2. 返回键退出界面后通知与后台控制丢失

不处理：划掉最近任务后的强保活、iOS、自定义通知 UI。

## 方案总览

```
[业务 controller]
    │  play/pause/seek/queue
    ▼
[mediaSession.ts] ──► @capgo/capacitor-media-session ──► MediaSessionService(FGS)
    │                    setMetadata / setPlaybackState / setPositionState
    │
[native.ts] ─────────► @capgo/capacitor-native-audio（showNotification:false，只负责播）

[App.vue backButton]
    │  Tab 层
    ▼
App.minimizeApp()  ──► Activity.moveTaskToBack  ──► 进程与 FGS 保持，通知不因 unbind destroy 消失
```

## 变更 1：退出界面不销毁 Activity

### 问题

`App.exitApp()` 结束 Activity → Capacitor 解绑 media-session → 插件 `MediaSessionService.onUnbind()` 调 `destroy()` → 前台服务与通知一起没。

即便音频线程仍在，**通知生命周期绑在了 Activity 上**。

### 方案

`src/App.vue` 返回键在关闭 overlay 后，将：

```ts
App.exitApp()
```

改为：

```ts
void App.minimizeApp()
```

`minimizeApp` 在 Android 上等价 `moveTaskToBack(true)`：

- 界面退到后台（用户感知为“退出应用”）
- Activity 进入 stop，**不 destroy**
- media-session 的 bind 仍在；`handleOnStop` 仅在 playback 非 active 时才 stop 服务
- 播放中/暂停中通知继续；按钮回调仍可走 JS bridge

### 与历史任务的关系

`07-09-back-button-exit` 曾要求 Tab 页 `exitApp()`。本任务 **D1 显式覆盖**：音乐 App 下“退出界面 ≠ 杀进程”。overlay 关闭逻辑不变。

### 不采用的方案

| 方案 | 为何不做（MVP） |
|------|----------------|
| 改 node_modules 里 `onUnbind` 不 destroy | 禁止改 capgo 源码，升级会被覆盖 |
| 自建独立 MediaSessionService 替代插件 | 范围过大，回归 media3 时代复杂度 |
| `foregroundService: "always"` 单独配置 | 不能阻止 exitApp 后 unbind destroy |

## 变更 2：消除通知延迟 / 闪断

### 2.1 状态映射

现状：`loading/idle/stopped/finished/error` → `none`。

切歌时 controller 会先 `loading`，插件把服务判为 inactive 并 stop，再在 `playing` 时重新 startForegroundService，表现为延迟/闪断。

调整 `updateMediaSessionPlayback`：

| PlaybackStatus | MediaSession state |
|----------------|--------------------|
| `playing` | `playing` |
| `paused` | `paused` |
| `loading` | **保持 active**：映射为 `playing`（乐观，避免服务 teardown） |
| `finished` | **保持 active**：映射为 `playing`（队列自动下一首前的短暂窗口） |
| `idle` / `stopped` / `error` | `none`（由 clear/stop 路径清理） |

`clearMediaSession` 仍只在无 currentSong / 真正 stop 时调用。

### 2.2 Metadata 分两段

`updateMediaSessionMetadata`：

1. **立即** `setMetadata({ title, artist, album, artwork: undefined 或旧封面策略 })`，不等待封面
2. **异步** `prepareArtworkDataUrl` 成功后再二次 `setMetadata` 带 artwork

避免大图 base64 阻塞首帧通知。

### 2.3 调用时序（playSong）

保持现有 `syncMediaSessionSong` 在 native play 前调用，但因 2.1/2.2，loading 阶段已能维持/启动通知。

不在此任务打开 native-audio `showNotification`（规范禁止双 provider）。

## 数据流（退出后控制）

```
用户按返回 → minimizeApp → Activity onStop
  media-session handleOnStop: playback active → 不 stop 服务
用户点通知暂停 → MediaButtonReceiver → MediaSessionCallback → JS setActionHandler
  → pausePlayback → NativeAudio.pause + setPlaybackState(paused)
```

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 部分机型 minimize 后 WebView 被挂起，按钮回调慢/失效 | 真机验证；若失效再评估原生侧转发（超出 MVP 再开任务） |
| loading 映射 playing 导致短暂图标不准 | 可接受；playing 事件很快覆盖 |
| 二次 setMetadata 竞态（快速切歌） | 用 songId/token 丢弃过期封面回调 |
| 用户以为“退出=停止” | D1 产品确认继续播；stop 按钮/通知 stop 仍可停 |

## 回滚

- `App.vue` 改回 `exitApp()` 即恢复旧退出行为
- `mediaSession.ts` 状态映射与 metadata 拆分可独立回滚
- 无 DB/存储迁移

## 文件影响

| 文件 | 变更 |
|------|------|
| `src/App.vue` | `exitApp` → `minimizeApp` |
| `src/features/player/mediaSession.ts` | 状态映射 + metadata 分发 + 封面 token |
| `.trellis/spec/frontend/features-player.md` | 实现后补生命周期约定 |
| 可选：`capacitor.config.ts` | 仅当需要文档化 MediaSession 配置；默认不强制 always |
