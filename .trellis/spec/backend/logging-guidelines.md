# Logging Guidelines

> Log levels, format, and conventions for this project.

---

## Overview

This project uses Android's built-in `android.util.Log` class.
No third-party logging library (no Timber, no SLF4J).

---

## Log Levels

| Level | When to use | Example |
|-------|------------|---------|
| `Log.i` | Lifecycle events, service start/stop | `"Service created"`, `"Connected to MusicService"` |
| `Log.d` | Debug detail: media transitions, metadata enrichment | `"onMediaItemTransition: mediaId=..."`, `"Found 12 items at $path"` |
| `Log.w` | Recoverable issues, expected failures (e.g. no metadata) | `"Failed to extract metadata from $uri"`, `"Skipping directory $path"` |
| `Log.e` | Unexpected failures, network errors | `"Network error for $path"`, `"Playback error: ${e.message}"` |

---

## TAG Convention

Every class defines `TAG` as a `companion object` constant:

```kotlin
class MusicService : MediaLibraryService() {
    companion object {
        private const val TAG = "MusicService"
    }
}

object MetadataExtractor {
    private const val TAG = "MetadataExtractor"
}
```

Tag format: class name or logical area (e.g. `"WebdavRepo"`, `"PlayerVM"`).

---

## What to Log

- Service lifecycle: `onCreate`, `onDestroy`
- Media playback events: track transitions, errors
- Network operations: PROPFIND requests, HTTP errors
- Metadata extraction: success/failure
- Configuration changes: WebDAV connect/disconnect

Example from `MusicService.kt`:
```kotlin
Log.i(TAG, "Service created")  // onCreate
Log.i(TAG, "Service destroyed")  // onDestroy
```

Example from `WebdavRepository.kt`:
```kotlin
Log.i(TAG, "PROPFIND $url")  // Before request
Log.i(TAG, "Found ${items.size} items at $path")  // On success
Log.w(TAG, "Empty PROPFIND response")  // On empty body
Log.e(TAG, "Network error for $path", e)  // On failure
```

Example from `PlayerViewModel.kt`:
```kotlin
Log.d(TAG, "onMediaItemTransition: mediaId=$trackId, enrichedIds=$enrichedIds")
Log.d(TAG, "enrichTrackMetadata: got metadata for $trackId: title=...")
Log.e(TAG, "Playback error: ${error.message}", error)
```

---

## What NOT to Log

- **Credentials**: never log usernames, passwords, or auth tokens
- **Full file/audio content**: avoid logging large binary data
- **PII**: user names, device identifiers if they appear in URLs
- **Stack traces via `printStackTrace()`**: use `Log.e(TAG, msg, e)` instead

---

## Structured Parameters

When logging events with parameters, include them inline after a `:` or `=`:

```kotlin
Log.d(TAG, "PROPFIND $url")                      // URL in message
Log.d(TAG, "Found ${items.size} items at $path")  // Count + path
Log.e(TAG, "Failed for $path", e)                 // Path + exception
```

Avoid: `Log.d(TAG, "url=$url, path=$path")` — keep it readable and grep-friendly.
