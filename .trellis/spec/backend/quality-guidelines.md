# Quality Guidelines

> Code quality standards, patterns, and anti-patterns for this project.

---

## Overview

This is a Kotlin/Android music player using Jetpack Compose, ExoPlayer, and MVVM architecture.
Key quality goals: keep data classes immutable, avoid memory leaks, handle errors gracefully.

---

## Required Patterns

### Immutable Data Classes

All data classes should be immutable (no `var` properties) and annotated `@Immutable`.

```kotlin
@Immutable
data class AudioTrack(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val source: TrackSource,
    val sizeBytes: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L
)
```

**Why**: Compose state equality checks rely on value equality. Immutable objects prevent subtle UI bugs.

### Compose State: `StateFlow` + `collectAsState`

ViewModels expose state via `StateFlow`. Screens collect via `state.collectAsState()`.

```kotlin
// ViewModel
val state: StateFlow<PlayerState> = _state.asStateFlow()

// Composable
val playerState by playerViewModel.state.collectAsState()
```

Do **not** expose `LiveData` — this project uses Kotlin coroutines throughout.

### `MediaController` Null Safety

`MediaController` may not be connected when first accessed. Always null-check:

```kotlin
fun playTrack(mediaItem: MediaItem) {
    val controller = mediaController ?: run {
        Log.w(TAG, "MediaController not ready, retrying connection")
        connectToService()
        return
    }
    controller.setMediaItem(mediaItem)
    controller.prepare()
}
```

### Pattern: `rememberAlbumArt` Async Bitmap Loading

For album art saved as local file URIs (`file:///data/.../album_art/{hash}.jpg`), always load asynchronously off the main thread. Use the `rememberAlbumArt` composable from `ui/util/AlbumArtLoader.kt`:

```kotlin
val bitmap = rememberAlbumArt(track.albumArtUri, targetSizePx = 96)
if (bitmap != null) {
    Image(bitmap = bitmap.asImageBitmap(), ...)
} else {
    Icon(Icons.Default.MusicNote, ...)
}
```

**Why**: `BitmapFactory.decodeFile()` is slow and runs on the calling thread. Calling it inside `remember { }` blocks the main thread and causes jank on large images. `rememberAlbumArt` uses `LaunchedEffect` + `Dispatchers.IO` + power-of-two downsampling (avoids loading 3000×3000 JPEG at full resolution into memory).

Key behaviors:
- Runs on `Dispatchers.IO`, result delivered via Compose state
- Uses `inSampleSize` to downsample; never allocates full-resolution pixel buffer
- Falls back to `null` (icon placeholder) on any decode error
- Re-triggers when `albumArtUri` changes (via `LaunchedEffect` key)

**Anti-pattern** — do not do this:
```kotlin
// BAD: blocks main thread, no downsampling, no error safety
val bitmap = remember(albumArtUri) {
    albumArtUri?.path?.let { BitmapFactory.decodeFile(it) }
}
```

---

## Pattern: Metadata Enrichment in PlayerViewModel

When enriching track metadata from `MediaMetadataRetriever`, always use `copy()` on the existing track — never create a new instance:

```kotlin
persistTrackUpdate(trackId) { existing ->
    existing.copy(
        title = metadata.title?.takeIf { it.isNotBlank() } ?: existing.title,
        artist = metadata.artist?.takeIf { it.isNotBlank() } ?: existing.artist,
        album = metadata.album?.takeIf { it.isNotBlank() } ?: existing.album,
        durationMs = metadata.durationMs?.takeIf { it > 0 } ?: existing.durationMs,
        albumArtUri = metadata.albumArtUri ?: existing.albumArtUri
    )
}
```

**Why**: `AudioTrack` carries `playCount`, `lastPlayedAt`, `sizeBytes`, and `source` that must be preserved. Creating a new `AudioTrack(...)` instead of `copy()` loses those fields.

Also: listen to **both** `onMediaItemTransition` (for track change) and `onMediaMetadataChanged` (for stream-level metadata parsed by ExoPlayer). `onMediaItemTransition` alone is insufficient — `onMediaMetadataChanged` captures ID3/Vorbis tags that ExoPlayer parses asynchronously.

---

## Schema Evolution in JSON Persistence

When adding fields to persisted data classes, always provide defaults and use `opt*` methods:

```kotlin
AudioTrack(
    // ...
    playCount = json.optInt("playCount", 0),    // Default 0 for old records
    lastPlayedAt = json.optLong("lastPlayedAt", 0L)
)
```

### Playback Metadata Updates

When a track starts playing, update `playCount` and `lastPlayedAt` via the load-map-save pattern in `onMediaItemTransition`. The update must run on `Dispatchers.IO`:

```kotlin
override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    mediaItem?.mediaId?.let { trackId ->
        updatePlayMetadata(trackId)  // Increments playCount, sets lastPlayedAt
    }
}
```

**Why**: `onMediaItemTransition` fires every time the current track changes (including when a playlist auto-advances). This is the correct hook for play counting — not `onIsPlayingChanged`, which fires on every pause/resume.

---

## Common Mistakes

### Common Mistake: Hardcoding `source` When Enriching Track Metadata

**Symptom**: Local tracks get incorrectly tagged as WebDAV source after metadata extraction.

**Cause**: Creating a new `AudioTrack(...)` instead of using `copy()` on the existing track.

**Fix**: Always use `existing.copy(...)`, never `AudioTrack(id=trackId, uri=uri, source=TrackSource.WEBDAV, ...)`.

**Prevention**: Use `persistTrackUpdate(trackId) { existing -> existing.copy(...) }` — the helper ensures the existing track is the base.

---

## Forbidden Patterns

- **Do not use `var` in data classes** — use `copy()` for modifications
- **Do not pass `activity` as a `Context` to long-lived objects** — causes memory leaks; always use `applicationContext`
- **Do not perform I/O on the main thread** — use `viewModelScope.launch { withContext(Dispatchers.IO) { ... } }`
- **Do not expose mutable collections** — `List<Track>` not `MutableList<Track>`
- **Do not use `printStackTrace()`** — use `Log.e(TAG, msg, e)`
- **Do not leak coroutines from ViewModels** — always cancel jobs via `positionJob?.cancel()` in `onCleared()`

---

## Testing Requirements

- No formal unit tests currently exist
- Prefer manual testing via Android Studio emulator or a physical device
- When adding tests, cover: repository error paths, `Result<T>` failure branches, JSON serialization round-trips

---

## Code Review Checklist

When reviewing PRs, verify:

1. Data classes are `@Immutable` and have no `var`
2. `viewModelScope.launch` blocks use `withContext(Dispatchers.IO)` for I/O
3. `MediaController` is null-checked before use
4. New persisted fields have defaults and `opt*` deserialization
5. Errors are either returned as `Result<T>` or logged with context — not silently swallowed in critical paths
6. `onCleared()` cancels all coroutine jobs and releases the `MediaController`
7. No credentials or sensitive data in log messages
8. Album art is loaded via `rememberAlbumArt` (async, not in `remember {}` block on main thread)
9. Metadata enrichment uses `existing.copy()` to preserve existing fields (not `AudioTrack(...)` constructor)
10. `Player.Listener` overrides dispatch persistence to `Dispatchers.IO`
