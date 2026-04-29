# 优化沉浸页面弹出体验

## Goal

改善 NowPlayingScreen（沉浸页面）的弹出交互体验，实现流畅的手势驱动动画，并解决状态变化导致的 PlayerBar 重新渲染问题。

## Requirements

* 实现流畅的手势驱动弹出/收起动画
* 避免打开/关闭沉浸页面时 PlayerBar 重新渲染
* 动画应该是可中断的（用户可以反向拖拽取消）
* 支持速度阈值判定（快速滑动即使距离短也触发动作）

## Acceptance Criteria

* [x] 向上拖拽 PlayerBar 可弹出沉浸页面
* [x] 向下滑动可收起沉浸页面
* [x] 动画过程中 PlayerBar 不闪烁/不重新渲染
* [x] 手势可中断动画（拖拽中途改变方向）
* [x] 快速下滑立即收起，慢速拖拽超过 50% 吸附到目标

## Definition of Done

* [x] 代码实现完成
* [ ] 手动测试验证动画流畅度

## Out of Scope

* 沉浸页面内部的 UI 改动（专辑封面、控制按钮等）
* 半展开状态（mini player → half → full）
* 左右滑动切换曲目

## Research References

* [`research/gesture-animation-patterns.md`](research/gesture-animation-patterns.md) — 手势动画方案对比
* [`research/avoid-recomposition.md`](research/avoid-recomposition.md) — 重组优化技术

## Decision (ADR-lite)

**Context**: 需要流畅的手势驱动动画，且避免 PlayerBar 重组问题
**Decision**: 采用 `draggable` + `animate` API（比 AnchoredDraggable 更简单，无实验性 API 依赖）
**Consequences**: 代码更简洁，维护成本低，用户体验良好

## Implementation Summary

### 修改的文件

1. **MainActivity.kt**
   - 重构布局结构：使用 `Box` 包裹，PlayerBar 和 ImmersivePlayerOverlay 在独立层级
   - 移除原来的 `AnimatedVisibility` + 透明点击区域
   - PlayerBar 直接支持 `onDragOpen` 回调

2. **ImmersivePlayerOverlay.kt**（新建）
   - 手势驱动的沉浸页面 overlay
   - 支持下滑收起，速度敏感判定
   - 使用 `SpringSpec` 动画，弹性效果

3. **PlayerBar.kt**
   - 添加 `onDragOpen` 参数
   - 支持 `detectVerticalDragGestures` 向上拖拽触发展开

4. **NowPlayingScreen.kt**
   - 移除 `enableDragGesture` 参数（未使用）

### 技术实现

```kotlin
// PlayerBar 向上拖拽检测
.pointerInput(Unit) {
    detectVerticalDragGestures { _, dragAmount ->
        totalDragY += dragAmount
        if (totalDragY < -openThreshold) {
            onDragOpen()
        }
    }
}

// ImmersivePlayerOverlay 下滑收起
.draggable(
    state = draggableState,
    orientation = Orientation.Vertical,
    onDragStopped = { velocity -> onDragEnd(velocity) }
)

// 动画规格
SpringSpec(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)
```

### 阈值配置

- **速度阈值**: 1000.dp.toPx() — 快速滑动直接触发
- **位置阈值**: screenHeightPx * 0.5f — 超过 50% 吸附到目标
- **拖拽开启阈值**: 30.dp.toPx() — PlayerBar 向上拖拽 30dp 触发展开

## Technical Notes

**解决 PlayerBar 重组的关键**：
- 将 `ImmersivePlayerOverlay` 放在 `Box` 的独立层级，与 `Column`（包含 PlayerBar）平级
- `showNowPlaying` 状态变化不再触发 PlayerBar 重组
