# 设计：后台原生队列自治

## 架构
- 新增 `PlaybackQueue.java` (简化版，基于SPlayer，移除personalFm/skipSong复杂逻辑，保留 repeatMode/windowTracks/windowCurrentIndex/hasNextOutsideWindow/hasPreviousOutsideWindow)
- `PlaybackService` 持有 `ExoPlayer + MediaSession + PlaybackQueue + Handler`
  - `updateQueueContext` PluginMethod 供JS推送窗口
  - `player.addListener` : STATE_ENDED -> `handleAutoAdvanceOnEnded()` 自治切歌
  - `setWakeMode(C.WAKE_MODE_NETWORK)` 在 onCreate 创建player时设置
  - `ForwardingPlayer` 可选：确保系统面板始终显示上一首/下一首可用（若需）
- `AudioPlayerPlugin` 仅转发 `updateQueueContext` 到 service，保留现有播放控制通过 MediaController（或改为直接操作service的player via singleton）

## 数据流
JS queue.ts (`enqueueSongs`/`advanceToNext`/`setRepeatMode`/`toggleShuffle`/`playSong`) 变化后 -> `syncQueueToNative()` -> `AudioPlayerBridge.updateQueueContext({windowTracks, windowCurrentIndex, repeatMode, hasNextOutsideWindow, hasPreviousOutsideWindow, shuffleEnabled})` -> PlaybackService.PlaybackQueue.replace

STATE_ENDED:
 PlaybackService.handleAutoAdvanceOnEnded()
  -> queue.advanceRaw(true) // 响应单曲循环
  -> if next==null && hasNextOutsideWindow -> emit requestUrls + pendingResumeAfterRefill=true -> return
  -> else playFromQueue(next) // setMediaItem+prepare+play

JS收到 requestUrls 事件 -> 计算新窗口 -> 再次 updateQueueContext -> Service检测 pendingResumeAfterRefill && windowRefilled -> advanceRaw + play

## 接口
PluginMethod:
- `updateQueueContext(JSObject window)` 包含 windowTracks JSONArray, windowCurrentIndex, repeatMode, hasNextOutsideWindow, hasPreviousOutsideWindow
- 事件：`requestUrls` 通知JS补窗, `playbackComplete` 保留兼容但后台不再依赖

## 兼容
- 前台仍走JS controller切歌，但原生也会切，需去重：JS的 handlePlaybackFinished 增加 guard，检测原生已切则跳过
- 本地/WebDAV URL已解析，原生playFromQueue直接用 url，无需异步resolver
