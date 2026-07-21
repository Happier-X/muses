# 进度条播放时不随时间更新 (#47)

## Goal

修复沉浸式播放页：播放中进度条与左侧时间应随播放推进；seek 成功后应继续推进，而不是停在 seek 位置。

## Background

用户反馈（#47）：

1. 播放时进度条**不会**随时间填充。
2. 点击跳转（seek）可以跳过去。
3. seek 之后底部时间与进度条**都不再动**。

相关实现：

- UI：`PlayerPage.vue` 使用 `ion-range`，`:value="effectiveSeekPosition"`（`seekPreviewPosition ?? playerState.position`），时间行用 `formatTime(playerState.position)`。
- 状态：`controller.applyNativeState` 写 `state.position`。
- 原生：`native.ts` 监听 Capgo `currentTime` / `playbackState`，再 `emitCurrentState` → controller。
- Capgo Android 端约每 100ms `notifyCurrentTime`；Web 实现亦有 interval。

近期相关改动：`ion-range` 替换原生 range（#41）、缓冲 seek 限制、finished 伪报防护。可能回归点：`ion-range` 受控 value、seek 保护窗、`currentTime` 被 assetId 过滤、seek 后 timer 未重启等。

## Requirements

### R1. 播放中进度推进
- 状态为 `playing` 时，`playerState.position` 应持续增大（约 0.1s 粒度即可）。
- `ion-range` 填充与时间行左侧时间与 `position` 一致更新。
- 非拖动预览时，进度条不得卡在 0 或某固定值。

### R2. Seek 后继续推进
- 用户拖动/点击 seek 成功后，进度跳到目标位置，**且**在仍 `playing` 时继续向前推进。
- seek 失败（未缓冲区间等）保持现有拒绝语义，不假更新。

### R3. 不回归
- 拖动中 preview 即时反馈；松手后提交 `seekPlayback`。
- seek 保护窗 + 自然结束判定仍有效。
- 缓冲 clamp、歌词点击 seek、手势隔离（不误触切面板/上下曲）保持。
- 媒体会话 `setPositionState` 仍随进度更新。

### Out of Scope
- MiniPlayer 进度 UI（若当前无进度条则不动）
- 改 capgo 插件源码
- 播放状态持久化（#49）

## Acceptance Criteria

- [x] 播放中进度条与左侧时间持续更新
- [x] seek 成功后进度与时间从新位置继续推进
- [x] 现有 player 相关单测通过；必要时补回归用例
- [x] lint / build 通过
- [ ] 关闭 GitHub #47

## Notes

- 优先排查链路：`currentTime` 事件是否到达 → `applyNativeState` 是否写入 → `ion-range` 是否响应 `value` 变化。
- 若根因在插件 timer 未在 resume/seek 后重启，优先在 `native.ts` / controller 用 `getCurrentTime` 轮询兜底，**不**改 `node_modules`。
