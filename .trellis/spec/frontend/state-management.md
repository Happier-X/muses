# State Management

> Local state, global state, and server-state conventions for this project.

---

## Overview

The current app has no global state management layer. There is no Pinia, Vuex, custom store directory, API client, or server-state library in the codebase.

State-related dependencies in `package.json` are limited to Vue, Vue Router, Ionic Vue, and Capacitor/Ionic packages.

Reference files:

- `package.json`
- `src/main.ts`
- `src/router/index.ts`
- `src/views/Tab1Page.vue`
- `src/views/TabsPage.vue`

---

## Current State Pattern

The current UI is effectively static and route-driven:

- Route state is managed by Vue Router through `src/router/index.ts`.
- Tab navigation is represented by Ionic tabs in `src/views/TabsPage.vue`.
- Pages render static labels and a shared presentational component.
- There is no app-level state initialized in `src/main.ts` beyond registering Ionic and the router.

---

## Route State

Keep navigation state in Vue Router and Ionic tab components.

Current route structure:

- `/` redirects to `/tabs/tab1`
- `/tabs/` renders `TabsPage`
- `/tabs/tab1`, `/tabs/tab2`, and `/tabs/tab3` lazy-load their page components

Reference file:

- `src/router/index.ts`

Avoid duplicating current tab selection in a separate store unless a task introduces a real cross-component state need.

---

## Local Component State

For future small UI interactions, prefer local Vue state inside `<script setup lang="ts">` using Vue Composition API primitives such as `ref` and `computed`.

Because current components are static, no existing file demonstrates local reactive state yet. Add it in the component that owns the interaction unless multiple components need the same value.

---

## Global State

Do not add a global store preemptively. Introduce a global state library only when the app has repeated cross-page or cross-component state that cannot be cleanly handled with local state, props, emits, router params, or a focused composable.

If a future task adds Pinia or another store, document the chosen pattern in this file and add concrete references to the first store modules.

---

## Server State and Persistence

Use the smallest persistence mechanism that satisfies the feature contract. For non-sensitive app metadata, feature-local helpers may use browser storage such as `localStorage` with explicit validation before data is trusted.

Sensitive values such as WebDAV passwords, tokens, or other credentials must not be written to `localStorage`, logs, route params, or task-visible metadata. On Android, store these values through a Capacitor secure storage plugin and keep only an opaque lookup key such as `credentialKey` in non-sensitive metadata.

Current source module contract:

- `localStorage` key `muses:sources` stores only source metadata.
- WebDAV source metadata stores `credentialKey`, never the password.
- WebDAV passwords are stored with `@aparajita/capacitor-secure-storage`.
- WebDAV directory browsing on Android must use the project-local native `WebDav` Capacitor plugin for `PROPFIND`, backed by OkHttp. Do not use browser `fetch`/XHR-based WebDAV clients because Android WebView still enforces CORS; do not use built-in `CapacitorHttp` or `HttpURLConnection` for `PROPFIND` because they reject non-standard methods.
- Because users may add arbitrary `http://` WebDAV servers, Android uses `network_security_config` with `base-config cleartextTrafficPermitted="true"`. Prefer HTTPS when possible and surface risk to users when adding plain HTTP sources.
- Removing a WebDAV source should also remove the corresponding secure-storage entry.

### Scenario: Source Library Scan Persistence

#### 1. Scope / Trigger

- Trigger: scanning local/WebDAV sources into a local song library touches browser storage, SecureStorage credentials, frontend feature helpers, and Android native plugin contracts.
- Owning files: `src/features/library/*`, `src/features/sources/webdav.ts`, `src/views/SourcesPage.vue`, and project-local Android plugins under `android/app/src/main/java/ionic/muses/`.

#### 2. Signatures

