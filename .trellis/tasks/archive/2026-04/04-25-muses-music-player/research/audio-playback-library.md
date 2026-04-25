# Audio Playback Library Research

**Date:** 2026-04-25

## Recommendation: Media3 ExoPlayer (1.10.0)

Google's current-gen media library. Consolidates legacy ExoPlayer + MediaSession into unified `androidx.media3.*`. Apache 2.0.

### Compose Integration
- `media3-ui-compose-material3` provides native Compose components: Player, PlayPauseButton, NextButton, PreviousButton, SeekBackButton, ProgressSlider, TimeText, etc.
- State holders: `rememberProgressStateWithTickInterval`, `MuteButtonState`
- Direct Player interface integration via `remember` composables

### WebDAV Streaming
- `MediaItem.fromUri("https://...")` plays progressive HTTP URLs
- Use `OkHttpDataSource.Factory` with custom `Interceptor` to inject `Authorization: Basic` headers
- Seamlessly handles Range requests for seeking

### Format Support
Comprehensive: MP3, FLAC, AAC (MP4/M4A), OGG (Vorbis + Opus), WAV, WMA, AC-3

### Background Playback
- `MediaSessionService` handles foreground service, wake locks, audio focus, becoming-noisy detection
- Lifecycle-aware since 1.10.0 (extends LifecycleService)
- Automatic lock screen controls, Android Auto, Wear OS

## Dependencies (libs.versions.toml)

```toml
[versions]
media3 = "1.10.0"

[libraries]
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
media3-ui-compose-material3 = { group = "androidx.media3", name = "media3-ui-compose-material3", version.ref = "media3" }
media3-datasource-okhttp = { group = "androidx.media3", name = "media3-datasource-okhttp", version.ref = "media3" }
```

NOT needed: dash, hls, smoothstreaming, ui (legacy View-based), cast, transformer, effect.

## WebDAV Streaming Architecture
1. Create `OkHttpClient` with `Interceptor` adding `Authorization: Basic <base64(user:pass)>`
2. Use `OkHttpDataSource.Factory(okHttpClient)` as base for `DefaultDataSource.Factory`
3. `MediaItem.fromUri("https://webdav.example.com/music/song.mp3")` works with auth transparently

## Why not MediaPlayer
- No Compose integration (manual bridge required)
- WebDAV auth not possible without custom MediaDataSource
- Device-dependent format support
- Background playback requires full manual implementation
- Google recommends against for new apps

## Why not LibVLC / IJKPlayer
- LibVLC: GPLv3 license (requires app to be GPL)
- IJKPlayer: Unmaintained since 2019
