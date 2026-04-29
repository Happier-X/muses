# Research: Avoid Recomposition Due to State Changes

- **Query**: Compose 中如何避免状态变化导致重组，特别是针对 `showNowPlaying` 状态变化导致 PlayerBar 重新渲染的问题
- **Scope**: mixed (internal code analysis + Compose optimization patterns)
- **Date**: 2026-04-28

## Findings

### Files Found

| File Path | Description |
|---|---|
| `app/src/main/java/com/example/muses/MainActivity.kt` | MainContent Composable，包含 `showNowPlaying` 状态和 PlayerBar/NowPlayingScreen 布局 |
| `app/src/main/java/com/example/muses/ui/screens/PlayerBar.kt` | 底部播放条 Composable，通过 `collectAsStateWithLifecycle` 收集 PlayerState |
| `app/src/main/java/com/example/muses/ui/screens/NowPlayingScreen.kt` | 沉浸式播放页面 Composable |
| `app/src/main/java/com/example/muses/ui/viewmodel/PlayerViewModel.kt` | PlayerState 状态管理，包含频繁更新的 `positionMs` 字段 |
| `app/src/main/java/com/example/muses/ui/util/AlbumArtLoader.kt` | `rememberAlbumArt` 工具函数，使用 `LaunchedEffect` 加载图片 |

### Root Cause Analysis: Why PlayerBar Recomposes

#### 1. Shared ViewModel with Frequent Updates

`PlayerViewModel` 在 `startPositionPolling()` 中每 250ms 更新一次状态 (`PlayerViewModel.kt:274-296`):

```kotlin
private fun startPositionPolling() {
    positionJob = viewModelScope.launch {
        while (isActive) {
            // ...
            _state.update {
                it.copy(
                    positionMs = pos,
                    durationMs = dur,
                    currentLyric = currentLyric
                )
            }
            delay(POSITION_POLL_MS)
        }
    }
}
```

**问题**: `positionMs` 每秒更新 4 次，而 PlayerBar 通过 `collectAsStateWithLifecycle()` 收集状态。这会触发 Composable 重组。

#### 2. State Location in Parent Composable

`MainActivity.kt:94`:
```kotlin
var showNowPlaying by remember { mutableStateOf(false) }
```

`showNowPlaying` 状态在 `MainContent` 中定义，但 `MainContent` 还包含:
- `Column` 包含 `Box(contentAlignment = Alignment.BottomCenter)`
- `PlayerBar` 位于 `Box` 内
- `AnimatedVisibility(visible = showNowPlaying)` 包裹 `NowPlayingScreen`

**问题**: 当 `showNowPlaying` 变为 true 时，Compose 会重新评估整个 `MainContent` 的 composition scope，可能影响 PlayerBar。

#### 3. Missing Stability Annotations

`PlayerState` data class (`PlayerViewModel.kt:34-52`) 未标注 `@Stable` 或 `@Immutable`：

```kotlin
data class PlayerState(
    val isPlaying: Boolean = false,
    val title: String? = null,
    // ...
    val positionMs: Long = 0L,  // 频繁变化
    val currentLyric: String? = null  // 频繁变化
)
```

**影响**: 编译器将此类视为不稳定类型，导致任何字段变化都可能触发重组。

#### 4. `rememberAlbumArt` Implementation

`AlbumArtLoader.kt:48`:
```kotlin
val bitmapState = remember { mutableStateOf<Bitmap?>(null) }
```

使用对象引用作为 key，当 `albumArtUri` 相同时不会触发重组，但如果 URI 变化会触发。

### Techniques to Avoid Recomposition

#### Technique 1: `key()` Composable

**用途**: 为特定子组件创建稳定的重组边界。

```kotlin
// 不推荐: 整个 Column 可能重组
Column {
    PlayerBar()
    AnimatedVisibility(visible = showNowPlaying) {
        NowPlayingScreen()
    }
}

// 推荐: 使用 key() 隔离重组
Column {
    key("player_bar") {
        PlayerBar()
    }
    AnimatedVisibility(visible = showNowPlaying) {
        NowPlayingScreen()
    }
}
```

**适用场景**: 当子组件有自己的稳定状态，不需要响应父组件状态变化时。

#### Technique 2: `derivedStateOf`

**用途**: 从多个状态派生新状态，避免不必要的重组。

```kotlin
// 问题: 每次 positionMs 变化都会触发 displayPosition 计算
val displayPosition = state.positionMs.toFloat()

// 解决: 使用 derivedStateOf，只有在值真正变化时才重组
val displayPosition by remember {
    derivedStateOf { state.positionMs.toFloat() }
}
```

**注意**: `derivedStateOf` 主要用于派生 UI 状态（如滚动位置），对于 ViewModel 状态已在 ViewModel 层处理。

#### Technique 3: State Hoisting

**用途**: 将状态提升到最小必要的层级。

