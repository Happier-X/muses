# 修复：应用后台时不自动播放下一首

## Goal

修复应用退到后台（切后台 / 锁屏 / 熄屏）时，当前曲播完后不自动切下一首的问题，保证后台/锁屏场景下队列连续播放。

## Background（根因分析）

### 播放架构

- 播放由原生 `@capgo/capacitor-native-audio` 执行（本地 file:// 走 AudioAsset/SoundPool，WebDAV 远程走 RemoteAudioAsset/ExoPlayer）。
- 切歌链路完全依赖 WebView JS：原生 complete 事件 → JS `NativeAudio.addListener('complete')` → `finished` 状态 → `handlePlaybackFinished()` → `advanceToNext()` + `playSong(next)` → 原生 preload/play。
- 原生 complete 检测不依赖 JS（主线程 Handler 100ms 轮询 / ExoPlayer 回调），**事件一定能从原生发出**。
- Capacitor 事件投递走 `WebView.evaluateJavascript`，**必须由 WebView 渲染进程执行 JS 才能被处理**。
- 前台服务由 `@capgo/capacitor-media-session` 提供（播放中 `startForeground` mediaPlayback），进程级保活无问题。

### 根因

应用退后台/锁屏后，WebView 页面不可见（`visibilityState=hidden`），Chromium 对不可见 WebView 页面：
- 节流：JS timer 降至 ~1 次/分钟（position 500ms 轮询实际失效）；
- 冻结：满足条件（无媒体活动、无连接等）时进入 Page Lifecycle `frozen`，**timers、事件回调、fetch 全部挂起**。

本项目音频在原生播放器播放，WebView 页面自身没有任何媒体活动，符合冻结条件。因此：

1. 当前曲播完 → 原生发出 complete 事件 → `evaluateJavascript` 投递；
2. WebView JS 冻结/节流 → complete 事件**无法被处理**（挂起或丢失）；
3. 前端无任何兜底（position 轮询只更新进度不检测播完；无回前台对账）→ 下一首永远不启动；
4. 回前台后事件可能积压执行（表现为"突然切歌"）或丢失（UI 停在已播完曲目）。

## 候选方案

### 方案 A：原生侧自动切歌兜底（彻底，推荐主方案）

**不 patch 第三方插件**，用 Capacitor 原生桥接实现：

- JS 每次播放成功时把"下一首预案"（songId/assetId/assetPath/认证/volume 等）注册到本项目原生插件 `AudioPlayerPlugin`（新 API `setAutoNext`）；
- 原生插件后台轮询（1s）：当 `AudioManager.isMusicActive() == false` 且 JS 期望播放状态为 playing（JS 通过 `reportPlaybackStatus` 上报）时，判定为"播完未切"；防抖 ~2.5s 后通过 `Bridge.callPluginMethod("NativeAudio", "preload"/"play")` 直接驱动 capgo 插件播放预案曲；
- 若期间 JS 正常切歌（isMusicActive 恢复 / 上报状态变化 / 预案被更新）则取消自动播放，互不冲突；
- 自动播放成功发出 `autoNextStarted` 事件，JS 恢复执行后据此同步 UI/媒体会话。

优点：锁屏/后台无缝切歌（等同原生音乐 App 体验），不动 node_modules、插件升级无痛。
缺点：依赖 `Bridge.callPluginMethod`（稳定公共 API）与 `AudioManager.isMusicActive()`（标准 API）；原生/JS 状态协调复杂度较高。

### 方案 B：JS 心跳轮询 + 回前台对账（轻量）

- 全局 1s 心跳：`document.hidden` 且应播放时调 `getState()`，发现原生已停止（播完未切）→ 立即触发 `handlePlaybackFinished()`；
- 后台 timer 节流后实际约 1 次/分钟 → 静音窗口最长 ~1 分钟；
- `App.addListener('appStateChange')` 回前台立即对账切歌。

优点：纯 JS 改动、风险低。缺点：锁屏场景最长 1 分钟静音，体验打折。

### 方案 C：A + B 组合（用户已选定）

原生兜底保证后台无缝切歌；回前台对账兜底事件丢失/原生自动播放后的状态同步。

## 复现信息（用户提供）

- 机型：小米 15（Android 15，国产 ROM）
- 场景：锁屏时当前曲播完不自动切下一首
- 播放源/模式：未单独说明 → 按本地 + WebDAV、列表/单曲/随机全场景设计

## 插件版本核查

- 当前 `@capgo/capacitor-native-audio` 8.4.19（2026-08-03 发布）已是 npm latest，无需升级；本项目无法通过升级获得后台自动切歌能力，故采用方案 C 自行实现。

## Acceptance Criteria

- [ ] 前台播放自然结束后仍自动切下一首（不回归）
- [ ] 锁屏/退后台后当前曲播完自动切下一首，无需回前台（小米 15 验证）
- [ ] 原生自动切歌后回前台：UI/队列/媒体会话与真实播放一致
- [ ] 手动切歌、暂停/恢复、seek 不回归
- [ ] 单曲循环 / 列表循环 / 随机模式行为正确
- [ ] 本地文件与 WebDAV 播放源均覆盖
- [ ] 预案播放失败有兜底（回前台恢复链/错误状态正确）
- [ ] lint / build 全绿

## Out of Scope

- 发版 / 升 version
- 队列跨设备同步
- iOS 端（如适用另行评估）
