# Compose 手势驱动动画研究报告

> **研究目标**: 为音乐播放器沉浸页面实现手势驱动弹出效果
> **日期**: 2026-04-28

---

## 1. 项目现状分析

### 当前实现 (MainActivity.kt:198-214)

```kotlin
AnimatedVisibility(
    visible = showNowPlaying,
    enter = slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(durationMillis = 300)
    ),
    exit = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(durationMillis = 300)
    )
) {
    NowPlayingScreen(
        onDismiss = { showNowPlaying = false },
        viewModel = playerViewModel
    )
}
```

**问题**:
1. 动画僵硬 — 无手势交互，只有固定时长的滑动
2. PlayerBar 重渲染 — `showNowPlaying` 状态变化触发 Column 重组
3. 动画不可中断 — 用户无法中途取消

### 现有 QueueSheet (QueueSheet.kt)

```kotlin
val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState
) { ... }
```

---

## 2. 方案对比

### 方案 A: ModalBottomSheet / BottomSheetScaffold

| 优点 | 缺点 |
|------|------|
| Material3 内置组件，经过充分测试 | 全屏沉浸体验不够灵活 |
| 内置拖拽手柄、锚点、手势支持 | 标准 sheet 行为可能不符合音乐 App UX |
| 自动处理键盘、系统栏 | 背景 scrim 始终存在 |
| 可访问性支持完善 | 自定义动画受限 |

**API 参考**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberModalBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    initialValue: SheetValue = Hidden,
    skipExpandedState: Boolean = false
): SheetState

// 使用示例
val sheetState = rememberModalBottomSheetState()
ModalBottomSheet(
    onDismissRequest = { showSheet = false },
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() }
) {
    // 内容
}
```

**适用场景**: 标准工具栏、表单、简单抽屉

---

### 方案 B: AnchoredDraggable + AnimatedVisibility (推荐)

| 优点 | 缺点 |
|------|------|
| 精确控制锚点位置 | 比 BottomSheet 需要更多代码 |
| 流畅的手势驱动动画 | 需要手动管理状态 |
| 支持速度阈值判定 | 需要与 AnimatedVisibility 协调 |
| Material3 基础组件 | 实验性 API |

**API 参考** (Material3 1.2+):
```kotlin
enum class DragValue { Collapsed, Expanded }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureDrivenScreen() {
    val density = LocalDensity.current
    val screenHeight = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    val state = rememberAnchoredDraggableState(
        initialValue = DragValue.Expanded,
        anchors = DraggableAnchors {
            DragValue.Collapsed at screenHeight
            DragValue.Expanded at 0f
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
        velocityThreshold = with(density) { 125.dp.toPx() }
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(0, state.requireOffset().roundToInt()) }
            .anchoredDraggable(state, Orientation.Vertical)
    ) {
        // 内容
    }
}
```

**完整示例**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersivePlayerOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    val dragState = rememberAnchoredDraggableState(
        initialValue = if (visible) DragValue.Expanded else DragValue.Collapsed,
        anchors = DraggableAnchors {
            DragValue.Expanded at 0f
            DragValue.Collapsed at screenHeightPx
        },
        positionalThreshold = { it * 0.4f },
        velocityThreshold = with(density) { 400.dp.toPx() }
    )

    // 状态同步
    LaunchedEffect(visible) {
        dragState.animateTo(if (visible) DragValue.Expanded else DragValue.Collapsed)
    }

    // 拖拽完成回调
    LaunchedEffect(dragState.currentValue) {
        if (dragState.currentValue == DragValue.Collapsed && visible) {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragState.requireOffset().roundToInt()) }
            .anchoredDraggable(dragState, Orientation.Vertical)
    ) {
        content()
    }
}
```

---

### 方案 C: 自定义 pointerInput + offset 动画

| 优点 | 缺点 |
|------|------|
| 最大灵活性 | 代码量最多 |
| 可实现任意手势模式 | 需要手动处理物理、吸附、中断 |
| 不依赖实验性 API | 容易出现 bug（手势边界情况） |
| | 无内置可访问性支持 |

**API 参考**:
```kotlin
@Composable
fun CustomGestureOverlay(
    onDismiss: () -> Unit
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val screenHeightPx = LocalConfiguration.current.screenHeightDp.dp.value

    val animatable = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        scope.launch {
                            // 根据位置和速度决定目标
                            val target = if (offsetY > screenHeightPx * 0.5f) {
                                screenHeightPx
                            } else {
                                0f
                            }
                            animatable.animateTo(target)
                            if (target > 0) onDismiss()
                        }
                    },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    offsetY = (offsetY + dragAmount).coerceIn(0f, screenHeightPx)
                }
            }
    ) {
        // 内容
    }
}
```

---

## 3. 主流音乐 App 交互模式

### Spotify 模式
- MiniPlayer 固定在底部
- 点击展开为全屏
- 下滑收起（速度敏感）
- 拖拽过程中 MiniPlayer 内容可见
- 使用手势驱动动画，非传统 BottomSheet

### Apple Music 模式
- 类似 Spotify
- 额外功能：全屏下左右滑动切换曲目
- 手势完成时触觉反馈

### 网易云音乐模式
- Bottom sheet 样式
- 支持半展开（半屏）
- 可拖拽关闭
- 背景模糊效果

### 共通要素
1. **速度阈值判定**: 快速滑动即使距离短也触发动作
2. **中断支持**: 手势中途可改变方向
3. **视觉连续性**: MiniPlayer 在过渡期间保持可见
4. **触觉反馈**: 可选但提升体验

