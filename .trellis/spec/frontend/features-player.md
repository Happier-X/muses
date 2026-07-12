# 特征·播放器 — 开发规范

> 本项目的音频播放、系统媒体通知、媒体会话体系在 `src/features/player/` 内统一管理。

---

## 范围/触发条件

涉及音频播放（本地、WebDAV）、通知栏媒体卡片、系统媒体会话（MediaSession / 媒体按键）的任何改动，都应在本规范约束下进行。

---

## 基础架构（战后记录）

### 前端播放器封装

- 业务层通过 `src/features/player/controller.ts` 调用统一的 `AudioPlayerNative` 抽象接口（`native.ts` 导出）。
- **永远不要**在 controller 层直接引入或调用原生播放器插件（Capacitor Plugin / NativeAudio）；播放器插件只存在于 `native.ts` 的封装层。
- `AudioPlayerNative` 现在底层实际使用 `@capgo/capacitor-native-audio`（播放引擎）和 `@capgo/capacitor-media-session`（系统媒体通知与按键映射）两个 Capacitor 插件。

### 通知架构（整理版）

以下是在本任务中加密的事实，未来任何媒体通知改动都不能绕开：

1. **不再使用 media3 `MediaSessionService`**  
   该项目历史上曾尝试使用 media3 的 `MediaSessionService + DefaultMediaNotificationProvider` 机制，但在自定义 `ACTION_PLAY` Intent 通道下，`DefaultMediaNotificationProvider` 的 `shouldShowNotification` 条件一直未完全满足，导致通知无法稳定显示官方媒体卡片。  
   **因此**：不再使用 media3 的 MediaSessionService 作为媒体通知创建者。

2. **`@capgo/capacitor-native-audio` 负责播放**  
   配置文件内 `showNotification: false`（因为通知已由 media-session 单独管理）。  
   配置内容：`focus: true`、`background: true`、`backgroundPlayback: true`。

3. **`@capgo/capacitor-media-session` 负责通知**  
   - 通知栏使用 `MediaStyle + MediaSessionCompat`，官方模板 `Notification$MediaStyle`。
   - 必须注册以下 action handler：
     - `play` → `resumePlayback`
     - `pause` → `pausePlayback`
     - `stop` → `stopPlayback`
     - `previoustrack` → `playPreviousFromQueue`
     - `nexttrack` → `playNextFromQueue`
   - 每次播放状态、进度变化后，必须同步到 `setPlaybackState` / `setPositionState`。
   - song 切换时，同步 `setMetadata`（title/artist/album/cover）。
   - **`loading`/`finished` 不得映射为 `none`**：应保持 active（当前实现映射为 `playing`），否则插件会 stop 前台服务再重建，造成通知延迟/闪断（含队列自动下一首窗口）。
   - **metadata 两段式更新**：先推 title/artist/album + **占位清空 artwork**（1×1 中性 JPEG `data:`），封面经 `prepareArtworkDataUrl` 转 `data:` 后二次 `setMetadata`；用 token 丢弃过期封面回调。
   - **空 `artwork: []` 不能清封面**：capgo Android 插件仅在 `artwork[].src` 非空时才 `urlToBitmap`；传入空数组会**保留上一首 Bitmap**。无封面 / prepare 失败 / clear 时必须用显式 `data:` 占位图强制覆盖。
   - **懒扫描补全封面后必须 re-sync**：`scanSongMetadata` → `syncDisplayStateFromSong` 在当前曲 `coverUri`/title/artist/album 变化时须再调 `syncMediaSessionSong`，不能只更新 UI。
   - **返回键退出界面 = `App.minimizeApp()`**（`moveTaskToBack`），禁止用 `App.exitApp()` 作为 Tab 层返回默认行为；`exitApp` 会 destroy Activity → media-session unbind → 前台服务与通知一并消失。

