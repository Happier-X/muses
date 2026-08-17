# 设计：后台自动切歌兜底（方案 C）

## 目标

锁屏/后台时当前曲播完 → 原生侧自动播下一首（预案）；JS 恢复后对账同步 UI/媒体会话；前台一切路径保持现状（JS 事件驱动）。

## 架构总览

```
播放中（JS 活跃）
  playSong 成功 ──► registerAutoNextPlan() ──► AudioPlayerBridge.setAutoNext(预案)
  pause/resume ──► AudioPlayerBridge.reportPlaybackStatus('paused'|'playing')

锁屏/后台（WebView JS 冻结）
  当前曲播完（原生 ExoPlayer/SoundPool 自然结束）
    └─► 原生轮询发现 isMusicActive()==false && 期望 playing && 预案存在
          └─► 防抖 2.5s 再确认
                └─► Bridge.callPluginMethod("NativeAudio","preload"/"play") 播预案曲
                      └─► notifyListeners("autoNextStarted", {songId})（JS 恢复后收到）
                      └─► 回收旧 asset（unload）

回前台（JS 恢复）
  autoNextStarted 事件积压到达 / appStateChange 对账（getState）
    └─► syncUiToNativeSong(songId)：playSongInternal(..., {nativeAlreadyPlaying:true})
          ├─► 同步队列/UI/媒体会话/预案（不重复调原生 play）
          └─► AudioPlayerNative.syncCurrentAsset(songId) 对齐原生模块内部状态
```

## 原生侧（android/.../AudioPlayerPlugin.kt）

### 新增字段

```kotlin
private data class AutoNextPlan(
    val songId: String,
    val assetId: String,        // JS 按 song-<sanitized> 规则生成
    val assetPath: String,      // local: uri（可能 content://）；webdav: 远程 url
    val isUrl: Boolean,
    val username: String?,      // webdav 用
    val password: String?,      // webdav 用（仅内存）
    val headers: JSObject?,     // 备用
    val volume: Double,         // RG 均衡后音量
    val currentAssetId: String?,// 播放中曲目的 assetId（自动播放后回收）
    val title: String?, val artist: String?, // 预案播放后更新媒体会话用（可选）
)
```

```kotlin
private var autoNextPlan: AutoNextPlan? = null
private var jsExpectedPlaying = false
private var pendingAutoNextAt = 0L          // 防抖起点（0=无）
private val autoNextCheckHandler = Handler(Looper.getMainLooper())
```

### 新 PluginMethod

1. `setAutoNext(call)`：解析入参存 `autoNextPlan`；`pendingAutoNextAt = 0`（预案更新 = JS 活跃，交给 JS 切歌）；resolve。
2. `clearAutoNext(call)`：`autoNextPlan = null; pendingAutoNextAt = 0`；resolve。
3. `reportPlaybackStatus(call)`：`jsExpectedPlaying = status == 'playing'`；非 playing 时清防抖；resolve。

### 轮询（load() 启动，1s tick）

```kotlin
private fun tickAutoNext() {
    val plan = autoNextPlan
    if (plan != null && jsExpectedPlaying) {
        if (isMusicActive()) {
            pendingAutoNextAt = 0          // 有音频在播：正常播放中，重置
        } else {
            val now = SystemClock.elapsedRealtime()
            if (pendingAutoNextAt == 0L) {
                pendingAutoNextAt = now    // 开始防抖
            } else if (now - pendingAutoNextAt >= AUTO_NEXT_DEBOUNCE_MS) {
                executeAutoNext(plan)
            }
        }
    } else {
        pendingAutoNextAt = 0
    }
    autoNextCheckHandler.postDelayed(::tickAutoNext, 1000L)
}
```

`AUTO_NEXT_DEBOUNCE_MS = 2500`（JS 正常切歌 preload 通常在 complete 后数百 ms 内发起；2.5s 足够区分冻结与正常处理）。

### 执行预案（executeAutoNext）

1. 快照 plan 并 `autoNextPlan = null; pendingAutoNextAt = 0`（一次性；防重复触发）。
2. 解析 assetPath：
   - `content://` → `copyContentUriToPlaybackCache`（已有）→ file://
   - webdav（username/password 非空）→ `audioCache.getCachedFile(url)` 命中则 file://（full）；否则远程 url + Basic Auth headers
