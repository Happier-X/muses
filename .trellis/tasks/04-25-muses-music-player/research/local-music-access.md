# Local Music Access Research for Android

**Date:** 2026-04-25

## Recommendation: MediaStore API + Media3 MediaLibraryService

### MediaStore API (Data Layer)
- Query `MediaStore.Audio.Media` via `ContentResolver`
- Returns `content://` URIs — playable directly by ExoPlayer
- Rich metadata: title, artist, album, duration, album art
- Minimal permissions: `READ_MEDIA_AUDIO` (API 33+) or `READ_EXTERNAL_STORAGE` (API 24-32)
- Change monitoring via `ContentObserver`

### Media3 MediaLibraryService (Architecture Layer)
- Extends `MediaSessionService` — foreground service, notifications, lock screen
- Browsable library via `MediaLibrarySession.Callback` (Artists → Albums → Songs)
- Free Android Auto / Wear OS / Google Assistant integration
- Can also serve WebDAV content as additional browse nodes

### Why NOT SAF (Storage Access Framework)
- User must manually pick folders — poor UX for music app
- Terrible performance: O(n) metadata retrieval per file
- No rich metadata without per-file `MediaMetadataRetriever`
- Android 11+ blocks root directories and Download/

### Why NOT MANAGE_EXTERNAL_STORAGE
- Google Play explicitly lists "Media Files access" as invalid use case
- Will be rejected by Play Store review
- Over-permission: grants access to all user files

### Permissions Strategy

```xml
<!-- API 33+: Granular media permission -->
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

<!-- API 24-32: Legacy -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />

<!-- Only needed if modifying files on API ≤ 28 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />

<!-- Media playback foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

### Change Monitoring
Use `ContentObserver` on `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`:

```kotlin
val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        // Re-query MediaStore; on API 30+, uri indicates specific changed row
        mediaLibrarySession.notifyChildrenChanged("ROOT")
    }
}
contentResolver.registerContentObserver(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
)
```

### Proposed Architecture

```
UI Layer (Compose) → MediaController
Playback Layer (Media3) → MediaLibraryService + ExoPlayer
Data Layer → MediaStore queries + ContentObserver
```

MediaLibraryService can serve both local (MediaStore) and remote (WebDAV via dav4jvm) content through the same browsing tree.
