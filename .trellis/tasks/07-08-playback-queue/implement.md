# 播放队列与循环随机实施计划

## 实施顺序

### 1. 队列管理层

- 新增 `src/features/player/queue.ts`。
- 导出：
  - `queueItems`（当前队列的 `SongItem` 只读数组）
  - `currentIndex: number`
  - `repeatMode: 'one' | 'all'`
  - `shuffleEnabled: boolean`
  - `enqueueSongs(songs: SongItem[]): void`
  - `enqueueSong(song: SongItem): void`
  - `removeSongFromQueue(songId: string): void`
  - `clearQueue(): void`
  - `selectSongAtIndex(index: number): void`
  - `advanceToNext(): SongItem | null`
  - `advanceToPrevious(): SongItem | null`
  - `setRepeatMode(mode): void`
  - `toggleShuffle(): void`
- 随机：Fisher-Yates shuffle。
- 持久化：`muses:player-config` + `muses:queue`。

### 2. Android 下一曲事件

- 在 `AudioPlaybackService.kt` 中添加广播 `STATUS_FINISHED` 常量。
- 在 `onPlaybackStateChanged == STATE_ENDED` 且服务未处于手动停止状态时，广播 `status=finished`。
- 不清除 `currentSongId` 和 foreground service，直到前端确认是否继续播放。
- `AudioPlayerPlugin.kt` 注册 `finished` status 到 JS 映射。

### 3. 控制器集成

- `src/features/player/controller.ts` 的 `applyNativeState` 监听 `finished`：
  - 改为 call `advanceToNext()` 获取下一首。
  - 如果得到歌曲，`playSong(nextSong)` 继续。
  - 如果返回 null，`stopPlayback()` 停止。
- 暴露 `queue` 状态给外观。

### 4. 新 UI 页面和控件

- 新增 `/queue` 路由页面：`src/views/QueuePage.vue`。
- 页面显示当前队列，每个条目有移除按钮。
- 页面底部有按钮：清除队列。
- `/player` 页面底部或右上角添加 `queue-button` 跳转 `/queue`。
- `/player` 页面控件区添加上一曲、播放/暂停、下一曲按钮，以及循环模式（one/all）、播放模式（顺序/随机）按钮。应用 `IonIcon` 或文字标识当前模式。
- `MiniPlayer` 不调整。

### 5. 状态管理同步

- 在播放开始时，确保 `currentIndex` 与队列内歌曲同步。
- `playSong` 保存 `nextSong` 参数以支持来自队列的切歌。
- 状态发布包含 queue 相关信息。

### 6. 前端测试

- `tests/unit/player.spec.ts` 新增：
  - 入队/出队不产生密码泄漏。
  - 顺序模式 next 正确。
  - 随机模式 next 正确，且洗牌数组长度一致。
  - 单曲循环 `advanceToNext` 返回当前歌，`repeatMode=one`。
  - 列表循环 `advanceToNext` 到达尾部后跳回第一首。
  - `advanceToPrevious` 按当前队列顺序回退，队首回到队尾。
- 保留现有播放器安全边界测试。

### 7. Android 测试

- `AudioPlaybackService` 广播 `STATUS_FINISHED` 单元测试。
- 本地编译：`cd android && ./gradlew :app:compileDebugKotlin`。

## 验证命令

```bash
npm run test:unit -- --run
npm run lint
npm run build
cd android && ./gradlew :app:compileDebugKotlin
```

## 设备验证清单

- 播放本地歌曲完成后自动跳到下一首（顺序模式）。
- 播放 WebDAV 歌曲完成后自动跳到下一首（顺序模式）。
- 单曲循环完成后重复当前歌曲。
- 列表循环：队列最后一首→第一首。
- 随机播放：第二个歌曲不一定是列表第二个，下一个也不一定是原始第三首。
- 转回顺序：恢复原始队列顺序。
- `/queue` 页面显示正确条目，移除功能可用。
- `/player` 页面显示上一曲、播放/暂停、下一曲、循环/播放模式按钮，状态切换后按钮反映当前模式。