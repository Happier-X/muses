# WebDAV Client Research for Android

**Date:** 2026-04-25

## Recommendation: dav4jvm (bitfireAT/dav4jvm)

Kotlin-first WebDAV client. Used in DAVx⁵ (production app). MPL 2.0 license.

### Key Features
- 100% Kotlin, null-safe, coroutine-friendly
- Both Basic + Digest auth via `BasicDigestAuthHandler`
- OkHttp 5.3.2 engine (connection pooling, HTTP/2)
- Clean PROPFIND API with property selection and depth control
- Streaming downloads via OkHttp's native InputStream
- XPP3 XML parser (already built into Android)

### Streaming Audio Pattern

```kotlin
val authHandler = BasicDigestAuthHandler(null, username, password)
val okHttpClient = OkHttpClient.Builder()
    .followRedirects(false)
    .authenticator(authHandler)
    .addNetworkInterceptor(authHandler)
    .build()

// Pass authenticated OkHttpClient to Media3 for playback
val factory = OkHttpDataSource.Factory(okHttpClient)
val mediaSource = ProgressiveMediaSource.Factory(factory)
    .createMediaSource(MediaItem.fromUri(webdavFileUrl))
exoPlayer.setMediaSource(mediaSource)
exoPlayer.prepare()
```

### Browsing Pattern

```kotlin
val davCollection = DavCollection(client, "https://example.com/webdav/".toHttpUrl())
davCollection.propfind(depth = 1,
    DisplayName.NAME, GetContentLength.NAME, GetLastModified.NAME,
    GetContentType.NAME, ResourceType.NAME
) { response, relation ->
    // response.properties contains the requested WebDAV properties
}
```

### Cons
- JitPack only (no Maven Central)
- Currently transitioning to ktor for Kotlin Multiplatform (ktor always on classpath, ~1MB)
- Steeper learning curve (WebDAV spec concepts exposed)

## Runner-up: Sardine-Android

OkHttp-based, simpler API. Cons: JitPack only, Basic auth only (no Digest), last release Feb 2024 (stale).

## Not Recommended
- **Sardine (lookfirst):** Apache HttpClient 4.5.x — dual HTTP stack antipattern
- **Jackrabbit WebDAV:** Apache HttpClient, OSGi packaging, "beta" quality, heavy deps
- **Manual OkHttp + XML:** Viable for minimal deps but ~400 lines of PROPFIND parsing to maintain

## Dependency Comparison

| Library | APK impact | HTTP Engine | Digest Auth |
|----------|:---:|------|:---:|
| dav4jvm | ~2MB | OkHttp 5.3.2 | Yes |
| Sardine-Android | ~1.5MB | OkHttp 4.12.0 | No |
| OkHttp + Manual | ~1MB | OkHttp (any) | DIY |
