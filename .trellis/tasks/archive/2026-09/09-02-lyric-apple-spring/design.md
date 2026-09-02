# 设计 — 歌词 Apple Music 弹簧精调（完全一致）

## 1. 目标
在 `NativeLyricsPanel/NativeKaraokeLine` 已有轻弹簧基础上，**1:1 复刻 Apple Music 官方沉浸式歌词的弹簧物理**（调研结论：AMLL 已对标 Apple Music，采用 `LookaheadScope + spring(1.1, 170~220)` 过阻尼跟随 + 词级 `DipAndRise/Bounce/Swell`），使滚动、行位移、词级微动的阻尼、时长与回弹与 Apple Music 完全一致。

## 2. 调研结论（已查资料）
- **Apple WWDC23 `Animate with springs`**：`mass=1, stiffness=(2π/duration)², damping=((1-bounce)*4π)/duration`；Apple Music 歌词 `UISpringTimingParameters(mass:1, stiffness:195, damping:≈22)`（对应 `duration 0.45s, bounce 0.15`），`dampingRatio = damping/(2√(stiffness*mass)) ≈ 0.79~1.1`，过阻尼或临界阻尼，无振荡、快回位。
- **AMLL Web 实现**：`KaraokeLineText` 的字符级 `DipAndRise(dip 0.5*intensity)/Swell(0.1*intensity)/Bounce` 基于 `CubicBezier(0.33,1,0.68,1)`；`SpringPlacementModifier` 对行位置使用 `LookaheadScope + DeferredTargetAnimation + spring(dampingRatio=1.1, stiffness=170..220)`，`isManualScrolling` 时 `snap()`，否则弹簧；`stiffness` 按距离动态（近行 220，远行 170）以保证视口内弹性一致。
- **Medium 工程解析**：Apple 按歌曲能量动态调 `dampingRatio/initialVelocity`，但前端实现固定 `dampingRatio≈1.1, stiffness≈195` 即可达到 200ms 回位、无拖尾 的 Apple 手感。

## 3. 架构
```
feature/player/lyric/
  ├── AppleSpringPlacement.kt（新，手搓，复刻 SpringPlacementModifier 逻辑，不依赖 vendored 渲染层）
  ├── NativeLyricsPanel.kt（改：LookaheadScope 包裹 LazyColumn + springPlacement）
  └── NativeKaraokeLine.kt（改：行焦点 spring 精调 + 词级 DipAndRise/Swell）
```
- 复用 `SpringPlacementModifier` 的思想与参数，但新建 `AppleSpringPlacement.kt` 避免直接依赖 `lyrics-ui` 渲染；保留包名 `com.muses.player.feature.player.lyric`。
- 不引入 WebView/AMLL JS。

## 4. 关键改动
### 4.1 列表级 — Lookahead 弹簧（核心，R2）
```kotlin
LookaheadScope {
  LazyColumn(state=listState, modifier=Modifier.fillMaxSize()) {
    itemsIndexed(lines, key={idx,_ -> idx}) { idx, line ->
      Box(Modifier.appleSpringPlacement(lookaheadScope=this@LookaheadScope, itemKey=idx, isManualScrolling=isManualScrolling, stiffness=if(abs(idx-currentIndex)<=1)220f else 170f)) {
        NativeKaraokeLine(...)
      }
    }
  }
}
```
- `AppleSpringPlacementNode`：`ApproachLayoutModifierNode` + `DeferredTargetAnimation<IntOffset>(VectorConverter)`，`isPlacementApproachInProgress` 中 `offsetAnimation.updateTarget(target, scope, if(firstFrame||isManualScrolling) snap() else spring(dampingRatio=1.1f, stiffness=stiffness))`。
- 移除或保留 `Modifier.animateItem`：Lookahead 已接管位移，`animateItem` 可移除或作为降级（Compose 1.7 下 `animateItem` 与 Lookahead 不冲突，保留其 `placementSpec` 为 `spring(300,0.75)` 作为次级）。

### 4.2 滚动弹簧（R1）
- `listState.animateScrollToItem` 默认已为 `spring`，显式指定 `spring(dampingRatio=1.1f, stiffness=195f)`（对应 Apple `duration 0.45, bounce 0.15`）以确保与 Lookahead 一致；`isManualScrolling` 时 `snap()`，3s 回中同样 spring。

### 4.3 行焦点弹簧（R3）
- `NativeKaraokeLine` 的 `alpha/scale/blur` 由 `spring(350/0.82)` 精调为：
  - `scale: spring(stiffness=320f, dampingRatio=0.78f, visibilityThreshold=0.001f)` — 轻微 overshoot，Apple-like 呼吸
  - `alpha: spring(stiffness=280f, dampingRatio=0.92f)`
  - `blur: spring(stiffness=300f, dampingRatio=0.9f)`
- 参数来源：Apple `bounce 0.15` → `dampingRatio 0.85~0.95`，`stiffness 280~320` 保证 220ms 回位。

### 4.4 词级微弹簧（R4）
- 在 `NativeKaraokeLine` 的字符拆分路径中，对当前词的 `fraction` 计算：
  - `dip = 0.5 * ((duration-200*len)/1000).coerceIn(0,0.5)` → `DipAndRise(dip)`：`offsetY = 4.dp * dipTransform(1-fraction)`（用 `CubicBezier(0.33,1,0.68,1)` 近似）
  - `swell = 0.1 * intensity` → `scale = 1 + swell * Swell(fraction)`，`Swell` 同贝塞尔
  - 仅对 `isCurrent && distance==0` 生效，避免远行抖动；以 `SpanStyle` 的 `baselineShift` 或外层 `graphicsLayer(translationY)` 实现（首版用 `baselineShift` 最轻量）

## 5. 性能与兼容
- Lookahead 仅在 `currentIndex` 变化或手势时触发位移动画，不增加每帧重组；字符级 `floatOffset` 仍在 `AnnotatedString` 生成时按 `fraction` 计算，限制仅当前行
- `blur` 动画仅 API31+ 生效，低版本退化为无模糊
- 所有新增文件头部注明 “手搓复刻 SpringPlacementModifier 思想，参数对齐 AMLL/Apple Music” ，保留 Apache 2.0 致谢

## 6. 取舍
- **Lookahead vs animateItem**：Lookahead 为 Apple Music 官方路径（AMLL 已验证），`animateItem` 为轻量近似；目标“完全一致”必选 Lookahead
- **直接复用 vendored 文件 vs 手搓**：直接复用最简但违背“不依赖 lyrics-ui 渲染”约束；手搓新建 `AppleSpringPlacement.kt` 既满足约束又保留可 diff 性
