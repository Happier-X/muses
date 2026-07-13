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

Avoid duplicating current tab selection in a separate store unless a task introduces a real cross-component state need. Do not mirror player/queue overlay visibility into the router; use `src/features/player/overlay.ts` for that state.

---

## Local Component State

For future small UI interactions, prefer local Vue state inside `<script setup lang="ts">` using Vue Composition API primitives such as `ref` and `computed`.

Because current components are static, no existing file demonstrates local reactive state yet. Add it in the component that owns the interaction unless multiple components need the same value.

---

## Global State

Do not add a global store preemptively. Introduce a global state library only when the app has repeated cross-page or cross-component state that cannot be cleanly handled with local state, props, emits, router params, or a focused composable.

Focused feature-level composables are acceptable for small cross-component UI state. Current example:

- `src/features/player/overlay.ts` owns `playerOverlayVisible` and `queueOverlayVisible` plus `open*/close*` helpers for global player/queue overlays.
- Player and queue overlays are UI state, not route state; opening them must not push `/player` or `/queue` into the router.
- `App.vue` is the overlay host and owns Android back-button precedence: close queue overlay first, then player overlay, then `App.minimizeApp()` (do not `exitApp()`; destroying the Activity tears down media-session foreground notification).
- Keep `MiniPlayer` mounted behind overlays and disable interaction while overlays are active; do not use route checks or `v-if` unmounting for overlay visibility side effects.

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
- `saveSongs(...)` dispatches the feature-local `SONGS_UPDATED_EVENT` after persistence; pages that display song metadata, such as `SongsPage.vue`, should listen while mounted and reload via `loadSongs()` so lazy metadata rescans are reflected without a full route reload.
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
- `AudioPlayer.seek({ position: number }): Promise<void>`，`position` 使用秒作为前端/插件边界单位，Android 服务内部再转换为毫秒。
- `AudioPlayer.getState(): Promise<AudioPlayerNativeState>`，返回 `status`、`position`、`duration`、`errorMessage?` 等安全播放快照。
- `AudioPlayer.addListener('stateChange', listener: (state: AudioPlayerNativeState) => void): Promise<PluginListenerHandle>`，播放中状态广播应携带当前进度与总时长；自然播放结束应广播 `status='finished'`，手动停止才广播 `status='stopped'`。
- Player queue helpers in `src/features/player/queue.ts`: `enqueueSongs(songs)`, `enqueueSong(song)`, `removeSongFromQueue(songId)`, `clearQueue()`, `selectSongAtIndex(index)`, `peekNext()`, `advanceToNext()`, `advanceToPrevious()`, `setRepeatMode(mode)`, `toggleShuffle()`, `syncCurrentIndex(songId)`.
- `peekNext()`：按当前 repeat/shuffle 规则返回下一首 `SongItem | null`，**不**修改 `currentIndex` / 不写持久化；与 `advanceToNext` 目标一致。
- Queue storage keys: `muses:queue` stores queue order as `{ songId }` items only; `muses:player-config` stores `{ repeatMode: 'one' | 'all', shuffleEnabled: boolean }`.
- Android `AudioPlaybackService` exposes the single native `androidx.media3.session.MediaSession` backed by the same `ExoPlayer` that performs playback.
- Media3 `MediaItem.MediaMetadata` contains non-sensitive display metadata only: title, artist, and album.
- System notification, lock-screen controls, and media-key commands are handled by Media3 native session/player APIs, not by a frontend media-session plugin or JS action handlers.
- Player controller state held in `src/features/player/controller.ts` as a `reactive<PlayerState>` shared across components，包含当前歌曲快照、播放状态、进度、总时长、**已缓冲终点 `bufferedPosition: number | null`**、歌词、封面引用与标签补扫状态。
- `bufferedPosition`：秒；`null` 表示缓冲未知（不画假缓冲条，seek 退化为 duration clamp）；WebDAV 远程直链始终为 `null`；WebDAV **完整缓存命中**与本地完整文件可设为 duration；有值时 seek 上限为 `min(duration, bufferedPosition)`，越界拒绝。切歌 / stop / 失败必须重置为 `null`。

#### 3. Contracts

