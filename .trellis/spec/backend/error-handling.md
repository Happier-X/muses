# Error Handling

> How errors are caught, logged, and propagated in this project.

---

## Overview

This project uses two error handling patterns depending on context:
1. **`Result<T>` return type** — for repository/network operations that callers handle explicitly
2. **`try-catch` with silent fallback** — for non-critical operations where a default is acceptable
3. **`Log.e/w` + no exception re-throw** — for listener callbacks and event handlers where crashing is inappropriate

---

## Repository Layer: `Result<T>`

Repository functions that perform I/O return `Result<List<T>>` or similar.
Callers use `.fold()`, `.getOrNull()`, or `.getOrDefault()`.

```kotlin
// WebdavRepository.kt
fun listDirectory(config: WebdavConfig, path: String): Result<List<WebdavItem>> {
    return try {
        // ... network call ...
        Result.success(items)
    } catch (e: IOException) {
        Log.e(TAG, "Network error for $path", e)
        Result.failure(e)
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error for $path", e)
        Result.failure(e)
    }
}
```

Callers handle gracefully:
```kotlin
val result = webdavRepo.listDirectory(config, path)
result.fold(
    onSuccess = { items -> /* update UI */ },
    onFailure = { e -> Log.w(TAG, "Skipping directory $path: ${e.message}") }
)
```

Key files:
- `app/src/main/java/com/example/muses/data/repository/WebdavRepository.kt`
- `app/src/main/java/com/example/muses/data/repository/MetadataExtractor.kt`

---

## Non-Critical Operations: Silent Suppression

Operations that can't affect core functionality use `try-catch` with a default/fallback value.
No exception is ever propagated to the caller.

```kotlin
// MetadataExtractor.kt — metadata extraction failure is non-critical
fun extractFromUri(context: Context, uri: Uri): Metadata? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        readMetadata(retriever)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to extract metadata from $uri", e)
        null  // Caller uses a default title/empty metadata instead
    } finally {
        try { retriever.release() } catch (_: Exception) {}
    }
}
```

---

## Coroutine Scope: `suspend` with Exception Logging

Suspend functions that run in `viewModelScope` catch exceptions and return safe defaults or log and re-throw.

```kotlin
suspend fun listAudioFiles(config: WebdavConfig, path: String, recursive: Boolean): Result<List<WebdavItem>> =
    withContext(Dispatchers.IO) {
        try {
            val audioFiles = mutableListOf<WebdavItem>()
            collectAudioFiles(config, path, recursive, audioFiles)
            Result.success(audioFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list audio files in $path", e)
            Result.failure(e)
        }
    }
```

---

## Listener Callbacks: Never Throw

Player listener callbacks (`Player.Listener` overrides) never re-throw. They log and update state safely.

```kotlin
// PlayerViewModel.kt — PlayerListener
override fun onPlayerError(error: PlaybackException) {
    Log.e(TAG, "Playback error: ${error.message}", error)
    _state.update {
        it.copy(errorMessage = error.message ?: "Playback error", isPlaying = false)
    }
}
```

### Persistence in Listener Callbacks

When a listener callback triggers a persistence update (e.g. play count in `onMediaItemTransition`), the write must be dispatched to `Dispatchers.IO` — listener callbacks run on the main thread.

```kotlin
override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    syncState()
    mediaItem?.mediaId?.let { trackId ->
        updatePlayMetadata(trackId)  // launches viewModelScope on IO internally
    }
}
```

**Gotcha**: Do not call `TrackStore.saveTracks()` synchronously inside a `Player.Listener` override — it performs disk I/O and will block the main thread.

---

## Forbidden Patterns

- Do **not** `throw` from `Player.Listener` overrides — causes crashes in ExoPlayer's thread
- Do **not** silently swallow `IOException` in network-heavy code — callers need to know if a request failed
- Do **not** use `e.printStackTrace()` — use `Log.e(TAG, msg, e)` instead
- Do **not** return `null` from functions that also return `Result.failure()` — pick one pattern per function
