# Playback Architecture

> ExoPlayer-based playback: MediaService, PlayerViewModel, and queue management.

---

## Architecture Overview

```
MusicService (MediaLibraryService)
  └── ExoPlayer instance
  └── MediaLibrarySession
        └── MusesLibraryCallback

PlayerViewModel (AndroidViewModel)
  └── MediaController → connects to MusicService via session
  └── Manages playlist, shuffle, repeat state
  └── PlayerListener → syncs ExoPlayer state to PlayerState flow

PlayerBar (Composable)
  └── Collects PlayerState from PlayerViewModel
  └── Controls: shuffle, prev, play/pause, next, repeat
```

---

## Key Design Decisions

### Decision: Queue-based playlist via ExoPlayer

**Context**: Initially, clicking a song used `controller.setMediaItem()` which only set a single track with no auto-next.

**Decision**: Use `controller.setMediaItems(items, startIndex, position)` to load the entire playlist into ExoPlayer's internal queue. ExoPlayer natively handles auto-advance to next track on completion — no custom `onCompletion` listener needed.

**Example**:
```kotlin
// Correct: load full playlist with start index
controller.setMediaItems(mediaItems, startIndex, 0L)
controller.prepare()
controller.playWhenReady = true

// Wrong: single item only, no queue
controller.setMediaItem(mediaItem)
```

### Decision: Playlist context passed from UI layer

**Context**: Both SongsScreen and WebdavScreen need to provide a playlist when a track is clicked.

**Decision**: `onTrackClick` callback signature is `(AudioTrack, List<AudioTrack>)` — the clicked track plus the full visible track list. The caller (MainActivity) extracts the start index and calls `playerViewModel.setPlaylist(tracks, index)`.

### Decision: Shuffle/repeat delegated to ExoPlayer

**Context**: Need shuffle and repeat mode toggles.

**Decision**: Use ExoPlayer's built-in `shuffleModeEnabled` and `repeatMode` properties directly. No custom shuffle implementation. State is observed via `Player.Listener` callbacks (`onShuffleModeEnabledChanged`, `onRepeatModeChanged`).

**Repeat mode cycle**: OFF → REPEAT_ALL → OFF (no single-track repeat in this app).

---

## PlayerState Contract

```kotlin
data class PlayerState(
    val isPlaying: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtUri: Uri? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isReady: Boolean = false,
    val hasTrack: Boolean = false,
    val errorMessage: String? = null,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)
```

### State Sync Points

| Event | Source | Fields Updated |
|-------|--------|---------------|
| `onIsPlayingChanged` | ExoPlayer | `isPlaying` |
| `onPlaybackStateChanged` | ExoPlayer | All via `syncState()` |
| `onMediaItemTransition` | ExoPlayer | All via `syncState()`, plus `updatePlayMetadata()` and `enrichTrackMetadata()` |
| `onShuffleModeEnabledChanged` | ExoPlayer | `shuffleModeEnabled` |
| `onRepeatModeChanged` | ExoPlayer | `repeatMode` |
| `onPlayerError` | ExoPlayer | `errorMessage`, `isPlaying` |
| Position polling (250ms) | Coroutine | `positionMs`, `durationMs` |

---

## Playback Control API

| Method | Action |
|--------|--------|
| `setPlaylist(tracks, startIndex)` | Load full playlist, start at index |
| `playTrack(mediaItem)` | Play single item (no queue) |
| `togglePlayPause()` | Play if paused, pause if playing |
| `skipToNext()` | `controller.seekToNextMediaItem()` |
| `skipToPrevious()` | `controller.seekToPreviousMediaItem()` |
| `toggleShuffle()` | Toggle `controller.shuffleModeEnabled` |
| `cycleRepeatMode()` | OFF → ALL → OFF |
| `seekTo(positionMs)` | Seek to position |

---

## Gotchas

> **Warning**: `playTrack()` sets a single item and clears the queue. Always use `setPlaylist()` when auto-next is expected.

> **Warning**: `enrichedIds` is a per-session set. If the playlist is reloaded (e.g. user clicks another song), previously enriched tracks won't be re-enriched — this is intentional to avoid duplicate metadata extraction.

> **Warning**: `syncState()` reads `controller.shuffleModeEnabled` and `controller.repeatMode` along with other fields. If you add new ExoPlayer state, add it there too.

---

## MusicService DataSource

WebDAV URIs require Basic auth. This is handled via an OkHttp interceptor in `createMediaSourceFactory()` that reads credentials from `WebdavConfigManager.getBasicAuthHeader()`. Local `content://` URIs go through `DefaultDataSource` which uses `ContentDataSource` internally.

---

## Common Mistakes

### Mistake: Forgetting to update `onTrackClick` signature

When a new screen with track list is added, its `onTrackClick` must pass `(AudioTrack, List<AudioTrack>)` so the playlist context is available. Using a single-track callback breaks auto-next.

### Mistake: Adding custom onCompletion listener

ExoPlayer handles auto-next internally when items are in the queue. Adding a manual `onPlaybackStateChanged` → `STATE_ENDED` → `seekToNext()` is redundant and causes double-skip bugs.