- Player state must never contain WebDAV passwords, Basic Auth headers, SecureStorage values, or cover image base64 data.
- `MiniPlayer.vue` and `SongsPage.vue` only read from `playerState` (readonly) and call `playSong`/`pausePlayback`/`resumePlayback`/`stopPlayback`/`seekPlayback`; queue UI may call feature-local queue helpers exported from the player controller.
- `playSong(song)` resolves the WebDAV password via `getWebDavPassword(source.credentialKey)` and passes it only to `AudioPlayerNative.play(...)` / 预取 bridge。`native.ts` 播放 WebDAV 时先查 `getCachedWebDavAudioFile`：完整缓存命中则 `file://` + full buffer；否则远程 URL + Basic `Authorization` 直链。**禁止**调用 `prepareWebDavAudioFile` 或播放增长中的 `.partial`。密码不得进入 localStorage、UI state、`muses:songs`、`muses:queue`、Media3 metadata 或通知文本。
- `playSong` 成功进入 `playing` 后调度下一首 WebDAV 完整预取（`peekNext` → `prefetchWebDavAudioFile`）；跳过本地/自身/空；失败静默；旧预取不取消，同 URL 复用会话。
- controller 对 `enqueueSongs`/`enqueueSong`/`removeSongFromQueue`/`clearQueue`/`setRepeatMode`/`toggleShuffle` 做包装：在 `playing`/`paused` 时队列或模式变化后重调度预取。
- `playSong(song)` must first merge the incoming list item with the latest matching `muses:songs` record (`id` or `(sourceId, path)`) before creating `PlayerSongSnapshot`; otherwise stale list objects can miss lyrics/cover data that lazy metadata rescan already persisted.
- Playback pages may persist and display `lyrics`, `lyricsSource`, `coverUri`, `tagsScanned`, and `tagsScannedAt`; `coverUri` must be an app-private cache URI/reference, not a `data:` URL or raw base64 payload. Display helpers must reject `data:` / `blob:` / `;base64,` cover URIs even if malformed legacy data reaches the page.
- Playback queue persistence must be ID-only: write `{ songId }` records and order arrays, never full `SongItem`, `uri`, WebDAV username/password, Authorization headers, SecureStorage values, lyrics text, or cover URI/base64. Resolve queue display items from latest `loadSongs()` records at read time.
- Random playback uses a persisted `shuffleOrder` separate from the original order; toggling shuffle off must restore the original sequential queue order and keep the current song index aligned when possible.
- The immersive player overlay (`PlayerPage.vue`) should expose previous, play/pause, next, repeat, playback mode, and queue-entry controls. Previous uses the current queue order only (wrap head to tail); do not introduce an independent playback history stack unless a future task explicitly requires it. Opening this overlay from `MiniPlayer` requires a current song (`playerState.currentSong`); when there is no current song, the mini player body must not open the overlay.
- Frontend code must not depend on `@capgo/capacitor-media-session` or any third-party media-session/notification plugin; Android system controls must route to the project-local `AudioPlaybackService` and its Media3 `MediaSession`.
- If a song has incomplete tags, `playSong(song)` may trigger a single-song lazy metadata rescan after playback starts. The rescan must not block playback, must not scan the whole library, and must only update the currently playing song if the song identity still matches.
- 本地标签扫描后若当前曲仍无安全 `coverUri`，可异步在线匹配封面（iTunes → kw）：下载到 app `cache/covers` 后 `upsertSong` 写回安全 `file://` URI，并 `syncDisplayStateFromSong` + 媒体会话封面；已有封面不覆盖；失败静默；token 防串曲。
- 歌词展示优先级为在线 amll-ttml-db TTML > 本地内嵌 LRC / 同目录同名 `.lrc` > 明确无歌词状态；开播/切歌后无论是否有本地词都异步匹配在线 TTML，失败静默回退。
- 在线歌词仅保存于运行时：`PlayerState.lyrics` 存当前展示原文，`lyricsFormat` 决定 LRC/TTML 解析器，`onlineLyricsStatus` 表示匹配状态；索引、TTML 与负结果均为内存缓存，不写回 `SongItem`、`muses:songs` 或队列持久化。在线**封面**与歌词不同：封面命中后写回 `SongItem.coverUri`（仅本地缓存 URI）。
- Android `AudioPlayerPlugin.kt` passes `EXTRA_PASSWORD` into an Intent; `AudioPlaybackService.kt` uses it only to construct a Basic Auth header for the ExoPlayer `DefaultHttpDataSource.Factory`, then the password is discarded after the MediaItem is created.
- `AudioPlaybackService.kt` is started with `ContextCompat.startForegroundService(...)`; it must call `startForeground(...)` during `onCreate()` or immediately on play start, otherwise Android kills the app with `Context.startForegroundService() did not then call Service.startForeground()`.
- `AudioPlaybackService.kt` may use a minimal bootstrap foreground notification to satisfy `startForegroundService(...)`, but the long-lived user-visible media notification should come from Media3 `DefaultMediaNotificationProvider` or an equivalent official Media3 notification provider.
- `AudioPlaybackService.kt` must distinguish natural end from stop/switch: natural `Player.STATE_ENDED` with a current song publishes `STATUS_FINISHED` without clearing `currentSongId` or stopping foreground service; manual stop and internal switch clear `currentSongId` before `player.stop()` so they do not emit a false finished event.
- When playback fails, the frontend shows only white-listed business errors or a generic message; it does not forward auth-related exception details into reactive state or `MiniPlayer.vue`.
- AMLL background usage in `PlayerPage.vue` must wrap `BackgroundRender` in a real positioned container with explicit dimensions. The Vue binding renders its component root as `display: contents`, so styling the component root alone can leave the internal canvas with a 1px/0px height. Use an outer `.amll-background` layer and place `BackgroundRender` inside it with a full-size class; fallback CSS background must not cover the AMLL canvas when lyrics exist.
- Two-panel player swiping uses a `200%`-wide flex container with two `50%` panels; switching to the lyric panel must translate by `-50%`, not `-100%`, or the lyric page is rendered off-screen.

