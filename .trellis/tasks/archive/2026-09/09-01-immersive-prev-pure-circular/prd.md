# 沉浸式上一曲改为纯队列循环

## 目标

将沉浸式上一曲/下一曲改为无视当前进度与 `repeatMode` 的纯队列循环：点上一曲必到 `index-1`（队首到队尾），点下一曲必到 `index+1`（队尾到队首），队列为 1 时回零；顺序播放即按原始队列顺序，随机播放即按 `shuffleEnabled` 打乱后的洗牌队列顺序（复用 ExoPlayer 洗牌时间线）。

## 背景

- 现 `PlayerConnection.skipToPrevious` 经 `hasPrevious/repeat` 判定，2 分钟时仍可能走 `seekTo(0)` 回零，用户预期为纯队列前后且与分钟数无关（`803000d3` 仍卡 `repeat==ALL`）。
- 锚点 `PlayerConnection.kt:256`，`skipToNext` 亦受 `hasNext` 限制，队尾循环不一致。
- 关联契约：`features-lyrics-playlist.md §7` 沉浸式切歌。

## 需求

### 功能需求

1. **纯循环**：`skipToPrevious` 为 `(idx-1+count)%count`，`skipToNext` 为 `(idx+1)%count`（`count>1` 时），`count<=1` 时 `seekTo(0)` 防空。
2. **无视进度/repeat**：不再以 `currentPosition` 或 `repeatMode` 决定是否回零。
3. **不回归**：图标、横滑、下滑、进度、歌词不受影响。

## 验收标准

- [ ] 队列 3 首，任意进度（0s/2分钟）在任意位置点上一曲必到前一首、队首到队尾；点下一曲必到后一首、队尾到队首。
- [ ] 队列 1 首时点上一曲/下一曲回零不崩溃。
- [ ] `assembleMusesDebug`、`lintMusesDebug` 通过。

## 范围外

- 不改响度/歌词解析。

## 已确认事实与决策

- 用户明确“跟当前播放到几分钟有啥关系”，要求纯队列前后；并补充“顺序即队列顺序、随机即洗牌打乱的队列”，已确认随机下队列为洗牌算法打乱后的顺序（复用 `shuffleEnabled` 的时间线）。
