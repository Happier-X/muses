# 修复进度条拖动误触上一曲下一曲

## Goal

在沉浸式播放页拖动进度条时，不得触发上一曲/下一曲，也不得误切换歌词面板。

## Issue

- GitHub #5：`沉浸式播放页面的进度条在调节进度的时候，会触发上一曲和下一曲`

## Confirmed Facts

- 上一曲/下一曲入口：`PlayerPage` 的 `onPrevious` / `onNext` → `playPreviousFromQueue` / `playNextFromQueue`
- 进度条：`<input type="range" class="progress-slider">`，`@change="onSeek"`
- 外层 `player-overlay` 有全局 touch 手势：横向滑动切换控制/歌词面板，纵向下滑关闭
- `isNativeInteractiveTarget` 仅跳过 `preventDefault`，**不阻止** `onTouchEnd` 的横向面板切换
- 进度条在控件上方；松手时 touch 可能落在上一曲/下一曲按钮上，或横向位移被当成面板手势

## Requirements

1. **R1** 在进度条上按下/拖动/松手过程中，不得调用 `playPreviousFromQueue` / `playNextFromQueue`
2. **R2** 从进度条发起的触摸不得触发横向面板切换（`activePanel`）
3. **R3** 进度条本身仍可正常 seek
4. **R4** 控件按钮在非 seek 场景下行为不变
5. **R5** 补充单元测试（或可验证手势隔离逻辑）

## Acceptance Criteria

- [ ] AC1：进度条交互期间/刚结束后，点击合成或松手不会触发 prev/next
- [ ] AC2：从进度条开始的横向滑动不切换 `activePanel`
- [ ] AC3：`onSeek` 仍调用 `seekPlayback`
- [ ] AC4：相关单测通过

## Technical Notes

推荐：

- `progress-area` 上 `@touchstart.stop` / `@pointerdown.stop`，隔离 overlay 手势
- 或 `onTouchStart` 若目标是 range，标记 `gestureLocked`，`onTouchEnd` 跳过面板切换
- `onPrevious`/`onNext` 在 seek 锁定期内直接 return
- seek 结束后短 debounce（约 300ms）防 click 穿透

## Task Type

Lightweight
