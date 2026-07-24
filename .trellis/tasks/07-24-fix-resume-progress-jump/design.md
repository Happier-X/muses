# 技术设计

## 问题链路

1. 冷启动时 `restorePlaybackSessionIfNeeded()` 把持久化进度恢复到 `state.position`，页面显示上次位置。
2. 用户点击播放后，`resumePlayback()` 通过 `playSongInternal()` 重新加载音频。
3. 原生播放器启动后可能先上报同曲 `playing + position: 0`。
4. `applyNativeState()` 当前无条件采用该 position，导致 UI 进度回退。
5. `playSongInternal()` 随后调用原生 seek 并把 position 写回恢复点，因此形成可见跳动。

## 设计边界

修复放在 `src/features/player/controller.ts` 的播放器状态协调层，不在 `PlayerPage.vue` 增加视觉层缓存。状态协调层掌握 `pendingResumePosition` 和原生事件时序，能够区分冷启动恢复与普通播放；UI 层只应忠实展示稳定的 `playerState.position`。

## 状态约束

- 冷启动恢复位置在恢复 seek 完成前必须视为权威 UI 位置。
- 只屏蔽当前歌曲在冷启动恢复窗口内、明显早于恢复点的原生 position；不得屏蔽其它歌曲或普通播放事件。
- 原生状态中的 status、duration、bufferedPosition 等仍应按现有规则更新，修复范围只约束 position。
- seek 成功后清除恢复窗口并恢复正常原生 position 同步。
- seek 失败后也必须结束恢复窗口，让后续原生 position 正常驱动 UI；播放继续从原生实际位置推进。
- 快速切歌与新歌播放仍通过现有 `playGeneration`、songId 匹配和 `pendingResumePosition` 清理逻辑隔离。

## 测试策略

在现有冷启动续播测试中捕获 `stateChange` listener，并让原生 `play` 尚未完成或 seek 尚未完成时主动发送当前歌曲 `playing + position: 0`。断言中间态仍保持恢复位置，再完成 seek 并断言最终状态。补充 seek 失败路径时，断言恢复保护结束且后续原生进度可以更新。

## 风险与回滚

主要风险是保护窗口清理不完整，造成真实播放进度被冻结。实现必须在 seek 成功、seek 失败、播放失败、主动新曲播放等终止路径清理保护状态，并用测试约束。若出现回归，可回退 controller 中的恢复位置保护，不涉及存储迁移或 UI 结构回滚。
