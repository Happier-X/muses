# Compose Conventions

> Jetpack Compose patterns and best practices for this project.

---

## Theme Usage

The project uses a custom `MusesTheme` wrapper around Material3. Always wrap content in `MusesTheme`:

```kotlin
// MainActivity.kt — existing pattern
setContent {
    MusesTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // screen content with Modifier.padding(innerPadding)
        }
    }
}
```

Theme supports:
- **System dark mode** (auto-detected via `isSystemInDarkTheme()`)
- **Dynamic color** on Android 12+ (Material You)
- Static fallback colors defined in `Color.kt`

**Rule**: Never use bare `MaterialTheme` directly. Always use `MusesTheme`.

---

## State Management

### Prefer these patterns (in order of preference):

1. **`remember` + `mutableStateOf`** for local UI state (form inputs, expanded/collapsed toggles):
   ```kotlin
   var isPlaying by remember { mutableStateOf(false) }
   ```

2. **`StateFlow` + `collectAsState()`** for ViewModel-driven state:
   ```kotlin
   val uiState by viewModel.uiState.collectAsStateWithLifecycle()
   ```

3. **`derivedStateOf`** for computed values that should avoid recomposition:
   ```kotlin
   val isQueueEmpty by remember { derivedStateOf { queue.isEmpty() } }
   ```

### Avoid:
- Passing mutable state objects across screen boundaries — hoist state instead
- Using `!!` (non-null assertion) on state values — use `?.let {}` or null checks
- Long-running operations in composable bodies — use `LaunchedEffect` or ViewModel coroutines

---

## Composable Naming

| Type | Convention | Example |
|------|-----------|---------|
| Screen composable | `<Feature>Screen` | `LibraryScreen`, `PlayerScreen` |
| Reusable component | Descriptive noun | `MusicListItem`, `PlaybackControls`, `SearchBar` |
| Content wrapper | `<Name>Content` | `PlayerContent` |
| Dialog | `<Name>Dialog` | `PlaylistCreateDialog` |

---

## Modifier Patterns

- **Always accept a `modifier` parameter** for reusable composables, defaulting to `Modifier`:
  ```kotlin
  @Composable
  fun MusicListItem(
      song: Song,
      modifier: Modifier = Modifier,
      onClick: () -> Unit = {}
  ) {
      // use modifier as outermost modifier
  }
  ```
- The modifier parameter should be applied as the **first/outermost** modifier in the root composable
- Use `Modifier.fillMaxWidth()` for list items, not fixed widths
- Use `Modifier.padding(innerPadding)` from `Scaffold` to avoid system bar overlap

---

## Preview Usage

Provide a preview for every screen and reusable component:

```kotlin
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PlayerScreenPreview() {
    MusesTheme {
        PlayerScreen()
    }
}
```

Use `@Preview` with `uiMode` for dark mode variants when relevant.

---

## Edge-to-Edge

`enableEdgeToEdge()` is called in `MainActivity.onCreate()`. All screens must handle system bar insets properly — typically via `Scaffold` padding or `WindowInsets` APIs.

---

## Performance Rules

- Use `LazyColumn` instead of `Column` for scrollable lists with more than ~20 items
- Use `key` parameter on `LazyColumn` items for stable identity
- Avoid recomposing large subtrees — lift state to the narrowest scope needed
- Use `@Stable` or `@Immutable` annotations on data classes passed to composables
- Use `remember` for expensive calculations, not every trivial value

---

## Persistent Player Bar Pattern

For screen layouts that share a persistent player bar at the bottom, use this structure:

```kotlin
Column(Modifier.fillMaxSize()) {
    // Tab content — takes remaining space
    Box(Modifier.weight(1f)) {
        // Screen content using Scaffold or direct layout
    }
    // Player bar — always visible at bottom
    PlayerBar()
}
```

This keeps the player bar visible regardless of which tab is selected. The player bar observes `PlayerViewModel` state to show current track info and playback controls.

---

## Common Mistakes

- **Forgetting `remember`** on state that should survive recomposition → state resets unexpectedly
- **Hoisting state too high** → entire screen recomposes on a small state change
- **Using `Column` for large lists** → OOM and scroll jank
- **Not providing a `modifier` parameter** → callers can't control layout
- **Hardcoding `Modifier.padding(16.dp)`** instead of using `innerPadding` from `Scaffold`
- **Using `NavigationBar` with persistent bottom bar** — use `Column` with `Modifier.weight(1f)` for tab content instead of NavHost, otherwise the player bar shifts