```kotlin
// 问题: showNowPlaying 在 MainContent 顶层定义
@Composable
fun MainContent() {
    var showNowPlaying by remember { mutableStateOf(false) }
    // ...
}

// 解决: 如果只有 PlayerBar 需要响应，可以考虑拆分
@Composable
fun MainContent() {
    // 仅包含 PlayerBar 的区域
    PlayerBarSection()
    // NowPlayingScreen 的状态在独立 scope 管理
}
```

#### Technique 4: Separate Composable Scope

**用途**: 将稳定组件和动态组件放在不同的 scope。

```kotlin
@Composable
fun MainContent() {
    val playerViewModel: PlayerViewModel = viewModel()

    // 稳定区域 - 不会被 showNowPlaying 变化影响
    StablePlayerArea(
        viewModel = playerViewModel,
        onQueueClick = { /* ... */ }
    )

    // 动态区域
    AnimatedVisibility(visible = showNowPlaying) {
        NowPlayingScreen(
            onDismiss = { showNowPlaying = false },
            viewModel = playerViewModel
        )
    }
}

@Composable
private fun StablePlayerArea(
    viewModel: PlayerViewModel,
    onQueueClick: () -> Unit
) {
    // 独立的 composition scope
}
```

#### Technique 5: Subcomposition with `CompositionLocal`

**用途**: 将频繁变化的状态（如 positionMs）与稳定状态分离。

```kotlin
// 定义 CompositionLocal
val LocalPlayerPosition = compositionLocalOf { 0L }

// 提供者
@Composable
fun PlayerStateProvider(
    state: PlayerState,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalPlayerPosition provides state.positionMs) {
        content()
    }
}

// 消费者 - positionMs 变化不会触发整个组件重组
@Composable
fun PlayerBar() {
    val position = LocalPlayerPosition.current
    // 仅在 position 真正需要的地方使用
}
```

**注意**: Subcomposition 会创建额外的 composition 层级，可能影响性能，需谨慎使用。

#### Technique 6: Modifier-based Stability

**用途**: 使用 `Modifier` 参数接收稳定的 lambda。

```kotlin
@Composable
fun PlayerBar(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel(),
    onQueueClick: () -> Unit = {}  // stable lambda
) { ... }
```

### Applicable Patterns for Current Codebase

根据当前代码分析，以下是最适用的方案：

#### 方案 1: 拆分 `MainContent` 布局 (最简单)

将 PlayerBar 和 NowPlayingScreen 放在独立的作用域:

```kotlin
@Composable
fun MainContent() {
    val playerViewModel: PlayerViewModel = viewModel()
    var showNowPlaying by remember { mutableStateOf(false) }
    // ...
    Column {
        // 屏幕内容区域
        ScreenContent(selectedItem = selectedItem, ...)

        // PlayerBar 区域 - 独立 Box
        Box(contentAlignment = Alignment.BottomCenter) {
            PlayerBar(viewModel = playerViewModel, ...)
        }
    }

    // Overlay - 独立于 Column 层级
    AnimatedVisibility(visible = showNowPlaying) {
        NowPlayingScreen(onDismiss = { showNowPlaying = false }, ...)
    }
}
```

#### 方案 2: 为 PlayerState 添加 `@Stable`

```kotlin
@Stable
data class PlayerState(
    val isPlaying: Boolean = false,
    val title: String? = null,
    // ...
    val positionMs: Long = 0L,
    val currentLyric: String? = null
)
```

**注意**: `@Stable` 会让 Compose 相信只要引用相等，内容就不会变化。但 `positionMs` 频繁变化，添加 `@Stable` 可能反而增加重组频率。

#### 方案 3: 使用 `SnapshotStateMap` 或分离 position 状态

```kotlin
// 方案 A: 将频繁变化的字段分离
data class PlayerUiState(
    val title: String? = null,
    val artist: String? = null,
    // 稳定字段
)

data class PlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

// 方案 B: PlayerBar 只订阅需要的状态
@Composable
fun PlayerBar(viewModel: PlayerViewModel) {
    val trackInfo by viewModel.trackInfo.collectAsStateWithLifecycle()
    // position 在单独的 coroutine 中处理，不触发重组
}
```

### Related Specs

- `.trellis/tasks/04-28-immersive-animation/prd.md` — 当前任务的需求定义

### External References

- [Jetpack Compose Performance](https://developer.android.com/develop/ui/compose/performance) — Google 官方性能优化指南
- [Understanding Compose state](https://developer.android.com/develop/ui/compose/state) — Compose 状态管理最佳实践
- [Stability in Compose](https://developer.android.com/develop/ui/compose/performance/stability) — 稳定性配置和 `@Stable`、`@Immutable` 注解

## Caveats / Not Found

- 外部搜索工具不可用，无法获取最新的 Compose 最佳实践文档
- `derivedStateOf` 在当前代码库中未使用，需要进一步验证其适用性
- Subcomposition 可能引入额外的性能开销，需要实际测试验证效果
