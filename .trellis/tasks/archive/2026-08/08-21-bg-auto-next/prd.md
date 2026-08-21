# 修复后台自动切歌-原生队列自治

## Goal
解决后台播放时歌曲播完无法自动切下一首（WebView冻结导致JS无法处理`playbackComplete`）的问题，参考 SPlayer 的原生队列自治方案，实现后台自治切歌。

## Background
- 现状：`PlaybackService` 仅包装 ExoPlayer+MediaSession，`STATE_ENDED` 时通过 `playbackComplete` 事件通知 JS，再由 `controller.ts` 的 `handlePlaybackFinished` -> `playNextFromQueue` 切歌。后台时WebView被冻结，JS无法响应，表现为后台卡在上一首，重新打开才恢复。
- SPlayer方案：原生持有滑动窗口队列 `PlaybackQueue`，`PlaybackManager.onPlaybackStateChanged(STATE_ENDED)` 直接在Java层 `advanceRaw(true)` 取下一首并 `resolveAndPlayAsync`/`playFromQueue`，完全不依赖JS；JS仅通过 `updateQueueContext` 推送窗口，窗口耗尽时原生 `emit requestUrls` 让JS补窗，`pendingResumeAfterRefill` 续播。
- 额外：SPlayer 设置 `player.setWakeMode(C.WAKE_MODE_NETWORK)` 防止Doze节流导致后台无法prepare。

## Scope
- In Scope:
  - 移植简化版 `PlaybackQueue.java` 到 muses
  - `PlaybackService` 维护原生队列，STATE_ENDED自治切歌
  - `player.setWakeMode(C.WAKE_MODE_NETWORK)` 防Doze
  - JS层新增 `syncQueueToNative()`，队列变化/切歌/模式变化时调用 `AudioPlayerBridge.updateQueueContext`
  - `WINDOW_REFILL_THRESHOLD=2` 触发 `requestUrls` 事件补窗
  - 单曲循环/列表循环/随机逻辑与JS保持一致
- Out of Scope:
  - 完整的URL异步解析（muses本地/WebDAV URL已就绪，无需网易云解析）
  - 双ExoPlayer/Automix
  - 频谱/均衡器

## Acceptance Criteria
1. 后台播放时歌曲自然播完自动切下一首，无需打开App
2. 前台/后台行为一致，队列/随机/单曲循环均正确
3. 窗口耗尽时能请求JS补窗并续播
4. 无后台ANR，Doze后仍能prepare
5. 通知栏/媒体按钮/耳机按键仍可用

## Key Decisions
- 复用 SPlayer 的滑动窗口队列思想，但简化：muses队列已在JS持久化，原生仅镜像窗口，无需独立存储
- 原生不存完整SongItem，仅存 `songId/url/title/artist/coverUrl` 最小元数据
- WakeMode 使用 NETWORK
