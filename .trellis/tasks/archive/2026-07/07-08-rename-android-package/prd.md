# 改安卓包名为 com.muses.player

## 目标

将项目 Android 原生包名从 `ionic.muses` 改为 `com.muses.player`。

## 涉及文件

- `capacitor.config.ts`：`appId: 'ionic.muses'`
- `android/app/build.gradle`：`namespace`, `applicationId`
- `android/app/src/main/AndroidManifest.xml`：`package=` 或 `applicationId` 引用（如有）
- `android/app/src/main/res/values/strings.xml`：`package_name`, `custom_url_scheme`
- 以下 Kotlin 源文件头部 `package ionic.muses`：
  - `AudioPlayerPlugin.kt`
  - `AudioPlaybackService.kt`（含 action 常量前缀 `ionic.muses.audio`）
  - `LocalLibraryPlugin.kt`
  - `WebDavPlugin.kt`
  - `WebDavAudioCache.kt`
  - `AudioMetadataReader.kt`
  - `MainActivity.kt`
- 目录：从 `android/app/src/main/java/ionic/muses/` 移到 `android/app/src/main/java/com/muses/player/`
- 前端 `src/features/player/native.ts` 中如有 Capacitor plugin name 引用需同步（当前 plugin name 为 `AudioPlayer`，与包名无关，但需确认）

## 非目标

- 不改 Capacitor iOS 端。
- 不改前端路由、UI 或状态管理。
- 不修改 ExoPlayer intent action 字符串（保持当前功能即可）。

## 验收标准

- `capacitor.config.ts` 中 `appId` 为 `com.muses.player`。
- `android/app/build.gradle` 中 `namespace` 和 `applicationId` 一致。
- 所有 Kotlin 文件中 `package` 声明为 `com.muses.player`。
- Android 编译通过 `cd android && ./gradlew :app:compileDebugKotlin`。
- 前端编译通过 `npm run build`。