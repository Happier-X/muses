# 设计 — 纯队列循环切歌

## 1. 背景

- 需移除 `hasPrevious/hasNext` 与 `repeat/position` 判定，改为基于 `mediaItemCount` 与 `currentMediaItemIndex` 的环形索引；该索引即为当前时间线索引，`shuffleEnabled` 时自动为洗牌后顺序，顺序时为原始队列顺序，符合用户“随机即洗牌队列”预期。

## 2. 方案

```kotlin
fun skipToPrevious() {
  controller?.let { c ->
    val count = c.mediaItemCount
    if (count <= 1) { c.seekTo(0); return@let }
    val idx = c.currentMediaItemIndex
    val target = (idx - 1 + count) % count
    Log.w("PlayerConnection","skipPrev circular idx=$idx count=$count -> $target")
    c.seekTo(target, 0)
  }
}
fun skipToNext() {
  controller?.let { c ->
    val count = c.mediaItemCount
    if (count <= 1) { c.seekTo(0); return@let }
    val idx = c.currentMediaItemIndex
    val target = (idx + 1) % count
    Log.w("PlayerConnection","skipNext circular idx=$idx count=$count -> $target")
    c.seekTo(target, 0)
  }
}
```

- 播放态保持：`seekTo(index,0)` 后 `playWhenReady` 保持原值，ExoPlayer 自动续播，无需额外 `play()`。
- 日志为 info 观察首尾循环。

## 3. 涉及文件

- `core/media/PlayerConnection.kt`

## 4. 验证

- 队列 3 首，索引 1 -> prev=0, idx0->prev=2, idx2->next=0，均与进度无关。
