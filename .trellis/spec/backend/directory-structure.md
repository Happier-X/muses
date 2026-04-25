# Directory Structure

> How Android code is organized in this project.

---

## Project Layout

```
muses/                           # Root project
├── app/                         # Main application module
│   ├── build.gradle.kts         # Module build config
│   ├── proguard-rules.pro       # ProGuard rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/muses/
│       │   │   ├── MainActivity.kt              # Single Activity + bottom nav
│       │   │   ├── ui/
│       │   │   │   ├── theme/                   # Material3 theme (Color, Type, Theme)
│       │   │   │   ├── screens/                 # Full-screen composables
│       │   │   │   │   ├── LibraryScreen.kt     # Local music browser
│       │   │   │   │   ├── WebdavScreen.kt      # WebDAV config + directory browser
│       │   │   │   │   └── PlayerBar.kt         # Persistent bottom playback bar
│       │   │   │   ├── viewmodel/               # ViewModels
│       │   │   │   │   ├── LibraryViewModel.kt
│       │   │   │   │   ├── WebdavViewModel.kt
│       │   │   │   │   └── PlayerViewModel.kt
│       │   │   │   └── util/                    # Shared UI utilities
│       │   │   │       └── Formatters.kt
│       │   │   ├── data/
│       │   │   │   ├── model/                   # Domain models
│       │   │   │   │   ├── AudioTrack.kt        # Track with source (LOCAL/WEBDAV)
│       │   │   │   │   └── WebdavConfig.kt      # WebDAV server config
│       │   │   │   └── repository/              # Data access
│       │   │   │       ├── LocalMusicRepository.kt    # MediaStore queries
│       │   │   │       ├── WebdavRepository.kt        # WebDAV PROPFIND
│       │   │   │       └── WebdavConfigManager.kt     # Config persistence
│       │   │   └── playback/
│       │   │       └── MusicService.kt          # MediaLibraryService + ExoPlayer
│       │   └── res/                             # Resources
│       ├── test/                                # Unit tests
│       └── androidTest/                         # Instrumented tests
├── gradle/
│   └── libs.versions.toml                       # Version catalog
├── build.gradle.kts                             # Top-level build
├── settings.gradle.kts                          # Project settings
└── gradle.properties                            # Gradle config
```

---

## Package Organization

| Package | Purpose | Key files |
|---------|---------|-----------|
| `ui/theme/` | Material3 theme, colors, typography | Color.kt, Type.kt, Theme.kt |
| `ui/screens/` | One composable per screen/bar | LibraryScreen, WebdavScreen, PlayerBar |
| `ui/viewmodel/` | ViewModels with sealed UI states | One ViewModel per screen |
| `ui/util/` | Shared UI helpers | Formatters (duration, etc.) |
| `data/model/` | Immutable data classes | AudioTrack, WebdavConfig |
| `data/repository/` | Data access layer | LocalMusicRepository, WebdavRepository, WebdavConfigManager |
| `playback/` | Media3 playback service | MusicService (MediaLibraryService) |

---

## Architecture Pattern

```
Compose UI (screens/)  ←──→  ViewModel (viewmodel/)  ←──→  Repository (data/repository/)
       │                            │                              │
  StateFlow observe          StateFlow emit              MediaStore / WebDAV / Prefs
  collectAsState()           viewModelScope.launch
```

- **Screens**: Composable functions, observe ViewModel state via `collectAsStateWithLifecycle()`
- **ViewModels**: Hold `StateFlow<SealedUiState>`, expose action functions, use `viewModelScope`
- **Repositories**: Suspend functions for data access, no Android framework dependencies
- **playback/**: `MusicService` extends `MediaLibraryService`, holds `ExoPlayer` instance

---

## Naming Conventions

- **Files**: PascalCase (`MainActivity.kt`, `PlayerScreen.kt`)
- **Composable functions**: PascalCase, noun/noun phrase (`PlayerScreen`, `MusicListItem`)
- **Composable with Screen suffix**: Full-screen content (`LibraryScreen`)
- **Non-composable functions**: camelCase (`formatDuration`, `loadPlaylist`)
- **ViewModels**: `<Screen>ViewModel` (`LibraryViewModel`)
- **Repositories**: `<DataSource>Repository` (`LocalMusicRepository`)
- **Data classes**: Noun, `@Immutable` (`AudioTrack`, `WebdavConfig`)

---

## Dependency Management

All dependencies live in `gradle/libs.versions.toml`. Module `build.gradle.kts` references via `libs.*` accessors:

```kotlin
implementation(libs.media3.exoplayer)
implementation(platform(libs.androidx.compose.bom))
```

**Rule**: Never hardcode version strings in `build.gradle.kts`. Go through version catalog.
