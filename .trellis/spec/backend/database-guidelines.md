# Database Guidelines

> Persistence patterns and conventions for this project.

---

## Overview

This project uses **SharedPreferences with JSON serialization** for persistence — no Room, no SQLite.
All persisted data is stored as JSON strings under a single `SharedPreferences` instance.

Primary stores:
- `TrackStore` — persists the list of known `AudioTrack` objects
- `WebdavConfigManager` — persists WebDAV server credentials and settings

---

## TrackStore

`TrackStore` is a Kotlin `object` (singleton) wrapping `SharedPreferences`.

```kotlin
object TrackStore {
    private const val PREFS_NAME = "track_store"
    private const val KEY_TRACKS = "tracks"

    fun saveTracks(context: Context, tracks: List<AudioTrack>) { ... }
    fun loadTracks(context: Context): List<AudioTrack> { ... }
    fun clear(context: Context) { ... }
}
```

Key file: `app/src/main/java/com/example/muses/data/repository/TrackStore.kt`

**Schema evolution**: When `AudioTrack` gains new fields, use `json.optInt("field", default)` / `json.optLong("field", default)` so old serialized JSON gracefully degrades.

---

## WebdavConfigManager

Persists the WebDAV server configuration (URL, username, password).

Key file: `app/src/main/java/com/example/muses/data/repository/WebdavConfigManager.kt`

---

## Naming Conventions

| Item | Convention | Example |
|------|-----------|---------|
| Preferences file name | `PREFS_NAME = "..."` constant | `"track_store"` |
| JSON keys | lowercase `camelCase` | `"playCount"`, `"lastPlayedAt"` |
| Context parameter | `context: Context` | Always last in overloaded signatures |

---

## Data Class Fields for Persistence

Mark fields with default values so JSON deserialization never fails:

```kotlin
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

Always use `optInt` / `optLong` / `optString` with defaults in `jsonToTrack()` so partial/missing keys don't throw.

---

## Mutation Pattern: Load → Map → Save

`TrackStore` has no partial-update API. To modify a single track, load all, map, save all:

```kotlin
private fun updatePlayMetadata(trackId: String) {
    viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val tracks = TrackStore.loadTracks(getApplication())
            val updated = tracks.map { track ->
                if (track.id == trackId) track.copy(playCount = track.playCount + 1, lastPlayedAt = now)
                else track
            }
            TrackStore.saveTracks(getApplication(), updated)
        }
    }
}
```

**Why**: SharedPreferences writes are atomic string replacements. There is no field-level patch. This is fine for a small track list (< 1000 tracks); do not use this pattern if the list grows beyond ~10K entries.

---

## Forbidden Patterns

- Do **not** store raw SQL or Room entities without a JSON shim — this project has no Room setup
- Do **not** store `SharedPreferences` in a non-singleton `object` without a `Context` parameter — causes memory leaks on Android
- Do **not** serialize complex types (Uri, custom enums) directly without a conversion step
- Do **not** call `TrackStore.saveTracks()` on the main thread — always wrap in `withContext(Dispatchers.IO)`
