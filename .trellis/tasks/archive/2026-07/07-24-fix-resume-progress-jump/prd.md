# 修复冷启动续播时进度条跳动

## 目标

修复应用重新打开后进入沉浸式播放页并点击继续播放时，播放进度条短暂回退或跳动的问题，让冷启动续播期间的进度展示保持稳定，并最终从恢复点继续播放。

## 用户影响

用户上次播放到某个进度后关闭应用，再重新打开应用进入沉浸式播放页点击播放时，进度条会先显示恢复位置，但播放启动过程中可能短暂跳回 0 或其它原生初始位置，再跳回恢复点，造成续播状态不可信。

## 已确认事实

- `src/features/player/session.ts` 使用 `muses:playback-session` 保存 `currentSongId` 和 `position`。
- `src/features/player/controller.ts` 的 `restorePlaybackSessionIfNeeded()` 会在冷启动时恢复 UI：`state.status = 'paused'`、`state.position = session.position`，并设置 `pendingResumePosition` 与 `restoredSessionUiOnly`。
- `resumePlayback()` 在 `restoredSessionUiOnly` 时走 `playSongInternal(song)`，播放成功后再调用 `AudioPlayerNative.seek({ position: pendingResumePosition })`。
- `playSongInternal()` 已在发起播放前把 `state.position` 保持为 `pendingResumePosition`，目的是避免 play 前 UI 闪回 0。
- `applyNativeState()` 对普通 `playing` 原生状态会直接写入 `state.position = normalizePlaybackTime(nativeState.position)`。
- 因此播放启动过程中若原生先上报同曲 `playing` 且 `position` 仍为 0，UI 进度会被中间状态覆盖，随后 seek 成功后再回到恢复进度，形成用户看到的跳动。
- `tests/unit/player.spec.ts` 的冷启动续播测试只覆盖最终 `play + seek` 和最终 `playerState.position`，尚未覆盖启动中间态不回退。
- `src/views/PlayerPage.vue` 的进度条 `ion-range` value 绑定 `effectiveSeekPosition`，默认来源是 `playerState.position`；非手势 `ionInput` 已被防御，不是本次问题的主要入口。

## 要求

- 冷启动恢复后，用户点击继续播放时，在恢复 seek 完成前，进度条不得被原生启动初始位置短暂覆盖为 0 或明显更小的值。
- 冷启动续播仍必须调用原生 `play`，并在播放启动成功后 seek 到恢复位置。
- 恢复 seek 成功后，`playerState.position` 保持恢复位置，并允许后续真实播放进度正常递增。
- 若恢复 seek 失败，仍允许播放从原生当前位置继续，但不得破坏现有错误处理或播放状态。
- 普通暂停后继续播放、用户主动点播新曲、手动拖动进度条、歌词点击 seek、自然播完切歌等既有行为不得回归。
- 不引入新的可见 UI、路由或用户设置。

## 验收标准

- [ ] 单元测试覆盖冷启动恢复到非零进度后点击继续播放时，原生播放启动阶段上报同曲 `position: 0` 不会让 `playerState.position` 回退。
- [ ] 单元测试覆盖恢复 seek 成功后仍调用 `AudioPlayerNative.seek({ position: 恢复进度 })`，最终状态为 playing 且 position 为恢复进度。
- [ ] 现有 `onSeekInput` 非手势不冻结进度条测试继续通过。
- [ ] 现有冷启动 session 恢复、普通 resume、seek、finished 防误切歌相关测试继续通过。
- [ ] `npm run lint`、`npm run build`、`npm run test:unit -- --run` 通过。

## 范围外

- 不调整会话持久化格式。
- 不改变自动续播策略：冷启动仍只恢复为 paused，不自动出声。
- 不重构播放器整体状态机或原生插件协议。
- 不修改播放器视觉样式。

## 待决策

- 无。
