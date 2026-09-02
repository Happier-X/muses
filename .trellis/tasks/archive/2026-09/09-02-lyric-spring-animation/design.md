# 设计 — 歌词弹簧动画补齐

## 1. 目标
在保持 `NativeLyricsPanel/NativeKaraokeLine` 现有逐词 lerp 与距离衰减基础上，叠加弹簧物理，使滚动、行焦点、词级微动具备 AMLL 弹性。

## 2. 架构
- 复用现有文件：`NativeLyricsPanel.kt`（列表）、`NativeKaraokeLine.kt`（单行）、`PlayerScreen.kt`（不改）
- 不新增模块/依赖，仅用 `androidx.compose.animation.core.spring/tween` 与 `Modifier.animateItem`

## 3. 关键改动
### 3.1 滚动弹簧（NativeLyricsPanel）
- `listState.animateScrollToItem(index, animationSpec = spring(stiffness=400f, dampingRatio=0.82f))` 替代默认 tween
- 保留 `isScrollInProgress` 防抖与 3s 自动回中，回中同样用 spring
- 备选：`spring(stiffness=350, dampingRatio=0.78)` 可调，注释说明

### 3.2 行 placement 弹簧
- `LazyColumn` 的 `itemsIndexed` 中为每行添加 `Modifier.animateItem(placementSpec = spring(stiffness=300f, dampingRatio=0.75f))`
- 要求 Compose BOM ≥1.7 且 `animateItem` 可用；key 已为 index 稳定

### 3.3 行焦点弹簧（NativeKaraokeLine）
- 将静态 `lineAlpha/lineScale/blurRadius` 改为 `animateFloatAsState(target, spring(...))` 与 `animateDpAsState`
- 参数：`alpha: spring(stiffness=350, dampingRatio=0.8)`，`scale: spring(stiffness=380, dampingRatio=0.7, visibilityThreshold=0.001f)`，`blur: spring(stiffness=300, dampingRatio=0.85)`
- `graphicsLayer(scale/alpha)` 与 `blur` 绑定动画值，非瞬切

### 3.4 词级微弹跳（可选，首版轻量）
- 在 `NativeKaraokeLine` 的 `AnnotatedString` 生成时，对当前词的 `fraction` 计算 `floatOffset = 4.dp * DipAndRise(1-fraction)`（复用简易三次贝塞尔 `CubicBezier(0.5, -0.5, 0.5, 1.5)` 近似），并通过 `SpanStyle(baselineShift)` 或 `LetterSpacing` 模拟微动；首版可仅对 `isCurrent && fraction in 0..1` 的词施加 `scale 1→1.04→1`（`animateFloatAsState` 按词维度）
- 若性能敏感，限制仅当前行且 `distance==0` 时启用；否则留空后续迭代

## 4. 兼容与性能
- 纯 Compose，无 WebView/lyrics-ui 依赖
- 弹簧仅触发 `currentIndex` 或 `distance` 变化时重组，不增加每帧重组（逐词 lerp 仍仅当前行每帧）
- 提供参数注释，便于用户在 MuMu 上微调手感

## 5. 取舍
- `spring` vs `tween`：spring 手感弹性但时长不可精确控制，tween 时长固定但生硬；沉浸式场景优先手感，选 spring
- `animateItem` vs 自定义 `SpringPlacementModifier`：前者官方、易维护，后者为 vendored 遗留，弃用
