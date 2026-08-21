# 实现计划

1. 移植 `PlaybackQueue.java` 简化版到 `com.muses.player` (copy SPlayer, 删除 personalFm/skipSong/favorite 相关，保留核心)
2. 修改 `PlaybackService.kt`:
   - 添加 PlaybackQueue 实例, pendingResumeAfterRefill, resolveToken等
   - onCreate 创建player时 setWakeMode(C.WAKE_MODE_NETWORK), addListener处理STATE_ENDED
   - 实现 handleAutoAdvanceOnEnded(), playFromQueue(track), prefetchUpcomingUrls, requestUrlsIfNeeded
   - 暴露 updateQueueContext() 供Plugin调用
   - 添加 notifyListeners requestUrls 事件
3. 修改 `AudioPlayerPlugin.kt`:
   - 新增 updateQueueContext PluginMethod 转发到 PlaybackService
   - 简化：playFromQueue时处理 file:// vs http:// 的 DataSource (复用现有逻辑)
   - 确保 mediaController/playbackService单例访问一致 (考虑让 PlaybackService 持有唯一player，Plugin通过 service.getPlayer() 操作)
4. JS层:
   - 新增 `src/features/player/nativeQueueSync.ts` 或在 controller.ts 增加 syncQueueToNative()
   - 在 queue.ts 的 onQueueChanged + controller playSong/ setRepeatMode/toggleShuffle 后调用
   - 监听原生 requestUrls 事件 -> 重新计算窗口并推送
5. 验证:
   - npm run build && cap sync && gradlew assembleDebug
   - 真机后台播放3首连续切歌测试
   - 单曲循环/随机/列表循环各模式后台测试
