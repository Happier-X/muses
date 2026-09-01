# 设计 — 沉浸式上一曲首次仍回零（队首循环与时间线就绪）

## 1. 背景

- `fca50e7d` 后 `skipToPrevious = hasPrevious ? seekToPrevious : seekTo(0)`，但首次点仍回零，说明 `hasPrevious==false`。
- Media3：`hasPreviousMediaItem == (currentIndex>0 || repeatMode==REPEAT_MODE_ALL && mediaItemCount>1)` 的变体，但时间线未就绪时 `mediaItemCount` 可能短暂为 0/1，或 `currentIndex` 仍为 0 且未同步 `repeatMode`，导致队首循环场景仍判 false。
- 洗牌/队列同步延迟亦可能使首次判 false。

## 2. 目标

使队首循环时上一曲可回到队尾，且首次点击不受时间线/队列同步竞态影响。

## 3. 方案

```
controller.let { c ->
  val count = c.mediaItemCount
  val idx = c.currentMediaItemIndex
  val hasPrev = c.hasPreviousMediaItem()
  val rep = c.repeatMode
  Log.w("PlayerConnection","skipPrev idx=$idx count=$count hasPrev=$hasPrev rep=$rep pos=${c.currentPosition}")
  when {
    hasPrev -> c.seekToPrevious()
    rep == Player.REPEAT_MODE_ALL && count > 1 -> c.seekTo(count-1, 0) // 队首循环回到队尾
    else -> c.seekTo(0) // 无前曲且非循环，回零
  }
}
```

- 兜底：若 `count==0` 则直接 `seekTo(0)`；此分支仅作防御。
- 日志同时在 `FullPlayerWebView` 的 `previous` 分支加 `position/hasPrev/index` 透出，便于 `adb` 定界。

## 4. 涉及文件

- `core/media/PlayerConnection.kt`（主改）
- `feature/player/lyric/FullPlayerWebView.kt`（可选日志）
- `spec/android/features-lyrics-playlist.md`（补充队首循环契约）

## 5. 验证

- REPEAT_ALL 队列 3 首，在第 0 首首次点上一曲 -> 应到第 2 首；第 1 首点上一曲 -> 到第 0 首；REPEAT_OFF 队首点上一曲 -> 回零。
- `adb logcat -s PlayerConnection/FullPlayer` 核验判分支。

## 6. 回滚

- 回退 `skipToPrevious` 至 `hasPrev ? seekToPrevious : seekTo(0)`。