3. 顺序调用 capgo 插件（`PluginCall.CALLBACK_ID_DANGLING`，不回传 JS）：
   - `preload { assetId, assetPath, isUrl, audioChannelNum:1, volume, headers }`
   - 失败且 `isPreloaded(assetId)` == true（asset 已存在，单曲循环等场景）→ 直接 `play`
   - 成功 → `play { assetId, volume }` → `setVolume { assetId, volume }`
4. 旧 asset 回收：`plan.currentAssetId != plan.assetId` 时 `unload { assetId: currentAssetId }`（先播后卸，避免卸掉正在播的）
5. `notifyListeners("autoNextStarted", { songId })`；任何异常 → `notifyListeners("autoNextFailed", { songId, reason })` 并清理。

**失败处理**：`autoNextFailed` 后不再重试（防止无限循环）；JS 回前台对账时走现有播放恢复链。

**安全性**：所有 capgo 调用包 try/catch；桥接调用失败不影响 JS 侧状态（JS 会自行对账）。

### 回调细节

`Bridge.callPluginMethod` 的 PluginCall 构造：`PluginCall(getBridge()?.messageHandler 或 null?...)` —— 需核对构造参数；使用 `PluginCall.CALLBACK_ID_DANGLING`（"-1"）避免响应回传 JS。preload/play 是异步（runOnUiThread），用调用链顺序：play 前先确认 preload 完成——通过 PluginCall 的 callback 串联（`call.setCallback`，核对 API），或 preload reject 后 isPreloaded 检查直接 play。

## JS 侧

### native.ts（AudioPlayerBridge 扩展）

```ts
interface AutoNextOptions {
  songId: string
  assetId: string
  assetPath: string
  isUrl: boolean
  username?: string
  password?: string
  volume: number
  currentAssetId?: string
  title?: string
  artist?: string
}
setAutoNext?(options: AutoNextOptions): Promise<void>
clearAutoNext?(): Promise<void>
reportPlaybackStatus?(status: 'playing' | 'paused' | 'stopped'): Promise<void>
addListener?('autoNextStarted'|'autoNextFailed', ...): Promise<PluginListenerHandle>
```

### controller.ts

1. `registerAutoNextPlan()`（幂等，静默）：
   - 无当前曲或 status 不在播放链 → `clearAutoNext()`
   - `peekNext()` null（队列空）→ `clearAutoNext()`
   - 否则 `buildPlayOptions(next)` → `setAutoNext({ songId: next.id, assetId: toAssetId(next.id), assetPath: next.sourceType==='webdav' ? options.url : options.uri, isUrl, username/password（webdav）, volume, currentAssetId: toAssetId(当前曲.id), title, artist })`
2. 触发时机：
   - `playSongInternal` 成功路径（prefetchNextTrack 旁）→ `registerAutoNextPlan()`；失败恢复链终止 → `clearAutoNext()`
   - `stopPlayback` → `clearAutoNext()`
   - queue 变更（`enqueueSongs`/`removeSongFromQueue`/`clearQueue`/`setRepeatMode`/`toggleShuffle`）→ 当前有播放则 `registerAutoNextPlan()`
   - `pausePlayback` → `reportPlaybackStatus('paused')`；`resumePlayback`/play 成功 → `reportPlaybackStatus('playing')`
3. **事件监听**（initializePlayer 中）：
   - `autoNextStarted({songId})` → 记 `nativeAutoNextSongId`；若 `state.currentSong?.id !== songId` → `void syncUiToNativeSong(songId)`
   - `autoNextFailed` → 仅记录日志（回前台对账处理）
4. **回前台对账**（controller 暴露 `reconcileAfterBackground()`，App.vue 在 `App.addListener('appStateChange')` isActive 时调用；同时 visibilitychange 兜底）：
   - `getState()` → nativeStatus/currentSongId
   - 原生 playing 且 currentSongId 存在且 ≠ state.currentSong.id → `syncUiToNativeSong(currentSongId)`
   - 原生非 playing 且 state.status ∈ {playing, finished} 且队列可继续 → `handlePlaybackFinished()`
