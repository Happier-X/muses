# 沉浸式上一曲首次仍回零排障

## 目标

解决移除 3s 阈值后首次点击上一曲仍回到 0s 的问题，使沉浸式中任意位置（尤其首次点击）有前曲时必切上一曲，队首在循环模式下可回到队尾。

## 背景

- 已按 B 方案将 `PlayerConnection.skipToPrevious` 改为 `hasPrevious -> seekToPrevious else seekTo(0)`（`fca50e7d`），但 MuMu 实测首次点击仍回零。
- 锚点 `PlayerConnection.kt:256` 与 `PlaybackService` 的 `ExoPlayer`：`hasPreviousMediaItem` 在队首 `index==0` 且 `repeatMode != ALL` 时为 false；即便 `REPEAT_MODE_ALL` 时 Media3 的 `hasPrevious` 亦可能受时间线未就绪或队列同步延迟影响导致首次为 false，从而走 `else seekTo(0)`。
- 需定界：队首循环、队列未同步、shuffle、controller 未就绪四分支；并加日志可观测。

## 需求

### 功能需求

1. **首次必切**：队列 ≥2 且 `repeatMode==ALL` 或非队首时，首次点上一曲必切上一曲（队首循环时切队尾），不再回零。
2. **队首无前曲且非循环时回零**：`repeatMode==OFF` 且在队首时点上一曲回零（保持 Media3 语义但明确）。
3. **不回归**：下一曲、图标、横滑、下滑、进度、歌词不受影响。

### 非功能 / 约束

- 保持不引新依赖，日志为 info 级。
- 兼容洗牌与时间线未就绪的竞态。

## 验收标准

- [ ] 队列 ≥2、REPEAT_ALL 时在队首首次点上一曲切到队尾而非回零；队中任意位置点上一曲必切前一首。
- [ ] REPEAT_OFF 队首点上一曲回零，队中仍切前一首。
- [ ] `adb logcat -s FullPlayer` 可见 `skipPrev hasPrev/index/repeatMode` 日志。
- [ ] `assembleMusesDebug`、`lintMusesDebug` 通过。

## 范围外

- 不改响度/歌词解析。

## 已确认事实

- 首次回零非 3s 阈值，疑为 `hasPrevious==false` 分支。
- 下一曲正常，说明 Bridge 与 WebView 链路已通，问题在 `hasPrevious` 判定或队首循环语义。

## 技术备注

- 拟在 `skipToPrevious` 内当 `!hasPrevious && repeatMode==ALL && count>1` 时改走 `seekTo(lastIndex,0)`，并加日志；若时间线未就绪则以 `queue.size` 为兜底。
