# 修复侧边栏已打开时继续右滑误关闭

## Goal

修复手机窄屏抽屉（`PhoneLayout` 推屏形态）在已打开状态下继续向右滑动被误判为关闭的问题，手势结算加入方向判断。

## 背景

文件：`app/src/main/kotlin/com/muses/player/navigation/TabsLayout.kt`，`PhoneLayout` 内 `detectHorizontalDragGestures` 的 `onDragEnd`。

当前结算逻辑只看位移/速度的绝对值：

- `OPENING`（起始关闭）：`shouldOpen = crossedDistance || crossedVelocity`
- `CLOSING`（起始打开）：`shouldOpen = !(crossedDistance || crossedVelocity)`

其中 `crossedDistance = abs(totalDx) >= drawerWidth * 25%`，`crossedVelocity = abs(velocity) >= 0.5px/ms`。

已打开时继续右滑，`totalDx` 为正且绝对值很大，仍命中 `crossedDistance`，导致 `shouldOpen = false` 被关闭。同理已关闭时向左大滑会被误打开。

## Requirements

- 手势结算必须考虑方向，只响应对应方向的滑动：
  - 起始关闭（`OPENING`）：仅向右（`totalDx > 0` / 正向快扫）可打开；向左滑动保持关闭。
  - 起始打开（`CLOSING`）：仅向左（`totalDx < 0` / 负向快扫）可关闭；向右滑动保持打开。
- 阈值口径不变：位移 ≥ 抽屉宽 25% 或快扫 ≥ 0.5 px/ms（500 px/s）。
- 小抖动/无位释放保持原状态（不闪开合）。
- 跟手阶段 `openFraction` 钳制 0..1 行为不变；`onDragCancel` 恢复起始状态行为不变。
- 平板 `TabletLayout`（固定 aside，无手势）不受影响。

## Acceptance Criteria

- [ ] 已打开时，向右滑动任意距离后松手，抽屉保持打开。
- [ ] 已打开时，向左滑动超过 25% 宽度（或快速左扫）后松手，抽屉关闭。
- [ ] 已关闭时，向左滑动后松手，抽屉保持关闭。
- [ ] 已关闭时，向右滑动超过 25% 宽度（或快速右扫）后松手，抽屉打开。
- [ ] `./gradlew :app:assembleMusesDebug` 通过。

## Notes

- 轻量任务，PRD-only，无设计/执行文档。