5. **心跳兜底**（方案 B，controller 启动 1s interval）：
   - 仅 `document.hidden` 且 `state.status === 'playing'` 时执行：`getState()` → 原生非 playing → `handlePlaybackFinished()`
   - 后台节流后 ~1 次/分钟，作为原生兜底失败时的最后防线
6. `syncUiToNativeSong(songId)`：
   - `loadSongs()` 找 song；不存在 → 清状态
   - `AudioPlayerNative.syncCurrentAsset(songId)`（native.ts 新增：对齐 currentAssetId/currentSongId/currentSourceType/currentStatus，emitCurrentState）
   - `playSongInternal(song, { nativeAlreadyPlaying: true })`

### playSongInternal 改造（`nativeAlreadyPlaying` 模式）

- 参数 `options?: { nativeAlreadyPlaying?: boolean }`
- `nativeAlreadyPlaying` 时跳过 `AudioPlayerNative.play(...)`（原生已在播），直接走成功路径：status='playing'、pendingResumePosition 清理、persist、scanSongMetadata、prefetchNextTrack、`registerAutoNextPlan()`
- 其余（syncDisplayStateFromSong、媒体会话、歌词匹配）原样
- 播放失败恢复链逻辑在跳过 play 后不适用 → 该分支直接返回成功态

### native.ts 新增 `syncCurrentAsset(songId)`

```ts
export const syncCurrentAsset = (songId: string): void => {
  currentAssetId = toAssetId(songId)
  currentSongId = songId
  currentStatus = 'playing'
  emitCurrentState('playing')
  startPositionPolling()
}
```

## 关键时序与竞争分析

| 场景 | 行为 | 冲突处理 |
|------|------|----------|
| 前台正常切歌 | complete → JS 立即 preload 新曲 | 轮询见 isMusicActive 恢复 → 防抖重置；预案随后被 JS 更新 |
| 锁屏播完（JS 冻结） | 原生 2.5s 后播预案 | 预案一次性消费；autoNextStarted 供 JS 对账 |
| 单曲循环 | 预案 = 当前曲，preload 已存在 → 直接 play 重播 | currentAssetId == assetId 跳过 unload |
| 队列尾播完 | peekNext null → 预案已 clear | 原生不触发，播放自然停止 |
| WebDAV 远程 | 预案直接远程 URL + Basic Auth；有完整缓存走 file:// | 复用 audioCache |
| 暂停（JS 活跃） | reportPlaybackStatus('paused') → 轮询不触发 | jsExpectedPlaying=false |
| JS 崩溃/被杀 | 预案残留 + 期望 playing → 可能自动播一首后停止 | autoNextStarted 事件无接收方，无害；预案只消费一次 |
| 预案播放失败（断网） | autoNextFailed → 清理预案 | 回前台 JS 对账走恢复链 |

## 兼容性与风险

- `Bridge.callPluginMethod` / `PluginCall.CALLBACK_ID_DANGLING` / `PluginCall.setCallback`：Capacitor 8 公共 API（实现已核实存在），但属于"跨插件调用"，异常全部 try/catch。
- `AudioManager.isMusicActive()`：系统 API，可能受其他应用音频影响（外部音乐在播时 isMusicActive=true → 本应用播完也不触发预案——可接受，用户场景通常是独占播放；且锁屏下自己 app 静音才是主要诉求）。
- 预案注册失败/桥不可用：静默降级，回到现状（JS 心跳 + 回前台对账仍兜底）。
- 不修改任何 node_modules 文件；`npm install` 不受影响。

## 验收路径（真机验证清单）

1. 前台播放自然结束 → 自动切歌（现状不回归）
2. 小米 15 锁屏：播完自动切下一首，音频连续（核心验收）
3. 锁屏自动切歌后解锁回 App → UI/队列/通知栏一致
4. 通知栏上一首/下一首/暂停/恢复仍正常
5. 单曲循环锁屏重播；列表循环回绕；随机模式
6. 暂停后锁屏 → 不自动切歌
7. 队列尾播完 → 停止（不循环播放预案）
