# 修复媒体播放通知延迟与退出后丢失

## Goal

让 Android 媒体播放通知及时稳定展示，并在用户退出/切到后台后仍能继续展示与控制（只要还在播放或暂停中）。

## Background / Confirmed Facts

### 当前架构

- 播放引擎：`@capgo/capacitor-native-audio`（`showNotification: false`）
- 媒体通知：`@capgo/capacitor-media-session`（`MediaSessionService` + `MediaStyle`）
- 业务同步：`src/features/player/controller.ts` → `src/features/player/mediaSession.ts`
- 返回键：`src/App.vue` 在 Tab 层调用 `App.exitApp()`
- 规范：`.trellis/spec/frontend/features-player.md`

### 根因

| 现象 | 根因 |
|------|------|
| 通知延迟/闪断 | `loading` 等状态被映射为 `none`，插件因此停/启前台服务；封面 `data:` 转换阻塞首帧 metadata |
| 退出后通知消失 | `App.exitApp()` 销毁 Activity → media-session `bindService` 解绑 → `onUnbind()` 直接 `destroy()` 停前台服务 |

### 相关历史

- media3 通知链路已弃用；当前策略是 native-audio 播 + media-session 通知
- 返回键历史任务 `07-09-back-button-exit` 曾要求 Tab 页 `exitApp()`；本任务 **D1 覆盖该语义**：播放场景下退出界面不等于杀进程

## Decisions

- **D1（退出语义）**：返回键离开应用界面 = 仅退到后台，**不停止播放**；播放/暂停中保留媒体通知与控制。真正 stop 或自然结束且无下一首时才清通知。
- **D2（最近任务）**：MVP 只保证返回键 / Home / 切后台后继续；从最近任务划掉后允许随进程结束。
- **D3（实现路线）**：优先在应用侧修，不 fork `node_modules/@capgo/*`。
  - 退出：Tab 返回改用 `App.minimizeApp()`（`moveTaskToBack`），避免销毁 Activity 导致 unbind destroy。
  - 延迟：修正 playback 状态映射 + metadata 文字先上、封面后补。

## Requirements

### R1. 通知及时出现

- 开始播放后尽快展示媒体通知，不依赖封面转换完成
- 切歌/loading 过程中不得把通知刷成 `none` 导致闪断
- 元数据更新：标题/艺人/专辑先上，封面异步补

### R2. 退出/后台后通知仍在

- 返回键退出界面后，播放或暂停中通知仍在
- 通知按钮 play/pause/prev/next/stop 在后台仍可用
- 真正 stop 或结束且无下一首时通知消失

### R3. 不回归

- 本地/WebDAV 播放、队列切歌保持可用
- 禁止双通知 / 双 MediaSession provider
- 禁止敏感信息进入 metadata

## Acceptance Criteria

- [ ] AC1：点击播放后，通知尽快出现，不依赖封面就绪
- [ ] AC2：切歌时通知不闪断；标题/艺人先更新，封面可稍后补上
- [ ] AC3：返回键退出界面后，若仍在播放/暂停，通知仍在
- [ ] AC4：退出后台后通知按钮仍可控制播放
- [ ] AC5：真正停止后通知正确消失、无残留
- [ ] AC6：无双通知、通知 ID 冲突、前台服务崩溃
- [ ] AC7：本地与 WebDAV 音源均通过
- [x] AC8：`npm run build` 与 Android 编译通过

## Out of Scope

- iOS Now Playing 专项
- 完整 media3 播放服务重写
- 自定义通知布局 / 桌面小组件
- 划掉最近任务后的强保活

## Technical Notes（摘要，细节见 design.md）

- `App.vue`：`exitApp()` → `minimizeApp()`
- `mediaSession.ts`：`loading` 保持 active 会话态；`setMetadata` 拆分文字/封面
- 不修改 capgo 插件源码；不启用 native-audio 的 `showNotification`
