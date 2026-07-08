# 播放队列与循环随机设计

## 方案决策

在前端新增播放队列管理层，复用现有 `playSong()` 起点。确保容易扩展的队列、循环模式和播放模式都不依赖额外 Capacitor 插件边界。

## 架构

### 前端队列管理层

新增 `src/features/player/queue.ts`：

- 负责：
  - 队列存储和排序。
  - 循环模式（`单曲循环` / `列表循环`）。
  - 播放模式（`顺序` / `随机`）。
  - 决定下一曲算法。
  - 将队列与 `playerState` 同步显示。
- 接口：
  - `enqueueSongs(songs: SongItem[]): void`
  - `enqueueSong(song: SongItem): void`
  - `removeSongFromQueue(songId: string): void`
  - `clearQueue(): void`
  - `selectSongAtIndex(index: number): void`
  - `advanceToNext(): SongItem | null`（ExoPlayer 结束或用户点击下一曲时调用）
  - `advanceToPrevious(): SongItem | null`（用户点击上一曲时调用，只按当前队列回退，不维护历史栈）
  - 只读暴露队列、当前索和模式对象。

### 循环/播放模式的持久化

两个模式都需持久化到 localStorage。使用键 `muses:player-config`，结构：

```json
{
  "repeatMode": "one" | "all",
  "shuffleEnabled": false | true
}
```

### 随机播放

在 `shuffle` 切到 true 时，入队后基于当前顺序创建一个 Fisher-Yates 洗牌数组。`advanceToNext` 基于洗牌索引推进。切回顺序时恢复原始列表。

### Android 下一曲接口

本任务不修改 Capacitor 插件 signature。
- Android `AudioPlaybackService` 已有 `STATE_ENDED` 检测。
- 只需为 `STATE_ENDED` 向 JS 广播一个特殊 `stateChange` event（status=`stopped` 或者额外 action）。
- 如果按现状 `STATE_ENDED` 一律 `stopPlayback()` 清空 `currentSongId` 并停止 foreground service，会导致前端无法区分“手动停止”和“自然结束”。
- 设计：
  - 添加 `STATUS_FINISHED` 常量，当 `STATE_ENDED` 发生且 `currentSongId != null` 且循环/播放模式需要下一曲时，广播 status=`finished`（但不清除 foreground service/currentSongId），让前端调用 `advanceToNext` 后再 `playSong`。
  - 若播放结束且队列已空或无法获取下一曲，则仍 fallback 到 `stopPlayback()`。
  - `playSong` 调用 `AudioPlayer.play(...)` 前应允许服务仍处于前台，避免 `STATE_ENDED`→foreground 停止→quick restart→`startForegroundService` 再次创建，smoother UX。

### API 契约

- `SongItem` 和 `PlayerSongSnapshot` 保持不变。
- `state.status` 在 `finished` 时允许前端知悉自然结束。
- 密码、Basic Auth、SecureStorage、`data:` cover 仍然不进队列持久化或 `localStorage` 中的 `muses:songs` 或 `muses:queue`（用 id 索引即可）。

## 数据流

1. 用户在歌曲列表页面选择入队 songs。
2. `enqueueSongs` 把歌曲 ID 列表和模式保存到 `muses:queue`（含 `items[]` 只存 ID 和顺序）。
3. 用户播放第一首，`playerState` 更新；ExoPlayer 播放结束送 `stateChange(status=finished)`。
4. 前端判断循环模式+播放模式，获取下一首（`advanceToNext`）或重复当前。
5. 如果 `advanceToNext` 返回 null，前端调用 `stopPlayback()` 并清除队列活跃状态。

## UI 新增

- 在 `/player` 页面底部/右上角添加 `queue-button`。
- 新建 `/queue` 路由页面展示当前队列、清除、移除和切歌操作。
- 在 `/player` 页面控件区显示上一曲、播放/暂停、下一曲、循环模式、播放模式按钮（图标或文字）。
- `MiniPlayer` 不单独暴露队列控制。

## 安全约束

- 队列 localStorage 只存 `{ ids[], order[], repeatMode, shuffleEnabled }`，不存完整 `SongItem`。
- 下一页逻辑通过 `loadSongs()` 同步获取最新 `SongItem`，避免队列过时。
- WebDAV 密码等继续走 `playSong` 的内部路由。

## 风险与回退

- 如果 Android `PLAY_ENDED` 事件不及时或丢失，导致前端依赖 ExoPlayer 状态来判断结束 → 可增加 `position/duration` 推断，但设备验证必须确认 ExoPlayer 广播可靠。
- 若 Android `STATE_ENDED` 不及时 restarts 导致 UX 卡顿，考虑使用 `MediaItem` queue 功能；但这需要更大 Capacitor 接口改动 → 暂放 out-of-scope，仅使用当前事件广播。
- 随机切回顺序可能丢失洗牌位置 → 存储 `shuffleOrder` 和 `originalOrder` 以保持精确恢复。

## 验证

- 单元测试：入队、出队、顺序/随机、单曲循环、列表循环、转换、安全过滤。
- 设备验证：循环和播放完下一曲/单曲重复、UI 展示。