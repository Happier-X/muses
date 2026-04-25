# Develop WebDAV + Local Music Player

## Goal

Build an Android music player app ("Muses") that plays local audio files from device storage and streams music from WebDAV servers. Single-activity architecture using Kotlin + Jetpack Compose + Material3.

## What I already know

* Android app with Kotlin 2.2, Compose BOM 2026.02, Material3, minSdk 24, targetSdk 36
* Single Activity architecture (`MainActivity.kt`)
* Existing theme: `MusesTheme`, dynamic colors, dark mode
* Gradle with version catalog (`gradle/libs.versions.toml`)
* Blank scaffold — no music logic exists

## Research References

* [`research/audio-playback-library.md`](research/audio-playback-library.md) — Media3 ExoPlayer 1.10.0 recommended
* [`research/webdav-client.md`](research/webdav-client.md) — dav4jvm recommended for Kotlin-native WebDAV
* [`research/local-music-access.md`](research/local-music-access.md) — MediaStore API + MediaLibraryService recommended

## Technical Decisions (ADR-lite)

### Audio Playback: Media3 ExoPlayer (1.10.0)
- **Why**: Google's recommended library. Native Compose UI components. Background playback via MediaSessionService. OkHttp datasource for WebDAV auth. Apache 2.0.
- **Consequences**: Adds ~4 Media3 dependencies. Must implement MediaSessionService.

### WebDAV Client: dav4jvm
- **Why**: Kotlin-first, Digest + Basic auth, OkHttp 5.x engine, used in DAVx⁵ (production).
- **Consequences**: JitPack-only distribution. MPL 2.0 license. ~2MB APK impact.

### Local Music Access: MediaStore + ContentObserver
- **Why**: Zero user friction (auto-discover). Rich metadata. `content://` URIs playable by ExoPlayer. Google Play compliant. SAF and MANAGE_EXTERNAL_STORAGE not viable (UX/policy reasons).
- **Consequences**: Requires runtime permission requests (READ_MEDIA_AUDIO on API 33+).

## Requirements

* **Local music**: Auto-discover audio files via MediaStore, display with metadata (title, artist, album, duration)
* **WebDAV**: Configure single server (URL + username + password, Basic/Digest auth), browse directories, play audio via direct streaming
* **Navigation**: Bottom tabs — Local Music / WebDAV
* **Playback**: Play/pause, skip next/prev, seek with progress bar, current track info
* **Background**: Foreground service with media notification, lock screen controls, audio focus
* **Theme**: Dark/light mode via existing MusesTheme

## Acceptance Criteria (evolving)

* [ ] App launches and shows music library (local files auto-discovered)
* [ ] Can play/pause/seek local audio files
* [ ] Can configure WebDAV server (URL, credentials)
* [ ] Can browse WebDAV directory listing
* [ ] Can play audio from WebDAV (streaming)
* [ ] Background playback works with notification controls
* [ ] Dark/light theme applied correctly

## Definition of Done

* All composables follow Compose Conventions (`.trellis/spec/backend/compose-conventions.md`)
* Error handling per `.trellis/spec/backend/error-handling.md`
* Tests: unit tests for ViewModels, UI tests for key screens
* Lint clean (`./gradlew lintDebug`)
* No hardcoded strings (use `stringResource`)

## Out of Scope (MVP)

* Playlist management (create/edit/reorder)
* Equalizer / audio effects / DSP
* Album art fetching from internet
* Multiple WebDAV servers simultaneously
* Chromecast / Bluetooth device selection
* Lyrics display
* User accounts / cloud sync

## Technical Notes

* Key new deps: media3-exoplayer, media3-session, media3-ui-compose-material3, media3-datasource-okhttp, dav4jvm, Room (if caching)
* Permissions: READ_MEDIA_AUDIO (API 33+), READ_EXTERNAL_STORAGE (API 24-32), FOREGROUND_SERVICE_MEDIA_PLAYBACK
* Architecture: Compose UI → MediaController → MediaLibraryService → {MediaStore, dav4jvm}