---

## 4. 推荐方案

### 推荐: AnchoredDraggable + AnimatedVisibility

**理由**:
1. AnchoredDraggable 提供手势处理 + 物理动画
2. 可与现有 AnimatedVisibility 结合用于入场动画
3. 比 ModalBottomSheet 更灵活，适合全屏 UX
4. 比自定义 pointerInput 更健壮
5. Material3 实验性 API，但正趋于稳定

### 实现策略

```
┌─────────────────────────────────────┐
│           屏幕可见区域               │
│  ┌─────────────────────────────┐    │
│  │                             │    │
│  │     NowPlayingScreen        │    │  ← AnchoredDraggable
│  │     (offset 驱动位置)        │    │
│  │                             │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │        PlayerBar            │    │  ← 固定底部
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

**关键点**:
1. NowPlayingScreen 使用 `offset` 定位，而非 `AnimatedVisibility`
2. PlayerBar 始终渲染，通过 `alpha` 或 `zIndex` 控制可见性
3. 使用 `derivedStateOf` 避免不必要重组

---

## 5. PlayerBar 重渲染解决方案

### 问题分析

当前代码结构：
```kotlin
Column(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier.weight(1f)) {
        // ... 内容
        Box(modifier = Modifier.clickable { showNowPlaying = true }) {
            PlayerBar(...)  // ← 状态变化时重组
        }
    }

    AnimatedVisibility(visible = showNowPlaying) {  // ← 状态变化
        NowPlayingScreen(...)
    }
}
```

### 解决方案 1: 分离动画状态

```kotlin
// 使用 derivedStateOf 减少重组
val playerAlpha by remember {
    derivedStateOf {
        if (showNowPlaying) 0f else 1f
    }
}

PlayerBar(
    modifier = Modifier.alpha(playerAlpha),
    ...
)
```

### 解决方案 2: movableContentOf

```kotlin
val playerBarContent = remember {
    movableContentOf {
        PlayerBar(
            viewModel = playerViewModel,
            onQueueClick = { showQueue = true }
        )
    }
}

// 在需要的位置复用，避免重组
playerBarContent()
```

### 解决方案 3: 状态提升 + key

```kotlin
// PlayerBar 使用独立的 ViewModel 状态，不依赖 showNowPlaying
key(playerViewModel.state.value.trackId) {
    PlayerBar(viewModel = playerViewModel)
}
```

---

## 6. 代码文件索引

### 项目文件
| 文件 | 用途 |
|------|------|
| `app/src/main/java/com/example/muses/MainActivity.kt` | 主动画逻辑 (198-214行) |
| `app/src/main/java/com/example/muses/ui/screens/PlayerBar.kt` | Mini Player |
| `app/src/main/java/com/example/muses/ui/screens/NowPlayingScreen.kt` | 全屏播放页 |
| `app/src/main/java/com/example/muses/ui/screens/QueueSheet.kt` | 现有 BottomSheet 实现 |

### Spec 文件
| 文件 | 内容 |
|------|------|
| `.trellis/tasks/archive/2026-04/04-10-immersive-now-playing/prd.md` | 原始功能规格 |
| `.trellis/tasks/04-28-immersive-animation/prd.md` | 当前任务规格 |

---

## 7. 相关 Material3 API

### AnchoredDraggable 相关
```kotlin
// androidx.compose.material3:material3:1.2.0+
@ExperimentalMaterial3Api
@Composable
fun <T : Any> rememberAnchoredDraggableState(
    initialValue: T,
    anchors: DraggableAnchors<T>,
    positionalThreshold: (totalDistance: Float) -> Float,
    velocityThreshold: Float,
    snapAnimationSpec: AnimationSpec<Float> = spring(),
    decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
): AnchoredDraggableState<T>

@ExperimentalMaterial3Api
fun Modifier.anchoredDraggable(
    state: AnchoredDraggableState<*>,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    interactionSource: MutableInteractionSource? = null
): Modifier
```

### ModalBottomSheet 相关
```kotlin
@ExperimentalMaterial3Api
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable () -> Unit = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: WindowInsets = ...,
    content: @Composable ColumnScope.() -> Unit
)
```

---

## 8. 实现建议

### 渐进式实现路径

1. **Phase 1**: 保持 AnimatedVisibility，添加手势支持
   - 在 NowPlayingScreen 内部添加 `pointerInput` 处理下滑
   - 验证手势交互的流畅度

2. **Phase 2**: 迁移到 AnchoredDraggable
   - 替换 AnimatedVisibility 为 offset 驱动
   - 实现锚点吸附动画

3. **Phase 3**: 优化 PlayerBar 渲染
   - 使用 movableContentOf 或状态分离
   - 确保过渡期间视觉连续

### 测试关注点
- [ ] 快速滑动是否触发正确动作
- [ ] 中途改变方向是否正常处理
- [ ] 慢速拖拽超过 50% 是否吸附到目标
- [ ] PlayerBar 在过渡期间是否保持稳定
- [ ] 无障碍服务是否正常工作

---

## 总结

| 方案 | 复杂度 | 灵活性 | 推荐度 |
|------|--------|--------|--------|
| ModalBottomSheet | 低 | 低 | ★★☆ |
| AnchoredDraggable | 中 | 高 | ★★★ |
| pointerInput | 高 | 最高 | ★☆☆ |

**最终建议**: 使用 **AnchoredDraggable** 方案，平衡开发成本与用户体验。
