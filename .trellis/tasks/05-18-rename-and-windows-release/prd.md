# PRD: Project Rename & Windows Release

## Summary

将项目从 `nagomusic` (NagoMusic) 重命名为 `Muses`，并在 GitHub Actions CI 中增加 Windows 版本的 MSIX 安装包构建和发布。

---

## Requirements

### 1. Project Rename (nagomusic → Muses)

| # | File | What to Change | Notes |
|---|------|---------------|-------|
| 1 | `pubspec.yaml` | `name: nagomusic` → `name: muses` | Pub package name |
| 2 | `android/app/build.gradle.kts` | `namespace` & `applicationId` → `com.happier.muses` |  |
| 3 | `android/app/src/main/AndroidManifest.xml` | `android:label` → `Muses`; lyricon metadata → `Muses` |  |
| 4 | `ios/Runner/Info.plist` | `CFBundleDisplayName` → `Muses`; `CFBundleName` → `Muses` |  |
| 5 | `macos/Runner/Configs/AppInfo.xcconfig` | `PRODUCT_NAME` → `Muses`; `PRODUCT_BUNDLE_IDENTIFIER` → `com.happier.muses` |  |
| 6 | `windows/CMakeLists.txt` | `project()` & `BINARY_NAME` → `muses` |  |
| 7 | `windows/runner/Runner.rc` | File/Product name fields → `Muses`; CompanyName → `happier` | Version info resource |
| 8 | `windows/runner/main.cpp` | Window title → `Muses` |  |
| 9 | `web/manifest.json` | `name` & `short_name` → `Muses` |  |
| 10 | `lib/app/app.dart` | Class `NagoMusicApp` → `MusesApp`; title → `Muses` |  |
| 11 | `lib/main.dart` | `NagoMusicApp()` → `MusesApp()` |  |
| 12 | `lib/pages/settings/version_info_page.dart` | `_appName` → `Muses`; debug log prefix → `muses-debug-` |  |
| 13 | `lib/pages/settings/cache_settings_page.dart` | Display strings `NagoMusic` → `Muses` |  |
| 14 | `lib/components/layout/side_menu.dart` | Display string `NagoMusic` → `Muses` |  |
| 15 | `lib/app/services/*.dart` (6 files) | Method channels, db name, content URIs → `com.happier.muses`; db → `muses.db` | song_download_service, native_audio_thumbnail_service, media_notification_service, meizu_lyrics_service, lyricon_service, android_platform_service, db_constants |
| 16 | `lib/app/services/app_update_service.dart` | GitHub repo URL → TBD (user's fork) | 需要用户提供 fork 后的 GitHub 用户名 |

### 2. Windows Release Build (MSIX)

- CI workflow 增加 `build-windows` job，与 `build-android` 同级（统一触发）
- Windows job 运行在 `windows-latest` runner
- 使用 `msix` pub package 打包 MSIX 安装包
- 产物上传至同一个 GitHub Release 页面

### 3. CI Artifact Naming

所有产物命名从 `nagomusic-*` 改为 `muses-*`。

---

## Acceptance Criteria

1. `flutter analyze` 通过，无剩余 `nagomusic`/`NagoMusic` 引用（除上游 GitHub URL 待定外）
2. `flutter test` 通过
3. CI workflow 能同时构建 Android APK 和 Windows MSIX
4. Windows MSIX 安装包正确命名并上传到 Release

---

## Constraints

- 保持原 Material 3 + dynamic color 主题方案不变
- 不更改现有业务逻辑
- 数据库文件路径变更需做好数据迁移或兼容（`muses.db` vs `nagomusic.db`）→ 老用户数据会丢失，可接受
- MSIX 签名：CI 需要 `WINDOWS_CERT_BASE64` 和 `WINDOWS_CERT_PASSWORD` 两个 secrets

## Open Questions

1. GitHub 用户名（用于更新 app_update_service.dart 中的更新检查 URL）→ 等待用户提供
2. Windows 代码签名证书 → CI secrets 需要提前准备