4. **封面兼容（file:// 无法直接使用）**  
   当前 `@capgo/capacitor-media-session` 只接受 `http://` 或 `data:` URI 作为 artwork。所以遇到 `file://` 封面时，需要通过原生桥接（`AudioPlayerPlugin.prepareArtworkDataUrl`）转换为 `data:image/jpeg;base64,...` 再传给 `setMetadata.artwork`。  
   `prepareArtworkDataUrl` 对 `file://` 优先 `FileInputStream`，`content://` 走 `ContentResolver`；失败 resolve `dataUrl=null`，由前端占位图清空旧封面。

5. **Android 13+ 运行时权限**  
   首次播放前仍然需要通过 `AudioPlayerBridge.ensureNotificationPermission()` 请求 `POST_NOTIFICATIONS`，并在授权失败时静默忽略，不阻塞播放。

6. **前台服务/通知 ID 冲突**  
   capacitor-media-session 使用自己的通知 ID（`id=1, channel=playback`），但我们的旧 `AudioPlaybackService`（当前已清理为空服务）不再产生第二个媒体通知，避免前台服务通知冲突。

7. **finished 自动切歌必须判定自然结束**（`controller.ts`）  
   - 进度条 / 歌词点击 / 媒体会话 `seekto` 均走 `seekPlayback`；成功后记录 `lastSeekAt`（保护窗约 1500ms）。
   - 仅当「不在 seek 保护窗」且「`duration > 0` 且 `position >= duration - epsilon`（epsilon≈1.25s）」时，才把 `finished` 当作自然播完并 `handlePlaybackFinished` → `advanceToNext`。
   - **非自然结束的 finished 不得 advance**：保护窗内或未接近结尾（含 `duration=0`）时丢弃伪 finished，恢复 `playing`/`paused`，保留 `currentSong` 与 seek 目标进度。
   - seek 保护窗优先于 near-end：即便 seek 到最后 1s 歌词，保护窗内 finished 也不切歌。
   - `playSong` / `stopPlayback` 必须清理 seek guard，避免新歌首帧误吞真实 finished 或卡住队列。
   - 不修改 capgo 插件源码；远程/未缓冲 seek 触发的 `STATE_ENDED`/`complete` 在前端边界消化。

8. **已缓冲进度与 seek 限制**（`bufferedPosition`）  
   - `PlayerState.bufferedPosition: number | null`：秒；`null` = 缓冲未知（不画假缓冲条，seek 退化为 duration clamp）。
   - **本地就绪**：`prepareLocalAudioFile` 完成后原生上报 `fullyBuffered`，前端 `bufferedPosition = duration`，缓冲条铺满，可全长 seek。
   - **WebDAV/远程**：`AudioPlayerPlugin.prepareWebDavAudioFile` 渐进下载到 `WebDavAudioCache` 同 key 缓存文件；达到可播阈值（约 256KB）即返回 `file://` 交给现有 `NativeAudio.play`（**非整首下完才播**）；下载进度经 `bufferProgress` 事件换算 `bufferedPosition`。
   - **seek 上限**：缓冲已知时 `min(duration, bufferedPosition)`；目标越界时 **拒绝 seek**（返回 `false`），不发起原生跳转。歌词点击共用 `seekPlayback`。
   - **切歌 / stop / 播放失败** 必须 `resetBufferState()`，禁止串曲缓冲。
   - 不改 `node_modules/@capgo/*`；缓冲由项目自有 `AudioPlayer` 插件上报，经 `native.ts` 合并进 `stateChange`。
   - 保留 seek 保护窗 + 自然结尾判定作为第二道防线。

---

## 约束与禁止模式

- **禁止**在除 `native.ts` 之外的任何文件直接调用 `NativeAudio.*` 或 `MediaSession.*`。
- **禁止**同时使用多个 notification provider（native-audio 的 showNotification 和 media-session 的通知只能开一个；当前我们只使用 media-session）。
- **禁止**在 `native.ts` 中搞双向依赖（目前 `mediaSession.ts` 和 `native.ts` 是解耦的；`mediaSession.ts` 仅 import `AudioPlayerBridge` 用于封面转换桥接）。
- **禁止**修改 `node_modules/@capgo/*` 源码（我们只修复了 manifest 中 `MediaButtonReceiver` 的缺失，这是 app 侧修正，不是插件修改）。
- **禁止**对任意 `finished`/`complete` 无条件 `advanceToNext`；必须经过 seek 保护窗 + 接近自然结尾校验。
- **禁止**在缓冲已知时把 seek 目标落到 `bufferedPosition` 之外（进度条与歌词均须拒绝或 clamp 到已缓冲终点）。
- **禁止**远程强制「整首下载完成才开始播放」；渐进下载达到可播阈值即 `file://` 起播。
- **禁止**缓冲未知时画假缓冲条；`bufferedPosition === null` 时 UI 不设 `--buffered`。

