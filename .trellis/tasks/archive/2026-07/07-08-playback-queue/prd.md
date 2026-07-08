# 播放队列与循环/随机模式

## 目标

为当前播放器添加播放队列和循环模式/播放模式，解决目前播放完一首歌曲后自动停止的问题。

- **循环模式**（播放完当前曲目后的行为）：
  - 单曲循环：重复播放当前歌曲
  - 列表循环：播放完最后一首后跳到第一首继续
- **播放模式**（队列的组织方式）：
  - 顺序播放：按歌曲列表原始顺序组织队列
  - 随机播放：用洗牌算法打乱排序后的列表

## 背景与已完成调研

- 播放器控制器 `src/features/player/controller.ts` 已存在 `playSong(song)` / `pausePlayback()` / `resumePlayback()` / `stopPlayback()` / `seekPlayback(pos)` 和 `exposeState`。
- Android `AudioPlaybackService.kt` 的 `onPlaybackStateChanged` 已检测 `STATE_ENDED`，当前逻辑是调用 `stopPlayback()` 清空歌曲并停止 foreground service。
- 前端 `applyNativeState` 在服务清除 `currentSongId` 后 reset `PlayerState`。
- 歌曲来源：`loadSongs()` 从 `muses:songs` localStorage 加载全部歌曲列表 `SongItem[]`；`loadSources()` 从 `muses:sources` 加载数据源描述。
- 歌曲 ID 通过 `createSongId(sourceId, path)` 生成确定性 ID；每个 `SongItem` 包含 `sourceId`、`sourceType`、`path`、`uri` 等字段。
- 已有两种数据源：本地和 WebDAV；WebDAV 播放需要 SecureStorage 密码。

## 需求

1. **播放队列**：新增播放队列数据结构，支持任意歌曲列表入队和在队列中切换。
2. **循环模式**：持久化循环模式开关，默认 **列表循环**。
   - `单曲循环`：当前歌曲播放完毕后自动重新播放同一首歌。
   - `列表循环`：当前歌曲播放完毕后自动跳到队列中的下一首继续播放。
3. **播放模式**：持久化播放模式开关，默认 **顺序播放**。
   - `顺序`：队列顺序和用户上次设置的顺序保持一致。
   - `随机`：首次切换到随机时用洗牌算法打乱当前队列；随机队列不覆盖原始列表顺序；切回顺序时恢复原顺序队列。
4. **队列操作**：
   - 从歌曲列表或搜索结果中一键入队全部歌曲。
   - 支持单首歌曲追加到队列尾部。
   - 从队列中删除指定歌曲。
   - 清空队列。
   - 点击队列中的歌曲直接切换播放。
5. **集成 ExoPlayer 结束时自动切换下一曲**：
   - Android `AudioPlaybackService` 在 `STATE_ENDED` 时通知前端播放结束。
   - 前端根据循环模式和队列决定下一曲行为，并调用 `playSong`。
   - 前端不扩展 Capacitor 插件接口，只需 Android 在播放结束时向 JS 广播一个事件。
6. **UI 入口**：
   - `/player` 页底部或右上角展示队列入口。
   - 新增队列页面，展示当前队列歌曲及顺序，包含清除、移除和切歌操作。
   - 循环模式和播放模式切换按钮，视觉或文字反馈当前模式。
   - 沉浸式 `/player` 页面必须提供上一曲、下一曲、播放/暂停、循环控制按钮；上一曲基于当前队列索引回退，不引入独立历史栈。
7. **安全约束**：
   - WebDAV 密码、Basic Auth header、SecureStorage 值、`data:` 封面仍不得进入 `PlayerState`、localStorage 的 `muses:songs`、logcat、Media3 metadata 或任何第三方 payload。
   - 队列持久化到 localStorage 时，只存储歌曲 `id` 和必要排序信息，不存储完整 `SongItem`（避免敏感数据冗余和随源更新不同步）。

## 非目标

- 不引入“上一首”跟踪历史；上一曲按钮只按当前队列顺序回退。
- 不修改 AudioPlayer Capacitor 插件接口签名。
- 不使用第三方队列/播放列表库。
- 不处理 Android Auto / Cast / Wear OS 队列联动。

## 待设备验证

- 播放完一首本地歌曲后自动切换到下一首。
- 播放完一首 WebDAV 歌曲后自动切换到下一首（缓存和远程均测试）。
- 单曲循环不切换到其他歌曲。
- 列表循环在队列最后一首结束后跳到第一首。
- 随机播放模式切换后队列显示为乱序，且播放按乱序进行。
- 切回顺序播放后恢复原始顺序。
- `/player` 页面上一曲、下一曲、播放/暂停、循环/播放模式按钮状态正确，点击后行为正确。
- 不清空 localStorage 的情况下刷新应用后队列和循环/播放模式保持。
- 密码/Basic Auth/`data:` cover 不出现在 `localStorage`、队列 UI、Media3 metadata、logcat。