- Song storage key: `muses:songs`.
- Song uniqueness: `(sourceId, path)`; use `upsertSong(...)` rather than appending raw arrays.
- `LocalLibrary.scanDirectory({ treeUri: string }) -> { files: Array<{ path: string; uri: string; name: string }> }`.
- `LocalLibrary.readMetadata({ uri: string }) -> { title?: string; artist?: string; album?: string; duration?: number }`.
- `WebDav.propfind({ url: string; username: string; password: string }) -> { status: number; data: string }`.
- `WebDav.readMetadata({ url: string; username: string; password: string }) -> { title?: string; artist?: string; album?: string; duration?: number }`.

#### 3. Contracts

- `muses:songs` may store song metadata only: IDs, source references, paths/URIs, display tags, and timestamps.
- `muses:songs` must never store WebDAV passwords, Basic Auth headers, tokens, or SecureStorage values.
- WebDAV scans must resolve passwords at scan time with `getWebDavPassword(source.credentialKey)` and pass them only to the native WebDAV call boundary.
- Local directory scans use the saved Android `content://` tree URI from `FilePicker.pickDirectory()`; the file picker does not provide recursive children.
- Real tag reading belongs in native/plugin or a bounded binary-read helper. Do not fake successful tag reads when metadata parsing fails.

#### 4. Validation & Error Matrix

- Missing WebDAV password -> fail the scan with a user-facing message such as `WebDAV 密码不存在，请重新添加该音源。`.
- Invalid `muses:songs` JSON or malformed entries -> ignore invalid data and return only valid songs.
- Per-file metadata parse failure -> fall back to filename title, increment degraded count, continue scanning.
- Per-file upsert failure -> increment failed count, continue scanning remaining files when safe.
- WebDAV `401` / `403` from `PROPFIND` -> show authentication failure and do not mutate source metadata.

#### 5. Good/Base/Bad Cases

- Good: repeated scans of the same source/path update or skip the existing song and preserve `createdAt` on updates.
- Base: scan with `readTags: false` stores filename-derived titles without invoking metadata readers.
- Bad: downloading an unbounded WebDAV audio file into frontend memory or logging a password to diagnose a scan failure.

#### 6. Tests Required

- Storage tests assert malformed `muses:songs` entries are ignored and `(sourceId, path)` prevents duplicate inserts.
- Scanner tests assert tag parse failures increment `degraded` and still persist fallback-title songs.
- WebDAV scanner tests assert `SecureStorage.get` is called with `credentialKey`, native metadata reading receives the password only at the call boundary, and `localStorage.getItem('muses:songs')` does not contain the password.
- WebDAV XML tests assert file and directory responses are distinguished, audio extensions are filtered, and existing directory-only browsing remains compatible.

#### 7. Wrong vs Correct

Wrong:

```ts
const songs = [...loadSongs(), newSong]
localStorage.setItem('muses:songs', JSON.stringify({ ...songs, password }))
```

Correct:

```ts
const password = await getWebDavPassword(source.credentialKey)
const result = upsertSong({ sourceId: source.id, path: file.path, uri: file.uri, title })
```