---

## 测试要点

- 本地音源播放→通知出现 → 封面 / 标题 / 上一曲 / 下一曲 可用
- WebDAV 音源播放→通知出现 → 封面 / 标题，Basic Auth headers 通过 NativeAudio 的 `headers` 送达
- 暂停/停止→通知同步出成 `none` 状态
- 队列自动下一首→通知立即刷新为新歌曲
- 有封面 A → 有封面 B：最终带 artwork 的 `setMetadata` 使用 B
- 有封面 → 无封面：最终用占位 `data:` 覆盖，不残留 A
- 开播后懒扫描写入 `coverUri` 会再次 `setMetadata`
- 快速切歌时过期 token 丢弃旧封面回调
- seek 后立刻注入 finished → 不切歌、保留 currentSong
- 接近结尾的 finished → 仍自动下一曲
- 歌词点击 seek 与进度条 seek 共用同一保护逻辑
- `duration=0` 的 finished 不自动 advance
- 缓冲已知时拖到未缓冲区 → 不调用原生 seek
- 歌词点击未缓冲时间码 → 不 seek
- 本地 full buffer → 可全长 seek
- 切歌 / stop 后 `bufferedPosition` 重置为 null
- 缓冲增长单调合并；回退上报不得拉低
- 缓冲未知时 seek 仍按 duration clamp

---

## 常见错误

- **manifest 中缺少 MediaButtonReceiver 声明**：导致通知栏按钮显示但点击没反应  
  修复：在 `AndroidManifest.xml` 的 `<application>` 中添加形如  
  `<receiver android:name="androidx.media.session.MediaButtonReceiver" android:exported="true"> ... </receiver>`。
- **Tab 返回键调用 `App.exitApp()`**：Activity destroy → media-session unbind destroy → 播放中通知消失  
  修复：改用 `App.minimizeApp()`，仅退到后台。
- **`loading` 映射为 media-session `none`**：切歌时通知闪断/延迟  
  修复：loading 保持 active（`playing`），仅 stop/clear 时置 `none`。
- **`setMetadata` 同步等待封面 base64**：首帧通知被大图转换拖慢  
  修复：文字先上、封面后补。
- **`artwork: []` 切到无封面歌曲仍显示上一首封面**  
  修复：首帧与 clear 路径一律用 1×1 中性 JPEG `data:` 强制覆盖；prepare 失败也保留占位清空。
- **懒扫描补到封面后通知栏不更新**  
  修复：`syncDisplayStateFromSong` 检测 cover/title/artist/album 变化后调用 `syncMediaSessionSong`。
- **`file://` 缓存封面 `prepareArtworkDataUrl` 静默失败**  
  修复：原生侧 `file://` 优先 `FileInputStream`。
- **seek 到未缓冲区间后伪 finished 误切下一曲**  
  修复：源头限制 seek ≤ `bufferedPosition`；`seekPlayback` 成功后开启保护窗；`applyNativeState` 仅在非保护窗且接近自然结尾时 `handlePlaybackFinished`。
- **缓冲串曲 / 切歌后仍显示上一首缓冲条**  
  修复：`playSong` / `stopPlayback` / 播放失败均 `resetBufferState()`；原生 `cancelBufferSession` 取消上一首渐进下载。
- **没有 `npx cap sync android` 就部署**：前端代码改动不会反映到 APK  
  修复：每次前端改完后执行 `npm run build && npx cap copy android && cd android && ./gradlew :app:assembleDebug`。
