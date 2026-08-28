# 设计 — 沉浸式面板半宽偏移修复

## 现状与根因

- `PlayerScreen` 顶部以 `BoxWithConstraints { val maxWidth = maxWidth }` 取轨道基线，进而 `Row(requiredWidth = maxWidth*2)`、`offset = panelOffset * maxWidth*2`、`Box(width = maxWidth)` 分割双面板
- 在 `TabsLayout.PhoneLayout` 的 `Box(offsetX)` 子树内，该 `maxWidth` 被上层 `Row`/`Box` 的 `50vw` 抽屉布局间接约束为半屏 `~180dp`（`540px`），导致轨道仅 `360dp`，左右面板各 `180dp` 并排，与截图“各占半屏”一致
- 预期：无论 `TabsLayout` 如何布局，沉浸页作为覆盖路由（`navVisible=false` 时 `TabsLayout` 直接 `Box(fillMaxSize){content()}` 全屏）应以**物理屏宽**为基线

## 设计选型

- **选型 A（采用）：** 沉浸页内改以 `LocalConfiguration.screenWidthDp.dp` 为 `screenWidth` 基线，完全摆脱局部 `BoxWithConstraints` 约束；保留 `BoxWithConstraints` 仅用于 `maxHeight` 等高度相关逻辑，或改为普通 `Box`
- **备选 B：** 在 `TabsLayout` 侧保证覆盖路由的 `content()` 测量为全屏——但覆盖路由已是 `if (!navVisible) Box(fillMaxSize){content()}` 全屏，问题仍出在 `PlayerScreen` 内部对 `maxWidth` 的局部采样，修 `PlayerScreen` 更收敛
- **额外保障：** 横滑偏移改用 `screenWidthPx`（`LocalDensity` 转换）计算，避免 `maxWidth.toPx()` 在半屏约束下的误差

## 改动面

- **唯一改动文件：** `feature/player/PlayerScreen.kt`
  - 将 `BoxWithConstraints` 的 `maxWidth` 替换为 `screenWidth = LocalConfiguration.current.screenWidthDp.dp`（或 `remember { }` 缓存）
  - `Row.requiredWidth = screenWidth * 2`，`offset { IntOffset((panelOffset * screenWidth.toPx() ).roundToInt()*2? ) }` 精确为 `panelOffset * screenWidthPx *2` 的整形偏移
  - `Box(width = screenWidth)` 替代 `width(maxWidth)`
  - 保留 `maxHeight` 如需可仍用 `BoxWithConstraints` 或 `LocalConfiguration.screenHeightDp`
- **不改：** `TabsLayout.kt`、`MeloX*`、`PlayerViewModel`、`InfoPanel/MetaWindow` 内部 padding

## 兼容与回滚

- `screenWidthDp` 随旋转自动更新，覆盖横竖屏；`TabletImmersiveLayout` 仍以同一 `screenWidth` 判定 `isTabletLayout`（已用 `LocalConfiguration`），一致
- 回滚：单文件 `git restore PlayerScreen.kt` 即可，`05-28` 后的 `navigationBarsPadding/canSeek` 等保留
- 风险低：仅测量基线替换，不涉及手势阈值与动画曲线

## 验证

- `uiautomator` 中 `ScrollView` 宽从 `480px` 恢复至 `~1080px`（`360dp` 屏宽减去 `20dp` 两侧 padding 后约 `960px` 内容宽）
- 目视：`0321 - space x` 封面居中、`暂无` 文案居中，无并排