#### 4. Validation & Error Matrix

- Missing WebDAV password when playing a WebDAV song -> `playSong` throws with `WebDAV 密码不存在，请重新添加该音源。`.
- Missing WebDAV source entry -> `playSong` throws with `找不到这首歌对应的 WebDAV 音源，请重新扫描音源。`.
- Non-white-listed native exceptions -> frontend shows `播放失败，请稍后重试。`.
- Native pause/resume/stop/seek exception -> frontend shows a generic operation-failed message.
- Seek position below `0` -> clamp to `0`; seek position beyond known duration -> clamp to duration before calling native seek.
- Seek position beyond known `bufferedPosition`（缓冲已知）-> **拒绝 seek**（`seekPlayback` 返回 `false`），不调用原生 seek；缓冲未知时退化为 duration clamp。
- Unknown or non-positive duration -> disable deterministic progress UI/notification progress rather than publishing misleading values.
- Lazy metadata rescan failure -> mark metadata status as failed or show a degraded UI; playback continues.
- Incoming song object lacks lyrics/cover but latest `muses:songs` has them -> `playSong` uses the latest stored display metadata immediately and does not show a false no-lyrics state.
- Native `status='finished'` 仅在可判定为**自然播完**时才 `handlePlaybackFinished` → `advanceToNext()`；有下一首则 `playSong(nextSong)`，否则 `stopPlayback()`。
- 自然播完判定：`duration > 0` 且 `position >= duration - NATURAL_END_EPSILON_SEC`（约 1.25s），且不在用户 seek 保护窗内（`SEEK_FINISH_GUARD_MS`≈1500ms）。
- **非自然结束的 finished 不得 advance**：seek 保护窗内、未接近结尾、或 `duration=0` 时，丢弃伪 finished，恢复 `playing`/`paused`，保留 `currentSong` 与最近有效/seek 目标进度。
- 进度条、歌词点击、媒体会话 `seekto` 共用 `seekPlayback`；成功后记录 `lastSeekAt`。`playSong`/`stopPlayback` 清理 seek guard。
- Manual stop or internal song switch publishes `stopped`/`loading`, not `finished`; otherwise queue auto-advance can skip tracks or recurse.
- Malformed or polluted `muses:queue` -> ignore invalid entries and re-save sanitized `{ songId }` records only.
- Android system notification, lock-screen, or media-key control fails to update player state -> verify the single Media3 `MediaSession` is bound to the same `ExoPlayer` used for playback.
- Media3 metadata or notification text contains WebDAV password, Basic Auth, authenticated audio URL, or `data:` artwork -> fail the implementation; keep metadata to safe title/artist/album fields.
- AMLL background canvas height is near zero in WebView DevTools -> wrap `BackgroundRender` in a real full-size element instead of relying on component root styling.
- Foreground service started but no timely `startForeground(...)` call -> app ANR/crash; fix by creating a notification channel and foreground notification before long playback work.
- Anonymous song or missing URI in a play Intent -> native service publishes `STATUS_ERROR` with a safe message, not raw exception details.

#### 5. Good/Base/Bad Cases

- Good: clicking a local song plays it; clicking a different song stops the previous one and plays the new one; the mini player updates across tabs, and the player overlay displays synced progress, cover, controls, AMLL background, and lyrics fallback states.
- Base: user stops playback via the mini player; Android notification disappears; `currentSong` is cleared; clicking the mini player body no longer opens the player overlay until a song is playing again.
- Good: a stale song list item is played after metadata rescan has persisted lyrics; the player overlay uses the latest stored lyrics immediately.
- Good: a song row is visible while lazy metadata rescan persists title/artist/album/cover; `SONGS_UPDATED_EVENT` causes the list page to reload the latest `muses:songs` record.
- Bad: WebDAV password ends up in `localStorage.getItem('muses:songs')`, `localStorage.getItem('muses:queue')`, or is logged to diagnose a playback failure.
- Bad: a queue stores full `SongItem` objects or audio `uri` values instead of ID-only order records.
- Bad: a cover image is saved into `muses:songs` as base64, rendered into the UI as a `data:` URL, stored in `muses:queue`, or passed into Media3 metadata/notification payloads as base64 artwork.
- Bad: adding a separate media-session plugin creates a second session/foreground service/notification instead of using the project-local Media3 service.
- Bad: AMLL `BackgroundRender` is mounted directly as the positioned layer and its internal canvas measures as 1px high.
- Bad: treating every native `finished`/`complete` as natural end and calling `advanceToNext` after a mid-track seek to an unbuffered position.
- Good: mid-track seek then pseudo-`finished` keeps the current song; only near-end finished without seek guard advances the queue.
- Good: WebDAV without complete cache uses remote URL + Basic Auth, keeps `bufferedPosition = null`, no fake buffer bar, duration-clamp seek.
- Good: WebDAV complete cache hit plays `file://` with full buffer (`bufferedPosition = duration`).
- Good: after play success, next WebDAV track is prefetched in background; local next / single-repeat self / empty queue skips prefetch.
- Good: local ready song reports full buffer (`bufferedPosition = duration`) and allows full-length seek.
- Bad: switching songs inherits previous track's `bufferedPosition`.
- Bad: playing a growing `.partial` file or treating partial as complete cache.

