# 歌词Apple Music弹簧精调

## Goal
在已有的原生自研歌词（`NativeLyricsPanel`/`NativeKaraokeLine`，已补 `animateItem` + `animate*AsState` 轻弹簧）基础上，精细复刻 **Apple Music 官方沉浸式歌词的弹簧物理**，使滚动、行进场、词级微动的阻尼与回弹与 Apple Music 一致，解决“还是没有弹簧效果”的体感问题。

## Background
- **现状（已验证）**：
  - 09-02 原生重构：`LazyColumn` + `AnnotatedString lerp` 实现逐词连续，但无弹簧
  - 09-02 补齐：加入 `Modifier.animateItem(spring 300/0.75)` 与 `animateFloatAsState` 行焦点弹簧（`alpha/scale/blur`），`animateScrollToItem` 默认 spring；已安装 MuMu 但用户反馈仍无 Apple Music 的弹性
  - 归档 vendored 的 Apple-like 弹簧实现：`feature/player/src/main/kotlin/com/mocharealm/.../utils/modifier/SpringPlacementModifier.kt` 采用 `LookaheadScope + DeferredTargetAnimation + spring(dampingRatio=1.1, stiffness=170..220)` 实现行位置的 **ApproachLayout** 弹簧（过阻尼、无振荡、跟随感），`KaraokeLineText.kt` 对字符级 `floatOffset/scale` 使用 `DipAndRise/Bounce/Swell` 曲线；但该渲染层被用户明确要求不直接使用，需在自研体系内手搓等效
  - 用户明确期望“就像苹果音乐的歌词效果一样的”——即列表滚动有惯性弹簧、行进入有轻微 overshoot、当前行有呼吸感

## Requirements
- **R1 — 列表滚动弹簧精调**：`LazyColumn` 自动居中滚动的 spring 参数由默认 `spring()` 精调为 Apple-like：`stiffness 180~220, dampingRatio 1.05~1.15`（过阻尼，无回弹振荡，跟随感），支持手势中断（`isManualScrolling` 时 `snap()`），与 vendored `SpringPlacementModifier` 的 `170..220 / 1.1` 对齐
- **R2 — 行 Placement 弹簧（核心）**：引入 `LookaheadScope` + `ApproachLayoutModifierNode` 的弹簧位移（复刻或手搓 `SpringPlacementModifier` 逻辑），替代或增强当前的 `animateItem`，使行在 `currentIndex` 切换时以弹簧跟随而非线性位移；`stiffness` 按距离动态（近行 220，远行 170）
- **R3 — 行焦点弹簧精调**：`NativeKaraokeLine` 的 `scale 0.92→1.05` 与 `alpha 0.18→1` 由当前 `spring(350/0.82)` 精调为 Apple-like：`scale spring(stiffness=320, dampingRatio=0.78, visibilityThreshold=0.001)` 带轻微 overshoot，`alpha spring(stiffness=280, dampingRatio=0.92)` 平滑，`blur` 同步 `spring(300,0.9)`
- **R4 — 词级/字符级微弹簧（可选，首版必做基础版）**：当前词的字符 `floatOffsetY` 与 `scale` 由静态 lerp 叠加弹簧式微动：`offsetY = 4dp * DipAndRise(1-fraction)` 与 `scale = 1 + 0.05*Swell(fraction)`，`DipAndRise`/`Swell` 用 `CubicBezier(0.33,1,0.68,1)` 近似，手搓实现不依赖 vendored easing 文件
- **R5 — 不回归 WebView/lyrics-ui 渲染**：保持纯 Compose，不引入 `KaraokeLyricsView`；允许复用 `SpringPlacementModifier` 的 **思想与参数**，但以自研文件 `AppleSpringPlacement.kt` 形式手搓，避免直接依赖 vendored 渲染层

## Acceptance Criteria
- [ ] AC1 — 播放时行切换，列表滚动与行位移有 Apple Music 的“跟随弹簧”感（过阻尼、200ms 内回位、无拖尾振荡），手势拖动后回中同样弹簧
- [ ] AC2 — 当前行获得焦点时有轻微 overshoot 弹性（scale 1.05 带回弹），失焦行平滑回 0.92
- [ ] AC3 — 词级微动：长词字符在演唱过程中有上下 4dp 浮动与 5% 缩放的弹簧微弹跳，无闪烁
- [ ] AC4 — `assembleMusesDebug` / `lint` / `test` 通过，MuMu 手动验收通过，`spec` 已补充弹簧参数

## Out of Scope
- 歌词解析、数据源、翻译、背景流体等
- 引入 WebView 或直接使用 `lyrics-ui` 的 `KaraokeLyricsView`

## Open Questions
（无）

## Decisions
- D1 — 完全一致：采用 `LookaheadScope + DeferredTargetAnimation + spring(dampingRatio=1.1, stiffness=170..220)` 方案手搓 `AppleSpringPlacement`（复刻 vendored `SpringPlacementModifier` 的过阻尼跟随，200ms 回位无振荡），目标与 Apple Music 完全一致（2026-09-02 确认，已调研 AMLL 官方与 Apple WWDC spring 文档）。

## Notes
- 已调研 Apple Music 原理（WWDC23 `Spring mass/stiffness/damping` 与 `duration/bounce` 转换、`UISpringTimingParameters`）与 AMLL 官方实现：`stiffness (2π/duration)², damping ((1-bounce)*4π)/duration`，AMLL 采用 `stiffness 170..220 / damping 1.1` 过阻尼跟随已验证为 Apple-like
- 现有 `NativeLyricsPanel` 已有 `listState` 与 `isManualScrolling` 状态，可直接接入 Lookahead
