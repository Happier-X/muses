# 歌词弹簧动画补齐

## Goal
为已上线的原生自研歌词（`NativeLyricsPanel`/`NativeKaraokeLine`）补充缺失的弹簧动画，还原 AMLL 的弹性体验：滚动跟手、行进场弹性、词级微弹跳，提升沉浸感与手感。

## Background
- **现状（已验证）**：09-02 原生重构已用 `NativeLyricsPanel(LazyColumn + animateScrollToItem tween)` 与 `NativeKaraokeLine(静态 alpha 1/0.45/0.28 + scale 1.05/0.92 + blur 6dp + AnnotatedString lerp)` 实现逐词连续渐变，但**无弹簧**：滚动为线性 tween，行 scale/alpha 为瞬切，词级无 Bounce/Swell/DipAndRise，观感偏硬。
- **归档参考**：vendored `lyrics-ui` 中 `KaraokeLineText` 使用 `DipAndRise/Bounce/Swell` 对字符级 `floatOffset/scale/blurRadius` 做帧级变换，`KaraokeLyricsView` 对行用 `AnimatedVisibility + spring`，背景有 `SpringPlacementModifier`；但该实现被用户明确弃用，需在自研体系内手搓等效弹簧。
- **用户诉求**：明确反馈“现在歌词没有弹簧动画效果呢”，要求补齐。

## Requirements
- **R1 — 滚动弹簧**：`LazyColumn` 的自动居中滚动由 `tween` 改为 `spring`（`stiffness≈400, dampingRatio≈0.8` 或等效阻尼），支持中断与手势抢占（`isScrollInProgress` 时不抢，3s 后恢复仍用弹簧回弹）。
- **R2 — 行 placement 弹簧**：`LazyColumn` 的 `items` 添加 `Modifier.animateItem(placementSpec = spring(stiffness=300, dampingRatio=0.7))`（或等效 `SpringPlacementModifier`），使插入/移动的行有弹性位移；需在 `LazyColumn` 中启用 `animateItem` 且 `key` 稳定。
- **R3 — 行焦点弹簧**：`NativeKaraokeLine` 的 `alpha/scale/blur` 由瞬切改为 `animateFloatAsState`/`animateDpAsState` 带 `spring(stiffness=350, dampingRatio=0.75)`，`distance 0→1` 时平滑弹性过渡，而非硬切。
- **R4 — 词级微弹跳（可选，首版可简化）**：当前词的字符在 `fraction` 0→1 过程中附加 `floatOffsetY`（`4dp * DipAndRise(1-fraction)`）与 `scale 1→1.05→1`（`Swell`）的弹簧式微动，保持与现有 `AnnotatedString` lerp 叠加，不引入 Canvas 重写；若性能敏感可仅对当前行生效。
- **R5 — 不回归 WebView**：保持纯 Compose，不引入 `lyrics-ui` 源码依赖；所有弹簧参数可调，默认提供平衡值并注释可调区间。

## Acceptance Criteria
- [ ] AC1 — 播放时当前行变化，列表滚动有明显弹簧回弹（非线性 tween），手势拖动后 3s 自动回中仍带弹簧。
- [ ] AC2 — 行成为焦点/失焦时，缩放 0.92↔1.05 与透明度 0.18→1 带有弹性过渡，远行 blur 同步弹簧淡入淡出。
- [ ] AC3 — 词级微弹跳（若实现 R4）：长词字符在演唱过程中有轻微上下浮动与缩放，无闪烁。
- [ ] AC4 — 无 WebView 回归，`./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest` 通过；MuMu 手动验收通过。

## Out of Scope
- 歌词解析、数据源、翻译开关、FAB 显隐、背景流体等已有逻辑的改动。
- 引入 `lyrics-ui` 或 WebView 渲染。

## Open Questions
（无）

## Decisions
- D1 — 采用 Compose `spring` 而非 `tween` 作为默认滚动与行动画规格，具体参数在 `design.md` 中固化并保留可调注释。

## Notes
- 现有 `NativeLyricsPanel` 已有 `listState.animateScrollToItem` 与 `listState.isScrollInProgress` 防抖，可直接替换 `animationSpec`。
- `NativeKaraokeLine` 的 `graphicsLayer` scale/alpha 已具备改造为 `animate*AsState` 的条件。
