# 沉浸式播放页适配顶部安全区域

## Goal

沉浸式播放页顶部标题/封面不再被状态栏（挖孔/灵动岛区域）压住，顶部留出系统安全区，背景仍全屏沉浸。

## Background（已确认事实）

- `MainActivity` 已 `enableEdgeToEdge`，状态栏叠加在内容上。
- `feature/player/.../PlayerScreen.kt`：
  - `PhoneImmersiveLayout` 中 `FixedSongHead` 仅 `padding(top=16.dp)`，外层 `Column(Modifier.fillMaxSize())` 未加 `statusBarsPadding`。
  - `TabletImmersiveLayout` 中 `FixedSongHead` 仅 `padding(top=4.dp)`，外层 `Column` 同样未避让。
  - `InfoPanel` 只有 `navigationBarsPadding()`，无顶部避让。
- 项目内已有正确范式：`TabsLayout.DrawerPanel` 用 `WindowInsets.statusBars.getTop()` 计算 `navbarPt`，`TabletLayout` 用 `Spacer(Modifier.statusBarsPadding())`。

## Requirements

- R1：手机形态沉浸页顶部内容（标题行）避让状态栏，背景层保持全屏延伸。
- R2：平板形态沉浸页顶部内容同样避让状态栏。
- R3：不改变下滑关闭手势、面板切换、底部控制条逻辑。

## Acceptance Criteria

- [ ] 真机/模拟器上打开沉浸式播放页，标题首行与状态栏无重叠，顶部留白与系统栏高度一致。
- [ ] 旋转/平板形态同样无重叠。
- [ ] 背景（FlowingLightBackdrop）仍铺满全屏，无黑边。
- [ ] 下滑关闭、左右切词面板、播放控制均正常。

## Out of Scope

- 队列页、底部 MiniPlayer 的安全区调整。
- 底部手势导航栏样式改动。

## Open Questions

- 无。
