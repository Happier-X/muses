# 去除放大精准复刻Apple Music无缩放弹簧

## Goal
去除当前自研歌词中与 Apple Music 不符的 `scale 1.05/0.92` 放大效果，精准复刻 Apple Music 官方沉浸式歌词的无缩放、仅位置/透明度/模糊与词级遮罩的弹簧，解决“现在并没有复刻”“苹果音乐没有放大”的体感偏差。

## Background
- **现状（已验证）**：
  - 原生重构后 `NativeKaraokeLine` 引入 `lineScale 1.05/0.92` 的 `animateFloatAsState(spring 320/0.78)` 放大，用于模拟“呼吸感”，但 Apple Music 真机并无明显缩放，核心为 **行位置的 Lookahead 过阻尼跟随（stiffness 170..220, damping 1.1）+ 距离 alpha 1/0.45/0.28/0.18 + blur 6dp + 词级进度遮罩**。
  - 用户明确反馈“并没有复刻”“苹果音乐好像没有你现在这些放大啥的效果”，要求去除放大、精准对齐。
- **调研**：Apple Music 官方（WWDC23 `UISpringTimingParameters`）与 AMLL Web 实现均以位置弹簧 + 透明度为主，`scale` 仅为 AMLL 的装饰性增强，非 Apple 官方；AMLL 的 `SpringPlacementModifier` 本身无 scale。

## Requirements
- **R1 — 去除放大**：`NativeKaraokeLine` 的 `scale` 由 `1.05/0.92` 改为恒 `1.0`，移除 `animateFloatAsState` 的 scale 动画与 `graphicsLayer(scaleX/scaleY)` 的缩放，仅保留 `alpha` 与 `blur` 的弹簧。
- **R2 — 保留 Apple 位置弹簧**：`NativeLyricsPanel` 的 `LookaheadScope + AppleSpringPlacement(stiffness 170..220, damping 1.1)` 保持不变，确保行位移仍为 Apple-like 过阻尼跟随（200ms 回位无振荡）。
- **R3 — 保留词级遮罩**：逐词 `fraction` 的 `AnnotatedString lerp` 与长词字符 `baselineShift` 微浮动保持不变（与放大无关）。
- **R4 — 可选：微调 alpha/blur 以补偿去除放大后的视觉层次**：`alpha` 保持 `1/0.45/0.28/0.18` 的 `spring(280/0.92)`，`blur` 保持 `spring(300/0.9)`，无需新增参数。

## Acceptance Criteria
- [ ] AC1 — 当前行与非当前行无缩放差异（`scale` 恒 1.0），视觉上与 Apple Music 官方一致，仅透明度/模糊区分焦点
- [ ] AC2 — 列表滚动与行位移仍保留 Apple-like 弹簧跟随（Lookahead 过阻尼），手势拖动后回中同样弹簧
- [ ] AC3 — 词级进度遮罩与字符微浮动保持，`assembleMusesDebug` / `lint` / `test` 通过，MuMu 验收通过

## Out of Scope
- 词级遮罩逻辑、背景、解析等其他改动
- 重新引入 WebView/lyrics-ui

## Open Questions
（无）

## Decisions
- D1 — 去除放大，回归 Apple Music 无缩放的克制效果（2026-09-02 确认）

## Notes
- 去除 `scale` 后，若层次感不足，可通过微调 `alpha` 的 `stiffness` 从 280→320 补偿，但首版保持原值以最小变更
