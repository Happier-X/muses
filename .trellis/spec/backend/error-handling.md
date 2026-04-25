# Error Handling

> How errors are handled in this Android/Kotlin project.

---

## Core Patterns

### Sealed Class Results

Use Kotlin `sealed class`/`sealed interface` for result types instead of throwing exceptions for expected failures:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
}
```

Or use the standard `kotlin.Result<T>` with `.getOrNull()` and `.exceptionOrNull()`.

### UI Error State

Each screen's UI state should include an error variant:

```kotlin
sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(val track: Track, val isPlaying: Boolean) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}
```

The composable renders each state explicitly:

```kotlin
when (val state = uiState) {
    is PlayerUiState.Loading -> CircularProgressIndicator()
    is PlayerUiState.Ready -> PlayerContent(state.track, state.isPlaying)
    is PlayerUiState.Error -> ErrorBanner(state.message, onRetry = { viewModel.retry() })
}
```

---

## Exception Handling

### In Coroutines

Use `try/catch` inside `viewModelScope.launch` for async operations:

```kotlin
viewModelScope.launch {
    try {
        val tracks = repository.loadTracks()
        _uiState.value = PlayerUiState.Ready(tracks.first(), false)
    } catch (e: IOException) {
        _uiState.value = PlayerUiState.Error("Network error: ${e.message}")
    } catch (e: Exception) {
        _uiState.value = PlayerUiState.Error("Unexpected error")
        Log.e(TAG, "Failed to load tracks", e)
    }
}
```

### In Composable (LaunchedEffect)

```kotlin
LaunchedEffect(key) {
    try {
        val result = someAsyncOperation()
        // handle success
    } catch (e: Exception) {
        // handle error
    }
}
```

---

## Logging

Use `android.util.Log` for Android logging:

| Level | When to use |
|-------|-------------|
| `Log.e(TAG, msg, throwable)` | Unexpected errors, crashes |
| `Log.w(TAG, msg)` | Recoverable issues, degraded functionality |
| `Log.i(TAG, msg)` | Important lifecycle events (playback start/stop, network requests) |
| `Log.d(TAG, msg)` | Debug-only detail; strip in release builds |

Define a `TAG` constant per class:
```kotlin
companion object {
    private const val TAG = "PlayerViewModel"
}
```

---

## Input Validation

Validate user input early, at the ViewModel or composable level. Show errors inline, not as toasts:

```kotlin
// In composable
var serverUrl by remember { mutableStateOf("") }
var urlError by remember { mutableStateOf<String?>(null) }
// ...
OutlinedTextField(
    value = serverUrl,
    isError = urlError != null,
    supportingText = urlError?.let { { Text(it) } },
    onValueChange = {
        serverUrl = it
        urlError = if (it.isBlank()) "URL cannot be empty" else null
    }
)
```

---

## Forbidden Patterns

- **Do not** use `try { } catch (e: Exception) { }` (empty catch block) — always at least log
- **Do not** throw exceptions in composable functions — composables should be side-effect-free
- **Do not** show raw exception messages to users — always map to user-friendly strings
- **Do not** catch `Throwable` (catches JVM errors like `OutOfMemoryError`)
- **Do not** suppress exceptions silently in coroutines — use `CoroutineExceptionHandler` or let them propagate to the UI state

---

## Common Mistakes

- Showing `Toast` for errors that should be persistent (use a banner or error state instead)
- Catching exceptions too broadly (catch `IOException` vs `Exception` specifically)
- Not handling empty/null states separately from error states ("no results" vs "failed to load")