#### 6. Tests Required

- `playSong` for a local song calls `AudioPlayerNative.play` with local options only (no password).
- `playSong` for a WebDAV song calls `getWebDavPassword`, passes it only to `AudioPlayerNative.play` / prefetch bridge, and does not store it；`native.ts` tests assert：无缓存时远程 URL + Authorization、缓存命中 `file://` + full buffer、不调用 `prepareWebDavAudioFile`、partial 回退远程。
- Queue tests assert `peekNext` matches `advanceToNext` without mutating index; controller tests assert prefetch trigger/skip (local next, single-repeat self) and silent failure.
- `controller.ts` error handler maps unknown native errors to a safe string.
- `MiniPlayer.vue` renders the current title, toggles play/pause, stops, opens the player overlay from the bar body only when `currentSong` exists (click/Enter/Space do nothing when empty), and prevents control-button click/keyboard events from bubbling into overlay open.
- `PlayerPage.vue` renders no-current-song and no-lyrics states; progress dragging calls `seekPlayback` with seconds.
- `PlayerPage.vue` rejects `data:` cover URIs, renders AMLL lyrics after a left-swipe, uses `translateX(-50%)` for the second panel, and mounts `BackgroundRender` under a full-size `.amll-background` container.
- `playSong` tests cover stale input objects by seeding `muses:songs` with newer lyrics/cover and asserting `playerState` uses the newer metadata.
- Queue tests assert ID-only persistence, enqueue/remove/clear behavior, sequential next, previous wrapping from head to tail, random shuffle order length, single-song repeat, list loop from tail to head, near-end `finished` auto-advance, seek-guard suppressing false `finished`, and empty queue fallback to `stopPlayback`.
- Buffer/seek tests assert: clamp/reject past `bufferedPosition`, lyric reject, local full buffer, stop/切歌 reset, monotonic buffer growth, unknown-buffer duration clamp, PlayerPage `--buffered` style.
- `SongsPage.vue` highlights the currently playing song and queue actions do not bubble into direct playback.
- Android validation should include progress broadcasting, `finished` broadcasting on natural end, Media3 notification/lock-screen position sync, seek clamping, service resource release, media-key routing to the same `ExoPlayer`, and checking for duplicate visible notifications.

#### 7. Wrong vs Correct

Wrong:

```ts
const options: WebDavPlayOptions = { ..., password }
state.currentSong = { ...song, password, coverBase64 }
localStorage.setItem('muses:queue', JSON.stringify([song]))
await AudioPlayerNative.play({ sourceType: 'webdav', ..., password })
await ThirdPartyMediaSession.setMetadata({ title, artwork: [{ src: song.uri }], password })
const coverSrc = song.coverUri // may be data:image/jpeg;base64,...
<BackgroundRender class="amll-background" /> // root is display: contents; canvas can measure 1px high
const transform = `translateX(-${activePanel * 100}%)` // wrong for 200%-wide two-panel container
```

Correct:

```ts
const latestSong = getLatestSongSnapshot(song)
const password = await getWebDavPassword(source.credentialKey)
await AudioPlayerNative.play({ sourceType: 'webdav', ..., password })
localStorage.setItem('muses:queue', JSON.stringify({ items: [{ songId: latestSong.id }] }))
await seekPlayback(42) // seconds at the frontend/native plugin boundary
const coverSrc = toDisplayableUri(latestSong.coverUri) // returns '' for data: URIs
// Android AudioPlaybackService creates MediaItem metadata with title/artist/album only.
// Android MediaSessionService + DefaultMediaNotificationProvider own system notification/media keys.
// password never assigned to reactive state, localStorage, Media3 metadata, or notification text; cover uses safe coverUri only
// AMLL background is wrapped in a real full-size element; lyric panel uses translateX(-50%)
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
