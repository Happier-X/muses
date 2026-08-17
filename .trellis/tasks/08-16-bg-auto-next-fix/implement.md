# 实施计划：后台自动切歌兜底（方案 C）

## 执行顺序（每步可独立验证）

### 1. 原生侧：AudioPlayerPlugin.kt 扩展

- [ ] 1.1 新增 `AutoNextPlan` data class + 字段（autoNextPlan / jsExpectedPlaying / pendingAutoNextAt / Handler）
- [ ] 1.2 新增 `setAutoNext` / `clearAutoNext` / `reportPlaybackStatus` PluginMethod
- [ ] 1.3 新增轮询 `tickAutoNext()`（1s，isMusicActive + 防抖 2.5s）并在 `load()` 启动
- [ ] 1.4 新增 `executeAutoNext()`：assetPath 解析（content:// 拷贝 / webdav 缓存或远程）、`Bridge.callPluginMethod` 调 capgo preload→play→setVolume、旧 asset unload、发 `autoNextStarted`/`autoNextFailed`
- [ ] 1.5 验证：`./gradlew :app:compileDebugKotlin` 通过（android 目录）

### 2. JS 侧：native.ts 扩展

- [ ] 2.1 AudioPlayerBridge 接口扩展（setAutoNext/clearAutoNext/reportPlaybackStatus/autoNextStarted/autoNextFailed）
- [ ] 2.2 新增 `syncCurrentAsset(songId)`（对齐 currentAssetId/currentSongId/状态 + 轮询重启）
- [ ] 2.3 验证：`npm run build` 通过（中途）

### 3. JS 侧：controller.ts 集成

- [ ] 3.1 `registerAutoNextPlan()`（peekNext + buildPlayOptions → setAutoNext；空队列 → clearAutoNext）
- [ ] 3.2 playSongInternal 成功路径注册预案；失败恢复链终止 clearAutoNext；stopPlayback clearAutoNext
- [ ] 3.3 pause/resume/play 上报 reportPlaybackStatus
- [ ] 3.4 queue 变更挂钩（enqueueSongs/removeSongFromQueue/clearQueue/setRepeatMode/toggleShuffle → 重新注册预案）
- [ ] 3.5 playSongInternal 增加 `nativeAlreadyPlaying` 模式（跳过原生 play，其余照常）
- [ ] 3.6 `syncUiToNativeSong(songId)`（syncCurrentAsset + playSongInternal(nativeAlreadyPlaying)）
- [ ] 3.7 initializePlayer 监听 autoNextStarted/autoNextFailed
- [ ] 3.8 回前台对账 `reconcileAfterBackground()` + App.vue appStateChange/visibilitychange 挂钩
- [ ] 3.9 心跳兜底（1s interval，hidden 且 playing 时 getState 检查）
- [ ] 3.10 验证：`npm run lint` + `npm run build` 全绿

### 4. 真机验证（小米 15）

- [ ] 4.1 锁屏播完自动切下一首（本地 + WebDAV 各测）
- [ ] 4.2 回前台 UI/通知栏一致
- [ ] 4.3 前台切歌/暂停/恢复/seek/单曲循环不回归
- [ ] 4.4 队列尾停止、随机模式

## 验证命令

```bash
cd android && ./gradlew :app:compileDebugKotlin
npm run lint
npm run build
```

## 回滚点

- R1：完成 1.x 后——原生代码可独立回滚（git checkout），JS 未动
- R2：完成 3.x 后——全部回滚为单 commit revert；预案/轮询均为旁路逻辑，不阻断正常播放

## 评审门

- G1：1.x + 2.x 完成 → 代码评审（原生桥接正确性）
- G2：3.x 完成 + lint/build 绿 → 真机验证
