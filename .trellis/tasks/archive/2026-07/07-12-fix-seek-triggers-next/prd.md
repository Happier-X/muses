# 修复 seek 未完成时误触发下一曲

## Goal

用户拖动进度条或点击歌词跳转时，若目标进度尚未缓冲/无法到达，不得把异常结束当成自然播完而自动切到下一曲。

## User Report

- 拖动进度会触发上一曲/下一曲；用户判断是歌曲还没缓存完，seek 到目标进度过不去，就直接下一曲。
- 点击歌词跳转时同样复现。
- 与此前 #5「进度条手势误触」不同：本问题在 seek 成功路径之后仍可能发生，根因在 finished 自动切歌逻辑。

## Confirmed Facts

1. UI 入口：`PlayerPage` 进度条 `onSeek`、歌词 `onLyricLineClick` → `seekPlayback(seconds)`。
2. `seekPlayback` → `AudioPlayerNative.seek` → `NativeAudio.setCurrentTime`。
3. WebDAV/远程源走 capgo native-audio 的 ExoPlayer（`RemoteAudioAsset`）；`STATE_ENDED` 会 `notifyCompletion`。
4. 前端 `native.ts` 把 `complete` / `reason==='complete'` 映射为 `finished`。
5. `controller.applyNativeState` 在 `status==='finished'` 时调用 `handlePlaybackFinished` → `advanceToNext()` → `playSong`。
6. **没有任何“是否接近真实结尾”或“最近是否用户 seek”的校验**。因此 seek 到未缓冲区间后被插件当成 ENDED，就会自动下一曲。
7. 本地已缓存文件通常可正常 seek；远程/未缓存完时更易复现。

## Requirements

1. **R1** 用户主动 seek（进度条 / 歌词点击 / 媒体会话 seekto）后的短保护窗内，忽略 `finished/complete`，不自动切歌。
2. **R2** 仅当可判定为自然播完时才自动下一曲：例如 `duration > 0` 且当前进度接近结尾（`position >= duration - epsilon`，epsilon 建议 1.0–1.5s，可配置常量）。
3. **R3** 保护窗内收到伪 finished 后：保持当前歌曲与当前 seek 目标/最近有效进度，状态回到 `playing` 或 `paused`（按 seek 前是否在播），不 `advanceToNext`。
4. **R4** 真正自然播完（接近结尾 + 无 seek 保护）仍走 `handlePlaybackFinished`。
5. **R5** 手动 `stop` / 主动切歌 / `playSong` 新歌路径不受影响，不得因保护窗卡住队列。
6. **R6** 补充单元测试：
   - seek 后立刻收到 finished → 不切歌
   - 接近结尾的 finished → 仍自动下一曲
   - 歌词点击 seek 后 finished → 不切歌
7. **R7** 同步 `features-player.md` / `state-management.md` 中 finished 自动切歌约定。

## Acceptance Criteria

- [ ] AC1：`seekPlayback` 后保护窗内注入 `finished` 不会调用 `playSong(下一首)` / 不会改 `currentSong` 为下一首。
- [ ] AC2：进度接近结尾时的 `finished` 仍会自动下一曲（队列顺序/循环模式保持原语义）。
- [ ] AC3：歌词点击 seek 与进度条 seek 共用同一保护逻辑。
- [ ] AC4：相关单测通过；lint / type-check 通过。
- [ ] AC5：spec 写明「非自然结束的 finished 不得 advance」。

## Out of Scope

- 不改 capgo 插件源码（`node_modules`）。
- 不实现完整的“预缓存到可 seek 区间”下载器。
- 不改进度条手势隔离（#5 已做）。
- 不改自动下一曲的业务规则（循环/随机/单曲）。

## Technical Notes

推荐实现（前端 controller/native 边界即可）：

```ts
// controller 或 native 内
const SEEK_FINISH_GUARD_MS = 1500
const NATURAL_END_EPSILON_SEC = 1.25
let lastSeekAt = 0

// seekPlayback 成功后：
lastSeekAt = Date.now()

// applyNativeState / handlePlaybackFinished 前：
const isWithinSeekGuard = Date.now() - lastSeekAt < SEEK_FINISH_GUARD_MS
const isNearNaturalEnd = state.duration > 0 && state.position >= state.duration - NATURAL_END_EPSILON_SEC
if (nativeState.status === 'finished' && (isWithinSeekGuard || !isNearNaturalEnd)) {
  // 丢弃伪 finished：恢复 playing/paused，不 advance
  return
}
```

注意：

- seek 后 `position` 可能已被写成目标值；若目标靠近结尾（最后 1s 歌词），保护窗优先于 near-end，避免误切。
- `playSong` / `stopPlayback` 时应清理 seek guard，避免新歌首帧误吞 finished。
- 若 `duration` 未知（0），宁可保守：seek 保护窗内忽略 finished；窗外且 duration=0 时也建议不 advance（或仅在 complete + 无 seek 时 advance——实现时选更安全的默认并写进测试）。

## Task Type

Lightweight–medium bugfix；PRD-only 可 start。
