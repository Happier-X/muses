# Directory Structure

> How Kotlin/Android code is organized in this project.

---

## Overview

This is a native Android music player app using Kotlin + Jetpack Compose.
Architecture is MVVM with a clean separation between UI, ViewModels, and data layer.
No traditional "backend server" — persistence and data fetching happen on-device.

---

## Directory Layout

```
app/src/main/java/com/example/muses/
├── data/
│   ├── model/          # Immutable data classes (AudioTrack, WebdavConfig, WebdavItem)
│   └── repository/     # Data access (WebdavRepository, LocalMusicRepository, TrackStore, MetadataExtractor)
├── playback/           # MusicService (ExoPlayer + MediaSession background service)
└── ui/
    ├── screens/        # Jetpack Compose screen composables
    ├── viewmodel/      # AndroidViewModel classes (PlayerViewModel, SongsViewModel, etc.)
    ├── theme/          # Compose theme (Color, Type, Theme)
    └── util/           # Utility functions (Formatters)
```

---

## Module Organization

### `data/model/`
Holds pure data classes. All should carry `@Immutable` (Compose annotation).
Examples: `AudioTrack`, `WebdavConfig`, `WebdavItem`

### `data/repository/`
Data access layer. Repositories are classes or objects that own data operations.
`TrackStore` is an `object` (singleton) for SharedPreferences persistence.
`MetadataExtractor` is an `object` for audio metadata extraction.

### `playback/`
Contains `MusicService` — a `MediaLibraryService` subclass that owns the `ExoPlayer` instance and `MediaLibrarySession`. Runs as an Android foreground service.

### `ui/viewmodel/`
`AndroidViewModel` subclasses that hold UI state and communicate with repositories or `MusicService` via `MediaController`.

### `ui/screens/`
Stateless or lightly stateful Compose `@Composable` functions. Receive state as parameters; emit events upward via callbacks.

### `ui/theme/`
Standard Jetpack Compose Material3 theme files.

---

## Naming Conventions

| What | Convention | Example |
|------|-----------|---------|
| Kotlin files | PascalCase | `PlayerViewModel.kt` |
| Data classes | PascalCase noun | `AudioTrack.kt`, `WebdavItem.kt` |
| ViewModel classes | PascalCase ending in `ViewModel` | `SongsViewModel.kt` |
| Repository objects | PascalCase ending in `Repository` or `Manager` | `WebdavRepository.kt`, `TrackStore.kt` |
| Screen composables | PascalCase ending in `Screen` | `SongsScreen.kt`, `PlayerBar.kt` |
| Package names | lowercase | `com.example.muses.data.repository` |

---

## Adding New Features

1. Add data models to `data/model/`
2. Add repository logic to `data/repository/`
3. Add `AndroidViewModel` to `ui/viewmodel/`
4. Add UI to `ui/screens/`
5. Connect ViewModel → Repository or ViewModel → MusicService (via `MediaController`)

---

## Examples

- `AudioTrack` model: `app/src/main/java/com/example/muses/data/model/AudioTrack.kt`
- Repository pattern: `app/src/main/java/com/example/muses/data/repository/WebdavRepository.kt`
- ExoPlayer service: `app/src/main/java/com/example/muses/playback/MusicService.kt`
- ViewModel: `app/src/main/java/com/example/muses/ui/viewmodel/PlayerViewModel.kt`