```

### Scenario: Global Audio Player State

#### 1. Scope / Trigger

- Trigger: the global audio player manages playback across pages, Android background playback, and notification controls. It touches frontend feature-local reactive state, Capacitor plugin communication, and Android `MediaSessionService` contracts.
- Owning files: `src/features/player/*`, `src/components/MiniPlayer.vue`, `src/App.vue`, and project-local Android plugins `AudioPlayerPlugin.kt` + `AudioPlaybackService.kt`.

#### 2. Signatures

- `AudioPlayer.play(options: LocalPlayOptions | WebDavPlayOptions): Promise<void>`.
- `AudioPlayer.pause(): Promise<void>`.
- `AudioPlayer.resume(): Promise<void>`.
- `AudioPlayer.stop(): Promise<void>`.
- `AudioPlayer.getState(): Promise<AudioPlayerNativeState>`.
- `AudioPlayer.addListener('stateChange', listener: (state: AudioPlayerNativeState) => void): Promise<PluginListenerHandle>`.
- Player controller state held in `src/features/player/controller.ts` as a `reactive<PlayerState>` shared across components.

#### 3. Contracts

- Player state must never contain WebDAV passwords, Basic Auth headers, or any SecureStorage values.
- `MiniPlayer.vue` and `SongsPage.vue` only read from `playerState` (readonly) and call `playSong`/`pausePlayback`/`resumePlayback`/`stopPlayback`.
- `playSong(song)` resolves the WebDAV password via `getWebDavPassword(source.credentialKey)` and passes it only to `AudioPlayerNative.play(...)`; the password never reaches localStorage, the UI state, or `muses:songs`.
- Android `AudioPlayerPlugin.kt` passes `EXTRA_PASSWORD` into an Intent; `AudioPlaybackService.kt` uses it only to construct a Basic Auth header for the ExoPlayer `DefaultHttpDataSource.Factory`, then the password is discarded after the MediaItem is created.
- `AudioPlaybackService.kt` is started with `ContextCompat.startForegroundService(...)`; it must call `startForeground(...)` during `onCreate()` or immediately on play start, otherwise Android kills the app with `Context.startForegroundService() did not then call Service.startForeground()`.
- When playback fails, the frontend shows only white-listed business errors or a generic message; it does not forward auth-related exception details into reactive state or `MiniPlayer.vue`.

#### 4. Validation & Error Matrix

- Missing WebDAV password when playing a WebDAV song -> `playSong` throws with `WebDAV 密码不存在，请重新添加该音源。`.
- Missing WebDAV source entry -> `playSong` throws with `找不到这首歌对应的 WebDAV 音源，请重新扫描音源。`.
- Non-white-listed native exceptions -> frontend shows `播放失败，请稍后重试。`.
- Native pause/resume/stop exception -> frontend shows a generic operation-failed message.
- Foreground service started but no timely `startForeground(...)` call -> app ANR/crash; fix by creating a notification channel and foreground notification before long playback work.
- Anonymous song or missing URI in a play Intent -> native service publishes `STATUS_ERROR` with a safe message, not raw exception details.

#### 5. Good/Base/Bad Cases

- Good: clicking a local song plays it; clicking a different song stops the previous one and plays the new one; the mini player updates across tabs.
- Base: user stops playback via the mini player; Android notification disappears; `currentSong` is cleared.
- Bad: WebDAV password ends up in `localStorage.getItem('muses:songs')` or is logged to diagnose a playback failure.

#### 6. Tests Required

- `playSong` for a local song calls `AudioPlayerNative.play` with local options only (no password).
- `playSong` for a WebDAV song calls `getWebDavPassword`, passes it only to `AudioPlayerNative.play`, and does not store it.
- `controller.ts` error handler maps unknown native errors to a safe string.
- `MiniPlayer.vue` renders the current title, toggles play/pause, and stops.
- `SongsPage.vue` highlights the currently playing song.

#### 7. Wrong vs Correct

Wrong:

```ts
const options: WebDavPlayOptions = { ..., password }
state.currentSong = { ...song, password }
```

Correct:

```ts
const password = await getWebDavPassword(source.credentialKey)
await AudioPlayerNative.play({ sourceType: 'webdav', ..., password })
// password never assigned to reactive state or localStorage
```

Avoid creating service/client/cache abstractions before the application has actual data access requirements.

---

## Anti-Patterns

Avoid:

- Adding Pinia/Vuex just to store tab/page labels.
- Mirroring router state in a global store.
- Creating API or persistence folders without actual API or persistence behavior.
- Storing credentials, tokens, or WebDAV passwords in `localStorage` or logs.
- Using browser `fetch`/XHR WebDAV clients for Android WebDAV browsing when remote servers do not provide CORS headers.
- Passing large untyped state objects through props.
